package com.example.demo.repository;

import com.example.demo.entity.UserTopicAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserTopicAccessRepository extends JpaRepository<UserTopicAccess, Long> {
    // Kiểm tra xem user đã mua topic này chưa
    boolean existsByUserIdAndTopicId(Long userId, Long topicId);
    
    // Lấy danh sách các topic đã mua của user
    List<UserTopicAccess> findByUserId(Long userId);
}