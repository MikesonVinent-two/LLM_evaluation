package com.example.demo.dto;

import com.example.demo.entity.jdbc.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {
    
    // 定义验证分组
    public interface RegisterValidation {}
    public interface LoginValidation {}
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空", groups = {RegisterValidation.class, LoginValidation.class})
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间", groups = {RegisterValidation.class})
    private String username;
    
    private String name;
    
    @NotBlank(message = "联系方式不能为空", groups = RegisterValidation.class)
    private String contactInfo;
    
    @NotBlank(message = "密码不能为空", groups = {RegisterValidation.class, LoginValidation.class})
    @Size(min = 6, message = "密码长度不能小于6个字符", groups = {RegisterValidation.class})
    private String password;
    
    private UserRole role = UserRole.CROWDSOURCE_USER;
    
    // 构造函数
    public UserDTO() {
    }
    
    public UserDTO(String username, String contactInfo, String password) {
        this.username = username;
        this.contactInfo = contactInfo;
        this.password = password;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
} 