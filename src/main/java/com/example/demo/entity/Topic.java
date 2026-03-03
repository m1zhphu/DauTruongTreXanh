package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore; // 1. Import cái này
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "topics")
@Data
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
    
    private String description;
    
    private int totalKnots; 
    
    private String filePath; 
    
    private boolean status = true; 

    private boolean isPublic; 

    private String accessCode; 
    
    private String password; 

    // --- SỬA LỖI TẠI ĐÂY ---
    // Thêm @JsonIgnore để khi lấy Topic không load hàng trăm câu hỏi đi kèm
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore 
    private List<Question> questions; 

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id") // Khóa ngoại trỏ đến bảng users
    @JsonIgnore // Tránh loop khi serialize
    private User creator;
}