package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOwnQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sửa tên trường từ 'content' (ở DTO) thành 'text' cho khớp, hoặc đổi tên trường trong DTO
    // Ở đây tôi thấy bạn dùng 'text' trong Entity nhưng 'content' trong DTO. Hãy thống nhất.
    // Tôi sẽ dùng 'text' theo Entity của bạn.
    private String text; 
    
    private String imageUrl;
    private int timeLimit; // Giây

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> options;
    
    private String correctAnswer;

    // --- THÊM QUAN HỆ NGƯỢC LẠI ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    @JsonIgnore // QUAN TRỌNG: Ngăn chặn vòng lặp vô tận khi convert JSON
    private UserOwnQuiz quiz;
}