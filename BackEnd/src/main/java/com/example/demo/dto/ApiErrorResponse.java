package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    
    private String status;
    private String message;
    private String error;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String debugMessage;
    private List<ValidationError> validationErrors;
    
    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApiErrorResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public ApiErrorResponse(String status, String message, String error) {
        this();
        this.status = status;
        this.message = message;
        this.error = error;
    }
    
    public void addValidationError(String field, String message, Object rejectedValue) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(new ValidationError(field, message, rejectedValue));
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDebugMessage() {
        return debugMessage;
    }
    
    public void setDebugMessage(String debugMessage) {
        this.debugMessage = debugMessage;
    }
    
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    // 内部类，表示具体的验证错误
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
        
        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
    }
} 