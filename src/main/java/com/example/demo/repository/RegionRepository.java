package com.example.demo.repository;

import com.example.demo.entity.UserRegionProgress;
import com.example.demo.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {
    List<Region> findByRequiredId(String requiredId);
    Optional<Region> findByTopicId(Long topicId);
}