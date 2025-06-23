package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 提示词组装配置DTO基类
 */
public class PromptAssemblyConfigDTO {
    
    private Long id;
    private String name;
    private String description;
    private String baseSystemPrompt;
    private String tagPromptsSectionHeader;
    private String tagPromptSeparator;
    private String sectionSeparator;
    private String finalInstruction;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdByUserId;
    private String createdByUsername;
    
    // Constructors
    public PromptAssemblyConfigDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getBaseSystemPrompt() {
        return baseSystemPrompt;
    }
    
    public void setBaseSystemPrompt(String baseSystemPrompt) {
        this.baseSystemPrompt = baseSystemPrompt;
    }
    
    public String getTagPromptsSectionHeader() {
        return tagPromptsSectionHeader;
    }
    
    public void setTagPromptsSectionHeader(String tagPromptsSectionHeader) {
        this.tagPromptsSectionHeader = tagPromptsSectionHeader;
    }
    
    public String getTagPromptSeparator() {
        return tagPromptSeparator;
    }
    
    public void setTagPromptSeparator(String tagPromptSeparator) {
        this.tagPromptSeparator = tagPromptSeparator;
    }
    
    public String getSectionSeparator() {
        return sectionSeparator;
    }
    
    public void setSectionSeparator(String sectionSeparator) {
        this.sectionSeparator = sectionSeparator;
    }
    
    public String getFinalInstruction() {
        return finalInstruction;
    }
    
    public void setFinalInstruction(String finalInstruction) {
        this.finalInstruction = finalInstruction;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public String getCreatedByUsername() {
        return createdByUsername;
    }
    
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }
} 