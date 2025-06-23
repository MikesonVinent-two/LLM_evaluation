package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * 原始问题实体类
 */
public class RawQuestion {
    
    // 表名
    public static final String TABLE_NAME = "raw_questions";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SOURCE_URL = "source_url";
    public static final String COLUMN_SOURCE_SITE = "source_site";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CRAWL_TIME = "crawl_time";
    public static final String COLUMN_TAGS = "tags";
    public static final String COLUMN_OTHER_METADATA = "other_metadata";
    
    private Long id;
    private String sourceUrl;
    private String sourceSite;
    private String title;
    private String content;
    private LocalDateTime crawlTime;
    private String tags;
    private String otherMetadata;
    private List<RawQuestionTag> questionTags = new ArrayList<>();

    // 构造函数
    public RawQuestion() {
        this.crawlTime = LocalDateTime.now();
    }

    // 添加标签关联
    public void addTag(RawQuestionTag tag) {
        questionTags.add(tag);
        tag.setRawQuestion(this);
    }
    
    // 移除标签关联
    public void removeTag(RawQuestionTag tag) {
        questionTags.remove(tag);
        tag.setRawQuestion(null);
    }

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
    
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
    
    @JsonManagedReference
    public List<RawQuestionTag> getQuestionTags() {
        return questionTags;
    }

    public void setQuestionTags(List<RawQuestionTag> questionTags) {
        this.questionTags = questionTags;
    }
} 