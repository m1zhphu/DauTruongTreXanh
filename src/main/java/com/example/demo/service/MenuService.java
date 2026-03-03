package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.MenuDto;
import com.example.demo.entity.Menu;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.MenuMapper;
import com.example.demo.repository.MenuRepository;

@Service
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuDto> getAllMenus() {
        // Lấy tất cả menu
        return menuRepository.findAll().stream()
                .map(MenuMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuDto getMenuById(Integer id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found with id: " + id));
        return MenuMapper.toDto(menu);
    }

    public MenuDto createMenu(MenuDto menuDto) {
        Menu menu = MenuMapper.toEntity(menuDto);
        Menu savedMenu = menuRepository.save(menu);
        return MenuMapper.toDto(savedMenu);
    }

    public MenuDto updateMenu(Integer id, MenuDto menuDto) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found with id: " + id));

        // Cập nhật thông tin từ DTO vào entity
        existingMenu.setName(menuDto.getName());
        existingMenu.setLink(menuDto.getLink());
        existingMenu.setDisplayOrder(menuDto.getDisplayOrder());
        existingMenu.setParentId(menuDto.getParentId());
        existingMenu.setPosition(menuDto.getPosition());
        existingMenu.setStatus(menuDto.isStatus());

        Menu updatedMenu = menuRepository.save(existingMenu);
        return MenuMapper.toDto(updatedMenu);
    }

    public void deleteMenu(Integer id) {
        if (!menuRepository.existsById(id)) {
            throw new ResourceNotFoundException("Menu not found with id: " + id);
        }
        menuRepository.deleteById(id);
    }
}