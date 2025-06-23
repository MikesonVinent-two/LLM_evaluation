package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class DeleteDatasetVersionRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 