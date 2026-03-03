package com.example.demo.service;

import com.example.demo.dto.SellRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketService {

    @Autowired private MarketListingRepository marketRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private UserInventoryRepository inventoryRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private OtpService otpService; // ✅ Inject OtpService vào đây
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private UserTopicAccessRepository accessRepository;

    // --- 1. HỖ TRỢ NGHIỆP VỤ OTP ---
    
    public void requestOtpForSelling(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("Tài khoản chưa cập nhật Email!");
        }

        otpService.generateAndSendOtp(username, user.getEmail());
    }

    // --- 2. CHỨC NĂNG RAO BÁN (SELL) ---

    // Hàm Wrapper: Kiểm tra OTP trước khi bán
    @Transactional
    public MarketListing sellItemWithOtp(String username, SellRequestDTO request) {
        // Kiểm tra OTP ngay tại Service
        if (!otpService.validateOtp(username, request.getOtp())) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        // Nếu OTP đúng, tiến hành logic bán hàng
        return sellItem(username, request.getItemId(), request.getQuantity(), request.getPrice());
    }

    // Logic bán hàng cốt lõi (private hoặc public tùy nhu cầu tái sử dụng)
    private MarketListing sellItem(String username, Long itemId, int quantity, int price) {
        User seller = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserInventory inventory = inventoryRepo.findByUserIdAndItemId(seller.getId(), itemId)
                .orElseThrow(() -> new RuntimeException("Bạn không sở hữu vật phẩm này"));

        if (inventory.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng trong kho không đủ để bán!");
        }

        // Trừ đồ
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepo.save(inventory);

        // Tạo lệnh bán
        MarketListing listing = MarketListing.builder()
                .seller(seller)
                .item(inventory.getItem())
                .quantity(quantity)
                .price(price)
                .status(ListingStatus.ACTIVE)
                .listedAt(LocalDateTime.now())
                .build();

        MarketListing savedListing = marketRepo.save(listing);

        // Bắn thông báo
        messagingTemplate.convertAndSend("/topic/market", savedListing);

        return savedListing;
    }

    // --- 3. CHỨC NĂNG MUA HÀNG (BUY) ---
    @Transactional
    public void buyItem(String buyerUsername, Long listingId) {
        // 1. Kiểm tra Listing
        MarketListing listing = marketRepo.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Tin rao không tồn tại"));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new RuntimeException("Vật phẩm này đã được bán hoặc bị hủy!");
        }

        // 2. Lấy thông tin người mua - người bán
        User buyer = userRepo.findByUsername(buyerUsername).orElseThrow();
        User seller = listing.getSeller();

        if (buyer.getId().equals(seller.getId())) {
            throw new RuntimeException("Không thể tự mua hàng của chính mình!");
        }

        // 3. Kiểm tra tiền (Quan Tiền)
        if (buyer.getQuanTien() < listing.getPrice()) {
            throw new RuntimeException("Bạn không đủ Quan Tiền để mua!");
        }

        // 4. TRỪ TIỀN - CỘNG TIỀN
        buyer.setQuanTien(buyer.getQuanTien() - listing.getPrice());
        seller.setQuanTien(seller.getQuanTien() + listing.getPrice());

        // 5. GIAO HÀNG (Xử lý tùy theo loại hàng)
        if ("GACHA".equals(listing.getListingType())) {
            // --- TRƯỜNG HỢP 1: MUA GACHA ITEM (Lưu vào kho) ---
            if (listing.getItem() == null) throw new RuntimeException("Lỗi dữ liệu: Item null");

            UserInventory buyerInventory = inventoryRepo.findByUserIdAndItemId(buyer.getId(), listing.getItem().getId())
                    .orElse(UserInventory.builder()
                            .user(buyer)
                            .item(listing.getItem())
                            .quantity(0)
                            .acquiredAt(LocalDateTime.now())
                            .build());
            
            buyerInventory.setQuantity(buyerInventory.getQuantity() + listing.getQuantity());
            inventoryRepo.save(buyerInventory);

        } else if ("TOPIC".equals(listing.getListingType())) {
            // --- TRƯỜNG HỢP 2: MUA TOPIC (Cấp quyền truy cập) ---
            if (listing.getTopic() == null) throw new RuntimeException("Lỗi dữ liệu: Topic null");

            // Kiểm tra xem người mua đã sở hữu topic này chưa (để tránh trừ tiền oan)
            if (accessRepository.existsByUserIdAndTopicId(buyer.getId(), listing.getTopic().getId())) {
                 throw new RuntimeException("Bạn đã sở hữu bộ đề này rồi! Vui lòng kiểm tra thư viện.");
            }

            // Tạo vé "Cấp quyền" (Không clone dữ liệu)
            UserTopicAccess access = UserTopicAccess.builder()
                    .user(buyer)
                    .topic(listing.getTopic())
                    .purchasedAt(LocalDateTime.now())
                    .build();
            
            accessRepository.save(access);
        }

        // 6. Cập nhật trạng thái Listing
        listing.setStatus(ListingStatus.SOLD);

        // 7. Lưu tất cả thay đổi
        userRepo.save(buyer);
        userRepo.save(seller);
        marketRepo.save(listing);

        // 8. Bắn socket thông báo realtime
        messagingTemplate.convertAndSend("/topic/market/sold", listing.getId());
    }

    /**
     * Hàm hỗ trợ: Copy toàn bộ Topic và các câu hỏi bên trong sang cho người mua
     */
    private void cloneTopicForBuyer(Topic originalTopic, User buyer) {
        // 1. Sao chép Topic
        Topic newTopic = new Topic();
        // Thêm hậu tố (Copy) hoặc (Bought) để tránh trùng tên unique
        newTopic.setName(originalTopic.getName() + " - " + buyer.getUsername() + " (Mua)"); 
        newTopic.setDescription(originalTopic.getDescription());
        newTopic.setTotalKnots(originalTopic.getTotalKnots());
        newTopic.setFilePath(originalTopic.getFilePath()); // Dùng lại file PDF cũ
        newTopic.setPublic(false); // Mặc định mua về là riêng tư
        newTopic.setStatus(true);
        newTopic.setCreator(buyer); // ✅ QUAN TRỌNG: Chuyển quyền sở hữu cho người mua
        
        // Lưu Topic mới trước để có ID
        newTopic = topicRepository.save(newTopic);

        // 2. Sao chép các câu hỏi (Questions)
        List<Question> originalQuestions = questionRepository.findByTopicId(originalTopic.getId());
        
        for (Question q : originalQuestions) {
            Question newQ = new Question();
            newQ.setTopic(newTopic); // Gán vào topic mới
            newQ.setContent(q.getContent());
            newQ.setOptions(new ArrayList<>(q.getOptions())); // Copy list options
            newQ.setCorrectAnswer(q.getCorrectAnswer());
            newQ.setExplanation(q.getExplanation());
            
            questionRepository.save(newQ);
        }
    }

    public List<MarketListing> getAllListings() {
        return marketRepo.findAllActiveListings();
    }

    // --- MỚI: ĐĂNG BÁN TOPIC ---
    @Transactional
    public MarketListing sellTopic(String username, Long topicId, int price) {
        User seller = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Kiểm tra quyền sở hữu: Chỉ người tạo mới được bán
        if (topic.getCreator() == null || !topic.getCreator().getId().equals(seller.getId())) {
            throw new RuntimeException("Bạn không phải người tạo ra Topic này!");
        }

        // Tạo lệnh bán
        MarketListing listing = MarketListing.builder()
                .seller(seller)
                .item(null) // Không bán item
                .topic(topic) // Bán topic
                .listingType("TOPIC") // Đánh dấu là bán Topic
                .quantity(1)
                .price(price)
                .status(ListingStatus.ACTIVE)
                .listedAt(LocalDateTime.now())
                .build();

        return marketRepo.save(listing);
    }
    // --- MỚI: HÀM WRAPPER ĐỂ CHECK OTP KHI BÁN TOPIC ---
    @Transactional
    public MarketListing sellTopicWithOtp(String username, SellRequestDTO request) {
        // 1. Kiểm tra OTP
        if (!otpService.validateOtp(username, request.getOtp())) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        // 2. Gọi logic bán topic gốc
        return sellTopic(username, request.getTopicId(), request.getPrice());
    }
}