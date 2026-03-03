package com.example.demo.dto;

import java.util.List;

public class N8nResponseWrapper {
    private List<QuestionResult> questions; // Tên biến phải trùng với key "questions" trong n8n JS
    private String updated_blacklist;
    private String debug_status;

    // Getter & Setter
    public List<QuestionResult> getQuestions() { return questions; }
    public void setQuestions(List<QuestionResult> questions) { this.questions = questions; }
    
    // Các getter/setter khác nếu cần log
}