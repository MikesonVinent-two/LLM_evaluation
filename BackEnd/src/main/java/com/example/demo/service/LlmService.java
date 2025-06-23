package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.LlmRequestDTO;
import com.example.demo.dto.LlmResponseDTO;
import com.example.demo.dto.ModelInfoDTO;

public interface LlmService {
    
    /**
     * 发送请求到LLM API并获取回答
     * 
     * @param request LLM请求参数
     * @return LLM响应结果
     */
    LlmResponseDTO sendRequest(LlmRequestDTO request);

    /**
     * 获取可用的模型列表
     * 
     * @param apiUrl API基础URL
     * @param apiKey API密钥
     * @return 模型列表
     */
    List<ModelInfoDTO> getAvailableModels(String apiUrl, String apiKey);
} 