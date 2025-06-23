package com.example.demo.service.impl;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.CrowdsourcedAnswerDTO;
import com.example.demo.entity.jdbc.CrowdsourcedAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.CrowdsourcedAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.CrowdsourcedAnswerService;

@Service
public class CrowdsourcedAnswerServiceImpl implements CrowdsourcedAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcedAnswerServiceImpl.class);
    
    @Autowired
    private CrowdsourcedAnswerRepository crowdsourcedAnswerRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable) {
        return crowdsourcedAnswerRepository.findByStandardQuestionId(standardQuestionId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable) {
        return crowdsourcedAnswerRepository.findByUserId(userId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByStatus(String status, Pageable pageable) {
        try {
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            return crowdsourcedAnswerRepository.findByQualityReviewStatus(reviewStatus, pageable)
                .map(this::convertToDTO);
        } catch (IllegalArgumentException e) {
            logger.error("无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        }
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByQuestionIdAndStatus(
            Long standardQuestionId, String status, Pageable pageable) {
        try {
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            return crowdsourcedAnswerRepository
                .findByStandardQuestionIdAndQualityReviewStatus(standardQuestionId, reviewStatus, pageable)
                .map(this::convertToDTO);
        } catch (IllegalArgumentException e) {
            logger.error("无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        }
    }
    
    @Override
    @Transactional
    public CrowdsourcedAnswerDTO createCrowdsourcedAnswer(CrowdsourcedAnswerDTO answerDTO) {
        logger.debug("开始创建众包回答 - 标准问题ID: {}, 用户ID: {}, 任务批次ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId(), answerDTO.getTaskBatchId());
        
        // 验证必填字段
        if (answerDTO.getStandardQuestionId() == null) {
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        if (answerDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getAnswerText())) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        
        // 查找标准问题
        StandardQuestion standardQuestion = standardQuestionRepository
            .findById(answerDTO.getStandardQuestionId())
            .orElseThrow(() -> {
                logger.error("创建众包回答失败 - 标准问题不存在: {}", answerDTO.getStandardQuestionId());
                return new IllegalArgumentException("标准问题不存在");
            });
        
        // 查找用户
        User user = userRepository.findById(answerDTO.getUserId())
            .orElseThrow(() -> {
                logger.error("创建众包回答失败 - 用户不存在: {}", answerDTO.getUserId());
                return new IllegalArgumentException("用户不存在");
            });
        
        // 检查是否已存在相同组合的记录（防止重复提交）
        if (answerDTO.getTaskBatchId() != null) {
            boolean exists = crowdsourcedAnswerRepository.existsByStandardQuestionIdAndUserIdAndTaskBatchId(
                answerDTO.getStandardQuestionId(), 
                answerDTO.getUserId(),
                answerDTO.getTaskBatchId()
            );
            
            if (exists) {
                String errorMsg = String.format("用户(ID:%d)已经在任务批次(ID:%d)中为标准问题(ID:%d)提交了回答", 
                    answerDTO.getUserId(), answerDTO.getTaskBatchId(), answerDTO.getStandardQuestionId());
                logger.error("创建众包回答失败 - {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }
        
        // 创建众包回答实体
        CrowdsourcedAnswer answer = new CrowdsourcedAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setUser(user);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setSubmissionTime(answerDTO.getSubmissionTime() != null ? 
            answerDTO.getSubmissionTime() : java.time.LocalDateTime.now());
        answer.setTaskBatchId(answerDTO.getTaskBatchId());
        answer.setQualityReviewStatus(CrowdsourcedAnswer.QualityReviewStatus.PENDING);
        
        // 保存并返回
        try {
            CrowdsourcedAnswer savedAnswer = crowdsourcedAnswerRepository.save(answer);
            logger.info("众包回答创建成功 - ID: {}, 问题ID: {}, 用户ID: {}, 任务批次ID: {}", 
                savedAnswer.getId(), savedAnswer.getStandardQuestion().getId(), 
                savedAnswer.getUser().getId(), savedAnswer.getTaskBatchId());
            return convertToDTO(savedAnswer);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 处理唯一约束冲突
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && 
                e.getMessage().contains("standard_question_id")) {
                String errorMsg = String.format("用户(ID:%d)已经在任务批次中为标准问题(ID:%d)提交了回答，不能重复提交", 
                    answerDTO.getUserId(), answerDTO.getStandardQuestionId());
                logger.error("创建众包回答失败 - 唯一约束冲突: {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            logger.error("创建众包回答失败 - 数据完整性错误", e);
            throw new RuntimeException("创建众包回答失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("保存众包回答失败", e);
            throw new RuntimeException("保存众包回答失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public CrowdsourcedAnswerDTO reviewAnswer(Long answerId, Long reviewerUserId, String status, String feedback) {
        logger.debug("开始审核众包回答 - 回答ID: {}, 审核者ID: {}, 状态: {}", 
            answerId, reviewerUserId, status);
        
        // 验证必填字段
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (reviewerUserId == null) {
            throw new IllegalArgumentException("审核者ID不能为空");
        }
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("审核状态不能为空");
        }
        
        // 查找众包回答
        CrowdsourcedAnswer answer = crowdsourcedAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("审核众包回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 查找审核者
        User reviewer = userRepository.findById(reviewerUserId)
            .orElseThrow(() -> {
                logger.error("审核众包回答失败 - 审核者不存在: {}", reviewerUserId);
                return new IllegalArgumentException("审核者不存在");
            });
        
        try {
            // 更新审核状态
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            answer.setQualityReviewStatus(reviewStatus);
            answer.setReviewedByUser(reviewer);
            answer.setReviewTime(LocalDateTime.now());
            answer.setReviewFeedback(feedback);
            
            // 保存并返回
            CrowdsourcedAnswer savedAnswer = crowdsourcedAnswerRepository.save(answer);
            logger.info("众包回答审核成功 - ID: {}, 审核者: {}, 状态: {}", 
                savedAnswer.getId(), reviewer.getId(), reviewStatus);
            return convertToDTO(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("审核众包回答失败 - 无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        } catch (Exception e) {
            logger.error("保存审核结果失败", e);
            throw new RuntimeException("保存审核结果失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public CrowdsourcedAnswerDTO updateCrowdsourcedAnswer(Long answerId, CrowdsourcedAnswerDTO answerDTO) {
        logger.debug("开始修改众包回答 - 回答ID: {}, 用户ID: {}", answerId, answerDTO.getUserId());
        
        // 验证必填字段
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getAnswerText())) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        
        // 查找众包回答
        CrowdsourcedAnswer answer = crowdsourcedAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("修改众包回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 检查权限 - 只有创建者可以修改自己的回答
        if (!answer.getUser().getId().equals(answerDTO.getUserId())) {
            String errorMsg = String.format("用户(ID:%d)无权修改其他用户(ID:%d)的回答", 
                answerDTO.getUserId(), answer.getUser().getId());
            logger.error("修改众包回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // 检查回答状态 - 只有PENDING状态的回答可以修改
        if (answer.getQualityReviewStatus() != CrowdsourcedAnswer.QualityReviewStatus.PENDING) {
            String errorMsg = String.format("已经被审核的回答(状态:%s)不能修改", 
                answer.getQualityReviewStatus().name());
            logger.error("修改众包回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            // 更新回答内容
            answer.setAnswerText(answerDTO.getAnswerText());
            
            // 保存并返回
            CrowdsourcedAnswer savedAnswer = crowdsourcedAnswerRepository.save(answer);
            logger.info("众包回答修改成功 - ID: {}, 用户ID: {}", 
                savedAnswer.getId(), savedAnswer.getUser().getId());
            return convertToDTO(savedAnswer);
        } catch (Exception e) {
            logger.error("保存修改的众包回答失败", e);
            throw new RuntimeException("保存修改的众包回答失败: " + e.getMessage());
        }
    }
    
    // 将实体转换为DTO
    private CrowdsourcedAnswerDTO convertToDTO(CrowdsourcedAnswer answer) {
        CrowdsourcedAnswerDTO dto = new CrowdsourcedAnswerDTO();
        dto.setId(answer.getId());
        dto.setStandardQuestionId(answer.getStandardQuestion().getId());
        dto.setUserId(answer.getUser().getId());
        dto.setAnswerText(answer.getAnswerText());
        dto.setSubmissionTime(answer.getSubmissionTime());
        dto.setQualityReviewStatus(answer.getQualityReviewStatus().name());
        
        if (answer.getReviewedByUser() != null) {
            dto.setReviewedByUserId(answer.getReviewedByUser().getId());
            dto.setReviewerUsername(answer.getReviewedByUser().getUsername());
        }
        
        dto.setReviewTime(answer.getReviewTime());
        dto.setReviewFeedback(answer.getReviewFeedback());
        dto.setUserUsername(answer.getUser().getUsername());
        
        return dto;
    }
    
    @Override
    @Transactional
    public boolean deleteCrowdsourcedAnswer(Long answerId, Long userId) {
        logger.debug("开始删除众包回答 - 回答ID: {}, 操作用户ID: {}", answerId, userId);
        
        // 验证参数
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        // 查找众包回答
        CrowdsourcedAnswer answer = crowdsourcedAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("删除众包回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 检查权限 - 只有创建者可以删除自己的回答
        if (!answer.getUser().getId().equals(userId)) {
            String errorMsg = String.format("用户(ID:%d)无权删除其他用户(ID:%d)的回答", 
                userId, answer.getUser().getId());
            logger.error("删除众包回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // 检查回答状态 - 只有PENDING状态的回答可以删除
        if (answer.getQualityReviewStatus() != CrowdsourcedAnswer.QualityReviewStatus.PENDING) {
            String errorMsg = String.format("已经被审核的回答(状态:%s)不能删除", 
                answer.getQualityReviewStatus().name());
            logger.error("删除众包回答失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            // 删除回答
            crowdsourcedAnswerRepository.delete(answer);
            logger.info("众包回答删除成功 - ID: {}, 用户ID: {}", answerId, userId);
            return true;
        } catch (Exception e) {
            logger.error("删除众包回答失败", e);
            throw new RuntimeException("删除众包回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAllAnswers(Pageable pageable) {
        logger.debug("获取所有众包回答，分页参数: {}", pageable);
        
        try {
            return crowdsourcedAnswerRepository.findAll(pageable)
                .map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取所有众包回答失败", e);
            throw new RuntimeException("获取所有众包回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getPendingAnswers(Pageable pageable) {
        logger.debug("获取所有未审核的众包回答，分页参数: {}", pageable);
        
        try {
            return crowdsourcedAnswerRepository
                .findByQualityReviewStatus(CrowdsourcedAnswer.QualityReviewStatus.PENDING, pageable)
                .map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取未审核众包回答失败", e);
            throw new RuntimeException("获取未审核众包回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersReviewedByUser(Long reviewedByUserId, Pageable pageable) {
        logger.debug("获取用户(ID:{})审核过的众包回答，分页参数: {}", reviewedByUserId, pageable);
        
        if (reviewedByUserId == null) {
            throw new IllegalArgumentException("审核者用户ID不能为空");
        }
        
        try {
            // 确认用户存在
            userRepository.findById(reviewedByUserId)
                .orElseThrow(() -> {
                    logger.error("获取用户审核过的众包回答失败 - 用户不存在: {}", reviewedByUserId);
                    return new IllegalArgumentException("用户不存在");
                });
            
            return crowdsourcedAnswerRepository.findByReviewedByUserId(reviewedByUserId, pageable)
                .map(this::convertToDTO);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取用户审核过的众包回答失败", e);
            throw new RuntimeException("获取用户审核过的众包回答失败: " + e.getMessage());
        }
    }
} 