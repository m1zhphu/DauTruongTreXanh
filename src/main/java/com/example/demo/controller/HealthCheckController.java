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

    // SỬA ĐƯỜNG DẪN Ở ĐÂY
    @GetMapping({"/api/public/health", "/api/public/health/"}) 
    public ResponseEntity<String> healthCheck() {
        jdbcTemplate.execute("SELECT 1");
        return ResponseEntity.ok("Backend and Database are both awake!");
    }
}