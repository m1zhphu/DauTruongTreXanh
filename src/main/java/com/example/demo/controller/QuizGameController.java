package com.example.demo.controller;

import com.example.demo.dto.UserQuizDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.QuizGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List; // <--- ĐÃ THÊM IMPORT NÀY ĐỂ SỬA LỖI
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/live-quiz")
public class QuizGameController {

    @Autowired private QuizGameService quizService;
    @Autowired private UserOwnQuizRepository userQuizRepository; // Sửa lại tên biến cho chuẩn
    @Autowired private UserRepository userRepository;
    @Autowired private GameHistoryRepository historyRepository;

    // --- 1. TẠO QUIZ (POST) ---
    @PostMapping("/create")
    public ResponseEntity<?> createQuiz(@RequestBody UserQuizDto req, Principal principal) {
        try {
            UserOwnQuiz quiz = new UserOwnQuiz();
            quiz.setTitle(req.getTitle());
            quiz.setDescription(req.getDescription());
            quiz.setCoverImage(req.getCoverImage());
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setOwner(userRepository.findByUsername(principal.getName()).orElseThrow());

            // Map danh sách câu hỏi
            if (req.getQuestions() != null) {
                List<UserOwnQuestion> questionList = req.getQuestions().stream().map(q -> {
                    UserOwnQuestion question = new UserOwnQuestion();
                    question.setText(q.getContent()); // Map content -> text
                    question.setImageUrl(q.getImageUrl());
                    question.setTimeLimit(q.getTimeLimit());
                    question.setOptions(q.getOptions());
                    question.setCorrectAnswer(q.getCorrectAnswer());
                    question.setQuiz(quiz); // Gán cha
                    return question;
                }).collect(Collectors.toList());
                quiz.setQuestions(questionList);
            }

            userQuizRepository.save(quiz);
            return ResponseEntity.ok("Tạo quiz thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi Server: " + e.getMessage());
        }
    }

    // --- 2. LẤY CHI TIẾT QUIZ (GET) -> ĐỂ SỬA ---
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuizDetail(@PathVariable Long id) {
        try {
            UserOwnQuiz quiz = userQuizRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lỗi: " + e.getMessage());
        }
    }

    // --- 3. CẬP NHẬT QUIZ (PUT) ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long id, @RequestBody UserQuizDto req) {
        try {
            UserOwnQuiz existingQuiz = userQuizRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Quiz không tồn tại"));

            // Cập nhật thông tin chung
            existingQuiz.setTitle(req.getTitle());
            existingQuiz.setDescription(req.getDescription());
            existingQuiz.setCoverImage(req.getCoverImage());

            // Cập nhật danh sách câu hỏi: Xóa cũ, thêm mới
            existingQuiz.getQuestions().clear();

            if (req.getQuestions() != null) {
                for (var qDto : req.getQuestions()) {
                    UserOwnQuestion q = new UserOwnQuestion();
                    q.setText(qDto.getContent()); // Map content -> text
                    q.setImageUrl(qDto.getImageUrl());
                    q.setTimeLimit(qDto.getTimeLimit());
                    q.setOptions(qDto.getOptions());
                    q.setCorrectAnswer(qDto.getCorrectAnswer());
                    
                    q.setQuiz(existingQuiz); // Gán cha
                    existingQuiz.getQuestions().add(q);
                }
            }

            userQuizRepository.save(existingQuiz);
            return ResponseEntity.ok("Cập nhật thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi cập nhật: " + e.getMessage());
        }
    }
    
    // --- 4. LẤY DANH SÁCH QUIZ CỦA TÔI ---
    @GetMapping("/my-list")
    public ResponseEntity<?> getMyQuizzes(Principal principal) {
        var user = userRepository.findByUsername(principal.getName()).orElseThrow();
        return ResponseEntity.ok(userQuizRepository.findByOwner_Id(user.getId())); 
    }

    // --- 5. HOST GAME ---
    @PostMapping("/host/{quizId}")
    public ResponseEntity<?> hostGame(@PathVariable Long quizId) {
        try {
            String code = quizService.createRoom(quizId);
            return ResponseEntity.ok(Map.of("roomCode", code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- WEBSOCKET HANDLERS ---
    
    @MessageMapping("/quiz/{roomCode}/join")
    public void join(@DestinationVariable String roomCode, @Payload Map<String, String> payload) {
        quizService.joinRoom(roomCode, payload.get("username"), payload.get("avatar"));
    }

    @MessageMapping("/quiz/{roomCode}/start")
    public void start(@DestinationVariable String roomCode) {
        quizService.startGame(roomCode);
    }

    @MessageMapping("/quiz/{roomCode}/answer")
    public void answer(@DestinationVariable String roomCode, @Payload Map<String, String> payload) {
        quizService.submitAnswer(roomCode, payload.get("username"), payload.get("answer"));
    }
    
    @MessageMapping("/quiz/{roomCode}/next")
    public void next(@DestinationVariable String roomCode) {
        quizService.showRoundResult(roomCode);
    }

    @MessageMapping("/quiz/{roomCode}/next-q")
    public void nextQuestion(@DestinationVariable String roomCode) {
        quizService.nextQuestion(roomCode);
    }

    // Thêm các endpoint Pause/Resume nếu bạn đã cài đặt trong Service
    @MessageMapping("/quiz/{roomCode}/pause")
    public void pause(@DestinationVariable String roomCode) {
        // quizService.pauseGame(roomCode); // Cần implement bên Service
    }

    @MessageMapping("/quiz/{roomCode}/resume")
    public void resume(@DestinationVariable String roomCode) {
        // quizService.resumeGame(roomCode); // Cần implement bên Service
    }
}