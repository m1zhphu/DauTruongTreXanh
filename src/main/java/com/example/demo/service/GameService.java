package com.example.demo.service;

import com.example.demo.dto.QuestionDto;
import com.example.demo.dto.SubmitAnswerRequest;
import com.example.demo.dto.TopicDto;
import com.example.demo.entity.Question;
import com.example.demo.entity.Region;
import com.example.demo.entity.Topic;
import com.example.demo.entity.User;
import com.example.demo.entity.UserTopicProgress;
import com.example.demo.mapper.AppMapper;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.RegionRepository;
import com.example.demo.repository.TopicRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserTopicProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final UserTopicProgressRepository progressRepository;
    private final AppMapper mapper;

    private final RegionRepository regionRepository;
    private final RegionService regionService;

    // 1. Lấy danh sách chủ đề CÔNG KHAI
    @Transactional(readOnly = true)
    public List<TopicDto> getAllPublicTopics() {
        List<Topic> topics = topicRepository.findByIsPublicTrue();
        return topics.stream()
                .map(mapper::toTopicResponse)
                .collect(Collectors.toList());
    }

    // 2. Tham gia lớp riêng tư
    @Transactional(readOnly = true)
    public TopicDto joinPrivateTopic(String accessCode, String password) {
        Topic topic = topicRepository.findByAccessCodeAndPassword(accessCode, password)
                .orElseThrow(() -> new IllegalArgumentException("Mã lớp hoặc mật khẩu không đúng!"));
        return mapper.toTopicResponse(topic);
    }

    // 3. LẤY CÂU HỎI THEO ID (Đã sửa: Nhận String ID, Parse, và Shuffle tại đây)
    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsByTopicId(String rawTopicId, String username) {
        // 1. Validate ID ngay tại Service
        long topicId;
        try {
            topicId = Long.parseLong(rawTopicId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID chủ đề không hợp lệ (Phải là số). Bạn đang gửi: " + rawTopicId);
        }

        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return List.of();

        List<Question> allQuestions = questionRepository.findByTopicId(topic.getId());
        // Sắp xếp ID để đảm bảo thứ tự consistent trước khi cắt theo tiến độ
        allQuestions.sort(Comparator.comparing(Question::getId));

        List<QuestionDto> result;

        // Logic xử lý User Progress
        if (username == null) {
            result = allQuestions.stream().map(mapper::toQuestionResponse).collect(Collectors.toList());
        } else {
            User user = userRepository.findByUsername(username).orElseThrow();
            UserTopicProgress progress = progressRepository.findByUserIdAndTopicId(user.getId(), topic.getId())
                    .orElse(null);

            if (progress == null) {
                result = allQuestions.stream().map(mapper::toQuestionResponse).collect(Collectors.toList());
            } else if (Boolean.TRUE.equals(progress.getIsCompleted())) {
                return List.of();
            } else {
                int currentIndex = progress.getCurrentQuestionIndex();
                if (currentIndex < 0) currentIndex = 0;
                if (currentIndex >= allQuestions.size()) {
                    return List.of();
                }
                List<Question> remainingQuestions = allQuestions.subList(currentIndex, allQuestions.size());
                result = remainingQuestions.stream()
                        .map(mapper::toQuestionResponse)
                        .collect(Collectors.toList());
            }
        }

        // 2. Shuffle câu hỏi tại Service (Business Logic)
        Collections.shuffle(result);
        
        return result;
    }

    // 4. Lấy tiến độ
    @Transactional(readOnly = true)
    public UserTopicProgress getProgress(String username, Long topicId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return progressRepository.findByUserIdAndTopicId(user.getId(), topicId)
                .orElseGet(() -> UserTopicProgress.builder()
                        .userId(user.getId())
                        .topicId(topicId)
                        .currentKnots(0)
                        .currentQuestionIndex(0)
                        .isCompleted(false)
                        .updatedAt(LocalDateTime.now())
                        .build());
    }

    // 5. Nộp bài
    @Transactional
    public UserTopicProgress submitAnswer(String username, SubmitAnswerRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserTopicProgress progress = progressRepository.findByUserIdAndTopicId(user.getId(), request.getTopicId())
                .orElseGet(() -> {
                    UserTopicProgress p = new UserTopicProgress();
                    p.setUserId(user.getId());
                    p.setTopicId(request.getTopicId());
                    p.setCurrentKnots(0);
                    p.setCurrentQuestionIndex(0);
                    p.setIsCompleted(false);
                    return progressRepository.save(p);
                });

        int totalQuestions = questionRepository.countByTopicId(request.getTopicId());

        if (request.isCorrect()) {
            updateStreak(user);
            
            // Cộng XP
            long currentXp = user.getTotalXp() == null ? 0L : user.getTotalXp();
            user.setTotalXp(currentXp + 10);

            // Cộng Rice
            long currentRice = user.getRice() == null ? 0L : user.getRice();
            user.setRice(currentRice + 5); 

            int currentKnots = progress.getCurrentKnots() == null ? 0 : progress.getCurrentKnots();
            int currentIndex = progress.getCurrentQuestionIndex() == null ? 0 : progress.getCurrentQuestionIndex();

            progress.setCurrentKnots(currentKnots + 1);
            progress.setCurrentQuestionIndex(currentIndex + 1);
        } else {
            progress.setCurrentKnots(0);
            int currentIndex = progress.getCurrentQuestionIndex() == null ? 0 : progress.getCurrentQuestionIndex();
            progress.setCurrentQuestionIndex(currentIndex + 1);
        }

        // Kiểm tra hoàn thành
        if (progress.getCurrentQuestionIndex() >= totalQuestions) {
            if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
                progress.setIsCompleted(true);
                
                // Thưởng lớn
                long currentRice = user.getRice() == null ? 0L : user.getRice();
                user.setRice(currentRice + 50);
                System.out.println("🌾 User " + username + " hoàn thành Topic -> +50 Rice");

                // Mở khóa Map
                try {
                    Optional<Region> regionOpt = regionRepository.findByTopicId(request.getTopicId());
                    if (regionOpt.isPresent()) {
                        Region region = regionOpt.get();
                        regionService.completeRegion(user.getId(), region.getId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        progress.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return progressRepository.save(progress);
    }

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastStudyDate = (user.getLastStudyDate() != null) ? user.getLastStudyDate().toLocalDate() : null;

        if (lastStudyDate != null && lastStudyDate.isEqual(today)) {
            user.setLastStudyDate(LocalDateTime.now());
            return;
        }

        int currentStreak = user.getCurrentStreak() == null ? 0 : user.getCurrentStreak();

        if (lastStudyDate == null) {
            user.setCurrentStreak(1);
        } else if (lastStudyDate.isEqual(today.minusDays(1))) {
            user.setCurrentStreak(currentStreak + 1);
        } else {
            user.setCurrentStreak(1);
        }

        user.setLastStudyDate(LocalDateTime.now());
    }
}