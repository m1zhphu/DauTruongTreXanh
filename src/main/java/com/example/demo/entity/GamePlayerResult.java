package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;
    private int totalScore;
    private int rankPosition;

    @ManyToOne
    @JoinColumn(name = "history_id")
    @JsonIgnore
    private GameHistory history;
}