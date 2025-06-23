package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QuestionHistoryDTO {
    private Long id;                     // 问题版本ID
    private String questionText;         // 问题文本
    private String questionType;         // 问题类型
    private String difficulty;           // 难度
    private LocalDateTime creationTime;  // 创建时间
    private Long createdByUserId;       // 创建用户ID
    private Map<String, Object> createdByUser;  // 创建用户的详细信息
    private Long parentQuestionId;       // 父版本ID
    private List<String> tags;          // 标签列表
    private String commitMessage;        // 变更说明
    private List<ChangeDetailDTO> changes; // 具体变更详情
    private Long changeLogId;           // 变更日志ID，用于回退功能

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
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

    public Map<String, Object> getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(Map<String, Object> createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Long getParentQuestionId() {
        return parentQuestionId;
    }

    public void setParentQuestionId(Long parentQuestionId) {
        this.parentQuestionId = parentQuestionId;
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

    public List<ChangeDetailDTO> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeDetailDTO> changes) {
        this.changes = changes;
    }
    
    public Long getChangeLogId() {
        return changeLogId;
    }

    public void setChangeLogId(Long changeLogId) {
        this.changeLogId = changeLogId;
    }
} 