package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnswerTagPromptDTO {
    
    private Long id;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "标签ID不能为空")
    private Long tagId;
    
    @NotBlank(message = "提示词名称不能为空")
    private String name;
    
    @NotBlank(message = "提示词模板内容不能为空")
    private String promptTemplate;
    
    private String description;
    
    private Boolean isActive = true;
    
    @Min(value = 1, message = "优先级最小值为1")
    @Max(value = 100, message = "优先级最大值为100")
    private Integer promptPriority = 50;
    
    private String version;
    
    private Long parentPromptId;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPromptPriority() {
        return promptPriority;
    }

    public void setPromptPriority(Integer promptPriority) {
        this.promptPriority = promptPriority;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Long getParentPromptId() {
        return parentPromptId;
    }

    public void setParentPromptId(Long parentPromptId) {
        this.parentPromptId = parentPromptId;
    }
} 