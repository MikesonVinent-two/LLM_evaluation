package com.example.demo.dto;

import java.util.Map;

public class LlmResponseDTO {
    
    private String content;
    private String model;
    private Integer tokenCount;
    private Long responseTime;
    private Boolean success;
    private String errorMessage;
    private Map<String, Object> metadata;
    
    // 构造函数
    public LlmResponseDTO() {
    }
    
    public LlmResponseDTO(String content, String model, Integer tokenCount, Long responseTime, Boolean success) {
        this.content = content;
        this.model = model;
        this.tokenCount = tokenCount;
        this.responseTime = responseTime;
        this.success = success;
    }
    
    public LlmResponseDTO(Boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
} 