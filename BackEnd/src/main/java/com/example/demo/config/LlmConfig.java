package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    
    private String defaultApiUrl;
    private String defaultModel;
    private int connectionTimeout;
    private int readTimeout;
    private RetryConfig retry;
    
    public static class RetryConfig {
        private int maxAttempts;
        private int backoffDelay;
        
        public int getMaxAttempts() {
            return maxAttempts;
        }
        
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
        
        public int getBackoffDelay() {
            return backoffDelay;
        }
        
        public void setBackoffDelay(int backoffDelay) {
            this.backoffDelay = backoffDelay;
        }
    }
    
    public String getDefaultApiUrl() {
        return defaultApiUrl;
    }
    
    public void setDefaultApiUrl(String defaultApiUrl) {
        this.defaultApiUrl = defaultApiUrl;
    }
    
    public String getDefaultModel() {
        return defaultModel;
    }
    
    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public RetryConfig getRetry() {
        return retry;
    }
    
    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }
} 
