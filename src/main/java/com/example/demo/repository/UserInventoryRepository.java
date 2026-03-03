package com.example.demo.repository;

import com.example.demo.entity.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {
    // Tìm xem user đã có item này chưa
    Optional<UserInventory> findByUserIdAndItemId(Long userId, Long itemId);
    
    // Lấy toàn bộ túi đồ của user
    List<UserInventory> findByUserId(Long userId);
    // ✅ Thêm hàm xóa theo Item ID
    @Modifying
    @Transactional
    @Query("DELETE FROM UserInventory u WHERE u.item.id = :itemId")
    void deleteByItemId(Long itemId);
}