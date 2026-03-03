package com.example.demo.mapper;

import com.example.demo.dto.QuestionDto;
import com.example.demo.dto.TopicDto;
import com.example.demo.entity.Question;
import com.example.demo.entity.Topic;
import org.springframework.stereotype.Component;

@Component
public class AppMapper {

    // Chuyển từ Topic Entity -> Topic DTO
    public TopicDto toTopicResponse(Topic topic) {
        TopicDto dto = new TopicDto();
        dto.setId(topic.getId());
        dto.setName(topic.getName());
        dto.setDescription(topic.getDescription());
        dto.setQuestionCount(topic.getQuestions() != null ? topic.getQuestions().size() : 0);   
        dto.setStatus(true);
        return dto;
    }

    // Chuyển từ Question Entity -> Question DTO
    public QuestionDto toQuestionResponse(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setContent(question.getContent());
        dto.setOptions(question.getOptions());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        
        // --- LOGIC QUAN TRỌNG ĐÃ THÊM ---
        if (question.getTopic() != null) {
            dto.setTopicName(question.getTopic().getName());
            // Lấy ID của Topic để gửi về Frontend
            dto.setTopicId(question.getTopic().getId()); 
        }
        // --------------------------------
        
        return dto;
    }
}