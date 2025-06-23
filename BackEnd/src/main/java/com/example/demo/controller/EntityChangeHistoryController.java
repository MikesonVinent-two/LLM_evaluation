package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.service.EntityChangeHistoryService;

/**
 * 实体变更历史控制器
 */
@RestController
@RequestMapping("/entity-history")
public class EntityChangeHistoryController {

    private final EntityChangeHistoryService entityChangeHistoryService;

    @Autowired
    public EntityChangeHistoryController(EntityChangeHistoryService entityChangeHistoryService) {
        this.entityChangeHistoryService = entityChangeHistoryService;
    }

    /**
     * 获取实体的变更历史
     */
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<Map<String, Object>>> getEntityChangeHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        // 解析实体类型
        EntityType entityTypeEnum;
        try {
            entityTypeEnum = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的实体类型");
        }
        
        List<Map<String, Object>> history = entityChangeHistoryService.getEntityChangeHistory(entityTypeEnum, entityId);
        return ResponseEntity.ok(history);
    }

    /**
     * 获取实体属性的变更历史
     */
    @GetMapping("/{entityType}/{entityId}/attribute/{attributeName}")
    public ResponseEntity<List<Map<String, Object>>> getAttributeChangeHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PathVariable String attributeName) {
        
        // 解析实体类型
        EntityType entityTypeEnum;
        try {
            entityTypeEnum = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的实体类型");
        }
        
        List<Map<String, Object>> history = entityChangeHistoryService.getAttributeChangeHistory(
                entityTypeEnum, entityId, attributeName);
        return ResponseEntity.ok(history);
    }

    /**
     * 获取实体当前状态
     */
    @GetMapping("/{entityType}/{entityId}/current-state")
    public ResponseEntity<Map<String, Object>> getEntityCurrentState(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        // 解析实体类型
        EntityType entityTypeEnum;
        try {
            entityTypeEnum = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的实体类型");
        }
        
        Map<String, Object> currentState = entityChangeHistoryService.getEntityCurrentState(
                entityTypeEnum, entityId);
        return ResponseEntity.ok(currentState);
    }

    /**
     * 获取用户的变更统计
     */
    @GetMapping("/user-statistics/{userId}")
    public ResponseEntity<Map<String, Object>> getUserChangeStatistics(
            @PathVariable Long userId) {
        
        Map<String, Object> statistics = entityChangeHistoryService.getUserChangeStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
} 