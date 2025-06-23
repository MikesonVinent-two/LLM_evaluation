package com.example.demo.dto;

import java.time.LocalDateTime;

public class TagDTO {
    
    private Long id;
    private String tagName;
    private String tagType;
    private LocalDateTime createdAt;
    private boolean hasAnswerPrompt; // 是否已生成回答prompt
    private boolean hasEvaluationPrompt; // 是否已生成评测prompt
    
    // 构造函数
    public TagDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHasAnswerPrompt() {
        return hasAnswerPrompt;
    }

    public void setHasAnswerPrompt(boolean hasAnswerPrompt) {
        this.hasAnswerPrompt = hasAnswerPrompt;
    }

    public boolean isHasEvaluationPrompt() {
        return hasEvaluationPrompt;
    }

    public void setHasEvaluationPrompt(boolean hasEvaluationPrompt) {
        this.hasEvaluationPrompt = hasEvaluationPrompt;
    }
} 