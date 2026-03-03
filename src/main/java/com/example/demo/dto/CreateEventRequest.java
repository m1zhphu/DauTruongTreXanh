package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

import com.example.demo.entity.GameEvent.EventStatus;

@Data
public class CreateEventRequest {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private Long topicId;
    private EventStatus status; 
    private Integer maxParticipants;
    private Integer durationMinutes;
    private Integer lobbyOpenMinutes;
    private Integer questionSeconds;
    private Integer resultSeconds;
    private String accessCode;
}
