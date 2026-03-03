package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "event_participants",
    uniqueConstraints = @UniqueConstraint(name = "uq_event_user", columnNames = {"event_id", "user_id"}),
    indexes = {
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_event_status", columnList = "event_id,status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventParticipant {

    public enum ParticipantStatus {
        JOINED, ALIVE, ELIMINATED, LEFT, WINNER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private GameEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.JOINED;

    @Column(nullable = false)
    @Builder.Default
    private int score = 0;

    // ✅ SỬA LỖI SQL: Thêm giá trị mặc định là 0 để không bị null khi insert
    @Column(name = "rank_position")
    @Builder.Default
    private Integer rankPosition = 0;

    @Column(name = "current_round", nullable = false)
    @Builder.Default
    private int currentRound = 0;

    @Column(name = "last_question_id")
    private Long lastQuestionId;

    @Column(name = "last_answer", length = 255)
    private String lastAnswer;

    @Column(name = "eliminated_at")
    private LocalDateTime eliminatedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (joinedAt == null) joinedAt = now;
        updatedAt = now;
        // ✅ Double check: đảm bảo không bao giờ null
        if (rankPosition == null) rankPosition = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}