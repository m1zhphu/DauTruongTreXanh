package com.example.demo.service;

import com.example.demo.entity.ContentType;
import com.example.demo.entity.RadioTrack;
import com.example.demo.repository.RadioTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RadioService {

    private final RadioTrackRepository radioTrackRepository;
    
    // Thư mục vật lý vẫn là "uploads/radio/"
    private final Path uploadPath = Paths.get("uploads/radio/");

    // --- READ ---
    public List<RadioTrack> getAllActiveTracks() {
        return radioTrackRepository.findByIsActiveTrue();
    }

    public List<RadioTrack> getAllTracksForAdmin() {
        return radioTrackRepository.findAll();
    }

    public Optional<RadioTrack> getTrackById(Long id) {
        return radioTrackRepository.findById(id);
    }

    // --- CREATE ---
    public RadioTrack createTrack(String title, String author, String description, 
                                  ContentType type, String audioUrl, 
                                  MultipartFile file, Boolean isActive) throws IOException {
        String finalUrl = audioUrl;

        if (file != null && !file.isEmpty()) {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            // Lưu file vật lý vào ổ cứng
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            
            // ✅ SỬA QUAN TRỌNG: Lưu đường dẫn URL khớp với WebConfig (/api/upload/files/...)
            finalUrl = "/api/upload/files/radio/" + fileName;
        }

        RadioTrack track = RadioTrack.builder()
                .title(title)
                .author(author)
                .description(description)
                .type(type)
                .audioUrl(finalUrl)
                .isActive(isActive != null ? isActive : true)
                .build();

        return radioTrackRepository.save(track);
    }

    // --- UPDATE ---
    public RadioTrack updateTrack(Long id, String title, String author, String description, 
                                  ContentType type, String audioUrl, 
                                  MultipartFile file, Boolean isActive) throws IOException {
        
        return radioTrackRepository.findById(id).map(track -> {
            track.setTitle(title);
            track.setAuthor(author);
            track.setDescription(description);
            track.setType(type);
            track.setIsActive(isActive);

            String finalUrl = audioUrl;

            if (file != null && !file.isEmpty()) {
                try {
                    if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                    
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    
                    // ✅ SỬA QUAN TRỌNG: URL khớp WebConfig
                    finalUrl = "/api/upload/files/radio/" + fileName;
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi lưu file", e);
                }
            }

            if (finalUrl != null && !finalUrl.isEmpty()) {
                track.setAudioUrl(finalUrl);
            }

            return radioTrackRepository.save(track);
        }).orElse(null);
    }

    public boolean deleteTrack(Long id) {
        if (radioTrackRepository.existsById(id)) {
            radioTrackRepository.deleteById(id);
            return true;
        }
        return false;
    }
}