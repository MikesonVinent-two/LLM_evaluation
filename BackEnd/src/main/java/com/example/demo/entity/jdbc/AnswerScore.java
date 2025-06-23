package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回答分数实体类 - JDBC版本
 * 对应数据库表: answer_scores
 */
public class AnswerScore {
    // 表名常量
    public static final String TABLE_NAME = "answer_scores";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LLM_ANSWER_ID = "llm_answer_id";
    public static final String COLUMN_EVALUATOR_ID = "evaluator_id";
    public static final String COLUMN_RAW_SCORE = "raw_score";
    public static final String COLUMN_NORMALIZED_SCORE = "normalized_score";
    public static final String COLUMN_WEIGHTED_SCORE = "weighted_score";
    public static final String COLUMN_SCORE_TYPE = "score_type";
    public static final String COLUMN_SCORING_METHOD = "scoring_method";
    public static final String COLUMN_EVALUATION_ID = "evaluation_id";
    public static final String COLUMN_SCORING_TIME = "scoring_time";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_COMMENTS = "comments";
    
    private Long id;
    private LlmAnswer llmAnswer;
    private Evaluator evaluator;
    private BigDecimal rawScore;
    private BigDecimal normalizedScore;
    private BigDecimal weightedScore;
    private String scoreType;
    private String scoringMethod;
    private Evaluation evaluation;
    private LocalDateTime scoringTime = LocalDateTime.now();
    private User createdByUser;
    private String comments;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LlmAnswer getLlmAnswer() {
        return llmAnswer;
    }

    public void setLlmAnswer(LlmAnswer llmAnswer) {
        this.llmAnswer = llmAnswer;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getNormalizedScore() {
        return normalizedScore;
    }

    public void setNormalizedScore(BigDecimal normalizedScore) {
        this.normalizedScore = normalizedScore;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public void setWeightedScore(BigDecimal weightedScore) {
        this.weightedScore = weightedScore;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public String getScoringMethod() {
        return scoringMethod;
    }

    public void setScoringMethod(String scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public LocalDateTime getScoringTime() {
        return scoringTime;
    }

    public void setScoringTime(LocalDateTime scoringTime) {
        this.scoringTime = scoringTime;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
} 