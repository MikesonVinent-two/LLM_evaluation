package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;

public class AnswerGenerationBatchDTO {
    
    private Long id;
    private String name;
    private String description;
    private Long datasetVersionId;
    private String datasetVersionName;
    private BatchStatus status;
    private LocalDateTime creationTime;
    private Long answerAssemblyConfigId;
    private Map<String, Object> globalParameters;
    private Long createdByUserId;
    private String createdByUsername;
    private LocalDateTime completedAt;
    private BigDecimal progressPercentage;
    private LocalDateTime lastActivityTime;
    private Integer resumeCount;
    private LocalDateTime pauseTime;
    private String pauseReason;
    private Integer answerRepeatCount;
    private Integer totalRuns;
    private Integer pendingRuns;
    private Integer completedRuns;
    private Integer failedRuns;
    private Long lastProcessedRunId;
    
    // 题型prompt相关字段
    private Long singleChoicePromptId;
    private String singleChoicePromptName;
    private Long multipleChoicePromptId;
    private String multipleChoicePromptName;
    private Long simpleFactPromptId;
    private String simpleFactPromptName;
    private Long subjectivePromptId;
    private String subjectivePromptName;
    
    // Constructors
    public AnswerGenerationBatchDTO() {
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
    
    public Long getDatasetVersionId() {
        return datasetVersionId;
    }
    
    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }
    
    public String getDatasetVersionName() {
        return datasetVersionName;
    }
    
    public void setDatasetVersionName(String datasetVersionName) {
        this.datasetVersionName = datasetVersionName;
    }
    
    public BatchStatus getStatus() {
        return status;
    }
    
    public void setStatus(BatchStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public Long getAnswerAssemblyConfigId() {
        return answerAssemblyConfigId;
    }
    
    public void setAnswerAssemblyConfigId(Long answerAssemblyConfigId) {
        this.answerAssemblyConfigId = answerAssemblyConfigId;
    }
    
    public Map<String, Object> getGlobalParameters() {
        return globalParameters;
    }
    
    public void setGlobalParameters(Map<String, Object> globalParameters) {
        this.globalParameters = globalParameters;
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
    
    public Integer getTotalRuns() {
        return totalRuns;
    }
    
    public void setTotalRuns(Integer totalRuns) {
        this.totalRuns = totalRuns;
    }
    
    public Integer getPendingRuns() {
        return pendingRuns;
    }
    
    public void setPendingRuns(Integer pendingRuns) {
        this.pendingRuns = pendingRuns;
    }
    
    public Integer getCompletedRuns() {
        return completedRuns;
    }
    
    public void setCompletedRuns(Integer completedRuns) {
        this.completedRuns = completedRuns;
    }
    
    public Integer getFailedRuns() {
        return failedRuns;
    }
    
    public void setFailedRuns(Integer failedRuns) {
        this.failedRuns = failedRuns;
    }
    
    public Long getLastProcessedRunId() {
        return lastProcessedRunId;
    }
    
    public void setLastProcessedRunId(Long lastProcessedRunId) {
        this.lastProcessedRunId = lastProcessedRunId;
    }
    
    // 题型prompt的getter和setter
    public Long getSingleChoicePromptId() {
        return singleChoicePromptId;
    }
    
    public void setSingleChoicePromptId(Long singleChoicePromptId) {
        this.singleChoicePromptId = singleChoicePromptId;
    }
    
    public String getSingleChoicePromptName() {
        return singleChoicePromptName;
    }
    
    public void setSingleChoicePromptName(String singleChoicePromptName) {
        this.singleChoicePromptName = singleChoicePromptName;
    }
    
    public Long getMultipleChoicePromptId() {
        return multipleChoicePromptId;
    }
    
    public void setMultipleChoicePromptId(Long multipleChoicePromptId) {
        this.multipleChoicePromptId = multipleChoicePromptId;
    }
    
    public String getMultipleChoicePromptName() {
        return multipleChoicePromptName;
    }
    
    public void setMultipleChoicePromptName(String multipleChoicePromptName) {
        this.multipleChoicePromptName = multipleChoicePromptName;
    }
    
    public Long getSimpleFactPromptId() {
        return simpleFactPromptId;
    }
    
    public void setSimpleFactPromptId(Long simpleFactPromptId) {
        this.simpleFactPromptId = simpleFactPromptId;
    }
    
    public String getSimpleFactPromptName() {
        return simpleFactPromptName;
    }
    
    public void setSimpleFactPromptName(String simpleFactPromptName) {
        this.simpleFactPromptName = simpleFactPromptName;
    }
    
    public Long getSubjectivePromptId() {
        return subjectivePromptId;
    }
    
    public void setSubjectivePromptId(Long subjectivePromptId) {
        this.subjectivePromptId = subjectivePromptId;
    }
    
    public String getSubjectivePromptName() {
        return subjectivePromptName;
    }
    
    public void setSubjectivePromptName(String subjectivePromptName) {
        this.subjectivePromptName = subjectivePromptName;
    }
} 