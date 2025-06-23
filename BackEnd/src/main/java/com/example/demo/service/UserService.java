package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.UserDTO;
import com.example.demo.entity.jdbc.User;
import com.example.demo.dto.UserProfileDTO;

public interface UserService {
    User register(UserDTO userDTO);
    Optional<User> login(String username, String password);
    boolean existsByUsername(String username);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
    
    /**
     * 分页获取所有用户
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    Page<User> getAllUsers(Pageable pageable);
    
    /**
     * 根据关键词搜索用户并分页
     * @param keyword 搜索关键词（用户名、姓名或联系方式）
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    Page<User> searchUsers(String keyword, Pageable pageable);
    
    Optional<User> getUserByUsername(String username);
    void deleteUser(Long id);
    User updateUser(User user);

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息DTO
     */
    Optional<UserProfileDTO> getUserProfile(Long userId);
} 