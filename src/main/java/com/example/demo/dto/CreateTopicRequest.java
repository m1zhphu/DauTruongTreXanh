package com.example.demo.dto;

import lombok.Data; // Nếu dùng Lombok
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data // Tự sinh Getter/Setter. Nếu không dùng Lombok thì tự viết Getter/Setter nhé
public class CreateTopicRequest {
    private MultipartFile file;
    private String title;
    private int numQuestions;
    private boolean isPublic; // Lưu ý: Lombok có thể sinh ra là getPublic(), cần check kỹ
    private String accessCode;
    private String password;
    
    // NẾU KHÔNG DÙNG LOMBOK, HÃY TỰ VIẾT GETTER/SETTER CHO TẤT CẢ CÁC TRƯỜNG TRÊN
    // Ví dụ:
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    // ... làm tương tự với các trường khác
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getNumQuestions() { return numQuestions; }
    public void setNumQuestions(int numQuestions) { this.numQuestions = numQuestions; }
    
    public boolean getIsPublic() { return isPublic; }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }
    
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Định dạng ISO (YYYY-MM-DDTHH:mm:ss) để khớp với input HTML5
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}