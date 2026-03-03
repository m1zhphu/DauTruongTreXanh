package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Menu;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {

    // Tìm các menu con dựa vào parentId
    List<Menu> findByParentIdOrderByDisplayOrderAsc(Integer parentId);

    // Tìm các menu gốc (cấp 1) và sắp xếp
    List<Menu> findByParentIdIsNullOrderByDisplayOrderAsc();

    // (Optional) Tìm menu theo vị trí và sắp xếp
    List<Menu> findByPositionAndParentIdIsNullOrderByDisplayOrderAsc(String position);

    // (Thêm các phương thức truy vấn tùy chỉnh khác nếu cần)
}