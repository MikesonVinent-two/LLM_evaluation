package com.example.demo.service;

import java.util.Map;

/**
 * 大模型评测员连通性测试服务接口
 */
public interface AiEvaluatorConnectivityService {
    
    /**
     * 测试大模型评测员的连通性
     * 
     * @param evaluatorId 评测员ID
     * @return 测试结果，包含成功标志、响应时间、模型信息等
     */
    Map<String, Object> testConnectivity(Long evaluatorId);
} 