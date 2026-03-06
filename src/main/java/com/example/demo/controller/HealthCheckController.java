package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/health")
    public ResponseEntity<String> healthCheck() {
        // Gửi một câu truy vấn vô thưởng vô phạt xuống Aiven để ép nó luôn thức
        jdbcTemplate.execute("SELECT 1");
        
        return ResponseEntity.ok("Backend and Database are both awake!");
    }
}