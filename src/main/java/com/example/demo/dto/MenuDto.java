package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuDto {
    private Integer id;
    private String name;
    private String link;
    private int displayOrder;
    private Integer parentId;
    private String position;
    private boolean status;

    // (Bạn có thể thêm các trường khác nếu cần, ví dụ danh sách menu con)
    // private List<MenuDto> children;
}