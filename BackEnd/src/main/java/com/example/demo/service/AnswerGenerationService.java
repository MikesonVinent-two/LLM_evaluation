package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.ModelAnswerRunDTO;

public interface AnswerGenerationService {
    
    /**
     * 创建新的回答生成批次
     * 
     * @param request 批次创建请求
     * @return 创建的批次DTO
     */
    AnswerGenerationBatchDTO createBatch(AnswerGenerationBatchCreateRequest request);
    
    /**
     * 启动批次执行
     * 
     * @param batchId 批次ID
     */
    void startBatch(Long batchId);
    
    /**
     * 暂停批次执行
     * 
     * @param batchId 批次ID
     * @param reason 暂停原因
     */
    void pauseBatch(Long batchId, String reason);
    
    /**
     * 恢复批次执行
     * 
     * @param batchId 批次ID
     */
    void resumeBatch(Long batchId);
    
    /**
     * 重置失败的批次状态为暂停
     * 
     * @param batchId 批次ID
     */
    void resetFailedBatch(Long batchId);
    
    /**
     * 强制恢复批次执行，不依赖于状态更新
     * 
     * @param batchId 批次ID
     */
    void forceBatchResume(Long batchId);
    
    /**
     * 发送错误通知
     * 
     * @param batchId 批次ID
     * @param errorData 错误数据
     */
    void sendErrorNotification(Long batchId, Map<String, Object> errorData);
    
    /**
     * 获取批次状态
     * 
     * @param batchId 批次ID
     * @return 批次DTO
     */
    AnswerGenerationBatchDTO getBatchStatus(Long batchId);
    
    /**
     * 获取特定运行的状态
     * 
     * @param runId 运行ID
     * @return 运行DTO
     */
    ModelAnswerRunDTO getRunStatus(Long runId);
    
    /**
     * 获取用户创建的所有批次
     * 
     * @param userId 用户ID
     * @return 批次DTO列表
     */
    List<AnswerGenerationBatchDTO> getBatchesByUserId(Long userId);
    
    /**
     * 获取批次的所有运行
     * 
     * @param batchId 批次ID
     * @return 运行DTO列表
     */
    List<ModelAnswerRunDTO> getRunsByBatchId(Long batchId);
    
    /**
     * 获取特定模型的所有运行
     * 
     * @param modelId 模型ID
     * @return 运行DTO列表
     */
    List<ModelAnswerRunDTO> getRunsByModelId(Long modelId);
    
    /**
     * 测试系统连通性，检查所有已配置模型的连通状态
     * 
     * @return 包含测试结果的Map，key为模型ID或名称，value为连通性测试结果
     */
    Map<String, Object> testSystemConnectivity();
    
    /**
     * 测试特定模型的连通性
     * 
     * @param modelId 模型ID
     * @return 包含测试结果的Map
     */
    Map<String, Object> testModelConnectivity(Long modelId);
    
    /**
     * 测试特定批次关联的所有模型连通性
     * 
     * @param batchId 批次ID
     * @return 包含所有模型测试结果的Map
     */
    Map<String, Object> testBatchModelsConnectivity(Long batchId);
    
    /**
     * 获取所有批次
     * 
     * @return 所有批次的列表
     */
    List<AnswerGenerationBatchDTO> getAllBatches();
    
    /**
     * 批次创建请求内部类
     */
    public static class AnswerGenerationBatchCreateRequest {
        private String name;
        private String description;
        private Long datasetVersionId;
        private Long answerAssemblyConfigId;
        private List<Long> llmModelIds;
        private java.util.Map<String, Object> globalParameters;
        private java.util.Map<Long, java.util.Map<String, Object>> modelSpecificParameters;
        private Integer answerRepeatCount;
        private Long userId;
        
        // 题型prompt配置
        private Long singleChoicePromptId;    // 单选题prompt ID
        private Long multipleChoicePromptId;  // 多选题prompt ID
        private Long simpleFactPromptId;      // 简单事实题prompt ID
        private Long subjectivePromptId;      // 主观题prompt ID
        
        // Getters and Setters
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
        
        public Long getAnswerAssemblyConfigId() {
            return answerAssemblyConfigId;
        }
        
        public void setAnswerAssemblyConfigId(Long answerAssemblyConfigId) {
            this.answerAssemblyConfigId = answerAssemblyConfigId;
        }
        
        public List<Long> getLlmModelIds() {
            return llmModelIds;
        }
        
        public void setLlmModelIds(List<Long> llmModelIds) {
            this.llmModelIds = llmModelIds;
        }
        
        public java.util.Map<String, Object> getGlobalParameters() {
            return globalParameters;
        }
        
        public void setGlobalParameters(java.util.Map<String, Object> globalParameters) {
            this.globalParameters = globalParameters;
        }
        
        public java.util.Map<Long, java.util.Map<String, Object>> getModelSpecificParameters() {
            return modelSpecificParameters;
        }
        
        public void setModelSpecificParameters(java.util.Map<Long, java.util.Map<String, Object>> modelSpecificParameters) {
            this.modelSpecificParameters = modelSpecificParameters;
        }
        
        public Integer getAnswerRepeatCount() {
            return answerRepeatCount;
        }
        
        public void setAnswerRepeatCount(Integer answerRepeatCount) {
            this.answerRepeatCount = answerRepeatCount;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public Long getSingleChoicePromptId() {
            return singleChoicePromptId;
        }
        
        public void setSingleChoicePromptId(Long singleChoicePromptId) {
            this.singleChoicePromptId = singleChoicePromptId;
        }
        
        public Long getMultipleChoicePromptId() {
            return multipleChoicePromptId;
        }
        
        public void setMultipleChoicePromptId(Long multipleChoicePromptId) {
            this.multipleChoicePromptId = multipleChoicePromptId;
        }
        
        public Long getSimpleFactPromptId() {
            return simpleFactPromptId;
        }
        
        public void setSimpleFactPromptId(Long simpleFactPromptId) {
            this.simpleFactPromptId = simpleFactPromptId;
        }
        
        public Long getSubjectivePromptId() {
            return subjectivePromptId;
        }
        
        public void setSubjectivePromptId(Long subjectivePromptId) {
            this.subjectivePromptId = subjectivePromptId;
        }
    }
} 