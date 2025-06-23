package com.example.demo.service;

import com.example.demo.dto.AnswerQuestionTypePromptDTO;
import com.example.demo.dto.AnswerTagPromptDTO;
import com.example.demo.dto.EvaluationSubjectivePromptDTO;
import com.example.demo.dto.EvaluationTagPromptDTO;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.AnswerTagPrompt;
import com.example.demo.entity.jdbc.EvaluationSubjectivePrompt;
import com.example.demo.entity.jdbc.EvaluationTagPrompt;
import com.example.demo.entity.jdbc.QuestionType;

import java.util.List;
import java.util.Optional;

public interface PromptService {
    
    /**
     * 创建标签提示词
     */
    AnswerTagPrompt createAnswerTagPrompt(AnswerTagPromptDTO dto, Long userId);
    
    /**
     * 更新标签提示词
     */
    AnswerTagPrompt updateAnswerTagPrompt(Long id, AnswerTagPromptDTO dto, Long userId);
    
    /**
     * 获取标签提示词详情
     */
    Optional<AnswerTagPrompt> getAnswerTagPromptById(Long id);
    
    /**
     * 按标签ID获取激活状态的提示词
     */
    List<AnswerTagPrompt> getActiveAnswerTagPromptsByTagId(Long tagId);
    
    /**
     * 获取所有标签提示词
     */
    List<AnswerTagPrompt> getAllAnswerTagPrompts();
    
    /**
     * 删除标签提示词（软删除）
     */
    void deleteAnswerTagPrompt(Long id, Long userId);
    
    /**
     * 创建题型提示词
     */
    AnswerQuestionTypePrompt createAnswerQuestionTypePrompt(AnswerQuestionTypePromptDTO dto, Long userId);
    
    /**
     * 更新题型提示词
     */
    AnswerQuestionTypePrompt updateAnswerQuestionTypePrompt(Long id, AnswerQuestionTypePromptDTO dto, Long userId);
    
    /**
     * 获取题型提示词详情
     */
    Optional<AnswerQuestionTypePrompt> getAnswerQuestionTypePromptById(Long id);
    
    /**
     * 按题型获取激活状态的提示词
     */
    List<AnswerQuestionTypePrompt> getActiveAnswerQuestionTypePromptsByType(QuestionType questionType);
    
    /**
     * 获取所有题型提示词
     */
    List<AnswerQuestionTypePrompt> getAllAnswerQuestionTypePrompts();
    
    /**
     * 删除题型提示词（软删除）
     */
    void deleteAnswerQuestionTypePrompt(Long id, Long userId);
    
    // ===== 评测标签提示词相关方法 =====
    
    /**
     * 创建评测标签提示词
     */
    EvaluationTagPrompt createEvaluationTagPrompt(EvaluationTagPromptDTO dto, Long userId);
    
    /**
     * 更新评测标签提示词
     */
    EvaluationTagPrompt updateEvaluationTagPrompt(Long id, EvaluationTagPromptDTO dto, Long userId);
    
    /**
     * 获取评测标签提示词详情
     */
    Optional<EvaluationTagPrompt> getEvaluationTagPromptById(Long id);
    
    /**
     * 按标签ID获取激活状态的评测提示词
     */
    List<EvaluationTagPrompt> getActiveEvaluationTagPromptsByTagId(Long tagId);
    
    /**
     * 获取所有评测标签提示词
     */
    List<EvaluationTagPrompt> getAllEvaluationTagPrompts();
    
    /**
     * 删除评测标签提示词（软删除）
     */
    void deleteEvaluationTagPrompt(Long id, Long userId);
    
    // ===== 评测主观题提示词相关方法 =====
    
    /**
     * 创建评测主观题提示词
     */
    EvaluationSubjectivePrompt createEvaluationSubjectivePrompt(EvaluationSubjectivePromptDTO dto, Long userId);
    
    /**
     * 更新评测主观题提示词
     */
    EvaluationSubjectivePrompt updateEvaluationSubjectivePrompt(Long id, EvaluationSubjectivePromptDTO dto, Long userId);
    
    /**
     * 获取评测主观题提示词详情
     */
    Optional<EvaluationSubjectivePrompt> getEvaluationSubjectivePromptById(Long id);
    
    /**
     * 获取所有激活状态的评测主观题提示词
     */
    List<EvaluationSubjectivePrompt> getActiveEvaluationSubjectivePrompts();
    
    /**
     * 获取所有评测主观题提示词
     */
    List<EvaluationSubjectivePrompt> getAllEvaluationSubjectivePrompts();
    
    /**
     * 删除评测主观题提示词（软删除）
     */
    void deleteEvaluationSubjectivePrompt(Long id, Long userId);
} 