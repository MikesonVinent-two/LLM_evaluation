package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.util.MetadataUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RawAnswerDTO {
    
    @NotNull(message = "问题ID不能为空")
    private Long rawQuestionId;
    
    private String authorInfo;
    
    @NotBlank(message = "回答内容不能为空")
    private String content;
    
    private LocalDateTime publishTime;
    
    private Integer upvotes;
    
    private Boolean isAccepted;
    
    // 改为Object类型，以支持JSON对象和字符串
    private Object otherMetadata;
    
    // 构造函数
    public RawAnswerDTO() {
    }
    
    // Getters and Setters
    public Long getRawQuestionId() {
        return rawQuestionId;
    }
    
    public void setRawQuestionId(Long rawQuestionId) {
        this.rawQuestionId = rawQuestionId;
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
    
    /**
     * 获取元数据
     * @return 元数据对象
     */
    public Object getOtherMetadata() {
        return otherMetadata;
    }
    
    /**
     * 获取元数据的字符串表示
     * @return 元数据的字符串表示
     */
    public String getOtherMetadataAsString() {
        if (otherMetadata == null) {
            return null;
        }
        return MetadataUtils.normalizeMetadata(otherMetadata);
    }
    
    /**
     * 设置元数据
     * @param otherMetadata 元数据对象
     */
    public void setOtherMetadata(Object otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 