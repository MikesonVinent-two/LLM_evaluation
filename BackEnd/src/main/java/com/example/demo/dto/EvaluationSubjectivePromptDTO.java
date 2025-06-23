package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class EvaluationSubjectivePromptDTO {
    
    private Long id;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "提示词名称不能为空")
    private String name;
    
    @NotBlank(message = "提示词模板内容不能为空")
    private String promptTemplate;
    
    private String description;
    
    private Map<String, Object> evaluationCriteriaFocus;
    
    private String scoringInstruction;
    
    private String outputFormatInstruction;
    
    private Boolean isActive = true;
    
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

    public Map<String, Object> getEvaluationCriteriaFocus() {
        return evaluationCriteriaFocus;
    }

    public void setEvaluationCriteriaFocus(Map<String, Object> evaluationCriteriaFocus) {
        this.evaluationCriteriaFocus = evaluationCriteriaFocus;
    }

    public String getScoringInstruction() {
        return scoringInstruction;
    }

    public void setScoringInstruction(String scoringInstruction) {
        this.scoringInstruction = scoringInstruction;
    }

    public String getOutputFormatInstruction() {
        return outputFormatInstruction;
    }

    public void setOutputFormatInstruction(String outputFormatInstruction) {
        this.outputFormatInstruction = outputFormatInstruction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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