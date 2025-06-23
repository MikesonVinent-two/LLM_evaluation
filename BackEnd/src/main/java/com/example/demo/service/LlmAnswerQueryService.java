package com.example.demo.service;

import com.example.demo.dto.LlmAnswerQueryDTO;
import com.example.demo.dto.LlmAnswerResponseDTO;
import com.example.demo.dto.PageResponseDTO;

/**
 * LLM回答查询服务接口
 */
public interface LlmAnswerQueryService {
    
    /**
     * 根据条件查询LLM回答，支持评测员筛选、未评测筛选、关键字和标签搜索
     * 
     * @param queryDTO 查询参数
     * @return 分页LLM回答结果
     */
    PageResponseDTO<LlmAnswerResponseDTO> queryLlmAnswers(LlmAnswerQueryDTO queryDTO);
} 