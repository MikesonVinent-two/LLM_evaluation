package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.dto.StandardAnswerDTO;

public interface StandardAnswerService {
    
    /**
     * 创建或更新标准答案
     * @param answerDTO 标准答案数据传输对象
     * @param userId 操作用户ID
     * @return 根据问题类型返回相应的标准答案实体
     */
    Object createOrUpdateStandardAnswer(StandardAnswerDTO answerDTO, Long userId);
    
    /**
     * 更新标准答案
     * @param standardQuestionId 标准问题ID
     * @param answerDTO 标准答案数据传输对象
     * @param userId 操作用户ID
     * @return 更新后的标准答案实体
     * @throws IllegalArgumentException 如果标准问题不存在或问题类型不匹配
     * @throws IllegalStateException 如果标准答案不存在
     */
    Object updateStandardAnswer(Long standardQuestionId, StandardAnswerDTO answerDTO, Long userId);
    
    /**
     * 获取标准问题的标准答案
     * @param standardQuestionId 标准问题ID
     * @return 标准答案实体（可能是客观题、简单题或主观题答案）
     */
    Object getStandardAnswer(Long standardQuestionId);
    
    /**
     * 删除标准答案（软删除）
     * @param standardQuestionId 标准问题ID
     * @param userId 操作用户ID
     */
    void deleteStandardAnswer(Long standardQuestionId, Long userId);

    /**
     * 获取标准答案的历史记录
     * @param answerId 标准答案ID
     * @return 历史记录列表
     */
    List<Map<String, Object>> getAnswerHistory(Long answerId);

    /**
     * 获取标准答案的版本树
     * @param answerId 标准答案ID
     * @return 版本树数据
     */
    Map<String, Object> getAnswerVersionTree(Long answerId);

    /**
     * 比较两个版本的标准答案
     * @param baseVersionId 基准版本ID
     * @param compareVersionId 比较版本ID
     * @return 比较结果
     */
    Map<String, Object> compareAnswerVersions(Long baseVersionId, Long compareVersionId);

    /**
     * 回滚标准答案到指定版本
     * @param versionId 要回滚到的版本ID
     * @param userId 操作用户ID
     * @param commitMessage 回滚说明信息
     * @return 回滚后的标准答案
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果版本冲突
     */
    Object rollbackAnswer(Long versionId, Long userId, String commitMessage);
} 