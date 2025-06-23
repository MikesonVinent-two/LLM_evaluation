package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 评测实体类 - JDBC版本
 * 对应数据库表: evaluations
 */
public class Evaluation {
    // 表名常量
    public static final String TABLE_NAME = "evaluations";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LLM_ANSWER_ID = "llm_answer_id";
    public static final String COLUMN_EVALUATOR_ID = "evaluator_id";
    public static final String COLUMN_EVALUATION_RUN_ID = "evaluation_run_id";
    public static final String COLUMN_EVALUATION_TYPE = "evaluation_type";
    public static final String COLUMN_OVERALL_SCORE = "overall_score";
    public static final String COLUMN_EVALUATION_TIME = "evaluation_time";
    public static final String COLUMN_EVALUATION_STATUS = "evaluation_status";
    public static final String COLUMN_ERROR_MESSAGE = "error_message";
    public static final String COLUMN_EVALUATION_RESULTS = "evaluation_results";
    public static final String COLUMN_PROMPT_USED = "prompt_used";
    public static final String COLUMN_COMMENTS = "comments";
    public static final String COLUMN_RAW_EVALUATOR_RESPONSE = "raw_evaluator_response";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_CREATION_TIME = "creation_time";
    public static final String COLUMN_COMPLETION_TIME = "completion_time";
    public static final String COLUMN_RAW_SCORE = "raw_score";
    public static final String COLUMN_NORMALIZED_SCORE = "normalized_score";
    public static final String COLUMN_WEIGHTED_SCORE = "weighted_score";
    public static final String COLUMN_SCORE_TYPE = "score_type";
    public static final String COLUMN_SCORING_METHOD = "scoring_method";
    
    private Long id;
    private LlmAnswer llmAnswer;
    private Evaluator evaluator;
    private EvaluationRun evaluationRun;
    private EvaluationType evaluationType = EvaluationType.AI_MODEL;
    private BigDecimal score;
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private EvaluationStatus status = EvaluationStatus.PENDING;
    private String errorMessage;
    private Map<String, Object> evaluationResults;
    private String promptUsed;
    private String comments;
    private String rawEvaluatorResponse;
    private User createdByUser;
    private ChangeLog createdChangeLog;
    private LocalDateTime creationTime;
    private LocalDateTime completionTime;
    private BigDecimal rawScore;
    private BigDecimal normalizedScore;
    private BigDecimal weightedScore;
    private String scoreType;
    private String scoringMethod;

    // 评测状态枚举
    public enum EvaluationStatus {
        SUCCESS,        // 评测成功
        FAILED,         // 评测失败
        PENDING,        // 待评测
        PROCESSING      // 评测进行中
    }

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
    
    public EvaluationRun getEvaluationRun() {
        return evaluationRun;
    }

    public void setEvaluationRun(EvaluationRun evaluationRun) {
        this.evaluationRun = evaluationRun;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(LocalDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getEvaluationResults() {
        return evaluationResults;
    }

    public void setEvaluationResults(Map<String, Object> evaluationResults) {
        this.evaluationResults = evaluationResults;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRawEvaluatorResponse() {
        return rawEvaluatorResponse;
    }

    public void setRawEvaluatorResponse(String rawEvaluatorResponse) {
        this.rawEvaluatorResponse = rawEvaluatorResponse;
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

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public EvaluationType getEvaluationType() {
        return evaluationType;
    }

    public void setEvaluationType(EvaluationType evaluationType) {
        this.evaluationType = evaluationType;
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
    
    /**
     * 获取关联的问题
     * 
     * @return 标准问题实体
     */
    public StandardQuestion getQuestion() {
        if (llmAnswer != null && llmAnswer.getDatasetQuestionMapping() != null) {
            return llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
        }
        return null;
    }
    
    /**
     * 获取回答文本
     * 
     * @return 回答文本
     */
    public String getAnswerText() {
        if (llmAnswer != null) {
            return llmAnswer.getAnswerText();
        }
        return null;
    }

    // 创建新的Evaluation实体时，设置默认的评测类型
    public Evaluation() {
        this.evaluationType = EvaluationType.AI_MODEL; // 设置默认值为AI_MODEL
    }
} 