package com.example.demo.entity.jdbc;

import com.example.demo.exception.EnumDeserializationException;

public enum QuestionType {
    SINGLE_CHOICE,    // 单选题
    MULTIPLE_CHOICE,  // 多选题
    SIMPLE_FACT,     // 简单事实题
    SUBJECTIVE;      // 主观题
    
    /**
     * 根据字符串查找对应的问题类型，忽略大小写
     * @param value 问题类型字符串
     * @return 对应的问题类型枚举值
     * @throws EnumDeserializationException 如果找不到匹配的枚举值
     */
    public static QuestionType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return QuestionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 尝试模糊匹配
            for (QuestionType type : QuestionType.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            
            // 处理特殊情况，如下划线与横杠的替换
            if (value.contains("_")) {
                try {
                    return fromString(value.replace("_", "-"));
                } catch (EnumDeserializationException ex) {
                    // 忽略这个异常，继续尝试其他方法
                }
            } else if (value.contains("-")) {
                try {
                    return fromString(value.replace("-", "_"));
                } catch (EnumDeserializationException ex) {
                    // 忽略这个异常，继续尝试其他方法
                }
            }
            
            // 所有尝试都失败了，抛出自定义异常
            throw new EnumDeserializationException("questionType", value, QuestionType.class);
        }
    }
} 