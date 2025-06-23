package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdateDatasetVersionRequest {
    
    @Size(max = 100, message = "数据集名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;
    
    private List<Long> standardQuestionsToAdd;
    
    private List<Long> standardQuestionsToRemove;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    // Getters and Setters
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
    
    public List<Long> getStandardQuestionsToAdd() {
        return standardQuestionsToAdd;
    }
    
    public void setStandardQuestionsToAdd(List<Long> standardQuestionsToAdd) {
        this.standardQuestionsToAdd = standardQuestionsToAdd;
    }
    
    public List<Long> getStandardQuestionsToRemove() {
        return standardQuestionsToRemove;
    }
    
    public void setStandardQuestionsToRemove(List<Long> standardQuestionsToRemove) {
        this.standardQuestionsToRemove = standardQuestionsToRemove;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 