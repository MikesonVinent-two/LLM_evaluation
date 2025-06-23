package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.repository.jdbc.RawAnswerRepository;
import com.example.demo.repository.jdbc.RawQuestionRepository;
import com.example.demo.service.RawAnswerService;
import com.example.demo.util.MetadataUtils;

@Service
public class RawAnswerServiceImpl implements RawAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(RawAnswerServiceImpl.class);
    private final RawAnswerRepository rawAnswerRepository;
    private final RawQuestionRepository rawQuestionRepository;
    
    @Autowired
    public RawAnswerServiceImpl(RawAnswerRepository rawAnswerRepository, 
                              RawQuestionRepository rawQuestionRepository) {
        this.rawAnswerRepository = rawAnswerRepository;
        this.rawQuestionRepository = rawQuestionRepository;
    }
    
    @Override
    public RawAnswer createRawAnswer(RawAnswerDTO rawAnswerDTO) {
        logger.debug("开始创建原始回答 - 问题ID: {}", rawAnswerDTO.getRawQuestionId());
        
        // 查找对应的原始问题
        RawQuestion rawQuestion = rawQuestionRepository.findById(rawAnswerDTO.getRawQuestionId())
            .orElseThrow(() -> {
                logger.error("创建原始回答失败 - 问题不存在: {}", rawAnswerDTO.getRawQuestionId());
                return new RuntimeException("对应的原始问题不存在");
            });
        
        // 创建新的原始回答实体
        RawAnswer rawAnswer = new RawAnswer();
        rawAnswer.setRawQuestion(rawQuestion);
        rawAnswer.setAuthorInfo(rawAnswerDTO.getAuthorInfo());
        rawAnswer.setContent(rawAnswerDTO.getContent());
        rawAnswer.setPublishTime(rawAnswerDTO.getPublishTime());
        rawAnswer.setUpvotes(rawAnswerDTO.getUpvotes());
        rawAnswer.setIsAccepted(rawAnswerDTO.getIsAccepted());
        rawAnswer.setOtherMetadata(MetadataUtils.normalizeMetadata(rawAnswerDTO.getOtherMetadata()));
        
        // 保存并返回
        try {
            RawAnswer savedAnswer = rawAnswerRepository.save(rawAnswer);
            logger.info("原始回答创建成功 - ID: {}, 问题ID: {}", 
                savedAnswer.getId(), savedAnswer.getRawQuestion().getId());
            return savedAnswer;
        } catch (Exception e) {
            logger.error("保存原始回答失败", e);
            throw new RuntimeException("保存原始回答失败: " + e.getMessage());
        }
    }
} 