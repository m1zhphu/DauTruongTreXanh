package com.example.demo.config;

import com.example.demo.entity.GachaItem;
import com.example.demo.repository.GachaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GachaSeeder implements CommandLineRunner {

    private final GachaItemRepository gachaItemRepository;

    // Đường dẫn thư mục uploads tương đối
    private final String UPLOAD_DIR_NAME = "uploads";

    @Override
    public void run(String... args) throws Exception {
        if (gachaItemRepository.count() == 0) {
            seedGachaItems();
        }
    }

    private void seedGachaItems() {
        System.out.println("--- Bat dau Seeder Gacha... ---");

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR_NAME);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Da tao thu muc: " + uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Khong the tao thu muc upload: " + e.getMessage());
            return;
        }

        List<GachaItemData> dataList = Arrays.asList(
            new GachaItemData("Skin Thanh Giong", "SKIN", "LEGENDARY", 5, 
                "https://cdn-icons-png.flaticon.com/512/3408/3408506.png", "thanh_giong.jpg"),
                
            new GachaItemData("Hat Thoc May Man", "CURRENCY", "COMMON", 50, 
                "https://cdn-icons-png.flaticon.com/512/2829/2829828.png", "hat_thoc.png"),
                
            new GachaItemData("The Bao Ho Streak", "PROTECTION", "RARE", 20, 
                "https://cdn-icons-png.flaticon.com/512/942/942748.png", "the_bao_ho.jpg"),
                
            new GachaItemData("The Doi Ten", "ITEM", "RARE", 15, 
                "https://cdn-icons-png.flaticon.com/512/1250/1250617.png", "the_doi_ten.jpg")
        );

        for (GachaItemData data : dataList) {
            String fileName = downloadImage(data.sourceUrl, data.fileName);

            if (fileName != null) {
                // Sử dụng dấu gạch chéo thông thường để tránh lỗi escape string
                String apiImageUrl = "/api/upload/gacha_items/" + fileName;

                GachaItem item = GachaItem.builder()
                        .name(data.name)
                        .type(data.type)
                        .rarity(data.rarity)
                        .dropWeight(data.dropWeight)
                        .imageUrl(apiImageUrl)
                        .build();

                gachaItemRepository.save(item);
                System.out.println("Da luu DB: " + data.name);
            }
        }

        System.out.println("--- Hoan tat Seeder Gacha! ---");
    }

    private String downloadImage(String imageUrl, String fileName) {
        try {
            Path destinationPath = Paths.get(UPLOAD_DIR_NAME).resolve(fileName);
            
            if (!Files.exists(destinationPath)) {
                // Thay thế new URL() bằng URI.create().toURL()
                try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
                    Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Da tai anh ve: " + destinationPath.toAbsolutePath());
                }
            } else {
                System.out.println("Anh da ton tai: " + fileName);
            }
            return fileName;
        } catch (IOException e) {
            System.err.println("Loi tai anh " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    @lombok.AllArgsConstructor
    static class GachaItemData {
        String name;
        String type;
        String rarity;
        int dropWeight;
        String sourceUrl;
        String fileName;
    }
}