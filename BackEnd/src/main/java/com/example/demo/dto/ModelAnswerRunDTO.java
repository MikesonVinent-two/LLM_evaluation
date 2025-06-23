package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.demo.entity.jdbc.ModelAnswerRun.RunStatus;

public class ModelAnswerRunDTO {
    
    private Long id;
    private Long answerGenerationBatchId;
    private String batchName;
    private Long llmModelId;
    private String modelName;
    private String modelProvider;
    private String runName;
    private String runDescription;
    private Integer runIndex;
    private LocalDateTime runTime;
    private RunStatus status;
    private Map<String, Object> parameters;
    private String errorMessage;
    private Long createdByUserId;
    private String createdByUsername;
    private BigDecimal progressPercentage;
    private LocalDateTime lastActivityTime;
    private Integer resumeCount;
    private LocalDateTime pauseTime;
    private String pauseReason;
    private Integer completedQuestionsCount;
    private Integer totalQuestionsCount;
    private Integer failedQuestionsCount;
    private List<Long> failedQuestionsIds;
    private Long lastProcessedQuestionId;
    private Integer lastProcessedQuestionIndex;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime creationTime;
    private Long createdBy;
    private Long evaluationRunId;
    
    // Constructors
    public ModelAnswerRunDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAnswerGenerationBatchId() {
        return answerGenerationBatchId;
    }
    
    public void setAnswerGenerationBatchId(Long answerGenerationBatchId) {
        this.answerGenerationBatchId = answerGenerationBatchId;
    }
    
    public String getBatchName() {
        return batchName;
    }
    
    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }
    
    public Long getLlmModelId() {
        return llmModelId;
    }
    
    public void setLlmModelId(Long llmModelId) {
        this.llmModelId = llmModelId;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getModelProvider() {
        return modelProvider;
    }
    
    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
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
    
    public Long getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public String getCreatedByUsername() {
        return createdByUsername;
    }
    
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
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
    
    public Integer getTotalQuestionsCount() {
        return totalQuestionsCount;
    }
    
    public void setTotalQuestionsCount(Integer totalQuestionsCount) {
        this.totalQuestionsCount = totalQuestionsCount;
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
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public Long getEvaluationRunId() {
        return evaluationRunId;
    }
    
    public void setEvaluationRunId(Long evaluationRunId) {
        this.evaluationRunId = evaluationRunId;
    }
} 