package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * LLM模型生成答案实体类
 */
public class LlmAnswer {
    
    // 表名
    public static final String TABLE_NAME = "llm_answers";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MODEL_ANSWER_RUN_ID = "model_answer_run_id";
    public static final String COLUMN_DATASET_QUESTION_MAPPING_ID = "dataset_question_mapping_id";
    public static final String COLUMN_ANSWER_TEXT = "answer_text";
    public static final String COLUMN_GENERATION_STATUS = "generation_status";
    public static final String COLUMN_ERROR_MESSAGE = "error_message";
    public static final String COLUMN_GENERATION_TIME = "generation_time";
    public static final String COLUMN_PROMPT_USED = "prompt_used";
    public static final String COLUMN_RAW_MODEL_RESPONSE = "raw_model_response";
    public static final String COLUMN_OTHER_METADATA = "other_metadata";
    public static final String COLUMN_REPEAT_INDEX = "repeat_index";
    
    private Long id;
    private ModelAnswerRun modelAnswerRun;
    private DatasetQuestionMapping datasetQuestionMapping;
    private String answerText;
    private GenerationStatus generationStatus;
    private String errorMessage;
    private LocalDateTime generationTime;
    private String promptUsed;
    private String rawModelResponse;
    private String otherMetadata;
    private Integer repeatIndex = 0;

    // 生成状态枚举
    public enum GenerationStatus {
        SUCCESS,    // 生成成功
        FAILED      // 生成失败
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ModelAnswerRun getModelAnswerRun() {
        return modelAnswerRun;
    }

    public void setModelAnswerRun(ModelAnswerRun modelAnswerRun) {
        this.modelAnswerRun = modelAnswerRun;
    }

    public DatasetQuestionMapping getDatasetQuestionMapping() {
        return datasetQuestionMapping;
    }

    public void setDatasetQuestionMapping(DatasetQuestionMapping datasetQuestionMapping) {
        this.datasetQuestionMapping = datasetQuestionMapping;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(GenerationStatus generationStatus) {
        this.generationStatus = generationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(LocalDateTime generationTime) {
        this.generationTime = generationTime;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    public String getRawModelResponse() {
        return rawModelResponse;
    }

    public void setRawModelResponse(String rawModelResponse) {
        this.rawModelResponse = rawModelResponse;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }

    public Integer getRepeatIndex() {
        return repeatIndex;
    }

    public void setRepeatIndex(Integer repeatIndex) {
        this.repeatIndex = repeatIndex;
    }
} 