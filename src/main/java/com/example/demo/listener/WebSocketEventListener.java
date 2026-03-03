package com.example.demo.listener;

import com.example.demo.service.LiveBattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Component
public class WebSocketEventListener {

    @Autowired
    private LiveBattleService liveBattleService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) return;

        // PRINCIPAL CHÍNH LÀ sessionId -> getName()
        String principalName = event.getUser() != null ? event.getUser().getName() : sessionId;

        System.out.println("🔌 User connected: principal=" + principalName + ", session=" + sessionId);
    }


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            liveBattleService.handleUserLeave(sessionId);
            System.out.println("User disconnected: " + sessionId);
        }
    }
    
    // Hàm helper trích xuất Event ID từ URL subscribe
    private Optional<Long> extractEventId(String destination) {
        if (destination.startsWith("/topic/event/")) {
            try {
                String idStr = destination.substring("/topic/event/".length());
                return Optional.of(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}