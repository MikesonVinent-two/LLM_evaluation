package com.example.demo.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class ExpertCandidateAnswerDTO {
    private Long id;
    
    @NotNull(message = "标准问题ID不能为空")
    private Long standardQuestionId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "答案内容不能为空")
    private String candidateAnswerText;
    
    private LocalDateTime submissionTime;
    private Integer qualityScore;
    private String feedback;
    private String userUsername;  // 提交者的用户名
    
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
    
    public String getCandidateAnswerText() {
        return candidateAnswerText;
    }
    
    public void setCandidateAnswerText(String candidateAnswerText) {
        this.candidateAnswerText = candidateAnswerText;
    }
    
    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }
    
    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }
    
    public Integer getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getUserUsername() {
        return userUsername;
    }
    
    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }
} 