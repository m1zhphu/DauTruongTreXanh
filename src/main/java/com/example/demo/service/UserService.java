package com.example.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.AuthProvider;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException; 
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final FileStorageService fileStorageService;
    // Helper: Chuyển Entity -> DTO
    private UserDto convertToUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatarUrl(),
            user.getTotalXp(),
            user.getCurrentStreak(),
            user.getRice()
        );
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // Tạo User mới (Gọn nhẹ)
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .role("ROLE_USER")
                .authProvider(AuthProvider.LOCAL)
                .totalXp(0L)
                .currentStreak(0)
                .avatarUrl("default_avatar.png") // Avatar mặc định
                .build();

        userRepository.save(user);
        return jwtService.generateToken(user.getUsername(), user.getRole());
    }

    public String login(LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng!"));
        
        return jwtService.generateToken(user.getUsername(), user.getRole());
    }

    @Transactional(readOnly = true)
    public UserDto getUserDtoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return convertToUserDto(user);
    }

    public Page<User> getAllUsersForAdmin(int page, int size, String keyword, String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        
        // ✅ Xử lý null trước khi truyền vào Repository
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
        String searchRole = (role == null || role.trim().isEmpty()) ? "" : role.trim();

        return userRepository.searchUsers(searchKeyword, searchRole, pageable);
    }
    
    // Các hàm khác (delete, update...)
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public UserDto updateUserProfile(String username, String name, String email, MultipartFile avatar) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(name);
        user.setEmail(email);

        if (avatar != null && !avatar.isEmpty()) {
            // ✅ GỌI HÀM MỚI VỚI THAM SỐ "avatars"
            // File sẽ được lưu vào: uploads/avatars/xyz.png
            String relativePath = fileStorageService.storeFile(avatar, "avatars"); 
            
            // Lưu vào DB đường dẫn đầy đủ để Frontend gọi
            // Kết quả trong DB: /api/upload/files/avatars/xyz.png
            user.setAvatarUrl("/api/upload/files/" + relativePath); 
        }

        User savedUser = userRepository.save(user);
        return mapToUserDto(savedUser);
    }

    // Hàm phụ trợ convert User -> UserDto (Nếu bạn chưa có thì thêm vào)
    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAvatarUrl(user.getAvatarUrl());
        // ... set các trường khác nếu cần
        return dto;
    }
}