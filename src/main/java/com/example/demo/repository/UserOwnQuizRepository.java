package com.example.demo.repository;

import com.example.demo.entity.GameHistory;
import com.example.demo.entity.UserOwnQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOwnQuizRepository extends JpaRepository<UserOwnQuiz, Long> {
    // Tìm tất cả bộ đề do User có ID này tạo ra
    List<UserOwnQuiz> findByOwner_Id(Long ownerId);
}