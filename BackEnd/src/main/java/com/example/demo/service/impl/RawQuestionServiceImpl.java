package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.RawAnswerCreateDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.repository.jdbc.RawAnswerRepository;
import com.example.demo.repository.jdbc.RawQuestionRepository;
import com.example.demo.service.RawQuestionService;
import com.example.demo.util.MetadataUtils;

@Service
public class RawQuestionServiceImpl implements RawQuestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RawQuestionServiceImpl.class);
    private final RawQuestionRepository rawQuestionRepository;
    private final RawAnswerRepository rawAnswerRepository;
    
    @Autowired
    public RawQuestionServiceImpl(RawQuestionRepository rawQuestionRepository,
                                RawAnswerRepository rawAnswerRepository) {
        this.rawQuestionRepository = rawQuestionRepository;
        this.rawAnswerRepository = rawAnswerRepository;
    }
    
    @Override
    public RawQuestion createRawQuestion(RawQuestionDTO rawQuestionDTO) {
        logger.debug("开始创建原始问题 - URL: {}", rawQuestionDTO.getSourceUrl());
        
        // 检查URL是否已存在
        if (rawQuestionRepository.existsBySourceUrl(rawQuestionDTO.getSourceUrl())) {
            logger.warn("创建原始问题失败 - URL已存在: {}", rawQuestionDTO.getSourceUrl());
            throw new RuntimeException("该来源URL已存在");
        }
        
        // 创建新的原始问题实体
        RawQuestion rawQuestion = new RawQuestion();
        rawQuestion.setSourceUrl(rawQuestionDTO.getSourceUrl());
        rawQuestion.setSourceSite(rawQuestionDTO.getSourceSite());
        rawQuestion.setTitle(rawQuestionDTO.getTitle());
        rawQuestion.setContent(rawQuestionDTO.getContent());
        rawQuestion.setOtherMetadata(MetadataUtils.normalizeMetadata(rawQuestionDTO.getOtherMetadata()));
        
        // 保存并返回
        try {
            RawQuestion savedQuestion = rawQuestionRepository.save(rawQuestion);
            logger.info("原始问题创建成功 - ID: {}, URL: {}", 
                savedQuestion.getId(), savedQuestion.getSourceUrl());
            return savedQuestion;
        } catch (Exception e) {
            logger.error("保存原始问题失败", e);
            throw new RuntimeException("保存原始问题失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Map<String, Object> createRawQuestionWithAnswers(RawQuestionWithAnswersDTO dto) {
        logger.debug("开始创建原始问题及其回答 - URL: {}", dto.getQuestion().getSourceUrl());
        
        // 创建新的原始问题实体
        RawQuestion rawQuestion = new RawQuestion();
        rawQuestion.setSourceUrl(dto.getQuestion().getSourceUrl());
        rawQuestion.setSourceSite(dto.getQuestion().getSourceSite());
        rawQuestion.setTitle(dto.getQuestion().getTitle());
        rawQuestion.setContent(dto.getQuestion().getContent());
        rawQuestion.setCrawlTime(dto.getQuestion().getCrawlTime());
        rawQuestion.setOtherMetadata(MetadataUtils.normalizeMetadata(dto.getQuestion().getOtherMetadata()));
        
        // 保存问题
        RawQuestion savedQuestion;
        try {
            savedQuestion = rawQuestionRepository.save(rawQuestion);
            logger.info("原始问题创建成功 - ID: {}, URL: {}", 
                savedQuestion.getId(), savedQuestion.getSourceUrl());
        } catch (Exception e) {
            logger.error("保存原始问题失败", e);
            throw new RuntimeException("保存原始问题失败: " + e.getMessage());
        }
        
        // 创建回答列表
        List<RawAnswer> savedAnswers = new ArrayList<>();
        if (dto.getAnswers() != null && !dto.getAnswers().isEmpty()) {
            for (RawAnswerCreateDTO answerDTO : dto.getAnswers()) {
                try {
                    // 创建回答实体
                    RawAnswer answer = new RawAnswer();
                    answer.setRawQuestion(savedQuestion);
                    answer.setAuthorInfo(answerDTO.getAuthorInfo());
                    answer.setContent(answerDTO.getContent());
                    answer.setPublishTime(answerDTO.getPublishTime());
                    answer.setUpvotes(answerDTO.getUpvotes());
                    answer.setIsAccepted(answerDTO.getIsAccepted());
                    answer.setOtherMetadata(MetadataUtils.normalizeMetadata(answerDTO.getOtherMetadata()));
                    
                    // 保存回答
                    RawAnswer savedAnswer = rawAnswerRepository.save(answer);
                    savedAnswers.add(savedAnswer);
                    
                    logger.debug("成功创建原始回答 - ID: {}, 问题ID: {}", 
                        savedAnswer.getId(), savedQuestion.getId());
                } catch (Exception e) {
                    logger.error("创建回答失败", e);
                    throw new RuntimeException("创建回答失败: " + e.getMessage());
                }
            }
        }
        
        // 准备返回数据
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("id", savedQuestion.getId());
        questionData.put("sourceUrl", savedQuestion.getSourceUrl());
        questionData.put("title", savedQuestion.getTitle());
        questionData.put("crawlTime", savedQuestion.getCrawlTime());
        
        List<Map<String, Object>> answersData = new ArrayList<>();
        for (RawAnswer answer : savedAnswers) {
            Map<String, Object> answerData = new HashMap<>();
            answerData.put("id", answer.getId());
            answerData.put("authorInfo", answer.getAuthorInfo());
            answerData.put("publishTime", answer.getPublishTime());
            answerData.put("isAccepted", answer.getIsAccepted());
            answersData.add(answerData);
        }
        
        result.put("question", questionData);
        result.put("answers", answersData);
        
        logger.info("成功创建原始问题及{}个回答 - 问题ID: {}", savedAnswers.size(), savedQuestion.getId());
        return result;
    }
} 