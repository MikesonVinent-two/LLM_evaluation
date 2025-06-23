package com.example.demo.exception;

public class StandardQuestionException extends RuntimeException {
    
    public StandardQuestionException(String message) {
        super(message);
    }

    public StandardQuestionException(String message, Throwable cause) {
        super(message, cause);
    }
} 