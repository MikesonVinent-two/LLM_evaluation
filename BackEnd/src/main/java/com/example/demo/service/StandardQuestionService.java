package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.TagOperationDTO;

public interface StandardQuestionService {
    /**
     * 创建标准问题
     * @param questionDTO 问题数据传输对象
     * @param userId 创建用户ID
     * @return 创建后的问题DTO
     */
    StandardQuestionDTO createStandardQuestion(StandardQuestionDTO questionDTO, Long userId);

    /**
     * 更新标准问题
     * @param questionId 要更新的问题ID
     * @param questionDTO 更新的问题数据
     * @param userId 操作用户ID
     * @return 更新后的问题DTO
     */
    StandardQuestionDTO updateStandardQuestion(Long questionId, StandardQuestionDTO questionDTO, Long userId);

    /**
     * 获取问题的修改历史
     * @param questionId 问题ID
     * @return 问题的历史版本列表
     */
    List<QuestionHistoryDTO> getQuestionHistory(Long questionId);

    /**
     * 获取问题的版本树
     * @param questionId 问题ID
     * @return 问题的版本树结构
     */
    List<QuestionHistoryDTO> getVersionTree(Long questionId);
    
    /**
     * 获取所有标准问题（分页）
     * @param pageable 分页参数
     * @return 标准问题分页列表
     */
    Page<StandardQuestionDTO> findAllStandardQuestions(Pageable pageable);
    
    /**
     * 获取所有最新版本的标准问题（分页）
     * 只返回版本树中的叶子节点，即没有子版本的问题
     * @param pageable 分页参数
     * @return 最新版本的标准问题分页列表
     */
    Page<StandardQuestionDTO> findLatestStandardQuestions(Pageable pageable);
    
    /**
     * 删除标准问题
     * @param questionId 要删除的问题ID
     * @param userId 执行删除操作的用户ID
     * @param permanent 是否永久删除，true为物理删除，false为逻辑删除（默认）
     * @return 删除结果，包含成功状态和消息
     */
    Map<String, Object> deleteStandardQuestion(Long questionId, Long userId, boolean permanent);

    /**
     * 操作单个标准问题的标签
     * @param operationDTO 标签操作请求
     * @return 更新后的标准问题DTO
     */
    StandardQuestionDTO updateQuestionTags(TagOperationDTO operationDTO);
    
    /**
     * 批量操作多个标准问题的标签
     * @param batchOperationsDTO 批量标签操作请求
     * @return 操作结果，key为问题ID，value为操作成功与否
     */
    Map<Long, Boolean> batchUpdateQuestionTags(BatchTagOperationsDTO batchOperationsDTO);

    /**
     * 搜索标准问题
     * @param tags 标签列表
     * @param keyword 关键词
     * @param userId 当前用户ID，用于判断用户是否已回答
     * @param onlyLatest 是否只返回叶子节点（最新版本，没有子问题的版本）
     * @param pageable 分页参数
     * @return 搜索结果，包含问题列表和额外信息
     */
    Map<String, Object> searchQuestions(List<String> tags, String keyword, Long userId, Boolean onlyLatest, Pageable pageable);
    
    /**
     * 根据标准问题ID获取原始问题和原始回答列表
     * @param questionId 标准问题ID
     * @param pageable 分页参数
     * @return 包含原始问题和回答的数据
     */
    Map<String, Object> getOriginalQuestionAndAnswers(Long questionId, Pageable pageable);
    
    /**
     * 查找所有没有标准回答的标准问题
     * 
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param pageable 分页参数
     * @return 无标准回答的问题列表
     */
    Map<String, Object> findQuestionsWithoutStandardAnswers(Boolean onlyLatest, Boolean onlyLatestVersion, Pageable pageable);
    
    /**
     * 回退标准问题到指定版本
     * @param versionId 要回退到的版本ID（变更日志ID）
     * @param userId 操作用户ID
     * @param commitMessage 回退说明信息
     * @return 回退后的标准问题DTO
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果版本冲突
     */
    StandardQuestionDTO rollbackQuestion(Long versionId, Long userId, String commitMessage);

    /**
     * 查找所有有标准答案的标准问题
     * 
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param pageable 分页参数
     * @return 包含问题列表和分页信息的Map
     */
    Map<String, Object> findQuestionsWithStandardAnswers(Boolean onlyLatest, Boolean onlyLatestVersion, Pageable pageable);

    /**
     * 根据数据集ID查询问题，支持数据集内或数据集外的查询
     * 
     * @param datasetId 数据集ID
     * @param isInDataset 是否查询数据集内的问题 (true - 数据集内, false - 数据集外)
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param onlyWithStandardAnswers 是否只返回有标准答案的问题
     * @param tags 标签列表
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 包含问题列表和分页信息的Map
     */
    Map<String, Object> findQuestionsByDataset(Long datasetId, Boolean isInDataset, Boolean onlyLatest, 
                                          Boolean onlyLatestVersion, Boolean onlyWithStandardAnswers, 
                                          List<String> tags, String keyword, Pageable pageable);

    /**
     * 搜索标准问题
     * @param tags 标签列表
     * @param keyword 关键词
     * @param userId 当前用户ID，用于判断用户是否已回答
     * @param onlyLatest 是否只返回叶子节点（最新版本，没有子问题的版本）
     * @param onlyWithStandardAnswers 是否只返回有标准答案的问题
     * @param pageable 分页参数
     * @return 搜索结果，包含问题列表和额外信息
     */
    Map<String, Object> searchQuestions(List<String> tags, String keyword, Long userId, Boolean onlyLatest, Boolean onlyWithStandardAnswers, Pageable pageable);
} 