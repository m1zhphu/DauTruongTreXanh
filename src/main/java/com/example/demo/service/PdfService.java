package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
@Service
public class PdfService {

    // Định nghĩa thư mục lưu file
    private final Path rootLocation = Paths.get("uploads");

    public PdfService() {
        // Tạo thư mục uploads nếu chưa có ngay khi khởi tạo Service
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ!", e);
        }
    }

    // Hàm 1: Lưu file vào ổ cứng (GIỮ NGUYÊN TÊN GỐC)
    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File rỗng!");
        }

        // Lấy tên gốc
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "unknown.pdf";

        // --- SỬA ĐỔI: KHÔNG thêm thời gian, giữ nguyên tên file gốc ---
        String fileName = originalFilename; 
        // -------------------------------------------------------------

        Path destinationFile = this.rootLocation.resolve(fileName).normalize().toAbsolutePath();

        // Kiểm tra bảo mật: Đảm bảo file nằm trong thư mục uploads
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new IOException("Không thể lưu file ra ngoài thư mục uploads.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Dùng REPLACE_EXISTING để ghi đè nếu file đã tồn tại (vì ta dùng tên gốc)
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("--> Đã lưu file (tên gốc): " + destinationFile.toString());
        return destinationFile.toString();
    }

    // Hàm 2: Đọc chữ từ file PDF (Giữ nguyên)
    public String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) return "";
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // Giới hạn ký tự để tránh quá tải
            return text.length() > 30000 ? text.substring(0, 30000) : text.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // 2. HÀM MỚI QUAN TRỌNG: Đọc text TỪ FILE TRÊN Ổ CỨNG (Không đọc từ MultipartFile nữa)
    public String extractTextFromFilePath(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return "";

        // Dùng try-with-resources để tự động đóng file, tránh bị lỗi "File Locked"
        try (PDDocument document = PDDocument.load(file)) {
            if (document.isEncrypted()) return "";
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            return text.length() > 30000 ? text.substring(0, 30000) : text.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}