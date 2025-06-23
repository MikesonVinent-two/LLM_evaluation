package com.example.demo.dto;

public class ChangeDetailDTO {
    private String attributeName;    // 变更的字段名
    private String oldValue;        // 变更前的值
    private String newValue;        // 变更后的值
    private String changeType;      // 变更类型（新增、修改、删除）

    // Getters and Setters
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

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
} 