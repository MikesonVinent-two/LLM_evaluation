package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 异常处理配置类
 */
@Configuration
@ConfigurationProperties(prefix = "exception.handler")
public class ExceptionHandlerConfig {
    
    private boolean developmentMode = false;
    private boolean traceEnabled = false;
    private boolean includeStacktraceInResponse = false;
    private boolean detailedErrorMessages = false;
    
    /**
     * 判断是否为开发环境
     */
    public boolean isDevelopmentMode() {
        return developmentMode;
    }
    
    public void setDevelopmentMode(boolean developmentMode) {
        this.developmentMode = developmentMode;
    }
    
    /**
     * 是否启用跟踪ID
     */
    public boolean isTraceEnabled() {
        return traceEnabled;
    }
    
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }
    
    /**
     * 是否在响应中包含堆栈跟踪
     */
    public boolean isIncludeStacktraceInResponse() {
        return includeStacktraceInResponse;
    }
    
    public void setIncludeStacktraceInResponse(boolean includeStacktraceInResponse) {
        this.includeStacktraceInResponse = includeStacktraceInResponse;
    }
    
    /**
     * 是否包含详细的错误消息
     */
    public boolean isDetailedErrorMessages() {
        return detailedErrorMessages;
    }
    
    public void setDetailedErrorMessages(boolean detailedErrorMessages) {
        this.detailedErrorMessages = detailedErrorMessages;
    }
} 
