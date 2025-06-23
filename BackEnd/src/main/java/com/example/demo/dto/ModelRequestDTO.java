package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class ModelRequestDTO {
    
    @NotBlank(message = "API URL不能为空")
    private String apiUrl;
    
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;
    
    // 构造函数
    public ModelRequestDTO() {
    }
    
    public ModelRequestDTO(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }
    
    // Getters and Setters
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
} 