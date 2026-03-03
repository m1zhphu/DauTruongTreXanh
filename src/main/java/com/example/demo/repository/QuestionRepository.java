package com.example.demo.repository;

import com.example.demo.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 1) Cũ (có thể vẫn dùng ở chỗ admin)
    List<Question> findByTopicId(Long topicId);

    // 1b) ✅ MỚI: dùng cho realtime game (start/join) để tránh LazyInitializationException
    @Query("""
        select distinct q
        from Question q
        left join fetch q.options
        where q.topic.id = :topicId
    """)
    List<Question> findByTopicIdFetchOptions(@Param("topicId") Long topicId);

    // 2) Chủ đề duy nhất
    @Query("SELECT DISTINCT q.topic.name FROM Question q")
    List<String> findDistinctTopics();

    // 3) Tìm theo name topic
    List<Question> findByTopicNameContainingIgnoreCase(String topicName);

    // 4) Search admin
    @Query("SELECT q FROM Question q WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR q.content LIKE %:keyword%) AND " +
            "(:topicId IS NULL OR q.topic.id = :topicId)")
    Page<Question> searchQuestions(@Param("keyword") String keyword,
                                   @Param("topicId") Long topicId,
                                   Pageable pageable);

    int countByTopicId(Long topicId);


}
