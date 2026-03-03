package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.EventParticipant;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    Optional<EventParticipant> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    Optional<EventParticipant> findByEvent_IdAndUser_Username(Long eventId, String username);

    long countByEvent_Id(Long eventId);

    List<EventParticipant> findByEvent_IdOrderByScoreDescUpdatedAtAsc(Long eventId);

    // ✅ Thêm dòng này để lấy danh sách người tham gia theo Event ID
    List<EventParticipant> findByEvent_Id(Long eventId);
}
