package com.example.demo.dto;

import java.time.LocalDateTime;

public class DatasetVersionDTO {
    
    private Long id;
    private String versionNumber;
    private String name;
    private String description;
    private LocalDateTime creationTime;
    private Long createdByUserId;
    private String createdByUserName;
    private int questionCount;
    
    public DatasetVersionDTO() {
    }
    
    // 构造函数
    public DatasetVersionDTO(Long id, String versionNumber, String name, String description, 
                            LocalDateTime creationTime, Long createdByUserId, String createdByUserName,
                            int questionCount) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.name = name;
        this.description = description;
        this.creationTime = creationTime;
        this.createdByUserId = createdByUserId;
        this.createdByUserName = createdByUserName;
        this.questionCount = questionCount;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public Long getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public String getCreatedByUserName() {
        return createdByUserName;
    }
    
    public void setCreatedByUserName(String createdByUserName) {
        this.createdByUserName = createdByUserName;
    }
    
    public int getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
} 