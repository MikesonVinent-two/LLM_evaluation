package com.example.demo.dto;

import java.util.List;

public class TagRecommendRequest {
    private String text;
    private String questionType;
    private List<String> existingTags;
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public List<String> getExistingTags() {
        return existingTags;
    }
    
    public void setExistingTags(List<String> existingTags) {
        this.existingTags = existingTags;
    }
} 