package com.example.demo.entity.jdbc;

import com.example.demo.exception.EnumDeserializationException;

public enum DifficultyLevel {
    EASY,    // 简单
    MEDIUM,  // 中等
    HARD;    // 困难
    
    /**
     * 根据字符串查找对应的难度级别，忽略大小写
     * @param value 难度级别字符串
     * @return 对应的难度级别枚举值
     * @throws EnumDeserializationException 如果找不到匹配的枚举值
     */
    public static DifficultyLevel fromString(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return DifficultyLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 尝试模糊匹配
            for (DifficultyLevel level : DifficultyLevel.values()) {
                if (level.name().equalsIgnoreCase(value)) {
                    return level;
                }
            }
            
            // 所有尝试都失败了，抛出自定义异常
            throw new EnumDeserializationException("difficulty", value, DifficultyLevel.class);
        }
    }
} 