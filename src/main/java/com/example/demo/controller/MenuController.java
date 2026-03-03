package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // <-- THÊM IMPORT
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping; // <-- THAY ĐỔI IMPORT
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MenuDto;
import com.example.demo.service.MenuService;

@RestController
@RequestMapping("/api/menus") // Đường dẫn gốc cho API menu
@CrossOrigin(origins = "*") // Cho phép gọi từ frontend khác domain (React)
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // API lấy tất cả menu (Public)
    @GetMapping
    public ResponseEntity<List<MenuDto>> getAllMenus() {
        List<MenuDto> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    // API lấy menu theo ID (Public)
    @GetMapping("/{id}")
    public ResponseEntity<MenuDto> getMenuById(@PathVariable Integer id) {
        MenuDto menuDto = menuService.getMenuById(id);
        return ResponseEntity.ok(menuDto);
    }

    // === CÁC API CHO ADMIN ===

    // API tạo menu mới (Admin)
    @PostMapping // <-- BẬT LÊN
    public ResponseEntity<MenuDto> createMenu(@RequestBody MenuDto menuDto) { // <-- BẬT LÊN
        MenuDto createdMenu = menuService.createMenu(menuDto); // <-- BẬT LÊN
        return new ResponseEntity<>(createdMenu, HttpStatus.CREATED); // <-- BẬT LÊN
    } // <-- BẬT LÊN

    // API cập nhật menu (Admin)
    @PutMapping("/{id}") // <-- BẬT LÊN
    public ResponseEntity<MenuDto> updateMenu(@PathVariable Integer id, @RequestBody MenuDto menuDto) { // <-- BẬT LÊN
        MenuDto updatedMenu = menuService.updateMenu(id, menuDto); // <-- BẬT LÊN
        return ResponseEntity.ok(updatedMenu); // <-- BẬT LÊN
    } // <-- BẬT LÊN

    // API xóa menu (Admin)
    @DeleteMapping("/{id}") // <-- BẬT LÊN
    public ResponseEntity<Void> deleteMenu(@PathVariable Integer id) { // <-- BẬT LÊN
        menuService.deleteMenu(id); // <-- BẬT LÊN
        return ResponseEntity.noContent().build(); // <-- BẬT LÊN
    } // <-- BẬT LÊN

    // (Bạn có thể thêm API lấy menu theo vị trí hoặc dạng cây ở đây)
}