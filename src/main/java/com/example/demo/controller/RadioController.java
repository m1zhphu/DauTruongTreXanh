package com.example.demo.controller;

import com.example.demo.entity.ContentType; // ✅ Đã import
import com.example.demo.entity.RadioTrack;
import com.example.demo.service.RadioService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api") 
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService; 

    // --- CLIENT ---
    @GetMapping("/radio")
    public ResponseEntity<List<RadioTrack>> getAllTracks() {
        return ResponseEntity.ok(radioService.getAllActiveTracks());
    }

    // --- ADMIN ---

    @GetMapping("/radio/admin/all") 
    public ResponseEntity<List<RadioTrack>> getAllTracksAdmin() {
        return ResponseEntity.ok(radioService.getAllTracksForAdmin());
    }

    @GetMapping("/radio/{id}")
    public ResponseEntity<RadioTrack> getTrackById(@PathVariable Long id) {
        return radioService.getTrackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // API Tạo mới: Ủy quyền toàn bộ logic cho Service
    @PostMapping(value = "/radio", consumes = { "multipart/form-data" })
    public ResponseEntity<?> createTrack(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("type") ContentType type,
            @RequestParam(value = "audioUrl", required = false) String audioUrl,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive
    ) {
        try {
            RadioTrack newTrack = radioService.createTrack(title, author, description, type, audioUrl, file, isActive);
            return ResponseEntity.ok(newTrack);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi upload file: " + e.getMessage());
        }
    }

    // 3. Cập nhật (Sửa lại để hỗ trợ Upload File giống Create)
    @PutMapping(value = "/radio/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<?> updateTrack(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("type") ContentType type,
            @RequestParam(value = "audioUrl", required = false) String audioUrl, // Link text (hoặc link cũ)
            @RequestParam(value = "file", required = false) MultipartFile file,  // File mới (nếu có)
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive
    ) {
        try {
            // Gọi Service xử lý logic
            RadioTrack updatedTrack = radioService.updateTrack(id, title, author, description, type, audioUrl, file, isActive);
            
            if (updatedTrack != null) {
                return ResponseEntity.ok(updatedTrack);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý file: " + e.getMessage());
        }
    }

    @DeleteMapping("/radio/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        boolean deleted = radioService.deleteTrack(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}