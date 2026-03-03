package com.example.demo.controller;

import com.example.demo.entity.GachaItem;
import com.example.demo.service.GachaService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GachaController {

    // ✅ CHỈ GỌI SERVICE, KHÔNG GỌI REPOSITORY TRỰC TIẾP
    @Autowired private GachaService gachaService;

    // ==========================================
    // 🟢 PHẦN DÀNH CHO USER (Người chơi)
    // ==========================================

    @GetMapping("/gacha/pool")
    public ResponseEntity<?> getPool() {
        return ResponseEntity.ok(gachaService.getGachaPool());
    }

    @GetMapping("/gacha/inventory")
    public ResponseEntity<?> getInventory(Principal principal) {
        try {
            // Logic tìm User đã được đẩy xuống Service
            return ResponseEntity.ok(gachaService.getUserInventoryByUsername(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/gacha/balance")
    public ResponseEntity<?> getBalance(Principal principal) {
        try {
            // Logic map dữ liệu đã được đẩy xuống Service
            return ResponseEntity.ok(gachaService.getUserBalance(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/gacha/spin")
    public ResponseEntity<?> spinWheel(
            Principal principal,
            @RequestParam(defaultValue = "false") boolean useQuanTien
    ) {
        try {
            // Service tự xử lý tìm user và trừ tiền
            GachaItem reward = gachaService.spinGacha(principal.getName(), useQuanTien);
            return ResponseEntity.ok(reward);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 🔴 PHẦN DÀNH CHO ADMIN (Quản lý)
    // ==========================================

    @GetMapping("/admin/gacha/items")
    public ResponseEntity<List<GachaItem>> getAllItemsForAdmin() {
        return ResponseEntity.ok(gachaService.getGachaPool());
    }

    @PostMapping(value = "/admin/gacha/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItem(
            @RequestPart("item") String itemJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            // Controller chỉ parse JSON
            ObjectMapper mapper = new ObjectMapper();
            GachaItem item = mapper.readValue(itemJson, GachaItem.class);
            
            // Service xử lý lưu file và DB
            GachaItem savedItem = gachaService.createGachaItem(item, file);
            return ResponseEntity.ok(savedItem);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping(value = "/admin/gacha/items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestPart("item") String itemJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            // Controller chỉ parse JSON
            ObjectMapper mapper = new ObjectMapper();
            GachaItem itemDetails = mapper.readValue(itemJson, GachaItem.class);

            // ✅ Service xử lý update (DB + File)
            GachaItem updatedItem = gachaService.updateGachaItem(id, itemDetails, file);
            
            return ResponseEntity.ok(updatedItem);

        } catch (RuntimeException e) {
            // Lỗi nghiệp vụ (VD: Item ko tồn tại) -> 404
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            // Lỗi hệ thống -> 500
            return ResponseEntity.status(500).body("Lỗi update: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/gacha/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            // Service xử lý xóa an toàn
            gachaService.deleteGachaItemSafely(id);
            return ResponseEntity.ok().body("{\"message\": \"Đã xóa vật phẩm thành công\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xóa: " + e.getMessage());
        }
    }
}