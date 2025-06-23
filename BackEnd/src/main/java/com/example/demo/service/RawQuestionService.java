package com.example.demo.service;

import java.util.Map;

import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawQuestion;

public interface RawQuestionService {
    RawQuestion createRawQuestion(RawQuestionDTO rawQuestionDTO);
    Map<String, Object> createRawQuestionWithAnswers(RawQuestionWithAnswersDTO dto);
} 