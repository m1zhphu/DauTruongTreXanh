package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GachaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Tên vật phẩm (Ví dụ: Skin Thánh Gióng)
    private String type;        // Loại: SKIN, NAME_CARD, PROTECTION_CARD
    private String imageUrl;    // Ảnh hiển thị
    private String rarity;      // Độ hiếm: COMMON, RARE, LEGENDARY
    
    // Tỉ lệ rơi (Trọng số). Ví dụ: 100 (phổ biến), 10 (hiếm), 1 (cực hiếm)
    private int dropWeight; 
}