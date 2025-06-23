package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * StandardSubjectiveAnswer 实体类 - JDBC版本
 * 对应数据库表: standard_subjective_answers
 */
public class StandardSubjectiveAnswer {
    // 表名常量
    public static final String TABLE_NAME = "standard_subjective_answers";

    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARDQUESTION = "standard_question";
    public static final String COLUMN_ANSWERTEXT = "answer_text";
    public static final String COLUMN_SCORINGGUIDANCE = "scoring_guidance";
    public static final String COLUMN_DETERMINEDBYUSER = "determined_by_user";
    public static final String COLUMN_DETERMINEDTIME = "determined_time";
    public static final String COLUMN_CREATEDCHANGELOG = "created_change_log";
    public static final String COLUMN_DELETEDAT = "deleted_at";

    
    private Long id;

    private StandardQuestion standardQuestion;

    private String answerText;

    private String scoringGuidance;

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

    public String getScoringGuidance() {
        return scoringGuidance;
    }

    public void setScoringGuidance(String scoringGuidance) {
        this.scoringGuidance = scoringGuidance;
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