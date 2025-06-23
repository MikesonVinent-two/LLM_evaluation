package com.example.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 批量标签操作请求DTO
 */
public class BatchTagOperationsDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotEmpty(message = "操作列表不能为空")
    @Valid
    private List<TagOperation> operations;
    
    /**
     * 单个标签操作
     */
    public static class TagOperation {
        
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
        
        @NotNull(message = "操作类型不能为空")
        private OperationType operationType;
        
        private List<String> tags;
        
        // Getters and Setters
        public Long getQuestionId() {
            return questionId;
        }
        
        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
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
    }
    
    // 可选的提交信息
    private String commitMessage;
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<TagOperation> getOperations() {
        return operations;
    }
    
    public void setOperations(List<TagOperation> operations) {
        this.operations = operations;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
} 