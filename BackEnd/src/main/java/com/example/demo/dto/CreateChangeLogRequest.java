package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建变更日志的请求DTO
 */
public class CreateChangeLogRequest {
    
    @NotBlank(message = "变更类型不能为空")
    private String changeType;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    private String commitMessage;
    
    private Long associatedStandardQuestionId;
    
    private List<ChangeDetailRequest> details = new ArrayList<>();
    
    /**
     * 变更详情请求
     */
    public static class ChangeDetailRequest {
        @NotBlank(message = "实体类型不能为空")
        private String entityType;
        
        @NotNull(message = "实体ID不能为空")
        private Long entityId;
        
        @NotBlank(message = "属性名不能为空")
        private String attributeName;
        
        private String oldValue;
        
        private String newValue;
        
        // Getters and Setters
        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public Long getEntityId() {
            return entityId;
        }

        public void setEntityId(Long entityId) {
            this.entityId = entityId;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }
    
    // Getters and Setters
    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public Long getAssociatedStandardQuestionId() {
        return associatedStandardQuestionId;
    }

    public void setAssociatedStandardQuestionId(Long associatedStandardQuestionId) {
        this.associatedStandardQuestionId = associatedStandardQuestionId;
    }

    public List<ChangeDetailRequest> getDetails() {
        return details;
    }

    public void setDetails(List<ChangeDetailRequest> details) {
        this.details = details;
    }
} 