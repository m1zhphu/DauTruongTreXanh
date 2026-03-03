package com.example.demo.config;

import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    
    // ✅ Inject Interceptor bạn đã viết
    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic: Public (Market listing, Chat room)
        // /queue: Private (Thông báo bán được hàng)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    // ✅ Đăng ký Interceptor vào đây
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Dùng chung 1 endpoint cho cả Game và Market
        registry.addEndpoint("/ws-game")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(
                            org.springframework.http.server.ServerHttpRequest request,
                            org.springframework.web.socket.WebSocketHandler wsHandler,
                            Map<String, Object> attributes
                    ) {
                        // Logic fallback: Nếu Interceptor chưa set User (do không gửi Header), 
                        // thì thử lấy từ Query Param (token=...) như code cũ của bạn.
                        try {
                            String query = request.getURI().getQuery(); 
                            String token = null;

                            if (query != null) {
                                for (String part : query.split("&")) {
                                    if (part.startsWith("token=")) {
                                        token = URLDecoder.decode(part.substring("token=".length()), StandardCharsets.UTF_8);
                                        break;
                                    }
                                }
                            }

                            if (token != null && !token.isBlank()) {
                                String username = jwtService.extractUsername(token);
                                if (username != null && !username.isBlank()) {
                                    final String u = username;
                                    return () -> u; // Principal là username
                                }
                            }
                        } catch (Exception ignored) {}

                        // Nếu không có token nào -> GUEST
                        final String guest = "GUEST-" + UUID.randomUUID();
                        return () -> guest;
                    }
                })
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}