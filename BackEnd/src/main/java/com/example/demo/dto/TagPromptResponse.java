package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.jdbc.AnswerTagPrompt;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;

/**
 * 标签提示词响应DTO
 */
public class TagPromptResponse {
    
    private Long id;
    private TagDTO tag;
    private String name;
    private String promptTemplate;
    private String description;
    private Boolean isActive;
    private Integer promptPriority;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserDTO createdByUser;
    
    // 默认构造函数
    public TagPromptResponse() {
    }
    
    // 从实体转换为DTO的构造函数
    public TagPromptResponse(AnswerTagPrompt prompt) {
        this.id = prompt.getId();
        if (prompt.getTag() != null) {
            this.tag = new TagDTO(prompt.getTag());
        }
        this.name = prompt.getName();
        this.promptTemplate = prompt.getPromptTemplate();
        this.description = prompt.getDescription();
        this.isActive = prompt.getIsActive();
        this.promptPriority = prompt.getPromptPriority();
        this.version = prompt.getVersion();
        this.createdAt = prompt.getCreatedAt();
        this.updatedAt = prompt.getUpdatedAt();
        if (prompt.getCreatedByUser() != null) {
            this.createdByUser = new UserDTO(prompt.getCreatedByUser());
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TagDTO getTag() {
        return tag;
    }

    public void setTag(TagDTO tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPromptPriority() {
        return promptPriority;
    }

    public void setPromptPriority(Integer promptPriority) {
        this.promptPriority = promptPriority;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserDTO getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(UserDTO createdByUser) {
        this.createdByUser = createdByUser;
    }
    
    /**
     * 标签DTO内部类
     */
    public static class TagDTO {
        private Long id;
        private String tagName;
        private String tagType;
        private String description;
        private LocalDateTime createdAt;
        private UserDTO createdByUser;
        
        public TagDTO() {
        }
        
        public TagDTO(Tag tag) {
            this.id = tag.getId();
            this.tagName = tag.getTagName();
            this.tagType = tag.getTagType();
            this.description = tag.getDescription();
            this.createdAt = tag.getCreatedAt();
            if (tag.getCreatedByUser() != null) {
                this.createdByUser = new UserDTO(tag.getCreatedByUser());
            }
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTagName() {
            return tagName;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }

        public String getTagType() {
            return tagType;
        }

        public void setTagType(String tagType) {
            this.tagType = tagType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public UserDTO getCreatedByUser() {
            return createdByUser;
        }

        public void setCreatedByUser(UserDTO createdByUser) {
            this.createdByUser = createdByUser;
        }
    }
    
    /**
     * 用户DTO内部类
     */
    public static class UserDTO {
        private Long id;
        private String username;
        private String password;
        private String name;
        private String contactInfo;
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public UserDTO() {
        }
        
        public UserDTO(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.password = user.getPassword();
            this.name = user.getName();
            this.contactInfo = user.getContactInfo();
            this.role = user.getRole().toString();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
        }

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

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
} 