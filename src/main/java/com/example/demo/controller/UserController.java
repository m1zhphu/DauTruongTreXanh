package com.example.demo.controller;

import org.springframework.http.MediaType; // ✅ Nhớ import cái này
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // ✅ Import MultipartFile
import com.example.demo.dto.UserDto;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ==========================================
    // 🟢 PHẦN CỦA USER THƯỜNG
    // ==========================================
    
    // 1. Lấy thông tin bản thân
    @GetMapping("/users/me") 
    public ResponseEntity<UserDto> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.getUserDtoByUsername(username));
    }

    // 2. ✅ CẬP NHẬT THÔNG TIN BẢN THÂN (Thêm mới)
    // React gửi FormData nên phải dùng consumes = MULTIPART_FORM_DATA_VALUE
    @PutMapping(value = "/users/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateCurrentUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        // Lấy username người đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Gọi service xử lý (Lưu ảnh + Update DB)
        UserDto updatedUser = userService.updateUserProfile(username, name, email, avatar);
        
        return ResponseEntity.ok(updatedUser);
    }

    // ==========================================
    // 🔴 PHẦN CỦA ADMIN
    // ==========================================

    @GetMapping("/admin/users") 
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String role
    ) {
        return ResponseEntity.ok(userService.getAllUsersForAdmin(page, size, keyword, role));
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().body("{\"message\": \"Xóa người dùng thành công\"}");
    }
}