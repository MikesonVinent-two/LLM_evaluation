package com.example.demo.util;

import com.example.demo.dto.StandardAnswerDTO;
import com.example.demo.entity.jdbc.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.List;

public class StandardAnswerValidator {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 验证标准答案数据的完整性
     * @param answerDTO 标准答案DTO
     * @throws IllegalArgumentException 如果数据验证失败
     */
    public static void validateStandardAnswer(StandardAnswerDTO answerDTO) {
        if (answerDTO == null) {
            throw new IllegalArgumentException("标准答案数据不能为空");
        }
        
        // 验证基本字段
        if (answerDTO.getStandardQuestionId() == null) {
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        if (answerDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (answerDTO.getQuestionType() == null) {
            throw new IllegalArgumentException("问题类型不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getAnswerText())) {
            throw new IllegalArgumentException("答案文本不能为空");
        }
        
        // 根据问题类型验证特定字段
        switch (answerDTO.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
                validateObjectiveAnswer(answerDTO);
                break;
            case SIMPLE_FACT:
                validateSimpleFactAnswer(answerDTO);
                break;
            case SUBJECTIVE:
                validateSubjectiveAnswer(answerDTO);
                break;
            default:
                throw new IllegalArgumentException("不支持的问题类型: " + answerDTO.getQuestionType());
        }
    }
    
    /**
     * 验证客观题（单选题/多选题）答案数据
     */
    private static void validateObjectiveAnswer(StandardAnswerDTO answerDTO) {
        if (!StringUtils.hasText(answerDTO.getOptions())) {
            throw new IllegalArgumentException("客观题选项不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getCorrectIds())) {
            throw new IllegalArgumentException("客观题正确答案不能为空");
        }
        
        try {
            // 验证选项格式
            List<?> options = objectMapper.readValue(answerDTO.getOptions(), List.class);
            if (options.isEmpty()) {
                throw new IllegalArgumentException("客观题选项列表不能为空");
            }
            
            // 验证正确答案格式
            List<?> correctIds = objectMapper.readValue(answerDTO.getCorrectIds(), List.class);
            if (correctIds.isEmpty()) {
                throw new IllegalArgumentException("客观题正确答案列表不能为空");
            }
            
            // 对于单选题，确保只有一个正确答案
            if (answerDTO.getQuestionType() == QuestionType.SINGLE_CHOICE && correctIds.size() != 1) {
                throw new IllegalArgumentException("单选题只能有一个正确答案");
            }
            
            // 验证正确答案ID是否在选项范围内
            int optionsSize = options.size();
            for (Object correctId : correctIds) {
                int id = Integer.parseInt(correctId.toString());
                if (id < 0 || id >= optionsSize) {
                    throw new IllegalArgumentException("正确答案ID超出选项范围");
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("选项或正确答案格式不正确: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("正确答案ID必须为数字");
        }
    }
    
    /**
     * 验证简单事实题答案数据
     */
    private static void validateSimpleFactAnswer(StandardAnswerDTO answerDTO) {
        if (StringUtils.hasText(answerDTO.getAlternativeAnswers())) {
            try {
                List<?> alternativeAnswers = objectMapper.readValue(answerDTO.getAlternativeAnswers(), List.class);
                if (alternativeAnswers.isEmpty()) {
                    throw new IllegalArgumentException("如果提供了可选答案，列表不能为空");
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("可选答案格式不正确: " + e.getMessage());
            }
        }
    }
    
    /**
     * 验证主观题答案数据
     */
    private static void validateSubjectiveAnswer(StandardAnswerDTO answerDTO) {
        if (!StringUtils.hasText(answerDTO.getScoringGuidance())) {
            throw new IllegalArgumentException("主观题必须提供评分指导");
        }
    }
} 