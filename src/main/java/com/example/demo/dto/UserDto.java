package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String role;
    
    // Chỉ số Game
    private String avatarUrl;
    private Long totalXp;
    private Integer currentStreak;
    private Long rice;
}