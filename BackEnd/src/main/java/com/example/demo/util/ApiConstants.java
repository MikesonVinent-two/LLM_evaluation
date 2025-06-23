package com.example.demo.util;

/**
 * API常量类，用于存储API响应中重复使用的键名和其他常量
 */
public final class ApiConstants {
    
    // 禁止实例化
    private ApiConstants() {}
    
    // 通用响应键
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ERROR = "error";
    public static final String KEY_STATUS = "status";
    public static final String KEY_TIMESTAMP = "timestamp";
    
    // 评估相关键
    public static final String KEY_SCORE = "score";
    public static final String KEY_COMMENTS = "comments";
    public static final String KEY_BATCH_ID = "batchId";
    public static final String KEY_RUN_ID = "runId";
    
    // 状态常量
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
} 