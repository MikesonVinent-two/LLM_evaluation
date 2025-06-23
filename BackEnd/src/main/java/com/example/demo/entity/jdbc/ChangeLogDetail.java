package com.example.demo.entity.jdbc;

/**
 * 变更日志详情实体类 - JDBC版本
 * 对应数据库表: CHANGE_LOG_DETAILS
 */
public class ChangeLogDetail {
    // 表名常量
    public static final String TABLE_NAME = "CHANGE_LOG_DETAILS";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CHANGE_LOG_ID = "change_log_id";
    public static final String COLUMN_ENTITY_TYPE = "entity_type";
    public static final String COLUMN_ENTITY_ID = "entity_id";
    public static final String COLUMN_ATTRIBUTE_NAME = "attribute_name";
    public static final String COLUMN_OLD_VALUE = "old_value";
    public static final String COLUMN_NEW_VALUE = "new_value";
    
    private Long id;
    private ChangeLog changeLog;
    private EntityType entityType;
    private Long entityId;
    private String attributeName;
    private String oldValue;
    private String newValue;
    
    // Getter and Setter methods
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChangeLog getChangeLog() {
        return this.changeLog;
    }
    
    public void setChangeLog(ChangeLog changeLog) {
        this.changeLog = changeLog;
    }
    
    public EntityType getEntityType() {
        return this.entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return this.entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getAttributeName() {
        return this.attributeName;
    }
    
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
    
    public String getOldValue() {
        return this.oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return this.newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
} 