package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 回答场景的prompt组装配置实体类 - JDBC版本
 * 对应数据库表: answer_prompt_assembly_configs
 */
public class AnswerPromptAssemblyConfig {
    // 表名常量
    public static final String TABLE_NAME = "answer_prompt_assembly_configs";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_BASE_SYSTEM_PROMPT = "base_system_prompt";
    public static final String COLUMN_TAG_PROMPTS_SECTION_HEADER = "tag_prompts_section_header";
    public static final String COLUMN_QUESTION_TYPE_SECTION_HEADER = "question_type_section_header";
    public static final String COLUMN_TAG_PROMPT_SEPARATOR = "tag_prompt_separator";
    public static final String COLUMN_SECTION_SEPARATOR = "section_separator";
    public static final String COLUMN_FINAL_INSTRUCTION = "final_instruction";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private String baseSystemPrompt;
    
    private String tagPromptsSectionHeader = "## 专业知识指导";
    
    private String questionTypeSectionHeader = "## 回答要求";
    
    private String tagPromptSeparator = "\n\n";
    
    private String sectionSeparator = "\n\n";
    
    private String finalInstruction;
    
    private Boolean isActive = true;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private User createdByUser;
    
    private ChangeLog createdChangeLog;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseSystemPrompt() {
        return baseSystemPrompt;
    }

    public void setBaseSystemPrompt(String baseSystemPrompt) {
        this.baseSystemPrompt = baseSystemPrompt;
    }

    public String getTagPromptsSectionHeader() {
        return tagPromptsSectionHeader;
    }

    public void setTagPromptsSectionHeader(String tagPromptsSectionHeader) {
        this.tagPromptsSectionHeader = tagPromptsSectionHeader;
    }

    public String getQuestionTypeSectionHeader() {
        return questionTypeSectionHeader;
    }

    public void setQuestionTypeSectionHeader(String questionTypeSectionHeader) {
        this.questionTypeSectionHeader = questionTypeSectionHeader;
    }

    public String getTagPromptSeparator() {
        return tagPromptSeparator;
    }

    public void setTagPromptSeparator(String tagPromptSeparator) {
        this.tagPromptSeparator = tagPromptSeparator;
    }

    public String getSectionSeparator() {
        return sectionSeparator;
    }

    public void setSectionSeparator(String sectionSeparator) {
        this.sectionSeparator = sectionSeparator;
    }

    public String getFinalInstruction() {
        return finalInstruction;
    }

    public void setFinalInstruction(String finalInstruction) {
        this.finalInstruction = finalInstruction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public ChangeLog getCreatedChangeLog() {
        return createdChangeLog;
    }

    public void setCreatedChangeLog(ChangeLog createdChangeLog) {
        this.createdChangeLog = createdChangeLog;
    }
} 