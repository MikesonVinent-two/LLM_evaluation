package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型答案运行实体类
 */
public class ModelAnswerRun {
    
    // 表名
    public static final String TABLE_NAME = "model_answer_runs";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ANSWER_GENERATION_BATCH_ID = "answer_generation_batch_id";
    public static final String COLUMN_LLM_MODEL_ID = "llm_model_id";
    public static final String COLUMN_RUN_NAME = "run_name";
    public static final String COLUMN_RUN_DESCRIPTION = "run_description";
    public static final String COLUMN_RUN_INDEX = "run_index";
    public static final String COLUMN_RUN_TIME = "run_time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_PARAMETERS = "parameters";
    public static final String COLUMN_ERROR_MESSAGE = "error_message";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_LAST_PROCESSED_QUESTION_ID = "last_processed_question_id";
    public static final String COLUMN_LAST_PROCESSED_QUESTION_INDEX = "last_processed_question_index";
    public static final String COLUMN_PROGRESS_PERCENTAGE = "progress_percentage";
    public static final String COLUMN_LAST_ACTIVITY_TIME = "last_activity_time";
    public static final String COLUMN_RESUME_COUNT = "resume_count";
    public static final String COLUMN_PAUSE_TIME = "pause_time";
    public static final String COLUMN_PAUSE_REASON = "pause_reason";
    public static final String COLUMN_COMPLETED_QUESTIONS_COUNT = "completed_questions_count";
    public static final String COLUMN_FAILED_QUESTIONS_COUNT = "failed_questions_count";
    public static final String COLUMN_FAILED_QUESTIONS_IDS = "failed_questions_ids";
    public static final String COLUMN_TOTAL_QUESTIONS_COUNT = "total_questions_count";
    
    private Long id;
    private AnswerGenerationBatch answerGenerationBatch;
    private LlmModel llmModel;
    private String runName;
    private String runDescription;
    private Integer runIndex = 0;
    private LocalDateTime runTime = LocalDateTime.now();
    private RunStatus status = RunStatus.PENDING;
    private Map<String, Object> parameters;
    private String errorMessage;
    private User createdByUser;
    private Long lastProcessedQuestionId;
    private Integer lastProcessedQuestionIndex;
    private BigDecimal progressPercentage;
    private LocalDateTime lastActivityTime;
    private Integer resumeCount = 0;
    private LocalDateTime pauseTime;
    private String pauseReason;
    private Integer completedQuestionsCount = 0;
    private Integer failedQuestionsCount = 0;
    private List<Long> failedQuestionsIds;
    private Integer totalQuestionsCount;

    public enum RunStatus {
        PENDING,             // 等待中
        GENERATING_ANSWERS,  // 正在生成回答
        COMPLETED,           // 已完成
        FAILED,              // 失败
        PAUSED               // 已暂停
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnswerGenerationBatch getAnswerGenerationBatch() {
        return answerGenerationBatch;
    }

    public void setAnswerGenerationBatch(AnswerGenerationBatch answerGenerationBatch) {
        this.answerGenerationBatch = answerGenerationBatch;
    }

    public LlmModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LlmModel llmModel) {
        this.llmModel = llmModel;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getRunDescription() {
        return runDescription;
    }

    public void setRunDescription(String runDescription) {
        this.runDescription = runDescription;
    }

    public Integer getRunIndex() {
        return runIndex;
    }

    public void setRunIndex(Integer runIndex) {
        this.runIndex = runIndex;
    }

    public LocalDateTime getRunTime() {
        return runTime;
    }

    public void setRunTime(LocalDateTime runTime) {
        this.runTime = runTime;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Long getLastProcessedQuestionId() {
        return lastProcessedQuestionId;
    }

    public void setLastProcessedQuestionId(Long lastProcessedQuestionId) {
        this.lastProcessedQuestionId = lastProcessedQuestionId;
    }

    public Integer getLastProcessedQuestionIndex() {
        return lastProcessedQuestionIndex;
    }

    public void setLastProcessedQuestionIndex(Integer lastProcessedQuestionIndex) {
        this.lastProcessedQuestionIndex = lastProcessedQuestionIndex;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public Integer getResumeCount() {
        return resumeCount;
    }

    public void setResumeCount(Integer resumeCount) {
        this.resumeCount = resumeCount;
    }

    public LocalDateTime getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(LocalDateTime pauseTime) {
        this.pauseTime = pauseTime;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public Integer getCompletedQuestionsCount() {
        return completedQuestionsCount;
    }

    public void setCompletedQuestionsCount(Integer completedQuestionsCount) {
        this.completedQuestionsCount = completedQuestionsCount;
    }

    public Integer getFailedQuestionsCount() {
        return failedQuestionsCount;
    }

    public void setFailedQuestionsCount(Integer failedQuestionsCount) {
        this.failedQuestionsCount = failedQuestionsCount;
    }

    public List<Long> getFailedQuestionsIds() {
        return failedQuestionsIds;
    }

    public void setFailedQuestionsIds(List<Long> failedQuestionsIds) {
        this.failedQuestionsIds = failedQuestionsIds;
    }

    public Integer getTotalQuestionsCount() {
        return totalQuestionsCount;
    }

    public void setTotalQuestionsCount(Integer totalQuestionsCount) {
        this.totalQuestionsCount = totalQuestionsCount;
    }
} 