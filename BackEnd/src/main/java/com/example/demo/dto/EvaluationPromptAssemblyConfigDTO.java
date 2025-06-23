package com.example.demo.dto;

/**
 * 评测场景的提示词组装配置DTO
 */
public class EvaluationPromptAssemblyConfigDTO extends PromptAssemblyConfigDTO {
    
    private String subjectiveSectionHeader;
    
    public EvaluationPromptAssemblyConfigDTO() {
        super();
    }
    
    public String getSubjectiveSectionHeader() {
        return subjectiveSectionHeader;
    }
    
    public void setSubjectiveSectionHeader(String subjectiveSectionHeader) {
        this.subjectiveSectionHeader = subjectiveSectionHeader;
    }
} 