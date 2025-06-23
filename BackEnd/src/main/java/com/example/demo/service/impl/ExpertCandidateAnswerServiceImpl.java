package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.ExpertCandidateAnswerDTO;
import com.example.demo.entity.jdbc.ExpertCandidateAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ExpertCandidateAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.ExpertCandidateAnswerService;

@Service
public class ExpertCandidateAnswerServiceImpl implements ExpertCandidateAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpertCandidateAnswerServiceImpl.class);
    
    @Autowired
    private ExpertCandidateAnswerRepository expertCandidateAnswerRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public ExpertCandidateAnswerDTO createExpertCandidateAnswer(ExpertCandidateAnswerDTO answerDTO) {
        logger.debug("开始创建专家候选回答 - 标准问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        // 验证必填字段
        if (answerDTO.getStandardQuestionId() == null) {
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        if (answerDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getCandidateAnswerText())) {
            throw new IllegalArgumentException("答案内容不能为空");
        }
        
        // 查找标准问题
        StandardQuestion standardQuestion = standardQuestionRepository
            .findById(answerDTO.getStandardQuestionId())
            .orElseThrow(() -> {
                logger.error("创建专家候选回答失败 - 标准问题不存在: {}", answerDTO.getStandardQuestionId());
                return new IllegalArgumentException("标准问题不存在");
            });
        
        // 查找用户
        User user = userRepository.findById(answerDTO.getUserId())
            .orElseThrow(() -> {
                logger.error("创建专家候选回答失败 - 用户不存在: {}", answerDTO.getUserId());
                return new IllegalArgumentException("用户不存在");
            });
        
        // 创建专家候选回答实体
        ExpertCandidateAnswer answer = new ExpertCandidateAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setUser(user);
        answer.setCandidateAnswerText(answerDTO.getCandidateAnswerText());
        answer.setSubmissionTime(answerDTO.getSubmissionTime() != null ? 
            answerDTO.getSubmissionTime() : java.time.LocalDateTime.now());
        
        // 保存并返回
        try {
            ExpertCandidateAnswer savedAnswer = expertCandidateAnswerRepository.save(answer);
            logger.info("专家候选回答创建成功 - ID: {}, 问题ID: {}, 用户ID: {}", 
                savedAnswer.getId(), savedAnswer.getStandardQuestion().getId(), savedAnswer.getUser().getId());
            return convertToDTO(savedAnswer);
        } catch (Exception e) {
            logger.error("保存专家候选回答失败", e);
            throw new RuntimeException("保存专家候选回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable) {
        return expertCandidateAnswerRepository.findByStandardQuestionId(standardQuestionId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable) {
        return expertCandidateAnswerRepository.findByUserId(userId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    @Transactional
    public ExpertCandidateAnswerDTO updateQualityScoreAndFeedback(
            Long answerId, Integer qualityScore, String feedback) {
        ExpertCandidateAnswer answer = expertCandidateAnswerRepository.findById(answerId)
            .orElseThrow(() -> new IllegalArgumentException("专家候选回答不存在"));
        
        answer.setQualityScore(qualityScore);
        answer.setFeedback(feedback);
        
        ExpertCandidateAnswer updatedAnswer = expertCandidateAnswerRepository.save(answer);
        return convertToDTO(updatedAnswer);
    }
    
    @Override
    @Transactional
    public ExpertCandidateAnswerDTO updateExpertCandidateAnswer(
            Long answerId, Long userId, String answerText) {
        logger.debug("开始修改专家候选回答 - 回答ID: {}, 用户ID: {}", answerId, userId);
        
        // 验证参数
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(answerText)) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        
        // 查找专家回答
        ExpertCandidateAnswer answer = expertCandidateAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("修改专家候选回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 检查权限 - 只有创建者可以修改自己的回答
        if (!answer.getUser().getId().equals(userId)) {
            String errorMsg = String.format("用户(ID:%d)无权修改其他用户(ID:%d)的回答", 
                userId, answer.getUser().getId());
            logger.error("修改专家候选回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            // 更新回答内容
            answer.setCandidateAnswerText(answerText);
            
            // 保存并返回
            ExpertCandidateAnswer savedAnswer = expertCandidateAnswerRepository.save(answer);
            logger.info("专家候选回答修改成功 - ID: {}, 用户ID: {}", 
                savedAnswer.getId(), savedAnswer.getUser().getId());
            return convertToDTO(savedAnswer);
        } catch (Exception e) {
            logger.error("保存修改的专家候选回答失败", e);
            throw new RuntimeException("保存修改的专家候选回答失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public boolean deleteExpertCandidateAnswer(Long answerId, Long userId) {
        logger.debug("开始删除专家候选回答 - 回答ID: {}, 操作用户ID: {}", answerId, userId);
        
        // 验证参数
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        // 查找专家回答
        ExpertCandidateAnswer answer = expertCandidateAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("删除专家候选回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 检查权限 - 只有创建者可以删除自己的回答
        if (!answer.getUser().getId().equals(userId)) {
            String errorMsg = String.format("用户(ID:%d)无权删除其他用户(ID:%d)的回答", 
                userId, answer.getUser().getId());
            logger.error("删除专家候选回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            // 删除回答
            expertCandidateAnswerRepository.delete(answer);
            logger.info("专家候选回答删除成功 - ID: {}, 用户ID: {}", answerId, userId);
            return true;
        } catch (Exception e) {
            logger.error("删除专家候选回答失败", e);
            throw new RuntimeException("删除专家候选回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getAllAnswers(Pageable pageable) {
        logger.debug("获取所有专家候选回答，分页参数: {}", pageable);
        return expertCandidateAnswerRepository.findAll(pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getUnratedAnswers(Pageable pageable) {
        logger.debug("获取所有未评分专家候选回答，分页参数: {}", pageable);
        return expertCandidateAnswerRepository.findUnrated(pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getRatedAnswersByUserId(Long userId, Pageable pageable) {
        logger.debug("获取用户ID为{}的已评分专家候选回答，分页参数: {}", userId, pageable);
        // 验证用户是否存在
        userRepository.findById(userId)
            .orElseThrow(() -> {
                logger.error("获取已评分专家候选回答失败 - 用户不存在: {}", userId);
                return new IllegalArgumentException("用户不存在");
            });
            
        return expertCandidateAnswerRepository.findRatedByUser(userId, pageable)
            .map(this::convertToDTO);
    }
    
    // 将实体转换为DTO
    private ExpertCandidateAnswerDTO convertToDTO(ExpertCandidateAnswer answer) {
        ExpertCandidateAnswerDTO dto = new ExpertCandidateAnswerDTO();
        dto.setId(answer.getId());
        dto.setStandardQuestionId(answer.getStandardQuestion().getId());
        dto.setUserId(answer.getUser().getId());
        dto.setCandidateAnswerText(answer.getCandidateAnswerText());
        dto.setSubmissionTime(answer.getSubmissionTime());
        dto.setQualityScore(answer.getQualityScore());
        dto.setFeedback(answer.getFeedback());
        dto.setUserUsername(answer.getUser().getUsername());
        
        return dto;
    }
} 