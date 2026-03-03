package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "radio_tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadioTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // Tên bài
    private String author;      // Tác giả/Người đọc (Đã thêm trường này)
    
    @Column(length = 1000)
    private String description; // Mô tả ngắn
    
    @Column(length = 500)
    private String audioUrl;    // Link file mp3
    
    private String coverUrl;    // Ảnh bìa
    
    @Enumerated(EnumType.STRING)
    private ContentType type;   // Thể loại (Enum)

    private Integer durationSeconds;
    
    @Builder.Default
    private Boolean isActive = true; // Mặc định là true
}