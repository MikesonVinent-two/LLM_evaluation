package com.example.demo.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AnswerTagPromptDTO;
import com.example.demo.dto.TagPromptResponse;
import com.example.demo.dto.UpdateTagPromptRequest;
import com.example.demo.entity.jdbc.AnswerTagPrompt;
import com.example.demo.service.PromptService;

import jakarta.validation.Valid;

/**
 * 标签提示词API控制器
 */
@RestController
@RequestMapping("/prompts/tags")
public class TagPromptController {
    
    private static final Logger logger = LoggerFactory.getLogger(TagPromptController.class);
    
    @Autowired
    private PromptService promptService;
    
    /**
     * 更新标签提示词
     * 
     * @param id 提示词ID
     * @param request 更新请求
     * @return 更新后的提示词
     */
    @PutMapping("/detail/{id}")
    public ResponseEntity<TagPromptResponse> updateTagPrompt(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTagPromptRequest request) {
        logger.info("接收到更新标签提示词请求，ID: {}", id);
        
        // 将请求转换为DTO
        AnswerTagPromptDTO dto = new AnswerTagPromptDTO();
        dto.setUserId(request.getUserId());
        dto.setTagId(request.getTagId());
        dto.setName(request.getName());
        dto.setPromptTemplate(request.getPromptTemplate());
        dto.setDescription(request.getDescription());
        dto.setIsActive(request.getIsActive());
        dto.setPromptPriority(request.getPromptPriority());
        dto.setVersion(request.getVersion());
        dto.setParentPromptId(request.getParentPromptId());
        
        // 调用服务更新提示词
        AnswerTagPrompt prompt = promptService.updateAnswerTagPrompt(id, dto, dto.getUserId());
        
        // 将实体转换为响应DTO
        TagPromptResponse response = new TagPromptResponse(prompt);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取标签提示词详情
     * 
     * @param id 提示词ID
     * @return 提示词详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<TagPromptResponse> getTagPromptById(@PathVariable Long id) {
        Optional<AnswerTagPrompt> promptOpt = promptService.getAnswerTagPromptById(id);
        
        return promptOpt.map(prompt -> ResponseEntity.ok(new TagPromptResponse(prompt)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取所有标签提示词
     * 
     * @return 提示词列表
     */
    @GetMapping("/all")
    public ResponseEntity<List<TagPromptResponse>> getAllTagPrompts() {
        List<AnswerTagPrompt> prompts = promptService.getAllAnswerTagPrompts();
        
        List<TagPromptResponse> responses = prompts.stream()
                .map(TagPromptResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
} 