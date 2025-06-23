package com.example.demo.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LlmRequestDTO {
    
    @NotBlank(message = "API调用方式不能为空")
    private String api;
    
    @NotBlank(message = "API URL不能为空")
    private String apiUrl;
    
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;
    
    @NotBlank(message = "模型名称不能为空")
    private String model;
    
    @NotBlank(message = "消息内容不能为空")
    @Size(min = 1, message = "消息内容不能为空")
    private String message;
    
    private Double temperature;
    
    private Integer maxTokens;
    
    private List<Map<String, String>> systemPrompts;
    
    private Map<String, Object> additionalParams;

    // 构造函数
    public LlmRequestDTO() {
    }

    // Getters and Setters
    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<Map<String, String>> getSystemPrompts() {
        return systemPrompts;
    }

    public void setSystemPrompts(List<Map<String, String>> systemPrompts) {
        this.systemPrompts = systemPrompts;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }
} 