package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder; // Thêm cái này
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Thêm cái này
@Table(name = "regions")
public class Region {
    @Id
    private String id;
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    // private String status; // Bỏ cái này vì lưu ở bảng progress rồi
    
    private String topPos; 
    private String leftPos;
    
    private String color; 
    private String requiredId; 
    private boolean isIsland;
    private Long topicId; // ID chủ đề liên kết
}