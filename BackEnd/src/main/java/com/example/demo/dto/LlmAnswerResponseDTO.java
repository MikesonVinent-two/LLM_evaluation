package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM回答响应DTO，包含LLM回答、标准问题和标准答案
 */
public class LlmAnswerResponseDTO {

    /**
     * LLM回答ID
     */
    private Long id;

    /**
     * 模型回答运行ID
     */
    private Long modelAnswerRunId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 回答内容
     */
    private String answerText;

    /**
     * 生成时间
     */
    private LocalDateTime generationTime;

    /**
     * 重复索引
     */
    private Integer repeatIndex;

    /**
     * 标准问题ID
     */
    private Long questionId;

    /**
     * 标准问题文本
     */
    private String questionText;

    /**
     * 问题类型
     */
    private String questionType;

    /**
     * 标准答案
     */
    private String standardAnswer;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 是否已评测
     */
    private Boolean evaluated;

    /**
     * 获取LLM回答ID
     * @return LLM回答ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置LLM回答ID
     * @param id LLM回答ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取模型回答运行ID
     * @return 模型回答运行ID
     */
    public Long getModelAnswerRunId() {
        return modelAnswerRunId;
    }

    /**
     * 设置模型回答运行ID
     * @param modelAnswerRunId 模型回答运行ID
     */
    public void setModelAnswerRunId(Long modelAnswerRunId) {
        this.modelAnswerRunId = modelAnswerRunId;
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 设置模型名称
     * @param modelName 模型名称
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * 获取回答内容
     * @return 回答内容
     */
    public String getAnswerText() {
        return answerText;
    }

    /**
     * 设置回答内容
     * @param answerText 回答内容
     */
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    /**
     * 获取生成时间
     * @return 生成时间
     */
    public LocalDateTime getGenerationTime() {
        return generationTime;
    }

    /**
     * 设置生成时间
     * @param generationTime 生成时间
     */
    public void setGenerationTime(LocalDateTime generationTime) {
        this.generationTime = generationTime;
    }

    /**
     * 获取重复索引
     * @return 重复索引
     */
    public Integer getRepeatIndex() {
        return repeatIndex;
    }

    /**
     * 设置重复索引
     * @param repeatIndex 重复索引
     */
    public void setRepeatIndex(Integer repeatIndex) {
        this.repeatIndex = repeatIndex;
    }

    /**
     * 获取标准问题ID
     * @return 标准问题ID
     */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * 设置标准问题ID
     * @param questionId 标准问题ID
     */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    /**
     * 获取标准问题文本
     * @return 标准问题文本
     */
    public String getQuestionText() {
        return questionText;
    }

    /**
     * 设置标准问题文本
     * @param questionText 标准问题文本
     */
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    /**
     * 获取问题类型
     * @return 问题类型
     */
    public String getQuestionType() {
        return questionType;
    }

    /**
     * 设置问题类型
     * @param questionType 问题类型
     */
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    /**
     * 获取标准答案
     * @return 标准答案
     */
    public String getStandardAnswer() {
        return standardAnswer;
    }

    /**
     * 设置标准答案
     * @param standardAnswer 标准答案
     */
    public void setStandardAnswer(String standardAnswer) {
        this.standardAnswer = standardAnswer;
    }

    /**
     * 获取标签列表
     * @return 标签列表
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 设置标签列表
     * @param tags 标签列表
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 是否已评测
     * @return 是否已评测
     */
    public Boolean getEvaluated() {
        return evaluated;
    }

    /**
     * 设置是否已评测
     * @param evaluated 是否已评测
     */
    public void setEvaluated(Boolean evaluated) {
        this.evaluated = evaluated;
    }
} 