package com.example.demo.controller;

import com.example.demo.dto.SellRequestDTO;
import com.example.demo.entity.MarketListing;
import com.example.demo.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    // ✅ Chỉ gọi Service chính, không gọi Repository hay Service phụ
    @Autowired private MarketService marketService;

    // Lấy danh sách chợ
    @GetMapping
    public ResponseEntity<List<MarketListing>> getMarket() {
        return ResponseEntity.ok(marketService.getAllListings());
    }

    // Mua hàng
    @PostMapping("/buy/{listingId}")
    public ResponseEntity<?> buyItem(@PathVariable Long listingId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            marketService.buyItem(username, listingId);
            return ResponseEntity.ok("Mua thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 1. API Yêu cầu OTP
    @PostMapping("/sell/request-otp")
    public ResponseEntity<?> requestOtp(Principal principal) {
        try {
            // Service tự lo việc tìm user và gửi mail
            marketService.requestOtpForSelling(principal.getName());
            return ResponseEntity.ok("Đã gửi OTP");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. API Bán hàng
    @PostMapping("/sell")
    public ResponseEntity<?> sellItem(
            Principal principal, 
            @RequestBody SellRequestDTO request
    ) {
        try {
            // Service tự lo việc check OTP và tạo giao dịch
            marketService.sellItemWithOtp(principal.getName(), request);
            return ResponseEntity.ok("Đăng bán thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sell-topic")
    public ResponseEntity<?> sellTopic(
            Principal principal,
            @RequestBody SellRequestDTO request
    ) {
        try {
            // Gọi hàm service mới thêm để check OTP và bán Topic
            marketService.sellTopicWithOtp(principal.getName(), request);
            return ResponseEntity.ok("Đăng bán bộ đề thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}