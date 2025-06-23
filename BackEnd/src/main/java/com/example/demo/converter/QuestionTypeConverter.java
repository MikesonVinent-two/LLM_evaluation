package com.example.demo.converter;

import com.example.demo.entity.jdbc.QuestionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * QuestionType枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写枚举值而Java代码使用大写枚举的问题
 */
@Component
public class QuestionTypeConverter {

    // 从数据库值转换到枚举
    public QuestionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return QuestionType.fromString(dbData);
    }
    
    // 从枚举转换到数据库值
    public String convertToDatabaseColumn(QuestionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }
    
    // 为RowMapper提供方便的转换方法
    public QuestionType fromString(String value) {
        return convertToEntityAttribute(value);
    }
} 