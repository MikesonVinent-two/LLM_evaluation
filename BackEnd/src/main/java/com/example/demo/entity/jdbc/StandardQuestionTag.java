package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 标准问题标签关联实体类
 */
public class StandardQuestionTag {
    
    // 表名
    public static final String TABLE_NAME = "standard_question_tags";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARD_QUESTION_ID = "standard_question_id";
    public static final String COLUMN_TAG_ID = "tag_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    
    private Long id;
    private StandardQuestion standardQuestion;
    private Tag tag;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private ChangeLog createdChangeLog;

    // 构造函数
    public StandardQuestionTag() {
    }
    
    public StandardQuestionTag(StandardQuestion standardQuestion, Tag tag, User user) {
        this.standardQuestion = standardQuestion;
        this.tag = tag;
        this.createdByUser = user;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StandardQuestion getStandardQuestion() {
        return standardQuestion;
    }

    public void setStandardQuestion(StandardQuestion standardQuestion) {
        this.standardQuestion = standardQuestion;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public ChangeLog getCreatedChangeLog() {
        return createdChangeLog;
    }

    public void setCreatedChangeLog(ChangeLog createdChangeLog) {
        this.createdChangeLog = createdChangeLog;
    }
} 