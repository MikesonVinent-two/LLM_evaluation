package com.example.demo.dto;

/**
 * 回答场景的提示词组装配置DTO
 */
public class AnswerPromptAssemblyConfigDTO extends PromptAssemblyConfigDTO {
    
    private String questionTypeSectionHeader;
    
    public AnswerPromptAssemblyConfigDTO() {
        super();
    }
    
    public String getQuestionTypeSectionHeader() {
        return questionTypeSectionHeader;
    }
    
    public void setQuestionTypeSectionHeader(String questionTypeSectionHeader) {
        this.questionTypeSectionHeader = questionTypeSectionHeader;
    }
} 