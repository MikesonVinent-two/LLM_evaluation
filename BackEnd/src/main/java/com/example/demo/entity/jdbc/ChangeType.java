package com.example.demo.entity.jdbc;

import com.example.demo.exception.EnumDeserializationException;

public enum ChangeType {
    CREATE_STANDARD_ANSWER("创建标准答案"),
    UPDATE_STANDARD_ANSWER("更新标准答案"),
    DELETE_STANDARD_ANSWER("删除标准答案"),
    CREATE_STANDARD_QUESTION("创建标准问题"),
    UPDATE_STANDARD_QUESTION("更新标准问题"),
    DELETE_STANDARD_QUESTION("删除标准问题"),
    UPDATE_STANDARD_QUESTION_TAGS("更新标准问题标签"),
    
    CREATE_OBJECTIVE_ANSWER("创建客观题答案"),
    UPDATE_OBJECTIVE_ANSWER("更新客观题答案"),
    DELETE_OBJECTIVE_ANSWER("删除客观题答案"),
    
    CREATE_SIMPLE_ANSWER("创建简答题答案"),
    UPDATE_SIMPLE_ANSWER("更新简答题答案"),
    DELETE_SIMPLE_ANSWER("删除简答题答案"),
    
    CREATE_SUBJECTIVE_ANSWER("创建主观题答案"),
    UPDATE_SUBJECTIVE_ANSWER("更新主观题答案"),
    DELETE_SUBJECTIVE_ANSWER("删除主观题答案"),
    
    CREATE_CHECKLIST_ITEM("创建评分检查项"),
    UPDATE_CHECKLIST_ITEM("更新评分检查项"),
    DELETE_CHECKLIST_ITEM("删除评分检查项"),
    
    CREATE_PROMPT_ASSEMBLY_CONFIG("创建提示词组装配置"),
    
    /**
     * 回滚标准答案
     */
    ROLLBACK_STANDARD_ANSWER("回滚标准答案"),
    
    /**
     * 回滚标准问题
     */
    ROLLBACK_STANDARD_QUESTION("回滚标准问题");

    private final String value;

    ChangeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串查找对应的变更类型，忽略大小写
     * @param value 变更类型字符串
     * @return 对应的变更类型枚举值
     * @throws EnumDeserializationException 如果找不到匹配的枚举值
     */
    public static ChangeType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return ChangeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 尝试匹配名称
            for (ChangeType type : ChangeType.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
                // 检查value字段是否匹配
                if (type.getValue().equals(value)) {
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
            throw new EnumDeserializationException("changeType", value, ChangeType.class);
        }
    }
} 