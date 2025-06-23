package com.example.demo.service;

import java.util.List;
import java.util.Map;

/**
 * 模型批次评分统计服务接口
 * 负责计算和管理模型在批次中的评分统计数据
 */
public interface ModelBatchScoreService {
    
    /**
     * 计算并保存指定批次中所有模型的评分统计数据
     * 
     * @param batchId 批次ID
     * @return 是否成功
     */
    boolean calculateBatchScores(Long batchId);
    
    /**
     * 计算并保存指定批次中特定模型的评分统计数据
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 是否成功
     */
    boolean calculateModelScoresInBatch(Long batchId, Long modelId);
    
    /**
     * 清除指定批次的所有评分统计数据
     * 
     * @param batchId 批次ID
     * @return 是否成功
     */
    boolean clearBatchScores(Long batchId);
    
    /**
     * 获取指定批次中所有模型的评分排名
     * 
     * @param batchId 批次ID
     * @param scoreType 评分类型，如"OVERALL"、"ACCURACY"等，为null则默认使用"OVERALL"
     * @return 模型评分排名列表
     */
    List<Map<String, Object>> getModelRankingsInBatch(Long batchId, String scoreType);
    
    /**
     * 获取指定批次和模型的详细评分统计
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 评分统计详情
     */
    Map<String, Object> getModelScoreDetails(Long batchId, Long modelId);
} 