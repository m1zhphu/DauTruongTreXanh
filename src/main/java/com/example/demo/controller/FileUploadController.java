package com.example.demo.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    // 1. Đường dẫn gốc (cho ảnh thường)
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
    
    // 2. Đường dẫn cho Radio (Phải khớp với RadioService)
    private final Path radioStorageLocation = Paths.get("uploads/radio").toAbsolutePath().normalize();

    public FileUploadController() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.radioStorageLocation); // Tạo luôn thư mục radio nếu chưa có
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục upload!", ex);
        }
    }

    // --- UPLOAD ẢNH (GIỮ NGUYÊN) ---
    @PostMapping("/image")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/upload/files/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok(Map.of("url", fileDownloadUri));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body("Lỗi upload file: " + ex.getMessage());
        }
    }

    // --- LẤY ẢNH THƯỜNG (GIỮ NGUYÊN) ---
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        // Logic cũ cho ảnh...
        return serveFile(this.fileStorageLocation, fileName, "image/jpeg");
    }

    // --- [QUAN TRỌNG] API MỚI DÀNH RIÊNG CHO RADIO STREAMING ---
    // Hứng đường dẫn: /api/upload/files/radio/{fileName}
    @GetMapping("/files/radio/{fileName:.+}")
    public ResponseEntity<Resource> downloadRadioFile(@PathVariable String fileName) {
        // 1. Xác định loại file để trả về Content-Type chuẩn
        String contentType = "application/octet-stream";
        if (fileName.endsWith(".mp4")) {
            contentType = "video/mp4"; // Quan trọng: Giúp trình duyệt hiểu đây là video stream
        } else if (fileName.endsWith(".mp3")) {
            contentType = "audio/mpeg";
        }
        
        // 2. Gọi hàm phục vụ file từ thư mục 'uploads/radio'
        return serveFile(this.radioStorageLocation, fileName, contentType);
    }

    // --- HÀM HỖ TRỢ DÙNG CHUNG (Refactor cho gọn) ---
    private ResponseEntity<Resource> serveFile(Path storagePath, String fileName, String defaultContentType) {
        try {
            Path filePath = storagePath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = defaultContentType;
                // Nếu không phải video/audio thì thử dò type
                if (!contentType.startsWith("video") && !contentType.startsWith("audio")) {
                    try {
                        String probedType = Files.probeContentType(filePath);
                        if (probedType != null) contentType = probedType;
                    } catch (IOException ex) { }
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        // Inline: Cho phép hiển thị/phát trên trình duyệt thay vì tải về
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}