package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_topic_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "topic_id"}) // Mỗi user chỉ có 1 dòng tiến độ cho 1 chủ đề
})
public class UserTopicProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "current_question_index")
    @Builder.Default
    private Integer currentQuestionIndex = 0; // Lưu chỉ số câu hỏi hiện tại (0, 1, 2...)

    @Column(name = "current_knots")
    @Builder.Default
    private Integer currentKnots = 0; // Số đốt tre hiện tại

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    @PrePersist
    public void updateTime() {
        this.updatedAt = LocalDateTime.now();
    }
}