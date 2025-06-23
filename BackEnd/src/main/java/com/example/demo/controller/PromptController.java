package com.example.demo.controller;

import com.example.demo.dto.AnswerQuestionTypePromptDTO;
import com.example.demo.dto.AnswerTagPromptDTO;
import com.example.demo.dto.EvaluationSubjectivePromptDTO;
import com.example.demo.dto.EvaluationTagPromptDTO;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.AnswerTagPrompt;
import com.example.demo.entity.jdbc.EvaluationSubjectivePrompt;
import com.example.demo.entity.jdbc.EvaluationTagPrompt;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.service.PromptService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prompts")
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);
    
    @Autowired
    private PromptService promptService;
    
    //===== 标签提示词API =====
    
    @PostMapping("/tags")
    public ResponseEntity<AnswerTagPrompt> createAnswerTagPrompt(
            @Valid @RequestBody AnswerTagPromptDTO dto) {
        logger.info("接收到创建标签提示词请求");
        AnswerTagPrompt prompt = promptService.createAnswerTagPrompt(dto, dto.getUserId());
        return new ResponseEntity<>(prompt, HttpStatus.CREATED);
    }
    
    @PutMapping("/tags/{id}")
    public ResponseEntity<AnswerTagPrompt> updateAnswerTagPrompt(
            @PathVariable Long id,
            @Valid @RequestBody AnswerTagPromptDTO dto) {
        logger.info("接收到更新标签提示词请求，ID: {}", id);
        AnswerTagPrompt prompt = promptService.updateAnswerTagPrompt(id, dto, dto.getUserId());
        return ResponseEntity.ok(prompt);
    }
    
    @GetMapping("/tags/{id}")
    public ResponseEntity<AnswerTagPrompt> getAnswerTagPromptById(@PathVariable Long id) {
        return promptService.getAnswerTagPromptById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/tags")
    public ResponseEntity<List<AnswerTagPrompt>> getAllAnswerTagPrompts() {
        List<AnswerTagPrompt> prompts = promptService.getAllAnswerTagPrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/tags/active/tag/{tagId}")
    public ResponseEntity<List<AnswerTagPrompt>> getActiveAnswerTagPromptsByTagId(@PathVariable Long tagId) {
        List<AnswerTagPrompt> prompts = promptService.getActiveAnswerTagPromptsByTagId(tagId);
        return ResponseEntity.ok(prompts);
    }
    
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteAnswerTagPrompt(
            @PathVariable Long id,
            @RequestBody Map<String, Long> requestBody) {
        Long userId = requestBody.get("userId");
        logger.info("接收到删除标签提示词请求，ID: {}", id);
        promptService.deleteAnswerTagPrompt(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    //===== 题型提示词API =====
    
    @PostMapping("/question-types")
    public ResponseEntity<AnswerQuestionTypePrompt> createAnswerQuestionTypePrompt(
            @Valid @RequestBody AnswerQuestionTypePromptDTO dto) {
        logger.info("接收到创建题型提示词请求");
        AnswerQuestionTypePrompt prompt = promptService.createAnswerQuestionTypePrompt(dto, dto.getUserId());
        return new ResponseEntity<>(prompt, HttpStatus.CREATED);
    }
    
    @PutMapping("/question-types/{id}")
    public ResponseEntity<AnswerQuestionTypePrompt> updateAnswerQuestionTypePrompt(
            @PathVariable Long id,
            @Valid @RequestBody AnswerQuestionTypePromptDTO dto) {
        logger.info("接收到更新题型提示词请求，ID: {}", id);
        AnswerQuestionTypePrompt prompt = promptService.updateAnswerQuestionTypePrompt(id, dto, dto.getUserId());
        return ResponseEntity.ok(prompt);
    }
    
    @GetMapping("/question-types/{id}")
    public ResponseEntity<AnswerQuestionTypePrompt> getAnswerQuestionTypePromptById(@PathVariable Long id) {
        return promptService.getAnswerQuestionTypePromptById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/question-types")
    public ResponseEntity<List<AnswerQuestionTypePrompt>> getAllAnswerQuestionTypePrompts() {
        List<AnswerQuestionTypePrompt> prompts = promptService.getAllAnswerQuestionTypePrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/question-types/active/type/{type}")
    public ResponseEntity<List<AnswerQuestionTypePrompt>> getActiveAnswerQuestionTypePromptsByType(
            @PathVariable QuestionType type) {
        List<AnswerQuestionTypePrompt> prompts = promptService.getActiveAnswerQuestionTypePromptsByType(type);
        return ResponseEntity.ok(prompts);
    }
    
    @DeleteMapping("/question-types/{id}")
    public ResponseEntity<Void> deleteAnswerQuestionTypePrompt(
            @PathVariable Long id,
            @RequestBody Map<String, Long> requestBody) {
        Long userId = requestBody.get("userId");
        logger.info("接收到删除题型提示词请求，ID: {}", id);
        promptService.deleteAnswerQuestionTypePrompt(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/question-types/supported-types")
    public ResponseEntity<QuestionType[]> getSupportedQuestionTypes() {
        return ResponseEntity.ok(QuestionType.values());
    }
    
    //===== 评测标签提示词API =====
    
    @PostMapping("/evaluation/tags")
    public ResponseEntity<EvaluationTagPrompt> createEvaluationTagPrompt(
            @Valid @RequestBody EvaluationTagPromptDTO dto) {
        logger.info("接收到创建评测标签提示词请求");
        EvaluationTagPrompt prompt = promptService.createEvaluationTagPrompt(dto, dto.getUserId());
        return new ResponseEntity<>(prompt, HttpStatus.CREATED);
    }
    
    @PutMapping("/evaluation/tags/{id}")
    public ResponseEntity<EvaluationTagPrompt> updateEvaluationTagPrompt(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationTagPromptDTO dto) {
        logger.info("接收到更新评测标签提示词请求，ID: {}", id);
        EvaluationTagPrompt prompt = promptService.updateEvaluationTagPrompt(id, dto, dto.getUserId());
        return ResponseEntity.ok(prompt);
    }
    
    @GetMapping("/evaluation/tags/{id}")
    public ResponseEntity<EvaluationTagPrompt> getEvaluationTagPromptById(@PathVariable Long id) {
        return promptService.getEvaluationTagPromptById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/evaluation/tags")
    public ResponseEntity<List<EvaluationTagPrompt>> getAllEvaluationTagPrompts() {
        List<EvaluationTagPrompt> prompts = promptService.getAllEvaluationTagPrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/evaluation/tags/active/tag/{tagId}")
    public ResponseEntity<List<EvaluationTagPrompt>> getActiveEvaluationTagPromptsByTagId(@PathVariable Long tagId) {
        List<EvaluationTagPrompt> prompts = promptService.getActiveEvaluationTagPromptsByTagId(tagId);
        return ResponseEntity.ok(prompts);
    }
    
    @DeleteMapping("/evaluation/tags/{id}")
    public ResponseEntity<Void> deleteEvaluationTagPrompt(
            @PathVariable Long id,
            @RequestBody Map<String, Long> requestBody) {
        Long userId = requestBody.get("userId");
        logger.info("接收到删除评测标签提示词请求，ID: {}", id);
        promptService.deleteEvaluationTagPrompt(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    //===== 评测主观题提示词API =====
    
    @PostMapping("/evaluation/subjective")
    public ResponseEntity<EvaluationSubjectivePrompt> createEvaluationSubjectivePrompt(
            @Valid @RequestBody EvaluationSubjectivePromptDTO dto) {
        logger.info("接收到创建评测主观题提示词请求");
        EvaluationSubjectivePrompt prompt = promptService.createEvaluationSubjectivePrompt(dto, dto.getUserId());
        return new ResponseEntity<>(prompt, HttpStatus.CREATED);
    }
    
    @PutMapping("/evaluation/subjective/{id}")
    public ResponseEntity<EvaluationSubjectivePrompt> updateEvaluationSubjectivePrompt(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationSubjectivePromptDTO dto) {
        logger.info("接收到更新评测主观题提示词请求，ID: {}", id);
        EvaluationSubjectivePrompt prompt = promptService.updateEvaluationSubjectivePrompt(id, dto, dto.getUserId());
        return ResponseEntity.ok(prompt);
    }
    
    @GetMapping("/evaluation/subjective/{id}")
    public ResponseEntity<EvaluationSubjectivePrompt> getEvaluationSubjectivePromptById(@PathVariable Long id) {
        return promptService.getEvaluationSubjectivePromptById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/evaluation/subjective")
    public ResponseEntity<List<EvaluationSubjectivePrompt>> getAllEvaluationSubjectivePrompts() {
        List<EvaluationSubjectivePrompt> prompts = promptService.getAllEvaluationSubjectivePrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @GetMapping("/evaluation/subjective/active")
    public ResponseEntity<List<EvaluationSubjectivePrompt>> getActiveEvaluationSubjectivePrompts() {
        List<EvaluationSubjectivePrompt> prompts = promptService.getActiveEvaluationSubjectivePrompts();
        return ResponseEntity.ok(prompts);
    }
    
    @DeleteMapping("/evaluation/subjective/{id}")
    public ResponseEntity<Void> deleteEvaluationSubjectivePrompt(
            @PathVariable Long id,
            @RequestBody Map<String, Long> requestBody) {
        Long userId = requestBody.get("userId");
        logger.info("接收到删除评测主观题提示词请求，ID: {}", id);
        promptService.deleteEvaluationSubjectivePrompt(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("处理请求时发生异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
} 