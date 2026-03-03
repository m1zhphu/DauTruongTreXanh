package com.example.demo.service;

import com.example.demo.entity.Question;
import com.example.demo.entity.Topic;
import com.example.demo.entity.User;
import com.example.demo.entity.UserTopicAccess;
import com.example.demo.dto.CreateTopicRequest;
import com.example.demo.dto.QuestionResult;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.TopicRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserTopicAccessRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired private PdfService pdfService;
    @Autowired private N8nService n8nService;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private UserRepository userRepository;
    
    @Autowired private UserTopicAccessRepository accessRepository;

    // --- HÀM 1: UPLOAD FILE & TẠO CÂU HỎI ---
    public List<Question> generateFromUploadedPdf(MultipartFile file, String topic, int quantity) throws IOException {
        String savedPath = pdfService.saveFile(file); 
        String content = pdfService.extractTextFromFilePath(savedPath);
        if (content == null || content.trim().isEmpty()) {
             throw new IllegalArgumentException("Nội dung trích xuất từ file PDF trống.");
        }
        return processAndSave(topic, content, quantity);
    }

    // --- HÀM 2: TẠO TỪ TOPIC ---
    public List<Question> generateFromTopic(String topic, int quantity) {
        return processAndSave(topic, "Không có tài liệu tham khảo, hãy tự dùng kiến thức của bạn.", quantity);
    }

    // --- HÀM 3: TẠO TỪ FILE LOCAL ---
    public List<Question> generateFromLocalPath(String filePath, String topic, int quantity) throws IOException {
        System.out.println("--> Đọc file local: " + filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("   (Tìm trong uploads/...)");
            file = new File("uploads/" + filePath);
        }
        if (!file.exists()) {
            throw new IOException("Không tìm thấy file tại: " + file.getAbsolutePath());
        }

        String content = pdfService.extractTextFromFilePath(file.getAbsolutePath());
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung file trống.");
        }
        
        return processAndSave(topic, content, quantity);
    }

    // --- HÀM 4: LOGIC CHUNG GỌI AI & LƯU ---
    private List<Question> processAndSave(String topicName, String content, int quantity) {
        List<QuestionResult> results = n8nService.generateQuestions(topicName, content, quantity);
        
        if (results == null || results.isEmpty()) {
            System.out.println("AI không trả về câu hỏi nào.");
            return new ArrayList<>();
        }

        Topic topicEntity = topicRepository.findByName(topicName);
        if (topicEntity == null) {
            topicEntity = new Topic();
            topicEntity.setName(topicName);
            topicEntity.setDescription("Tạo tự động bởi AI");
            topicEntity.setTotalKnots(results.size()); 
            topicEntity = topicRepository.save(topicEntity);
        } else {
            topicEntity.setTotalKnots(topicEntity.getTotalKnots() + results.size());
            topicRepository.save(topicEntity);
        }

        List<Question> savedQuestions = new ArrayList<>();
        for (QuestionResult res : results) {
            if (res.getQuestion() != null && res.getCorrect_answer() != null) {
                shuffleOptionsAndMapCorrectAnswer(res);
                if (!res.getOptions().contains(res.getCorrect_answer())) {
                    res.getOptions().add(res.getCorrect_answer());
                    Collections.shuffle(res.getOptions());
                }
                Question q = new Question(
                    topicEntity, 
                    res.getQuestion(), 
                    res.getOptions(), 
                    res.getCorrect_answer(), 
                    res.getExplanation()
                );
                savedQuestions.add(questionRepository.save(q));
            }
        }
        return savedQuestions;
    }

    // --- HÀM 5: TẠO TOPIC FULL OPTION ---
    public Topic createTopicAndGenerateQuestions(
            MultipartFile file, 
            String title, 
            boolean isPublic, 
            String accessCode, 
            String password, 
            int numQuestions,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String username
    ) throws IOException {

        if (topicRepository.findByName(title) != null) {
            throw new IllegalArgumentException("Chủ đề '" + title + "' đã tồn tại!");
        }
        if (!isPublic && (accessCode == null || accessCode.trim().isEmpty() || password == null)) {
            throw new IllegalArgumentException("Chủ đề riêng tư cần Mã lớp và Mật khẩu.");
        }
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }

        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));

        String savedPath = pdfService.saveFile(file);
        Topic newTopic = new Topic();
        newTopic.setName(title);
        newTopic.setDescription("Tạo từ file: " + file.getOriginalFilename());
        newTopic.setFilePath(savedPath);
        newTopic.setPublic(isPublic);
        newTopic.setTotalKnots(numQuestions);
        newTopic.setStartTime(startTime);
        newTopic.setEndTime(endTime);
        newTopic.setCreator(creator);

        if (!isPublic) {
            newTopic.setAccessCode(accessCode);
            newTopic.setPassword(password);
        }
        
        newTopic = topicRepository.save(newTopic);

        String content = pdfService.extractTextFromFilePath(savedPath);
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("File PDF không đọc được nội dung!");
        }

        List<QuestionResult> results = n8nService.generateQuestions(title, content, numQuestions);
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("AI không tạo được câu hỏi nào.");
        }

        List<Question> savedQuestions = new ArrayList<>();
        for (QuestionResult res : results) {
            if (res.getQuestion() != null) {
                shuffleOptionsAndMapCorrectAnswer(res);
                Question q = new Question(newTopic, res.getQuestion(), res.getOptions(), res.getCorrect_answer(), res.getExplanation());
                savedQuestions.add(questionRepository.save(q));
            }
        }
        
        newTopic.setTotalKnots(savedQuestions.size());
        return topicRepository.save(newTopic);
    }

    // --- ✅ SỬA LẠI HÀM NÀY ĐỂ TRÁNH LỖI PROXY 500 ---
    public Map<String, Object> getMyLibrary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Topic do tôi tạo
        List<Map<String, Object>> createdTopics = topicRepository.findByCreator_Id(user.getId())
                .stream()
                .map(this::convertTopicToMap) // Chuyển sang Map an toàn
                .collect(Collectors.toList());

        // 2. Topic tôi đã mua
        List<Map<String, Object>> purchasedTopics = accessRepository.findByUserId(user.getId())
                .stream()
                .map(access -> convertTopicToMap(access.getTopic())) // Chuyển sang Map an toàn
                .collect(Collectors.toList());

        return Map.of(
            "created", createdTopics,
            "purchased", purchasedTopics
        );
    }

    // --- HÀM PHỤ TRỢ: CHUYỂN TOPIC ENTITY SANG MAP (ĐỂ JSON KHÔNG BỊ LỖI) ---
    private Map<String, Object> convertTopicToMap(Topic topic) {
        if (topic == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", topic.getId());
        map.put("name", topic.getName());
        map.put("description", topic.getDescription());
        map.put("totalKnots", topic.getTotalKnots());
        map.put("isPublic", topic.isPublic());
        // Không map các field phức tạp (như creator, questions) để tránh lỗi lazy loading
        return map;
    }

    // --- LOGIC XÁO TRỘN ĐÁP ÁN ---
    private void shuffleOptionsAndMapCorrectAnswer(QuestionResult res) {
        if (res.getOptions() != null && !res.getOptions().isEmpty()) {
            Collections.shuffle(res.getOptions());
        }
    }

    // --- CÁC HÀM PHỤ TRỢ KHÁC (Giữ nguyên) ---

    public Topic findTopicByName(String name) {
        return topicRepository.findByName(name);
    }

    public Topic createTopicOnly(MultipartFile file, String name, String description, int totalKnots, boolean status) throws IOException {
        String savedPath = pdfService.saveFile(file);
        Topic topic = topicRepository.findByName(name);
        if (topic == null) topic = new Topic();
        topic.setName(name);
        topic.setDescription(description);
        topic.setTotalKnots(totalKnots);
        topic.setStatus(status);
        topic.setFilePath(savedPath);
        return topicRepository.save(topic);
    }

    public List<Topic> getAllTopicsForAdmin() {
        return topicRepository.findAll();
    }

    public Page<Question> getAllQuestionsForAdmin(String keyword, Long topicId, Pageable pageable) {
        return questionRepository.searchQuestions(keyword, topicId, pageable);
    }

    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề có ID: " + id));
    }

    public Topic updateTopicMetadata(Long id, CreateTopicRequest request) {
        Topic topic = getTopicById(id);
        topic.setName(request.getTitle());
        topic.setPublic(request.getIsPublic()); 
        topic.setStartTime(request.getStartTime());
        topic.setEndTime(request.getEndTime());

        if (!request.getIsPublic()) {
            topic.setAccessCode(request.getAccessCode());
            topic.setPassword(request.getPassword());
        } else {
            topic.setAccessCode(null);
            topic.setPassword(null);
        }

        return topicRepository.save(topic);
    }
    public void deleteTopic(Long id) {
        if (!topicRepository.existsById(id)) {
            throw new RuntimeException("Chủ đề không tồn tại với ID: " + id);
        }
        topicRepository.deleteById(id);
    }
}