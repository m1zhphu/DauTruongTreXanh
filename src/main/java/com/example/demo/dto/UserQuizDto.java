package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserQuizDto {
    private String title;
    private String description;
    private String coverImage;
    private List<QuestionItemDto> questions;

    @Data
    public static class QuestionItemDto {
        private String content;       // Nội dung câu hỏi
        private String imageUrl;      // Link ảnh
        private Integer timeLimit;    // Thời gian (giây)
        private List<String> options; // 4 đáp án
        private String correctAnswer; // Đáp án đúng
    }
}