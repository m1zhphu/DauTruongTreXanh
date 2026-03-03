package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionDto {
    private Long id;
    private String content;
    private List<String> options;
    // Frontend cần biết đáp án đúng để check game
    private String correctAnswer; 
    private String explanation;
    private String topicName; // Chỉ cần tên chủ đề, không cần cả object Topic
    private Long topicId;

}