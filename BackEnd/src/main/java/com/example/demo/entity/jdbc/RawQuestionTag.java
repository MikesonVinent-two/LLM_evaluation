package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * 原始问题标签关联实体类
 */
public class RawQuestionTag {
    
    // 表名
    public static final String TABLE_NAME = "raw_question_tags";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_RAW_QUESTION_ID = "raw_question_id";
    public static final String COLUMN_TAG_ID = "tag_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    
    // 唯一约束名
    public static final String UK_RAW_QUESTION_TAG = "uk_raw_question_tag";
    
    private Long id;
    private RawQuestion rawQuestion;
    private Tag tag;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private ChangeLog createdChangeLog;
    
    // 构造函数
    public RawQuestionTag() {
    }
    
    public RawQuestionTag(RawQuestion rawQuestion, Tag tag) {
        this.rawQuestion = rawQuestion;
        this.tag = tag;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    @JsonBackReference
    public RawQuestion getRawQuestion() {
        return rawQuestion;
    }
    
    public void setRawQuestion(RawQuestion rawQuestion) {
        this.rawQuestion = rawQuestion;
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