package com.example.demo.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class CrowdsourcedAnswerDTO {
    private Long id;
    
    @NotNull(message = "标准问题ID不能为空")
    private Long standardQuestionId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "回答内容不能为空")
    private String answerText;
    
    private LocalDateTime submissionTime;
    private String qualityReviewStatus;  // PENDING, ACCEPTED, REJECTED, FLAGGED
    private Long reviewedByUserId;
    private LocalDateTime reviewTime;
    private String reviewFeedback;
    private String userUsername;  // 添加提交者的用户名
    private String reviewerUsername;  // 添加审核者的用户名
    private Long taskBatchId;  // 任务批次ID

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStandardQuestionId() {
        return standardQuestionId;
    }

    public void setStandardQuestionId(Long standardQuestionId) {
        this.standardQuestionId = standardQuestionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getQualityReviewStatus() {
        return qualityReviewStatus;
    }

    public void setQualityReviewStatus(String qualityReviewStatus) {
        this.qualityReviewStatus = qualityReviewStatus;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public void setReviewedByUserId(Long reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
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

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public Long getTaskBatchId() {
        return taskBatchId;
    }

    public void setTaskBatchId(Long taskBatchId) {
        this.taskBatchId = taskBatchId;
    }
} 