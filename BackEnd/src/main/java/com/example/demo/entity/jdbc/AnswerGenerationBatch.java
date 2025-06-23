package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 答案生成批次实体类 - JDBC版本
 * 对应数据库表: answer_generation_batches
 */
public class AnswerGenerationBatch {
    // 表名常量
    public static final String TABLE_NAME = "answer_generation_batches";

    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATASET_VERSION_ID = "dataset_version_id";
    public static final String COLUMN_CREATION_TIME = "creation_time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_ANSWER_ASSEMBLY_CONFIG_ID = "answer_assembly_config_id";
    public static final String COLUMN_EVALUATION_ASSEMBLY_CONFIG_ID = "evaluation_assembly_config_id";
    public static final String COLUMN_SINGLE_CHOICE_PROMPT_ID = "single_choice_prompt_id";
    public static final String COLUMN_MULTIPLE_CHOICE_PROMPT_ID = "multiple_choice_prompt_id";
    public static final String COLUMN_SIMPLE_FACT_PROMPT_ID = "simple_fact_prompt_id";
    public static final String COLUMN_SUBJECTIVE_PROMPT_ID = "subjective_prompt_id";
    public static final String COLUMN_GLOBAL_PARAMETERS = "global_parameters";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_COMPLETED_AT = "completed_at";
    public static final String COLUMN_PROGRESS_PERCENTAGE = "progress_percentage";
    public static final String COLUMN_LAST_ACTIVITY_TIME = "last_activity_time";
    public static final String COLUMN_LAST_CHECK_TIME = "last_check_time";
    public static final String COLUMN_RESUME_COUNT = "resume_count";
    public static final String COLUMN_PAUSE_TIME = "pause_time";
    public static final String COLUMN_PAUSE_REASON = "pause_reason";
    public static final String COLUMN_ANSWER_REPEAT_COUNT = "answer_repeat_count";
    public static final String COLUMN_ERROR_MESSAGE = "error_message";
    public static final String COLUMN_PROCESSING_INSTANCE = "processing_instance";
    public static final String COLUMN_LAST_PROCESSED_RUN_ID = "last_processed_run_id";
    
    private Long id;
    private String name;
    private String description;
    private DatasetVersion datasetVersion;
    private LocalDateTime creationTime = LocalDateTime.now();
    private BatchStatus status = BatchStatus.PENDING;
    private AnswerPromptAssemblyConfig answerAssemblyConfig;
    private AnswerQuestionTypePrompt singleChoicePrompt;
    private AnswerQuestionTypePrompt multipleChoicePrompt;
    private AnswerQuestionTypePrompt simpleFactPrompt;
    private AnswerQuestionTypePrompt subjectivePrompt;
    private Map<String, Object> globalParameters;
    private User createdByUser;
    private LocalDateTime completedAt;
    private BigDecimal progressPercentage;
    private LocalDateTime lastActivityTime;
    private LocalDateTime lastCheckTime;
    private Integer resumeCount = 0;
    private LocalDateTime pauseTime;
    private String pauseReason;
    private Integer answerRepeatCount = 1;
    private String errorMessage;
    private String processingInstance;
    private Long lastProcessedRunId;
    
    public enum BatchStatus {
        PENDING,        // 等待中
        GENERATING_ANSWERS,    // 生成回答中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        PAUSED,         // 已暂停
        RESUMING        // 正在恢复
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public AnswerPromptAssemblyConfig getAnswerAssemblyConfig() {
        return answerAssemblyConfig;
    }

    public void setAnswerAssemblyConfig(AnswerPromptAssemblyConfig answerAssemblyConfig) {
        this.answerAssemblyConfig = answerAssemblyConfig;
    }

    public Map<String, Object> getGlobalParameters() {
        return globalParameters;
    }

    public void setGlobalParameters(Map<String, Object> globalParameters) {
        this.globalParameters = globalParameters;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
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

    public Integer getAnswerRepeatCount() {
        return answerRepeatCount;
    }

    public void setAnswerRepeatCount(Integer answerRepeatCount) {
        this.answerRepeatCount = answerRepeatCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProcessingInstance() {
        return processingInstance;
    }

    public void setProcessingInstance(String processingInstance) {
        this.processingInstance = processingInstance;
    }

    public AnswerQuestionTypePrompt getSingleChoicePrompt() {
        return singleChoicePrompt;
    }
    
    public void setSingleChoicePrompt(AnswerQuestionTypePrompt singleChoicePrompt) {
        this.singleChoicePrompt = singleChoicePrompt;
    }
    
    public AnswerQuestionTypePrompt getMultipleChoicePrompt() {
        return multipleChoicePrompt;
    }
    
    public void setMultipleChoicePrompt(AnswerQuestionTypePrompt multipleChoicePrompt) {
        this.multipleChoicePrompt = multipleChoicePrompt;
    }
    
    public AnswerQuestionTypePrompt getSimpleFactPrompt() {
        return simpleFactPrompt;
    }
    
    public void setSimpleFactPrompt(AnswerQuestionTypePrompt simpleFactPrompt) {
        this.simpleFactPrompt = simpleFactPrompt;
    }
    
    public AnswerQuestionTypePrompt getSubjectivePrompt() {
        return subjectivePrompt;
    }
    
    public void setSubjectivePrompt(AnswerQuestionTypePrompt subjectivePrompt) {
        this.subjectivePrompt = subjectivePrompt;
    }

    public Long getLastProcessedRunId() {
        return lastProcessedRunId;
    }

    public void setLastProcessedRunId(Long lastProcessedRunId) {
        this.lastProcessedRunId = lastProcessedRunId;
    }
} 