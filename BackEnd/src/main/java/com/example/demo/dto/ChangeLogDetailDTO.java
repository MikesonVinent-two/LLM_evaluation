package com.example.demo.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.entity.jdbc.ChangeLogDetail;

/**
 * 变更日志详情DTO，用于API接口返回
 */
public class ChangeLogDetailDTO {
    private Long id;
    private Long changeLogId;
    private String entityType;
    private Long entityId;
    private String attributeName;
    private String oldValue;
    private String newValue;
    
    public ChangeLogDetailDTO() {
    }
    
    public ChangeLogDetailDTO(ChangeLogDetail detail) {
        this.id = detail.getId();
        
        if (detail.getChangeLog() != null) {
            this.changeLogId = detail.getChangeLog().getId();
        }
        
        this.entityType = detail.getEntityType() != null ? detail.getEntityType().name() : null;
        this.entityId = detail.getEntityId();
        this.attributeName = detail.getAttributeName();
        this.oldValue = detail.getOldValue();
        this.newValue = detail.getNewValue();
    }
    
    /**
     * 将ChangeLogDetail实体列表转换为DTO列表
     */
    public static List<ChangeLogDetailDTO> fromEntityList(List<ChangeLogDetail> details) {
        return details.stream()
                .map(ChangeLogDetailDTO::new)
                .collect(Collectors.toList());
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChangeLogId() {
        return changeLogId;
    }

    public void setChangeLogId(Long changeLogId) {
        this.changeLogId = changeLogId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
} 