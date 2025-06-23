package com.example.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RawQuestionWithAnswersDTO {
    
    @NotNull(message = "问题信息不能为空")
    @Valid
    private RawQuestionDTO question;
    
    @Valid
    private List<RawAnswerCreateDTO> answers;
    
    // 构造函数
    public RawQuestionWithAnswersDTO() {
    }
    
    // Getters and Setters
    public RawQuestionDTO getQuestion() {
        return question;
    }
    
    public void setQuestion(RawQuestionDTO question) {
        this.question = question;
    }
    
    public List<RawAnswerCreateDTO> getAnswers() {
        return answers;
    }
    
    public void setAnswers(List<RawAnswerCreateDTO> answers) {
        this.answers = answers;
    }
} 