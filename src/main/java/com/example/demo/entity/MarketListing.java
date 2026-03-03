package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_listings") // Tên bảng trong DB
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người bán (Liên kết với bảng users)
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // Vật phẩm bán (Liên kết với bảng gacha_item)
    // Lưu ý: Chúng ta bán "loại vật phẩm", không phải bán dòng inventory cụ thể
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = true)
    private GachaItem item;

    @Column(nullable = false)
    private int price; // Giá bán (tính bằng Lúa)

    @Column(nullable = false)
    private int quantity; // Số lượng bán

    @Enumerated(EnumType.STRING)
    private ListingStatus status; // Trạng thái: ACTIVE, SOLD, CANCELLED

    private LocalDateTime listedAt; // Thời gian đăng

    // --- MỚI: Món hàng Topic (có thể null nếu bán Gacha) ---
    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = true)
    private Topic topic;

    @Column(nullable = false)
    private String listingType;
}