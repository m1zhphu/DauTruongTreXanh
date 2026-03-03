package com.example.demo.repository;

import com.example.demo.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    // Tìm lịch sử đấu theo Quiz ID, sắp xếp ngày chơi mới nhất
    List<GameHistory> findByQuiz_IdOrderByEndedAtDesc(Long quizId);
}