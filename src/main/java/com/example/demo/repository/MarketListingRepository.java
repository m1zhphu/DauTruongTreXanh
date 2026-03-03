package com.example.demo.repository;

import com.example.demo.entity.MarketListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {
    
    // Lấy danh sách đang bán, sắp xếp mới nhất lên đầu
    @Query("SELECT m FROM MarketListing m WHERE m.status = 'ACTIVE' ORDER BY m.listedAt DESC")
    List<MarketListing> findAllActiveListings();
    
    // Lấy danh sách vật phẩm người dùng đang bán (để hiển thị trong trang quản lý của họ)
    List<MarketListing> findBySellerId(Long sellerId);
}