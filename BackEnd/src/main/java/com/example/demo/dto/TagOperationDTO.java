package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * 标签操作请求DTO
 */
public class TagOperationDTO {
    
    /**
     * 操作类型
     */
    public enum OperationType {
        ADD,      // 添加标签
        REMOVE,   // 移除标签
        REPLACE   // 替换所有标签
    }
    
    @NotNull(message = "标准问题ID不能为空")
    private Long questionId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "操作类型不能为空")
    private OperationType operationType;
    
    private List<String> tags;
    
    // 可选的提交信息
    private String commitMessage;
    
    // Getters and Setters
    public Long getQuestionId() {
        return questionId;
    }
    
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
} 