package com.example.demo.controller;

import com.example.demo.entity.Region;
import com.example.demo.entity.User;
import com.example.demo.dto.RegionDto;
import com.example.demo.repository.UserRepository; // Import cái này
import com.example.demo.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/map") // Sửa lại path cho khớp với Frontend (/api/map)
@CrossOrigin("*")
public class RegionController {
    
    @Autowired 
    private RegionService regionService; // Sửa tên biến viết thường

    @Autowired
    private UserRepository userRepository; // Cần cái này để tìm ID user thật

    // Hàm lấy ID user từ Security Context (Token)
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Lấy username từ token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }

    // --- API ---

    @GetMapping("/my-journey")
    public List<RegionDto> getMyRegion() {
        Long userId = getCurrentUserId(); // Lấy ID thật, không dùng 1L nữa
        return regionService.getRegionForUser(userId);
    }

    @PostMapping("/complete/{regionId}")
    public ResponseEntity<?> completeRegion(@PathVariable String regionId) {
        Long userId = getCurrentUserId(); // Lấy ID thật
        regionService.completeRegion(userId, regionId);
        return ResponseEntity.ok("Unlocked next region!");
    }

    @PostMapping("/admin/region")
    public Region createRegion(@RequestBody Region region) {
        return regionService.createRegion(region);
    }

    // 1. Lấy danh sách tất cả vùng (cho Admin quản lý)
    @GetMapping("/admin/all")
    // @PreAuthorize("hasRole('ADMIN')") // Bật dòng này nếu muốn bảo mật chặt
    public List<Region> getAllRegionsForAdmin() {
        return regionService.getAllRegions();
    }

    // 3. Cập nhật (Sửa)
    @PutMapping("/admin/region/{id}")
    public Region updateRegion(@PathVariable String id, @RequestBody Region region) {
        return regionService.updateRegion(id, region);
    }

    // 4. Xóa
    @DeleteMapping("/admin/region/{id}")
    public ResponseEntity<?> deleteRegion(@PathVariable String id) {
        regionService.deleteRegion(id);
        return ResponseEntity.ok().build();
    }

    // 5. Lấy chi tiết 1 vùng (để hiển thị lên trang Edit)
    @GetMapping("/admin/region/{id}")
    public ResponseEntity<Region> getRegionDetail(@PathVariable String id) {
        return regionService.getRegionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}