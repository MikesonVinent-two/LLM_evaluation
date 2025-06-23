package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 众包答案实体类 - JDBC版本
 * 对应数据库表: crowdsourced_answers
 */
public class CrowdsourcedAnswer {
    // 表名常量
    public static final String TABLE_NAME = "crowdsourced_answers";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARD_QUESTION_ID = "standard_question_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ANSWER_TEXT = "answer_text";
    public static final String COLUMN_SUBMISSION_TIME = "submission_time";
    public static final String COLUMN_TASK_BATCH_ID = "task_batch_id";
    public static final String COLUMN_QUALITY_REVIEW_STATUS = "quality_review_status";
    public static final String COLUMN_REVIEWED_BY_USER_ID = "reviewed_by_user_id";
    public static final String COLUMN_REVIEW_TIME = "review_time";
    public static final String COLUMN_REVIEW_FEEDBACK = "review_feedback";
    public static final String COLUMN_OTHER_METADATA = "other_metadata";
    
    private Long id;
    private StandardQuestion standardQuestion;
    private User user;
    private String answerText;
    private LocalDateTime submissionTime = LocalDateTime.now();
    private Long taskBatchId;
    private QualityReviewStatus qualityReviewStatus = QualityReviewStatus.PENDING;
    private User reviewedByUser;
    private LocalDateTime reviewTime;
    private String reviewFeedback;
    private String otherMetadata;

    // 质量审核状态枚举
    public enum QualityReviewStatus {
        PENDING,    // 待审核
        ACCEPTED,   // 已接受
        REJECTED,   // 已拒绝
        FLAGGED     // 已标记
    }

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Long getTaskBatchId() {
        return taskBatchId;
    }

    public void setTaskBatchId(Long taskBatchId) {
        this.taskBatchId = taskBatchId;
    }

    public QualityReviewStatus getQualityReviewStatus() {
        return qualityReviewStatus;
    }

    public void setQualityReviewStatus(QualityReviewStatus qualityReviewStatus) {
        this.qualityReviewStatus = qualityReviewStatus;
    }

    public User getReviewedByUser() {
        return reviewedByUser;
    }

    public void setReviewedByUser(User reviewedByUser) {
        this.reviewedByUser = reviewedByUser;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewFeedback() {
        return reviewFeedback;
    }

    public void setReviewFeedback(String reviewFeedback) {
        this.reviewFeedback = reviewFeedback;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 