package com.example.demo.service.impl;

import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.EvaluatorRepository;
import com.example.demo.repository.jdbc.LlmModelRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.EvaluatorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EvaluatorServiceImpl implements EvaluatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluatorServiceImpl.class);
    
    private final EvaluatorRepository evaluatorRepository;
    private final UserRepository userRepository;
    private final LlmModelRepository llmModelRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EvaluatorServiceImpl(EvaluatorRepository evaluatorRepository, 
                              UserRepository userRepository,
                              LlmModelRepository llmModelRepository,
                              ObjectMapper objectMapper) {
        this.evaluatorRepository = evaluatorRepository;
        this.userRepository = userRepository;
        this.llmModelRepository = llmModelRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public Evaluator createHumanEvaluator(Long userId, String name, String description, 
                                        String expertiseAreas, Long createdByUserId) {
        logger.info("创建人类评测者，用户ID: {}, 名称: {}", userId, name);
        
        try {
            // 验证用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户: " + userId));
            
            // 验证创建者是否存在
            User createdByUser = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的创建者: " + createdByUserId));
            
            // 检查是否已存在同名评测者
            if (evaluatorRepository.findByName(name).isPresent()) {
                throw new IllegalArgumentException("已存在同名评测者: " + name);
            }
            
            // 创建评测者
            Evaluator evaluator = new Evaluator();
            evaluator.setUser(user);
            evaluator.setName(name);
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.HUMAN);
            evaluator.setCreatedByUser(createdByUser);
            evaluator.setCreatedAt(LocalDateTime.now());
            
            // 保存评测者
            evaluator = evaluatorRepository.save(evaluator);
            
            logger.info("人类评测者创建成功，ID: {}, 名称: {}", evaluator.getId(), evaluator.getName());
            return evaluator;
            
        } catch (Exception e) {
            logger.error("创建人类评测者失败", e);
            throw new RuntimeException("创建人类评测者失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public Evaluator createAiEvaluator(Long llmModelId, String name, String description,
                                     String apiUrl, String apiKey, String apiType, String modelName,
                                     String modelParameters, String evaluationPromptTemplate,
                                     Long createdByUserId) {
        logger.info("创建AI评测者，模型ID: {}, 名称: {}", llmModelId, name);
        
        try {
            // 验证LLM模型是否存在
            LlmModel llmModel = llmModelRepository.findById(llmModelId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM模型: " + llmModelId));
            
            // 验证创建者是否存在
            User createdByUser = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的创建者: " + createdByUserId));
            
            // 检查是否已存在同名评测者
            if (evaluatorRepository.findByName(name).isPresent()) {
                throw new IllegalArgumentException("已存在同名评测者: " + name);
            }
            
            // 创建评测者
            Evaluator evaluator = new Evaluator();
            evaluator.setLlmModel(llmModel);
            evaluator.setName(name);
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.AI_MODEL);
            evaluator.setCreatedByUser(createdByUser);
            evaluator.setCreatedAt(LocalDateTime.now());
            
            // 保存评测者
            evaluator = evaluatorRepository.save(evaluator);
            
            logger.info("AI评测者创建成功，ID: {}, 名称: {}", evaluator.getId(), evaluator.getName());
            return evaluator;
            
        } catch (Exception e) {
            logger.error("创建AI评测者失败", e);
            throw new RuntimeException("创建AI评测者失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<Evaluator> getEvaluatorById(Long id) {
        logger.debug("根据ID查找评测者，ID: {}", id);
        return evaluatorRepository.findById(id);
    }
    
    @Override
    public Optional<Evaluator> getEvaluatorByName(String name) {
        logger.debug("根据名称查找评测者，名称: {}", name);
        return evaluatorRepository.findByName(name);
    }
    
    @Override
    public Optional<Evaluator> getEvaluatorByUserId(Long userId) {
        logger.debug("根据用户ID查找评测者，用户ID: {}", userId);
        
        // 查找关联到指定用户的评测者
        List<Evaluator> evaluators = evaluatorRepository.findAll();
        return evaluators.stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(userId))
                .findFirst();
    }
    
    @Override
    public List<Evaluator> getAllEvaluators() {
        logger.debug("获取所有评测者");
        return evaluatorRepository.findAll();
    }
    
    @Override
    public List<Evaluator> getAllHumanEvaluators() {
        logger.debug("获取所有人类评测者");
        return evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.HUMAN);
    }
    
    @Override
    public List<Evaluator> getAllAiEvaluators() {
        logger.debug("获取所有AI评测者");
        return evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.AI_MODEL);
    }
    
    @Override
    @Transactional
    public Evaluator updateEvaluator(Evaluator evaluator) {
        logger.info("更新评测者，ID: {}, 名称: {}", evaluator.getId(), evaluator.getName());
        
        // 验证评测者是否存在
        if (!evaluatorRepository.existsById(evaluator.getId())) {
            throw new EntityNotFoundException("找不到指定的评测者: " + evaluator.getId());
        }
        
        // 保存评测者
        return evaluatorRepository.save(evaluator);
    }
    
    @Override
    @Transactional
    public void deleteEvaluator(Long id) {
        logger.info("删除评测者，ID: {}", id);
        
        // 验证评测者是否存在
        Evaluator evaluator = evaluatorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + id));
        
        // 软删除（设置删除时间）
        evaluator.setDeletedAt(LocalDateTime.now());
        evaluatorRepository.save(evaluator);
    }
    
    @Override
    @Transactional
    public Evaluator registerUserAsEvaluator(Long userId, String name, String description, String expertiseAreas) {
        logger.info("将用户注册为评测者，用户ID: {}, 名称: {}", userId, name);
        
        try {
            // 验证用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户: " + userId));
            
            // 检查用户是否已经是评测者
            if (getEvaluatorByUserId(userId).isPresent()) {
                throw new IllegalStateException("该用户已经是评测者: " + userId);
            }
            
            // 检查是否已存在同名评测者
            if (evaluatorRepository.findByName(name).isPresent()) {
                throw new IllegalArgumentException("已存在同名评测者: " + name);
            }
            
            // 创建评测者
            Evaluator evaluator = new Evaluator();
            evaluator.setUser(user);
            evaluator.setName(name);
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.HUMAN);
            evaluator.setCreatedByUser(user); // 自己创建自己的评测者账号
            evaluator.setCreatedAt(LocalDateTime.now());
            
            // 保存评测者
            evaluator = evaluatorRepository.save(evaluator);
            
            logger.info("用户成功注册为评测者，评测者ID: {}, 名称: {}", evaluator.getId(), evaluator.getName());
            return evaluator;
            
        } catch (Exception e) {
            logger.error("将用户注册为评测者失败", e);
            throw new RuntimeException("将用户注册为评测者失败: " + e.getMessage(), e);
        }
    }
} 