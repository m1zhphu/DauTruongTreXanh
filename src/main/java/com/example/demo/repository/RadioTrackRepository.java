package com.example.demo.repository;

import com.example.demo.entity.RadioTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RadioTrackRepository extends JpaRepository<RadioTrack, Long> {
    // Tìm các bài đang hoạt động (isActive = true)
    List<RadioTrack> findByIsActiveTrue();
}