package com.example.demo.repository;

import com.example.demo.entity.GameEvent;
import com.example.demo.entity.GameEvent.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    @Query(value = """
        SELECT *
        FROM game_events
        WHERE status = 'LIVE'
          AND start_time IS NOT NULL
          AND start_time <= :now
          AND DATE_ADD(start_time, INTERVAL duration_minutes MINUTE) > :now
        ORDER BY start_time ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<GameEvent> findActiveLiveForBanner(@Param("now") LocalDateTime now);

    @Query(value = """
        SELECT *
        FROM game_events
        WHERE status = 'UPCOMING'
          AND start_time IS NOT NULL
          AND start_time > :now
        ORDER BY start_time ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<GameEvent> findNearestUpcomingFromNow(@Param("now") LocalDateTime now);

    @Query(value = """
        SELECT *
        FROM game_events
        WHERE status = 'LIVE'
          AND start_time IS NOT NULL
          AND DATE_ADD(start_time, INTERVAL duration_minutes MINUTE) <= :now
    """, nativeQuery = true)
    List<GameEvent> findExpiredLive(@Param("now") LocalDateTime now);

    Optional<GameEvent> findFirstByStatusOrderByStartTimeAsc(EventStatus status);

    List<GameEvent> findByStatusOrderByStartTimeAsc(EventStatus status);
    // ✅ FIX LỖI 500: Sử dụng Method Name chuẩn của Spring Data JPA (An toàn hơn @Query thủ công)
    // Tìm các event có Status là X và StartTime sau thời điểm Y
    List<GameEvent> findByStatusAndStartTimeAfterOrderByStartTimeAsc(EventStatus status, LocalDateTime startTime);
}