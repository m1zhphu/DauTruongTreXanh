package com.example.demo.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.entity.Menu;
import com.example.demo.repository.MenuRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MenuSeeder implements CommandLineRunner {

    private final MenuRepository menuRepository;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra nếu chưa có menu nào thì mới tạo
        if (menuRepository.count() == 0) {
            seedMenus();
        }
    }

    private void seedMenus() {
        // --- CẤU HÌNH MENU CHÍNH ---
        // Vị trí: 'main_menu'

        // 1. Trang chủ
        Menu home = new Menu(null, "Trang Chủ", "/app", 1, null, "main_menu", true);

        // 2. Học tập (Trang danh sách chủ đề)
        Menu learn = new Menu(null, "Học Tập", "/topic", 2, null, "main_menu", true);

        // 3. Bảng vàng
        Menu rank = new Menu(null, "Bảng Vàng", "/leaderboard", 3, null, "main_menu", true);

        // 4. Thành tích
        Menu achievement = new Menu(null, "Thành Tích", "/achievements", 4, null, "main_menu", true);
        
        // 5. Tạo lớp học (Dẫn đến trang Teacher Studio)
        Menu createClass = new Menu(null, "Tạo Lớp Học", "/topic/create", 5, null, "main_menu", true);

        // 5. Tạo lớp học (Dẫn đến trang Teacher Studio)
        Menu events = new Menu(null, "Sự Kiện", "/events", 5, null, "main_menu", true);

        Menu radio = new Menu(null, "Đài Phát Thanh", "/radio", 6, null, "main_menu", true);
        // Lưu tất cả vào database
        menuRepository.saveAll(List.of(home, learn, rank, achievement, createClass, events, radio));

        System.out.println("--- Đã khởi tạo 6 Menu: Trang chủ, Học tập, Bảng vàng, Thành tích, Tạo lớp, Tham gia lớp ---");
    }
}