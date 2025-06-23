package com.example.demo.entity.jdbc;

/**
 * 评测状态枚举
 */
public enum EvaluationStatus {
    /** 等待中 */
    PENDING,
    /** 进行中 */
    IN_PROGRESS,
    /** 已完成 */
    COMPLETED,
    /** 失败 */
    FAILED,
    /** 成功 */
    SUCCESS;
} 