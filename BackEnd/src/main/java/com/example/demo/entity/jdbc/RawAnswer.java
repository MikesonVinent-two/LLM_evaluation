package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;

/**
 * 原始答案实体类
 */
public class RawAnswer {
    
    // 表名
    public static final String TABLE_NAME = "raw_answers";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_RAW_QUESTION_ID = "raw_question_id";
    public static final String COLUMN_AUTHOR_INFO = "author_info";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_PUBLISH_TIME = "publish_time";
    public static final String COLUMN_UPVOTES = "upvotes";
    public static final String COLUMN_IS_ACCEPTED = "is_accepted";
    public static final String COLUMN_OTHER_METADATA = "other_metadata";
    
    private Long id;
    private RawQuestion rawQuestion;
    private String authorInfo;
    private String content;
    private LocalDateTime publishTime;
    private Integer upvotes = 0;
    private Boolean isAccepted;
    private String otherMetadata;

    // 构造函数
    public RawAnswer() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RawQuestion getRawQuestion() {
        return rawQuestion;
    }

    public void setRawQuestion(RawQuestion rawQuestion) {
        this.rawQuestion = rawQuestion;
    }

    public String getAuthorInfo() {
        return authorInfo;
    }

    public void setAuthorInfo(String authorInfo) {
        this.authorInfo = authorInfo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public Boolean getIsAccepted() {
        return isAccepted;
    }

    public void setIsAccepted(Boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 