package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 数据集问题映射实体类 - JDBC版本
 * 对应数据库表: dataset_question_mapping
 */
public class DatasetQuestionMapping {
    // 表名常量
    public static final String TABLE_NAME = "dataset_question_mapping";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DATASET_VERSION_ID = "dataset_version_id";
    public static final String COLUMN_STANDARD_QUESTION_ID = "standard_question_id";
    public static final String COLUMN_ORDER_IN_DATASET = "order_in_dataset";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    
    private Long id;
    private DatasetVersion datasetVersion;
    private StandardQuestion standardQuestion;
    private Integer orderInDataset;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private ChangeLog createdChangeLog;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public StandardQuestion getStandardQuestion() {
        return standardQuestion;
    }

    public void setStandardQuestion(StandardQuestion standardQuestion) {
        this.standardQuestion = standardQuestion;
    }

    public Integer getOrderInDataset() {
        return orderInDataset;
    }

    public void setOrderInDataset(Integer orderInDataset) {
        this.orderInDataset = orderInDataset;
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