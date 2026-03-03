package com.example.demo.config;

import com.example.demo.entity.ContentType;
import com.example.demo.entity.RadioTrack;
import com.example.demo.repository.RadioTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RadioSeeder implements CommandLineRunner {

    private final RadioTrackRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            repository.saveAll(Arrays.asList(
                RadioTrack.builder()
                    .title("Hào Khí Việt Nam")
                    .author("Holy Thắng")
                    .description("Bài hát hào hùng về lịch sử dân tộc.")
                    .type(ContentType.HISTORY) // Dùng Enum
                    .audioUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                    .isActive(true)
                    .build(),

                RadioTrack.builder()
                    .title("Sáo Trúc: Bèo Dạt Mây Trôi")
                    .author("Dân Ca")
                    .description("Nhạc không lời thư giãn, tập trung học tập.")
                    .type(ContentType.LOFI) // Dùng Enum
                    .audioUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
                    .isActive(true)
                    .build(),

                RadioTrack.builder()
                    .title("Luật Thừa Kế (Đọc chậm)")
                    .author("Pháp Luật Đại Cương")
                    .description("Podcast kiến thức pháp luật, giọng đọc trầm ấm.")
                    .type(ContentType.LAW) // Dùng Enum
                    .audioUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
                    .isActive(true)
                    .build()
            ));
            System.out.println("--- Radio Seeder: Đã khởi tạo dữ liệu mẫu thành công! ---");
        }
    }
}