package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 评测标签提示语实体类
 */
public class EvaluationTagPrompt {
    
    // 表名
    public static final String TABLE_NAME = "evaluation_tag_prompts";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TAG_ID = "tag_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PROMPT_TEMPLATE = "prompt_template";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_PROMPT_PRIORITY = "prompt_priority";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_PARENT_PROMPT_ID = "parent_prompt_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private Tag tag;
    private String name;
    private String promptTemplate;
    private String description;
    private Boolean isActive = true;
    private Integer promptPriority = 50;
    private String version;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private User createdByUser;
    private EvaluationTagPrompt parentPrompt;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPromptPriority() {
        return promptPriority;
    }

    public void setPromptPriority(Integer promptPriority) {
        this.promptPriority = promptPriority;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public EvaluationTagPrompt getParentPrompt() {
        return parentPrompt;
    }

    public void setParentPrompt(EvaluationTagPrompt parentPrompt) {
        this.parentPrompt = parentPrompt;
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