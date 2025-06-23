package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.service.WebSocketService;

/**
 * WebSocket测试控制器
 * 用于测试WebSocket消息发送功能
 */
@RestController
@RequestMapping("/websocket-test")
public class WebSocketTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketTestController.class);
    
    private final WebSocketService webSocketService;
    
    @Autowired
    public WebSocketTestController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }
    
    /**
     * 测试发送问题完成消息
     */
    @RequestMapping(value = "/question-completed/{runId}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> testQuestionCompleted(
            @PathVariable Long runId,
            @RequestParam(defaultValue = "1") Long questionId,
            @RequestParam(defaultValue = "1") int completedCount) {
        
        logger.warn("【测试】开始发送问题完成消息: 运行ID={}, 问题ID={}, 完成数量={}", 
            runId, questionId, completedCount);
        
        try {
            // 只使用一种方法发送，避免混淆
            Map<String, Object> payload = new HashMap<>();
            payload.put("runId", runId);
            payload.put("questionId", questionId);
            payload.put("questionText", "【测试】问题完成消息");
            payload.put("repeatIndex", 0);
            payload.put("completedCount", completedCount);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("source", "test-api");
            
            logger.warn("【测试】准备发送到 /topic/run/{} 的问题完成消息", runId);
            
            webSocketService.sendRunMessage(runId, MessageType.QUESTION_COMPLETED, payload);
            
            logger.warn("【测试】问题完成消息发送调用完成");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "测试消息发送成功");
            response.put("runId", runId);
            response.put("questionId", questionId);
            response.put("completedCount", completedCount);
            response.put("destination", "/topic/run/" + runId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("【测试】发送问题完成消息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("stackTrace", e.getStackTrace());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 测试发送运行消息
     */
    @RequestMapping(value = "/run-message/{runId}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> testRunMessage(
            @PathVariable Long runId,
            @RequestParam(defaultValue = "STATUS_CHANGE") String messageType) {
        
        logger.info("测试发送运行消息: 运行ID={}, 消息类型={}", runId, messageType);
        
        try {
            MessageType type = MessageType.valueOf(messageType);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("runId", runId);
            payload.put("message", "测试消息");
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("source", "test-api");
            
            webSocketService.sendRunMessage(runId, type, payload);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "测试消息发送成功");
            response.put("runId", runId);
            response.put("messageType", messageType);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试发送运行消息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 测试发送全局消息
     */
    @RequestMapping(value = "/global-message", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> testGlobalMessage(
            @RequestParam(defaultValue = "NOTIFICATION") String messageType,
            @RequestParam(defaultValue = "测试全局消息") String message) {
        
        logger.info("测试发送全局消息: 消息类型={}, 内容={}", messageType, message);
        
        try {
            MessageType type = MessageType.valueOf(messageType);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("source", "test-api");
            
            webSocketService.sendGlobalMessage(type, payload);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "测试全局消息发送成功");
            response.put("messageType", messageType);
            response.put("content", message);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试发送全局消息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 检查WebSocket状态
     */
    @RequestMapping(value = "/check-status", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> checkWebSocketStatus() {
        logger.info("检查WebSocket状态");
        
        try {
            webSocketService.checkWebSocketStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WebSocket状态检查完成，请查看日志");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("WebSocket状态检查失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
} 