package com.example.demo.controller;

import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.service.UserService;
import com.example.demo.security.CustomUserDetailsService;

// 1. Import thư viện Logger
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 2. Khởi tạo biến Logger
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(UserService userService, CustomUserDetailsService customUserDetailsService) {
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            logger.info("Đang xử lý đăng ký cho email: {}", request.getEmail()); // Log đăng ký
            String token = userService.register(request);
            return ResponseEntity.ok(Map.of(
                "message", "Đăng ký thành công!",
                "token", token
            ));
        } catch (RuntimeException e) {
            logger.error("Lỗi đăng ký: {}", e.getMessage()); // Log lỗi đăng ký
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 3. LOG DEBUG QUAN TRỌNG: In ra chính xác những gì điện thoại gửi lên
        // Dùng dấu ngoặc [] để phát hiện dấu cách thừa. Ví dụ: [admin ] là sai.
        logger.info("--------------------------------------------------");
        logger.info("LOGIN REQUEST RECEIVED:");
        logger.info("Username: [{}]", request.getUsername());
        logger.info("Password: [{}]", request.getPassword()); 
        // Lưu ý: Chỉ in password khi debug lỗi này, xong việc nên xóa dòng in password để bảo mật.
        logger.info("--------------------------------------------------");

        try {
            String token = userService.login(request);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());
            
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(item -> item.getAuthority())
                    .orElse("ROLE_USER");

            logger.info("Đăng nhập thành công cho User: {}", request.getUsername()); // Log thành công

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng nhập thành công!");
            response.put("token", token);
            response.put("role", role);
            response.put("username", userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // Log lỗi sai pass/user
            logger.warn("Đăng nhập thất bại (Sai thông tin) - User: [{}]", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sai tên đăng nhập hoặc mật khẩu!"));
        } catch (Exception e) {
            // Log lỗi hệ thống chi tiết (Full stack trace)
            logger.error("Lỗi hệ thống khi đăng nhập: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}