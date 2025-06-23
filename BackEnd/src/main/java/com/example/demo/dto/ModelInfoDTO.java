package com.example.demo.dto;

public class ModelInfoDTO {
    private String id;
    private String name;
    private String provider;
    private String description;
    private Integer maxTokens;
    private Boolean available;
    private Double pricePerToken;
    
    // 构造函数
    public ModelInfoDTO() {
    }
    
    public ModelInfoDTO(String id, String name, String provider) {
        this.id = id;
        this.name = name;
        this.provider = provider;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Boolean getAvailable() {
        return available;
    }
    
    public void setAvailable(Boolean available) {
        this.available = available;
    }
    
    public Double getPricePerToken() {
        return pricePerToken;
    }
    
    public void setPricePerToken(Double pricePerToken) {
        this.pricePerToken = pricePerToken;
    }
} 