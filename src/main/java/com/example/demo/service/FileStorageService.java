package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    // Thư mục gốc
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục upload gốc.", ex);
        }
    }

    // ✅ SỬA HÀM NÀY: Thêm tham số subFolder
    public String storeFile(MultipartFile file, String subFolder) {
        // 1. Chuẩn hóa tên file gốc
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        // 2. Tạo tên file mới (ngẫu nhiên) để tránh trùng lặp
        // Ví dụ: avatar.png -> 550e8400-e29b...png
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch(Exception e) {
            fileExtension = "";
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // 3. Tạo đường dẫn thư mục con: uploads/avatars/
            Path targetDir = this.fileStorageLocation.resolve(subFolder);
            
            // 4. Nếu thư mục con chưa có thì tạo mới
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 5. Copy file vào thư mục con
            Path targetLocation = targetDir.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 6. Trả về đường dẫn tương đối (để lưu vào DB)
            // Kết quả trả về dạng: "avatars/ten_file_moi.png"
            return subFolder + "/" + newFileName; 

        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + newFileName, ex);
        }
    }
}