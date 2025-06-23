package com.example.demo.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.DifficultyLevel;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.exception.EnumDeserializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * Jackson配置类，用于处理枚举的大小写敏感问题和日期时间格式
 */
@Configuration
public class JacksonConfig {

    // 定义多种可接受的日期时间格式
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // ISO标准格式：2025-06-01T04:45:44
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) // 空格分隔格式：2025-06-01 04:45:44
            .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) // 斜杠日期空格分隔格式：2025/06/01 04:45:44
            .toFormatter();

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 启用对单引号的支持
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许使用未引用的字段名
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 允许使用C/C++风格的注释
        objectMapper.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
        objectMapper.configure(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature(), true);
        // 允许使用转义字符
        objectMapper.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);
        // 允许数字含有前导零
        objectMapper.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true);
        // 忽略空值
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 创建自定义模块处理特殊枚举
        SimpleModule module = new SimpleModule();
        
        // 为QuestionType添加自定义反序列化器
        module.addDeserializer(QuestionType.class, new StdScalarDeserializer<QuestionType>(QuestionType.class) {
            @Override
            public QuestionType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return QuestionType.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        // 为DifficultyLevel添加自定义反序列化器
        module.addDeserializer(DifficultyLevel.class, new StdScalarDeserializer<DifficultyLevel>(DifficultyLevel.class) {
            @Override
            public DifficultyLevel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return DifficultyLevel.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        // 为ChangeType添加自定义反序列化器
        module.addDeserializer(ChangeType.class, new StdScalarDeserializer<ChangeType>(ChangeType.class) {
            @Override
            public ChangeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return ChangeType.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        objectMapper.registerModule(module);
        
        // 配置JavaTimeModule以支持多种日期时间格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }
} 
