package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.StandardAnswerDTO;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.StandardObjectiveAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardSimpleAnswer;
import com.example.demo.entity.jdbc.StandardSubjectiveAnswer;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;
import com.example.demo.repository.jdbc.StandardObjectiveAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.StandardSimpleAnswerRepository;
import com.example.demo.repository.jdbc.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.StandardAnswerService;
import com.example.demo.util.ChangeLogUtils;

@Service
public class StandardAnswerServiceImpl implements StandardAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardAnswerServiceImpl.class);
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private StandardObjectiveAnswerRepository objectiveAnswerRepository;
    
    @Autowired
    private StandardSimpleAnswerRepository simpleAnswerRepository;
    
    @Autowired
    private StandardSubjectiveAnswerRepository subjectiveAnswerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChangeLogRepository changeLogRepository;
    
    @Autowired
    private ChangeLogDetailRepository changeLogDetailRepository;
    
    @Override
    @Transactional
    public Object createOrUpdateStandardAnswer(StandardAnswerDTO answerDTO, Long userId) {
        logger.debug("开始创建/更新标准答案 - 标准问题ID: {}, 用户ID: {}", answerDTO.getStandardQuestionId(), userId);
        
        // 验证基本参数
        if (answerDTO.getStandardQuestionId() == null || userId == null || answerDTO.getQuestionType() == null) {
            logger.error("创建/更新标准答案失败 - 标准问题ID、用户ID或问题类型为空");
            throw new IllegalArgumentException("标准问题ID、用户ID和问题类型不能为空");
        }
        
        // 验证答案文本
        if (answerDTO.getAnswerText() == null || answerDTO.getAnswerText().trim().isEmpty()) {
            logger.error("创建/更新标准答案失败 - 答案文本为空");
            throw new IllegalArgumentException("答案文本不能为空");
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("创建/更新标准答案失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(answerDTO.getStandardQuestionId())
                .orElseThrow(() -> {
                    logger.error("创建/更新标准答案失败 - 找不到标准问题ID: {}", answerDTO.getStandardQuestionId());
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + answerDTO.getStandardQuestionId() + "）");
                });
            
            // 验证问题类型是否匹配
            if (standardQuestion.getQuestionType() != answerDTO.getQuestionType()) {
                logger.error("创建/更新标准答案失败 - 问题类型不匹配，期望: {}, 实际: {}", 
                    standardQuestion.getQuestionType(), answerDTO.getQuestionType());
                throw new IllegalArgumentException("问题类型不匹配");
            }
            
            // 根据问题类型验证特定字段
            validateAnswerFields(answerDTO);
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage(answerDTO.getCommitMessage());
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            // 根据问题类型创建或更新相应的答案
            Object result = null;
            switch (answerDTO.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    result = createOrUpdateObjectiveAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                case SIMPLE_FACT:
                    result = createOrUpdateSimpleAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                case SUBJECTIVE:
                    result = createOrUpdateSubjectiveAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                default:
                    logger.error("创建/更新标准答案失败 - 不支持的问题类型: {}", answerDTO.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + answerDTO.getQuestionType());
            }
            
            logger.info("成功创建/更新标准答案 - 标准问题ID: {}, 用户ID: {}", answerDTO.getStandardQuestionId(), userId);
            return result;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建/更新标准答案时发生未预期的错误", e);
            throw new RuntimeException("创建/更新标准答案时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Object updateStandardAnswer(Long standardQuestionId, StandardAnswerDTO answerDTO, Long userId) {
        logger.debug("开始更新标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
        
        // 验证基本参数
        if (standardQuestionId == null || userId == null) {
            logger.error("更新标准答案失败 - 标准问题ID或用户ID为空");
            throw new IllegalArgumentException("标准问题ID和用户ID不能为空");
        }
        
        // 验证答案文本
        if (answerDTO.getAnswerText() == null || answerDTO.getAnswerText().trim().isEmpty()) {
            logger.error("更新标准答案失败 - 答案文本为空");
            throw new IllegalArgumentException("答案文本不能为空");
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("更新标准答案失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(standardQuestionId)
                .orElseThrow(() -> {
                    logger.error("更新标准答案失败 - 找不到标准问题ID: {}", standardQuestionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + standardQuestionId + "）");
                });
            
            // 确保问题类型已设置
            if (answerDTO.getQuestionType() == null) {
                answerDTO.setQuestionType(standardQuestion.getQuestionType());
            }
            
            // 验证问题类型是否匹配
            if (standardQuestion.getQuestionType() != answerDTO.getQuestionType()) {
                logger.error("更新标准答案失败 - 问题类型不匹配，期望: {}, 实际: {}", 
                    standardQuestion.getQuestionType(), answerDTO.getQuestionType());
                throw new IllegalArgumentException("问题类型不匹配");
            }
            
            // 根据问题类型验证特定字段
            validateAnswerFields(answerDTO);
            
            // 确保标准问题ID已设置
            answerDTO.setStandardQuestionId(standardQuestionId);
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage(answerDTO.getCommitMessage() != null ? 
                answerDTO.getCommitMessage() : "更新标准答案");
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            // 检查是否存在答案
            Object existingAnswer = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    existingAnswer = objectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    break;
                case SIMPLE_FACT:
                    existingAnswer = simpleAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    break;
                case SUBJECTIVE:
                    existingAnswer = subjectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    break;
                default:
                    logger.error("更新标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            // 如果不存在答案，则抛出异常
            if (existingAnswer == null) {
                logger.error("更新标准答案失败 - 标准问题ID: {} 没有关联的标准答案", standardQuestionId);
                throw new IllegalStateException("该标准问题没有关联的标准答案，请先创建");
            }
            
            // 更新相应的答案
            Object result = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    result = updateObjectiveAnswer(standardQuestion, answerDTO, user, changeLog, 
                        (StandardObjectiveAnswer) existingAnswer);
                    break;
                case SIMPLE_FACT:
                    result = updateSimpleAnswer(standardQuestion, answerDTO, user, changeLog, 
                        (StandardSimpleAnswer) existingAnswer);
                    break;
                case SUBJECTIVE:
                    result = updateSubjectiveAnswer(standardQuestion, answerDTO, user, changeLog, 
                        (StandardSubjectiveAnswer) existingAnswer);
                    break;
                default:
                    logger.error("更新标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            logger.info("成功更新标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
            return result;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新标准答案时发生未预期的错误", e);
            throw new RuntimeException("更新标准答案时发生错误: " + e.getMessage());
        }
    }
    
    private void validateAnswerFields(StandardAnswerDTO answerDTO) {
        switch (answerDTO.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
                if (answerDTO.getOptions() == null || answerDTO.getOptions().trim().isEmpty()) {
                    throw new IllegalArgumentException("客观题选项不能为空");
                }
                if (answerDTO.getCorrectIds() == null || answerDTO.getCorrectIds().trim().isEmpty()) {
                    throw new IllegalArgumentException("客观题正确答案不能为空");
                }
                break;
            case SIMPLE_FACT:
                // alternativeAnswers是可选的，不需要验证
                break;
            case SUBJECTIVE:
                if (answerDTO.getScoringGuidance() == null || answerDTO.getScoringGuidance().trim().isEmpty()) {
                    throw new IllegalArgumentException("主观题评分指导不能为空");
                }
                break;
        }
    }
    
    private StandardObjectiveAnswer createOrUpdateObjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardObjectiveAnswer existingAnswer = objectiveAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
            .orElse(null);
        
        StandardObjectiveAnswer answer = existingAnswer != null ? existingAnswer : new StandardObjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setOptions(answerDTO.getOptions());
        answer.setCorrectOptionIds(answerDTO.getCorrectIds());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = objectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_OBJECTIVE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "options", "correctIds"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_OBJECTIVE_ANSWER,
                answer.getId(),
                answer,
                "options", "correctIds"
            );
        }
        
        return answer;
    }
    
    private StandardSimpleAnswer createOrUpdateSimpleAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardSimpleAnswer existingAnswer = simpleAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
            .orElse(null);
        
        StandardSimpleAnswer answer = existingAnswer != null ? existingAnswer : new StandardSimpleAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setAlternativeAnswers(answerDTO.getAlternativeAnswers());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = simpleAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_SIMPLE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "answerText", "alternativeAnswers"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_SIMPLE_ANSWER,
                answer.getId(),
                answer,
                "answerText", "alternativeAnswers"
            );
        }
        
        return answer;
    }
    
    private StandardSubjectiveAnswer createOrUpdateSubjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardSubjectiveAnswer existingAnswer = subjectiveAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
        
        StandardSubjectiveAnswer answer = existingAnswer != null ? existingAnswer : new StandardSubjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setScoringGuidance(answerDTO.getScoringGuidance());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = subjectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_SUBJECTIVE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "answerText", "scoringGuidance"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_SUBJECTIVE_ANSWER,
                answer.getId(),
                answer,
                "answerText", "scoringGuidance"
            );
        }
        
        return answer;
    }
    
    // 更新客观题答案
    private StandardObjectiveAnswer updateObjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog,
            StandardObjectiveAnswer existingAnswer) {
        
        // 更新答案字段
        existingAnswer.setOptions(answerDTO.getOptions());
        existingAnswer.setCorrectOptionIds(answerDTO.getCorrectIds());
        existingAnswer.setDeterminedByUser(user);
        existingAnswer.setDeterminedTime(LocalDateTime.now());
        existingAnswer.setCreatedChangeLog(changeLog);
        
        // 保存更新后的答案
        existingAnswer = objectiveAnswerRepository.save(existingAnswer);
        
        // 记录变更详情
        List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
            changeLog,
            EntityType.STANDARD_OBJECTIVE_ANSWER,
            existingAnswer.getId(),
            existingAnswer,
            existingAnswer,
            "options", "correctIds"
        );
        
        for (ChangeLogDetail detail : details) {
            changeLogDetailRepository.save(detail);
        }
        
        return existingAnswer;
    }
    
    // 更新简单题答案
    private StandardSimpleAnswer updateSimpleAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog,
            StandardSimpleAnswer existingAnswer) {
        
        // 更新答案字段
        existingAnswer.setAnswerText(answerDTO.getAnswerText());
        existingAnswer.setAlternativeAnswers(answerDTO.getAlternativeAnswers());
        existingAnswer.setDeterminedByUser(user);
        existingAnswer.setDeterminedTime(LocalDateTime.now());
        existingAnswer.setCreatedChangeLog(changeLog);
        
        // 保存更新后的答案
        existingAnswer = simpleAnswerRepository.save(existingAnswer);
        
        // 记录变更详情
        List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
            changeLog,
            EntityType.STANDARD_SIMPLE_ANSWER,
            existingAnswer.getId(),
            existingAnswer,
            existingAnswer,
            "answerText", "alternativeAnswers"
        );
        
        for (ChangeLogDetail detail : details) {
            changeLogDetailRepository.save(detail);
        }
        
        return existingAnswer;
    }
    
    // 更新主观题答案
    private StandardSubjectiveAnswer updateSubjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog,
            StandardSubjectiveAnswer existingAnswer) {
        
        // 更新答案字段
        existingAnswer.setAnswerText(answerDTO.getAnswerText());
        existingAnswer.setScoringGuidance(answerDTO.getScoringGuidance());
        existingAnswer.setDeterminedByUser(user);
        existingAnswer.setDeterminedTime(LocalDateTime.now());
        existingAnswer.setCreatedChangeLog(changeLog);
        
        // 保存更新后的答案
        existingAnswer = subjectiveAnswerRepository.save(existingAnswer);
        
        // 记录变更详情
        List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
            changeLog,
            EntityType.STANDARD_SUBJECTIVE_ANSWER,
            existingAnswer.getId(),
            existingAnswer,
            existingAnswer,
            "answerText", "scoringGuidance"
        );
        
        for (ChangeLogDetail detail : details) {
            changeLogDetailRepository.save(detail);
        }
        
        return existingAnswer;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Object getStandardAnswer(Long standardQuestionId) {
        logger.debug("开始获取标准答案 - 标准问题ID: {}", standardQuestionId);
        
        if (standardQuestionId == null) {
            logger.error("获取标准答案失败 - 标准问题ID为空");
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        
        try {
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(standardQuestionId)
                .orElseThrow(() -> {
                    logger.error("获取标准答案失败 - 找不到标准问题ID: {}", standardQuestionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + standardQuestionId + "）");
                });
            
            // 根据问题类型获取相应的答案
            Object answer = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    answer = objectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    break;
                case SIMPLE_FACT:
                    answer = simpleAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    break;
                case SUBJECTIVE:
                    answer = subjectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    break;
                default:
                    logger.error("获取标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            logger.info("成功获取标准答案 - 标准问题ID: {}", standardQuestionId);
            return answer;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取标准答案时发生未预期的错误", e);
            throw new RuntimeException("获取标准答案时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void deleteStandardAnswer(Long standardQuestionId, Long userId) {
        logger.debug("开始删除标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
        
        if (standardQuestionId == null || userId == null) {
            logger.error("删除标准答案失败 - 标准问题ID或用户ID为空");
            throw new IllegalArgumentException("标准问题ID和用户ID不能为空");
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("删除标准答案失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(standardQuestionId)
                .orElseThrow(() -> {
                    logger.error("删除标准答案失败 - 找不到标准问题ID: {}", standardQuestionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + standardQuestionId + "）");
                });
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.DELETE_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage("删除标准答案");
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            LocalDateTime now = LocalDateTime.now();
            
            // 根据问题类型删除相应的答案
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    StandardObjectiveAnswer objectiveAnswer = objectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    if (objectiveAnswer != null) {
                        objectiveAnswer.setDeletedAt(now);
                        objectiveAnswerRepository.save(objectiveAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_OBJECTIVE_ANSWER, objectiveAnswer.getId());
                    }
                    break;
                case SIMPLE_FACT:
                    StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId)
                        .orElse(null);
                    if (simpleAnswer != null) {
                        simpleAnswer.setDeletedAt(now);
                        simpleAnswerRepository.save(simpleAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_SIMPLE_ANSWER, simpleAnswer.getId());
                    }
                    break;
                case SUBJECTIVE:
                    StandardSubjectiveAnswer subjectiveAnswer = subjectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    if (subjectiveAnswer != null) {
                        subjectiveAnswer.setDeletedAt(now);
                        subjectiveAnswerRepository.save(subjectiveAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_SUBJECTIVE_ANSWER, subjectiveAnswer.getId());
                    }
                    break;
                default:
                    logger.error("删除标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            logger.info("成功删除标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("删除标准答案时发生未预期的错误", e);
            throw new RuntimeException("删除标准答案时发生错误: " + e.getMessage());
        }
    }
    
    private void recordDeletionDetails(ChangeLog changeLog, EntityType entityType, Long entityId) {
        ChangeLogDetail detail = new ChangeLogDetail();
        detail.setChangeLog(changeLog);
        detail.setEntityType(entityType);
        detail.setEntityId(entityId);
        detail.setAttributeName("deleted_at");
        detail.setOldValue(null);
        detail.setNewValue(LocalDateTime.now().toString());
        changeLogDetailRepository.save(detail);
    }

    @Override
    public List<Map<String, Object>> getAnswerHistory(Long answerId) {
        logger.debug("开始获取标准答案历史记录 - 答案ID: {}", answerId);
        
        if (answerId == null) {
            logger.error("获取标准答案历史记录失败 - 答案ID为空");
            throw new IllegalArgumentException("答案ID不能为空");
        }

        try {
            // 从变更日志表中获取该答案的所有历史记录
            List<Map<String, Object>> history = changeLogRepository.findByAnswerId(answerId).stream()
                .map(changeLog -> {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("id", changeLog.getId());
                    historyItem.put("commitMessage", changeLog.getCommitMessage());
                    historyItem.put("commitTime", changeLog.getCommitTime());
                    historyItem.put("userId", changeLog.getUser().getId());
                    historyItem.put("userName", changeLog.getUser().getUsername());
                    
                    // 获取变更详情
                    List<Map<String, Object>> details = changeLogDetailRepository
                        .findByChangeLogId(changeLog.getId())
                        .stream()
                        .map(detail -> {
                            Map<String, Object> detailMap = new HashMap<>();
                            detailMap.put("field", detail.getAttributeName());
                            detailMap.put("oldValue", detail.getOldValue());
                            detailMap.put("newValue", detail.getNewValue());
                            return detailMap;
                        })
                        .collect(Collectors.toList());
                    
                    historyItem.put("details", details);
                    return historyItem;
                })
                .collect(Collectors.toList());

            logger.info("成功获取标准答案历史记录 - 答案ID: {}, 记录数: {}", answerId, history.size());
            return history;
        } catch (Exception e) {
            logger.error("获取标准答案历史记录时发生错误", e);
            throw new RuntimeException("获取历史记录时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAnswerVersionTree(Long answerId) {
        logger.debug("开始获取标准答案版本树 - 答案ID: {}", answerId);
        
        if (answerId == null) {
            logger.error("获取标准答案版本树失败 - 答案ID为空");
            throw new IllegalArgumentException("答案ID不能为空");
        }

        try {
            // 构建版本树
            Map<String, Object> versionTree = new HashMap<>();
            
            // 获取根版本
            var rootVersion = changeLogRepository.findRootVersionByAnswerId(answerId);
            if (rootVersion == null) {
                logger.warn("未找到标准答案的根版本 - 答案ID: {}", answerId);
                return versionTree;
            }

            // 递归构建版本树
            versionTree = buildVersionTreeNode(rootVersion);

            logger.info("成功获取标准答案版本树 - 答案ID: {}", answerId);
            return versionTree;
        } catch (Exception e) {
            logger.error("获取标准答案版本树时发生错误", e);
            throw new RuntimeException("获取版本树时发生错误: " + e.getMessage());
        }
    }

    private Map<String, Object> buildVersionTreeNode(ChangeLog changeLog) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", changeLog.getId());
        node.put("commitMessage", changeLog.getCommitMessage());
        node.put("commitTime", changeLog.getCommitTime());
        node.put("userId", changeLog.getUser().getId());
        node.put("userName", changeLog.getUser().getUsername());

        // 获取子版本
        List<Map<String, Object>> children = changeLogRepository.findChildVersions(changeLog.getId())
            .stream()
            .map(this::buildVersionTreeNode)
            .collect(Collectors.toList());
        
        if (!children.isEmpty()) {
            node.put("children", children);
        }

        return node;
    }

    @Override
    public Map<String, Object> compareAnswerVersions(Long baseVersionId, Long compareVersionId) {
        logger.debug("开始比较标准答案版本 - 基准版本ID: {}, 比较版本ID: {}", baseVersionId, compareVersionId);
        
        if (baseVersionId == null || compareVersionId == null) {
            logger.error("比较标准答案版本失败 - 版本ID为空");
            throw new IllegalArgumentException("版本ID不能为空");
        }

        try {
            Map<String, Object> comparison = new HashMap<>();
            
            // 获取两个版本的变更日志
            var baseVersion = changeLogRepository.findById(baseVersionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到基准版本"));
            var compareVersion = changeLogRepository.findById(compareVersionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到比较版本"));

            // 获取两个版本的变更详情
            var baseDetails = changeLogDetailRepository.findByChangeLogId(baseVersionId);
            var compareDetails = changeLogDetailRepository.findByChangeLogId(compareVersionId);

            // 比较基本信息
            comparison.put("baseVersion", Map.of(
                "id", baseVersion.getId(),
                "commitMessage", baseVersion.getCommitMessage(),
                "commitTime", baseVersion.getCommitTime(),
                "userId", baseVersion.getUser().getId(),
                "userName", baseVersion.getUser().getUsername()
            ));

            comparison.put("compareVersion", Map.of(
                "id", compareVersion.getId(),
                "commitMessage", compareVersion.getCommitMessage(),
                "commitTime", compareVersion.getCommitTime(),
                "userId", compareVersion.getUser().getId(),
                "userName", compareVersion.getUser().getUsername()
            ));

            // 比较字段变化
            List<Map<String, Object>> fieldDiffs = new ArrayList<>();
            Set<String> allFields = new HashSet<>();
            baseDetails.forEach(detail -> allFields.add(detail.getAttributeName()));
            compareDetails.forEach(detail -> allFields.add(detail.getAttributeName()));

            for (String field : allFields) {
                Map<String, Object> fieldDiff = new HashMap<>();
                fieldDiff.put("field", field);

                var baseValue = baseDetails.stream()
                    .filter(d -> d.getAttributeName().equals(field))
                    .map(d -> d.getNewValue())
                    .findFirst()
                    .orElse(null);

                var compareValue = compareDetails.stream()
                    .filter(d -> d.getAttributeName().equals(field))
                    .map(d -> d.getNewValue())
                    .findFirst()
                    .orElse(null);

                fieldDiff.put("baseValue", baseValue);
                fieldDiff.put("compareValue", compareValue);
                fieldDiff.put("changed", !Objects.equals(baseValue, compareValue));

                fieldDiffs.add(fieldDiff);
            }

            comparison.put("fieldDiffs", fieldDiffs);

            logger.info("成功比较标准答案版本 - 基准版本ID: {}, 比较版本ID: {}", baseVersionId, compareVersionId);
            return comparison;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("比较标准答案版本时发生错误", e);
            throw new RuntimeException("比较版本时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Object rollbackAnswer(Long versionId, Long userId, String commitMessage) {
        logger.debug("开始回滚标准答案到指定版本 - 版本ID: {}, 用户ID: {}", versionId, userId);
        
        if (versionId == null || userId == null) {
            logger.error("回滚标准答案失败 - 参数为空");
            throw new IllegalArgumentException("版本ID和用户ID不能为空");
        }

        try {
            // 获取要回滚到的版本
            ChangeLog targetVersion = changeLogRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的版本"));
            
            // 获取标准问题
            StandardQuestion standardQuestion = targetVersion.getAssociatedStandardQuestion();
            if (standardQuestion == null) {
                throw new IllegalStateException("该版本没有关联的标准问题");
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的用户"));
            
            // 创建新的变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.ROLLBACK_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage(commitMessage);
            changeLog.setCommitTime(LocalDateTime.now());
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            // 根据问题类型获取当前答案
            Object currentAnswer = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    currentAnswer = objectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
                        .orElse(null);
                    break;
                case SIMPLE_FACT:
                    currentAnswer = simpleAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
                        .orElse(null);
                    break;
                case SUBJECTIVE:
                    currentAnswer = subjectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            if (currentAnswer == null) {
                throw new IllegalStateException("找不到当前的标准答案");
            }
            
            // 获取变更详情
            List<ChangeLogDetail> targetDetails = changeLogDetailRepository.findByChangeLogId(versionId);
            if (targetDetails == null || targetDetails.isEmpty()) {
                throw new IllegalStateException("目标版本没有变更详情");
            }
            
            // 根据问题类型执行回滚
            Object result = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    result = rollbackObjectiveAnswer(targetDetails, standardQuestion, user, changeLog);
                    break;
                case SIMPLE_FACT:
                    result = rollbackSimpleAnswer(targetDetails, standardQuestion, user, changeLog);
                    break;
                case SUBJECTIVE:
                    result = rollbackSubjectiveAnswer(targetDetails, standardQuestion, user, changeLog);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }

            logger.info("成功回滚标准答案 - 版本ID: {}, 用户ID: {}", versionId, userId);
            return result;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("回滚标准答案时发生错误", e);
            throw new RuntimeException("回滚答案时发生错误: " + e.getMessage());
        }
    }
    
    private StandardObjectiveAnswer rollbackObjectiveAnswer(
            List<ChangeLogDetail> targetDetails, 
            StandardQuestion standardQuestion,
            User user,
            ChangeLog changeLog) {
        
        // 创建新的答案实例
        StandardObjectiveAnswer answer = new StandardObjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        // 从变更详情中恢复数据
        for (ChangeLogDetail detail : targetDetails) {
            switch (detail.getAttributeName()) {
                case "options":
                    answer.setOptions(detail.getNewValue());
                    break;
                case "correctOptionIds":
                    answer.setCorrectOptionIds(detail.getNewValue());
                    break;
            }
        }
        
        // 软删除当前答案
        objectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
            .ifPresent(current -> {
                current.setDeletedAt(LocalDateTime.now());
                objectiveAnswerRepository.save(current);
            });
        
        // 保存新答案
        answer = objectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        for (ChangeLogDetail detail : targetDetails) {
            ChangeLogDetail newDetail = new ChangeLogDetail();
            newDetail.setChangeLog(changeLog);
            newDetail.setEntityType(detail.getEntityType());
            newDetail.setEntityId(answer.getId());
            newDetail.setAttributeName(detail.getAttributeName());
            newDetail.setOldValue(null); // 回滚操作不需要记录旧值
            newDetail.setNewValue(detail.getNewValue());
            changeLogDetailRepository.save(newDetail);
        }
        
        return answer;
    }
    
    private StandardSimpleAnswer rollbackSimpleAnswer(
            List<ChangeLogDetail> targetDetails, 
            StandardQuestion standardQuestion,
            User user,
            ChangeLog changeLog) {
        
        // 创建新的答案实例
        StandardSimpleAnswer answer = new StandardSimpleAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        // 从变更详情中恢复数据
        for (ChangeLogDetail detail : targetDetails) {
            switch (detail.getAttributeName()) {
                case "answerText":
                    answer.setAnswerText(detail.getNewValue());
                    break;
                case "alternativeAnswers":
                    answer.setAlternativeAnswers(detail.getNewValue());
                    break;
            }
        }
        
        // 软删除当前答案
        simpleAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId())
            .ifPresent(current -> {
                current.setDeletedAt(LocalDateTime.now());
                simpleAnswerRepository.save(current);
            });
        
        // 保存新答案
        answer = simpleAnswerRepository.save(answer);
        
        // 记录变更详情
        for (ChangeLogDetail detail : targetDetails) {
            ChangeLogDetail newDetail = new ChangeLogDetail();
            newDetail.setChangeLog(changeLog);
            newDetail.setEntityType(detail.getEntityType());
            newDetail.setEntityId(answer.getId());
            newDetail.setAttributeName(detail.getAttributeName());
            newDetail.setOldValue(null); // 回滚操作不需要记录旧值
            newDetail.setNewValue(detail.getNewValue());
            changeLogDetailRepository.save(newDetail);
        }
        
        return answer;
    }
    
    private StandardSubjectiveAnswer rollbackSubjectiveAnswer(
            List<ChangeLogDetail> targetDetails, 
            StandardQuestion standardQuestion,
            User user,
            ChangeLog changeLog) {
        
        // 创建新的答案实例
        StandardSubjectiveAnswer answer = new StandardSubjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        // 从变更详情中恢复数据
        for (ChangeLogDetail detail : targetDetails) {
            switch (detail.getAttributeName()) {
                case "answerText":
                    answer.setAnswerText(detail.getNewValue());
                    break;
                case "scoringGuidance":
                    answer.setScoringGuidance(detail.getNewValue());
                    break;
            }
        }
        
        // 软删除当前答案
        StandardSubjectiveAnswer current = subjectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
        if (current != null) {
            current.setDeletedAt(LocalDateTime.now());
            subjectiveAnswerRepository.save(current);
        }
        
        // 保存新答案
        answer = subjectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        for (ChangeLogDetail detail : targetDetails) {
            ChangeLogDetail newDetail = new ChangeLogDetail();
            newDetail.setChangeLog(changeLog);
            newDetail.setEntityType(detail.getEntityType());
            newDetail.setEntityId(answer.getId());
            newDetail.setAttributeName(detail.getAttributeName());
            newDetail.setOldValue(null); // 回滚操作不需要记录旧值
            newDetail.setNewValue(detail.getNewValue());
            changeLogDetailRepository.save(newDetail);
        }
        
        return answer;
    }
} 