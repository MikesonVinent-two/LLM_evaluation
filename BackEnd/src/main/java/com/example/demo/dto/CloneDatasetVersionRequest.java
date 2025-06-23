package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CloneDatasetVersionRequest {
    
    @NotBlank(message = "新版本号不能为空")
    @Size(max = 50, message = "版本号长度不能超过50个字符")
    private String newVersionNumber;
    
    @NotBlank(message = "新数据集名称不能为空")
    @Size(max = 100, message = "数据集名称长度不能超过100个字符")
    private String newName;
    
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    public String getNewVersionNumber() {
        return newVersionNumber;
    }

    public void setNewVersionNumber(String newVersionNumber) {
        this.newVersionNumber = newVersionNumber;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 