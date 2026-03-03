package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_topic_access", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "topic_id"}) // 1 người chỉ cần mua 1 lần
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTopicAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người mua

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic; // Topic được mua

    private LocalDateTime purchasedAt; // Ngày mua
}