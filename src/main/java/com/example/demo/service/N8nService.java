package com.example.demo.service;

import com.example.demo.dto.QuestionResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class N8nService {

    // Link chính thức (Production URL)
    private final String N8N_URL = "http://localhost:5678/webhook/tao-cau-hoi";

    // --- CLASS PHỤ ĐỂ HỨNG JSON TỪ N8N (Chứa key "questions") ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class N8nResponseWrapper {
        private List<QuestionResult> questions;
        
        public List<QuestionResult> getQuestions() {
            return questions;
        }
        public void setQuestions(List<QuestionResult> questions) {
            this.questions = questions;
        }
    }
    // -------------------------------------------------------------

    // Đổi kiểu trả về thành List<QuestionResult>
    public List<QuestionResult> generateQuestions(String topic, String content, int quantity) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 1. Đóng gói dữ liệu gửi đi
            Map<String, Object> body = new HashMap<>();
            body.put("topic", topic);
            body.put("content", content);
            body.put("quantity", quantity);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // 2. Gọi n8n và nhận về chuỗi JSON thô
            String jsonResponse = restTemplate.postForObject(N8N_URL, request, String.class);
            
            // Log kiểm tra nếu cần
            System.out.println("JSON Response from N8n: " + jsonResponse);

            // 3. Map vào Wrapper trước để lấy danh sách
            if (jsonResponse != null) {
                N8nResponseWrapper wrapper = mapper.readValue(jsonResponse, N8nResponseWrapper.class);
                if (wrapper != null && wrapper.getQuestions() != null) {
                    return wrapper.getQuestions();
                }
            }
            
            return Collections.emptyList();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi khi gọi n8n: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}