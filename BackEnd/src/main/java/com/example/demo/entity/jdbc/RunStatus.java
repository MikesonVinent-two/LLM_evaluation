package com.example.demo.entity.jdbc;

/**
 * 评测运行状态枚举
 */
public enum RunStatus {
    /** 等待中 */
    PENDING,
    /** 进行中 */
    IN_PROGRESS,
    /** 已完成 */
    COMPLETED,
    /** 失败 */
    FAILED,
    /** 暂停 */
    PAUSED,
    /** 恢复中 */
    RESUMING,
    /** 新创建 */
    CREATED,
    /** 运行中 */
    RUNNING,
    /** 错误 */
    ERROR;
} 