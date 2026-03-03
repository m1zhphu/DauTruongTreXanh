package com.example.demo.dto;

import lombok.Data;

@Data
public class SellRequestDTO {
    private Long itemId;
    private int quantity;
    private int price;
    private String otp;
    private Long topicId;
}