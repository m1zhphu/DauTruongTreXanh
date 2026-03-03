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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password; // Null nếu dùng Google login

    private String name; // Tên hiển thị (VD: Trạng Tí)
    
    @Column(unique = true)
    private String email;
    
    private String role; // ROLE_USER, ROLE_ADMIN

    // === GAMIFICATION FIELDS ===
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "total_xp")
    @Builder.Default
    private Long totalXp = 0L;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "last_study_date")
    private LocalDateTime lastStudyDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider;

    // --- TRƯỜNG MỚI: TIỀN TỆ (HẠT THÓC) ---
    @Column(name = "rice") 
    @Builder.Default
    private Long rice = 100L; // Tặng sẵn 1000 hạt thóc để test tính năng

    @Column(columnDefinition = "bigint default 0")
    private Long quanTien = 0L; // ✅ Thêm Quan Tiền
}