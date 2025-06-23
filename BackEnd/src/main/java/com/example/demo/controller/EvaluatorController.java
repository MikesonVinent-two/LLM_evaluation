package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.EvaluatorRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.AiEvaluatorConnectivityService;
import com.example.demo.util.ApiConstants;

/**
 * 评测者管理控制器
 */
@RestController
@RequestMapping("/evaluators")
@CrossOrigin(origins = "*")
public class EvaluatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluatorController.class);
    
    private final EvaluatorRepository evaluatorRepository;
    private final UserRepository userRepository;
    private final AiEvaluatorConnectivityService aiEvaluatorConnectivityService;
    
    public EvaluatorController(
            EvaluatorRepository evaluatorRepository, 
            UserRepository userRepository,
            AiEvaluatorConnectivityService aiEvaluatorConnectivityService) {
        this.evaluatorRepository = evaluatorRepository;
        this.userRepository = userRepository;
        this.aiEvaluatorConnectivityService = aiEvaluatorConnectivityService;
    }
    
    /**
     * 获取所有评测者（不包括已删除的）
     */
    @GetMapping
    public ResponseEntity<List<Evaluator>> getAllEvaluators() {
        logger.info("接收到获取所有评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findAll().stream()
            .filter(e -> e.getDeletedAt() == null)
            .toList();
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 分页获取评测者列表（不包括已删除的）
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getEvaluatorsPage(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        logger.info("接收到分页获取评测者请求，页码：{}，每页大小：{}", pageNumber, pageSize);
        
        try {
            // 过滤掉已删除的评测者
            List<Evaluator> allEvaluators = evaluatorRepository.findAll().stream()
                .filter(e -> e.getDeletedAt() == null)
                .toList();
                
            int totalCount = allEvaluators.size();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            
            // 分页处理
            List<Evaluator> pagedEvaluators;
            if (totalCount > 0) {
                int startIndex = pageNumber * pageSize;
                int endIndex = Math.min(startIndex + pageSize, totalCount);
                
                // 确保起始索引不超过列表大小
                if (startIndex >= totalCount) {
                    startIndex = Math.max(0, totalCount - pageSize);
                    endIndex = totalCount;
                    pageNumber = Math.max(0, totalPages - 1); // 调整页码为最后一页
                }
                
                pagedEvaluators = allEvaluators.subList(startIndex, endIndex);
            } else {
                pagedEvaluators = List.of();
            }
            
            // 组装结果
            Map<String, Object> result = new HashMap<>();
            result.put("evaluators", pagedEvaluators);
            result.put("totalCount", totalCount);
            result.put("pageNumber", pageNumber);
            result.put("pageSize", pageSize);
            result.put("totalPages", totalPages);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("分页获取评测者失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "获取评测者列表失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取所有人类评测者（不包括已删除的）
     */
    @GetMapping("/human")
    public ResponseEntity<List<Evaluator>> getAllHumanEvaluators() {
        logger.info("接收到获取所有人类评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.HUMAN).stream()
            .filter(e -> e.getDeletedAt() == null)
            .toList();
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 获取所有AI评测者（不包括已删除的）
     */
    @GetMapping("/ai")
    public ResponseEntity<List<Evaluator>> getAllAiEvaluators() {
        logger.info("接收到获取所有AI评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.AI_MODEL).stream()
            .filter(e -> e.getDeletedAt() == null)
            .toList();
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 根据ID获取评测者（如果已删除则返回404）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Evaluator> getEvaluatorById(@PathVariable Long id) {
        logger.info("接收到获取评测者请求，ID: {}", id);
        
        return evaluatorRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据用户ID获取评测者（不包括已删除的）
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Evaluator> getEvaluatorByUserId(@PathVariable Long userId) {
        logger.info("接收到根据用户ID获取评测者请求，用户ID: {}", userId);
        
        // 查找与该用户关联的评测者
        List<Evaluator> evaluators = evaluatorRepository.findAll();
        Optional<Evaluator> evaluator = evaluators.stream()
            .filter(e -> e.getDeletedAt() == null) // 过滤掉已删除的
            .filter(e -> e.getUser() != null && e.getUser().getId().equals(userId))
            .findFirst();
        
        return evaluator
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建新评测者
     */
    @PostMapping
    public ResponseEntity<Evaluator> createEvaluator(@RequestBody Evaluator evaluator) {
        logger.info("接收到创建评测者请求，ID: {}", evaluator.getId());
        
        if (evaluator.getId() != null) {
            return ResponseEntity.badRequest().build();
        }
        
        // 如果是人类评测者，验证用户ID是否存在
        if (evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN && evaluator.getUser() != null) {
            if (evaluator.getUser().getId() == null || !userRepository.existsById(evaluator.getUser().getId())) {
                return ResponseEntity.badRequest().body(null);
            }
        }
        
        Evaluator savedEvaluator = evaluatorRepository.save(evaluator);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvaluator);
    }
    
    /**
     * 更新评测者信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Evaluator> updateEvaluator(@PathVariable Long id, @RequestBody Evaluator evaluator) {
        logger.info("接收到更新评测者请求，ID: {}", id);
        
        if (!evaluatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        evaluator.setId(id);
        Evaluator updatedEvaluator = evaluatorRepository.save(evaluator);
        return ResponseEntity.ok(updatedEvaluator);
    }
    
    /**
     * 删除评测者
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvaluator(@PathVariable Long id) {
        logger.info("接收到删除评测者请求，ID: {}", id);
        
        if (!evaluatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        evaluatorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 用户注册成为评测者
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAsEvaluator(@RequestBody Map<String, Object> request) {
        logger.info("接收到注册用户成为评测者请求");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未登录"));
            
            if (currentUser == null) {
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 检查用户是否已经是评测者
            List<Evaluator> evaluators = evaluatorRepository.findAll();
            Optional<Evaluator> existingEvaluator = evaluators.stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                .findFirst();
            
            if (existingEvaluator.isPresent()) {
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "用户已经是评测者");
                response.put("evaluator", existingEvaluator.get());
                return ResponseEntity.ok(response);
            }
            
            // 创建新评测者
            String evaluatorName = request.get("name") != null ? 
                    request.get("name").toString() : currentUser.getName() + "的评测账号";
            
            Evaluator evaluator = new Evaluator();
            evaluator.setName(evaluatorName);
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.HUMAN);
            evaluator.setUser(currentUser);
            evaluator.setCreatedByUser(currentUser);
            
            Evaluator savedEvaluator = evaluatorRepository.save(evaluator);
            
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "成功注册为评测者");
            response.put("evaluator", savedEvaluator);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            response.put(ApiConstants.KEY_SUCCESS, false);
            response.put(ApiConstants.KEY_MESSAGE, "注册评测者失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 部分更新评测者信息
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> patchEvaluator(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        logger.info("接收到部分更新评测者请求，ID: {}", id);
        
        try {
            // 检查评测者是否存在
            Optional<Evaluator> evaluatorOpt = evaluatorRepository.findById(id);
            if (evaluatorOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "找不到ID为" + id + "的评测者");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Evaluator evaluator = evaluatorOpt.get();
            boolean changed = false;
            
            // 处理名称更新
            if (updates.containsKey("name")) {
                String newName = (String) updates.get("name");
                if (newName != null && !newName.trim().isEmpty()) {
                    evaluator.setName(newName);
                    changed = true;
                }
            }
            
            // 处理评测者类型更新
            if (updates.containsKey("evaluatorType")) {
                String typeStr = (String) updates.get("evaluatorType");
                if (typeStr != null && !typeStr.trim().isEmpty()) {
                    try {
                        Evaluator.EvaluatorType newType = Evaluator.EvaluatorType.valueOf(typeStr.toUpperCase());
                        evaluator.setEvaluatorType(newType);
                        changed = true;
                    } catch (IllegalArgumentException e) {
                        logger.warn("无效的评测者类型: {}", typeStr);
                        Map<String, Object> response = new HashMap<>();
                        response.put(ApiConstants.KEY_SUCCESS, false);
                        response.put(ApiConstants.KEY_MESSAGE, "无效的评测者类型: " + typeStr);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                }
            }
            
            // 处理用户ID更新
            if (updates.containsKey("userId")) {
                Object userIdObj = updates.get("userId");
                if (userIdObj != null) {
                    Long userId;
                    if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else if (userIdObj instanceof String) {
                        try {
                            userId = Long.parseLong((String) userIdObj);
                        } catch (NumberFormatException e) {
                            Map<String, Object> response = new HashMap<>();
                            response.put(ApiConstants.KEY_SUCCESS, false);
                            response.put(ApiConstants.KEY_MESSAGE, "无效的用户ID: " + userIdObj);
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                        }
                    } else {
                        Map<String, Object> response = new HashMap<>();
                        response.put(ApiConstants.KEY_SUCCESS, false);
                        response.put(ApiConstants.KEY_MESSAGE, "无效的用户ID类型");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                    
                    // 检查用户是否存在
                    if (userId != null) {
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) {
                            Map<String, Object> response = new HashMap<>();
                            response.put(ApiConstants.KEY_SUCCESS, false);
                            response.put(ApiConstants.KEY_MESSAGE, "找不到ID为" + userId + "的用户");
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                        }
                        
                        evaluator.setUser(userOpt.get());
                        changed = true;
                    } else {
                        // 移除用户关联
                        evaluator.setUser(null);
                        changed = true;
                    }
                }
            }
            
            // 处理LLM模型ID更新（如果模型类）
            if (updates.containsKey("llmModelId")) {
                // 这里需要根据实际情况实现LLM模型的更新逻辑
                logger.info("LLM模型ID更新尚未实现");
            }
            
            // 如果有字段被更新，则保存更改
            if (changed) {
                Evaluator updatedEvaluator = evaluatorRepository.save(evaluator);
                
                Map<String, Object> response = new HashMap<>();
                response.put(ApiConstants.KEY_SUCCESS, true);
                response.put(ApiConstants.KEY_MESSAGE, "评测者信息更新成功");
                response.put("evaluator", updatedEvaluator);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "没有提供有效的更新字段");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("更新评测者信息时发生错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(ApiConstants.KEY_SUCCESS, false);
            response.put(ApiConstants.KEY_MESSAGE, "更新评测者信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 按类型分页获取评测者列表（不包括已删除的）
     */
    @GetMapping("/by-type/{evaluatorType}")
    public ResponseEntity<Map<String, Object>> getEvaluatorsByType(
            @PathVariable String evaluatorType,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        logger.info("接收到按类型分页获取评测者请求，类型：{}，页码：{}，每页大小：{}", evaluatorType, pageNumber, pageSize);
        
        try {
            // 验证评测者类型
            Evaluator.EvaluatorType type;
            try {
                type = Evaluator.EvaluatorType.valueOf(evaluatorType.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> response = new HashMap<>();
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "无效的评测者类型: " + evaluatorType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // 获取指定类型的评测者（不包括已删除的）
            List<Evaluator> allEvaluatorsOfType = evaluatorRepository.findByEvaluatorType(type).stream()
                .filter(e -> e.getDeletedAt() == null)
                .toList();
                
            int totalCount = allEvaluatorsOfType.size();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            
            // 分页处理
            List<Evaluator> pagedEvaluators;
            if (totalCount > 0) {
                int startIndex = pageNumber * pageSize;
                int endIndex = Math.min(startIndex + pageSize, totalCount);
                
                // 确保起始索引不超过列表大小
                if (startIndex >= totalCount) {
                    startIndex = Math.max(0, totalCount - pageSize);
                    endIndex = totalCount;
                    pageNumber = Math.max(0, totalPages - 1); // 调整页码为最后一页
                }
                
                pagedEvaluators = allEvaluatorsOfType.subList(startIndex, endIndex);
            } else {
                pagedEvaluators = List.of();
            }
            
            // 组装结果
            Map<String, Object> result = new HashMap<>();
            result.put("evaluators", pagedEvaluators);
            result.put("totalCount", totalCount);
            result.put("pageNumber", pageNumber);
            result.put("pageSize", pageSize);
            result.put("totalPages", totalPages);
            result.put("evaluatorType", type.name());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("按类型分页获取评测者失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "获取评测者列表失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 测试大模型评测员的连通性
     * 
     * @param evaluatorId 评测员ID
     * @return 连通性测试结果
     */
    @GetMapping("/ai/connectivity-test/{evaluatorId}")
    public ResponseEntity<Map<String, Object>> testAiEvaluatorConnectivity(@PathVariable Long evaluatorId) {
        logger.info("接收到测试大模型评测员连通性请求，评测员ID: {}", evaluatorId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取评测员信息
            Optional<Evaluator> evaluatorOpt = evaluatorRepository.findById(evaluatorId);
            
            if (evaluatorOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "未找到指定的评测员");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Evaluator evaluator = evaluatorOpt.get();
            
            // 检查是否为AI评测员
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
                response.put("success", false);
                response.put("message", "指定的评测员不是大模型评测员");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查是否有关联的LLM模型
            if (evaluator.getLlmModel() == null) {
                response.put("success", false);
                response.put("message", "评测员没有关联的大模型");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用服务进行连通性测试
            Map<String, Object> testResult = aiEvaluatorConnectivityService.testConnectivity(evaluatorId);
            
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            logger.error("测试大模型评测员连通性失败", e);
            response.put("success", false);
            response.put("message", "测试连通性时发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("处理评测者请求时发生异常", e);
        
        Map<String, String> response = new HashMap<>();
        response.put("error", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 