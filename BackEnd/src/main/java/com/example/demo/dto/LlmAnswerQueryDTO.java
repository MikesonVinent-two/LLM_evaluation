package com.example.demo.dto;

/**
 * LLM回答查询参数DTO，用于封装查询参数
 */
public class LlmAnswerQueryDTO {

    /**
     * 评测员ID
     */
    private Long evaluatorId;

    /**
     * 是否只返回未评测的回答
     */
    private Boolean onlyUnevaluated = false;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 标签搜索
     */
    private String tag;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 批次ID
     */
    private Long batchId;
    
    /**
     * 问题类型（SINGLE_CHOICE, MULTIPLE_CHOICE, SIMPLE_FACT, SUBJECTIVE）
     */
    private String questionType;
    
    /**
     * 获取评测员ID
     * @return 评测员ID
     */
    public Long getEvaluatorId() {
        return evaluatorId;
    }
    
    /**
     * 设置评测员ID
     * @param evaluatorId 评测员ID
     */
    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
    }
    
    /**
     * 是否只返回未评测的回答
     * @return 是否只返回未评测的回答
     */
    public Boolean getOnlyUnevaluated() {
        return onlyUnevaluated;
    }
    
    /**
     * 设置是否只返回未评测的回答
     * @param onlyUnevaluated 是否只返回未评测的回答
     */
    public void setOnlyUnevaluated(Boolean onlyUnevaluated) {
        this.onlyUnevaluated = onlyUnevaluated;
    }
    
    /**
     * 获取关键词
     * @return 关键词
     */
    public String getKeyword() {
        return keyword;
    }
    
    /**
     * 设置关键词
     * @param keyword 关键词
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    /**
     * 获取标签
     * @return 标签
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * 设置标签
     * @param tag 标签
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    /**
     * 获取页码
     * @return 页码
     */
    public Integer getPage() {
        return page;
    }
    
    /**
     * 设置页码
     * @param page 页码
     */
    public void setPage(Integer page) {
        this.page = page;
    }
    
    /**
     * 获取每页大小
     * @return 每页大小
     */
    public Integer getSize() {
        return size;
    }
    
    /**
     * 设置每页大小
     * @param size 每页大小
     */
    public void setSize(Integer size) {
        this.size = size;
    }
    
    /**
     * 获取批次ID
     * @return 批次ID
     */
    public Long getBatchId() {
        return batchId;
    }
    
    /**
     * 设置批次ID
     * @param batchId 批次ID
     */
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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
    
    @Override
    public String toString() {
        return "LlmAnswerQueryDTO{" +
                "evaluatorId=" + evaluatorId +
                ", onlyUnevaluated=" + onlyUnevaluated +
                ", keyword='" + keyword + '\'' +
                ", tag='" + tag + '\'' +
                ", page=" + page +
                ", size=" + size +
                ", batchId=" + batchId +
                ", questionType='" + questionType + '\'' +
                '}';
    }
} 