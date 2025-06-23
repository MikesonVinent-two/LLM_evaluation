package com.example.demo.serializer;

import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.RawQuestionTag;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义RawQuestion序列化器，避免循环引用问题
 */
public class RawQuestionSerializer extends JsonSerializer<RawQuestion> {

    @Override
    public void serialize(RawQuestion rawQuestion, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        
        // 序列化基本字段
        jsonGenerator.writeNumberField("id", rawQuestion.getId());
        
        if (rawQuestion.getSourceUrl() != null) {
            jsonGenerator.writeStringField("sourceUrl", rawQuestion.getSourceUrl());
        }
        
        if (rawQuestion.getSourceSite() != null) {
            jsonGenerator.writeStringField("sourceSite", rawQuestion.getSourceSite());
        }
        
        if (rawQuestion.getTitle() != null) {
            jsonGenerator.writeStringField("title", rawQuestion.getTitle());
        }
        
        if (rawQuestion.getContent() != null) {
            jsonGenerator.writeStringField("content", rawQuestion.getContent());
        }
        
        if (rawQuestion.getCrawlTime() != null) {
            jsonGenerator.writeStringField("crawlTime", rawQuestion.getCrawlTime().toString());
        }
        
        if (rawQuestion.getTags() != null) {
            jsonGenerator.writeStringField("tags", rawQuestion.getTags());
        }
        
        if (rawQuestion.getOtherMetadata() != null) {
            jsonGenerator.writeStringField("otherMetadata", rawQuestion.getOtherMetadata());
        }
        
        // 序列化标签关系，但避免循环引用
        if (rawQuestion.getQuestionTags() != null && !rawQuestion.getQuestionTags().isEmpty()) {
            jsonGenerator.writeArrayFieldStart("questionTags");
            
            for (RawQuestionTag tag : rawQuestion.getQuestionTags()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("id", tag.getId());
                
                // 序列化Tag对象，但不包含RawQuestion引用
                if (tag.getTag() != null) {
                    jsonGenerator.writeObjectFieldStart("tag");
                    jsonGenerator.writeNumberField("id", tag.getTag().getId());
                    jsonGenerator.writeStringField("tagName", tag.getTag().getTagName());
                    jsonGenerator.writeEndObject();
                }
                
                jsonGenerator.writeEndObject();
            }
            
            jsonGenerator.writeEndArray();
        }
        
        jsonGenerator.writeEndObject();
    }
} 