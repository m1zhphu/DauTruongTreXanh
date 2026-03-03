package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menus")
@Data // Lombok: Tự tạo getter, setter, toString...
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name; // Tên menu (vd: "Trang chủ", "Sản phẩm")

    @Column(length = 255)
    private String link; // Đường dẫn URL (vd: "/", "/products", "/contact")

    @Column(name = "display_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int displayOrder; // Thứ tự hiển thị

    // Quan hệ cha-con (để tạo menu đa cấp)
    @Column(name = "parent_id")
    private Integer parentId; // ID của menu cha (null nếu là menu cấp 1)

    // (Optional) Vị trí hiển thị menu (vd: 'main_menu', 'footer_menu')
    @Column(name = "menu_position", length = 50)
    private String position;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private boolean status = true; // Trạng thái (hiển thị/ẩn)

    // Bạn có thể thêm các trường khác nếu cần
}