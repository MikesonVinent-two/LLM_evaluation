package com.example.demo.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.WebSocketMessage;
import com.example.demo.service.AnswerGenerationService;
import com.example.demo.service.WebSocketService;

/**
 * WebSocket控制器，处理WebSocket连接和消息
 */
@Controller
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    private final WebSocketService webSocketService;
    private final AnswerGenerationService answerGenerationService;
    
    @Autowired
    public WebSocketController(WebSocketService webSocketService, AnswerGenerationService answerGenerationService) {
        this.webSocketService = webSocketService;
        this.answerGenerationService = answerGenerationService;
    }
    
    /**
     * 处理客户端发送的批次订阅确认消息
     */
    @MessageMapping("/batch/{batchId}/subscribe")
    @SendTo("/topic/batch/{batchId}")
    public WebSocketMessage confirmBatchSubscription(@DestinationVariable Long batchId, Principal principal) {
        String userName = (principal != null) ? principal.getName() : "anonymous";
        logger.debug("用户 {} 订阅批次消息: {}", userName, batchId);
        
        // 模拟一个可能发生的错误
        // if (batchId == 0) {
        //     throw new IllegalArgumentException("批次ID不能为0");
        // }

        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", batchId);
        payload.put("subscribed", true);
        payload.put("message", "成功订阅批次 " + batchId + " 的消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }
    
    /**
     * 处理客户端发送的运行订阅确认消息
     */
    @MessageMapping("/run/{runId}/subscribe")
    @SendTo("/topic/run/{runId}")
    public WebSocketMessage confirmRunSubscription(@DestinationVariable Long runId, Principal principal) {
        String userName = (principal != null) ? principal.getName() : "anonymous";
        logger.debug("用户 {} 订阅运行消息: {}", userName, runId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", runId);
        payload.put("subscribed", true);
        payload.put("message", "成功订阅运行 " + runId + " 的消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }
    
    /**
     * 处理客户端发送的全局订阅确认消息
     */
    @MessageMapping("/global/subscribe")
    @SendTo("/topic/global")
    public WebSocketMessage confirmGlobalSubscription(Principal principal) {
        String userName = (principal != null) ? principal.getName() : "anonymous";
        logger.debug("用户 {} 订阅全局消息", userName);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribed", true);
        payload.put("message", "成功订阅全局消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }

    /**
     * 处理客户端发送的所有批次状态订阅消息
     */
    @MessageMapping("/batches/all/subscribe")
    @SendTo("/topic/batches/all")
    public WebSocketMessage subscribeAllBatches(Principal principal) {
        String userName = (principal != null) ? principal.getName() : "anonymous";
        logger.debug("用户 {} 订阅所有批次状态", userName);
        
        // 获取所有批次的当前状态
        List<AnswerGenerationBatchDTO> batches = answerGenerationService.getAllBatches();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("batches", batches);
        payload.put("subscribed", true);
        payload.put("message", "成功订阅所有批次状态");
        payload.put("timestamp", System.currentTimeMillis());
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }

    /**
     * 处理来自@MessageMapping方法的异常.
     * 将错误信息发送给导致错误的用户.
     * @param exception 发生的异常
     * @param principal 发送消息的用户 (如果可用)
     * @return 发送给用户的错误消息
     */
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public WebSocketMessage handleException(Exception exception, Principal principal) {
        String userName = (principal != null) ? principal.getName() : "anonymous";
        logger.error("WebSocket消息处理错误，用户: {}, 错误: {}", userName, exception.getMessage(), exception);

        Map<String, Object> payload = new HashMap<>();
        payload.put("error", "处理您的请求时发生内部错误。");
        // 出于安全考虑，通常不应将原始异常消息直接暴露给客户端
        // payload.put("details", exception.getMessage()); 
        payload.put("type", exception.getClass().getSimpleName());
        payload.put("source", "system-error-handler");

        return new WebSocketMessage(WebSocketMessage.MessageType.ERROR, payload);
    }
} 