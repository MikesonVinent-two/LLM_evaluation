package com.example.demo.exception;

public class EnumDeserializationException extends RuntimeException {
    
    private final String fieldName;
    private final Object invalidValue;
    private final Class<?> enumType;
    private final String[] allowedValues;
    
    public EnumDeserializationException(String fieldName, Object invalidValue, Class<?> enumType) {
        super(String.format("无效的枚举值 '%s' 用于字段 '%s'", invalidValue, fieldName));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.enumType = enumType;
        
        if (enumType.isEnum()) {
            Object[] enumConstants = enumType.getEnumConstants();
            this.allowedValues = new String[enumConstants.length];
            for (int i = 0; i < enumConstants.length; i++) {
                this.allowedValues[i] = enumConstants[i].toString();
            }
        } else {
            this.allowedValues = new String[0];
        }
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getInvalidValue() {
        return invalidValue;
    }
    
    public Class<?> getEnumType() {
        return enumType;
    }
    
    public String[] getAllowedValues() {
        return allowedValues;
    }
} 