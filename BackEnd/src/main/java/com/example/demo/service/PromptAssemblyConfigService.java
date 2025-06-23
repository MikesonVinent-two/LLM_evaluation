package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.AnswerPromptAssemblyConfigDTO;
import com.example.demo.dto.EvaluationPromptAssemblyConfigDTO;

/**
 * 提示词组装配置服务接口
 */
public interface PromptAssemblyConfigService {
    
    /**
     * 创建回答提示词组装配置
     * 
     * @param configDTO 配置DTO
     * @param userId 创建者用户ID
     * @return 创建后的配置DTO
     */
    AnswerPromptAssemblyConfigDTO createAnswerConfig(AnswerPromptAssemblyConfigDTO configDTO, Long userId);
    
    /**
     * 创建评测提示词组装配置
     * 
     * @param configDTO 配置DTO
     * @param userId 创建者用户ID
     * @return 创建后的配置DTO
     */
    EvaluationPromptAssemblyConfigDTO createEvaluationConfig(EvaluationPromptAssemblyConfigDTO configDTO, Long userId);
    
    /**
     * 获取回答提示词组装配置
     * 
     * @param configId 配置ID
     * @return 配置DTO
     */
    AnswerPromptAssemblyConfigDTO getAnswerConfig(Long configId);
    
    /**
     * 获取评测提示词组装配置
     * 
     * @param configId 配置ID
     * @return 配置DTO
     */
    EvaluationPromptAssemblyConfigDTO getEvaluationConfig(Long configId);
    
    /**
     * 获取所有活跃的回答提示词组装配置
     * 
     * @return 配置DTO列表
     */
    List<AnswerPromptAssemblyConfigDTO> getAllActiveAnswerConfigs();
    
    /**
     * 获取所有活跃的回答提示词组装配置（分页版本）
     * 
     * @param pageable 分页参数
     * @return 配置DTO分页列表
     */
    Page<AnswerPromptAssemblyConfigDTO> getAllActiveAnswerConfigsPageable(Pageable pageable);
    
    /**
     * 获取所有活跃的评测提示词组装配置
     * 
     * @return 配置DTO列表
     */
    List<EvaluationPromptAssemblyConfigDTO> getAllActiveEvaluationConfigs();
    
    /**
     * 获取用户创建的回答提示词组装配置
     * 
     * @param userId 用户ID
     * @return 配置DTO列表
     */
    List<AnswerPromptAssemblyConfigDTO> getAnswerConfigsByUser(Long userId);
    
    /**
     * 获取用户创建的评测提示词组装配置
     * 
     * @param userId 用户ID
     * @return 配置DTO列表
     */
    List<EvaluationPromptAssemblyConfigDTO> getEvaluationConfigsByUser(Long userId);
} 