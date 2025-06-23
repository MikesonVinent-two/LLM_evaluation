package com.example.demo.entity.jdbc;

/**
 * 评测类型枚举
 */
public enum EvaluationType {
    /**
     * 人工评测
     */
    MANUAL,
    
    /**
     * 自动评测（系统规则）
     */
    AUTO,
    
    /**
     * AI评测（大模型）
     */
    AI_MODEL
} 