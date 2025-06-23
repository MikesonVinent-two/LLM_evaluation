package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 评测者实体类，包括人类评测者和AI评测者
 */
public class Evaluator {
    
    // 表名
    public static final String TABLE_NAME = "evaluators";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_EVALUATOR_TYPE = "evaluator_type";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_LLM_MODEL_ID = "llm_model_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    /**
     * 评测者类型枚举
     */
    public enum EvaluatorType {
        HUMAN,    // 人类评测者
        AI_MODEL  // AI模型评测者
    }
    
    private Long id;
    private EvaluatorType evaluatorType;
    private User user;
    private LlmModel llmModel;
    private String name;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EvaluatorType getEvaluatorType() {
        return evaluatorType;
    }

    public void setEvaluatorType(EvaluatorType evaluatorType) {
        this.evaluatorType = evaluatorType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LlmModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LlmModel llmModel) {
        this.llmModel = llmModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    /**
     * 判断是否为AI评测者
     * 
     * @return 是否为AI评测者
     */
    public boolean isAiEvaluator() {
        return evaluatorType == EvaluatorType.AI_MODEL;
    }
    
    /**
     * 判断是否为人类评测者
     * 
     * @return 是否为人类评测者
     */
    public boolean isHumanEvaluator() {
        return evaluatorType == EvaluatorType.HUMAN;
    }
}