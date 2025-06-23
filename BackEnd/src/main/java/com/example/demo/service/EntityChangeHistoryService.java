package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 实体变更历史服务类
 */
@Service
public class EntityChangeHistoryService {

    private final ChangeLogService changeLogService;
    private final ChangeLogRepository changeLogRepository;
    private final ChangeLogDetailRepository changeLogDetailRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public EntityChangeHistoryService(ChangeLogService changeLogService,
                                     ChangeLogRepository changeLogRepository,
                                     ChangeLogDetailRepository changeLogDetailRepository,
                                     UserRepository userRepository,
                                     ObjectMapper objectMapper) {
        this.changeLogService = changeLogService;
        this.changeLogRepository = changeLogRepository;
        this.changeLogDetailRepository = changeLogDetailRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 获取实体的变更历史
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 变更历史列表，按时间排序
     */
    public List<Map<String, Object>> getEntityChangeHistory(EntityType entityType, Long entityId) {
        // 获取该实体的所有变更详情
        List<ChangeLogDetail> allDetails = changeLogService.findDetailsByEntityTypeAndEntityId(entityType, entityId);
        
        // 按变更日志ID分组
        Map<Long, List<ChangeLogDetail>> detailsByChangeLogId = allDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getChangeLog().getId()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 处理每个变更日志
        for (Map.Entry<Long, List<ChangeLogDetail>> entry : detailsByChangeLogId.entrySet()) {
            Long changeLogId = entry.getKey();
            List<ChangeLogDetail> details = entry.getValue();
            
            // 获取变更日志
            Optional<ChangeLog> changeLogOpt = changeLogRepository.findById(changeLogId);
            if (changeLogOpt.isPresent()) {
                ChangeLog changeLog = changeLogOpt.get();
                
                // 创建结果条目
                Map<String, Object> historyItem = new HashMap<>();
                historyItem.put("changeLogId", changeLog.getId());
                historyItem.put("changeType", changeLog.getChangeType().name());
                historyItem.put("commitTime", changeLog.getCommitTime());
                historyItem.put("commitMessage", changeLog.getCommitMessage());
                
                // 获取用户信息
                if (changeLog.getUser() != null) {
                    User user = changeLog.getUser();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    historyItem.put("user", userInfo);
                }
                
                // 处理变更详情
                List<Map<String, Object>> detailsList = new ArrayList<>();
                for (ChangeLogDetail detail : details) {
                    Map<String, Object> detailMap = new HashMap<>();
                    detailMap.put("id", detail.getId());
                    detailMap.put("attributeName", detail.getAttributeName());
                    
                    // 尝试将JSON字符串转换为对象
                    try {
                        if (detail.getOldValue() != null) {
                            detailMap.put("oldValue", objectMapper.readValue(detail.getOldValue(), Object.class));
                        } else {
                            detailMap.put("oldValue", null);
                        }
                        
                        if (detail.getNewValue() != null) {
                            detailMap.put("newValue", objectMapper.readValue(detail.getNewValue(), Object.class));
                        } else {
                            detailMap.put("newValue", null);
                        }
                    } catch (JsonProcessingException e) {
                        // 如果解析失败，则直接使用原始字符串
                        detailMap.put("oldValue", detail.getOldValue());
                        detailMap.put("newValue", detail.getNewValue());
                    }
                    
                    detailsList.add(detailMap);
                }
                
                historyItem.put("details", detailsList);
                result.add(historyItem);
            }
        }
        
        // 按时间排序，最新的变更排在前面
        result.sort((a, b) -> {
            return ((java.time.LocalDateTime)b.get("commitTime")).compareTo((java.time.LocalDateTime)a.get("commitTime"));
        });
        
        return result;
    }
    
    /**
     * 获取实体属性的变更历史
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param attributeName 属性名
     * @return 属性变更历史，按时间排序
     */
    public List<Map<String, Object>> getAttributeChangeHistory(EntityType entityType, Long entityId, String attributeName) {
        // 获取该实体的所有变更详情
        List<ChangeLogDetail> allDetails = changeLogService.findDetailsByEntityTypeAndEntityId(entityType, entityId);
        
        // 过滤出指定属性的变更
        List<ChangeLogDetail> attributeDetails = allDetails.stream()
                .filter(detail -> attributeName.equals(detail.getAttributeName()))
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 处理每个变更详情
        for (ChangeLogDetail detail : attributeDetails) {
            // 获取变更日志
            Optional<ChangeLog> changeLogOpt = changeLogRepository.findById(detail.getChangeLog().getId());
            if (changeLogOpt.isPresent()) {
                ChangeLog changeLog = changeLogOpt.get();
                
                // 创建结果条目
                Map<String, Object> historyItem = new HashMap<>();
                historyItem.put("changeLogId", changeLog.getId());
                historyItem.put("detailId", detail.getId());
                historyItem.put("changeType", changeLog.getChangeType().name());
                historyItem.put("commitTime", changeLog.getCommitTime());
                historyItem.put("commitMessage", changeLog.getCommitMessage());
                
                // 获取用户信息
                if (changeLog.getUser() != null) {
                    User user = changeLog.getUser();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    historyItem.put("user", userInfo);
                }
                
                // 尝试将JSON字符串转换为对象
                try {
                    if (detail.getOldValue() != null) {
                        historyItem.put("oldValue", objectMapper.readValue(detail.getOldValue(), Object.class));
                    } else {
                        historyItem.put("oldValue", null);
                    }
                    
                    if (detail.getNewValue() != null) {
                        historyItem.put("newValue", objectMapper.readValue(detail.getNewValue(), Object.class));
                    } else {
                        historyItem.put("newValue", null);
                    }
                } catch (JsonProcessingException e) {
                    // 如果解析失败，则直接使用原始字符串
                    historyItem.put("oldValue", detail.getOldValue());
                    historyItem.put("newValue", detail.getNewValue());
                }
                
                result.add(historyItem);
            }
        }
        
        // 按时间排序，最新的变更排在前面
        result.sort((a, b) -> {
            return ((java.time.LocalDateTime)b.get("commitTime")).compareTo((java.time.LocalDateTime)a.get("commitTime"));
        });
        
        return result;
    }
    
    /**
     * 获取实体当前状态的变更历史构建
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 实体当前状态的构建过程
     */
    public Map<String, Object> getEntityCurrentState(EntityType entityType, Long entityId) {
        // 获取该实体的所有变更详情
        List<ChangeLogDetail> allDetails = changeLogService.findDetailsByEntityTypeAndEntityId(entityType, entityId);
        
        // 按属性名分组，并在每个分组内按时间排序
        Map<String, List<ChangeLogDetail>> detailsByAttribute = allDetails.stream()
                .collect(Collectors.groupingBy(ChangeLogDetail::getAttributeName));
        
        Map<String, Object> currentState = new LinkedHashMap<>();
        
        // 获取每个属性的最新值
        for (Map.Entry<String, List<ChangeLogDetail>> entry : detailsByAttribute.entrySet()) {
            String attributeName = entry.getKey();
            List<ChangeLogDetail> details = entry.getValue();
            
            // 按变更时间排序，获取最新的变更
            details.sort(Comparator.comparing(detail -> 
                    detail.getChangeLog().getCommitTime()));
            
            // 获取最新的变更详情
            ChangeLogDetail latestDetail = details.get(details.size() - 1);
            
            // 尝试将JSON字符串转换为对象
            try {
                if (latestDetail.getNewValue() != null) {
                    currentState.put(attributeName, objectMapper.readValue(latestDetail.getNewValue(), Object.class));
                } else {
                    currentState.put(attributeName, null);
                }
            } catch (JsonProcessingException e) {
                // 如果解析失败，则直接使用原始字符串
                currentState.put(attributeName, latestDetail.getNewValue());
            }
        }
        
        return currentState;
    }
    
    /**
     * 获取用户的变更统计信息
     * 
     * @param userId 用户ID
     * @return 用户变更统计信息
     */
    public Map<String, Object> getUserChangeStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 获取所有变更日志
        List<ChangeLog> allChangeLogs = changeLogRepository.findAll();
        
        // 筛选出该用户的变更
        List<ChangeLog> userChangeLogs = allChangeLogs.stream()
                .filter(log -> log.getUser() != null &&
                              log.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        
        // 统计总变更次数
        statistics.put("totalChanges", userChangeLogs.size());
        
        // 按变更类型统计
        Map<ChangeType, Long> changeTypeCount = userChangeLogs.stream()
                .collect(Collectors.groupingBy(ChangeLog::getChangeType, Collectors.counting()));
        statistics.put("changeTypeDistribution", changeTypeCount);
        
        // 获取最近的变更
        List<Map<String, Object>> recentChanges = userChangeLogs.stream()
                .sorted(Comparator.comparing(ChangeLog::getCommitTime).reversed())
                .limit(10)
                .map(log -> {
                    Map<String, Object> change = new HashMap<>();
                    change.put("id", log.getId());
                    change.put("changeType", log.getChangeType().name());
                    change.put("commitTime", log.getCommitTime());
                    change.put("commitMessage", log.getCommitMessage());
                    return change;
                })
                .collect(Collectors.toList());
        statistics.put("recentChanges", recentChanges);
        
        return statistics;
    }
} 