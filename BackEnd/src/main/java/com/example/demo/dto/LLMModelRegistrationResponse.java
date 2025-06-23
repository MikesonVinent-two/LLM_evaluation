package com.example.demo.dto;

import java.util.List;

public class LLMModelRegistrationResponse {
    private boolean success;
    private String message;
    private List<LLMModelDTO> registeredModels;
    private String errorDetails;

    public LLMModelRegistrationResponse() {}

    public static LLMModelRegistrationResponse success(List<LLMModelDTO> models) {
        LLMModelRegistrationResponse response = new LLMModelRegistrationResponse();
        response.setSuccess(true);
        response.setMessage("模型注册成功");
        response.setRegisteredModels(models);
        return response;
    }
    
    public static LLMModelRegistrationResponse success(String message, List<LLMModelDTO> models) {
        LLMModelRegistrationResponse response = new LLMModelRegistrationResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setRegisteredModels(models);
        return response;
    }

    public static LLMModelRegistrationResponse error(String message, String errorDetails) {
        LLMModelRegistrationResponse response = new LLMModelRegistrationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorDetails(errorDetails);
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<LLMModelDTO> getRegisteredModels() {
        return registeredModels;
    }

    public void setRegisteredModels(List<LLMModelDTO> registeredModels) {
        this.registeredModels = registeredModels;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
} 