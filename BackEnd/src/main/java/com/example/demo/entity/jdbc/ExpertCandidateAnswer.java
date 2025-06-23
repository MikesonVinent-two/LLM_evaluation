package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 专家候选答案实体类
 */
public class ExpertCandidateAnswer {
    
    // 表名
    public static final String TABLE_NAME = "expert_candidate_answers";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_STANDARD_QUESTION_ID = "standard_question_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CANDIDATE_ANSWER_TEXT = "candidate_answer_text";
    public static final String COLUMN_SUBMISSION_TIME = "submission_time";
    public static final String COLUMN_QUALITY_SCORE = "quality_score";
    public static final String COLUMN_FEEDBACK = "feedback";
    
    private Long id;
    private StandardQuestion standardQuestion;
    private User user;
    private String candidateAnswerText;
    private LocalDateTime submissionTime = LocalDateTime.now();
    private Integer qualityScore;
    private String feedback;

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
} 