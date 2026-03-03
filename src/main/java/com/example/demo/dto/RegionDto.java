package com.example.demo.dto;

import lombok.Data;
import com.example.demo.entity.Region;

@Data
public class RegionDto {
    private Region region;
    private String status; // Status riêng của user này
}