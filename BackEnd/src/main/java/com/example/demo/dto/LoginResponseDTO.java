package com.example.demo.dto;

import com.example.demo.entity.jdbc.UserRole;

public class LoginResponseDTO {
    private Long userId;
    private String username;
    private String contactInfo;
    private UserRole role;
    private String token;
    private Long expiresIn;  // token过期时间（秒）
    private Boolean success;
    private String message;
    
    // 构造函数
    public LoginResponseDTO() {
        this.success = false;
    }
    
    // 成功登录时使用的构造函数
    public LoginResponseDTO(Long userId, String username, String contactInfo, UserRole role, String token, Long expiresIn) {
        this.userId = userId;
        this.username = username;
        this.contactInfo = contactInfo;
        this.role = role;
        this.token = token;
        this.expiresIn = expiresIn;
        this.success = true;
        this.message = "登录成功";
    }
    
    // 登录失败时使用的构造函数
    public LoginResponseDTO(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
} 