package com.example.demo.repository;

import com.example.demo.entity.UserRegionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRegionProgressRepository extends JpaRepository<UserRegionProgress, Long> {
    
    // SỬA: Thêm dấu gạch dưới (_) để JPA hiểu là tìm theo ID bên trong object User
    Optional<UserRegionProgress> findByUser_IdAndRegion_Id(Long userId, String regionId);
    
    // SỬA: Tương tự, dùng findByUser_Id
    List<UserRegionProgress> findByUser_Id(Long userId);
}