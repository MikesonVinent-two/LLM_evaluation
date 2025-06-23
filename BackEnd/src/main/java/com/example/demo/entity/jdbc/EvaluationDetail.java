package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评测详情实体类 - JDBC版本
 * 对应数据库表: evaluation_details
 */
public class EvaluationDetail {
    // 表名常量
    public static final String TABLE_NAME = "evaluation_details";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_EVALUATION_ID = "evaluation_id";
    public static final String COLUMN_CRITERION_ID = "criterion_id";
    public static final String COLUMN_CRITERION_NAME = "criterion_name";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_COMMENTS = "comments";
    public static final String COLUMN_CREATED_AT = "created_at";
    
    private Long id;
    private Evaluation evaluation;
    private EvaluationCriterion criterion;
    private String criterionName;
    private BigDecimal score;
    private String comments;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public EvaluationCriterion getCriterion() {
        return criterion;
    }

    public void setCriterion(EvaluationCriterion criterion) {
        this.criterion = criterion;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 