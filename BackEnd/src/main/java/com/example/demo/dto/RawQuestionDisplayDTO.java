package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RawQuestionDisplayDTO {
    private Long id;
    private String sourceUrl;
    private String sourceSite;
    private String title;
    private String content;
    private LocalDateTime crawlTime;
    private List<String> tags;
    private boolean isStandardized;  // 是否已标准化
    private Long standardQuestionId; // 如果已标准化，对应的标准问题ID

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceSite() {
        return sourceSite;
    }

    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }

    public void setCrawlTime(LocalDateTime crawlTime) {
        this.crawlTime = crawlTime;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isStandardized() {
        return isStandardized;
    }

    public void setStandardized(boolean standardized) {
        isStandardized = standardized;
    }

    public Long getStandardQuestionId() {
        return standardQuestionId;
    }

    public void setStandardQuestionId(Long standardQuestionId) {
        this.standardQuestionId = standardQuestionId;
    }
} 