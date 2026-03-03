package com.example.demo.repository;

import com.example.demo.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional; // Quan trọng: Thêm import này

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
       // --- MỚI: Tìm các chủ đề do user X tạo ---
    List<Topic> findByCreator_Id(Long userId);
    
    // Tìm topic theo tên (Giữ nguyên)
    Topic findByName(String name);

    // MỚI: Chỉ lấy danh sách các chủ đề CÔNG KHAI (isPublic = true)
    List<Topic> findByIsPublicTrue();

    // MỚI: Tìm chủ đề khớp cả Mã Code và Mật khẩu (Dùng cho chức năng Join)
    Optional<Topic> findByAccessCodeAndPassword(String accessCode, String password);

    // --- MỚI: Hàm tìm kiếm nâng cao cho Admin ---
    // Tìm theo nội dung câu hỏi VÀ (nếu có chọn topic thì lọc theo topic)
    @Query("SELECT q FROM Question q WHERE " +
           "(:keyword IS NULL OR q.content LIKE %:keyword%) AND " +
           "(:topicId IS NULL OR q.topic.id = :topicId)")
    Page<Question> searchQuestions(@Param("keyword") String keyword, 
                                   @Param("topicId") Long topicId, 
                                   Pageable pageable);
}