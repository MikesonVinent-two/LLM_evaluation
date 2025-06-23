package com.example.demo.dto;

import com.example.demo.entity.jdbc.QuestionType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardAnswerDTO {
    private Long standardQuestionId;
    private Long userId;
    private QuestionType questionType;
    private String commitMessage;
    
    // 答案内容
    private String answerText;  // 所有类型的问题都需要答案文本
    
    // 客观题特有字段
    private String options;      // JSON格式的选项列表，仅用于客观题
    private String correctIds;   // JSON格式的正确答案ID列表，仅用于客观题
    
    // 简单题特有字段
    private String alternativeAnswers;  // JSON格式的可选答案列表，仅用于简单题
    
    // 主观题特有字段
    private String scoringGuidance;  // 评分指导，仅用于主观题
    
    // Getters and Setters
    public Long getStandardQuestionId() {
        return standardQuestionId;
    }
    
    public void setStandardQuestionId(Long standardQuestionId) {
        this.standardQuestionId = standardQuestionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public QuestionType getQuestionType() {
        return questionType;
    }
    
    @JsonCreator
    public void setQuestionType(@JsonProperty("questionType") String questionTypeStr) {
        if (questionTypeStr != null) {
            this.questionType = QuestionType.fromString(questionTypeStr);
        }
    }
    
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public String getCorrectIds() {
        return correctIds;
    }
    
    public void setCorrectIds(String correctIds) {
        this.correctIds = correctIds;
    }
    
    public String getAlternativeAnswers() {
        return alternativeAnswers;
    }
    
    public void setAlternativeAnswers(String alternativeAnswers) {
        this.alternativeAnswers = alternativeAnswers;
    }
    
    public String getScoringGuidance() {
        return scoringGuidance;
    }
    
    public void setScoringGuidance(String scoringGuidance) {
        this.scoringGuidance = scoringGuidance;
    }
} 