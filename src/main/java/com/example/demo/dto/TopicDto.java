package com.example.demo.dto;

import lombok.Data;

@Data
public class TopicDto {
    private Long id;
    private String name;
    private String description;
    private int questionCount;
    private boolean status;
}