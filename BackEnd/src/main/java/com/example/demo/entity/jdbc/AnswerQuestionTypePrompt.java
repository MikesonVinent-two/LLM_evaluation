package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 问题类型回答提示实体类 - JDBC版本
 * 对应数据库表: answer_question_type_prompts
 */
public class AnswerQuestionTypePrompt {
    // 表名常量
    public static final String TABLE_NAME = "answer_question_type_prompts";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_QUESTION_TYPE = "question_type";
    public static final String COLUMN_PROMPT_TEMPLATE = "prompt_template";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_RESPONSE_FORMAT_INSTRUCTION = "response_format_instruction";
    public static final String COLUMN_RESPONSE_EXAMPLE = "response_example";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_PARENT_PROMPT_ID = "parent_prompt_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private String name;
    private QuestionType questionType;
    private String promptTemplate;
    private String description;
    private Boolean isActive = true;
    private String responseFormatInstruction;
    private String responseExample;
    private String version;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private User createdByUser;
    private AnswerQuestionTypePrompt parentPrompt;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
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

    public String getResponseFormatInstruction() {
        return responseFormatInstruction;
    }

    public void setResponseFormatInstruction(String responseFormatInstruction) {
        this.responseFormatInstruction = responseFormatInstruction;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
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

    public AnswerQuestionTypePrompt getParentPrompt() {
        return parentPrompt;
    }

    public void setParentPrompt(AnswerQuestionTypePrompt parentPrompt) {
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