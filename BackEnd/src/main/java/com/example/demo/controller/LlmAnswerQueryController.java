package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LlmAnswerQueryDTO;
import com.example.demo.dto.LlmAnswerResponseDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.service.LlmAnswerQueryService;

/**
 * LLM回答查询控制器
 */
@RestController
@RequestMapping("/llm-answers")
public class LlmAnswerQueryController {

    private static final Logger logger = LoggerFactory.getLogger(LlmAnswerQueryController.class);
    
    private final LlmAnswerQueryService llmAnswerQueryService;
    
    public LlmAnswerQueryController(LlmAnswerQueryService llmAnswerQueryService) {
        this.llmAnswerQueryService = llmAnswerQueryService;
    }
    
    /**
     * 查询LLM回答
     * 支持:
     * 1. 根据evaluatorId筛选
     * 2. onlyUnevaluated参数，只返回未被指定评测员评测过的回答
     * 3. 关键词和标签搜索
     * 4. 批次ID(batchId)筛选
     * 5. 问题类型(questionType)筛选，支持SINGLE_CHOICE, MULTIPLE_CHOICE, SIMPLE_FACT, SUBJECTIVE
     * 6. 分页返回
     * 
     * @param evaluatorId 评测员ID
     * @param onlyUnevaluated 是否只返回未评测的回答
     * @param keyword 关键词
     * @param tag 标签
     * @param batchId 批次ID
     * @param questionType 问题类型（SINGLE_CHOICE, MULTIPLE_CHOICE, SIMPLE_FACT, SUBJECTIVE）
     * @param page 页码
     * @param size 每页大小
     * @return 分页的LLM回答结果
     */
    @GetMapping("/query")
    public ResponseEntity<PageResponseDTO<LlmAnswerResponseDTO>> queryLlmAnswers(
            @RequestParam(required = false) Long evaluatorId,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyUnevaluated,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        LlmAnswerQueryDTO queryDTO = new LlmAnswerQueryDTO();
        queryDTO.setEvaluatorId(evaluatorId);
        queryDTO.setOnlyUnevaluated(onlyUnevaluated);
        queryDTO.setKeyword(keyword);
        queryDTO.setTag(tag);
        queryDTO.setBatchId(batchId);
        queryDTO.setQuestionType(questionType);
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        
        logger.info("查询LLM回答, 参数: evaluatorId={}, onlyUnevaluated={}, keyword={}, tag={}, batchId={}, questionType={}, page={}, size={}", 
                evaluatorId, onlyUnevaluated, keyword, tag, batchId, questionType, page, size);
        
        PageResponseDTO<LlmAnswerResponseDTO> result = llmAnswerQueryService.queryLlmAnswers(queryDTO);
        
        logger.info("查询LLM回答完成，共查询到{}条记录", result.getTotalElements());
        return ResponseEntity.ok(result);
    }
} 