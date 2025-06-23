package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 主观题评测提示语实体类
 */
public class EvaluationSubjectivePrompt {
    
    // 表名
    public static final String TABLE_NAME = "evaluation_subjective_prompts";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PROMPT_TEMPLATE = "prompt_template";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_EVALUATION_CRITERIA_FOCUS = "evaluation_criteria_focus";
    public static final String COLUMN_SCORING_INSTRUCTION = "scoring_instruction";
    public static final String COLUMN_OUTPUT_FORMAT_INSTRUCTION = "output_format_instruction";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_PARENT_PROMPT_ID = "parent_prompt_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private String name;
    private String promptTemplate;
    private String description;
    private Map<String, Object> evaluationCriteriaFocus;
    private String scoringInstruction;
    private String outputFormatInstruction;
    private Boolean isActive = true;
    private String version;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private User createdByUser;
    private EvaluationSubjectivePrompt parentPrompt;
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

    public Map<String, Object> getEvaluationCriteriaFocus() {
        return evaluationCriteriaFocus;
    }

    public void setEvaluationCriteriaFocus(Map<String, Object> evaluationCriteriaFocus) {
        this.evaluationCriteriaFocus = evaluationCriteriaFocus;
    }

    public String getScoringInstruction() {
        return scoringInstruction;
    }

    public void setScoringInstruction(String scoringInstruction) {
        this.scoringInstruction = scoringInstruction;
    }

    public String getOutputFormatInstruction() {
        return outputFormatInstruction;
    }

    public void setOutputFormatInstruction(String outputFormatInstruction) {
        this.outputFormatInstruction = outputFormatInstruction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public EvaluationSubjectivePrompt getParentPrompt() {
        return parentPrompt;
    }

    public void setParentPrompt(EvaluationSubjectivePrompt parentPrompt) {
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