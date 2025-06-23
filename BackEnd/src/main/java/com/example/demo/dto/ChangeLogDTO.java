package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.entity.jdbc.ChangeLog;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 变更日志DTO，用于API接口返回
 */
public class ChangeLogDTO {
    private Long id;
    private String changeType;
    private Long userId;
    private String userName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime commitTime;
    
    private String commitMessage;
    private Long associatedStandardQuestionId;
    
    public ChangeLogDTO() {
    }
    
    public ChangeLogDTO(ChangeLog changeLog) {
        this.id = changeLog.getId();
        this.changeType = changeLog.getChangeType() != null ? changeLog.getChangeType().name() : null;
        
        if (changeLog.getUser() != null) {
            this.userId = changeLog.getUser().getId();
            this.userName = changeLog.getUser().getUsername();
        }
        
        this.commitTime = changeLog.getCommitTime();
        this.commitMessage = changeLog.getCommitMessage();
        
        if (changeLog.getAssociatedStandardQuestion() != null) {
            this.associatedStandardQuestionId = changeLog.getAssociatedStandardQuestion().getId();
        }
    }
    
    /**
     * 将ChangeLog实体列表转换为DTO列表
     */
    public static List<ChangeLogDTO> fromEntityList(List<ChangeLog> changeLogs) {
        return changeLogs.stream()
                .map(ChangeLogDTO::new)
                .collect(Collectors.toList());
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
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
} 