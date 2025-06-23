package com.example.demo.converter;

import com.example.demo.entity.jdbc.EntityType;
import org.springframework.stereotype.Component;

/**
 * EntityType枚举与数据库字符串之间的自动转换器
 * 用于解决数据库存储小写下划线形式而Java代码使用大写枚举的问题
 */
@Component
public class EntityTypeConverter {

    // 从数据库值转换到枚举
    public EntityType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return EntityType.valueOf(dbData.toUpperCase());
    }
    
    // 从枚举转换到数据库值
    public String convertToDatabaseColumn(EntityType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }
    
    // 为RowMapper提供方便的转换方法
    public EntityType fromString(String value) {
        return convertToEntityAttribute(value);
    }
} 