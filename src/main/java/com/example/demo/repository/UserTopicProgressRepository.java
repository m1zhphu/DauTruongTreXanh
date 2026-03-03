package com.example.demo.repository;

import com.example.demo.entity.UserTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserTopicProgressRepository extends JpaRepository<UserTopicProgress, Long> {
    Optional<UserTopicProgress> findByUserIdAndTopicId(Long userId, Long topicId);
}