package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.util.MetadataUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RawQuestionDTO {
    
    @NotBlank(message = "来源URL不能为空")
    private String sourceUrl;
    
    private String sourceSite;
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 512, message = "标题长度不能超过512个字符")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private LocalDateTime crawlTime;
    
    // 改为Object类型，以支持JSON对象和字符串
    private Object otherMetadata;
    
    private List<String> tags;
    
    // 构造函数
    public RawQuestionDTO() {
    }
    
    // Getters and Setters
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
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
} 