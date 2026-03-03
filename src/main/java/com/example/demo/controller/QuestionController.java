package com.example.demo.controller;

import com.example.demo.dto.CreateTopicRequest;
import com.example.demo.dto.QuestionDto;
import com.example.demo.entity.Question;
import com.example.demo.entity.Topic;
import com.example.demo.mapper.AppMapper;
import com.example.demo.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AppMapper mapper;

    // --- API LẤY THƯ VIỆN CỦA TÔI (Tạo + Mua) ---
    @GetMapping("/my-library")
    public ResponseEntity<?> getMyLibrary(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập!");
        }
        try {
            Map<String, Object> library = questionService.getMyLibrary(principal.getName());
            return ResponseEntity.ok(library);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API LẤY CHI TIẾT CHỦ ĐỀ (Dùng cho Admin hoặc Edit) ---
    // Đổi đường dẫn thành /detail/{id} để tránh xung đột
    @GetMapping("/detail/{id}") 
    public ResponseEntity<?> getTopicDetail(@PathVariable Long id) {
        try {
            Topic topic = questionService.getTopicById(id);
            return ResponseEntity.ok(topic);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Lỗi: " + e.getMessage());
        }
    }

    // --- API UPLOAD PDF ---
    @PostMapping("/generate-from-pdf")
    public ResponseEntity<?> generateFromPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("topic") String topic
    ) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Thiếu file PDF!");
            List<Question> result = questionService.generateFromUploadedPdf(file, topic, 10);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API TẠO TỪ TOPIC ---
    @PostMapping("/generate-by-topic")
    public ResponseEntity<?> generateByTopic(@RequestBody Map<String, String> payload) {
        try {
            String topic = payload.get("topic");
            List<Question> result = questionService.generateFromTopic(topic, 10);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API TẠO TỪ FILE LOCAL ---
    @PostMapping("/generate-from-local-path")
    public ResponseEntity<?> generateFromLocalPath(
            @RequestBody Map<String, String> payload, 
            @RequestParam(value = "quantity", defaultValue = "10") int quantity 
    ) {
        try {
            String filePath = payload.get("filePath");
            String topic = payload.get("topic");
            List<Question> result = questionService.generateFromLocalPath(filePath, topic, quantity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý file: " + e.getMessage());
        }
    }

    // --- API TÌM KIẾM ---
    @GetMapping("/find-topic")
    public ResponseEntity<?> findTopic(@RequestParam String name) {
        try {
            Topic topic = questionService.findTopicByName(name);
            if (topic != null) return ResponseEntity.ok(topic);
            else return ResponseEntity.status(404).body("Chưa tìm thấy chủ đề này.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API TẠO CHỦ ĐỀ THỦ CÔNG ---
    @PostMapping("/create-topic-only")
    public ResponseEntity<?> createTopicOnly(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,              
            @RequestParam("description") String description, 
            @RequestParam("totalKnots") int totalKnots,      
            @RequestParam("status") boolean status           
    ) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Thiếu file PDF!");
            Topic newTopic = questionService.createTopicOnly(file, name, description, totalKnots, status);
            return ResponseEntity.ok("Đã tạo chủ đề & lưu file thành công! ID: " + newTopic.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API TẠO FULL TÍNH NĂNG ---
    @PostMapping("/create-full-topic")
    public ResponseEntity<?> createFullTopic(
            @ModelAttribute CreateTopicRequest request,
            Principal principal
    ) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Vui lòng đăng nhập để thực hiện chức năng này!");
            }
            
            MultipartFile file = request.getFile();
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file PDF!");
            }

            Topic createdTopic = questionService.createTopicAndGenerateQuestions(
                    file, 
                    request.getTitle(), 
                    request.getIsPublic(), 
                    request.getAccessCode(), 
                    request.getPassword(), 
                    request.getNumQuestions(),
                    request.getStartTime(),
                    request.getEndTime(),
                    principal.getName()
            );

            return ResponseEntity.ok(Map.of(
                "message", "Tạo chủ đề thành công!",
                "topicId", createdTopic.getId(),
                "topicName", createdTopic.getName(),
                "questionCount", createdTopic.getTotalKnots()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // --- API LẤY TẤT CẢ (ADMIN) ---
    @GetMapping("/all-topics")
    public ResponseEntity<?> getAllTopicsForAdmin() {
        try {
            List<Topic> topics = questionService.getAllTopicsForAdmin();
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API QUẢN LÝ CÂU HỎI (ADMIN) ---
    @GetMapping("/admin-list")
    public ResponseEntity<?> getQuestionsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long topicId
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Question> questionPage = questionService.getAllQuestionsForAdmin(keyword, topicId, pageable);
            Page<QuestionDto> responsePage = questionPage.map(q -> mapper.toQuestionResponse(q));
            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // --- API CẬP NHẬT ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTopic(@PathVariable Long id, @ModelAttribute CreateTopicRequest request) {
        try {
            Topic updatedTopic = questionService.updateTopicMetadata(id, request);
            return ResponseEntity.ok("Cập nhật thành công chủ đề: " + updatedTopic.getName());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi cập nhật: " + e.getMessage());
        }
    }

    // --- API XÓA ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTopic(@PathVariable Long id) {
        try {
            questionService.deleteTopic(id); 
            return ResponseEntity.ok("Đã xóa chủ đề thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xóa chủ đề: " + e.getMessage());
        }
    }
}