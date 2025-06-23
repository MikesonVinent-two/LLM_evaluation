package com.example.demo.dto;

import java.util.List;

import com.example.demo.entity.jdbc.DifficultyLevel;
import com.example.demo.entity.jdbc.QuestionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StandardQuestionDTO {
    
    private Long id;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    private Long originalRawQuestionId;
    
    @NotBlank(message = "问题文本不能为空")
    private String questionText;
    
    @NotNull(message = "问题类型不能为空")
    private QuestionType questionType;
    
    private DifficultyLevel difficulty;
    
    private Long parentStandardQuestionId;
    
    private String commitMessage;
    
    private List<String> tags;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getOriginalRawQuestionId() {
        return originalRawQuestionId;
    }
    
    public void setOriginalRawQuestionId(Long originalRawQuestionId) {
        this.originalRawQuestionId = originalRawQuestionId;
    }
    
    public String getQuestionText() {
        return questionText;
    }
    
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    
    public QuestionType getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
    
    public Long getParentStandardQuestionId() {
        return parentStandardQuestionId;
    }
    
    public void setParentStandardQuestionId(Long parentStandardQuestionId) {
        this.parentStandardQuestionId = parentStandardQuestionId;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
} 