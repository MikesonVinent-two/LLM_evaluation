package com.example.demo.dto;

import com.example.demo.entity.jdbc.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnswerQuestionTypePromptDTO {
    
    private Long id;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "提示词名称不能为空")
    private String name;
    
    @NotNull(message = "题型不能为空")
    private QuestionType questionType;
    
    @NotBlank(message = "提示词模板内容不能为空")
    private String promptTemplate;
    
    private String description;
    
    private Boolean isActive = true;
    
    private String responseFormatInstruction;
    
    private String responseExample;
    
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
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

    public String getResponseFormatInstruction() {
        return responseFormatInstruction;
    }

    public void setResponseFormatInstruction(String responseFormatInstruction) {
        this.responseFormatInstruction = responseFormatInstruction;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
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