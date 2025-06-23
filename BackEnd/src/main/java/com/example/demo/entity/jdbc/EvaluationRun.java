package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评测运行实体类
 */
public class EvaluationRun {
    
    // 表名
    public static final String TABLE_NAME = "EVALUATION_RUNS";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MODEL_ANSWER_RUN_ID = "MODEL_ANSWER_RUN_ID";
    public static final String COLUMN_EVALUATOR_ID = "EVALUATOR_ID";
    public static final String COLUMN_RUN_NAME = "RUN_NAME";
    public static final String COLUMN_RUN_DESCRIPTION = "RUN_DESCRIPTION";
    public static final String COLUMN_RUN_TIME = "RUN_TIME";
    public static final String COLUMN_STATUS = "STATUS";
    public static final String COLUMN_PARAMETERS = "PARAMETERS";
    public static final String COLUMN_ERROR_MESSAGE = "ERROR_MESSAGE";
    public static final String COLUMN_CREATED_BY_USER_ID = "CREATED_BY_USER_ID";
    public static final String COLUMN_LAST_PROCESSED_ANSWER_ID = "LAST_PROCESSED_ANSWER_ID";
    public static final String COLUMN_PROGRESS_PERCENTAGE = "PROGRESS_PERCENTAGE";
    public static final String COLUMN_LAST_ACTIVITY_TIME = "LAST_ACTIVITY_TIME";
    public static final String COLUMN_COMPLETED_ANSWERS_COUNT = "COMPLETED_ANSWERS_COUNT";
    public static final String COLUMN_TOTAL_ANSWERS_COUNT = "TOTAL_ANSWERS_COUNT";
    public static final String COLUMN_FAILED_EVALUATIONS_COUNT = "FAILED_EVALUATIONS_COUNT";
    public static final String COLUMN_RESUME_COUNT = "RESUME_COUNT";
    public static final String COLUMN_COMPLETED_AT = "COMPLETED_AT";
    public static final String COLUMN_LAST_CHECKPOINT_ID = "LAST_CHECKPOINT_ID";
    public static final String COLUMN_PAUSE_REASON = "PAUSE_REASON";
    public static final String COLUMN_PAUSE_TIME = "PAUSE_TIME";
    public static final String COLUMN_PAUSED_BY_USER_ID = "PAUSED_BY_USER_ID";
    public static final String COLUMN_TIMEOUT_SECONDS = "TIMEOUT_SECONDS";
    public static final String COLUMN_IS_AUTO_RESUME = "IS_AUTO_RESUME";
    public static final String COLUMN_AUTO_CHECKPOINT_INTERVAL = "AUTO_CHECKPOINT_INTERVAL";
    public static final String COLUMN_CURRENT_BATCH_START_ID = "CURRENT_BATCH_START_ID";
    public static final String COLUMN_CURRENT_BATCH_END_ID = "CURRENT_BATCH_END_ID";
    public static final String COLUMN_BATCH_SIZE = "BATCH_SIZE";
    public static final String COLUMN_RETRY_COUNT = "RETRY_COUNT";
    public static final String COLUMN_MAX_RETRIES = "MAX_RETRIES";
    public static final String COLUMN_LAST_ERROR_TIME = "LAST_ERROR_TIME";
    public static final String COLUMN_CONSECUTIVE_ERRORS = "CONSECUTIVE_ERRORS";
    public static final String COLUMN_START_TIME = "START_TIME";
    public static final String COLUMN_END_TIME = "END_TIME";
    public static final String COLUMN_LAST_UPDATED = "LAST_UPDATED";
    
    private Long id;
    private Long modelAnswerRunId;
    private Long evaluatorId;
    private String runName;
    private String runDescription;
    private LocalDateTime runTime;
    private RunStatus status;
    private String parameters;
    private EvaluationPromptAssemblyConfig evaluationAssemblyConfig;
    private EvaluationSubjectivePrompt subjectivePrompt;
    private String errorMessage;
    private Long createdByUserId;
    private Long lastProcessedAnswerId;
    private BigDecimal progressPercentage;
    private LocalDateTime lastActivityTime;
    private Integer completedAnswersCount = 0;
    private Integer totalAnswersCount;
    private Integer failedEvaluationsCount = 0;
    private Integer resumeCount = 0;
    private LocalDateTime completedAt;
    private Long lastCheckpointId;
    private String pauseReason;
    private LocalDateTime pauseTime;
    private Long pausedByUserId;
    private Integer timeoutSeconds = 3600;
    private Boolean isAutoResume = false;
    private Integer autoCheckpointInterval = 60;
    private Long currentBatchStartId;
    private Long currentBatchEndId;
    private Integer batchSize = 50;
    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    private LocalDateTime lastErrorTime;
    private Integer consecutiveErrors = 0;
    private ModelAnswerRun modelAnswerRun;
    private Evaluator evaluator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime lastUpdated;

    // 运行状态枚举
    public enum RunStatus {
        PENDING,      // 等待中
        IN_PROGRESS,  // 进行中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        PAUSED,       // 暂停
        RESUMING      // 恢复中
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getModelAnswerRunId() {
        return modelAnswerRunId;
    }

    public void setModelAnswerRunId(Long modelAnswerRunId) {
        this.modelAnswerRunId = modelAnswerRunId;
    }

    public Long getEvaluatorId() {
        return evaluatorId;
    }

    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
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

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public EvaluationPromptAssemblyConfig getEvaluationAssemblyConfig() {
        return evaluationAssemblyConfig;
    }

    public void setEvaluationAssemblyConfig(EvaluationPromptAssemblyConfig evaluationAssemblyConfig) {
        this.evaluationAssemblyConfig = evaluationAssemblyConfig;
    }

    public EvaluationSubjectivePrompt getSubjectivePrompt() {
        return subjectivePrompt;
    }

    public void setSubjectivePrompt(EvaluationSubjectivePrompt subjectivePrompt) {
        this.subjectivePrompt = subjectivePrompt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreatedBy() {
        return createdByUserId;
    }

    public void setCreatedBy(Long userId) {
        this.createdByUserId = userId;
    }

    public Long getLastProcessedAnswerId() {
        return lastProcessedAnswerId;
    }

    public void setLastProcessedAnswerId(Long lastProcessedAnswerId) {
        this.lastProcessedAnswerId = lastProcessedAnswerId;
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

    public Integer getCompletedAnswersCount() {
        return completedAnswersCount;
    }

    public void setCompletedAnswersCount(Integer completedAnswersCount) {
        this.completedAnswersCount = completedAnswersCount;
    }

    public Integer getTotalAnswersCount() {
        return totalAnswersCount;
    }

    public void setTotalAnswersCount(Integer totalAnswersCount) {
        this.totalAnswersCount = totalAnswersCount;
    }

    public Integer getFailedEvaluationsCount() {
        return failedEvaluationsCount;
    }

    public void setFailedEvaluationsCount(Integer failedEvaluationsCount) {
        this.failedEvaluationsCount = failedEvaluationsCount;
    }

    public Integer getResumeCount() {
        return resumeCount;
    }

    public void setResumeCount(Integer resumeCount) {
        this.resumeCount = resumeCount;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getLastCheckpointId() {
        return lastCheckpointId;
    }

    public void setLastCheckpointId(Long lastCheckpointId) {
        this.lastCheckpointId = lastCheckpointId;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public LocalDateTime getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(LocalDateTime pauseTime) {
        this.pauseTime = pauseTime;
    }

    public Long getPausedByUserId() {
        return pausedByUserId;
    }

    public void setPausedByUserId(Long pausedByUserId) {
        this.pausedByUserId = pausedByUserId;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getIsAutoResume() {
        return isAutoResume;
    }

    public void setIsAutoResume(Boolean isAutoResume) {
        this.isAutoResume = isAutoResume;
    }

    public Integer getAutoCheckpointInterval() {
        return autoCheckpointInterval;
    }

    public void setAutoCheckpointInterval(Integer autoCheckpointInterval) {
        this.autoCheckpointInterval = autoCheckpointInterval;
    }

    public Long getCurrentBatchStartId() {
        return currentBatchStartId;
    }

    public void setCurrentBatchStartId(Long currentBatchStartId) {
        this.currentBatchStartId = currentBatchStartId;
    }

    public Long getCurrentBatchEndId() {
        return currentBatchEndId;
    }

    public void setCurrentBatchEndId(Long currentBatchEndId) {
        this.currentBatchEndId = currentBatchEndId;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getLastErrorTime() {
        return lastErrorTime;
    }

    public void setLastErrorTime(LocalDateTime lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    public Integer getConsecutiveErrors() {
        return consecutiveErrors;
    }

    public void setConsecutiveErrors(Integer consecutiveErrors) {
        this.consecutiveErrors = consecutiveErrors;
    }

    public ModelAnswerRun getModelAnswerRun() {
        return modelAnswerRun;
    }

    public void setModelAnswerRun(ModelAnswerRun modelAnswerRun) {
        this.modelAnswerRun = modelAnswerRun;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 