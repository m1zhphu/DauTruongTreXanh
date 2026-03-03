package com.example.demo.config;

import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Chỉ xử lý lúc CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 1) Ưu tiên JWT
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null) auth = accessor.getFirstNativeHeader("authorization");

            if (auth != null && !auth.isBlank()) {
                String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;

                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // ✅ dùng đúng signature của bạn: isTokenValid(token, username)
                        if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            accessor.setUser(authentication); // Principal = username thật
                        }
                    }
                } catch (Exception ignored) {
                    // token sai -> không set principal
                }
            }

            // 2) Nếu không có JWT, vẫn set PRINCIPAL theo guestId để /user/queue dùng được
            // (không bắt buộc, nhưng giúp guest vẫn nhận NEW_QUESTION qua /user/queue)
            if (accessor.getUser() == null) {
                String guestId = accessor.getFirstNativeHeader("X-Guest-Id");
                if (guestId != null && !guestId.isBlank()) {
                    Principal p = () -> "guest:" + guestId;
                    accessor.setUser(p);
                }
            }
        }

        return message;
    }
}
