package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.CrowdsourcedAnswerDTO;

public interface CrowdsourcedAnswerService {
    // 根据标准问题ID获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable);
    
    // 根据用户ID获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable);
    
    // 根据审核状态获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByStatus(String status, Pageable pageable);
    
    // 根据标准问题ID和审核状态获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByQuestionIdAndStatus(
        Long standardQuestionId, String status, Pageable pageable);
        
    // 创建众包回答
    CrowdsourcedAnswerDTO createCrowdsourcedAnswer(CrowdsourcedAnswerDTO answerDTO);
    
    // 修改众包回答
    CrowdsourcedAnswerDTO updateCrowdsourcedAnswer(Long answerId, CrowdsourcedAnswerDTO answerDTO);
    
    // 审核众包回答
    CrowdsourcedAnswerDTO reviewAnswer(Long answerId, Long reviewerUserId, String status, String feedback);
    
    /**
     * 删除众包回答
     * @param answerId 回答ID
     * @param userId 操作用户ID（用于权限验证）
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果回答不存在
     * @throws IllegalStateException 如果用户无权删除或回答状态不允许删除
     */
    boolean deleteCrowdsourcedAnswer(Long answerId, Long userId);
    
    /**
     * 获取所有众包回答（分页）
     * @param pageable 分页参数
     * @return 分页众包回答列表
     */
    Page<CrowdsourcedAnswerDTO> getAllAnswers(Pageable pageable);
    
    /**
     * 获取所有未审核的众包回答（分页）
     * @param pageable 分页参数
     * @return 分页未审核众包回答列表
     */
    Page<CrowdsourcedAnswerDTO> getPendingAnswers(Pageable pageable);
    
    /**
     * 获取由特定用户审核过的众包回答（分页）
     * @param reviewedByUserId 审核者用户ID
     * @param pageable 分页参数
     * @return 分页的众包回答列表
     */
    Page<CrowdsourcedAnswerDTO> getAnswersReviewedByUser(Long reviewedByUserId, Pageable pageable);
} 