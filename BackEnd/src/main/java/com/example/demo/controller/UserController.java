package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserDTO.LoginValidation;
import com.example.demo.dto.UserDTO.RegisterValidation;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.entity.jdbc.User;
import com.example.demo.service.EvaluatorService;
import com.example.demo.service.UserService;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final EvaluatorService evaluatorService;

    @Autowired
    public UserController(UserService userService, EvaluatorService evaluatorService) {
        this.userService = userService;
        this.evaluatorService = evaluatorService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated(RegisterValidation.class) @RequestBody UserDTO userDTO) {
        logger.info("收到用户注册请求 - 用户信息: username={}, contactInfo={}, role={}", 
            userDTO.getUsername(), userDTO.getContactInfo(), userDTO.getRole());
        
        try {
            // 验证请求数据
            if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
                logger.error("注册失败 - 用户名为空");
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "用户名不能为空", 
                    "errorField", "username"
                ), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                logger.error("注册失败 - 密码为空");
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "密码不能为空",
                    "errorField", "password"
                ), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getContactInfo() == null || userDTO.getContactInfo().trim().isEmpty()) {
                logger.error("注册失败 - 联系方式为空");
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "联系方式不能为空",
                    "errorField", "contactInfo"
                ), HttpStatus.BAD_REQUEST);
            }

            // 先检查用户名是否已存在
            if (userService.existsByUsername(userDTO.getUsername())) {
                logger.warn("注册失败 - 用户名已存在: {}", userDTO.getUsername());
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "用户名 '" + userDTO.getUsername() + "' 已被注册，请选择其他用户名",
                    "errorField", "username",
                    "errorCode", "USERNAME_EXISTS"
                ), HttpStatus.CONFLICT);
            }

            logger.debug("开始处理用户注册 - 验证通过，准备保存用户信息");
            User user = userService.register(userDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("name", user.getName());
            response.put("contactInfo", user.getContactInfo());
            response.put("role", user.getRole());
            response.put("message", "注册成功");
            
            logger.info("用户注册成功 - ID: {}, 用户名: {}, 角色: {}", 
                user.getId(), user.getUsername(), user.getRole());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("用户注册失败 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            // 根据错误消息判断错误类型
            if (e.getMessage().contains("用户名") && e.getMessage().contains("已被注册")) {
                error.put("errorField", "username");
                error.put("errorCode", "USERNAME_EXISTS");
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
            } else {
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("用户注册发生未预期的错误 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "注册过程中发生错误：" + e.getMessage());
            
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated(LoginValidation.class) @RequestBody UserDTO userDTO) {
        logger.info("收到用户登录请求 - 用户名: {}", userDTO.getUsername());
        
        try {
            // 验证请求数据
            if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
                logger.error("登录失败 - 用户名为空");
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "用户名不能为空",
                    "errorField", "username"
                ), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                logger.error("登录失败 - 密码为空");
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "密码不能为空",
                    "errorField", "password"
                ), HttpStatus.BAD_REQUEST);
            }

            // 先检查用户是否存在
            boolean userExists = userService.existsByUsername(userDTO.getUsername());
            if (!userExists) {
                logger.warn("登录失败 - 用户名不存在: {}", userDTO.getUsername());
                return new ResponseEntity<>(Map.of(
                    "success", false,
                    "error", "用户名不存在，请先注册",
                    "errorField", "username",
                    "errorCode", "USERNAME_NOT_EXIST"
                ), HttpStatus.UNAUTHORIZED);
            }

            logger.debug("开始处理用户登录 - 用户名: {}", userDTO.getUsername());
            return userService.login(userDTO.getUsername(), userDTO.getPassword())
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("id", user.getId());
                        response.put("username", user.getUsername());
                        response.put("name", user.getName());
                        response.put("contactInfo", user.getContactInfo());
                        response.put("role", user.getRole());
                        response.put("message", "登录成功");
                        
                        // 添加用户是否为评测员的判断
                        boolean isEvaluator = evaluatorService.getEvaluatorByUserId(user.getId()).isPresent();
                        response.put("isEvaluator", isEvaluator);
                        
                        // 如果是评测员，添加评测员ID
                        if (isEvaluator) {
                            evaluatorService.getEvaluatorByUserId(user.getId())
                                .ifPresent(evaluator -> response.put("evaluatorId", evaluator.getId()));
                        }
                        
                        logger.info("用户登录成功 - ID: {}, 用户名: {}, 角色: {}, 是否为评测者: {}", 
                            user.getId(), user.getUsername(), user.getRole(), isEvaluator);
                        
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.warn("用户登录失败 - 用户名: {}, 原因: 密码错误", 
                            userDTO.getUsername());
                        return new ResponseEntity<>(
                            Map.of(
                                "success", false,
                                "error", "密码错误，请重新输入",
                                "errorField", "password",
                                "errorCode", "INVALID_PASSWORD"
                            ), 
                            HttpStatus.UNAUTHORIZED);
                    });
        } catch (Exception e) {
            logger.error("用户登录发生未预期的错误 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "登录过程中发生错误：" + e.getMessage());
            
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("收到获取所有用户请求");
        List<User> users = userService.getAllUsers();
        logger.info("成功获取所有用户 - 用户数量: {}", users.size());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        logger.info("收到获取用户信息请求 - 用户ID: {}", id);
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("成功获取用户信息 - ID: {}, 用户名: {}", 
                        user.getId(), user.getUsername());
                    return new ResponseEntity<>(user, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("获取用户信息失败 - 用户ID: {}, 原因: 用户不存在", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        logger.info("收到更新用户信息请求 - 用户ID: {}", id);
        
        try {
            return userService.getUserById(id)
                    .map(existingUser -> {
                        // 只更新非null的字段，避免覆盖现有数据
                        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                            // 检查用户名是否已被其他用户使用
                            if (userService.existsByUsername(user.getUsername()) && 
                                !existingUser.getUsername().equals(user.getUsername())) {
                                logger.warn("更新用户信息失败 - 用户名已存在: {}", user.getUsername());
                                return ResponseEntity.badRequest().body(Map.of(
                                    "success", false,
                                    "error", "用户名已被使用，请选择其他用户名",
                                    "errorField", "username"
                                ));
                            }
                            existingUser.setUsername(user.getUsername());
                        }
                        if (user.getName() != null) {
                            existingUser.setName(user.getName());
                        }
                        if (user.getContactInfo() != null) {
                            existingUser.setContactInfo(user.getContactInfo());
                        }
                        if (user.getRole() != null) {
                            existingUser.setRole(user.getRole());
                        }
                        // 密码更新需要特殊处理，暂时跳过
                        // 更新时间戳
                        existingUser.updateTimestamp();
                        
                        User updatedUser = userService.updateUser(existingUser);
                        logger.info("成功更新用户信息 - ID: {}, 用户名: {}", 
                            updatedUser.getId(), updatedUser.getUsername());
                        return ResponseEntity.ok(updatedUser);
                    })
                    .orElseGet(() -> {
                        logger.warn("更新用户信息失败 - 用户ID: {}, 原因: 用户不存在", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("更新用户信息时发生错误 - 用户ID: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "更新用户信息时发生错误: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("收到删除用户请求 - 用户ID: {}", id);
        return userService.getUserById(id)
                .map(user -> {
                    userService.deleteUser(id);
                    logger.info("成功删除用户 - ID: {}, 用户名: {}", 
                        user.getId(), user.getUsername());
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> {
                    logger.warn("删除用户失败 - 用户ID: {}, 原因: 用户不存在", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, Object> logoutRequest) {
        logger.info("收到用户登出请求");
        
        try {
            // 获取并验证请求参数
            Long userId = Long.valueOf(logoutRequest.get("id").toString());
            String username = (String) logoutRequest.get("username");
            
            if (userId == null || username == null || username.trim().isEmpty()) {
                logger.error("登出失败 - 用户ID或用户名为空");
                return new ResponseEntity<>(Map.of("error", "用户ID和用户名不能为空"), HttpStatus.BAD_REQUEST);
            }

            // 验证用户是否存在
            return userService.getUserById(userId)
                .map(user -> {
                    if (!user.getUsername().equals(username)) {
                        logger.error("登出失败 - 用户ID和用户名不匹配");
                        return new ResponseEntity<>(Map.of("error", "用户信息验证失败"), HttpStatus.BAD_REQUEST);
                    }
                    
                    logger.info("用户验证成功，处理登出请求 - 用户ID: {}, 用户名: {}", userId, username);
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "登出成功");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.error("登出失败 - 用户不存在");
                    return new ResponseEntity<>(Map.of("error", "用户不存在"), HttpStatus.NOT_FOUND);
                });
        } catch (Exception e) {
            logger.error("登出过程中发生错误", e);
            return new ResponseEntity<>(Map.of("error", "登出过程中发生错误：" + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/profile/{userId:\\d+}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pageable")
    public ResponseEntity<?> getAllUsersPaginated(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("收到分页获取所有用户请求 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<User> usersPage = userService.getAllUsers(pageable);
            
            // 转换为前端友好的响应格式
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            // 用户列表，过滤掉敏感信息
            List<Map<String, Object>> usersList = usersPage.getContent().stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("name", user.getName());
                    userMap.put("contactInfo", user.getContactInfo());
                    userMap.put("role", user.getRole());
                    userMap.put("createdAt", user.getCreatedAt());
                    userMap.put("updatedAt", user.getUpdatedAt());
                    
                    // 添加是否是评测员的判断
                    boolean isEvaluator = evaluatorService.getEvaluatorByUserId(user.getId()).isPresent();
                    userMap.put("isEvaluator", isEvaluator);
                    
                    // 如果是评测员，添加评测员ID
                    if (isEvaluator) {
                        evaluatorService.getEvaluatorByUserId(user.getId())
                            .ifPresent(evaluator -> userMap.put("evaluatorId", evaluator.getId()));
                    }
                    
                    // 不返回密码等敏感信息
                    return userMap;
                })
                .collect(Collectors.toList());
            
            response.put("users", usersList);
            
            // 分页信息
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("currentPage", usersPage.getNumber());
            pageInfo.put("totalPages", usersPage.getTotalPages());
            pageInfo.put("totalElements", usersPage.getTotalElements());
            pageInfo.put("size", usersPage.getSize());
            pageInfo.put("numberOfElements", usersPage.getNumberOfElements());
            pageInfo.put("first", usersPage.isFirst());
            pageInfo.put("last", usersPage.isLast());
            pageInfo.put("empty", usersPage.isEmpty());
            
            response.put("pageInfo", pageInfo);
            
            logger.info("成功分页获取所有用户 - 总用户数: {}, 当前页用户数: {}", 
                usersPage.getTotalElements(), usersPage.getNumberOfElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("分页获取用户列表失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "获取用户列表失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("收到搜索用户请求 - 关键词: {}, 页码: {}, 每页大小: {}", 
            keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<User> usersPage = userService.searchUsers(keyword, pageable);
            
            // 转换为前端友好的响应格式
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            // 用户列表，过滤掉敏感信息
            List<Map<String, Object>> usersList = usersPage.getContent().stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("name", user.getName());
                    userMap.put("contactInfo", user.getContactInfo());
                    userMap.put("role", user.getRole());
                    userMap.put("createdAt", user.getCreatedAt());
                    userMap.put("updatedAt", user.getUpdatedAt());
                    
                    // 添加是否是评测员的判断
                    boolean isEvaluator = evaluatorService.getEvaluatorByUserId(user.getId()).isPresent();
                    userMap.put("isEvaluator", isEvaluator);
                    
                    // 如果是评测员，添加评测员ID
                    if (isEvaluator) {
                        evaluatorService.getEvaluatorByUserId(user.getId())
                            .ifPresent(evaluator -> userMap.put("evaluatorId", evaluator.getId()));
                    }
                    
                    // 不返回密码等敏感信息
                    return userMap;
                })
                .collect(Collectors.toList());
            
            response.put("users", usersList);
            
            // 分页信息
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("currentPage", usersPage.getNumber());
            pageInfo.put("totalPages", usersPage.getTotalPages());
            pageInfo.put("totalElements", usersPage.getTotalElements());
            pageInfo.put("size", usersPage.getSize());
            pageInfo.put("numberOfElements", usersPage.getNumberOfElements());
            pageInfo.put("first", usersPage.isFirst());
            pageInfo.put("last", usersPage.isLast());
            pageInfo.put("empty", usersPage.isEmpty());
            
            response.put("pageInfo", pageInfo);
            response.put("keyword", keyword); // 返回搜索关键词
            
            logger.info("成功搜索用户 - 关键词: {}, 总结果数: {}, 当前页结果数: {}", 
                keyword, usersPage.getTotalElements(), usersPage.getNumberOfElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索用户失败 - 关键词: {}", keyword, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "搜索用户失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
} 