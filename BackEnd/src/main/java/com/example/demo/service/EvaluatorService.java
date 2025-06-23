package com.example.demo.service;

import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.User;

import java.util.List;
import java.util.Optional;

/**
 * 评测者服务接口
 */
public interface EvaluatorService {
    
    /**
     * 创建人类评测者
     * 
     * @param userId 用户ID
     * @param name 评测者名称
     * @param description 描述 (不再使用，保留参数以兼容现有代码)
     * @param expertiseAreas 专业领域 (不再使用，保留参数以兼容现有代码)
     * @param createdByUserId 创建者用户ID
     * @return 创建的评测者
     */
    Evaluator createHumanEvaluator(Long userId, String name, String description, 
                                  String expertiseAreas, Long createdByUserId);
    
    /**
     * 创建AI评测者
     * 
     * @param llmModelId LLM模型ID
     * @param name 评测者名称
     * @param description 描述 (不再使用，保留参数以兼容现有代码)
     * @param apiUrl API URL (不再使用，保留参数以兼容现有代码)
     * @param apiKey API密钥 (不再使用，保留参数以兼容现有代码)
     * @param apiType API类型 (不再使用，保留参数以兼容现有代码)
     * @param modelName 模型名称 (不再使用，保留参数以兼容现有代码)
     * @param modelParameters 模型参数 (不再使用，保留参数以兼容现有代码)
     * @param evaluationPromptTemplate 评测提示词模板 (不再使用，保留参数以兼容现有代码)
     * @param createdByUserId 创建者用户ID
     * @return 创建的评测者
     */
    Evaluator createAiEvaluator(Long llmModelId, String name, String description,
                               String apiUrl, String apiKey, String apiType, String modelName,
                               String modelParameters, String evaluationPromptTemplate,
                               Long createdByUserId);
    
    /**
     * 根据ID查找评测者
     * 
     * @param id 评测者ID
     * @return 评测者
     */
    Optional<Evaluator> getEvaluatorById(Long id);
    
    /**
     * 根据名称查找评测者
     * 
     * @param name 评测者名称
     * @return 评测者
     */
    Optional<Evaluator> getEvaluatorByName(String name);
    
    /**
     * 根据用户ID查找评测者
     * 
     * @param userId 用户ID
     * @return 评测者
     */
    Optional<Evaluator> getEvaluatorByUserId(Long userId);
    
    /**
     * 获取所有评测者
     * 
     * @return 评测者列表
     */
    List<Evaluator> getAllEvaluators();
    
    /**
     * 获取所有人类评测者
     * 
     * @return 评测者列表
     */
    List<Evaluator> getAllHumanEvaluators();
    
    /**
     * 获取所有AI评测者
     * 
     * @return 评测者列表
     */
    List<Evaluator> getAllAiEvaluators();
    
    /**
     * 更新评测者信息
     * 
     * @param evaluator 评测者
     * @return 更新后的评测者
     */
    Evaluator updateEvaluator(Evaluator evaluator);
    
    /**
     * 删除评测者
     * 
     * @param id 评测者ID
     */
    void deleteEvaluator(Long id);
    
    /**
     * 将用户注册为评测者
     * 
     * @param userId 用户ID
     * @param name 评测者名称
     * @param description 描述 (不再使用，保留参数以兼容现有代码)
     * @param expertiseAreas 专业领域 (不再使用，保留参数以兼容现有代码)
     * @return 创建的评测者
     */
    Evaluator registerUserAsEvaluator(Long userId, String name, String description, String expertiseAreas);
} 