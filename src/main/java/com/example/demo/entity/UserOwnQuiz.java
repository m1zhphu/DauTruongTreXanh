package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList; // Import ArrayList
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOwnQuiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String coverImage;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner; 

    // SỬA LẠI: mappedBy="quiz" để trỏ tới trường 'quiz' bên UserOwnQuestion
    // orphanRemoval = true: Khi xóa câu hỏi khỏi list, nó sẽ xóa khỏi DB
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserOwnQuestion> questions = new ArrayList<>(); // Khởi tạo list rỗng để tránh Null

    private LocalDateTime createdAt;
}