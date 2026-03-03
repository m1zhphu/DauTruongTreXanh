package com.example.demo.service;

import com.example.demo.dto.CreateEventRequest;
import com.example.demo.dto.QuestionDto;
import com.example.demo.entity.EventParticipant;
import com.example.demo.entity.GameEvent;
import com.example.demo.entity.Question;
import com.example.demo.entity.Topic;
import com.example.demo.entity.User;
import com.example.demo.entity.GameEvent.EventStatus;
import com.example.demo.repository.EventParticipantRepository;
import com.example.demo.repository.GameEventRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.TopicRepository;
import com.example.demo.repository.UserRepository;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// ✅ Thêm import này
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class LiveBattleService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private GameEventRepository gameEventRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;

    @Autowired private EventParticipantRepository eventParticipantRepository;
    @Autowired private UserRepository userRepository;
    
    // ✅ Autowire TransactionTemplate
    @Autowired private TransactionTemplate transactionTemplate;

    // ===================== RAM =====================
    private final Map<Long, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> userConnections = new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> aliveUsers = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> pendingUsers = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, String>> roundAnswers = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> eliminatedUsers = new ConcurrentHashMap<>();

    private final Map<Long, Object> eventLocks = new ConcurrentHashMap<>();
    private Object lockOf(Long eventId) { return eventLocks.computeIfAbsent(eventId, k -> new Object()); }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<Long, ScheduledFuture<?>> roundTimers = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdown() {
        try { scheduler.shutdownNow(); } catch (Exception ignored) {}
    }

    // =====================
    // DB HELPERS
    // =====================
    private User requireUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
    }

    private GameEvent requireEvent(Long eventId) {
        return gameEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event không tồn tại: " + eventId));
    }

    public GameEvent getEventById(Long id) {
        return gameEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện: " + id));
    }

    private EventParticipant upsertParticipant(Long eventId, String username) {
        GameEvent event = requireEvent(eventId);
        User user = requireUserByUsername(username);

        return eventParticipantRepository
                .findByEvent_IdAndUser_Id(eventId, user.getId())
                .orElseGet(() -> EventParticipant.builder()
                        .event(event)
                        .user(user)
                        .status(EventParticipant.ParticipantStatus.JOINED)
                        .score(0)
                        .rankPosition(0) 
                        .currentRound(0)
                        .lastQuestionId(null)
                        .lastAnswer(null)
                        .joinedAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
                );
    }

    private void sendUser(String username, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(username, "/queue/messages", payload);
    }

    private void broadcast(Long eventId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/event/" + eventId, payload);
    }

    private void broadcastParticipantCount(Long eventId) {
        long count = userConnections.values().stream().filter(id -> id.equals(eventId)).count();
        broadcast(eventId, Map.of("type", "PARTICIPANT_UPDATE", "count", count));
    }

    private int safeInt(Integer v, int def) {
        if (v == null) return def;
        if (v <= 0) return def;
        return v;
    }

    // =====================
    // JOIN / LEAVE
    // =====================
    @Transactional
    public void handleUserJoin(String username, Long eventId) {
        if (username == null || eventId == null) return;

        GameEvent event = gameEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            sendUser(username, Map.of("type", "ERROR", "message", "Sự kiện không tồn tại!"));
            return;
        }

        if (event.getStatus() == EventStatus.ENDED) {
            sendUser(username, Map.of("type", "ERROR", "message", "Sự kiện đã kết thúc!"));
            return;
        }

        var existing = eventParticipantRepository.findByEvent_IdAndUser_Username(eventId, username);
        if (existing.isPresent() && existing.get().getStatus() == EventParticipant.ParticipantStatus.ELIMINATED) {
            eliminatedUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(username);
            sendUser(username, Map.of("type", "ELIMINATED", "message", "Bạn đã bị loại ở sự kiện này!"));
            return;
        }

        int max = safeInt(event.getMaxParticipants(), 0);
        long currentCount = userConnections.values().stream().filter(id -> id.equals(eventId)).count();
        if (max > 0 && currentCount >= max) {
            sendUser(username, Map.of("type", "ERROR", "message", "Sự kiện đã đủ số người tham gia!"));
            return;
        }

        userConnections.put(username, eventId);
        broadcastParticipantCount(eventId);

        EventParticipant p = upsertParticipant(eventId, username);
        if (p.getStatus() == EventParticipant.ParticipantStatus.ELIMINATED) {
            eliminatedUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(username);
            sendUser(username, Map.of("type", "ELIMINATED", "message", "Bạn đã bị loại ở sự kiện này!"));
            return;
        }
        
        p.setStatus(EventParticipant.ParticipantStatus.ALIVE);
        p.setUpdatedAt(LocalDateTime.now());
        eventParticipantRepository.save(p);

        ensureLiveSessionIfNeeded(eventId);

        GameSession session = activeSessions.get(eventId);
        
        aliveUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(username);

        if (session != null && session.getCurrentQuestionIndex() >= 0) {
            QuestionDto currentQ = session.getQuestions().get(session.getCurrentQuestionIndex());
            int questionSec = safeInt(event.getQuestionSeconds(), 15);

            sendUser(username, Map.of(
                    "type", "NEW_QUESTION",
                    "content", currentQ.getContent(),
                    "options", currentQ.getOptions(),
                    "timeLeft", questionSec,
                    "round", session.getCurrentQuestionIndex() + 1
            ));
        } else {
            sendUser(username, Map.of("type", "WAITING", "message", "Đang chờ admin bắt đầu..."));
        }
    }

    @Transactional
    public void handleUserLeave(String username) {
        Long eventId = userConnections.remove(username);
        if (eventId == null) return;

        var alive = aliveUsers.get(eventId);
        if (alive != null) alive.remove(username);

        var pending = pendingUsers.get(eventId);
        if (pending != null) pending.remove(username);

        var answers = roundAnswers.get(eventId);
        if (answers != null) answers.remove(username);

        var opt = eventParticipantRepository.findByEvent_IdAndUser_Username(eventId, username);
        if (opt.isPresent()) {
            EventParticipant p = opt.get();
            if (p.getStatus() != EventParticipant.ParticipantStatus.ELIMINATED) {
                p.setStatus(EventParticipant.ParticipantStatus.LEFT);
                p.setUpdatedAt(LocalDateTime.now());
                eventParticipantRepository.save(p);
            }
        }

        broadcastParticipantCount(eventId);
    }

    // =====================
    // REST API
    // =====================
    public List<GameEvent> getAllEvents() {
        return gameEventRepository.findAll(Sort.by(Sort.Direction.DESC, "startTime"));
    }

    public GameEvent getNearestUpcomingEvent() {
        return gameEventRepository.findFirstByStatusOrderByStartTimeAsc(EventStatus.UPCOMING).orElse(null);
    }

    public List<GameEvent> getUpcomingEvents() {
        return gameEventRepository.findByStatusOrderByStartTimeAsc(EventStatus.UPCOMING);
    }

    @Transactional
    public GameEvent createGameEvent(CreateEventRequest req) {
        Topic topic = topicRepository.findById(req.getTopicId())
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại"));

        GameEvent event = new GameEvent();
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setStatus(EventStatus.UPCOMING);
        event.setTopic(topic);

        if (req.getAccessCode() != null && !req.getAccessCode().isBlank()) {
            event.setAccessCode(req.getAccessCode().trim());
        }

        event.setMaxParticipants(req.getMaxParticipants() != null ? req.getMaxParticipants() : 50);
        event.setDurationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes() : 30);
        event.setLobbyOpenMinutes(req.getLobbyOpenMinutes() != null ? req.getLobbyOpenMinutes() : 5);
        event.setQuestionSeconds(req.getQuestionSeconds() != null ? req.getQuestionSeconds() : 15);
        event.setResultSeconds(req.getResultSeconds() != null ? req.getResultSeconds() : 5);

        return gameEventRepository.save(event);
    }

    @Transactional
    public GameEvent updateGameEvent(Long id, CreateEventRequest req) {
        GameEvent event = getEventById(id);

        Topic topic = topicRepository.findById(req.getTopicId())
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại"));

        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setTopic(topic);

        if (req.getStatus() != null) {
            event.setStatus(req.getStatus());
        }
        
        if (req.getAccessCode() != null) {
            event.setAccessCode(req.getAccessCode().trim());
        }

        if (req.getMaxParticipants() != null) event.setMaxParticipants(req.getMaxParticipants());
        if (req.getDurationMinutes() != null) event.setDurationMinutes(req.getDurationMinutes());
        if (req.getLobbyOpenMinutes() != null) event.setLobbyOpenMinutes(req.getLobbyOpenMinutes());
        if (req.getQuestionSeconds() != null) event.setQuestionSeconds(req.getQuestionSeconds());
        if (req.getResultSeconds() != null) event.setResultSeconds(req.getResultSeconds());

        return gameEventRepository.save(event);
    }

    // =====================
    // START EVENT
    // =====================
    @Transactional
    public void startEvent(Long eventId) {
        GameEvent event = requireEvent(eventId);

        if (event.getTopic() == null) throw new RuntimeException("Sự kiện chưa chọn chủ đề!");

        List<Question> dbQuestions = questionRepository.findByTopicIdFetchOptions(event.getTopic().getId());
        if (dbQuestions == null || dbQuestions.isEmpty())
            throw new RuntimeException("Chủ đề chưa có câu hỏi!");

        event.setStatus(EventStatus.LIVE);
        gameEventRepository.save(event);

        synchronized (lockOf(eventId)) {
            GameSession session = buildSession(eventId, dbQuestions);
            activeSessions.put(eventId, session);

            aliveUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
            pendingUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
            eliminatedUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
            roundAnswers.remove(eventId);
        }

        broadcast(eventId, Map.of("type", "START_GAME"));
        startNextRound(eventId);
    }

    // =====================
    // RESTORE LIVE SESSION
    // =====================
    private void ensureLiveSessionIfNeeded(Long eventId) {
        if (activeSessions.containsKey(eventId)) return;

        GameEvent event = gameEventRepository.findById(eventId).orElse(null);
        if (event == null) return;
        if (event.getStatus() != EventStatus.LIVE) return;
        if (event.getTopic() == null) return;

        synchronized (lockOf(eventId)) {
            if (activeSessions.containsKey(eventId)) return;

            List<Question> dbQuestions = questionRepository.findByTopicIdFetchOptions(event.getTopic().getId());
            if (dbQuestions == null || dbQuestions.isEmpty()) return;

            GameSession session = buildSession(eventId, dbQuestions);
            activeSessions.put(eventId, session);

            aliveUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
            pendingUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
            eliminatedUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());

            broadcast(eventId, Map.of("type", "START_GAME"));
            startNextRound(eventId);
        }
    }

    private GameSession buildSession(Long eventId, List<Question> dbQuestions) {
        GameSession session = new GameSession(eventId);

        List<QuestionDto> questions = dbQuestions.stream().map(q -> {
            QuestionDto dto = new QuestionDto();
            dto.setId(q.getId());
            dto.setContent(q.getContent());
            dto.setOptions(new ArrayList<>(q.getOptions()));
            dto.setCorrectAnswer(q.getCorrectAnswer());
            return dto;
        }).collect(Collectors.toList());

        Collections.shuffle(questions);
        session.setQuestions(questions);
        return session;
    }

    // =====================
    // ROUND FLOW
    // =====================
    private void startNextRound(Long eventId) {
        GameSession session = activeSessions.get(eventId);
        if (session == null) return;

        // ✅ Dùng transactionTemplate để đảm bảo truy cập DB trong luồng scheduler
        GameEvent event = transactionTemplate.execute(status -> gameEventRepository.findById(eventId).orElse(null));
        if (event == null) return;

        LocalDateTime endTime = event.getStartTime().plusMinutes(event.getDurationMinutes());
        if (LocalDateTime.now().isAfter(endTime)) {
            endGame(eventId);
            return;
        }

        int questionSec = safeInt(event.getQuestionSeconds(), 15);
        int resultSec = safeInt(event.getResultSeconds(), 5);

        Set<String> alive = aliveUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
        Set<String> pending = pendingUsers.get(eventId);
        if (pending != null && !pending.isEmpty()) {
            alive.addAll(pending);
            pending.clear();
        }

        QuestionDto q = session.getNextQuestion();
        if (q == null) {
            endGame(eventId);
            return;
        }

        roundAnswers.remove(eventId);
        int roundNo = session.getCurrentQuestionIndex() + 1;

        for (String username : alive) {
            // ✅ Dùng transactionTemplate
            transactionTemplate.execute(status -> {
                EventParticipant p = upsertParticipant(eventId, username);
                if (p.getStatus() != EventParticipant.ParticipantStatus.ELIMINATED) {
                    p.setStatus(EventParticipant.ParticipantStatus.ALIVE);
                    p.setCurrentRound(roundNo);
                    p.setLastQuestionId(q.getId());
                    p.setLastAnswer(null);
                    p.setUpdatedAt(LocalDateTime.now());
                    eventParticipantRepository.save(p);
                }
                return null;
            });
        }

        for (String username : alive) {
            sendUser(username, Map.of(
                    "type", "NEW_QUESTION",
                    "content", q.getContent(),
                    "options", q.getOptions(),
                    "timeLeft", questionSec,
                    "round", roundNo
            ));
        }

        ScheduledFuture<?> old = roundTimers.remove(eventId);
        if (old != null) old.cancel(true);

        ScheduledFuture<?> f = scheduler.schedule(() -> {
            try {
                // ✅ Dùng transactionTemplate
                transactionTemplate.execute(status -> {
                    calculateRoundResult(eventId, q, resultSec);
                    return null;
                });
            } catch (Exception ignored) {}
        }, questionSec, TimeUnit.SECONDS);

        roundTimers.put(eventId, f);
    }

    private void calculateRoundResult(Long eventId, QuestionDto q, int resultSec) {
        Set<String> alive = aliveUsers.getOrDefault(eventId, ConcurrentHashMap.newKeySet());
        Map<String, String> answers = roundAnswers.getOrDefault(eventId, Map.of());

        Set<String> eliminated = new HashSet<>();

        // Chuẩn hóa đáp án đúng (cắt khoảng trắng, để chắc chắn)
        String correctAnsStandardized = q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "";

        for (String username : new HashSet<>(alive)) {
            String userAns = answers.get(username);
            
            // ✅ SỬA LỖI Ở ĐÂY: Chuẩn hóa đáp án người dùng trước khi so sánh
            String userAnsStandardized = userAns != null ? userAns.trim() : "";

            // So sánh không phân biệt hoa thường và khoảng trắng thừa
            boolean correct = userAnsStandardized.equalsIgnoreCase(correctAnsStandardized);

            EventParticipant p = upsertParticipant(eventId, username);

            if (correct) {
                p.setScore(p.getScore() + 10); // Cộng 10 điểm
                p.setStatus(EventParticipant.ParticipantStatus.ALIVE);
            } else {
                eliminated.add(username);
                p.setStatus(EventParticipant.ParticipantStatus.ELIMINATED);
                p.setEliminatedAt(LocalDateTime.now());
                
                // Gửi thông báo ELIMINATED kèm đáp án đúng
                sendUser(username, Map.of(
                    "type", "ELIMINATED", 
                    "message", "Sai rồi! Đáp án đúng là: " + q.getCorrectAnswer()
                ));
            }
            p.setLastAnswer(userAns);
            p.setUpdatedAt(LocalDateTime.now());
            eventParticipantRepository.save(p);
        }

        if (!eliminated.isEmpty()) {
            alive.removeAll(eliminated);
            eliminatedUsers.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).addAll(eliminated);
        }

        // ✅ Lấy leaderboard
        List<Map<String, Object>> leaderboard = eventParticipantRepository.findByEvent_Id(eventId).stream()
                .filter(p -> p.getStatus() == EventParticipant.ParticipantStatus.ALIVE)
                .sorted((p1, p2) -> p2.getScore() - p1.getScore())
                .limit(5)
                .map(this::mapParticipantToDto)
                .collect(Collectors.toList());

        broadcast(eventId, Map.of(
                "type", "ROUND_RESULT",
                "correctAnswer", q.getCorrectAnswer(),
                "survivors", alive.size(),
                "leaderboard", leaderboard
        ));

        // ✅ Kiểm tra người chiến thắng
        if (alive.size() <= 1) {
             scheduler.schedule(() -> {
                transactionTemplate.execute(status -> {
                    endGame(eventId);
                    return null;
                });
            }, Math.max(2, resultSec), TimeUnit.SECONDS);
        } else {
            scheduler.schedule(() -> startNextRound(eventId), Math.max(1, resultSec), TimeUnit.SECONDS);
        }
    }

    // ✅ Hàm này chạy trong transactionTemplate
    private void endGame(Long eventId) {
        ScheduledFuture<?> old = roundTimers.remove(eventId);
        if (old != null) old.cancel(true);

        GameEvent event = gameEventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.setStatus(EventStatus.ENDED);
            gameEventRepository.save(event);
        }

        Map<String, Object> winnerInfo = null;
        List<Map<String, Object>> finalLeaderboard = new ArrayList<>();

        try {
            List<EventParticipant> ps = eventParticipantRepository.findByEvent_IdOrderByScoreDescUpdatedAtAsc(eventId);

            if (!ps.isEmpty()) {
                int maxScore = ps.get(0).getScore();
                for (EventParticipant p : ps) {
                    if (p.getScore() == maxScore) {
                        p.setStatus(EventParticipant.ParticipantStatus.WINNER);
                        p.setUpdatedAt(LocalDateTime.now());
                        eventParticipantRepository.save(p);
                        
                        // Lấy người đầu tiên làm winner đại diện
                        if(winnerInfo == null) {
                             winnerInfo = mapParticipantToDto(p);
                        }
                    } else {
                        break; 
                    }
                }
                
                finalLeaderboard = ps.stream().limit(10).map(this::mapParticipantToDto).collect(Collectors.toList());
            }
        } catch (Exception ignored) {}

        broadcast(eventId, Map.of(
            "type", "GAME_OVER", 
            "result", "Game đã kết thúc",
            "winner", winnerInfo != null ? winnerInfo : "null",
            "leaderboard", finalLeaderboard
        ));

        activeSessions.remove(eventId);
        roundAnswers.remove(eventId);
        aliveUsers.remove(eventId);
        pendingUsers.remove(eventId);
        eliminatedUsers.remove(eventId);
        eventLocks.remove(eventId);
    }

    @Transactional
    public void processUserAnswer(Long eventId, String username, String answer) {
        if (eventId == null || username == null) return;

        if (eliminatedUsers.getOrDefault(eventId, Set.of()).contains(username)) return;
        if (!aliveUsers.getOrDefault(eventId, Set.of()).contains(username)) return;

        roundAnswers.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(username, answer);

        EventParticipant p = upsertParticipant(eventId, username);
        if (p.getStatus() != EventParticipant.ParticipantStatus.ELIMINATED) {
            p.setLastAnswer(answer);
            p.setUpdatedAt(LocalDateTime.now());
            eventParticipantRepository.save(p);
        }
    }

    @Scheduled(fixedDelay = 60_000) 
    public void autoEndExpiredLiveEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<GameEvent> expiredLives = gameEventRepository.findExpiredLive(now);

        for (GameEvent e : expiredLives) {
            try { 
                transactionTemplate.execute(status -> {
                    endGame(e.getId());
                    return null;
                });
            } catch (Exception ignored) {}
        }
    }
    
    private Map<String, Object> mapParticipantToDto(EventParticipant p) {
        Map<String, Object> map = new HashMap<>();
        // Hibernate.initialize(p.getUser()); // Nếu cần thiết
        map.put("username", p.getUser().getUsername());
        map.put("name", p.getUser().getName() != null ? p.getUser().getName() : p.getUser().getUsername());
        map.put("score", p.getScore());
        map.put("avatar", p.getUser().getAvatarUrl());
        return map;
    }
}

class GameSession {
    private final Long eventId;
    private int currentQuestionIndex = -1;
    private List<QuestionDto> questions = new ArrayList<>();

    public GameSession(Long eventId) { this.eventId = eventId; }

    public void setQuestions(List<QuestionDto> questions) { this.questions = questions; }
    public List<QuestionDto> getQuestions() { return this.questions; }

    public QuestionDto getNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) return null;
        return questions.get(currentQuestionIndex);
    }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
}