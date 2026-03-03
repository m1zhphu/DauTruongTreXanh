package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomCode;
    private LocalDateTime endedAt;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private UserOwnQuiz quiz;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL)
    private List<GamePlayerResult> results;
}