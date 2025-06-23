package com.example.demo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class ChangeLogUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeLogUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建变更日志详情
     * @param changeLog 变更日志主记录
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param attributeName 属性名
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 变更日志详情对象
     */
    public static ChangeLogDetail createDetail(
            ChangeLog changeLog, 
            EntityType entityType,
            Long entityId,
            String attributeName,
            Object oldValue,
            Object newValue) {
        
        ChangeLogDetail detail = new ChangeLogDetail();
        detail.setChangeLog(changeLog);
        detail.setEntityType(entityType);
        detail.setEntityId(entityId);
        detail.setAttributeName(attributeName);
        
        try {
            // 如果值不为null，则转换为JSON字符串
            detail.setOldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null);
            detail.setNewValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null);
        } catch (JsonProcessingException e) {
            logger.error("JSON转换失败", e);
            // 如果JSON转换失败，则存储toString()结果
            detail.setOldValue(oldValue != null ? oldValue.toString() : null);
            detail.setNewValue(newValue != null ? newValue.toString() : null);
        }
        
        return detail;
    }
    
    /**
     * 比较两个对象的属性值并创建变更日志详情列表
     * @param changeLog 变更日志主记录
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param oldEntity 旧对象
     * @param newEntity 新对象
     * @param propertyNames 需要比较的属性名列表
     * @return 变更日志详情列表
     */
    public static List<ChangeLogDetail> compareAndCreateDetails(
            ChangeLog changeLog,
            EntityType entityType,
            Long entityId,
            Object oldEntity,
            Object newEntity,
            String... propertyNames) {
        
        List<ChangeLogDetail> details = new ArrayList<>();
        BeanWrapper oldWrapper = new BeanWrapperImpl(oldEntity);
        BeanWrapper newWrapper = new BeanWrapperImpl(newEntity);

        for (String propertyName : propertyNames) {
            Object oldValue = oldWrapper.getPropertyValue(propertyName);
            Object newValue = newWrapper.getPropertyValue(propertyName);

            if (!isEqual(oldValue, newValue)) {
                ChangeLogDetail detail = new ChangeLogDetail();
                detail.setChangeLog(changeLog);
                detail.setEntityType(entityType);
                detail.setEntityId(entityId);
                detail.setAttributeName(propertyName);
                try {
                    detail.setOldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null);
                    detail.setNewValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null);
                } catch (Exception e) {
                    throw new RuntimeException("Error converting value to JSON", e);
                }
                details.add(detail);
            }
        }

        return details;
    }
    
    /**
     * 获取对象的属性值
     * @param obj 对象
     * @param propertyName 属性名
     * @return 属性值
     */
    private static Object getPropertyValue(Object obj, String propertyName) {
        try {
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            return obj.getClass().getMethod(getterName).invoke(obj);
        } catch (Exception e) {
            logger.error("获取属性值失败: {}", propertyName, e);
            return null;
        }
    }

    public static void createAndSaveNewEntityDetails(
            ChangeLogDetailRepository repository,
            ChangeLog changeLog,
            EntityType entityType,
            Long entityId,
            Object entity,
            String... propertyNames) {
        
        BeanWrapper wrapper = new BeanWrapperImpl(entity);

        for (String propertyName : propertyNames) {
            Object value = wrapper.getPropertyValue(propertyName);
            if (value != null) {
                ChangeLogDetail detail = new ChangeLogDetail();
                detail.setChangeLog(changeLog);
                detail.setEntityType(entityType);
                detail.setEntityId(entityId);
                detail.setAttributeName(propertyName);
                try {
                    detail.setNewValue(objectMapper.writeValueAsString(value));
                } catch (Exception e) {
                    throw new RuntimeException("Error converting value to JSON", e);
                }
                repository.save(detail);
            }
        }
    }

    private static boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == obj2) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }
} 