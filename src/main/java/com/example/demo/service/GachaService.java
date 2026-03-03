package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class GachaService {

    @Autowired private GachaItemRepository gachaItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserInventoryRepository inventoryRepository;
    @Autowired private FileStorageService fileStorageService;

    // Cấu hình giá
    private final int PRICE_RICE = 100;
    private final int PRICE_QUAN_TIEN = 10;

    // --- READ ---

    public List<GachaItem> getGachaPool() {
        return gachaItemRepository.findAll();
    }

    public List<UserInventory> getUserInventory(Long userId) {
        return inventoryRepository.findByUserId(userId);
    }
    
    // ✅ MỚI: Hàm hỗ trợ lấy Inventory qua Username (để Controller không cần gọi UserRepository)
    public List<UserInventory> getUserInventoryByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inventoryRepository.findByUserId(user.getId());
    }

    // ✅ MỚI: Hàm lấy số dư (Controller không cần biết logic lấy User)
    public Map<String, Object> getUserBalance(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return Map.of(
            "rice", user.getRice() == null ? 0 : user.getRice(),
            "quanTien", user.getQuanTien() == null ? 0 : user.getQuanTien()
        );
    }

    // --- ACTIONS ---

    @Transactional
    public GachaItem spinGacha(String username, boolean useQuanTien) {
        // Tìm user theo username thay vì bắt Controller tìm ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. TRỪ TIỀN
        if (useQuanTien) {
            if (user.getQuanTien() < PRICE_QUAN_TIEN) {
                throw new RuntimeException("Không đủ Quan Tiền! Cần " + PRICE_QUAN_TIEN);
            }
            user.setQuanTien(user.getQuanTien() - PRICE_QUAN_TIEN);
        } else {
            if (user.getRice() < PRICE_RICE) {
                throw new RuntimeException("Không đủ Lúa! Cần " + PRICE_RICE);
            }
            user.setRice(user.getRice() - PRICE_RICE);
        }
        userRepository.save(user);

        // 2. XÁC ĐỊNH ĐỘ HIẾM
        String selectedRarity = determineRarity(useQuanTien);

        // 3. LẤY LIST ITEM
        List<GachaItem> poolByRarity = gachaItemRepository.findByRarity(selectedRarity);
        if (poolByRarity.isEmpty()) {
            poolByRarity = gachaItemRepository.findAll();
        }
        if (poolByRarity.isEmpty()) throw new RuntimeException("Kho Gacha đang trống!");

        // 4. RANDOM
        int randomIndex = new Random().nextInt(poolByRarity.size());
        GachaItem selectedItem = poolByRarity.get(randomIndex);

        // 5. LƯU VÀO TÚI ĐỒ
        UserInventory inventory = inventoryRepository.findByUserIdAndItemId(user.getId(), selectedItem.getId())
                .orElse(UserInventory.builder()
                        .user(user)
                        .item(selectedItem)
                        .quantity(0)
                        .build());
        
        inventory.setQuantity(inventory.getQuantity() + 1);
        inventory.setAcquiredAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        return selectedItem;
    }

    // --- ADMIN MANAGEMENT ---

    @Transactional
    public GachaItem createGachaItem(GachaItem item, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            String relativePath = fileStorageService.storeFile(file, "gacha_items"); 
            item.setImageUrl("/api/upload/files/" + relativePath);
        }
        return gachaItemRepository.save(item);
    }

    // ✅ MỚI: Logic Update chuyển hết vào đây
    @Transactional
    public GachaItem updateGachaItem(Long id, GachaItem itemDetails, MultipartFile file) {
        GachaItem item = gachaItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại với ID: " + id));

        // Cập nhật thông tin
        item.setName(itemDetails.getName());
        item.setType(itemDetails.getType());
        item.setRarity(itemDetails.getRarity());
        item.setDropWeight(itemDetails.getDropWeight());

        // Cập nhật ảnh nếu có file mới
        if (file != null && !file.isEmpty()) {
            String relativePath = fileStorageService.storeFile(file, "gacha_items");
            item.setImageUrl("/api/upload/files/" + relativePath);
        }
        // Nếu file null thì giữ nguyên ảnh cũ

        return gachaItemRepository.save(item);
    }

    @Transactional
    public void deleteGachaItemSafely(Long itemId) {
        // Xóa trong Túi đồ người dùng trước
        inventoryRepository.deleteByItemId(itemId); 
        // Xóa Item gốc
        gachaItemRepository.deleteById(itemId);
    }

    // --- PRIVATE HELPERS ---
    private String determineRarity(boolean isVip) {
        int random = new Random().nextInt(100) + 1;
        if (isVip) {
            if (random <= 20) return "LEGENDARY";
            if (random <= 70) return "RARE";
            return "COMMON";
        } else {
            if (random <= 1) return "LEGENDARY";
            if (random <= 20) return "RARE";
            return "COMMON";
        }
    }
}