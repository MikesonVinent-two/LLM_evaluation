package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 增强版API错误响应，提供更丰富的错误信息给前端
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnhancedApiErrorResponse {
    
    private String status;                // HTTP状态码
    private String message;               // 用户友好的错误消息
    private String error;                 // 错误类型
    private String code;                  // 业务错误代码
    private String path;                  // 请求路径
    private Object data;                  // 可能的相关数据
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;      // 错误发生时间
    
    private String debugMessage;          // 调试信息（仅开发环境显示）
    private String exceptionName;         // 异常类名
    private String exceptionMessage;      // 异常消息
    private Map<String, String> hints;    // 解决提示
    private List<ValidationError> validationErrors;  // 验证错误列表
    private String traceId;               // 追踪ID，便于日志关联
    private String stackTrace;            // 堆栈跟踪（仅开发环境显示）
    
    public EnhancedApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public EnhancedApiErrorResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public EnhancedApiErrorResponse(String status, String message, String error) {
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
    
    public void addHint(String key, String value) {
        if (hints == null) {
            hints = new HashMap<>();
        }
        hints.put(key, value);
    }
    
    // 常用提示添加方法
    public void addSolutionHint(String solution) {
        addHint("solution", solution);
    }
    
    public void addDocumentationLink(String link) {
        addHint("documentation", link);
    }
    
    // Getters and Setters
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
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
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
    
    public String getExceptionName() {
        return exceptionName;
    }
    
    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }
    
    public String getExceptionMessage() {
        return exceptionMessage;
    }
    
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
    
    public Map<String, String> getHints() {
        return hints;
    }
    
    public void setHints(Map<String, String> hints) {
        this.hints = hints;
    }
    
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    // 内部类，表示具体的验证错误
    public static class ValidationError {
        private String field;             // 出错的字段
        private String message;           // 错误消息
        private Object rejectedValue;     // 被拒绝的值
        private String code;              // 验证错误代码
        private Map<String, String> constraints;  // 约束条件
        
        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
        
        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public Map<String, String> getConstraints() {
            return constraints;
        }
        
        public void setConstraints(Map<String, String> constraints) {
            this.constraints = constraints;
        }
        
        public void addConstraint(String name, String value) {
            if (constraints == null) {
                constraints = new HashMap<>();
            }
            constraints.put(name, value);
        }
    }
} 