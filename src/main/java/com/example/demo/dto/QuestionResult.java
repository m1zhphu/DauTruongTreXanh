package com.example.demo.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionResult {
    
    // Các trường này PHẢI TRÙNG TÊN với JSON mà n8n trả về
    private String question;
    private List<String> options;
    private String correct_answer; // Để nguyên snake_case cho khớp với JSON của AI
    private String explanation;
    
    // Trường này để hứng lỗi (Strict Mode)
    private String error; 

    private String original_content;
    // Constructor rỗng (Bắt buộc để Jackson phân giải JSON)
    public QuestionResult() {}

    // --- GETTER VÀ SETTER ---

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrect_answer() {
        return correct_answer;
    }

    public void setCorrect_answer(String correct_answer) {
        this.correct_answer = correct_answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}