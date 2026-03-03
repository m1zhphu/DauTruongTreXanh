package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.dto.RegionDto;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RegionService {
    @Autowired
    private RegionRepository regionRepo;
    @Autowired
    private UserRegionProgressRepository progressRepo;
    @Autowired
    private UserRepository userRepo;

    public List<RegionDto> getRegionForUser(Long userId) {
        List<Region> allRegions = regionRepo.findAll();
        List<RegionDto> dtos = new ArrayList<>();

        for (Region r : allRegions) {
            RegionDto dto = new RegionDto();
            dto.setRegion(r);

            // SỬA: Gọi hàm có dấu gạch dưới
            Optional<UserRegionProgress> progress = progressRepo.findByUser_IdAndRegion_Id(userId, r.getId());

            if (progress.isPresent()) {
                dto.setStatus(progress.get().getStatus());
            } else {
                // Logic mặc định: Nếu không yêu cầu vùng nào -> Unlocked
                if (r.getRequiredId() == null || r.getRequiredId().isEmpty()) {
                    dto.setStatus("unlocked");
                } else {
                    dto.setStatus("locked");
                }
            }
            dtos.add(dto);
        }
        return dtos;
    }

    public Region createRegion(Region region) {
        return regionRepo.save(region);
    }

    @Transactional
    public void completeRegion(Long userId, String regionId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")); // Bắt lỗi nếu user null
        Region region = regionRepo.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));

        // 1. Lưu trạng thái completed
        // SỬA: Gọi hàm có dấu gạch dưới
        UserRegionProgress up = progressRepo.findByUser_IdAndRegion_Id(userId, regionId)
                .orElse(new UserRegionProgress());
        up.setUser(user);
        up.setRegion(region);
        up.setStatus("completed");
        up.setCompletedAt(LocalDateTime.now());
        progressRepo.save(up);

        // 2. Mở khóa vùng tiếp theo
        List<Region> nextRegions = regionRepo.findByRequiredId(regionId);

        for (Region next : nextRegions) {
            Optional<UserRegionProgress> nextProgressOpt = progressRepo.findByUser_IdAndRegion_Id(userId, next.getId());

            if (nextProgressOpt.isEmpty()) {
                // Trường hợp 1: Chưa có record -> Tạo mới status unlocked
                UserRegionProgress nextUp = new UserRegionProgress();
                nextUp.setUser(user);
                nextUp.setRegion(next);
                nextUp.setStatus("unlocked");
                progressRepo.save(nextUp);
            } else {
                // Trường hợp 2: Đã có record nhưng đang bị locked -> Update thành unlocked
                UserRegionProgress existingProgress = nextProgressOpt.get();
                if ("locked".equals(existingProgress.getStatus())) {
                    existingProgress.setStatus("unlocked");
                    progressRepo.save(existingProgress);
                }
            }
        }
    }

    public List<Region> getAllRegions() {
        return regionRepo.findAll();
    }

    public Region updateRegion(String id, Region newData) {
        return regionRepo.findById(id).map(region -> {
            region.setName(newData.getName());
            region.setDescription(newData.getDescription()); // Đây chính là "Chủ đề"
            region.setTopPos(newData.getTopPos());
            region.setLeftPos(newData.getLeftPos());
            region.setColor(newData.getColor());
            region.setRequiredId(newData.getRequiredId());
            region.setIsland(newData.isIsland());
            region.setTopicId(newData.getTopicId());
            return regionRepo.save(region);
        }).orElseThrow(() -> new RuntimeException("Region not found"));
    }

    public void deleteRegion(String id) {
        // Xóa cả tiến trình học của user liên quan đến vùng này trước (nếu cần)
        // progressRepo.deleteByRegionId(id); // Cần viết thêm hàm này trong Repo nếu
        // muốn xóa sạch
        regionRepo.deleteById(id);
    }

    public Optional<Region> getRegionById(String id) {
        return regionRepo.findById(id);
    }
}