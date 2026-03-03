package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.entity.AuthProvider;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Component
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Quản Trị Viên")
                    .email("admin@caytretramdot.com")
                    .role("ROLE_ADMIN")
                    .authProvider(AuthProvider.LOCAL)
                    .totalXp(9999L) // Admin cho max điểm để test
                    .currentStreak(100)
                    .avatarUrl("admin_avatar.png")
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Đã tạo tài khoản Admin mặc định: admin / admin123");
        }
    }
}