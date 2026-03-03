package com.example.demo.mapper;

import com.example.demo.dto.MenuDto;
import com.example.demo.entity.Menu;

public class MenuMapper {

    // Chuyển Entity sang DTO
    public static MenuDto toDto(Menu menu) {
        if (menu == null) return null;
        return new MenuDto(
                menu.getId(),
                menu.getName(),
                menu.getLink(),
                menu.getDisplayOrder(),
                menu.getParentId(),
                menu.getPosition(),
                menu.isStatus()
        );
    }

    // Chuyển DTO sang Entity (khi tạo mới hoặc cập nhật)
    public static Menu toEntity(MenuDto dto) {
        if (dto == null) return null;
        Menu menu = new Menu();
        // Không set ID khi toEntity từ DTO (ID thường do DB tự tạo hoặc lấy từ URL)
        menu.setName(dto.getName());
        menu.setLink(dto.getLink());
        menu.setDisplayOrder(dto.getDisplayOrder());
        menu.setParentId(dto.getParentId());
        menu.setPosition(dto.getPosition());
        menu.setStatus(dto.isStatus());
        return menu;
    }
}