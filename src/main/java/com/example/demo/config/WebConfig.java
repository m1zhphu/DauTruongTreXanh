package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình: Khi truy cập URL /api/upload/files/** // -> Sẽ tìm trong thư mục uploads nằm ở thư mục gốc dự án
        registry.addResourceHandler("/api/upload/files/**")
                .addResourceLocations("file:uploads/");
    }
}