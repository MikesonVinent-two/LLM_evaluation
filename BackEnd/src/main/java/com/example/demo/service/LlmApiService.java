package com.example.demo.service;

import java.util.Map;

import com.example.demo.entity.jdbc.LlmModel;

/**
 * LLM API服务接口
 */
public interface LlmApiService {
    
    /**
     * 调用LLM API生成回答
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param prompt 提示词
     * @param parameters 参数
     * @return 生成的回答文本
     */
    String generateAnswer(String apiUrl, String apiKey, String prompt, Map<String, Object> parameters);
    
    /**
     * 调用LLM API生成回答（带API类型）
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param apiType API类型(如"openai", "azure", "anthropic", "google"等)
     * @param prompt 提示词
     * @param parameters 参数
     * @return 生成的回答文本
     */
    String generateAnswer(String apiUrl, String apiKey, String apiType, String prompt, Map<String, Object> parameters);
    
    /**
     * 使用LLM模型生成回答
     * 
     * @param model LLM模型
     * @param prompt 提示词
     * @param contextVariables 上下文变量
     * @return 生成的回答文本
     */
    String generateModelAnswer(LlmModel model, String prompt, Map<String, Object> contextVariables);
    
    /**
     * 测试模型连通性
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param apiType API类型
     * @return 连接是否成功
     */
    boolean testModelConnectivity(String apiUrl, String apiKey, String apiType);
    
    /**
     * 测试模型连通性（使用指定的模型名称）
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param apiType API类型
     * @param modelName 模型名称
     * @return 连接是否成功
     */
    boolean testModelConnectivity(String apiUrl, String apiKey, String apiType, String modelName);
} 