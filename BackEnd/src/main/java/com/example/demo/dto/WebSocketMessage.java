package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket消息数据传输对象
 */
public class WebSocketMessage {
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 进度更新
        PROGRESS_UPDATE,
        // 状态变更
        STATUS_CHANGE,
        // 任务开始
        TASK_STARTED,
        // 任务完成
        TASK_COMPLETED,
        // 任务暂停
        TASK_PAUSED,
        // 任务恢复
        TASK_RESUMED,
        // 任务失败
        TASK_FAILED,
        // 问题开始处理
        QUESTION_STARTED,
        // 问题处理完成
        QUESTION_COMPLETED,
        // 问题处理失败
        QUESTION_FAILED,
        // 错误消息
        ERROR,
        // 系统通知
        NOTIFICATION
    }
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 消息内容
     */
    private Map<String, Object> payload;
    
    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 默认构造函数
     */
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     * 
     * @param type 消息类型
     * @param payload 消息内容
     */
    public WebSocketMessage(MessageType type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
} 