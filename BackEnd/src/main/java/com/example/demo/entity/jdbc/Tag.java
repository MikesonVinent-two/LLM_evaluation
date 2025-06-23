package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 标签实体类
 */
public class Tag {
    
    // 表名
    public static final String TABLE_NAME = "tags";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TAG_NAME = "tag_name";
    public static final String COLUMN_TAG_TYPE = "tag_type";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private String tagName;
    private String tagType;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;
    
    // 构造函数
    public Tag() {
    }
    
    public Tag(String tagName) {
        this.tagName = tagName;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTagName() {
        return tagName;
    }
    
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    public String getTagType() {
        return tagType;
    }
    
    public void setTagType(String tagType) {
        this.tagType = tagType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
} 