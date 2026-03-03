package com.example.demo.controller;

import com.example.demo.dto.CreateEventRequest;
import com.example.demo.entity.EventParticipant;
import com.example.demo.entity.GameEvent;
import com.example.demo.entity.GameEvent.EventStatus;
import com.example.demo.repository.EventParticipantRepository;
import com.example.demo.repository.GameEventRepository;
import com.example.demo.service.LiveBattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class GameEventController {

    @Autowired
    private LiveBattleService liveBattleService;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private EventParticipantRepository eventParticipantRepository;

    // ✅ HELPER: Chuyển GameEvent sang Map (Thêm logic lấy người thắng cuộc)
    private Map<String, Object> mapEventToDto(GameEvent event) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", event.getId());
        dto.put("title", event.getTitle());
        dto.put("description", event.getDescription());
        dto.put("startTime", event.getStartTime());
        dto.put("status", event.getStatus());
        dto.put("accessCode", event.getAccessCode());
        dto.put("maxParticipants", event.getMaxParticipants());
        dto.put("durationMinutes", event.getDurationMinutes());
        
        // Lấy số lượng người tham gia thực tế
        long realParticipantCount = eventParticipantRepository.countByEvent_Id(event.getId());
        dto.put("currentParticipants", realParticipantCount);

        // Map Topic
        if (event.getTopic() != null) {
            Map<String, Object> topicDto = new HashMap<>();
            topicDto.put("id", event.getTopic().getId());
            topicDto.put("name", event.getTopic().getName());
            dto.put("topic", topicDto);
        }

        // ✅ LOGIC MỚI: Nếu game đã kết thúc, tìm người chiến thắng
        if (event.getStatus() == EventStatus.ENDED) {
            List<EventParticipant> winners = eventParticipantRepository.findByEvent_IdOrderByScoreDescUpdatedAtAsc(event.getId())
                    .stream()
                    .filter(p -> p.getStatus() == EventParticipant.ParticipantStatus.WINNER)
                    .limit(1) // Lấy 1 người đại diện
                    .collect(Collectors.toList());
            
            if (!winners.isEmpty()) {
                EventParticipant w = winners.get(0);
                Map<String, Object> winnerDto = new HashMap<>();
                winnerDto.put("name", w.getUser().getName());
                winnerDto.put("avatarUrl", w.getUser().getAvatarUrl());
                winnerDto.put("score", w.getScore());
                dto.put("winner", winnerDto);
            }
        }

        return dto;
    }

    // ✅ API MỚI: Lấy danh sách hiển thị cho Client (Bao gồm ALL status)
    @GetMapping("/public-list")
    public ResponseEntity<?> getPublicEvents() {
        try {
            // Lấy tất cả sự kiện sắp xếp mới nhất lên đầu
            List<GameEvent> events = liveBattleService.getAllEvents(); 
            
            List<Map<String, Object>> result = events.stream()
                .map(this::mapEventToDto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    // --- CÁC API CŨ GIỮ NGUYÊN ---

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventDetail(@PathVariable Long id) {
        try {
            GameEvent event = gameEventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + id));
            return ResponseEntity.ok(mapEventToDto(event));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody CreateEventRequest request) {
        try {
            GameEvent updatedEvent = liveBattleService.updateGameEvent(id, request);
            return ResponseEntity.ok(mapEventToDto(updatedEvent));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvent() {
        GameEvent event = liveBattleService.getNearestUpcomingEvent();
        if (event == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(mapEventToDto(event));
    }

    @GetMapping("/upcoming-list")
    public ResponseEntity<?> getUpcomingEvents() {
        try {
            List<GameEvent> events = liveBattleService.getUpcomingEvents();
            List<Map<String, Object>> result = events.stream().map(this::mapEventToDto).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllEvents() {
        List<GameEvent> events = liveBattleService.getAllEvents();
        List<Map<String, Object>> result = events.stream().map(this::mapEventToDto).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/participants")
    @Transactional(readOnly = true) 
    public ResponseEntity<?> getEventParticipants(@PathVariable Long id) {
        try {
            List<EventParticipant> participants = eventParticipantRepository.findByEvent_IdOrderByScoreDescUpdatedAtAsc(id);
            List<Map<String, Object>> result = participants.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("score", p.getScore());
                map.put("status", p.getStatus());
                map.put("rankPosition", p.getRankPosition());
                if (p.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", p.getUser().getId());
                    userMap.put("name", p.getUser().getName() != null ? p.getUser().getName() : "Người chơi");
                    userMap.put("username", p.getUser().getUsername());
                    userMap.put("avatarUrl", p.getUser().getAvatarUrl());
                    map.put("user", userMap);
                } else {
                     map.put("user", null);
                }
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createEvent(@RequestBody CreateEventRequest request) {
        try {
            GameEvent newEvent = liveBattleService.createGameEvent(request);
            return ResponseEntity.ok(mapEventToDto(newEvent));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startEvent(@PathVariable Long id) {
        try {
            liveBattleService.startEvent(id);
            return ResponseEntity.ok("Sự kiện đã bắt đầu!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNextEventForBanner() {
        var now = java.time.LocalDateTime.now();
        GameEvent live = gameEventRepository.findActiveLiveForBanner(now).orElse(null);
        if (live != null) return ResponseEntity.ok(mapEventToDto(live));
        GameEvent up = gameEventRepository.findNearestUpcomingFromNow(now).orElse(null);
        if (up == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(mapEventToDto(up));
    }

    @MessageMapping("/event/{eventId}/answer")
    public void receiveAnswer(@DestinationVariable Long eventId, StompHeaderAccessor accessor, @Payload Map<String, String> payload) {
        Principal user = accessor.getUser();
        String userId = (user != null) ? user.getName() : accessor.getSessionId();
        String answer = payload.get("answer");
        liveBattleService.processUserAnswer(eventId, userId, answer);
    }

    @MessageMapping("/event/{eventId}/join")
    public void joinGame(@DestinationVariable Long eventId, StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String userId = (user != null) ? user.getName() : accessor.getSessionId();
        liveBattleService.handleUserJoin(userId, eventId);
    }
}