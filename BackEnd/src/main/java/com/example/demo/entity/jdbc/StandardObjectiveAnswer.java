package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * StandardObjectiveAnswer 实体类 - JDBC版本
 * 对应数据库表: standard_objective_answers
 */
public class StandardObjectiveAnswer {
    // 表名常量
    public static final String TABLE_NAME = "standard_objective_answers";

    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARDQUESTION = "standard_question";
    public static final String COLUMN_OPTIONS = "options";
    public static final String COLUMN_CORRECTOPTIONIDS = "correct_option_ids";
    public static final String COLUMN_DETERMINEDBYUSER = "determined_by_user";
    public static final String COLUMN_DETERMINEDTIME = "determined_time";
    public static final String COLUMN_CREATEDCHANGELOG = "created_change_log";
    public static final String COLUMN_DELETEDAT = "deleted_at";

    
    private Long id;

    private StandardQuestion standardQuestion;

    private String options;

    private String correctOptionIds;

    private User determinedByUser;

    private LocalDateTime determinedTime = LocalDateTime.now();

    private ChangeLog createdChangeLog;

    private LocalDateTime deletedAt;

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

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectOptionIds() {
        return correctOptionIds;
    }

    public void setCorrectOptionIds(String correctOptionIds) {
        this.correctOptionIds = correctOptionIds;
    }

    public User getDeterminedByUser() {
        return determinedByUser;
    }

    public void setDeterminedByUser(User determinedByUser) {
        this.determinedByUser = determinedByUser;
    }

    public LocalDateTime getDeterminedTime() {
        return determinedTime;
    }

    public void setDeterminedTime(LocalDateTime determinedTime) {
        this.determinedTime = determinedTime;
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
     * 获取正确选项ID（作为getCorrectOptionIds的别名）
     * @return 正确选项ID
     */
    public String getCorrectIds() {
        return getCorrectOptionIds();
    }
} 