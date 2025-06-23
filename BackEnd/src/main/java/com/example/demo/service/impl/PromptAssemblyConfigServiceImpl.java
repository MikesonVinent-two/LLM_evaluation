package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AnswerPromptAssemblyConfigDTO;
import com.example.demo.dto.EvaluationPromptAssemblyConfigDTO;
import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.jdbc.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.repository.jdbc.AnswerPromptAssemblyConfigRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;
import com.example.demo.repository.jdbc.EvaluationPromptAssemblyConfigRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.PromptAssemblyConfigService;

/**
 * 提示词组装配置服务实现类
 */
@Service
public class PromptAssemblyConfigServiceImpl implements PromptAssemblyConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptAssemblyConfigServiceImpl.class);
    
    private final AnswerPromptAssemblyConfigRepository answerConfigRepository;
    private final EvaluationPromptAssemblyConfigRepository evalConfigRepository;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;
    
    @Autowired
    public PromptAssemblyConfigServiceImpl(
            AnswerPromptAssemblyConfigRepository answerConfigRepository,
            EvaluationPromptAssemblyConfigRepository evalConfigRepository,
            UserRepository userRepository,
            ChangeLogRepository changeLogRepository) {
        this.answerConfigRepository = answerConfigRepository;
        this.evalConfigRepository = evalConfigRepository;
        this.userRepository = userRepository;
        this.changeLogRepository = changeLogRepository;
    }
    
    @Override
    @Transactional
    public AnswerPromptAssemblyConfigDTO createAnswerConfig(AnswerPromptAssemblyConfigDTO configDTO, Long userId) {
        logger.info("创建回答提示词组装配置: {}", configDTO.getName());
        
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户(ID: " + userId + ")"));
        
        // 创建变更日志
        ChangeLog changeLog = new ChangeLog();
        changeLog.setChangeType(ChangeType.CREATE_PROMPT_ASSEMBLY_CONFIG);
        changeLog.setUser(user);
        changeLog.setCommitTime(LocalDateTime.now());
        changeLog.setCommitMessage("创建回答提示词组装配置: " + configDTO.getName());
        changeLogRepository.save(changeLog);
        
        // 创建配置实体
        AnswerPromptAssemblyConfig config = new AnswerPromptAssemblyConfig();
        config.setName(configDTO.getName());
        config.setDescription(configDTO.getDescription());
        config.setBaseSystemPrompt(configDTO.getBaseSystemPrompt());
        config.setTagPromptsSectionHeader(configDTO.getTagPromptsSectionHeader());
        config.setQuestionTypeSectionHeader(configDTO.getQuestionTypeSectionHeader());
        config.setTagPromptSeparator(configDTO.getTagPromptSeparator());
        config.setSectionSeparator(configDTO.getSectionSeparator());
        config.setFinalInstruction(configDTO.getFinalInstruction());
        config.setIsActive(true);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedByUser(user);
        config.setCreatedChangeLog(changeLog);
        
        // 保存配置
        AnswerPromptAssemblyConfig savedConfig = answerConfigRepository.save(config);
        logger.debug("回答提示词组装配置已创建: ID={}, 名称={}", savedConfig.getId(), savedConfig.getName());
        
        // 转换为DTO并返回
        return convertToAnswerDTO(savedConfig);
    }
    
    @Override
    @Transactional
    public EvaluationPromptAssemblyConfigDTO createEvaluationConfig(EvaluationPromptAssemblyConfigDTO configDTO, Long userId) {
        logger.info("创建评测提示词组装配置: {}", configDTO.getName());
        
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户(ID: " + userId + ")"));
        
        // 创建变更日志
        ChangeLog changeLog = new ChangeLog();
        changeLog.setChangeType(ChangeType.CREATE_PROMPT_ASSEMBLY_CONFIG);
        changeLog.setUser(user);
        changeLog.setCommitTime(LocalDateTime.now());
        changeLog.setCommitMessage("创建评测提示词组装配置: " + configDTO.getName());
        changeLogRepository.save(changeLog);
        
        // 创建配置实体
        EvaluationPromptAssemblyConfig config = new EvaluationPromptAssemblyConfig();
        config.setName(configDTO.getName());
        config.setDescription(configDTO.getDescription());
        config.setBaseSystemPrompt(configDTO.getBaseSystemPrompt());
        config.setTagPromptsSectionHeader(configDTO.getTagPromptsSectionHeader());
        config.setSubjectiveSectionHeader(configDTO.getSubjectiveSectionHeader());
        config.setTagPromptSeparator(configDTO.getTagPromptSeparator());
        config.setSectionSeparator(configDTO.getSectionSeparator());
        config.setFinalInstruction(configDTO.getFinalInstruction());
        config.setIsActive(true);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedByUser(user);
        config.setCreatedChangeLog(changeLog);
        
        // 保存配置
        EvaluationPromptAssemblyConfig savedConfig = evalConfigRepository.save(config);
        logger.debug("评测提示词组装配置已创建: ID={}, 名称={}", savedConfig.getId(), savedConfig.getName());
        
        // 转换为DTO并返回
        return convertToEvaluationDTO(savedConfig);
    }
    
    @Override
    @Transactional(readOnly = true)
    public AnswerPromptAssemblyConfigDTO getAnswerConfig(Long configId) {
        AnswerPromptAssemblyConfig config = answerConfigRepository.findById(configId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的回答提示词组装配置(ID: " + configId + ")"));
        
        return convertToAnswerDTO(config);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EvaluationPromptAssemblyConfigDTO getEvaluationConfig(Long configId) {
        EvaluationPromptAssemblyConfig config = evalConfigRepository.findById(configId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测提示词组装配置(ID: " + configId + ")"));
        
        return convertToEvaluationDTO(config);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AnswerPromptAssemblyConfigDTO> getAllActiveAnswerConfigs() {
        return answerConfigRepository.findByIsActiveTrue().stream()
                .map(this::convertToAnswerDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<AnswerPromptAssemblyConfigDTO> getAllActiveAnswerConfigsPageable(Pageable pageable) {
        Page<AnswerPromptAssemblyConfig> configPage = answerConfigRepository.findByIsActiveTruePageable(pageable);
        
        // 将实体转换为DTO
        List<AnswerPromptAssemblyConfigDTO> dtos = configPage.getContent().stream()
                .map(this::convertToAnswerDTO)
                .collect(Collectors.toList());
        
        // 创建新的Page对象
        return new PageImpl<>(dtos, pageable, configPage.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EvaluationPromptAssemblyConfigDTO> getAllActiveEvaluationConfigs() {
        return evalConfigRepository.findByIsActiveTrue().stream()
                .map(this::convertToEvaluationDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AnswerPromptAssemblyConfigDTO> getAnswerConfigsByUser(Long userId) {
        return answerConfigRepository.findByCreatedByUserId(userId).stream()
                .map(this::convertToAnswerDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EvaluationPromptAssemblyConfigDTO> getEvaluationConfigsByUser(Long userId) {
        return evalConfigRepository.findByCreatedByUserId(userId).stream()
                .map(this::convertToEvaluationDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将回答提示词组装配置实体转换为DTO
     */
    private AnswerPromptAssemblyConfigDTO convertToAnswerDTO(AnswerPromptAssemblyConfig config) {
        AnswerPromptAssemblyConfigDTO dto = new AnswerPromptAssemblyConfigDTO();
        dto.setId(config.getId());
        dto.setName(config.getName());
        dto.setDescription(config.getDescription());
        dto.setBaseSystemPrompt(config.getBaseSystemPrompt());
        dto.setTagPromptsSectionHeader(config.getTagPromptsSectionHeader());
        dto.setQuestionTypeSectionHeader(config.getQuestionTypeSectionHeader());
        dto.setTagPromptSeparator(config.getTagPromptSeparator());
        dto.setSectionSeparator(config.getSectionSeparator());
        dto.setFinalInstruction(config.getFinalInstruction());
        dto.setIsActive(config.getIsActive());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        
        if (config.getCreatedByUser() != null) {
            dto.setCreatedByUserId(config.getCreatedByUser().getId());
            dto.setCreatedByUsername(config.getCreatedByUser().getUsername());
        }
        
        return dto;
    }
    
    /**
     * 将评测提示词组装配置实体转换为DTO
     */
    private EvaluationPromptAssemblyConfigDTO convertToEvaluationDTO(EvaluationPromptAssemblyConfig config) {
        EvaluationPromptAssemblyConfigDTO dto = new EvaluationPromptAssemblyConfigDTO();
        dto.setId(config.getId());
        dto.setName(config.getName());
        dto.setDescription(config.getDescription());
        dto.setBaseSystemPrompt(config.getBaseSystemPrompt());
        dto.setTagPromptsSectionHeader(config.getTagPromptsSectionHeader());
        dto.setSubjectiveSectionHeader(config.getSubjectiveSectionHeader());
        dto.setTagPromptSeparator(config.getTagPromptSeparator());
        dto.setSectionSeparator(config.getSectionSeparator());
        dto.setFinalInstruction(config.getFinalInstruction());
        dto.setIsActive(config.getIsActive());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        
        if (config.getCreatedByUser() != null) {
            dto.setCreatedByUserId(config.getCreatedByUser().getId());
            dto.setCreatedByUsername(config.getCreatedByUser().getUsername());
        }
        
        return dto;
    }
} 