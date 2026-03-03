package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long topicId;

    @JsonProperty("correct") 
    private boolean isCorrect;
}