package com.example.demo.config;

import com.example.demo.entity.Region;
import com.example.demo.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class RegionSeeder implements CommandLineRunner {

    @Autowired
    private RegionRepository regionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (regionRepository.count() == 0) {
            System.out.println("--> Bắt đầu tạo dữ liệu mẫu cho 63 tỉnh thành...");
            List<Region> regions = new ArrayList<>();

            // 1. CÁC THÀNH PHỐ TRỰC THUỘC TRUNG ƯƠNG
            regions.add(Region.builder().id("VNHN").name("Hà Nội").description("Thủ đô ngàn năm văn hiến").isIsland(false).color("bg-red-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VNSG").name("TP. Hồ Chí Minh").description("Thành phố mang tên Bác").isIsland(false).color("bg-red-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VNDN").name("Đà Nẵng").description("Thành phố đáng sống").isIsland(false).color("bg-blue-500").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VNHP").name("Hải Phòng").description("Thành phố Hoa Phượng Đỏ").isIsland(false).color("bg-red-500").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VNCT").name("Cần Thơ").description("Tây Đô sông nước").isIsland(false).color("bg-green-500").requiredId("VNSG").topicId(null).build());

            // 2. MIỀN BẮC
            regions.add(Region.builder().id("VN13").name("Quảng Ninh").description("Di sản Hạ Long").isIsland(false).color("bg-blue-500").requiredId("VNHP").topicId(null).build());
            regions.add(Region.builder().id("VN18").name("Ninh Bình").description("Cố đô Hoa Lư").isIsland(false).color("bg-yellow-500").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VN02").name("Lào Cai").description("Sapa").isIsland(false).color("bg-green-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN04").name("Cao Bằng").description("Thác Bản Giốc").isIsland(false).color("bg-green-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN03").name("Hà Giang").description("Cao nguyên đá").isIsland(false).color("bg-gray-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN09").name("Lạng Sơn").description("Cửa khẩu Tân Thanh").isIsland(false).color("bg-green-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN06").name("Yên Bái").description("Mù Cang Chải").isIsland(false).color("bg-yellow-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN69").name("Thái Nguyên").description("Chè Thái").isIsland(false).color("bg-green-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN01").name("Lai Châu").description("Núi non hùng vĩ").isIsland(false).color("bg-gray-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN05").name("Sơn La").description("Mộc Châu").isIsland(false).color("bg-green-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN71").name("Điện Biên").description("Điện Biên Phủ").isIsland(false).color("bg-red-500").requiredId("VN05").topicId(null).build());
            regions.add(Region.builder().id("VN07").name("Tuyên Quang").description("Thủ đô kháng chiến").isIsland(false).color("bg-yellow-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN68").name("Phú Thọ").description("Đất tổ").isIsland(false).color("bg-red-500").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VN70").name("Vĩnh Phúc").description("Tam Đảo").isIsland(false).color("bg-blue-400").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VN54").name("Bắc Giang").description("Vải thiều").isIsland(false).color("bg-red-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN56").name("Bắc Ninh").description("Quan Họ").isIsland(false).color("bg-purple-500").requiredId("VNHN").topicId(null).build());
            regions.add(Region.builder().id("VN61").name("Hải Dương").description("Bánh đậu xanh").isIsland(false).color("bg-yellow-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN66").name("Hưng Yên").description("Nhãn lồng").isIsland(false).color("bg-orange-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN20").name("Thái Bình").description("Chị Hai Năm Tấn").isIsland(false).color("bg-yellow-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN63").name("Hà Nam").description("Làng Vũ Đại").isIsland(false).color("bg-gray-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN67").name("Nam Định").description("Đền Trần").isIsland(false).color("bg-red-600").requiredId(null).topicId(null).build());

            // 3. MIỀN TRUNG
            regions.add(Region.builder().id("VN21").name("Thanh Hóa").description("Thành nhà Hồ").isIsland(false).color("bg-blue-500").requiredId("VN18").topicId(null).build());
            regions.add(Region.builder().id("VN22").name("Nghệ An").description("Quê Bác").isIsland(false).color("bg-red-500").requiredId("VN21").topicId(null).build());
            regions.add(Region.builder().id("VN23").name("Hà Tĩnh").description("Ngã ba Đồng Lộc").isIsland(false).color("bg-yellow-600").requiredId("VN22").topicId(null).build());
            regions.add(Region.builder().id("VN24").name("Quảng Bình").description("Phong Nha").isIsland(false).color("bg-green-600").requiredId("VN23").topicId(null).build());
            regions.add(Region.builder().id("VN25").name("Quảng Trị").description("Thành cổ").isIsland(false).color("bg-red-600").requiredId("VN24").topicId(null).build());
            regions.add(Region.builder().id("VN26").name("Thừa Thiên Huế").description("Cố đô Huế").isIsland(false).color("bg-purple-600").requiredId("VN25").topicId(null).build());
            regions.add(Region.builder().id("VN27").name("Quảng Nam").description("Hội An").isIsland(false).color("bg-yellow-500").requiredId("VNDN").topicId(null).build());
            regions.add(Region.builder().id("VN29").name("Quảng Ngãi").description("Lý Sơn").isIsland(false).color("bg-blue-400").requiredId("VN27").topicId(null).build());
            regions.add(Region.builder().id("VN31").name("Bình Định").description("Đất võ").isIsland(false).color("bg-red-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN32").name("Phú Yên").description("Hoa vàng cỏ xanh").isIsland(false).color("bg-green-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN34").name("Khánh Hòa").description("Nha Trang").isIsland(false).color("bg-blue-600").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN36").name("Ninh Thuận").description("Phan Rang").isIsland(false).color("bg-orange-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN40").name("Bình Thuận").description("Mũi Né").isIsland(false).color("bg-yellow-400").requiredId(null).topicId(null).build());

            // 4. TÂY NGUYÊN
            regions.add(Region.builder().id("VN28").name("Kon Tum").description("Ngã ba Đông Dương").isIsland(false).color("bg-green-700").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN30").name("Gia Lai").description("Biển Hồ").isIsland(false).color("bg-green-600").requiredId("VN28").topicId(null).build());
            regions.add(Region.builder().id("VN33").name("Đắk Lắk").description("Buôn Ma Thuột").isIsland(false).color("bg-red-700").requiredId("VN30").topicId(null).build());
            regions.add(Region.builder().id("VN72").name("Đắk Nông").description("Hồ Tà Đùng").isIsland(false).color("bg-blue-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN35").name("Lâm Đồng").description("Đà Lạt").isIsland(false).color("bg-purple-500").requiredId("VN34").topicId(null).build());

            // 5. ĐÔNG NAM BỘ
            regions.add(Region.builder().id("VN39").name("Đồng Nai").description("Sân bay Long Thành").isIsland(false).color("bg-orange-600").requiredId("VNSG").topicId(null).build());
            regions.add(Region.builder().id("VN57").name("Bình Dương").description("KCN").isIsland(false).color("bg-blue-600").requiredId("VNSG").topicId(null).build());
            regions.add(Region.builder().id("VN58").name("Bình Phước").description("Hạt điều").isIsland(false).color("bg-yellow-600").requiredId("VN57").topicId(null).build());
            regions.add(Region.builder().id("VN37").name("Tây Ninh").description("Núi Bà Đen").isIsland(false).color("bg-gray-400").requiredId("VNSG").topicId(null).build());
            regions.add(Region.builder().id("VN43").name("Bà Rịa - Vũng Tàu").description("Biển Vũng Tàu").isIsland(false).color("bg-blue-500").requiredId("VN39").topicId(null).build());

            // 6. MIỀN TÂY
            regions.add(Region.builder().id("VN41").name("Long An").description("Cửa ngõ miền Tây").isIsland(false).color("bg-green-400").requiredId("VNSG").topicId(null).build());
            regions.add(Region.builder().id("VN46").name("Tiền Giang").description("Trái cây").isIsland(false).color("bg-yellow-400").requiredId("VN41").topicId(null).build());
            regions.add(Region.builder().id("VN50").name("Bến Tre").description("Xứ dừa").isIsland(false).color("bg-green-600").requiredId("VN46").topicId(null).build());
            regions.add(Region.builder().id("VN51").name("Trà Vinh").description("Ao Bà Om").isIsland(false).color("bg-green-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN49").name("Vĩnh Long").description("Cầu Mỹ Thuận").isIsland(false).color("bg-orange-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN45").name("Đồng Tháp").description("Sen hồng").isIsland(false).color("bg-pink-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN44").name("An Giang").description("Miếu Bà").isIsland(false).color("bg-purple-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN47").name("Kiên Giang").description("Phú Quốc").isIsland(false).color("bg-blue-400").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN73").name("Hậu Giang").description("Chợ nổi").isIsland(false).color("bg-yellow-500").requiredId("VNCT").topicId(null).build());
            regions.add(Region.builder().id("VN52").name("Sóc Trăng").description("Chùa Dơi").isIsland(false).color("bg-orange-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN55").name("Bạc Liêu").description("Công tử Bạc Liêu").isIsland(false).color("bg-gray-500").requiredId(null).topicId(null).build());
            regions.add(Region.builder().id("VN59").name("Cà Mau").description("Đất Mũi").isIsland(false).color("bg-green-700").requiredId(null).topicId(null).build());

            // 7. ĐẢO
            regions.add(Region.builder().id("hoangsa").name("Quần đảo Hoàng Sa").description("Chủ quyền thiêng liêng").isIsland(true).color("bg-yellow-500").requiredId("VNDN").topicId(null).build());
            regions.add(Region.builder().id("truongsa").name("Quần đảo Trường Sa").description("Máu thịt Việt Nam").isIsland(true).color("bg-yellow-500").requiredId("VN34").topicId(null).build());

            regionRepository.saveAll(regions);
            System.out.println("--> Đã tạo xong 63 tỉnh thành & hải đảo!");
        }
    }
}