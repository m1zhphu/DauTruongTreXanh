package com.example.demo.controller;

import com.example.demo.dto.QuestionDto;
import com.example.demo.dto.SubmitAnswerRequest;
import com.example.demo.dto.TopicDto;
import com.example.demo.entity.Question; // ✅ Import Entity
import com.example.demo.entity.Topic;    // ✅ Import Entity
import com.example.demo.entity.User;     // ✅ Import Entity
import com.example.demo.entity.UserTopicProgress;
import com.example.demo.repository.QuestionRepository; // ✅ Import Repo
import com.example.demo.repository.TopicRepository;    // ✅ Import Repo
import com.example.demo.repository.UserRepository;     // ✅ Import Repo
import com.example.demo.repository.UserTopicAccessRepository;
import com.example.demo.service.GameService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections; // ✅ Import Collections
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class GameController {

    private final GameService gameService;
    
    // ✅ INJECT CÁC REPOSITORY CÒN THIẾU
    @Autowired private UserTopicAccessRepository accessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;

    // 1. Lấy danh sách chủ đề CÔNG KHAI
    @GetMapping("/topics")
    public ResponseEntity<List<TopicDto>> getTopics() {
        return ResponseEntity.ok(gameService.getAllPublicTopics());
    }

    // 2. Tham gia lớp học riêng tư
    @PostMapping("/join")
    public ResponseEntity<?> joinTopic(@RequestBody Map<String, String> payload) {
        try {
            String code = payload.get("code");
            String pass = payload.get("password");
            TopicDto topic = gameService.joinPrivateTopic(code, pass);
            return ResponseEntity.ok(topic);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống"));
        }
    }

    // 3. Lấy danh sách câu hỏi để chơi (API Cũ - Có thể giữ lại hoặc bỏ)
    @GetMapping("/play/{rawTopicId}") 
    public ResponseEntity<?> playGame(@PathVariable String rawTopicId) {
        try {
            String username = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                 username = auth.getName(); 
            }

            List<QuestionDto> questions = gameService.getQuestionsByTopicId(rawTopicId, username); 
            return ResponseEntity.ok(questions);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    // 4. Lấy tiến độ
    @GetMapping("/progress/{topicId}")
    public ResponseEntity<UserTopicProgress> getProgress(@PathVariable Long topicId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(gameService.getProgress(username, topicId));
    }

    // 5. Nộp bài
    @PostMapping("/submit")
    public ResponseEntity<UserTopicProgress> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserTopicProgress progress = gameService.submitAnswer(username, request);
        return ResponseEntity.ok(progress);
    }

    // --- API MỚI: Lấy câu hỏi dựa trên quyền truy cập ---
    @GetMapping("/play-access/{topicId}")
    public ResponseEntity<?> playTopicWithAccessCheck(@PathVariable Long topicId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Tìm User (nếu chưa đăng nhập thì user là null)
        User user = null;
        if (!"anonymousUser".equals(username)) {
             user = userRepository.findByUsername(username).orElse(null);
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại"));

        boolean hasAccess = false;

        // 1. Nếu topic công khai -> OK
        if (topic.isPublic()) {
            hasAccess = true;
        } 
        // 2. Nếu user đã đăng nhập -> Kiểm tra quyền
        else if (user != null) {
            // 2a. Là tác giả -> OK
            if (topic.getCreator() != null && topic.getCreator().getId().equals(user.getId())) {
                hasAccess = true;
            }
            // 2b. Đã mua -> OK
            else if (accessRepository.existsByUserIdAndTopicId(user.getId(), topicId)) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            return ResponseEntity.status(403).body("Bạn chưa có quyền truy cập chủ đề này (Cần nhập mật khẩu hoặc mua).");
        }

        // Lấy danh sách câu hỏi
        // Lưu ý: Hàm findByTopicIdFetchOptions cần được định nghĩa trong QuestionRepository
        // Nếu chưa có, hãy dùng findByTopicId tạm thời
        List<Question> questions = questionRepository.findByTopicId(topicId); 
        Collections.shuffle(questions);
        
        List<QuestionDto> dtos = questions.stream().map(q -> {
            QuestionDto dto = new QuestionDto();
            dto.setId(q.getId());
            dto.setContent(q.getContent());
            dto.setOptions(new ArrayList<>(q.getOptions()));
            Collections.shuffle(dto.getOptions());
            dto.setExplanation(q.getExplanation());
            dto.setTopicId(q.getTopic().getId());
            dto.setCorrectAnswer(q.getCorrectAnswer()); 
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}