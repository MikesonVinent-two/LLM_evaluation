package com.example.demo.converter;

import com.example.demo.entity.jdbc.ChangeType;
import org.springframework.stereotype.Component;

/**
 * ChangeType枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写枚举值而Java代码使用大写枚举的问题
 */
@Component
public class ChangeTypeConverter {

    // 从数据库值转换到枚举
    public ChangeType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ChangeType.valueOf(dbData.toUpperCase());
    }
    
    // 从枚举转换到数据库值
    public String convertToDatabaseColumn(ChangeType attribute) {
        if (attribute == null) {
            return null;
        }
        // 可以选择存储enum的name()（大写加下划线）或getValue()（中文描述）
        // 这里选择存储name()的小写形式，与之前的实现保持一致
        return attribute.name().toLowerCase();
    }
    
    // 为RowMapper提供方便的转换方法
    public ChangeType fromString(String value) {
        return convertToEntityAttribute(value);
    }
} 