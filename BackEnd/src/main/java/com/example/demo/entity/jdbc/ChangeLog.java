package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 变更日志实体类 - JDBC版本
 * 对应数据库表: CHANGE_LOG
 */
public class ChangeLog {
    // 表名常量
    public static final String TABLE_NAME = "CHANGE_LOG";
    
    // 列名常量
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_COMMIT_MESSAGE = "COMMIT_MESSAGE";
    public static final String COLUMN_COMMIT_TIME = "CHANGE_TIME";
    public static final String COLUMN_USER_ID = "CHANGED_BY_USER_ID";
    public static final String COLUMN_CHANGE_TYPE = "CHANGE_TYPE";
    public static final String COLUMN_STANDARD_QUESTION_ID = "ASSOCIATED_STANDARD_QUESTION_ID";
    
    private Long id;
    private String commitMessage;
    private LocalDateTime commitTime;
    private User user;
    private ChangeType changeType;
    private StandardQuestion associatedStandardQuestion;
    
    // Getter and Setter methods
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCommitMessage() {
        return this.commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
    
    public LocalDateTime getCommitTime() {
        return this.commitTime;
    }
    
    public void setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
    }
    
    public User getUser() {
        return this.user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public ChangeType getChangeType() {
        return this.changeType;
    }
    
    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
    
    public StandardQuestion getAssociatedStandardQuestion() {
        return this.associatedStandardQuestion;
    }
    
    public void setAssociatedStandardQuestion(StandardQuestion standardQuestion) {
        this.associatedStandardQuestion = standardQuestion;
    }
    
    /**
     * 设置更改操作的用户
     */
    public void setChangedByUser(User user) {
        this.user = user;
    }
} 