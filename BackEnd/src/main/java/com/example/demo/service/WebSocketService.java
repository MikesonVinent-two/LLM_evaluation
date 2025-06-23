package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.WebSocketMessage;
import com.example.demo.dto.WebSocketMessage.MessageType;

/**
 * WebSocket服务实现类，用于向客户端发送消息
 */
@Service
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * 发送批次相关消息
     * 
     * @param batchId 批次ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendBatchMessage(Long batchId, MessageType type, Map<String, Object> payload) {
        String destination = "/topic/batch/" + batchId;
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送运行相关消息
     * 
     * @param runId 运行ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendRunMessage(Long runId, MessageType type, Map<String, Object> payload) {
        String destination = "/topic/run/" + runId;
        WebSocketMessage message = new WebSocketMessage(type, payload);
        
        // 添加详细日志，特别是对问题完成消息
        if (type == MessageType.QUESTION_COMPLETED) {
            // 详细记录问题完成消息
            Object questionId = payload.get("questionId");
            Object questionText = payload.get("questionText");
            Object completedCount = payload.get("completedCount");
            
            logger.info("准备发送问题完成消息: 运行ID={}, 问题ID={}, 已完成数量={}, 目的地={}",
                runId, questionId, completedCount, destination);
            logger.debug("问题完成消息内容: 问题文本={}", questionText);
        } else {
            // 记录其他类型的消息
            logger.debug("发送运行消息: 运行ID={}, 类型={}, 目的地={}", runId, type, destination);
        }
        
        try {
            // 记录发送前的详细信息
            logger.info("准备发送WebSocket消息: 目的地={}, 类型={}, 消息={}", 
                destination, type, message);
            
            // 使用异步方式发送消息，避免线程上下文问题
            messagingTemplate.convertAndSend(destination, message);
            
            // 记录发送后的状态
            logger.info("WebSocket消息发送完成: 目的地={}, 类型={}", destination, type);
            
            if (type == MessageType.QUESTION_COMPLETED) {
                logger.warn("【重要】问题完成消息发送成功: 运行ID={}, 问题ID={}, 目的地={}", 
                    runId, payload.get("questionId"), destination);
            }
        } catch (Exception e) {
            logger.error("发送WebSocket消息失败: 运行ID={}, 类型={}, 错误={}", 
                runId, type, e.getMessage(), e);
            
            // 如果是问题完成消息发送失败，记录详细错误
            if (type == MessageType.QUESTION_COMPLETED) {
                logger.error("【严重】问题完成消息发送失败: 运行ID={}, 问题ID={}, 目的地={}, 错误详情={}", 
                    runId, payload.get("questionId"), destination, e);
            }
            
            // 重新抛出异常，确保调用方知道发送失败
            throw e;
        }
    }
    
    /**
     * 发送用户相关消息
     * 
     * @param userId 用户ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendUserMessage(Long userId, MessageType type, Map<String, Object> payload) {
        String destination = "/user/" + userId + "/queue/messages";
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送全局消息
     * 
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendGlobalMessage(MessageType type, Map<String, Object> payload) {
        String destination = "/topic/global";
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送状态变更消息
     * 
     * @param entityId 实体ID
     * @param status 状态
     * @param message 消息内容
     */
    public void sendStatusChangeMessage(Long entityId, String status, String message) {
        String destination = "/topic/status/" + entityId;
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.STATUS_CHANGE, 
                Map.of("id", entityId, "status", status, "message", message));
        messagingTemplate.convertAndSend(destination, wsMessage);
    }
    
    /**
     * 发送运行进度消息
     * 
     * @param runId 运行ID
     * @param progress 进度百分比
     * @param message 消息内容
     */
    public void sendRunProgressMessage(Long runId, double progress, String message) {
        String destination = "/topic/progress/run/" + runId;
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.PROGRESS_UPDATE, 
                Map.of("runId", runId, "progress", progress, "message", message));
        messagingTemplate.convertAndSend(destination, wsMessage);
    }
    
    /**
     * 发送错误消息
     *
     * @param entityId 实体ID（批次ID或运行ID）
     * @param errorMessage 错误消息
     * @param status 当前状态（可选）
     */
    public void sendErrorMessage(Long entityId, String errorMessage, Object status) {
        String destination = "/topic/error/" + entityId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", entityId);
        payload.put("error", errorMessage);
        payload.put("timestamp", System.currentTimeMillis());
        
        // 如果提供了状态，也添加进去
        if (status != null) {
            payload.put("status", status.toString());
        }
        
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.ERROR, payload);
        messagingTemplate.convertAndSend(destination, wsMessage);
        
        // 同时发送到全局错误频道
        messagingTemplate.convertAndSend("/topic/errors", wsMessage);
    }
    
    /**
     * 发送所有批次状态更新
     * 
     * @param batches 批次状态列表
     */
    public void sendAllBatchesStatus(List<AnswerGenerationBatchDTO> batches) {
        String destination = "/topic/batches/all";
        Map<String, Object> payload = new HashMap<>();
        payload.put("batches", batches);
        payload.put("timestamp", System.currentTimeMillis());
        
        WebSocketMessage message = new WebSocketMessage(MessageType.STATUS_CHANGE, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 强制发送问题完成消息（用于测试和调试）
     * 
     * @param runId 运行ID
     * @param questionId 问题ID
     * @param completedCount 完成数量
     */
    public void forceQuestionCompletedMessage(Long runId, Long questionId, int completedCount) {
        String destination = "/topic/run/" + runId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", runId);
        payload.put("questionId", questionId);
        payload.put("completedCount", completedCount);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("source", "force-send");
        
        WebSocketMessage message = new WebSocketMessage(MessageType.QUESTION_COMPLETED, payload);
        
        logger.warn("强制发送问题完成消息: 运行ID={}, 问题ID={}, 完成数量={}, 目的地={}",
            runId, questionId, completedCount, destination);
        
        try {
            messagingTemplate.convertAndSend(destination, message);
            logger.warn("强制发送问题完成消息成功: 运行ID={}, 问题ID={}", runId, questionId);
        } catch (Exception e) {
            logger.error("强制发送问题完成消息失败: 运行ID={}, 问题ID={}, 错误={}", 
                runId, questionId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查WebSocket连接状态和发送能力
     */
    public void checkWebSocketStatus() {
        logger.info("=== WebSocket状态检查开始 ===");
        logger.info("SimpMessagingTemplate实例: {}", messagingTemplate != null ? "正常" : "NULL");
        
        try {
            // 尝试发送一个测试消息到全局频道
            Map<String, Object> testPayload = new HashMap<>();
            testPayload.put("test", "connection-check");
            testPayload.put("timestamp", System.currentTimeMillis());
            
            WebSocketMessage testMessage = new WebSocketMessage(MessageType.NOTIFICATION, testPayload);
            messagingTemplate.convertAndSend("/topic/global", testMessage);
            
            logger.info("WebSocket连接测试成功: 全局消息发送正常");
            
            // 测试运行频道
            messagingTemplate.convertAndSend("/topic/run/test", testMessage);
            logger.info("WebSocket连接测试成功: 运行频道消息发送正常");
            
        } catch (Exception e) {
            logger.error("WebSocket连接测试失败: {}", e.getMessage(), e);
        }
        
        logger.info("=== WebSocket状态检查完成 ===");
    }
} 