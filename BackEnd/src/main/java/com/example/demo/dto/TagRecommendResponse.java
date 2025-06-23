package com.example.demo.dto;

import java.util.List;

public class TagRecommendResponse {
    private List<String> tags;
    private List<Double> confidence;
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public List<Double> getConfidence() {
        return confidence;
    }
    
    public void setConfidence(List<Double> confidence) {
        this.confidence = confidence;
    }
} 