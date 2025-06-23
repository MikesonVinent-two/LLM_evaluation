package com.example.demo.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.BatchTagOperationsDTO.TagOperation;
import com.example.demo.dto.ChangeDetailDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.TagOperationDTO;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.DifficultyLevel;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardQuestionTag;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;
import com.example.demo.repository.jdbc.CrowdsourcedAnswerRepository;
import com.example.demo.repository.jdbc.DatasetQuestionMappingRepository;
import com.example.demo.repository.jdbc.ExpertCandidateAnswerRepository;
import com.example.demo.repository.jdbc.RawQuestionRepository;
import com.example.demo.repository.jdbc.StandardObjectiveAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.StandardQuestionTagRepository;
import com.example.demo.repository.jdbc.StandardSimpleAnswerRepository;
import com.example.demo.repository.jdbc.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.jdbc.TagRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.StandardQuestionService;
import com.example.demo.util.ChangeLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.util.CacheUtils;

@Service
public class StandardQuestionServiceImpl implements StandardQuestionService {

    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionServiceImpl.class);

    private final StandardQuestionRepository standardQuestionRepository;
    private final RawQuestionRepository rawQuestionRepository;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;
    private final ChangeLogDetailRepository changeLogDetailRepository;
    private final TagRepository tagRepository;
    private final StandardQuestionTagRepository standardQuestionTagRepository;
    private final ObjectMapper objectMapper;
    private final CrowdsourcedAnswerRepository crowdsourcedAnswerRepository;
    private final ExpertCandidateAnswerRepository expertCandidateAnswerRepository;
    private final StandardObjectiveAnswerRepository standardObjectiveAnswerRepository;
    private final StandardSimpleAnswerRepository standardSimpleAnswerRepository;
    private final StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository;
    private final DatasetQuestionMappingRepository datasetQuestionMappingRepository;
    
    private final CacheUtils cacheUtils;

    // 显式构造函数
    public StandardQuestionServiceImpl(
            StandardQuestionRepository standardQuestionRepository,
            RawQuestionRepository rawQuestionRepository,
            UserRepository userRepository,
            ChangeLogRepository changeLogRepository,
            ChangeLogDetailRepository changeLogDetailRepository,
            TagRepository tagRepository,
            StandardQuestionTagRepository standardQuestionTagRepository,
            ObjectMapper objectMapper,
            CrowdsourcedAnswerRepository crowdsourcedAnswerRepository,
            ExpertCandidateAnswerRepository expertCandidateAnswerRepository,
            StandardObjectiveAnswerRepository standardObjectiveAnswerRepository,
            StandardSimpleAnswerRepository standardSimpleAnswerRepository,
            StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository,
            DatasetQuestionMappingRepository datasetQuestionMappingRepository,
            CacheUtils cacheUtils) {
        this.standardQuestionRepository = standardQuestionRepository;
        this.rawQuestionRepository = rawQuestionRepository;
        this.userRepository = userRepository;
        this.changeLogRepository = changeLogRepository;
        this.changeLogDetailRepository = changeLogDetailRepository;
        this.tagRepository = tagRepository;
        this.standardQuestionTagRepository = standardQuestionTagRepository;
        this.objectMapper = objectMapper;
        this.crowdsourcedAnswerRepository = crowdsourcedAnswerRepository;
        this.expertCandidateAnswerRepository = expertCandidateAnswerRepository;
        this.standardObjectiveAnswerRepository = standardObjectiveAnswerRepository;
        this.standardSimpleAnswerRepository = standardSimpleAnswerRepository;
        this.standardSubjectiveAnswerRepository = standardSubjectiveAnswerRepository;
        this.datasetQuestionMappingRepository = datasetQuestionMappingRepository;
        this.cacheUtils = cacheUtils;
    }

    @Override
    @Transactional
    public StandardQuestionDTO createStandardQuestion(StandardQuestionDTO questionDTO, Long userId) {
        logger.debug("开始创建标准问题 - 用户ID: {}, 问题文本: {}", userId, questionDTO.getQuestionText());
        
        // 验证用户ID
        if (userId == null) {
            logger.error("创建标准问题失败 - 用户ID为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("创建标准问题失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 创建标准问题实体
            StandardQuestion standardQuestion = new StandardQuestion();
            standardQuestion.setQuestionText(questionDTO.getQuestionText());
            standardQuestion.setQuestionType(questionDTO.getQuestionType());
            standardQuestion.setDifficulty(questionDTO.getDifficulty());
            standardQuestion.setCreatedByUser(user);
            
            // 如果有原始问题ID，设置关联
            if (questionDTO.getOriginalRawQuestionId() != null) {
                RawQuestion rawQuestion = rawQuestionRepository.findById(questionDTO.getOriginalRawQuestionId())
                    .orElseThrow(() -> {
                        logger.error("创建标准问题失败 - 找不到原始问题ID: {}", questionDTO.getOriginalRawQuestionId());
                        return new IllegalArgumentException("找不到指定的原始问题（ID: " + questionDTO.getOriginalRawQuestionId() + "）");
                    });
                standardQuestion.setOriginalRawQuestion(rawQuestion);
            }
            
            // 如果指定了父问题，则需要先查询
            if (questionDTO.getParentStandardQuestionId() != null) {
                StandardQuestion parentQuestion = standardQuestionRepository.findById(questionDTO.getParentStandardQuestionId())
                    .orElseThrow(() -> {
                        logger.error("创建标准问题失败 - 找不到父标准问题ID: {}", questionDTO.getParentStandardQuestionId());
                        return new IllegalArgumentException("找不到指定的父标准问题（ID: " + questionDTO.getParentStandardQuestionId() + "）");
                    });
                standardQuestion.setParentStandardQuestion(parentQuestion);
            }
            
            // 先保存标准问题，获取ID
            try {
                standardQuestion = standardQuestionRepository.save(standardQuestion);
                logger.debug("已保存标准问题 - ID: {}", standardQuestion.getId());
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存标准问题时出错", e);
                throw new RuntimeException("保存标准问题时出错: " + e.getMessage());
            }
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(questionDTO.getParentStandardQuestionId() != null ? 
                ChangeType.UPDATE_STANDARD_QUESTION : ChangeType.CREATE_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage(questionDTO.getCommitMessage());
            // 设置关联的标准问题（此时标准问题已有ID）
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            
            // 保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }
            
            // 设置变更日志关联并更新标准问题
            standardQuestion.setCreatedChangeLog(changeLog);
            try {
                standardQuestion = standardQuestionRepository.save(standardQuestion);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 更新标准问题变更日志关联时出错", e);
                throw new RuntimeException("更新标准问题变更日志关联时出错: " + e.getMessage());
            }
            
            // 处理标签关联
            if (questionDTO.getTags() != null && !questionDTO.getTags().isEmpty()) {
                processQuestionTags(standardQuestion, questionDTO.getTags(), user, changeLog);
            }
            
            // 现在标准问题已经有ID了，可以创建变更详情
            try {
                if (questionDTO.getParentStandardQuestionId() != null) {
                    StandardQuestion parentQuestion = standardQuestion.getParentStandardQuestion();
                    List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        parentQuestion,
                        standardQuestion,
                        "questionText", "questionType", "difficulty"
                    );
                    
                    // 保存所有变更详情
                    for (ChangeLogDetail detail : details) {
                        changeLogDetailRepository.save(detail);
                    }
                } else {
                    // 如果是全新创建的问题，记录所有字段为新增
                    ChangeLogDetail textDetail = ChangeLogUtils.createDetail(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        "questionText",
                        null,
                        standardQuestion.getQuestionText()
                    );
                    changeLogDetailRepository.save(textDetail);
                    
                    ChangeLogDetail typeDetail = ChangeLogUtils.createDetail(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        "questionType",
                        null,
                        standardQuestion.getQuestionType()
                    );
                    changeLogDetailRepository.save(typeDetail);
                    
                    if (standardQuestion.getDifficulty() != null) {
                        ChangeLogDetail difficultyDetail = ChangeLogUtils.createDetail(
                            changeLog,
                            EntityType.STANDARD_QUESTION,
                            standardQuestion.getId(), // 使用新创建的问题ID
                            "difficulty",
                            null,
                            standardQuestion.getDifficulty()
                        );
                        changeLogDetailRepository.save(difficultyDetail);
                    }
                }
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存变更详情时出错", e);
                throw new RuntimeException("保存变更详情时出错: " + e.getMessage());
            }
            
            logger.info("成功创建标准问题 - ID: {}, 用户ID: {}", standardQuestion.getId(), userId);
            return convertToDTO(standardQuestion);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建标准问题时发生未预期的错误", e);
            throw new RuntimeException("创建标准问题时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public StandardQuestionDTO updateStandardQuestion(Long questionId, StandardQuestionDTO questionDTO, Long userId) {
        logger.debug("开始修改标准问题 - 问题ID: {}, 用户ID: {}", questionId, userId);
        
        // 验证参数
        if (questionId == null || userId == null) {
            logger.error("修改标准问题失败 - 问题ID或用户ID为空");
            throw new IllegalArgumentException("问题ID和用户ID不能为空");
        }

        try {
            // 获取原问题
            StandardQuestion originalQuestion = standardQuestionRepository.findById(questionId)
                .orElseThrow(() -> {
                    logger.error("修改标准问题失败 - 找不到问题ID: {}", questionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + questionId + "）");
                });

            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("修改标准问题失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });

            // 创建新版本
            StandardQuestion newVersion = new StandardQuestion();
            newVersion.setQuestionText(questionDTO.getQuestionText());
            newVersion.setQuestionType(questionDTO.getQuestionType());
            newVersion.setDifficulty(questionDTO.getDifficulty());
            newVersion.setCreatedByUser(user);
            newVersion.setParentStandardQuestion(originalQuestion);
            newVersion.setOriginalRawQuestion(originalQuestion.getOriginalRawQuestion());

            // 先保存新版本标准问题
            try {
                newVersion = standardQuestionRepository.save(newVersion);
                logger.debug("已保存标准问题新版本 - ID: {}", newVersion.getId());
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 保存新版本时出错", e);
                throw new RuntimeException("保存新版本时出错: " + e.getMessage());
            }

            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage(questionDTO.getCommitMessage());
            // 设置关联的标准问题（此时标准问题已有ID）
            changeLog.setAssociatedStandardQuestion(newVersion);
            
            // 保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }

            // 设置变更日志关联并更新标准问题
            newVersion.setCreatedChangeLog(changeLog);
            try {
                newVersion = standardQuestionRepository.save(newVersion);
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 更新标准问题变更日志关联时出错", e);
                throw new RuntimeException("更新标准问题变更日志关联时出错: " + e.getMessage());
            }

            // 处理标签关联
            if (questionDTO.getTags() != null && !questionDTO.getTags().isEmpty()) {
                processQuestionTags(newVersion, questionDTO.getTags(), user, changeLog);
            }

            // 创建变更详情
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_QUESTION,
                newVersion.getId(),
                originalQuestion,
                newVersion,
                "questionText", "questionType", "difficulty"
            );

            // 保存所有变更详情
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }

            logger.info("成功修改标准问题 - 原问题ID: {}, 新版本ID: {}, 用户ID: {}", 
                questionId, newVersion.getId(), userId);
            return convertToDTO(newVersion);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("修改标准问题时发生未预期的错误", e);
            throw new RuntimeException("修改标准问题时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理标准问题的标签关联
     * @param standardQuestion 已保存的标准问题实体
     * @param tagNames 标签名称列表
     * @param user 当前用户
     * @param changeLog 变更日志
     */
    @Transactional
    protected void processQuestionTags(StandardQuestion standardQuestion, List<String> tagNames, 
                                      User user, ChangeLog changeLog) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        
        List<StandardQuestionTag> questionTags = new ArrayList<>();
        
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            
            // 查找或创建标签
            Tag tag = tagRepository.findByTagName(tagName.trim())
                    .orElseGet(() -> {
                        Tag newTag = new Tag(tagName.trim());
                        newTag.setCreatedByUser(user);
                        newTag.setCreatedChangeLog(changeLog);
                        return tagRepository.save(newTag);
                    });
            
            // 如果该问题与标签的关联不存在，则创建关联
            if (!standardQuestionTagRepository.existsByStandardQuestionAndTag(standardQuestion, tag)) {
                StandardQuestionTag questionTag = new StandardQuestionTag(standardQuestion, tag, user);
                questionTag.setCreatedChangeLog(changeLog);
                questionTags.add(questionTag);
                standardQuestion.addTag(questionTag);
                
                // 记录变更日志详情
                ChangeLogDetail tagDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    standardQuestion.getId(),
                    "tag_id",
                    null,
                    tag.getId()
                );
                changeLogDetailRepository.save(tagDetail);
            }
        }
        
        if (!questionTags.isEmpty()) {
            standardQuestionTagRepository.saveAll(questionTags);
        }
    }

    private StandardQuestionDTO convertToDTO(StandardQuestion question) {
        StandardQuestionDTO dto = new StandardQuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setDifficulty(question.getDifficulty());
        dto.setUserId(question.getCreatedByUser().getId());
        
        if (question.getParentStandardQuestion() != null) {
            dto.setParentStandardQuestionId(question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.setOriginalRawQuestionId(question.getOriginalRawQuestion().getId());
        }
        
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.setTags(tags);
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionHistoryDTO> getQuestionHistory(Long questionId) {
        logger.debug("开始获取问题修改历史 - 问题ID: {}", questionId);
        
        // 尝试使用递归SQL获取完整的问题历史
        try {
            List<QuestionHistoryDTO> recursiveHistory = getQuestionHistoryWithRecursiveQuery(questionId);
            if (!recursiveHistory.isEmpty()) {
                logger.info("成功使用递归查询获取问题历史 - 问题ID: {}, 版本数量: {}", 
                    questionId, recursiveHistory.size());
                return recursiveHistory;
            }
        } catch (Exception e) {
            logger.warn("递归查询问题历史失败，回退到原始实现 - 问题ID: {}, 错误: {}", 
                questionId, e.getMessage());
        }
        
        // 如果递归查询失败，回退到原始实现
        logger.info("使用原始方法获取问题历史 - 问题ID: {}", questionId);
        
        StandardQuestion currentQuestion = standardQuestionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("问题不存在"));

        List<QuestionHistoryDTO> history = new ArrayList<>();
        collectQuestionHistory(currentQuestion, history);
        
        // 去重处理，确保每个问题ID只出现一次
        Map<Long, QuestionHistoryDTO> uniqueNodes = new HashMap<>();
        for (QuestionHistoryDTO node : history) {
            uniqueNodes.put(node.getId(), node);
        }
        
        List<QuestionHistoryDTO> uniqueHistory = new ArrayList<>(uniqueNodes.values());
        
        // 按创建时间排序，最新的在前面
        uniqueHistory.sort((a, b) -> b.getCreationTime().compareTo(a.getCreationTime()));
        
        // 记录更多详细信息，特别是父子关系
        logger.debug("成功获取问题修改历史 - 问题ID: {}, 原始版本数量: {}, 去重后版本数量: {}", 
            questionId, history.size(), uniqueHistory.size());
        
        for (QuestionHistoryDTO version : uniqueHistory) {
            logger.info("历史版本 - ID: {}, 父ID: {}, 创建时间: {}", 
                version.getId(), version.getParentQuestionId(), version.getCreationTime());
        }
        
        return uniqueHistory;
    }

    /**
     * 使用递归SQL查询获取问题历史
     * 
     * @param questionId 问题ID
     * @return 问题历史列表
     */
    private List<QuestionHistoryDTO> getQuestionHistoryWithRecursiveQuery(Long questionId) {
        logger.debug("开始使用递归SQL获取问题历史 - 问题ID: {}", questionId);
        
        // 查询从当前问题向上追溯的所有父问题
        String sql = 
            "WITH RECURSIVE question_history AS (" +
            "  SELECT sq.* " +
            "  FROM standard_questions sq " +
            "  WHERE sq.id = ? AND sq.deleted_at IS NULL " +
            "  UNION ALL " +
            "  SELECT p.* " +
            "  FROM standard_questions p " +
            "  JOIN question_history q ON p.id = q.parent_standard_question_id " +
            "  WHERE p.deleted_at IS NULL" +
            ") " +
            "SELECT * FROM question_history";
        
        List<StandardQuestion> historyQuestions;
        try {
            // 创建一个RowMapper来处理结果集
            RowMapper<StandardQuestion> rowMapper = new RowMapper<StandardQuestion>() {
                @Override
                public StandardQuestion mapRow(ResultSet rs, int rowNum) throws SQLException {
                    StandardQuestion question = new StandardQuestion();
                    question.setId(rs.getLong("id"));
                    question.setQuestionText(rs.getString("question_text"));
                    
                    // 解析枚举
                    String questionTypeStr = rs.getString("question_type");
                    if (questionTypeStr != null) {
                        question.setQuestionType(QuestionType.valueOf(questionTypeStr));
                    }
                    
                    String difficultyStr = rs.getString("difficulty");
                    if (difficultyStr != null) {
                        question.setDifficulty(DifficultyLevel.valueOf(difficultyStr));
                    }
                    
                    // 设置时间
                    Timestamp creationTime = rs.getTimestamp("creation_time");
                    if (creationTime != null) {
                        question.setCreationTime(creationTime.toLocalDateTime());
                    }
                    
                    // 设置父问题ID
                    Long parentStandardQuestionId = rs.getLong("parent_standard_question_id");
                    if (!rs.wasNull()) {
                        StandardQuestion parentQuestion = new StandardQuestion();
                        parentQuestion.setId(parentStandardQuestionId);
                        question.setParentStandardQuestion(parentQuestion);
                    }
                    
                    // 设置创建者用户ID
                    Long createdByUserId = rs.getLong("created_by_user_id");
                    if (!rs.wasNull()) {
                        User user = new User();
                        user.setId(createdByUserId);
                        question.setCreatedByUser(user);
                    }
                    
                    // 设置变更日志ID
                    Long createdChangeLogId = rs.getLong("created_change_log_id");
                    if (!rs.wasNull()) {
                        ChangeLog changeLog = new ChangeLog();
                        changeLog.setId(createdChangeLogId);
                        question.setCreatedChangeLog(changeLog);
                    }
                    
                    return question;
                }
            };
            
            // 使用自定义查询执行递归SQL
            historyQuestions = standardQuestionRepository.findHistoryWithRecursiveQuery(questionId, sql, rowMapper);
        } catch (Exception e) {
            logger.error("递归SQL查询问题历史失败 - 问题ID: {}", questionId, e);
            return new ArrayList<>();
        }
        
        if (historyQuestions.isEmpty()) {
            logger.warn("递归SQL查询问题历史未找到结果 - 问题ID: {}", questionId);
            return new ArrayList<>();
        }
        
        // 转换为DTO
        List<QuestionHistoryDTO> history = new ArrayList<>();
        for (StandardQuestion question : historyQuestions) {
            QuestionHistoryDTO dto = convertToHistoryDTO(question);
            history.add(dto);
            logger.debug("历史问题 - ID: {}, 父ID: {}", 
                question.getId(), 
                question.getParentStandardQuestion() != null ? 
                    question.getParentStandardQuestion().getId() : null);
        }
        
        // 按创建时间排序，最新的在前面
        history.sort((a, b) -> b.getCreationTime().compareTo(a.getCreationTime()));
        
        logger.info("成功使用递归SQL获取问题历史 - 问题ID: {}, 版本数量: {}", 
            questionId, history.size());
        
        return history;
    }

    private void collectQuestionHistory(StandardQuestion question, List<QuestionHistoryDTO> history) {
        if (question == null) {
            return;
        }
        
        QuestionHistoryDTO historyDTO = convertToHistoryDTO(question);
        history.add(historyDTO);
        
        if (question.getParentStandardQuestion() != null) {
            collectQuestionHistory(question.getParentStandardQuestion(), history);
        }
    }

    private StandardQuestion findRootQuestion(StandardQuestion question) {
        StandardQuestion current = question;
        while (current.getParentStandardQuestion() != null) {
            current = current.getParentStandardQuestion();
        }
        return current;
    }

    private QuestionHistoryDTO convertToHistoryDTO(StandardQuestion question) {
        QuestionHistoryDTO dto = new QuestionHistoryDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        
        // 处理 questionType 可能为 null 的情况
        if (question.getQuestionType() != null) {
            dto.setQuestionType(question.getQuestionType().toString());
        } else {
            dto.setQuestionType("未知");
            logger.warn("问题类型为空 - 问题ID: {}", question.getId());
        }
        
        // 处理 difficulty 可能为 null 的情况
        if (question.getDifficulty() != null) {
            dto.setDifficulty(question.getDifficulty().toString());
        } else {
            dto.setDifficulty("未知");
            logger.warn("难度级别为空 - 问题ID: {}", question.getId());
        }
        
        dto.setCreationTime(question.getCreationTime());
        
        // 处理 createdByUser 可能为 null 的情况
        if (question.getCreatedByUser() != null) {
            dto.setCreatedByUserId(question.getCreatedByUser().getId());
            
            // 添加用户详细信息
            User createdByUser = question.getCreatedByUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", createdByUser.getId());
            userInfo.put("username", createdByUser.getUsername());
            userInfo.put("name", createdByUser.getName());
            userInfo.put("role", createdByUser.getRole());
            userInfo.put("contactInfo", createdByUser.getContactInfo());
            dto.setCreatedByUser(userInfo);
        } else {
            logger.warn("创建用户为空 - 问题ID: {}", question.getId());
            Map<String, Object> emptyUserInfo = new HashMap<>();
            emptyUserInfo.put("id", null);
            emptyUserInfo.put("username", "未知用户");
            emptyUserInfo.put("name", "未知");
            emptyUserInfo.put("role", "未知");
            emptyUserInfo.put("contactInfo", "");
            dto.setCreatedByUser(emptyUserInfo);
        }
        
        // 处理 parentStandardQuestion 可能为null或者只包含ID的情况
        if (question.getParentStandardQuestion() != null) {
            Long parentId = question.getParentStandardQuestion().getId();
            if (parentId != null) {
                // 添加更多调试日志
                logger.debug("设置父问题ID - 当前问题ID: {}, 父问题ID: {}", question.getId(), parentId);
                dto.setParentQuestionId(parentId);
            } else {
                logger.warn("父问题对象存在但ID为null - 问题ID: {}", question.getId());
                
                // 尝试从数据库中查找父问题关系
                List<StandardQuestion> childQuestions = standardQuestionRepository.findByParentStandardQuestionId(question.getId());
                if (!childQuestions.isEmpty()) {
                    for (StandardQuestion child : childQuestions) {
                        logger.info("找到子问题关系 - 父ID: {}, 子ID: {}", question.getId(), child.getId());
                    }
                }
            }
        } else {
            // 明确记录父问题为null的情况
            logger.debug("问题没有父问题对象 - 问题ID: {}", question.getId());
            
            // 尝试从数据库验证是否真的没有父问题
            try {
                StandardQuestion fullQuestion = standardQuestionRepository.findById(question.getId()).orElse(null);
                if (fullQuestion != null && fullQuestion.getParentStandardQuestion() != null) {
                    Long actualParentId = fullQuestion.getParentStandardQuestion().getId();
                    logger.warn("数据库中存在父问题但未加载 - 问题ID: {}, 实际父ID: {}", 
                        question.getId(), actualParentId);
                    dto.setParentQuestionId(actualParentId);
                }
            } catch (Exception e) {
                logger.error("验证父问题关系时出错 - 问题ID: {}", question.getId(), e);
            }
        }
        
        // 处理标签，避免 NPE
        if (question.getQuestionTags() != null) {
            List<String> tags = question.getQuestionTags().stream()
                .filter(tag -> tag != null && tag.getTag() != null)
                .map(tag -> tag.getTag().getTagName())
                .collect(Collectors.toList());
            dto.setTags(tags);
        } else {
            dto.setTags(new ArrayList<>());
        }
        
        // 获取变更日志
        ChangeLog changeLog = changeLogRepository.findByAssociatedStandardQuestionId(question.getId());
        if (changeLog != null) {
            dto.setCommitMessage(changeLog.getCommitMessage());
            dto.setChangeLogId(changeLog.getId()); // 添加变更日志ID
            
            // 获取变更详情
            List<ChangeDetailDTO> changes = changeLogDetailRepository.findByChangeLogId(changeLog.getId())
                .stream()
                .map(this::convertToChangeDetailDTO)
                .collect(Collectors.toList());
            dto.setChanges(changes);
        }
        
        return dto;
    }

    private ChangeDetailDTO convertToChangeDetailDTO(ChangeLogDetail detail) {
        ChangeDetailDTO dto = new ChangeDetailDTO();
        dto.setAttributeName(detail.getAttributeName());
        dto.setOldValue(detail.getOldValue() != null ? detail.getOldValue().toString() : null);
        dto.setNewValue(detail.getNewValue() != null ? detail.getNewValue().toString() : null);
        
        if (detail.getOldValue() == null && detail.getNewValue() != null) {
            dto.setChangeType("ADD");
        } else if (detail.getOldValue() != null && detail.getNewValue() == null) {
            dto.setChangeType("DELETE");
        } else {
            dto.setChangeType("MODIFY");
        }
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StandardQuestionDTO> findAllStandardQuestions(Pageable pageable) {
        logger.debug("获取所有标准问题 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestion> standardQuestionsPage = standardQuestionRepository.findAll(pageable);
            
            // 对问题进行去重处理
            List<StandardQuestion> questions = standardQuestionsPage.getContent();
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            
            // 使用Map来去重，保留每个ID的唯一实例
            for (StandardQuestion question : questions) {
                // 如果已存在该ID的问题，确保合并标签
                if (uniqueQuestionsMap.containsKey(question.getId())) {
                    StandardQuestion existingQuestion = uniqueQuestionsMap.get(question.getId());
                    // 合并标签，保留所有标签
                    for (StandardQuestionTag tag : question.getQuestionTags()) {
                        if (!existingQuestion.getQuestionTags().contains(tag)) {
                            existingQuestion.getQuestionTags().add(tag);
                        }
                    }
                } else {
                    uniqueQuestionsMap.put(question.getId(), question);
                }
            }
            
            // 创建新的列表，保持排序顺序
            List<StandardQuestion> uniqueQuestions = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 创建新的Page对象
            Page<StandardQuestion> uniqueQuestionsPage = new PageImpl<>(
                uniqueQuestions, 
                pageable, 
                standardQuestionsPage.getTotalElements()
            );
            
            logger.info("成功获取标准问题 - 原始总数: {}, 去重后总数: {}", 
                questions.size(), uniqueQuestions.size());
            
            return uniqueQuestionsPage.map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取所有标准问题失败", e);
            throw new RuntimeException("获取标准问题列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StandardQuestionDTO> findLatestStandardQuestions(Pageable pageable) {
        logger.debug("获取所有最新版本的标准问题 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestion> latestQuestionsPage = standardQuestionRepository.findLatestVersions(pageable);
            
            // 对问题进行去重处理
            List<StandardQuestion> questions = latestQuestionsPage.getContent();
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            
            // 使用Map来去重，保留每个ID的唯一实例
            for (StandardQuestion question : questions) {
                // 如果已存在该ID的问题，确保合并标签
                if (uniqueQuestionsMap.containsKey(question.getId())) {
                    StandardQuestion existingQuestion = uniqueQuestionsMap.get(question.getId());
                    // 合并标签，保留所有标签
                    for (StandardQuestionTag tag : question.getQuestionTags()) {
                        if (!existingQuestion.getQuestionTags().contains(tag)) {
                            existingQuestion.getQuestionTags().add(tag);
                        }
                    }
                } else {
                    uniqueQuestionsMap.put(question.getId(), question);
                }
            }
            
            // 创建新的列表，保持排序顺序
            List<StandardQuestion> uniqueQuestions = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 创建新的Page对象
            Page<StandardQuestion> uniqueQuestionsPage = new PageImpl<>(
                uniqueQuestions, 
                pageable, 
                latestQuestionsPage.getTotalElements()
            );
            
            logger.info("成功获取最新版本标准问题 - 原始总数: {}, 去重后总数: {}, 总记录数: {}", 
                questions.size(), uniqueQuestions.size(), uniqueQuestionsPage.getTotalElements());
            
            return uniqueQuestionsPage.map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取最新版本标准问题失败", e);
            throw new RuntimeException("获取最新版本标准问题列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public StandardQuestionDTO updateQuestionTags(TagOperationDTO operationDTO) {
        logger.debug("开始更新标准问题标签 - 问题ID: {}, 操作类型: {}", 
            operationDTO.getQuestionId(), operationDTO.getOperationType());
        
        if (operationDTO.getQuestionId() == null || operationDTO.getUserId() == null) {
            throw new IllegalArgumentException("问题ID和用户ID不能为空");
        }
        
        try {
            // 获取标准问题
            StandardQuestion question = standardQuestionRepository.findById(operationDTO.getQuestionId())
                .orElseThrow(() -> {
                    logger.error("更新标签失败 - 找不到问题ID: {}", operationDTO.getQuestionId());
                    return new IllegalArgumentException("找不到指定的标准问题");
                });
            
            // 获取用户
            User user = userRepository.findById(operationDTO.getUserId())
                .orElseThrow(() -> {
                    logger.error("更新标签失败 - 找不到用户ID: {}", operationDTO.getUserId());
                    return new IllegalArgumentException("找不到指定的用户");
                });
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_QUESTION_TAGS);
            changeLog.setUser(user);
            changeLog.setCommitMessage(operationDTO.getCommitMessage());
            changeLog.setAssociatedStandardQuestion(question);
            changeLogRepository.save(changeLog);
            
            // 获取当前标签
            Set<String> currentTags = question.getQuestionTags().stream()
                .map(tag -> tag.getTag().getTagName())
                .collect(Collectors.toSet());
            
            // 新标签集合
            Set<String> newTags = new HashSet<>();
            
            // 根据操作类型处理标签
            switch (operationDTO.getOperationType()) {
                case ADD:
                    // 添加标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(currentTags);  // 保留现有标签
                        newTags.addAll(operationDTO.getTags());  // 添加新标签
                    }
                    break;
                    
                case REMOVE:
                    // 移除标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(currentTags);  // 复制现有标签
                        newTags.removeAll(operationDTO.getTags());  // 移除指定标签
                    }
                    break;
                    
                case REPLACE:
                    // 替换所有标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(operationDTO.getTags());  // 使用新标签集合
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的操作类型: " + operationDTO.getOperationType());
            }
            
            // 清除现有标签关联
            List<StandardQuestionTag> existingTags = new ArrayList<>(question.getQuestionTags());
            for (StandardQuestionTag tagLink : existingTags) {
                question.removeTag(tagLink);
                standardQuestionTagRepository.delete(tagLink);
                
                // 记录变更日志详情
                ChangeLogDetail removeDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    question.getId(),
                    "tag_id",
                    tagLink.getTag().getId(),
                    null
                );
                changeLogDetailRepository.save(removeDetail);
            }
            
            // 添加新标签
            for (String tagName : newTags) {
                if (!StringUtils.hasText(tagName)) {
                    continue;
                }
                
                // 查找或创建标签
                Tag tag = tagRepository.findByTagName(tagName.trim())
                        .orElseGet(() -> {
                            Tag newTag = new Tag(tagName.trim());
                            newTag.setCreatedByUser(user);
                            newTag.setCreatedChangeLog(changeLog);
                            return tagRepository.save(newTag);
                        });
                
                // 创建关联
                StandardQuestionTag questionTag = new StandardQuestionTag(question, tag, user);
                questionTag.setCreatedChangeLog(changeLog);
                standardQuestionTagRepository.save(questionTag);
                question.addTag(questionTag);
                
                // 记录变更日志详情
                ChangeLogDetail addDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    question.getId(),
                    "tag_id",
                    null,
                    tag.getId()
                );
                changeLogDetailRepository.save(addDetail);
            }
            
            // 保存问题
            question = standardQuestionRepository.save(question);
            
            logger.info("成功更新标准问题标签 - 问题ID: {}, 标签数量: {}", 
                question.getId(), newTags.size());
            
            return convertToDTO(question);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新标准问题标签时发生错误", e);
            throw new RuntimeException("更新标准问题标签时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<Long, Boolean> batchUpdateQuestionTags(BatchTagOperationsDTO batchOperationsDTO) {
        logger.debug("开始批量更新标准问题标签 - 操作数量: {}", 
            batchOperationsDTO.getOperations() != null ? batchOperationsDTO.getOperations().size() : 0);
        
        if (batchOperationsDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (batchOperationsDTO.getOperations() == null || batchOperationsDTO.getOperations().isEmpty()) {
            throw new IllegalArgumentException("操作列表不能为空");
        }
        
        // 操作结果
        Map<Long, Boolean> results = new HashMap<>();
        
        try {
            // 获取用户
            User user = userRepository.findById(batchOperationsDTO.getUserId())
                .orElseThrow(() -> {
                    logger.error("批量更新标签失败 - 找不到用户ID: {}", batchOperationsDTO.getUserId());
                    return new IllegalArgumentException("找不到指定的用户");
                });
            
            // 逐个处理操作
            for (TagOperation operation : batchOperationsDTO.getOperations()) {
                try {
                    // 构建单个操作DTO
                    TagOperationDTO singleOperation = new TagOperationDTO();
                    singleOperation.setQuestionId(operation.getQuestionId());
                    singleOperation.setUserId(batchOperationsDTO.getUserId());
                    singleOperation.setOperationType(convertOperationType(operation.getOperationType()));
                    singleOperation.setTags(operation.getTags());
                    singleOperation.setCommitMessage(batchOperationsDTO.getCommitMessage());
                    
                    // 执行单个操作
                    updateQuestionTags(singleOperation);
                    results.put(operation.getQuestionId(), true);
                } catch (Exception e) {
                    logger.error("批量更新标签时处理问题ID: {} 失败", operation.getQuestionId(), e);
                    results.put(operation.getQuestionId(), false);
                }
            }
            
            logger.info("批量更新标准问题标签完成 - 总数: {}, 成功: {}", 
                results.size(), results.values().stream().filter(v -> v).count());
            
            return results;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("批量更新标准问题标签时发生错误", e);
            throw new RuntimeException("批量更新标准问题标签时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 转换操作类型
     */
    private TagOperationDTO.OperationType convertOperationType(BatchTagOperationsDTO.TagOperation.OperationType type) {
        switch (type) {
            case ADD:
                return TagOperationDTO.OperationType.ADD;
            case REMOVE:
                return TagOperationDTO.OperationType.REMOVE;
            case REPLACE:
                return TagOperationDTO.OperationType.REPLACE;
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + type);
        }
    }

    @Override
    public Map<String, Object> searchQuestions(List<String> tags, String keyword, Long userId, Boolean onlyLatest, Pageable pageable) {
        // 调用新的方法，传递false作为onlyWithStandardAnswers的默认值
        return searchQuestions(tags, keyword, userId, onlyLatest, false, pageable);
    }
    
    /**
     * 根据标签列表获取包含所有指定标签的问题
     * 
     * @param tags 标签列表
     * @return 包含所有指定标签的问题列表
     */
    @Cacheable(value = "query_results", key = "'questions_by_tags_' + #tags.toString()")
    private List<StandardQuestion> getQuestionsByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 生成缓存键
        String cacheKey = cacheUtils.generateQueryCacheKey("questions_by_tags", tags);
        
        // 先尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<StandardQuestion> cachedResult = cacheUtils.getCachedQueryResult(cacheKey, List.class);
        if (cachedResult != null) {
            logger.debug("从缓存获取问题列表，标签: {}", tags);
            return cachedResult;
        }
        
        try {
            // 使用优化的数据库查询方法，避免在内存中过滤
            List<StandardQuestion> result = standardQuestionRepository.findByAllTagNames(tags);
            
            // 缓存查询结果（缓存30分钟）
            cacheUtils.cacheQueryResult(cacheKey, result, 30);
            
            return result;
        } catch (Exception e) {
            logger.error("根据标签查询问题失败，回退到内存过滤", e);
            
            // 如果数据库查询失败，回退到内存过滤
            List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
            
            // 过滤出包含所有指定标签的问题
            List<StandardQuestion> result = allQuestions.stream()
                    .filter(question -> {
                        if (question == null) {
                            return false;
                        }
                        
                        // 获取问题标签名称 - 修改为final变量
                        final List<String> questionTagNames;
                        if (question.getQuestionTags() != null) {
                            questionTagNames = question.getQuestionTags().stream()
                                    .filter(tag -> tag != null && tag.getTag() != null && tag.getTag().getTagName() != null)
                                    .map(tag -> tag.getTag().getTagName().toLowerCase())
                                    .collect(Collectors.toList());
                        } else {
                            questionTagNames = new ArrayList<>();
                        }
                        
                        // 检查问题是否包含所有指定标签
                        return tags.stream()
                                .filter(tag -> tag != null)
                                .map(tag -> tag.toLowerCase())
                                .allMatch(tag -> questionTagNames.contains(tag));
                    })
                    .collect(Collectors.toList());
            
            // 缓存结果（较短时间，因为是降级查询）
            cacheUtils.cacheQueryResult(cacheKey, result, 10);
            
            return result;
        }
    }

    @Override
    public Map<String, Object> getOriginalQuestionAndAnswers(Long questionId, Pageable pageable) {
        logger.debug("开始获取原始问题和回答 - 标准问题ID: {}, 页码: {}, 每页大小: {}", 
            questionId, pageable.getPageNumber(), pageable.getPageSize());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(questionId)
                .orElseThrow(() -> {
                    logger.error("获取原始问题和回答失败 - 找不到标准问题ID: {}", questionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + questionId + "）");
                });
            
            // 检查是否有关联的原始问题
            if (standardQuestion.getOriginalRawQuestion() == null) {
                logger.warn("标准问题没有关联的原始问题 - 标准问题ID: {}", questionId);
                return result;
            }
            
            // 获取原始问题
            RawQuestion rawQuestion = standardQuestion.getOriginalRawQuestion();
            
            // 转换原始问题为DTO
            Map<String, Object> rawQuestionDTO = new HashMap<>();
            rawQuestionDTO.put("id", rawQuestion.getId());
            rawQuestionDTO.put("questionText", rawQuestion.getTitle());
            rawQuestionDTO.put("source", rawQuestion.getSourceSite());
            rawQuestionDTO.put("collectionTime", rawQuestion.getCrawlTime());
            
            // 获取原始回答列表
            List<RawAnswer> rawAnswers = rawQuestionRepository.findRawAnswersByQuestionId(rawQuestion.getId());
            
            // 分页处理回答列表
            int totalAnswers = rawAnswers.size();
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), totalAnswers);
            
            List<RawAnswer> pagedAnswers;
            if (start <= end && totalAnswers > 0) {
                pagedAnswers = rawAnswers.subList(start, end);
            } else {
                pagedAnswers = new ArrayList<>();
            }
            
            List<Map<String, Object>> rawAnswersDTO = new ArrayList<>();
            for (RawAnswer answer : pagedAnswers) {
                Map<String, Object> answerDTO = new HashMap<>();
                answerDTO.put("id", answer.getId());
                answerDTO.put("answerText", answer.getContent());
                answerDTO.put("respondent", answer.getAuthorInfo());
                answerDTO.put("answerTime", answer.getPublishTime());
                answerDTO.put("score", answer.getUpvotes());
                
                rawAnswersDTO.add(answerDTO);
            }
            
            // 构建结果
            result.put("standardQuestion", convertToDTO(standardQuestion));
            result.put("rawQuestion", rawQuestionDTO);
            result.put("rawAnswers", rawAnswersDTO);
            result.put("total", totalAnswers);
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) totalAnswers / pageable.getPageSize()));
            
            logger.info("成功获取原始问题和回答 - 标准问题ID: {}, 原始问题ID: {}, 原始回答总数: {}, 当前页回答数: {}", 
                questionId, rawQuestion.getId(), totalAnswers, rawAnswersDTO.size());
            
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取原始问题和回答时发生错误", e);
            throw new RuntimeException("获取原始问题和回答时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> findQuestionsWithoutStandardAnswers(Boolean onlyLatest, Boolean onlyLatestVersion, Pageable pageable) {
        logger.debug("开始查找无标准回答的问题 - 仅最新版本: {}, 仅最新标准问题: {}, 页码: {}, 每页大小: {}", 
            onlyLatest, onlyLatestVersion, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 获取所有标准问题
            List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
            
            // 过滤出没有标准回答的问题
            List<StandardQuestion> questionsWithoutAnswers = allQuestions.stream()
                .filter(question -> {
                    // 检查不同类型问题的标准回答
                    if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        return !standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                        return !standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                        return !standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    }
                    return true;  // 未知类型，默认视为没有回答
                })
                .collect(Collectors.toList());
            
            // 如果需要过滤出叶子节点（没有子问题的节点）
            if (Boolean.TRUE.equals(onlyLatest)) {
                logger.debug("过滤出最新版本（叶子节点）标准问题");
                
                // 创建一个辅助Set用于快速检查问题是否有子问题
                Set<Long> questionsWithChildren = new HashSet<>();
                for (StandardQuestion question : allQuestions) {
                    if (question != null && question.getParentStandardQuestion() != null) {
                        questionsWithChildren.add(question.getParentStandardQuestion().getId());
                    }
                }
                
                // 过滤出没有子问题的节点（叶子节点）
                questionsWithoutAnswers = questionsWithoutAnswers.stream()
                    .filter(q -> q != null && q.getId() != null && !questionsWithChildren.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新版本无标准回答问题数量: {}", questionsWithoutAnswers.size());
            }
            
            // 如果需要只显示最新的标准问题（没有子版本的问题）
            if (Boolean.TRUE.equals(onlyLatestVersion)) {
                logger.debug("过滤出最新标准问题（没有子版本的问题）");
                
                // 获取所有最新版本的标准问题
                List<StandardQuestion> latestVersionQuestions = standardQuestionRepository.findAllLatestVersions();
                
                // 过滤出同时满足"无标准答案"和"是最新版本"的问题
                Set<Long> latestVersionIds = latestVersionQuestions.stream()
                    .map(StandardQuestion::getId)
                    .collect(Collectors.toSet());
                
                questionsWithoutAnswers = questionsWithoutAnswers.stream()
                    .filter(q -> latestVersionIds.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新标准问题数量: {}", questionsWithoutAnswers.size());
            }
            
            // 去重处理
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            for (StandardQuestion question : questionsWithoutAnswers) {
                uniqueQuestionsMap.put(question.getId(), question);
            }
            questionsWithoutAnswers = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 分页处理
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questionsWithoutAnswers.size());
            
            List<StandardQuestion> pagedQuestions;
            if (start <= end) {
                pagedQuestions = questionsWithoutAnswers.subList(start, end);
            } else {
                pagedQuestions = new ArrayList<>();
            }
            
            // 转换为DTO
            List<Map<String, Object>> questionDTOs = pagedQuestions.stream()
                .map(this::convertToDTOWithTags)
                .collect(Collectors.toList());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questions", questionDTOs);
            result.put("total", questionsWithoutAnswers.size());
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) questionsWithoutAnswers.size() / pageable.getPageSize()));
            result.put("onlyLatest", onlyLatest);  // 添加过滤条件信息
            result.put("onlyLatestVersion", onlyLatestVersion);  // 添加新的过滤条件信息
            
            logger.info("成功获取无标准回答问题 - 仅最新版本: {}, 仅最新标准问题: {}, 总数: {}, 当前页: {}", 
                onlyLatest, onlyLatestVersion, questionsWithoutAnswers.size(), questionDTOs.size());
            
            return result;
        } catch (Exception e) {
            logger.error("查找无标准回答问题时发生错误", e);
            throw new RuntimeException("查找无标准回答问题时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 转换标准问题为DTO，包含标签信息
     */
    private Map<String, Object> convertToDTOWithTags(StandardQuestion question) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", question.getId());
        dto.put("questionText", question.getQuestionText());
        dto.put("questionType", question.getQuestionType());
        dto.put("difficulty", question.getDifficulty());
        dto.put("creationTime", question.getCreationTime());
        
        if (question.getCreatedByUser() != null) {
            dto.put("createdByUserId", question.getCreatedByUser().getId());
        }
        
        if (question.getParentStandardQuestion() != null) {
            dto.put("parentQuestionId", question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.put("originalRawQuestionId", question.getOriginalRawQuestion().getId());
        }
        
        // 添加标签信息
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.put("tags", tags);
        
        // 添加是否有标准答案的信息
        boolean hasStandardAnswer = false;
        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
            question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            hasStandardAnswer = standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
        } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
            hasStandardAnswer = standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
        } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
            hasStandardAnswer = standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
        }
        dto.put("hasStandardAnswer", hasStandardAnswer);
        
        return dto;
    }

    @Override
    @Transactional
    public StandardQuestionDTO rollbackQuestion(Long versionId, Long userId, String commitMessage) {
        logger.debug("开始回退标准问题到指定版本 - 版本ID: {}, 用户ID: {}", versionId, userId);
        
        if (versionId == null || userId == null) {
            logger.error("回退标准问题失败 - 参数为空");
            throw new IllegalArgumentException("版本ID和用户ID不能为空");
        }

        try {
            // 获取要回退到的版本
            ChangeLog targetVersion = changeLogRepository.findById(versionId)
                .orElseThrow(() -> {
                    logger.error("回退标准问题失败 - 找不到版本ID: {}", versionId);
                    return new IllegalArgumentException("找不到指定的版本（ID: " + versionId + "）");
                });
            
            // 获取标准问题
            StandardQuestion targetQuestion = targetVersion.getAssociatedStandardQuestion();
            if (targetQuestion == null) {
                logger.error("回退标准问题失败 - 版本没有关联的标准问题");
                throw new IllegalStateException("该版本没有关联的标准问题");
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("回退标准问题失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 根据目标问题的问题文本、问题类型和难度创建新版本问题
            StandardQuestion newVersion = new StandardQuestion();
            newVersion.setQuestionText(targetQuestion.getQuestionText());
            newVersion.setQuestionType(targetQuestion.getQuestionType());
            newVersion.setDifficulty(targetQuestion.getDifficulty());
            newVersion.setCreatedByUser(user);
            newVersion.setOriginalRawQuestion(targetQuestion.getOriginalRawQuestion());
            
            // 找到最新版本作为父问题
            StandardQuestion latestVersion = findLatestVersionOfQuestion(targetQuestion);
            newVersion.setParentStandardQuestion(latestVersion);
            
            // 保存新版本
            try {
                newVersion = standardQuestionRepository.save(newVersion);
                logger.debug("已保存回退后的标准问题 - ID: {}", newVersion.getId());
            } catch (Exception e) {
                logger.error("回退标准问题失败 - 保存新版本时出错", e);
                throw new RuntimeException("保存新版本时出错: " + e.getMessage());
            }
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.ROLLBACK_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage(StringUtils.hasText(commitMessage) ? 
                                      commitMessage : "回退到版本 " + versionId);
            changeLog.setAssociatedStandardQuestion(newVersion);
            
            // 保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("回退标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }
            
            // 设置变更日志关联并更新标准问题
            newVersion.setCreatedChangeLog(changeLog);
            try {
                newVersion = standardQuestionRepository.save(newVersion);
            } catch (Exception e) {
                logger.error("回退标准问题失败 - 更新标准问题变更日志关联时出错", e);
                throw new RuntimeException("更新标准问题变更日志关联时出错: " + e.getMessage());
            }
            
            // 处理标签 - 复制目标版本的标签
            List<StandardQuestionTag> targetTags = standardQuestionTagRepository.findByStandardQuestion(targetQuestion);
            for (StandardQuestionTag targetTag : targetTags) {
                StandardQuestionTag newTag = new StandardQuestionTag(newVersion, targetTag.getTag(), user);
                newTag.setCreatedChangeLog(changeLog);
                standardQuestionTagRepository.save(newTag);
                
                // 记录变更日志详情
                ChangeLogDetail tagDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    newVersion.getId(),
                    "tag_id",
                    null,
                    targetTag.getTag().getId()
                );
                changeLogDetailRepository.save(tagDetail);
            }
            
            // 创建变更详情
            // 记录回退操作详情
            ChangeLogDetail rollbackDetail = ChangeLogUtils.createDetail(
                changeLog,
                EntityType.STANDARD_QUESTION,
                newVersion.getId(),
                "rollback_from_version",
                null,
                versionId
            );
            changeLogDetailRepository.save(rollbackDetail);
            
            // 记录问题文本变更
            ChangeLogDetail textDetail = ChangeLogUtils.createDetail(
                changeLog,
                EntityType.STANDARD_QUESTION,
                newVersion.getId(),
                "questionText",
                latestVersion.getQuestionText(),
                newVersion.getQuestionText()
            );
            changeLogDetailRepository.save(textDetail);
            
            // 记录问题类型变更
            if (!latestVersion.getQuestionType().equals(newVersion.getQuestionType())) {
                ChangeLogDetail typeDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION,
                    newVersion.getId(),
                    "questionType",
                    latestVersion.getQuestionType(),
                    newVersion.getQuestionType()
                );
                changeLogDetailRepository.save(typeDetail);
            }
            
            // 记录难度变更
            if ((latestVersion.getDifficulty() == null && newVersion.getDifficulty() != null) ||
                (latestVersion.getDifficulty() != null && !latestVersion.getDifficulty().equals(newVersion.getDifficulty()))) {
                ChangeLogDetail difficultyDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION,
                    newVersion.getId(),
                    "difficulty",
                    latestVersion.getDifficulty(),
                    newVersion.getDifficulty()
                );
                changeLogDetailRepository.save(difficultyDetail);
            }
            
            logger.info("成功回退标准问题 - 目标版本ID: {}, 新版本ID: {}, 用户ID: {}", 
                versionId, newVersion.getId(), userId);
            
            return convertToDTO(newVersion);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("回退标准问题时发生错误", e);
            throw new RuntimeException("回退标准问题时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 查找问题的最新版本
     * @param question 标准问题
     * @return 最新版本的标准问题
     */
    private StandardQuestion findLatestVersionOfQuestion(StandardQuestion question) {
        // 获取问题的根节点
        StandardQuestion root = findRootQuestion(question);
        
        // 使用DFS查找最新版本
        return findLatestVersionRecursive(root);
    }
    
    /**
     * 递归查找最新版本
     * @param question 当前问题
     * @return 最新版本的标准问题
     */
    private StandardQuestion findLatestVersionRecursive(StandardQuestion question) {
        List<StandardQuestion> children = standardQuestionRepository.findByParentStandardQuestionId(question.getId());
        
        // 如果没有子节点，这就是最新版本
        if (children == null || children.isEmpty()) {
            return question;
        }
        
        // 找出最新的子节点（按创建时间排序）
        StandardQuestion latestChild = children.stream()
            .sorted((a, b) -> b.getCreationTime().compareTo(a.getCreationTime()))
            .findFirst()
            .orElse(question);
        
        // 递归查找该子节点的最新版本
        return findLatestVersionRecursive(latestChild);
    }

    /**
     * 获取新版的版本树
     * 使用递归SQL查询直接获取完整版本树
     */
    private List<QuestionHistoryDTO> getVersionTreeWithRecursiveQuery(Long questionId) {
        logger.debug("开始使用递归SQL获取问题版本树 - 问题ID: {}", questionId);
        
        // 使用新的查询方法获取完整版本树
        List<StandardQuestion> allVersions = standardQuestionRepository.findCompleteVersionTreeById(questionId);
        logger.debug("使用递归查询获取的版本树节点数量: {}", allVersions.size());
        
        if (allVersions.isEmpty()) {
            logger.warn("递归查询版本树未找到结果 - 问题ID: {}", questionId);
            return new ArrayList<>();
        }
        
        // 构建版本树DTO
        List<QuestionHistoryDTO> versionTree = new ArrayList<>();
        
        for (StandardQuestion question : allVersions) {
            QuestionHistoryDTO node = convertToHistoryDTO(question);
            versionTree.add(node);
            
            // 额外记录日志，确保父ID正确设置
            if (question.getParentStandardQuestion() != null) {
                logger.debug("版本树节点父关系 - 问题ID: {}, 父ID: {}", 
                    question.getId(), question.getParentStandardQuestion().getId());
            }
        }
        
        logger.info("成功通过递归SQL查询获取问题版本树 - 问题ID: {}, 版本数量: {}", 
            questionId, versionTree.size());
        
        return versionTree;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionHistoryDTO> getVersionTree(Long questionId) {
        logger.debug("开始获取问题版本树 - 问题ID: {}", questionId);
        
        try {
            // 先尝试使用递归SQL查询方法
            List<QuestionHistoryDTO> recursiveResult = getVersionTreeWithRecursiveQuery(questionId);
            if (!recursiveResult.isEmpty()) {
                logger.info("使用递归SQL查询成功获取版本树 - 问题ID: {}, 版本数量: {}", 
                    questionId, recursiveResult.size());
                    
                // 记录每个版本的详细信息
                for (QuestionHistoryDTO version : recursiveResult) {
                    logger.info("版本节点 - ID: {}, 父ID: {}, 创建时间: {}, 变更日志ID: {}", 
                        version.getId(), 
                        version.getParentQuestionId(), 
                        version.getCreationTime(),
                        version.getChangeLogId());
                }
                
                return recursiveResult;
            }
        } catch (Exception e) {
            logger.warn("递归SQL查询版本树失败，回退到原始方法 - 问题ID: {}, 错误: {}", 
                questionId, e.getMessage());
        }
        
        // 如果递归查询失败，回退到原始方法
        logger.info("回退到原始版本树查询方法 - 问题ID: {}", questionId);
        
        // 以下是原始实现的代码
        // 先获取指定的问题
        StandardQuestion currentQuestion = standardQuestionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("问题不存在"));
        
        // 获取所有标准问题
        List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
        logger.debug("数据库中所有标准问题数量: {}", allQuestions.size());
        
        // 记录所有标准问题的父子关系
        logger.debug("记录所有问题的父子关系:");
        for (StandardQuestion q : allQuestions) {
            if (q.getParentStandardQuestion() != null) {
                logger.debug("问题ID: {}, 父问题ID: {}", q.getId(), q.getParentStandardQuestion().getId());
            } else {
                logger.debug("问题ID: {}, 没有父问题", q.getId());
            }
        }
        
        // 构建问题ID到问题的映射，方便查找
        Map<Long, StandardQuestion> questionMap = new HashMap<>();
        for (StandardQuestion question : allQuestions) {
            questionMap.put(question.getId(), question);
        }
        
        // 构建父ID到子问题列表的映射
        Map<Long, List<StandardQuestion>> parentToChildrenMap = new HashMap<>();
        for (StandardQuestion question : allQuestions) {
            if (question.getParentStandardQuestion() != null && question.getParentStandardQuestion().getId() != null) {
                Long parentId = question.getParentStandardQuestion().getId();
                if (!parentToChildrenMap.containsKey(parentId)) {
                    parentToChildrenMap.put(parentId, new ArrayList<>());
                }
                parentToChildrenMap.get(parentId).add(question);
            }
        }
        
        // 找到根问题
        StandardQuestion rootQuestion = findRootQuestion(currentQuestion);
        logger.debug("找到根问题 - 根问题ID: {}, 当前问题ID: {}", rootQuestion.getId(), questionId);
        
        // 构建版本树
        List<QuestionHistoryDTO> versionTree = new ArrayList<>();
        
        // 从根问题开始，使用广度优先搜索构建完整版本树
        Queue<StandardQuestion> queue = new LinkedList<>();
        Set<Long> visitedIds = new HashSet<>();
        queue.add(rootQuestion);
        visitedIds.add(rootQuestion.getId());
        
        while (!queue.isEmpty()) {
            StandardQuestion question = queue.poll();
            QuestionHistoryDTO node = convertToHistoryDTO(question);
            versionTree.add(node);
            
            // 添加子问题到队列
            List<StandardQuestion> children = parentToChildrenMap.getOrDefault(question.getId(), new ArrayList<>());
            logger.debug("问题ID: {} 的子问题数量: {}", question.getId(), children.size());
            for (StandardQuestion child : children) {
                logger.debug("子问题ID: {}, 父问题ID: {}", child.getId(), question.getId());
                if (!visitedIds.contains(child.getId())) {
                    queue.add(child);
                    visitedIds.add(child.getId());
                }
            }
        }
        
        // 记录详细的版本树信息
        logger.info("成功获取问题版本树 - 问题ID: {}, 版本数量: {}, 根问题ID: {}", 
            questionId, versionTree.size(), rootQuestion.getId());
        
        // 记录每个版本的详细信息
        for (QuestionHistoryDTO version : versionTree) {
            logger.info("版本节点 - ID: {}, 父ID: {}, 创建时间: {}, 变更日志ID: {}", 
                version.getId(), 
                version.getParentQuestionId(), 
                version.getCreationTime(),
                version.getChangeLogId());
        }
        
        return versionTree;
    }

    @Override
    public Map<String, Object> findQuestionsWithStandardAnswers(Boolean onlyLatest, Boolean onlyLatestVersion, Pageable pageable) {
        logger.debug("开始查找有标准回答的问题 - 仅最新版本: {}, 仅最新标准问题: {}, 页码: {}, 每页大小: {}", 
            onlyLatest, onlyLatestVersion, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 获取所有标准问题
            List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
            
            // 过滤出有标准回答的问题
            List<StandardQuestion> questionsWithAnswers = allQuestions.stream()
                .filter(question -> {
                    // 检查不同类型问题的标准回答
                    if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        return standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                        return standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                        return standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    }
                    return false;  // 未知类型，默认视为没有回答
                })
                .collect(Collectors.toList());
            
            // 如果需要过滤出叶子节点（没有子问题的节点）
            if (Boolean.TRUE.equals(onlyLatest)) {
                logger.debug("过滤出最新版本（叶子节点）标准问题");
                
                // 创建一个辅助Set用于快速检查问题是否有子问题
                Set<Long> questionsWithChildren = new HashSet<>();
                for (StandardQuestion question : allQuestions) {
                    if (question != null && question.getParentStandardQuestion() != null) {
                        questionsWithChildren.add(question.getParentStandardQuestion().getId());
                    }
                }
                
                // 过滤出没有子问题的节点（叶子节点）
                questionsWithAnswers = questionsWithAnswers.stream()
                    .filter(q -> q != null && q.getId() != null && !questionsWithChildren.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新版本有标准回答问题数量: {}", questionsWithAnswers.size());
            }
            
            // 如果需要只显示最新的标准问题（没有子版本的问题）
            if (Boolean.TRUE.equals(onlyLatestVersion)) {
                logger.debug("过滤出最新标准问题（没有子版本的问题）");
                
                // 获取所有最新版本的标准问题
                List<StandardQuestion> latestVersionQuestions = standardQuestionRepository.findAllLatestVersions();
                
                // 过滤出同时满足"有标准答案"和"是最新版本"的问题
                Set<Long> latestVersionIds = latestVersionQuestions.stream()
                    .map(StandardQuestion::getId)
                    .collect(Collectors.toSet());
                
                questionsWithAnswers = questionsWithAnswers.stream()
                    .filter(q -> latestVersionIds.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新标准问题数量: {}", questionsWithAnswers.size());
            }
            
            // 去重处理
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            for (StandardQuestion question : questionsWithAnswers) {
                uniqueQuestionsMap.put(question.getId(), question);
            }
            questionsWithAnswers = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 分页处理
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questionsWithAnswers.size());
            
            List<StandardQuestion> pagedQuestions;
            if (start <= end && start < questionsWithAnswers.size()) {
                pagedQuestions = questionsWithAnswers.subList(start, end);
            } else {
                pagedQuestions = new ArrayList<>();
            }
            
            // 转换为DTO
            List<Map<String, Object>> questionDTOs = pagedQuestions.stream()
                .map(this::convertToDTOWithTags)
                .collect(Collectors.toList());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questions", questionDTOs);
            result.put("total", questionsWithAnswers.size());
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) questionsWithAnswers.size() / pageable.getPageSize()));
            result.put("onlyLatest", onlyLatest);  // 添加过滤条件信息
            
            logger.info("成功获取有标准回答问题 - 仅最新版本: {}, 总数: {}, 当前页: {}", 
                onlyLatest, questionsWithAnswers.size(), questionDTOs.size());
            
            return result;
        } catch (Exception e) {
            logger.error("查找有标准回答问题时发生错误", e);
            throw new RuntimeException("查找有标准回答问题时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> findQuestionsByDataset(Long datasetId, Boolean isInDataset, Boolean onlyLatest, 
                                                 Boolean onlyLatestVersion, Boolean onlyWithStandardAnswers,
                                                 List<String> tags, String keyword, Pageable pageable) {
        logger.debug("开始查找基于数据集的问题 - 数据集ID: {}, 是否数据集内: {}, 仅最新版本: {}, 仅最新标准问题: {}, 仅有标准答案: {}, 标签: {}, 关键词: {}", 
            datasetId, isInDataset, onlyLatest, onlyLatestVersion, onlyWithStandardAnswers, tags, keyword);
        
        try {
            // 获取所有标准问题
            List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
            
            // 根据是否只需要有标准答案的问题进行筛选
            List<StandardQuestion> filteredByAnswers;
            if (Boolean.TRUE.equals(onlyWithStandardAnswers)) {
                // 过滤出有标准答案的问题
                filteredByAnswers = allQuestions.stream()
                    .filter(question -> {
                        // 检查不同类型问题的标准回答
                        if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                            question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                            return standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                        } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                            return standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                        } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                            return standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                        }
                        return false;  // 未知类型，默认视为没有回答
                    })
                    .collect(Collectors.toList());
                
                logger.debug("有标准答案的问题数量: {}", filteredByAnswers.size());
            } else {
                // 不过滤，使用所有问题
                filteredByAnswers = allQuestions;
                logger.debug("不过滤标准答案，使用所有问题: {}", filteredByAnswers.size());
            }
            
            // 获取数据集内的问题ID列表
            List<Long> datasetQuestionIds = datasetQuestionMappingRepository.findByDatasetVersionId(datasetId)
                .stream()
                .map(mapping -> mapping.getStandardQuestion().getId())
                .collect(Collectors.toList());
            
            logger.debug("数据集 {} 内的问题数量: {}", datasetId, datasetQuestionIds.size());
            
            // 根据 isInDataset 参数筛选问题
            List<StandardQuestion> filteredQuestions;
            if (Boolean.TRUE.equals(isInDataset)) {
                // 筛选数据集内的问题
                filteredQuestions = filteredByAnswers.stream()
                    .filter(q -> datasetQuestionIds.contains(q.getId()))
                    .collect(Collectors.toList());
                logger.debug("筛选后数据集内的问题数量: {}", filteredQuestions.size());
            } else {
                // 筛选数据集外的问题
                filteredQuestions = filteredByAnswers.stream()
                    .filter(q -> !datasetQuestionIds.contains(q.getId()))
                    .collect(Collectors.toList());
                logger.debug("筛选后数据集外的问题数量: {}", filteredQuestions.size());
            }
            
            // 如果需要过滤出叶子节点（没有子问题的节点）
            if (Boolean.TRUE.equals(onlyLatest)) {
                logger.debug("过滤出最新版本（叶子节点）标准问题");
                
                // 创建一个辅助Set用于快速检查问题是否有子问题
                Set<Long> questionsWithChildren = new HashSet<>();
                for (StandardQuestion question : allQuestions) {
                    if (question != null && question.getParentStandardQuestion() != null) {
                        questionsWithChildren.add(question.getParentStandardQuestion().getId());
                    }
                }
                
                // 过滤出没有子问题的节点（叶子节点）
                filteredQuestions = filteredQuestions.stream()
                    .filter(q -> q != null && q.getId() != null && !questionsWithChildren.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新版本问题数量: {}", filteredQuestions.size());
            }
            
            // 如果需要只显示最新的标准问题（没有子版本的问题）
            if (Boolean.TRUE.equals(onlyLatestVersion)) {
                logger.debug("过滤出最新标准问题（没有子版本的问题）");
                
                // 获取所有最新版本的标准问题
                List<StandardQuestion> latestVersionQuestions = standardQuestionRepository.findAllLatestVersions();
                
                // 过滤出是最新版本的问题
                Set<Long> latestVersionIds = latestVersionQuestions.stream()
                    .map(StandardQuestion::getId)
                    .collect(Collectors.toSet());
                
                filteredQuestions = filteredQuestions.stream()
                    .filter(q -> latestVersionIds.contains(q.getId()))
                    .collect(Collectors.toList());
                
                logger.debug("过滤后的最新标准问题数量: {}", filteredQuestions.size());
            }
            
            // 如果有标签筛选条件
            if (tags != null && !tags.isEmpty()) {
                logger.debug("按标签筛选问题 - 标签: {}", tags);
                
                // 过滤出包含所有指定标签的问题
                filteredQuestions = filteredQuestions.stream()
                    .filter(question -> {
                        // 获取问题标签名称
                        List<String> questionTagNames = question.getQuestionTags().stream()
                            .map(tag -> tag.getTag().getTagName().toLowerCase())
                            .collect(Collectors.toList());
                        
                        // 检查问题是否包含所有指定标签
                        return tags.stream()
                            .map(String::toLowerCase)
                            .allMatch(questionTagNames::contains);
                    })
                    .collect(Collectors.toList());
                
                logger.debug("标签筛选后的问题数量: {}", filteredQuestions.size());
            }
            
            // 如果有关键词筛选条件
            if (keyword != null && !keyword.trim().isEmpty()) {
                logger.debug("按关键词筛选问题 - 关键词: {}", keyword);
                
                // 按关键词筛选问题
                String lowerKeyword = keyword.toLowerCase();
                filteredQuestions = filteredQuestions.stream()
                    .filter(q -> q.getQuestionText() != null && 
                                q.getQuestionText().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
                
                logger.debug("关键词筛选后的问题数量: {}", filteredQuestions.size());
            }
            
            // 去重处理
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            for (StandardQuestion question : filteredQuestions) {
                uniqueQuestionsMap.put(question.getId(), question);
            }
            filteredQuestions = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 分页处理
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredQuestions.size());
            
            List<StandardQuestion> pagedQuestions;
            if (start <= end && start < filteredQuestions.size()) {
                pagedQuestions = filteredQuestions.subList(start, end);
            } else {
                pagedQuestions = new ArrayList<>();
            }
            
            // 转换为DTO
            List<Map<String, Object>> questionDTOs = pagedQuestions.stream()
                .map(this::convertToDTOWithTags)
                .collect(Collectors.toList());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questions", questionDTOs);
            result.put("total", filteredQuestions.size());
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) filteredQuestions.size() / pageable.getPageSize()));
            result.put("datasetId", datasetId);
            result.put("isInDataset", isInDataset);
            result.put("onlyLatest", onlyLatest);
            result.put("onlyLatestVersion", onlyLatestVersion);
            result.put("onlyWithStandardAnswers", onlyWithStandardAnswers);
            
            logger.info("成功获取基于数据集的问题 - 数据集ID: {}, 是否数据集内: {}, 仅有标准答案: {}, 总数: {}, 当前页: {}", 
                datasetId, isInDataset, onlyWithStandardAnswers, filteredQuestions.size(), questionDTOs.size());
            
            return result;
        } catch (Exception e) {
            logger.error("查找基于数据集的问题时发生错误", e);
            throw new RuntimeException("查找基于数据集的问题时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> deleteStandardQuestion(Long questionId, Long userId, boolean permanent) {
        logger.info("开始删除标准问题 - ID: {}, 用户ID: {}, 永久删除: {}", questionId, userId, permanent);
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 检查问题是否存在
            Optional<StandardQuestion> questionOpt = standardQuestionRepository.findById(questionId);
            if (!questionOpt.isPresent()) {
                logger.warn("标准问题不存在 - ID: {}", questionId);
                result.put("success", false);
                result.put("message", "标准问题不存在");
                return result;
            }

            StandardQuestion question = questionOpt.get();
            
            // 2. 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                logger.warn("用户不存在 - ID: {}", userId);
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();

            // 3. 检查是否有子问题
            boolean hasChildQuestions = standardQuestionRepository.existsByParentStandardQuestionId(questionId);
            if (hasChildQuestions) {
                logger.warn("无法删除标准问题，存在子问题 - ID: {}", questionId);
                result.put("success", false);
                result.put("message", "无法删除标准问题，存在子问题版本");
                return result;
            }

            // 4. 检查是否有关联的数据集映射
            boolean hasDatasetMappings = datasetQuestionMappingRepository.existsByStandardQuestionId(questionId);
            if (hasDatasetMappings) {
                logger.warn("标准问题已关联到数据集 - ID: {}", questionId);
                // 这里可以选择阻止删除或者继续删除
                // 我们选择提示但仍然允许删除
                result.put("warning", "此问题已关联到数据集，删除可能影响数据集完整性");
            }

            // 5. 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.DELETE_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage("删除标准问题: " + question.getQuestionText());
            changeLog.setAssociatedStandardQuestion(question);
            
            // 保存变更日志
            changeLog = changeLogRepository.save(changeLog);
            Long changeLogId = changeLog.getId();

            // 6. 执行删除操作
            if (permanent) {
                // 永久删除前，先删除关联的标签关系
                standardQuestionTagRepository.deleteByStandardQuestionId(questionId);
                
                // 删除关联的标准答案
                if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                    question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    standardObjectiveAnswerRepository.deleteByStandardQuestionId(questionId);
                } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                    standardSimpleAnswerRepository.deleteByStandardQuestionId(questionId);
                } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                    standardSubjectiveAnswerRepository.deleteByStandardQuestionId(questionId);
                }
                
                // 永久删除问题
                standardQuestionRepository.deleteById(questionId);
                logger.info("永久删除标准问题 - ID: {}", questionId);
            } else {
                // 逻辑删除
                standardQuestionRepository.softDelete(questionId);
                logger.info("逻辑删除标准问题 - ID: {}", questionId);
            }

            // 7. 构建返回结果
            result.put("success", true);
            result.put("message", permanent ? "标准问题已永久删除" : "标准问题已删除");
            result.put("questionId", questionId);
            result.put("changeLogId", changeLogId);
            
            return result;
        } catch (Exception e) {
            logger.error("删除标准问题时发生错误 - ID: {}", questionId, e);
            result.put("success", false);
            result.put("message", "删除标准问题时发生错误: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> searchQuestions(List<String> tags, String keyword, Long userId, Boolean onlyLatest, Boolean onlyWithStandardAnswers, Pageable pageable) {
        logger.info("搜索标准问题 - 标签: {}, 关键词: {}, 用户ID: {}, 仅最新版本: {}, 仅有标准答案: {}", 
            tags, keyword, userId, onlyLatest, onlyWithStandardAnswers);
        
        // 1. 根据条件获取问题列表
        List<StandardQuestion> questions = new ArrayList<>();
        
        // 如果同时有标签和关键词，先按标签过滤，再按关键词过滤
        if (tags != null && !tags.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            // 获取包含所有指定标签的问题
            List<StandardQuestion> tagFilteredQuestions = getQuestionsByTags(tags);
            
            // 在标签过滤结果中进一步按关键词过滤
            for (StandardQuestion question : tagFilteredQuestions) {
                if (question != null && question.getQuestionText() != null && 
                    question.getQuestionText().toLowerCase().contains(keyword.toLowerCase())) {
                    questions.add(question);
                }
            }
        }
        // 只有标签
        else if (tags != null && !tags.isEmpty()) {
            questions = getQuestionsByTags(tags);
        }
        // 只有关键词
        else if (keyword != null && !keyword.trim().isEmpty()) {
            // 注意：不要再添加百分号，因为Repository实现中已经添加了
            questions = standardQuestionRepository.findByQuestionTextContaining(keyword);
        }
        // 没有搜索条件，返回所有问题或最新版本
        else {
            // 先获取所有问题
            questions = standardQuestionRepository.findAll();
            
            // 如果需要最新版本，后续会过滤
        }
        
        // 如果需要过滤出有标准答案的问题
        if (Boolean.TRUE.equals(onlyWithStandardAnswers)) {
            logger.debug("过滤出有标准答案的问题");
            
            questions = questions.stream()
                .filter(question -> {
                    // 检查不同类型问题的标准回答
                    if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        return standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                        return standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                        return standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    }
                    return false;  // 未知类型，默认视为没有回答
                })
                .collect(Collectors.toList());
            
            logger.debug("有标准答案的问题数量: {}", questions.size());
        }
        
        // 如果需要过滤出叶子节点（没有子问题的节点）
        if (Boolean.TRUE.equals(onlyLatest)) {
            // 创建一个辅助Set用于快速检查问题是否有子问题
            Set<Long> questionsWithChildren = new HashSet<>();
            for (StandardQuestion question : standardQuestionRepository.findAll()) {
                if (question != null && question.getParentStandardQuestion() != null) {
                    questionsWithChildren.add(question.getParentStandardQuestion().getId());
                }
            }
            
            // 过滤出没有子问题的节点（叶子节点）
            questions = questions.stream()
                .filter(q -> q != null && q.getId() != null && !questionsWithChildren.contains(q.getId()))
                .collect(Collectors.toList());
        }
        
        // 去重处理，确保每个问题ID只出现一次
        Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
        for (StandardQuestion question : questions) {
            if (question != null && question.getId() != null) {
                uniqueQuestionsMap.put(question.getId(), question);
            }
        }
        questions = new ArrayList<>(uniqueQuestionsMap.values());
        
        // 2. 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), questions.size());
        
        List<StandardQuestion> pagedQuestions;
        if (start <= end && start < questions.size()) {
            pagedQuestions = questions.subList(start, end);
        } else {
            pagedQuestions = new ArrayList<>();
        }
        
        // 3. 转换为DTO并添加额外信息
        List<Map<String, Object>> questionDTOs = new ArrayList<>();
        
        for (StandardQuestion question : pagedQuestions) {
            Map<String, Object> questionDTO = new HashMap<>();
            
            // 基本信息
            questionDTO.put("id", question.getId());
            questionDTO.put("questionText", question.getQuestionText());
            questionDTO.put("questionType", question.getQuestionType());
            questionDTO.put("difficulty", question.getDifficulty());
            questionDTO.put("creationTime", question.getCreationTime());
            
            // 标签信息 - 添加空值检查
            List<String> questionTags = new ArrayList<>();
            if (question.getQuestionTags() != null) {
                questionTags = question.getQuestionTags().stream()
                        .filter(tag -> tag != null && tag.getTag() != null && tag.getTag().getTagName() != null)
                        .map(tag -> tag.getTag().getTagName())
                        .collect(Collectors.toList());
            }
            questionDTO.put("tags", questionTags);
            
            // 检查是否有标准回答 - 添加问题类型的空值检查
            boolean hasStandardAnswer = false;
            
            if (question.getQuestionType() != null) {
                if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    hasStandardAnswer = standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                    hasStandardAnswer = standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                    hasStandardAnswer = standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                }
            }
            
            questionDTO.put("hasStandardAnswer", hasStandardAnswer);
            
            // 如果提供了用户ID，检查用户是否已回答
            if (userId != null) {
                // 检查是否有众包回答
                boolean hasCrowdsourcedAnswer = crowdsourcedAnswerRepository.existsByStandardQuestionIdAndUserIdAndTaskBatchId(
                        question.getId(), userId, null);
                questionDTO.put("hasCrowdsourcedAnswer", hasCrowdsourcedAnswer);
                
                // 检查是否有专家回答
                boolean hasExpertAnswer = expertCandidateAnswerRepository.findByStandardQuestionIdAndUserId(
                        question.getId(), userId).isPresent();
                questionDTO.put("hasExpertAnswer", hasExpertAnswer);
            } else {
                questionDTO.put("hasCrowdsourcedAnswer", false);
                questionDTO.put("hasExpertAnswer", false);
            }
            
            // 添加父子关系信息
            if (question.getParentStandardQuestion() != null) {
                questionDTO.put("parentQuestionId", question.getParentStandardQuestion().getId());
            }
            
            // 添加叶子节点标识 - 检查是否有以此ID为父ID的问题
            boolean isLeafNode = true;
            for (StandardQuestion potentialChild : standardQuestionRepository.findAll()) {
                if (potentialChild.getParentStandardQuestion() != null && 
                    potentialChild.getParentStandardQuestion().getId() != null && 
                    potentialChild.getParentStandardQuestion().getId().equals(question.getId())) {
                    isLeafNode = false;
                    break;
                }
            }
            questionDTO.put("isLatestVersion", isLeafNode);
            
            questionDTOs.add(questionDTO);
        }
        
        // 4. 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("questions", questionDTOs);
        result.put("total", questions.size());
        result.put("page", pageable.getPageNumber());
        result.put("size", pageable.getPageSize());
        result.put("totalPages", (int) Math.ceil((double) questions.size() / pageable.getPageSize()));
        
        // 添加筛选条件信息
        result.put("onlyLatest", onlyLatest);
        result.put("onlyWithStandardAnswers", onlyWithStandardAnswers);
        
        return result;
    }

    /**
     * 保存问题时清除相关缓存
     */
    @CacheEvict(value = {"query_results", "question_tags"}, allEntries = true)
    public StandardQuestion save(StandardQuestion question) {
        // 清除相关的查询缓存
        cacheUtils.evictCacheByPattern("questions_by_tags*");
        cacheUtils.evictCacheByPattern("questions_with_answers*");
        
        return standardQuestionRepository.save(question);
    }
} 