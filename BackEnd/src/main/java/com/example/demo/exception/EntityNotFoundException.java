package com.example.demo.exception;

/**
 * 自定义实体未找到异常
 * 用于替代JPA的EntityNotFoundException
 */
public class EntityNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 