package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * StandardSimpleAnswer 实体类 - JDBC版本
 * 对应数据库表: standard_simple_answers
 */
public class StandardSimpleAnswer {
    // 表名常量
    public static final String TABLE_NAME = "standard_simple_answers";

    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARDQUESTION = "standard_question";
    public static final String COLUMN_ANSWERTEXT = "answer_text";
    public static final String COLUMN_ALTERNATIVEANSWERS = "alternative_answers";
    public static final String COLUMN_DETERMINEDBYUSER = "determined_by_user";
    public static final String COLUMN_DETERMINEDTIME = "determined_time";
    public static final String COLUMN_CREATEDCHANGELOG = "created_change_log";
    public static final String COLUMN_DELETEDAT = "deleted_at";

    
    private Long id;

    private StandardQuestion standardQuestion;

    private String answerText;

    private String alternativeAnswers;

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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getAlternativeAnswers() {
        return alternativeAnswers;
    }

    public void setAlternativeAnswers(String alternativeAnswers) {
        this.alternativeAnswers = alternativeAnswers;
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
} 