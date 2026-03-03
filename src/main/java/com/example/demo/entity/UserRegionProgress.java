package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_region_progress")
public class UserRegionProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Giả sử bạn đã có Entity User

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    // "locked", "unlocked", "completed"
    private String status; 
    
    private LocalDateTime completedAt;
}