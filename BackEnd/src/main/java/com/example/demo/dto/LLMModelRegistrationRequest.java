package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LLMModelRegistrationRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "API URL不能为空")
    private String apiUrl;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @NotBlank(message = "API Type不能为空")
    private String apiType;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }
} 