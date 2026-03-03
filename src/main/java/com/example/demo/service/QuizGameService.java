package com.example.demo.service;

import com.example.demo.entity.GameHistory;
import com.example.demo.entity.GamePlayerResult;
import com.example.demo.entity.User; // Import User
import com.example.demo.entity.UserOwnQuestion;
import com.example.demo.entity.UserOwnQuiz;
import com.example.demo.repository.GameHistoryRepository;
import com.example.demo.repository.UserOwnQuizRepository;
import com.example.demo.repository.UserRepository; // Import UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class QuizGameService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private UserOwnQuizRepository quizRepository;
    @Autowired private GameHistoryRepository historyRepository;
    @Autowired private UserRepository userRepository;

    // Quản lý luồng thời gian
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    // Lưu trữ timer của từng phòng
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();
    // Lưu trữ danh sách các phòng đang hoạt động
    private final Map<String, RoomData> activeRooms = new ConcurrentHashMap<>();

    /**
     * 1. Tạo phòng mới
     */
    public String createRoom(Long quizId) {
        UserOwnQuiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ câu hỏi với ID: " + quizId));

        String roomCode = generateRoomCode();
        
        RoomData room = new RoomData();
        room.quiz = quiz;
        room.status = "LOBBY"; 
        room.currentQuestionIndex = -1;
        room.players = new ConcurrentHashMap<>();
        room.scores = new ConcurrentHashMap<>();
        
        activeRooms.put(roomCode, room);
        System.out.println("✅ [KAHOOT] Đã tạo phòng mới: " + roomCode);
        return roomCode;
    }

    /**
     * 2. Người chơi tham gia
     */
    public void joinRoom(String roomCode, String username, String avatar) {
        RoomData room = activeRooms.get(roomCode);
        if (room == null || !room.status.equals("LOBBY")) {
            System.err.println("❌ [KAHOOT] Lỗi Join: Phòng không tồn tại hoặc đang chơi.");
            return;
        }

        String finalName = username;
        int count = 1;
        while (room.players.containsKey(finalName)) {
            finalName = username + count++;
        }

        room.players.put(finalName, avatar != null ? avatar : "😊");
        room.scores.put(finalName, 0); 
        
        broadcast(roomCode, "PLAYER_JOINED", room.players);
    }

    /**
     * 3. Bắt đầu game
     */
    public void startGame(String roomCode) {
        RoomData room = activeRooms.get(roomCode);
        if (room != null) {
            room.status = "PLAYING";
            room.currentQuestionIndex = -1;
            nextQuestion(roomCode);
        }
    }

    // 4. Next Question
    public void nextQuestion(String roomCode) {
        RoomData room = activeRooms.get(roomCode);
        if (room == null) return;

        ScheduledFuture<?> oldTimer = roomTimers.remove(roomCode);
        if (oldTimer != null) oldTimer.cancel(false);

        room.currentQuestionIndex++;

        if (room.currentQuestionIndex >= room.quiz.getQuestions().size()) {
            endGame(roomCode);
            return;
        }

        UserOwnQuestion q = room.quiz.getQuestions().get(room.currentQuestionIndex);
        room.questionStartTime = System.currentTimeMillis();
        room.status = "PLAYING";

        int timeLimit = (q.getTimeLimit() > 0) ? q.getTimeLimit() : 15;

        Map<String, Object> payload = new HashMap<>();
        payload.put("index", room.currentQuestionIndex + 1);
        payload.put("total", room.quiz.getQuestions().size());
        payload.put("text", q.getText());
        payload.put("image", q.getImageUrl());
        payload.put("time", timeLimit);
        payload.put("options", q.getOptions());
        
        broadcast(roomCode, "NEW_QUESTION", payload);

        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            showRoundResult(roomCode);
        }, timeLimit, TimeUnit.SECONDS); 

        roomTimers.put(roomCode, timer);
    }

    /**
     * 5. Nhận câu trả lời
     */
    public void submitAnswer(String roomCode, String username, String answer) {
        RoomData room = activeRooms.get(roomCode);
        if (room == null || !room.status.equals("PLAYING")) return;

        UserOwnQuestion currentQ = room.quiz.getQuestions().get(room.currentQuestionIndex);
        
        if (currentQ.getCorrectAnswer() != null && 
            currentQ.getCorrectAnswer().trim().equalsIgnoreCase(answer.trim())) {
            
            long timeElapsedMillis = System.currentTimeMillis() - room.questionStartTime;
            int timeLimit = (currentQ.getTimeLimit() > 0) ? currentQ.getTimeLimit() : 15;
            double totalTimeMillis = timeLimit * 1000.0;
            
            double ratio = timeElapsedMillis / totalTimeMillis;
            if (ratio > 1.0) ratio = 1.0; 

            int score = (int) (1000 * (1 - (ratio / 2))); 
            
            int currentTotalScore = room.scores.getOrDefault(username, 0);
            room.scores.put(username, currentTotalScore + score);
        } 
    }
    
    /**
     * 6. Hiện kết quả vòng đấu
     */
    public void showRoundResult(String roomCode) {
         RoomData room = activeRooms.get(roomCode);
         if(room != null) {
             ScheduledFuture<?> timer = roomTimers.remove(roomCode);
             if (timer != null) timer.cancel(false);

             room.status = "RESULT";
             UserOwnQuestion q = room.quiz.getQuestions().get(room.currentQuestionIndex);
             
             Map<String, Object> payload = new HashMap<>();
             payload.put("correctAnswer", q.getCorrectAnswer());
             payload.put("leaderboard", getLeaderboard(room));
             
             broadcast(roomCode, "ROUND_RESULT", payload);
         }
    }

    /**
     * 7. Kết thúc Game
     */
    private void endGame(String roomCode) {
        RoomData room = activeRooms.get(roomCode);
        if (room != null) {
            ScheduledFuture<?> timer = roomTimers.remove(roomCode);
            if (timer != null) timer.cancel(false);

            room.status = "FINISHED";
            
            List<Map<String, Object>> leaderboard = getLeaderboard(room);
            broadcast(roomCode, "GAME_OVER", leaderboard);
            
            saveToDatabase(room, roomCode, leaderboard);

            activeRooms.remove(roomCode);
        }
    }

    // ✅ Đã đưa hàm này vào bên trong class QuizGameService
    private void saveToDatabase(RoomData room, String roomCode, List<Map<String, Object>> leaderboard) {
        try {
            System.out.println("💾 Đang lưu lịch sử vào Database...");

            GameHistory history = new GameHistory();
            history.setRoomCode(roomCode);
            history.setEndedAt(LocalDateTime.now());
            history.setQuiz(room.quiz);

            List<GamePlayerResult> results = new ArrayList<>();
            int rank = 1;
            
            for (Map<String, Object> player : leaderboard) {
                String username = (String) player.get("username");
                int score = (int) player.get("score");

                GamePlayerResult res = new GamePlayerResult();
                res.setNickname(username);
                res.setTotalScore(score);
                res.setRankPosition(rank); 
                res.setHistory(history); 
                results.add(res);

                // --- LOGIC CỘNG RICE CHO USER ---
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    long rewardRice = 0;
                    if (rank == 1) rewardRice = 50;
                    else if (rank == 2) rewardRice = 30;
                    else if (rank == 3) rewardRice = 20;
                    else rewardRice = 5;

                    user.setRice(user.getRice() + rewardRice);
                    userRepository.save(user); 
                    System.out.println("💰 Thưởng " + rewardRice + " Rice cho " + username);
                }
                // -------------------------------
                rank++; 
            }

            history.setResults(results); 
            historyRepository.save(history);
            System.out.println("✅ Đã lưu thành công lịch sử phòng: " + roomCode);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- HELPER METHODS ---

    private void broadcast(String roomCode, String type, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("data", data);
        messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, msg);
    }

    private List<Map<String, Object>> getLeaderboard(RoomData room) {
        return room.scores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue()) 
                .limit(5) 
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", e.getKey());
                    map.put("score", e.getValue());
                    map.put("avatar", room.players.get(e.getKey()));
                    return map;
                }).collect(Collectors.toList());
    }

    private String generateRoomCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    /**
     * Class nội bộ lưu trạng thái của một phòng chơi
     */
    private static class RoomData {
        UserOwnQuiz quiz;
        String status; 
        int currentQuestionIndex;
        long questionStartTime; 
        Map<String, String> players; 
        Map<String, Integer> scores; 
    }
}