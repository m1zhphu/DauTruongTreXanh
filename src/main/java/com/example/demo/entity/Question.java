package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString; // Import thêm cái này
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.BatchSize; // Import thêm cái này
import java.util.List;

@Entity
@Table(name = "questions")
@Data 
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    // --- SỬA LỖI TẠI ĐÂY ---
    @ElementCollection
    @BatchSize(size = 50) // <--- Thêm dòng này: Giúp tải 50 list options cùng lúc thay vì 1 cái
    private List<String> options;

    private String correctAnswer;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    @ToString.Exclude // <--- Thêm dòng này: Tránh lỗi vòng lặp vô tận (StackOverflow) khi in log
    private Topic topic;

    // Constructor rỗng
    public Question() {}

    // Constructor đầy đủ
    public Question(Topic topic, String content, List<String> options, String correctAnswer, String explanation) {
        this.topic = topic;
        this.content = content;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }
}