package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateDatasetVersionRequest {
    
    @NotBlank(message = "版本号不能为空")
    @Size(max = 50, message = "版本号长度不能超过50个字符")
    private String versionNumber;
    
    @NotBlank(message = "数据集名称不能为空")
    @Size(max = 100, message = "数据集名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;
    
    private List<Long> standardQuestionIds;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    // Getters and Setters
    public String getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
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
    
    public List<Long> getStandardQuestionIds() {
        return standardQuestionIds;
    }
    
    public void setStandardQuestionIds(List<Long> standardQuestionIds) {
        this.standardQuestionIds = standardQuestionIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 