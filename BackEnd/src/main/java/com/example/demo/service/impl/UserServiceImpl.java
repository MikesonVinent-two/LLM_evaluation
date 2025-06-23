package com.example.demo.service.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.entity.jdbc.User;
import com.example.demo.entity.jdbc.UserRole;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(UserDTO userDTO) {
        logger.debug("开始处理用户注册 - 用户信息: username={}, contactInfo={}, role={}", 
            userDTO.getUsername(), userDTO.getContactInfo(), userDTO.getRole());
        
        try {
            // 检查用户名是否已存在
            if (existsByUsername(userDTO.getUsername())) {
                logger.warn("用户注册失败 - 用户名: {} 已存在", userDTO.getUsername());
                throw new RuntimeException("用户名 '" + userDTO.getUsername() + "' 已被注册，请选择其他用户名");
            }

            // 创建新用户实体
            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setContactInfo(userDTO.getContactInfo());
            user.setName(userDTO.getName());
            
            // 加密密码
            try {
                String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
                user.setPassword(encodedPassword);
                logger.debug("密码加密成功");
            } catch (Exception e) {
                logger.error("密码加密失败", e);
                throw new RuntimeException("密码处理失败");
            }
            
            // 设置用户角色
            user.setRole(userDTO.getRole() != null ? userDTO.getRole() : UserRole.CROWDSOURCE_USER);
            
            // 保存用户
            try {
                User savedUser = userRepository.save(user);
                logger.info("用户注册成功 - ID: {}, 用户名: {}", savedUser.getId(), savedUser.getUsername());
                return savedUser;
            } catch (Exception e) {
                logger.error("保存用户信息到数据库失败", e);
                throw new RuntimeException("保存用户信息失败");
            }
        } catch (RuntimeException e) {
            logger.error("用户注册过程中发生错误", e);
            throw e;
        }
    }

    @Override
    public Optional<User> login(String username, String password) {
        logger.debug("开始处理用户登录 - 用户名: {}", username);
        
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                try {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        logger.info("用户登录成功 - ID: {}, 用户名: {}", user.getId(), user.getUsername());
                        return Optional.of(user);
                    } else {
                        logger.warn("用户登录失败 - 用户名: {}, 原因: 密码错误", username);
                    }
                } catch (Exception e) {
                    logger.error("密码验证过程中发生错误", e);
                    throw new RuntimeException("密码验证失败");
                }
            } else {
                logger.warn("用户登录失败 - 用户名: {}, 原因: 用户不存在", username);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            logger.error("用户登录过程中发生错误", e);
            throw new RuntimeException("登录过程中发生错误: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        boolean exists = userRepository.existsByUsername(username);
        logger.debug("检查用户名是否存在 - 用户名: {}, 结果: {}", username, exists);
        return exists;
    }

    @Override
    public List<User> getAllUsers() {
        logger.debug("获取所有用户列表");
        List<User> users = userRepository.findAll();
        logger.debug("成功获取用户列表 - 用户数量: {}", users.size());
        return users;
    }
    
    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("分页获取用户列表 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<User> usersPage = userRepository.findAll(pageable);
            logger.debug("成功分页获取用户列表 - 总用户数: {}, 当前页用户数: {}", 
                usersPage.getTotalElements(), usersPage.getNumberOfElements());
            return usersPage;
        } catch (Exception e) {
            logger.error("分页获取用户列表失败", e);
            throw new RuntimeException("获取用户列表时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Optional<User> getUserById(Long id) {
        logger.debug("根据ID获取用户 - ID: {}", id);
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            logger.debug("成功获取用户 - ID: {}, 用户名: {}", id, user.get().getUsername());
        } else {
            logger.debug("未找到用户 - ID: {}", id);
        }
        return user;
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        logger.debug("根据用户名获取用户 - 用户名: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            logger.debug("成功获取用户 - 用户名: {}, ID: {}", username, user.get().getId());
        } else {
            logger.debug("未找到用户 - 用户名: {}", username);
        }
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        logger.debug("开始删除用户 - ID: {}", id);
        userRepository.softDelete(id);
        logger.debug("成功删除用户 - ID: {}", id);
    }

    @Override
    public User updateUser(User user) {
        logger.debug("开始更新用户信息 - ID: {}", user.getId());
        User updatedUser = userRepository.save(user);
        logger.debug("成功更新用户信息 - ID: {}, 用户名: {}", updatedUser.getId(), updatedUser.getUsername());
        return updatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertToProfileDTO);
    }

    private UserProfileDTO convertToProfileDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setContactInfo(user.getContactInfo());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    @Override
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        logger.debug("根据关键词搜索用户 - 关键词: {}, 页码: {}, 每页大小: {}", 
            keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                // 如果关键词为空，则返回所有用户
                logger.debug("关键词为空，返回所有用户");
                return getAllUsers(pageable);
            }
            
            Page<User> usersPage = userRepository.searchByKeyword(keyword.trim(), pageable);
            logger.debug("成功搜索用户 - 关键词: {}, 总结果数: {}, 当前页结果数: {}", 
                keyword, usersPage.getTotalElements(), usersPage.getNumberOfElements());
            return usersPage;
        } catch (Exception e) {
            logger.error("搜索用户失败 - 关键词: {}", keyword, e);
            throw new RuntimeException("搜索用户时发生错误: " + e.getMessage());
        }
    }
} 