package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_events")
@Data
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description; // mô tả / luật chơi

    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    private EventStatus status; // UPCOMING, LIVE, ENDED

    private String accessCode;

    @Column(columnDefinition = "integer default 0")
    private int currentParticipants = 0;

    // ✅ THÊM MỚI (có default để khỏi lỗi DB khi update)
    @Column(columnDefinition = "integer default 50")
    private int maxParticipants = 50;

    @Column(columnDefinition = "integer default 30")
    private int durationMinutes = 30; // event kéo dài bao lâu

    @Column(columnDefinition = "integer default 5")
    private int lobbyOpenMinutes = 5; // mở phòng chờ trước bao nhiêu phút

    @Column(columnDefinition = "integer default 15")
    private int questionSeconds = 15; // mỗi câu bao nhiêu giây

    @Column(columnDefinition = "integer default 5")
    private int resultSeconds = 5; // xem đáp án bao nhiêu giây

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    public enum EventStatus {
        UPCOMING, LIVE, ENDED
    }
}
