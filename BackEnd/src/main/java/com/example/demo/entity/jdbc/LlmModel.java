package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * LLM模型实体类
 */
public class LlmModel {
    
    // 表名
    public static final String TABLE_NAME = "llm_models";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PROVIDER = "provider";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_API_URL = "api_url";
    public static final String COLUMN_API_KEY = "api_key";
    public static final String COLUMN_API_TYPE = "api_type";
    public static final String COLUMN_MODEL_PARAMETERS = "model_parameters";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private String name;
    private String provider;
    private String version;
    private String description;
    private String apiUrl;
    private String apiKey;
    private String apiType;
    private Map<String, Object> modelParameters;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public Map<String, Object> getModelParameters() {
        return modelParameters;
    }

    public void setModelParameters(Map<String, Object> modelParameters) {
        this.modelParameters = modelParameters;
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