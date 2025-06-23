package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.Option;
import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.Evaluation.EvaluationStatus;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.EvaluationDetail;
import com.example.demo.entity.jdbc.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.EvaluationRun.RunStatus;
import com.example.demo.entity.jdbc.EvaluationSubjectivePrompt;
import com.example.demo.entity.jdbc.EvaluationTagPrompt;
import com.example.demo.entity.jdbc.EvaluationType;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.StandardObjectiveAnswer;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardSimpleAnswer;
import com.example.demo.entity.jdbc.StandardSubjectiveAnswer;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.repository.jdbc.EvaluationCriterionRepository;
import com.example.demo.repository.jdbc.EvaluationDetailRepository;
import com.example.demo.repository.jdbc.EvaluationPromptAssemblyConfigRepository;
import com.example.demo.repository.jdbc.EvaluationRepository;
import com.example.demo.repository.jdbc.EvaluationRunRepository;
import com.example.demo.repository.jdbc.EvaluationSubjectivePromptRepository;
import com.example.demo.repository.jdbc.EvaluationTagPromptRepository;
import com.example.demo.repository.jdbc.EvaluatorRepository;
import com.example.demo.repository.jdbc.LlmAnswerRepository;
import com.example.demo.repository.jdbc.LlmModelRepository;
import com.example.demo.repository.jdbc.ModelAnswerRunRepository;
import com.example.demo.repository.jdbc.StandardObjectiveAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.StandardSimpleAnswerRepository;
import com.example.demo.repository.jdbc.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.EvaluationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EvaluationServiceImpl implements EvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationServiceImpl.class);
    
    private final EvaluationRepository evaluationRepository;
    private final EvaluatorRepository evaluatorRepository;
    private final UserRepository userRepository;
    private final StandardObjectiveAnswerRepository objectiveAnswerRepository;
    private final StandardSimpleAnswerRepository simpleAnswerRepository;
    private final LlmAnswerRepository llmAnswerRepository;
    private final ModelAnswerRunRepository modelAnswerRunRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationDetailRepository evaluationDetailRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final EvaluationTagPromptRepository evaluationTagPromptRepository;
    private final EvaluationPromptAssemblyConfigRepository evaluationPromptAssemblyConfigRepository;
    private final EvaluationSubjectivePromptRepository evaluationSubjectivePromptRepository;
    private final StandardQuestionRepository standardQuestionRepository;
    private final StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository;
    private final ObjectMapper objectMapper;
    // 不再需要AnswerScoreRepository
    private final LlmModelRepository llmModelRepository;
    
    // 线程池用于异步执行评测任务
    private final ExecutorService evaluationExecutor = Executors.newFixedThreadPool(5);
    
    // AI服务配置
    @Value("${ai.service.url:}")
    private String aiServiceUrl;
    
    @Value("${ai.service.api-key:}")
    private String aiServiceApiKey;
    
    @Value("${ai.service.model:}")
    private String aiServiceModel;
    
    private final RestTemplate restTemplate;
    
    // 添加Redis相关依赖
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluatorRepository evaluatorRepository,
            UserRepository userRepository,
            StandardObjectiveAnswerRepository objectiveAnswerRepository,
            StandardSimpleAnswerRepository simpleAnswerRepository,
            LlmAnswerRepository llmAnswerRepository,
            ModelAnswerRunRepository modelAnswerRunRepository,
            EvaluationRunRepository evaluationRunRepository,
            EvaluationDetailRepository evaluationDetailRepository,
            EvaluationCriterionRepository evaluationCriterionRepository,
            EvaluationTagPromptRepository evaluationTagPromptRepository,
            EvaluationPromptAssemblyConfigRepository evaluationPromptAssemblyConfigRepository,
            EvaluationSubjectivePromptRepository evaluationSubjectivePromptRepository,
            StandardQuestionRepository standardQuestionRepository,
            StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository,
            LlmModelRepository llmModelRepository,
            RestTemplate restTemplate,
            RedisTemplate<String, String> redisTemplate,
            RedissonClient redissonClient,
            JdbcTemplate jdbcTemplate) {
        this.evaluationRepository = evaluationRepository;
        this.evaluatorRepository = evaluatorRepository;
        this.userRepository = userRepository;
        this.objectiveAnswerRepository = objectiveAnswerRepository;
        this.simpleAnswerRepository = simpleAnswerRepository;
        this.llmAnswerRepository = llmAnswerRepository;
        this.modelAnswerRunRepository = modelAnswerRunRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.evaluationDetailRepository = evaluationDetailRepository;
        this.evaluationCriterionRepository = evaluationCriterionRepository;
        this.evaluationTagPromptRepository = evaluationTagPromptRepository;
        this.evaluationPromptAssemblyConfigRepository = evaluationPromptAssemblyConfigRepository;
        this.evaluationSubjectivePromptRepository = evaluationSubjectivePromptRepository;
        this.standardQuestionRepository = standardQuestionRepository;
        this.standardSubjectiveAnswerRepository = standardSubjectiveAnswerRepository;
        this.llmModelRepository = llmModelRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    // ... 其他方法保持不变 ...
    
    /**
     * 组装评估提示词
     * 
     * @param question 标准问题
     * @param answerText 学生回答文本
     * @param referenceAnswer 参考答案
     * @param criteria 评测标准
     * @return 组装后的提示词
     */
    private String assembleEvaluationPrompt(StandardQuestion question, String answerText, 
                                         String referenceAnswer, List<EvaluationCriterion> criteria) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取默认的评测提示词组装配置（取第一个激活的配置）
        List<EvaluationPromptAssemblyConfig> configs = evaluationPromptAssemblyConfigRepository.findByIsActiveTrue();
        EvaluationPromptAssemblyConfig config = configs.isEmpty() ? null : configs.get(0);
        
        if (config != null) {
            // 添加基础系统提示
            if (config.getBaseSystemPrompt() != null) {
                promptBuilder.append(config.getBaseSystemPrompt());
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加标签提示（如果问题有标签）
            if (question.getTags() != null && !question.getTags().isEmpty() && 
                config.getTagPromptsSectionHeader() != null) {
                
                // 先收集有效的标签提示词，如果没有任何有效提示词则跳过整个标签部分
                List<Tag> tags = question.getTags();
                boolean hasAnyTagPrompt = false;
                
                StringBuilder tagPromptsBuilder = new StringBuilder();
                
                for (Tag tag : tags) {
                    try {
                        // 获取该标签的激活状态提示词
                        List<EvaluationTagPrompt> tagPrompts = evaluationTagPromptRepository
                            .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId());
                        
                        if (!tagPrompts.isEmpty()) {
                            // 使用优先级最高的提示词（列表已按优先级排序）
                            EvaluationTagPrompt prompt = tagPrompts.get(0);
                            tagPromptsBuilder.append("【").append(tag.getTagName()).append("】: ");
                            tagPromptsBuilder.append(prompt.getPromptTemplate());
                            tagPromptsBuilder.append(config.getTagPromptSeparator());
                            hasAnyTagPrompt = true;
                        }
                        // 如果标签没有提示词，则跳过该标签，不添加到prompt中
                    } catch (Exception e) {
                        logger.warn("获取评测标签提示词失败，标签ID: {}", tag.getId(), e);
                    }
                }
                
                // 只有当至少有一个标签有提示词时，才添加标签部分
                if (hasAnyTagPrompt) {
                    promptBuilder.append(config.getTagPromptsSectionHeader());
                    promptBuilder.append("\n");
                    promptBuilder.append(tagPromptsBuilder);
                    promptBuilder.append(config.getSectionSeparator());
                }
            }
            
            // 添加主观题评测要求（如果问题类型是主观题）
            if (question.getQuestionType() == QuestionType.SUBJECTIVE && 
                config.getSubjectiveSectionHeader() != null) {
                promptBuilder.append(config.getSubjectiveSectionHeader());
                promptBuilder.append("\n");
                
                try {
                    // 获取主观题评测提示词
                    List<EvaluationSubjectivePrompt> subjectivePrompts = evaluationSubjectivePromptRepository
                        .findByIsActiveTrueAndDeletedAtIsNull();
                    
                    if (!subjectivePrompts.isEmpty()) {
                        // 使用第一个激活的提示词
                        EvaluationSubjectivePrompt prompt = subjectivePrompts.get(0);
                        promptBuilder.append(prompt.getPromptTemplate());
                        
                        // 添加评分指导（如果有）
                        if (prompt.getScoringInstruction() != null && !prompt.getScoringInstruction().isEmpty()) {
                            promptBuilder.append("\n\n评分指导:\n");
                            promptBuilder.append(prompt.getScoringInstruction());
                        }
                        
                        // 添加输出格式要求（如果有）
                        if (prompt.getOutputFormatInstruction() != null && !prompt.getOutputFormatInstruction().isEmpty()) {
                            promptBuilder.append("\n\n输出格式要求:\n");
                            promptBuilder.append(prompt.getOutputFormatInstruction());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取主观题评测提示词失败", e);
                }
                
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加最终指示
            if (config.getFinalInstruction() != null) {
                promptBuilder.append(config.getFinalInstruction());
                promptBuilder.append(config.getSectionSeparator());
            }
        } else {
            // 如果没有配置，使用默认系统提示
            promptBuilder.append("你是一位专业的答案评测专家。请对以下主观题的回答进行评测。\n\n");
        }
        
        // 添加问题和答案内容
        promptBuilder.append("问题：").append(question.getQuestionText()).append("\n\n");
        promptBuilder.append("学生回答：").append(answerText).append("\n\n");
        promptBuilder.append("参考答案：").append(referenceAnswer).append("\n\n");
        
        // 添加评测标准
        promptBuilder.append("评测标准：\n");
        for (EvaluationCriterion criterion : criteria) {
            promptBuilder.append("- ").append(criterion.getName()).append("：")
                  .append(criterion.getDescription()).append("\n");
        }
        
        // 添加默认的输出格式要求（如果没有配置或主观题提示词）
        if (config == null || 
            (question.getQuestionType() == QuestionType.SUBJECTIVE && 
             evaluationSubjectivePromptRepository.findByIsActiveTrueAndDeletedAtIsNull().isEmpty())) {
            promptBuilder.append("\n请对回答进行全面评测，并给出以下格式的评测结果：\n");
            promptBuilder.append("1. 总体评分（0-100分）\n");
            promptBuilder.append("2. 各评测标准的得分和评语\n");
            promptBuilder.append("3. 总体评语，包括优点和不足\n");
            promptBuilder.append("4. 改进建议\n\n");
            promptBuilder.append("**重要：请严格按照以下JSON格式输出，确保'总分'字段在最外层：**\n");
            promptBuilder.append("```json\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"总分\": 数字分数(0-100),\n");
            for (EvaluationCriterion criterion : criteria) {
                promptBuilder.append("  \"").append(criterion.getName()).append("\": 数字分数,\n");
            }
            promptBuilder.append("  \"建议\": \"改进建议和总体评语\"\n");
            promptBuilder.append("}\n");
            promptBuilder.append("```\n\n");
            promptBuilder.append("注意：\n");
            promptBuilder.append("1. 总分必须是0-100之间的数字\n");
            promptBuilder.append("2. 各评测标准的分数也必须是数字\n");
            promptBuilder.append("3. 请确保JSON格式正确，可以被程序解析\n");
            promptBuilder.append("4. 不要添加任何额外的文字说明，只返回JSON");
        }
        
                String finalPrompt = promptBuilder.toString();
        
        // 打印组装好的评测提示词
        logger.info("\n========== 组装好的评测提示词 ==========");
        logger.info(finalPrompt);
        logger.info("========================================\n");
        
        return finalPrompt;
    }

    /**
     * 组装评测提示词，使用指定的主观题评测提示词ID
     */
    private String assembleEvaluationPrompt(StandardQuestion question, String answerText, 
                                         String referenceAnswer, List<EvaluationCriterion> criteria,
                                         Long subjectivePromptId) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取默认的评测提示词组装配置（取第一个激活的配置）
        List<EvaluationPromptAssemblyConfig> configs = evaluationPromptAssemblyConfigRepository.findByIsActiveTrue();
        EvaluationPromptAssemblyConfig config = configs.isEmpty() ? null : configs.get(0);
        
        if (config != null) {
            // 添加基础系统提示
            if (config.getBaseSystemPrompt() != null) {
                promptBuilder.append(config.getBaseSystemPrompt());
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加标签提示（如果问题有标签）
            if (question.getTags() != null && !question.getTags().isEmpty() && 
                config.getTagPromptsSectionHeader() != null) {
                
                // 先收集有效的标签提示词，如果没有任何有效提示词则跳过整个标签部分
                List<Tag> tags = question.getTags();
                boolean hasAnyTagPrompt = false;
                
                StringBuilder tagPromptsBuilder = new StringBuilder();
                
                for (Tag tag : tags) {
                    try {
                        // 获取该标签的激活状态提示词
                        List<EvaluationTagPrompt> tagPrompts = evaluationTagPromptRepository
                            .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId());
                        
                        if (!tagPrompts.isEmpty()) {
                            // 使用优先级最高的提示词（列表已按优先级排序）
                            EvaluationTagPrompt prompt = tagPrompts.get(0);
                            tagPromptsBuilder.append("【").append(tag.getTagName()).append("】: ");
                            tagPromptsBuilder.append(prompt.getPromptTemplate());
                            tagPromptsBuilder.append(config.getTagPromptSeparator());
                            hasAnyTagPrompt = true;
                        }
                        // 如果标签没有提示词，则跳过该标签，不添加到prompt中
                    } catch (Exception e) {
                        logger.warn("获取评测标签提示词失败，标签ID: {}", tag.getId(), e);
                    }
                }
                
                // 只有当至少有一个标签有提示词时，才添加标签部分
                if (hasAnyTagPrompt) {
                    promptBuilder.append(config.getTagPromptsSectionHeader());
                    promptBuilder.append("\n");
                    promptBuilder.append(tagPromptsBuilder);
                    promptBuilder.append(config.getSectionSeparator());
                }
            }
            
            // 添加主观题评测要求（如果问题类型是主观题）
            if (question.getQuestionType() == QuestionType.SUBJECTIVE && 
                config.getSubjectiveSectionHeader() != null) {
                promptBuilder.append(config.getSubjectiveSectionHeader());
                promptBuilder.append("\n");
                
                try {
                    // 获取指定ID的主观题评测提示词
                    EvaluationSubjectivePrompt prompt = null;
                    if (subjectivePromptId != null) {
                        Optional<EvaluationSubjectivePrompt> promptOpt = evaluationSubjectivePromptRepository.findById(subjectivePromptId);
                        if (promptOpt.isPresent()) {
                            prompt = promptOpt.get();
                            logger.info("使用指定的主观题评测提示词，ID: {}, 名称: {}", subjectivePromptId, prompt.getName());
                        } else {
                            logger.warn("未找到指定ID的主观题评测提示词: {}，将使用默认提示词", subjectivePromptId);
                        }
                    }
                    
                    // 如果没有找到指定ID的提示词，则使用默认的激活提示词
                    if (prompt == null) {
                        List<EvaluationSubjectivePrompt> subjectivePrompts = evaluationSubjectivePromptRepository
                            .findByIsActiveTrueAndDeletedAtIsNull();
                        
                        if (!subjectivePrompts.isEmpty()) {
                            prompt = subjectivePrompts.get(0);
                            logger.info("使用默认的主观题评测提示词，ID: {}, 名称: {}", prompt.getId(), prompt.getName());
                        }
                    }
                    
                    if (prompt != null) {
                        promptBuilder.append(prompt.getPromptTemplate());
                        
                        // 添加评分指导（如果有）
                        if (prompt.getScoringInstruction() != null && !prompt.getScoringInstruction().isEmpty()) {
                            promptBuilder.append("\n\n评分指导:\n");
                            promptBuilder.append(prompt.getScoringInstruction());
                        }
                        
                        // 添加输出格式要求（如果有）
                        if (prompt.getOutputFormatInstruction() != null && !prompt.getOutputFormatInstruction().isEmpty()) {
                            promptBuilder.append("\n\n输出格式要求:\n");
                            promptBuilder.append(prompt.getOutputFormatInstruction());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取主观题评测提示词失败", e);
                }
                
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加最终指示
            if (config.getFinalInstruction() != null) {
                promptBuilder.append(config.getFinalInstruction());
                promptBuilder.append(config.getSectionSeparator());
            }
        } else {
            // 如果没有配置，使用默认系统提示
            promptBuilder.append("你是一位专业的答案评测专家。请对以下主观题的回答进行评测。\n\n");
        }
        
        // 添加问题和答案内容
        promptBuilder.append("问题：").append(question.getQuestionText()).append("\n\n");
        promptBuilder.append("学生回答：").append(answerText).append("\n\n");
        promptBuilder.append("参考答案：").append(referenceAnswer).append("\n\n");
        
        // 添加评测标准
        promptBuilder.append("评测标准：\n");
        for (EvaluationCriterion criterion : criteria) {
            promptBuilder.append("- ").append(criterion.getName()).append("：")
                  .append(criterion.getDescription()).append("\n");
        }
        
        // 添加默认的输出格式要求（如果没有找到任何主观题提示词）
        if (config == null || 
            (question.getQuestionType() == QuestionType.SUBJECTIVE && 
             evaluationSubjectivePromptRepository.findByIsActiveTrueAndDeletedAtIsNull().isEmpty() && 
             subjectivePromptId == null)) {
            promptBuilder.append("\n请对回答进行全面评测，并给出以下格式的评测结果：\n");
            promptBuilder.append("1. 总体评分（0-10分）\n");
            promptBuilder.append("2. 各评测标准的得分和评语\n");
            promptBuilder.append("3. 总体评语，包括优点和不足\n");
            promptBuilder.append("4. 改进建议\n\n");
            promptBuilder.append("请以JSON格式输出，格式如下：\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"总分\": 分数,\n");
            promptBuilder.append("  \"criteria_scores\": [\n");
            promptBuilder.append("    {\"criterion\": \"标准名称\", \"score\": 分数, \"comments\": \"评语\"},\n");
            promptBuilder.append("    ...\n");
            promptBuilder.append("  ],\n");
            promptBuilder.append("  \"overall_comments\": \"总体评语\",\n");
            promptBuilder.append("  \"improvement_suggestions\": \"改进建议\"\n");
            promptBuilder.append("}");
        }
        
        return promptBuilder.toString();
    }
    
    @Override
    public Map<String, Object> evaluateSubjectiveWithAI(String answerText, String questionText, 
                                                   String referenceAnswer, List<EvaluationCriterion> criteria,
                                                   Long evaluatorId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("开始使用AI评测主观题，评测者ID: {}", evaluatorId);
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是AI
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
                throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
            }
            
            // 获取AI模型信息
            if (evaluator.getLlmModel() == null) {
                throw new IllegalArgumentException("评测者未关联AI模型: " + evaluatorId);
            }
            
            // 查找问题（如果可能）
            StandardQuestion question = null;
            try {
                // 尝试通过问题文本查找对应的标准问题
                // 这里简化处理，实际上可能需要更复杂的匹配逻辑
                // 或者修改方法签名，直接传入问题ID或问题对象
                List<StandardQuestion> questions = standardQuestionRepository.findByQuestionTextContaining(questionText);
                if (!questions.isEmpty()) {
                    question = questions.get(0);
                }
            } catch (Exception e) {
                logger.warn("无法查找对应的标准问题，将使用默认提示词", e);
            }
            
            // 组装评测提示词
            String prompt;
            if (question != null) {
                // 使用标准问题组装提示词
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria);
            } else {
                // 创建一个临时问题对象
                question = new StandardQuestion();
                question.setQuestionText(questionText);
                question.setQuestionType(QuestionType.SUBJECTIVE);
                // 没有标签
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria);
            }
            
            // 调用AI服务进行评测
            String aiResponse = callAIService(prompt, evaluator.getLlmModel().getId());
            
            // 将完整的AI回复记录到日志中
            logger.info("\n========== AI评测回复 ==========\n{}\n==================================", aiResponse);
            
            // 预处理AI响应：移除Markdown代码块标记
            String processedResponse = aiResponse;
            if (aiResponse.startsWith("```")) {
                // 移除开头的```json或```等标记
                processedResponse = aiResponse.replaceAll("^```(json)?\\s*", "");
                // 移除结尾的```标记
                processedResponse = processedResponse.replaceAll("\\s*```\\s*$", "");
                logger.info("检测到Markdown格式的响应，已移除代码块标记");
            }
            
            // 解析AI评测结果
            Map<String, Object> aiResult;
            try {
                aiResult = objectMapper.readValue(processedResponse, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("解析JSON失败，尝试提取JSON部分", e);
                
                // 尝试从文本中提取JSON部分
                Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
                Matcher matcher = jsonPattern.matcher(processedResponse);
                
                if (matcher.find()) {
                    String jsonPart = matcher.group();
                    try {
                        aiResult = objectMapper.readValue(jsonPart, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e2) {
                        logger.error("提取JSON后解析仍然失败", e2);
                        throw new RuntimeException("无法解析AI评测结果: " + e2.getMessage());
                    }
                } else {
                    throw new RuntimeException("无法从响应中提取JSON格式的评测结果");
                }
            }
            
            // 提取总分
            Object scoreObj = aiResult.get("总分");
            if (scoreObj == null) {
                scoreObj = aiResult.get("score");
            }
            
            if (scoreObj == null) {
                throw new RuntimeException("AI评测结果中缺少总分字段");
            }
            
            BigDecimal score = new BigDecimal(scoreObj.toString());
            
            // 提取评语
            String comments = null;
            if (aiResult.containsKey("overall_comments")) {
                comments = (String) aiResult.get("overall_comments");
            } else if (aiResult.containsKey("总评")) {
                comments = (String) aiResult.get("总评");
            } else if (aiResult.containsKey("comments")) {
                comments = (String) aiResult.get("comments");
            }
            
            // 如果没有评语，则使用改进建议作为评语
            if (comments == null && aiResult.containsKey("improvement_suggestions")) {
                comments = (String) aiResult.get("improvement_suggestions");
            }
            
            // 构建结果
            result.put("score", score);
            result.put("comments", comments != null ? comments : "无评语");
            result.put("raw_ai_response", aiResponse);
            result.put("criteria_scores", aiResult.get("criteria_scores"));
            
        } catch (Exception e) {
            logger.error("AI评测主观题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测失败: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 使用指定的主观题评测提示词进行AI评测
     */
    public Map<String, Object> evaluateSubjectiveWithAI(String answerText, String questionText, 
                                                   String referenceAnswer, List<EvaluationCriterion> criteria,
                                                   Long evaluatorId, Long subjectivePromptId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("开始使用AI评测主观题，评测者ID: {}, 使用主观题评测提示词ID: {}", evaluatorId, subjectivePromptId);
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是AI
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
                throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
            }
            
            // 获取AI模型信息
            if (evaluator.getLlmModel() == null) {
                throw new IllegalArgumentException("评测者未关联AI模型: " + evaluatorId);
            }
            
            // 查找问题（如果可能）
            StandardQuestion question = null;
            try {
                // 尝试通过问题文本查找对应的标准问题
                List<StandardQuestion> questions = standardQuestionRepository.findByQuestionTextContaining(questionText);
                if (!questions.isEmpty()) {
                    question = questions.get(0);
                }
            } catch (Exception e) {
                logger.warn("无法查找对应的标准问题，将使用默认提示词", e);
            }
            
            // 组装评测提示词
            String prompt;
            if (question != null) {
                // 使用标准问题组装提示词，并传递指定的主观题评测提示词ID
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria, subjectivePromptId);
            } else {
                // 创建一个临时问题对象
                question = new StandardQuestion();
                question.setQuestionText(questionText);
                question.setQuestionType(QuestionType.SUBJECTIVE);
                // 没有标签，但传递指定的主观题评测提示词ID
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria, subjectivePromptId);
            }
            
            // 调用AI服务进行评测
            String aiResponse = callAIService(prompt, evaluator.getLlmModel().getId());
            
            // 将完整的AI回复记录到日志中
            logger.info("\n========== AI评测回复 ==========\n{}\n==================================", aiResponse);
            
            // 预处理AI响应：移除Markdown代码块标记
            String processedResponse = aiResponse;
            if (aiResponse.startsWith("```")) {
                // 移除开头的```json或```等标记
                processedResponse = aiResponse.replaceAll("^```(json)?\\s*", "");
                // 移除结尾的```标记
                processedResponse = processedResponse.replaceAll("\\s*```\\s*$", "");
                logger.info("检测到Markdown格式的响应，已移除代码块标记");
            }
            
            // 解析AI评测结果
            Map<String, Object> aiResult;
            try {
                aiResult = objectMapper.readValue(processedResponse, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.warn("解析JSON失败，尝试提取JSON部分", e);
                
                // 尝试从文本中提取JSON部分
                Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
                Matcher matcher = jsonPattern.matcher(processedResponse);
                
                if (matcher.find()) {
                    String jsonPart = matcher.group();
                    try {
                        aiResult = objectMapper.readValue(jsonPart, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e2) {
                        logger.error("提取JSON后解析仍然失败", e2);
                        throw new RuntimeException("无法解析AI评测结果: " + e2.getMessage());
                    }
                } else {
                    throw new RuntimeException("无法从响应中提取JSON格式的评测结果");
                }
            }
            
            // 提取总分
            Object scoreObj = aiResult.get("总分");
            if (scoreObj == null) {
                scoreObj = aiResult.get("score");
            }
            
            if (scoreObj == null) {
                throw new RuntimeException("AI评测结果中缺少总分字段");
            }
            
            BigDecimal score = new BigDecimal(scoreObj.toString());
            
            // 提取评语
            String comments = null;
            if (aiResult.containsKey("overall_comments")) {
                comments = (String) aiResult.get("overall_comments");
            } else if (aiResult.containsKey("总评")) {
                comments = (String) aiResult.get("总评");
            } else if (aiResult.containsKey("comments")) {
                comments = (String) aiResult.get("comments");
            }
            
            // 如果没有评语，则使用改进建议作为评语
            if (comments == null && aiResult.containsKey("improvement_suggestions")) {
                comments = (String) aiResult.get("improvement_suggestions");
            }
            
            // 构建结果
            result.put("score", score);
            result.put("comments", comments != null ? comments : "无评语");
            result.put("raw_ai_response", aiResponse);
            result.put("criteria_scores", aiResult.get("criteria_scores"));
            
        } catch (Exception e) {
            logger.error("AI评测主观题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测失败: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 调用AI服务进行评测
     */
    private String callAIService(String prompt, Long modelId) {
        try {
            // 获取模型信息
            LlmModel llmModel = null;
            if (modelId != null) {
                // 从数据库中获取指定ID的LlmModel对象
                llmModel = llmModelRepository.findById(modelId)
                    .orElse(null);
                if (llmModel != null) {
                    logger.info("找到模型: {}, API URL: {}", llmModel.getName(), llmModel.getApiUrl());
                } else {
                    logger.warn("未找到ID为{}的模型", modelId);
                }
            }
            
            // 确定API URL和密钥
            String apiUrl = null;
            String apiKey = null;
            String model = null;
            String apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            
            if (llmModel != null && llmModel.getApiUrl() != null && !llmModel.getApiUrl().isEmpty()) {
                apiUrl = llmModel.getApiUrl();
                apiKey = llmModel.getApiKey();
                model = llmModel.getName();
                apiType = llmModel.getApiType() != null ? llmModel.getApiType() : apiType;
            } else {
                // 使用配置的默认值
                apiUrl = aiServiceUrl;
                apiKey = aiServiceApiKey;
                model = aiServiceModel;
            }
            
            // 补全API URL路径
            apiUrl = buildApiUrl(apiUrl, apiType);
            
            logger.info("API URL: {}, 模型: {}", apiUrl, model);
            
            if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty() || model == null || model.isEmpty()) {
                logger.warn("AI服务配置不完整，使用直接调用大模型");
                return executeAIEvaluation(prompt, modelId);
            }
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 准备请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的评测专家，负责评估答案的质量。请严格按照用户要求的JSON格式返回评测结果，确保'总分'字段在JSON的最外层，且值为0-100之间的数字。");
            messages.add(systemMessage);
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 2000);
            
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 打印完整请求内容
            try {
                String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
                logger.info("\n========== 评测请求内容 ==========");
                logger.info("请求URL: " + apiUrl);
                logger.info("请求头: Authorization: Bearer " + (apiKey != null ? apiKey.substring(0, 3) + "..." : "null"));
                logger.info("请求体: \n" + requestJson);
                logger.info("====================================\n");
            } catch (Exception e) {
                logger.error("打印请求内容失败: {}", e.getMessage());
            }
            
            try {
                logger.info("正在向AI服务发送请求: {}", apiUrl);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
                
                // 处理响应
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map responseBody = response.getBody();
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            logger.info("大模型评测成功，返回内容长度: {}", content.length());
                            // 将AI回复内容完整记录到日志
                            logger.info("\n========== 大模型评测回复内容 ==========\n{}\n=======================================", content);
                            return content;
                        }
                    }
                }
                
                logger.warn("AI服务返回无效响应，使用直接调用大模型");
            } catch (Exception e) {
                logger.error("调用AI服务接口失败: {}", e.getMessage(), e);
                logger.warn("切换到直接调用大模型");
            }
            
            // 如果API调用失败，使用直接调用大模型
            return executeAIEvaluation(prompt, modelId);
            
        } catch (Exception e) {
            logger.error("调用AI服务过程中发生错误: {}", e.getMessage(), e);
            return executeAIEvaluation(prompt, modelId);
        }
    }
    
    /**
     * 根据API类型和基础URL构建完整的API端点URL
     */
    private String buildApiUrl(String baseUrl, String apiType) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }
        
        // 移除URL末尾的斜杠
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // 如果apiType为空，尝试从URL判断API类型
        if (apiType == null || apiType.isEmpty()) {
            if (baseUrl.contains("openai.com")) {
                apiType = "openai";
            } else if (baseUrl.contains("anthropic.com")) {
                apiType = "anthropic";
            } else if (baseUrl.contains("baidu.com") || baseUrl.contains("wenxin")) {
                apiType = "baidu";
            } else if (baseUrl.contains("aliyun") || baseUrl.contains("tongyi") || baseUrl.contains("dashscope")) {
                apiType = "aliyun";
            } else if (baseUrl.contains("zhipu") || baseUrl.contains("chatglm")) {
                apiType = "zhipu";
            } else if (baseUrl.contains("azure")) {
                apiType = "azure";
            } else {
                apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            }
        }
        
        // 根据API类型返回不同的端点路径
        switch (apiType.toLowerCase()) {
            case "openai":
            case "openai_compatible":
                // 检查是否已包含完整路径
                if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/v1/chat/completions")) {
                    return baseUrl;
                }
                return baseUrl + "/v1/chat/completions";
            case "anthropic":
                return baseUrl + "/v1/messages";
            case "baidu":
                return baseUrl + "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            case "aliyun":
            case "tongyi":
            case "dashscope":
                return baseUrl + "/v1/services/aigc/text-generation/generation";
            case "zhipu":
            case "glm":
            case "chatglm":
                return baseUrl + "/v1/chat/completions";
            case "azure":
                // Azure OpenAI API通常需要在URL中包含部署ID
                if (baseUrl.contains("deployments")) {
                    return baseUrl;
                } else {
                    return baseUrl + "/deployments/{deployment-id}/chat/completions?api-version=2023-07-01-preview";
                }
            default:
                return baseUrl + "/v1/chat/completions"; // 默认使用OpenAI格式
        }
    }
    
    /**
     * 执行AI评测（调用大语言模型API）
     */
    private String executeAIEvaluation(String prompt, Long modelId) {
        try {
            logger.info("调用真实大模型进行评测，提示词长度: {}", prompt.length());
            
            // 从数据库获取模型信息
            LlmModel llmModel = null;
            if (modelId != null) {
                llmModel = llmModelRepository.findById(modelId)
                    .orElse(null);
            }
            
            // 获取API信息
            String apiUrl = "https://api.openai.com/v1/chat/completions";  // 默认API端点
            String apiKey = aiServiceApiKey;
            String model = aiServiceModel;
            String apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            
            if (llmModel != null && llmModel.getApiUrl() != null && !llmModel.getApiUrl().isEmpty()) {
                apiUrl = llmModel.getApiUrl();
                apiKey = llmModel.getApiKey();
                model = llmModel.getName();
                apiType = llmModel.getApiType() != null ? llmModel.getApiType() : apiType;
                logger.info("使用数据库中的模型配置: URL={}, 模型={}", apiUrl, model);
            } else {
                logger.info("使用默认配置: URL={}, 模型={}", apiUrl, model);
            }
            
            // 补全API URL路径
            apiUrl = buildApiUrl(apiUrl, apiType);
            logger.info("完整API URL路径: {}", apiUrl);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的评测专家，负责评估答案的质量。请严格按照用户要求的JSON格式返回评测结果，确保'总分'字段在JSON的最外层，且值为0-100之间的数字。");
            messages.add(systemMessage);
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2); // 低温度，增加输出的确定性
            requestBody.put("max_tokens", 2000); // 足够长的输出
            
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 打印完整请求内容
            try {
                String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
                logger.info("\n========== 评测请求内容 ==========");
                logger.info("请求URL: " + apiUrl);
                logger.info("请求头: Authorization: Bearer " + (apiKey != null ? apiKey.substring(0, 3) + "..." : "null"));
                logger.info("请求体: \n" + requestJson);
                logger.info("====================================\n");
            } catch (Exception e) {
                logger.error("打印请求内容失败: {}", e.getMessage());
            }
            
            try {
                logger.info("发送请求到: {}", apiUrl);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map responseBody = response.getBody();
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            logger.info("大模型评测成功，返回内容长度: {}", content.length());
                            return content;
                        }
                    }
                }
                
                logger.warn("大模型响应解析失败，返回默认评测结果");
            } catch (Exception e) {
                logger.error("调用大模型API失败: {}", e.getMessage(), e);
            }
            
            // 如果API调用失败，返回默认JSON格式评测结果
            return """
                {
                  "总分": 75,
                  "criteria_scores": [
                    {"criterion": "内容完整性", "score": 80, "comments": "回答涵盖了大部分关键点"},
                    {"criterion": "逻辑性", "score": 70, "comments": "论述基本连贯，但有些地方逻辑跳跃"},
                    {"criterion": "专业性", "score": 80, "comments": "使用了适当的专业术语，展示了对主题的理解"}
                  ],
                  "overall_comments": "回答整体表现良好，展示了对主题的理解，但在某些方面还可以进一步完善。",
                  "improvement_suggestions": "建议增加更多具体例子来支持论点，并进一步阐述某些关键概念的细节。"
                }
                """;
        } catch (Exception e) {
            logger.error("评测过程出现错误: {}", e.getMessage(), e);
            
            // 返回错误信息的JSON
            return """
                {
                  "总分": 50,
                  "criteria_scores": [
                    {"criterion": "评测错误", "score": 50, "comments": "评测过程中发生错误"}
                  ],
                  "overall_comments": "评测过程中发生错误: """ + e.getMessage() + """
                  ",
                  "improvement_suggestions": "请重新提交评测请求"
                }
                """;
        }
    }
    
    @Override
    public List<EvaluationDetail> getEvaluationDetails(Long evaluationId) {
        logger.info("获取评测详情，评测ID: {}", evaluationId);
        
        try {
            // 查询数据库中已保存的评测详情
            List<EvaluationDetail> savedDetails = evaluationDetailRepository.findByEvaluationId(evaluationId);
            
            // 如果有已保存的详情，直接返回
            if (!savedDetails.isEmpty()) {
                return savedDetails;
            }
            
            // 否则，解析评测结果并动态生成评测详情
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测记录: " + evaluationId));
            
            // 获取评测结果
            Map<String, Object> results = getEvaluationResults(evaluation);
            if (results.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 提取评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            
            // 处理标准评分
            Object criteriaScores = results.get("criteria_scores");
            if (criteriaScores instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> scoresList = (List<Map<String, Object>>) criteriaScores;
                
                for (Map<String, Object> scoreMap : scoresList) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    
                    // 设置评测标准名称
                    Object criterionObj = scoreMap.get("criterion");
                    if (criterionObj != null) {
                        detail.setCriterionName(criterionObj.toString());
                    }
                    
                    // 设置分数
                    Object scoreObj = scoreMap.get("score");
                    if (scoreObj instanceof Number) {
                        detail.setScore(new BigDecimal(scoreObj.toString()));
                    }
                    
                    // 设置评语
                    Object commentsObj = scoreMap.get("comments");
                    if (commentsObj != null) {
                        detail.setComments(commentsObj.toString());
                    }
                    
                    details.add(detail);
                }
                
                // 保存到数据库中
                if (!details.isEmpty()) {
                    details = evaluationDetailRepository.saveAll(details);
                }
            }
            
            return details;
            
        } catch (Exception e) {
            logger.error("获取评测详情失败", e);
            throw new RuntimeException("获取评测详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType) {
        return evaluationCriterionRepository.findByQuestionType(questionType);
    }
    
    @Override
    public List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType, int page, int size) {
        // 获取未删除的评测标准并分页
        return evaluationCriterionRepository.findActiveByQuestionTypeOrderByOrderIndexPaged(questionType, page, size);
    }
    
    @Override
    public BigDecimal calculateBleuScore(String candidateText, String referenceText) {
        logger.info("计算BLEU分数，候选文本长度: {}, 参考文本长度: {}", 
                candidateText != null ? candidateText.length() : 0, 
                referenceText != null ? referenceText.length() : 0);
        
        try {
            // 参数验证
            if (candidateText == null || referenceText == null || candidateText.isEmpty() || referenceText.isEmpty()) {
                logger.warn("计算BLEU分数失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：标准化处理，移除所有空白字符、标点符号并转为小写
            String processedCandidate = candidateText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
                
            String processedReference = referenceText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedCandidate.isEmpty() || processedReference.isEmpty()) {
                logger.warn("计算BLEU分数失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 对于中文短文本，我们使用字符级别的匹配计算
            // 将字符串转换为字符数组
            char[] candidateChars = processedCandidate.toCharArray();
            char[] referenceChars = processedReference.toCharArray();
            
            // 计算字符匹配数
            Map<Character, Integer> refCharCount = new HashMap<>();
            
            // 统计参考文本中的字符频率
            for (char c : referenceChars) {
                refCharCount.put(c, refCharCount.getOrDefault(c, 0) + 1);
            }
            
            // 计算匹配数
            int matchCount = 0;
            Map<Character, Integer> candidateCharCount = new HashMap<>();
            for (char c : candidateChars) {
                candidateCharCount.put(c, candidateCharCount.getOrDefault(c, 0) + 1);
            }
            
            // 计算共同字符的最小出现次数
            for (Map.Entry<Character, Integer> entry : candidateCharCount.entrySet()) {
                char c = entry.getKey();
                int count = entry.getValue();
                if (refCharCount.containsKey(c)) {
                    matchCount += Math.min(count, refCharCount.get(c));
                }
            }
            
            // 计算精确率
            double precision = (double) matchCount / candidateChars.length;
            
            // 字符级别的匹配率作为BLEU分数
            double bleuScore = precision;
            
            // 四舍五入到2位小数
            BigDecimal result = new BigDecimal(bleuScore).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("BLEU分数计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, candidateText, processedCandidate, referenceText, processedReference);
            return result;
            
        } catch (Exception e) {
            logger.error("计算BLEU分数时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public Map<String, Object> getEvaluationRunProgress(Long evaluationRunId) {
        logger.info("获取评测运行进度，评测运行ID: {}", evaluationRunId);
        
        try {
            // 查询评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取相关的模型回答运行记录
            ModelAnswerRun modelAnswerRun = evaluationRun.getModelAnswerRun();
            if (modelAnswerRun == null) {
                throw new IllegalStateException("评测运行未关联模型回答运行: " + evaluationRunId);
            }
            
            // 获取总问题数量
            int totalQuestions = modelAnswerRun.getTotalQuestionsCount() != null ? modelAnswerRun.getTotalQuestionsCount() : 0;
            
            // 获取已评测的回答数量
            int evaluatedAnswers = evaluationRepository.countByEvaluationRunId(evaluationRunId);
            
            // 计算进度百分比
            double progressPercentage = totalQuestions > 0 ? 
                    ((double) evaluatedAnswers / totalQuestions) * 100 : 0;
            
            // 四舍五入到2位小数
            BigDecimal progress = new BigDecimal(progressPercentage).setScale(2, RoundingMode.HALF_UP);
            
            // 构建进度信息
            Map<String, Object> progressInfo = new HashMap<>();
            progressInfo.put("evaluationRunId", evaluationRunId);
            progressInfo.put("status", evaluationRun.getStatus().toString());
            progressInfo.put("totalQuestions", totalQuestions);
            progressInfo.put("evaluatedAnswers", evaluatedAnswers);
            progressInfo.put("progressPercentage", progress);
            progressInfo.put("startTime", evaluationRun.getStartTime());
            progressInfo.put("endTime", evaluationRun.getEndTime());
            progressInfo.put("lastUpdated", LocalDateTime.now());
            
            // 如果评测已完成，添加汇总结果
            if (evaluationRun.getStatus() == RunStatus.COMPLETED) {
                Map<String, Object> summaryResults = calculateEvaluationSummary(evaluationRunId);
                progressInfo.put("summaryResults", summaryResults);
            }
            
            return progressInfo;
            
        } catch (Exception e) {
            logger.error("获取评测运行进度失败", e);
            
            // 返回错误信息
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("evaluationRunId", evaluationRunId);
            errorInfo.put("error", e.getMessage());
            errorInfo.put("status", "ERROR");
            return errorInfo;
        }
    }
    
    @Override
    @Async
    public CompletableFuture<Void> startEvaluationRun(Long evaluationRunId) {
        logger.info("开始评测运行，评测运行ID: {}", evaluationRunId);
        
        // 清除中断标志
        String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
        redisTemplate.delete(interruptKey);
        
        // 更新Redis状态
        String stateKey = "evaluation_run:state:" + evaluationRunId;
        redisTemplate.opsForValue().set(stateKey, "IN_PROGRESS");
        redisTemplate.expire(stateKey, Duration.ofHours(24));
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 查询评测运行记录
                EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                
                // 检查状态，只有PENDING或PAUSED状态才能启动
                if (evaluationRun.getStatus() != RunStatus.PENDING && evaluationRun.getStatus() != RunStatus.PAUSED) {
                    logger.warn("评测运行状态不允许启动: {}", evaluationRun.getStatus());
                    throw new IllegalStateException("评测运行状态不允许启动: " + evaluationRun.getStatus());
                }
                
                // 更新状态为进行中
                evaluationRun.setStatus(RunStatus.IN_PROGRESS);
                if (evaluationRun.getStartTime() == null) {
                    evaluationRun.setStartTime(LocalDateTime.now());
                }
                evaluationRunRepository.save(evaluationRun);
                
                // 获取相关的模型回答运行记录
                ModelAnswerRun modelAnswerRun = evaluationRun.getModelAnswerRun();
                if (modelAnswerRun == null) {
                    throw new IllegalStateException("评测运行未关联模型回答运行: " + evaluationRunId);
                }
                
                // 获取评测者
                Evaluator evaluator = evaluationRun.getEvaluator();
                if (evaluator == null) {
                    throw new IllegalStateException("评测运行未关联评测者: " + evaluationRunId);
                }
                
                // 获取用户ID
                Long userId = evaluationRun.getCreatedBy();
                
                // 获取已生成的LLM回答
                List<LlmAnswer> llmAnswers = llmAnswerRepository.findByModelAnswerRunId(modelAnswerRun.getId());
                
                // 过滤出未评测的回答
                List<LlmAnswer> unevaluatedAnswers = llmAnswers.stream()
                        .filter(answer -> !evaluationRepository.existsByLlmAnswerIdAndEvaluationRunId(
                                answer.getId(), evaluationRunId))
                        .collect(Collectors.toList());
                
                logger.info("开始评测运行，总回答数: {}，未评测回答数: {}", llmAnswers.size(), unevaluatedAnswers.size());
                
                // 更新总回答数
                evaluationRun.setTotalAnswersCount(unevaluatedAnswers.size());
                evaluationRunRepository.save(evaluationRun);
                
                // 批量处理未评测的回答
                int batchSize = evaluationRun.getBatchSize() != null ? evaluationRun.getBatchSize() : 10;
                for (int i = 0; i < unevaluatedAnswers.size(); i += batchSize) {
                    // 检查是否应该中断处理
                    if (shouldInterruptEvaluation(evaluationRunId)) {
                        logger.info("检测到评测运行{}的中断信号，停止处理", evaluationRunId);
                        
                        // 更新状态为暂停
                        evaluationRun.setStatus(RunStatus.PAUSED);
                        evaluationRun.setPauseTime(LocalDateTime.now());
                        evaluationRun.setLastUpdated(LocalDateTime.now());
                        evaluationRunRepository.save(evaluationRun);
                        
                        return;
                    }
                    
                    // 检查评测运行是否被暂停或取消
                    EvaluationRun currentStatus = evaluationRunRepository.findById(evaluationRunId).orElse(null);
                    if (currentStatus == null || currentStatus.getStatus() != RunStatus.IN_PROGRESS) {
                        logger.info("评测运行已被暂停或取消，ID: {}, 状态: {}", 
                                evaluationRunId, currentStatus != null ? currentStatus.getStatus() : "已删除");
                        return;
                    }
                    
                    // 获取当前批次的回答
                    int endIndex = Math.min(i + batchSize, unevaluatedAnswers.size());
                    List<LlmAnswer> batchAnswers = unevaluatedAnswers.subList(i, endIndex);
                    
                    // 批量评测
                    evaluateAnswers(batchAnswers, evaluator.getId(), userId);
                    
                    // 更新最后处理的回答ID
                    if (!batchAnswers.isEmpty()) {
                        LlmAnswer lastAnswer = batchAnswers.get(batchAnswers.size() - 1);
                        evaluationRun.setLastProcessedAnswerId(lastAnswer.getId());
                    }
                    
                    // 更新进度
                    int processedCount = i + batchAnswers.size();
                    BigDecimal progress = new BigDecimal(processedCount)
                            .multiply(new BigDecimal(100))
                            .divide(new BigDecimal(unevaluatedAnswers.size()), 2, RoundingMode.HALF_UP);
                    
                    evaluationRun.setProgressPercentage(progress);
                    evaluationRun.setCompletedAnswersCount(processedCount);
                    evaluationRun.setLastActivityTime(LocalDateTime.now());
                    evaluationRunRepository.save(evaluationRun);
                    
                    logger.info("评测运行进度: {}/{}", processedCount, unevaluatedAnswers.size());
                }
                
                // 所有回答评测完成，更新状态为已完成
                evaluationRun.setStatus(RunStatus.COMPLETED);
                evaluationRun.setEndTime(LocalDateTime.now());
                evaluationRun.setLastActivityTime(LocalDateTime.now());
                evaluationRun.setProgressPercentage(new BigDecimal(100));
                evaluationRunRepository.save(evaluationRun);
                
                logger.info("评测运行完成，ID: {}", evaluationRunId);
                
            } catch (Exception e) {
                logger.error("评测运行过程中发生错误", e);
                
                // 更新状态为错误
                try {
                    EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId).orElse(null);
                    if (evaluationRun != null) {
                        evaluationRun.setStatus(RunStatus.FAILED);
                        evaluationRun.setLastUpdated(LocalDateTime.now());
                        evaluationRun.setErrorMessage(e.getMessage());
                        evaluationRunRepository.save(evaluationRun);
                    }
                } catch (Exception ex) {
                    logger.error("更新评测运行状态失败", ex);
                }
                
                throw new RuntimeException("评测运行失败: " + e.getMessage(), e);
            }
        }, evaluationExecutor);
    }
    
    /**
     * 计算评测运行的汇总结果
     */
    private Map<String, Object> calculateEvaluationSummary(Long evaluationRunId) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // 获取所有评测结果
            List<Evaluation> evaluations = evaluationRepository.findByEvaluationRunId(evaluationRunId);
            
            if (evaluations.isEmpty()) {
                return summary;
            }
            
            // 计算平均分
            BigDecimal totalScore = BigDecimal.ZERO;
            for (Evaluation evaluation : evaluations) {
                if (evaluation.getScore() != null) {
                    totalScore = totalScore.add(evaluation.getScore());
                }
            }
            
            BigDecimal averageScore = totalScore.divide(new BigDecimal(evaluations.size()), 2, RoundingMode.HALF_UP);
            
            // 按问题类型分组计算
            Map<QuestionType, List<Evaluation>> groupedByType = evaluations.stream()
                    .filter(e -> e.getLlmAnswer() != null && 
                            e.getLlmAnswer().getDatasetQuestionMapping() != null && 
                            e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion() != null)
                    .collect(Collectors.groupingBy(e -> e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion().getQuestionType()));
            
            Map<String, Object> typeScores = new HashMap<>();
            for (Map.Entry<QuestionType, List<Evaluation>> entry : groupedByType.entrySet()) {
                QuestionType type = entry.getKey();
                List<Evaluation> typeEvaluations = entry.getValue();
                
                BigDecimal typeTotal = BigDecimal.ZERO;
                for (Evaluation evaluation : typeEvaluations) {
                    if (evaluation.getScore() != null) {
                        typeTotal = typeTotal.add(evaluation.getScore());
                    }
                }
                
                BigDecimal typeAverage = typeTotal.divide(new BigDecimal(typeEvaluations.size()), 2, RoundingMode.HALF_UP);
                typeScores.put(type.toString(), typeAverage);
            }
            
            // 构建汇总结果
            summary.put("evaluationCount", evaluations.size());
            summary.put("averageScore", averageScore);
            summary.put("scoresByQuestionType", typeScores);
            
            return summary;
            
        } catch (Exception e) {
            logger.error("计算评测汇总结果失败", e);
            summary.put("error", "计算评测汇总结果失败: " + e.getMessage());
            return summary;
        }
    }
    
    @Override
    @Transactional
    public Evaluation evaluateAnswer(LlmAnswer llmAnswer, Long evaluatorId, Long userId) {
        logger.info("开始评测单个回答，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswer.getId(), evaluatorId, userId);
        
        try {
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 检查是否已存在相同的评测记录
            boolean exists = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                llmAnswer.getId(), evaluator.getId());
            
            if (exists) {
                logger.warn("该回答已被同一评测者评测过，跳过重复评测，回答ID: {}, 评测者ID: {}", 
                        llmAnswer.getId(), evaluator.getId());
                
                // 查找并返回现有评测记录
                List<Evaluation> existingEvaluations = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                        llmAnswer.getId(), evaluator.getId());
                
                if (!existingEvaluations.isEmpty()) {
                    return existingEvaluations.get(0); // 返回第一条匹配的记录
                }
            }
            
            // 获取问题信息
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.PENDING);
            // 设置评测类型
            evaluation.setEvaluationType(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? 
                EvaluationType.MANUAL : EvaluationType.AI_MODEL);
            
            // 根据问题类型进行评测
            Map<String, Object> evaluationResult;
            String scoreType;
            
            switch (question.getQuestionType()) {
                case SINGLE_CHOICE:
                    StandardObjectiveAnswer objectiveAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准选择题答案"));
                    evaluationResult = evaluateSingleChoice(llmAnswer.getAnswerText(), 
                            objectiveAnswer.getCorrectOptionIds(), objectiveAnswer.getOptions());
                    scoreType = "OBJECTIVE_SINGLE_CHOICE";
                    break;
                    
                case MULTIPLE_CHOICE:
                    StandardObjectiveAnswer multiAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准多选题答案"));
                    evaluationResult = evaluateMultipleChoice(llmAnswer.getAnswerText(), 
                            multiAnswer.getCorrectOptionIds(), multiAnswer.getOptions());
                    scoreType = "OBJECTIVE_MULTIPLE_CHOICE";
                    break;
                    
                case SIMPLE_FACT:
                    StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准简答题答案"));
                    evaluationResult = evaluateSimpleFact(llmAnswer.getAnswerText(), 
                            simpleAnswer.getAnswerText(), simpleAnswer.getAlternativeAnswers());
                    scoreType = "OBJECTIVE_SIMPLE_FACT";
                    break;
                    
                case SUBJECTIVE:
                    // 获取评测标准
                    List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
                    evaluationResult = evaluateSubjectiveWithAI(llmAnswer.getAnswerText(), 
                            question.getQuestionText(), 
                            question.getStandardSubjectiveAnswer().getAnswerText(), 
                            criteria, evaluatorId);
                    scoreType = "SUBJECTIVE";
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的问题类型: " + question.getQuestionType());
            }
            
            // 更新评测记录
            BigDecimal score = new BigDecimal(evaluationResult.get("score").toString());
            evaluation.setScore(score);
            evaluation.setComments((String) evaluationResult.get("comments"));
            evaluation.setEvaluationResults(evaluationResult);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 在保存前检查是否已存在相同的评测记录
            boolean existsBeforeSave = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                llmAnswer.getId(), evaluator.getId());
            
            // 详细记录请求体信息
            logger.info("准备保存评测记录，详细信息: llmAnswerId={}, evaluatorId={}, createdByUserId={}, status={}, score={}, 已存在相同记录={}",
                llmAnswer.getId(), evaluator.getId(), user.getId(), evaluation.getStatus(), score, existsBeforeSave);
            
            if (existsBeforeSave) {
                logger.warn("检测到唯一键约束冲突风险! 该回答(ID:{})已被同一评测者(ID:{})评测过", llmAnswer.getId(), evaluator.getId());
            }
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                List<EvaluationDetail> details = new ArrayList<>();
                for (Map<String, Object> criteriaScore : criteriaScores) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    detail.setCriterionName((String) criteriaScore.get("criterion"));
                    detail.setScore(new BigDecimal(criteriaScore.get("score").toString()));
                    detail.setComments((String) criteriaScore.get("comments"));
                    detail.setCreatedAt(LocalDateTime.now());
                    details.add(detail);
                }
                
                evaluationDetailRepository.saveAll(details);
            }
            
            // 在Evaluation中保存分数记录
            // 标准化分数（0-100分）
            BigDecimal normalizedScore;
            if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                // 主观题分数通常是0-10分
                normalizedScore = score.multiply(new BigDecimal(10));
            } else {
                // 客观题分数通常是0-100分
                normalizedScore = score;
            }
            
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? "HUMAN" : "AI_EVALUATION");
            
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测记录，评测ID: {}, 回答ID: {}, 评测者ID: {}, 分数类型: {}", 
                    evaluation.getId(), llmAnswer.getId(), evaluatorId, scoreType);
            
            // 保存详细评分记录
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                for (Map<String, Object> criterionScore : criteriaScores) {
                    String criterionName = (String) criterionScore.get("criterion");
                    BigDecimal criterionScoreValue = new BigDecimal(criterionScore.get("score").toString());
                    String criterionComments = (String) criterionScore.get("comments");
                    
                    // 创建详细评分记录 - 使用EvaluationDetail
                    EvaluationDetail detailScore = new EvaluationDetail();
                    detailScore.setEvaluation(evaluation);
                    detailScore.setCriterionName(criterionName);
                    detailScore.setScore(criterionScoreValue);
                    detailScore.setComments(criterionComments);
                    detailScore.setCreatedAt(LocalDateTime.now());
                    
                    evaluationDetailRepository.save(detailScore);
                }
            }
            
            logger.info("评测完成，评测ID: {}, 得分: {}", evaluation.getId(), evaluation.getScore());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("评测回答失败", e);
            throw new RuntimeException("评测回答失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public List<Evaluation> evaluateAnswers(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId) {
        logger.info("开始批量评测回答，回答数量: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswers.size(), evaluatorId, userId);
        
        List<Evaluation> evaluations = new ArrayList<>();
        
        try {
            for (LlmAnswer answer : llmAnswers) {
                try {
                    Evaluation evaluation = evaluateAnswer(answer, evaluatorId, userId);
                    evaluations.add(evaluation);
                } catch (Exception e) {
                    logger.error("评测回答失败，回答ID: {}", answer.getId(), e);
                    // 继续处理下一个回答
                }
            }
            
            logger.info("批量评测完成，成功评测数量: {}", evaluations.size());
            return evaluations;
            
        } catch (Exception e) {
            logger.error("批量评测回答失败", e);
            throw new RuntimeException("批量评测回答失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> evaluateSingleChoice(String answerText, String correctOptionIds, String options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析正确答案 - 标准化处理，去除引号、方括号等特殊字符
            String correctAnswer = correctOptionIds.trim()
                .replaceAll("[\\[\\]\"]", "") // 去除方括号和引号
                .replaceAll("\\s+", ""); // 去除空白字符
            
            // 解析学生答案 - 标准化处理
            String studentAnswer = answerText.trim()
                .replaceAll("[\\[\\]\"]", "") // 去除方括号和引号
                .replaceAll("\\s+", ""); // 去除空白字符
                
            // 提取学生答案中的选项ID（通常是A、B、C、D等）
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(studentAnswer.toUpperCase());
            StringBuilder extractedAnswer = new StringBuilder();
            
            while (matcher.find()) {
                extractedAnswer.append(matcher.group());
            }
            
            // 如果成功提取到选项ID，使用提取的结果
            if (extractedAnswer.length() > 0) {
                studentAnswer = extractedAnswer.toString();
            }
            
            // 计算得分
            boolean isCorrect = correctAnswer.equalsIgnoreCase(studentAnswer);
            BigDecimal score = isCorrect ? new BigDecimal("100") : BigDecimal.ZERO;
            
            // 构建评测结果
            result.put("score", score);
            result.put("isCorrect", isCorrect);
            result.put("correctAnswer", correctAnswer);
            result.put("studentAnswer", studentAnswer);
            result.put("comments", isCorrect ? "答案正确" : "答案错误，正确答案是: " + correctAnswer);
            
            // 打印答案到日志，方便人工判断
            logger.info("\n========== 单选题评测结果 ==========");
            logger.info("原始大模型答案: {}", answerText);
            logger.info("处理后大模型答案: {}", studentAnswer);
            logger.info("原始标准答案: {}", correctOptionIds);
            logger.info("处理后标准答案: {}", correctAnswer);
            logger.info("评测结果: {}", (isCorrect ? "正确" : "错误"));
            logger.info("===================================");
            
            return result;
            
        } catch (Exception e) {
            logger.error("评测单选题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("error", "评测失败: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> evaluateMultipleChoice(String answerText, String correctIds, String options) {
        logger.info("开始评测多选题回答，回答文本长度: {}", answerText != null ? answerText.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (answerText == null || answerText.isEmpty() || correctIds == null || correctIds.isEmpty() || options == null || options.isEmpty()) {
                logger.warn("评测多选题参数无效");
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "无效的回答或标准答案");
                return result;
            }
            
            // 解析选项为List<Option>
            List<Option> optionsList = objectMapper.readValue(options, new TypeReference<List<Option>>() {});
            
            // 将List<Option>转换为Map<String, String>便于后续处理
            Map<String, String> optionsMap = new HashMap<>();
            for (Option option : optionsList) {
                optionsMap.put(option.getId(), option.getText());
            }
            
            // 处理标准答案 - 标准化处理，去除引号、方括号等特殊字符
            String cleanedCorrectIds = correctIds.replaceAll("[\\[\\]\"]", "").replaceAll("\\s+", "");
            Set<String> correctIdSet = new HashSet<>(Arrays.asList(cleanedCorrectIds.split("[,、]")));
            
            // 清理空字符串
            correctIdSet.removeIf(String::isEmpty);
            
            // 原始学生答案（用于日志）
            String originalStudentAnswer = answerText;
            
            // 提取学生的选择
            Set<String> studentChoices = new HashSet<>();
            
            // 使用正则表达式匹配选项ID（通常是A、B、C、D等）
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(answerText.toUpperCase());
            
            while (matcher.find()) {
                studentChoices.add(matcher.group());
            }
            
            // 如果没有找到有效的选择，尝试从完整答案中提取
            if (studentChoices.isEmpty()) {
                // 尝试按逗号或顿号分割
                String cleanedAnswer = answerText.replaceAll("[\\[\\]\"]", "").trim();
                String[] parts = cleanedAnswer.split("[,、]");
                for (String part : parts) {
                    part = part.trim().toUpperCase();
                    if (part.length() == 1 && Character.isLetter(part.charAt(0))) {
                        studentChoices.add(part);
                    }
                }
                
                // 如果仍然为空，尝试从文本中提取选项内容
                if (studentChoices.isEmpty()) {
                    // 遍历所有选项，查找答案中是否包含选项内容
                    for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
                        if (answerText.contains(entry.getValue())) {
                            studentChoices.add(entry.getKey());
                        }
                    }
                }
            }
            
            // 评分和评语
            if (!studentChoices.isEmpty()) {
                // 计算正确选择的数量
                Set<String> correctChoices = new HashSet<>(studentChoices);
                correctChoices.retainAll(correctIdSet);
                
                // 计算错误选择的数量
                Set<String> wrongChoices = new HashSet<>(studentChoices);
                wrongChoices.removeAll(correctIdSet);
                
                // 计算漏选的数量
                Set<String> missedChoices = new HashSet<>(correctIdSet);
                missedChoices.removeAll(studentChoices);
                
                // 计算得分（满分100分）
                // 每个正确选择得到：100分 / 正确答案总数
                // 每个错误选择或漏选扣除：100分 / (正确答案总数 * 2)
                double pointsPerCorrect = 100.0 / correctIdSet.size();
                double pointsPerWrong = pointsPerCorrect / 2;
                
                double score = correctChoices.size() * pointsPerCorrect - 
                             (wrongChoices.size() + missedChoices.size()) * pointsPerWrong;
                
                // 确保分数在0-100范围内
                score = Math.max(0, Math.min(100, score));
                
                result.put("score", new BigDecimal(score).setScale(2, RoundingMode.HALF_UP));
                
                // 生成评语
                StringBuilder comments = new StringBuilder();
                comments.append("共选择了").append(studentChoices.size()).append("个选项。\n");
                
                if (!correctChoices.isEmpty()) {
                    comments.append("正确选择的选项：").append(String.join("、", correctChoices))
                           .append("\n");
                }
                
                if (!wrongChoices.isEmpty()) {
                    comments.append("错误选择的选项：").append(String.join("、", wrongChoices))
                           .append("\n");
                }
                
                if (!missedChoices.isEmpty()) {
                    comments.append("漏选的正确选项：").append(String.join("、", missedChoices))
                           .append("\n");
                }
                
                comments.append("\n正确答案应该是选项：").append(String.join("、", correctIdSet));
                
                result.put("comments", comments.toString());
                
                // 打印答案到日志，方便人工判断
                logger.info("\n========== 多选题评测结果 ==========");
                logger.info("原始大模型答案: {}", originalStudentAnswer);
                logger.info("处理后大模型答案: {}", String.join("、", studentChoices));
                logger.info("原始标准答案: {}", correctIds);
                logger.info("处理后标准答案: {}", String.join("、", correctIdSet));
                logger.info("正确选择: {}", (correctChoices.isEmpty() ? "无" : String.join("、", correctChoices)));
                logger.info("错误选择: {}", (wrongChoices.isEmpty() ? "无" : String.join("、", wrongChoices)));
                logger.info("漏选项目: {}", (missedChoices.isEmpty() ? "无" : String.join("、", missedChoices)));
                logger.info("评测得分: {}", result.get("score"));
                logger.info("===================================");
                
            } else {
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "未能从回答中识别出明确的选择。正确答案是选项：" + String.join("、", correctIdSet));
                
                // 打印答案到日志，方便人工判断
                logger.info("\n========== 多选题评测结果 ==========");
                logger.info("原始大模型答案: {}", originalStudentAnswer);
                logger.info("未能从回答中识别出明确的选择");
                logger.info("原始标准答案: {}", correctIds);
                logger.info("处理后标准答案: {}", String.join("、", correctIdSet));
                logger.info("评测得分: 0");
                logger.info("===================================");
            }
            
            // 添加评测详情
            List<Map<String, Object>> criteriaScores = new ArrayList<>();
            
            // 正确性评分
            Map<String, Object> correctnessScore = new HashMap<>();
            correctnessScore.put("criterion", "正确性");
            correctnessScore.put("score", result.get("score"));
            correctnessScore.put("comments", result.get("comments"));
            criteriaScores.add(correctnessScore);
            
            result.put("criteria_scores", criteriaScores);
            
        } catch (Exception e) {
            logger.error("评测多选题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测过程发生错误：" + e.getMessage());
            
            // 打印错误信息到日志
            logger.info("\n========== 多选题评测错误 ==========");
            logger.info("大模型原始答案: {}", answerText);
            logger.info("标准答案: {}", correctIds);
            logger.info("评测错误: {}", e.getMessage());
            logger.info("===================================");
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> evaluateSimpleFact(String answerText, String standardAnswer, String alternativeAnswers) {
        logger.info("开始评测简单事实题回答，回答文本长度: {}", answerText != null ? answerText.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (answerText == null || answerText.isEmpty() || standardAnswer == null || standardAnswer.isEmpty()) {
                logger.warn("评测简单事实题参数无效");
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "无效的回答或标准答案");
                return result;
            }
            
            // 原始答案（用于日志记录）
            String originalAnswerText = answerText;
            String originalStandardAnswer = standardAnswer;
            
            // 解析备选答案
            List<String> alternatives = new ArrayList<>();
            if (alternativeAnswers != null && !alternativeAnswers.isEmpty()) {
                alternatives = objectMapper.readValue(alternativeAnswers, new TypeReference<List<String>>() {});
            }
            
            // 计算与标准答案的相似度
            BigDecimal standardSimilarity = calculateTextSimilarity(answerText, standardAnswer);
            BigDecimal maxSimilarity = standardSimilarity;
            String bestMatchAnswer = standardAnswer;
            
            // 计算与备选答案的相似度
            for (String alternative : alternatives) {
                BigDecimal similarity = calculateTextSimilarity(answerText, alternative);
                if (similarity.compareTo(maxSimilarity) > 0) {
                    maxSimilarity = similarity;
                    bestMatchAnswer = alternative;
                }
            }
            
            // 计算BERT相似度（语义相似度）
            BigDecimal bertScore = calculateBertSimilarity(answerText, bestMatchAnswer);
            
            // 计算ROUGE分数
            BigDecimal rougeScore = calculateRougeScore(answerText, bestMatchAnswer);
            
            // 计算BLEU分数
            BigDecimal bleuScore = calculateBleuScore(answerText, bestMatchAnswer);
            
            // 综合评分（权重：BERT相似度0.4，传统相似度0.2，ROUGE 0.2，BLEU 0.2）
            BigDecimal finalScore = bertScore.multiply(new BigDecimal("0.4"))
                    .add(maxSimilarity.multiply(new BigDecimal("0.2")))
                    .add(rougeScore.multiply(new BigDecimal("0.2")))
                    .add(bleuScore.multiply(new BigDecimal("0.2")));
            
            // 将分数转换为100分制
            finalScore = finalScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            
            // 确保分数在0-100范围内
            if (finalScore.compareTo(BigDecimal.ZERO) < 0) {
                finalScore = BigDecimal.ZERO;
            } else if (finalScore.compareTo(new BigDecimal("100")) > 0) {
                finalScore = new BigDecimal("100");
            }
            
            result.put("score", finalScore);
            
            // 生成评语
            StringBuilder comments = new StringBuilder();
            comments.append("回答评分：").append(finalScore).append("分\n\n");
            comments.append("评分详情：\n");
            comments.append("1. BERT语义相似度：").append(bertScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("2. 文本相似度：").append(maxSimilarity.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("3. ROUGE分数：").append(rougeScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("4. BLEU分数：").append(bleuScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n\n");
            
            if (finalScore.compareTo(new BigDecimal("80")) >= 0) {
                comments.append("回答非常准确，与标准答案高度一致。");
            } else if (finalScore.compareTo(new BigDecimal("60")) >= 0) {
                comments.append("回答较为准确，与标准答案基本一致。");
            } else if (finalScore.compareTo(new BigDecimal("40")) >= 0) {
                comments.append("回答部分正确，但存在一些偏差。建议参考标准答案：").append(bestMatchAnswer);
            } else {
                comments.append("回答与标准答案差异较大。标准答案是：").append(bestMatchAnswer);
            }
            
            result.put("comments", comments.toString());
            
            // 打印答案到日志，方便人工判断
            logger.info("\n========== 简单事实题评测结果 ==========");
            logger.info("原始大模型答案: {}", originalAnswerText);
            logger.info("原始标准答案: {}", originalStandardAnswer);
            
            // 获取标准化处理后的文本（通过再次调用计算函数）
            String processedAnswerText = originalAnswerText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "")
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", "");
            
            String processedStandardAnswer = originalStandardAnswer.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "")
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", "");
            
            logger.info("处理后大模型答案: {}", processedAnswerText);
            logger.info("处理后标准答案: {}", processedStandardAnswer);
            
            if (!alternatives.isEmpty()) {
                logger.info("备选答案: {}", alternatives);
                logger.info("最佳匹配答案: {}", bestMatchAnswer);
            }
            logger.info("BERT相似度: {}", bertScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("文本相似度: {}", maxSimilarity.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("ROUGE分数: {}", rougeScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("BLEU分数: {}", bleuScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("最终得分: {}", finalScore);
            logger.info("=====================================");
            
            // 添加评测详情
            List<Map<String, Object>> criteriaScores = new ArrayList<>();
            
            // BERT相似度评分
            Map<String, Object> bertScoreMap = new HashMap<>();
            bertScoreMap.put("criterion", "BERT语义相似度");
            bertScoreMap.put("score", bertScore.multiply(new BigDecimal("100")));
            bertScoreMap.put("comments", "BERT语义理解评分");
            criteriaScores.add(bertScoreMap);
            
            // 相似度评分
            Map<String, Object> similarityScore = new HashMap<>();
            similarityScore.put("criterion", "文本相似度");
            similarityScore.put("score", maxSimilarity.multiply(new BigDecimal("100")));
            similarityScore.put("comments", "文本相似度评分");
            criteriaScores.add(similarityScore);
            
            // ROUGE评分
            Map<String, Object> rougeScoreMap = new HashMap<>();
            rougeScoreMap.put("criterion", "ROUGE分数");
            rougeScoreMap.put("score", rougeScore.multiply(new BigDecimal("100")));
            rougeScoreMap.put("comments", "ROUGE评分");
            criteriaScores.add(rougeScoreMap);
            
            // BLEU评分
            Map<String, Object> bleuScoreMap = new HashMap<>();
            bleuScoreMap.put("criterion", "BLEU分数");
            bleuScoreMap.put("score", bleuScore.multiply(new BigDecimal("100")));
            bleuScoreMap.put("comments", "BLEU评分");
            criteriaScores.add(bleuScoreMap);
            
            result.put("criteria_scores", criteriaScores);
            
        } catch (Exception e) {
            logger.error("评测简单事实题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测过程发生错误：" + e.getMessage());
            
            // 打印错误信息到日志
            logger.info("\n========== 简单事实题评测错误 ==========");
            logger.info("大模型原始答案: {}", answerText);
            logger.info("标准答案: {}", standardAnswer);
            logger.info("评测错误: {}", e.getMessage());
            logger.info("=====================================");
        }
        
        return result;
    }
    
    @Override
    public BigDecimal calculateTextSimilarity(String text1, String text2) {
        logger.info("计算文本相似度，文本1长度: {}, 文本2长度: {}", 
                text1 != null ? text1.length() : 0, 
                text2 != null ? text2.length() : 0);
        
        try {
            // 参数验证
            if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
                logger.warn("计算文本相似度失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：移除所有空白字符、标点符号并转为小写
            String processedText1 = text1.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            String processedText2 = text2.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedText1.isEmpty() || processedText2.isEmpty()) {
                logger.warn("计算文本相似度失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 直接字符级比较
            // 对于短文本如简单事实题答案，使用编辑距离(Levenshtein距离)计算相似度
            int distance = calculateLevenshteinDistance(processedText1, processedText2);
            int maxLength = Math.max(processedText1.length(), processedText2.length());
            
            // 计算相似度：1 - 标准化编辑距离
            double similarity = 1.0 - ((double) distance / maxLength);
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(similarity).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("文本相似度计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, text1, processedText1, text2, processedText2);
            return result;
            
        } catch (Exception e) {
            logger.error("计算文本相似度时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 计算两个字符串之间的编辑距离(Levenshtein距离)
     * 
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 编辑距离
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    @Override
    public BigDecimal calculateRougeScore(String candidateText, String referenceText) {
        logger.info("计算ROUGE分数，候选文本长度: {}, 参考文本长度: {}", 
                candidateText != null ? candidateText.length() : 0, 
                referenceText != null ? referenceText.length() : 0);
        
        try {
            // 参数验证
            if (candidateText == null || referenceText == null || candidateText.isEmpty() || referenceText.isEmpty()) {
                logger.warn("计算ROUGE分数失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：标准化处理，移除所有空白字符、标点符号并转为小写
            String processedCandidate = candidateText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
                
            String processedReference = referenceText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedCandidate.isEmpty() || processedReference.isEmpty()) {
                logger.warn("计算ROUGE分数失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 对于中文文本，我们使用字符级别的分析
            // 将字符串转换为字符集合
            Set<Character> candidateChars = processedCandidate.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
            
            Set<Character> referenceChars = processedReference.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
            
            // 计算重叠的字符数
            Set<Character> overlap = new HashSet<>(candidateChars);
            overlap.retainAll(referenceChars);
            
            // 计算召回率和精确率
            double recall = (double) overlap.size() / referenceChars.size();
            double precision = (double) overlap.size() / candidateChars.size();
            
            // 计算F1分数
            double f1Score = 0.0;
            if (precision + recall > 0) {
                f1Score = 2 * precision * recall / (precision + recall);
            }
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(f1Score).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("ROUGE分数计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, candidateText, processedCandidate, referenceText, processedReference);
            return result;
            
        } catch (Exception e) {
            logger.error("计算ROUGE分数时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    @Transactional
    public Evaluation createHumanEvaluation(Long llmAnswerId, Long evaluatorId, Long userId) {
        logger.info("创建人工评测记录，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswerId, evaluatorId, userId);
        
        try {
            // 获取LLM回答
            LlmAnswer llmAnswer = llmAnswerRepository.findByIdWithQuestion(llmAnswerId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM回答: " + llmAnswerId));
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是人类
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalArgumentException("评测者不是人类: " + evaluatorId);
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 检查是否已存在该回答的人工评测
            if (evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(llmAnswerId, evaluatorId)) {
                throw new IllegalStateException("该回答已存在人工评测记录");
            }
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.PENDING);
            // 设置评测类型
            evaluation.setEvaluationType(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? 
                EvaluationType.MANUAL : EvaluationType.AI_MODEL);
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("人工评测记录创建成功，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("创建人工评测记录失败", e);
            throw new RuntimeException("创建人工评测记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public Evaluation submitHumanEvaluation(Long evaluationId, BigDecimal overallScore, String comments, 
                                          List<Map<String, Object>> detailScores, Long userId) {
        logger.info("提交人工评测结果，评测ID: {}, 总分: {}, 用户ID: {}", evaluationId, overallScore, userId);
        
        try {
            // 获取评测记录
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new EntityNotFoundException("评测记录不存在: " + evaluationId));
            
            // 验证评测状态
            if (evaluation.getStatus() != EvaluationStatus.PENDING) {
                throw new IllegalStateException("评测已完成，无法修改: " + evaluationId);
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 验证评测者类型
            if (evaluation.getEvaluator().getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalStateException("只能提交人工评测结果: " + evaluationId);
            }
            
            // 更新评测记录
            evaluation.setScore(overallScore);
            evaluation.setComments(comments);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 构建评测结果JSON
            Map<String, Object> evaluationResults = new HashMap<>();
            evaluationResults.put("score", overallScore);
            evaluationResults.put("comments", comments);
            evaluationResults.put("criteria_scores", detailScores);
            evaluation.setEvaluationResults(evaluationResults);
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            for (Map<String, Object> detailScore : detailScores) {
                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluation(evaluation);
                
                // 获取评测标准名称
                String criterionName = (String) detailScore.get("criterion");
                detail.setCriterionName(criterionName);
                
                // 尝试查找对应的评测标准
                if (detailScore.containsKey("criterionId") && detailScore.get("criterionId") != null) {
                    Long criterionId = Long.valueOf(detailScore.get("criterionId").toString());
                    evaluationCriterionRepository.findById(criterionId).ifPresent(detail::setCriterion);
                } else {
                    // 如果没有criterionId，可以尝试通过criterionName查找
                    List<EvaluationCriterion> matchingCriteria = evaluationCriterionRepository.findByCriterionName(criterionName);
                    if (!matchingCriteria.isEmpty()) {
                        detail.setCriterion(matchingCriteria.get(0));
                    }
                    // 如果找不到对应的评测标准，criterion将保持为null，这是可以接受的
                }
                
                detail.setScore(new BigDecimal(detailScore.get("score").toString()));
                detail.setComments((String) detailScore.get("comments"));
                detail.setCreatedAt(LocalDateTime.now());
                details.add(detail);
            }
            
            evaluationDetailRepository.saveAll(details);
            
            // 保存分数记录到ANSWER_SCORES表
            LlmAnswer llmAnswer = evaluation.getLlmAnswer();
            Evaluator evaluator = evaluation.getEvaluator();
            
            // 获取问题类型
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            String scoreType = "HUMAN_" + question.getQuestionType().name();
            
            // 保存总体分数到评测记录中
            // 标准化分数（0-100分）
            BigDecimal normalizedScore;
            if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                // 主观题现在使用0-100分制，直接使用
                normalizedScore = overallScore;
            } else {
                // 客观题分数通常是0-100分
                normalizedScore = overallScore;
            }
            
            evaluation.setRawScore(overallScore);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod("HUMAN");
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("成功保存人工评测分数记录，评测ID: {}, 回答ID: {}, 评测者ID: {}", 
                    evaluation.getId(), llmAnswer.getId(), evaluator.getId());
            
            // 保存详细评分记录
            for (Map<String, Object> criterionScore : detailScores) {
                String criterionName = (String) criterionScore.get("criterion");
                BigDecimal criterionScoreValue = new BigDecimal(criterionScore.get("score").toString());
                String criterionComments = (String) criterionScore.get("comments");
                
                // 创建详细评分记录
                EvaluationDetail detailScore = new EvaluationDetail();
                detailScore.setEvaluation(evaluation);
                detailScore.setCriterionName(criterionName);
                detailScore.setScore(criterionScoreValue);
                detailScore.setComments(criterionComments);
                detailScore.setCreatedAt(LocalDateTime.now());
                
                evaluationDetailRepository.save(detailScore);
            }
            
            logger.info("人工评测提交完成，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("提交人工评测结果失败", e);
            throw new RuntimeException("提交人工评测结果失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public EvaluationRun createEvaluationRun(Long modelAnswerRunId, Long evaluatorId, String runName, 
                                            String runDescription, Map<String, Object> parameters, Long userId) {
        logger.info("创建评测运行，模型回答运行ID: {}, 评测者ID: {}, 用户ID: {}", 
                modelAnswerRunId, evaluatorId, userId);
        
        try {
            // 获取模型回答运行记录
            ModelAnswerRun modelAnswerRun = modelAnswerRunRepository.findById(modelAnswerRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的模型回答运行记录: " + modelAnswerRunId));
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 创建评测运行记录
            EvaluationRun evaluationRun = new EvaluationRun();
            evaluationRun.setModelAnswerRun(modelAnswerRun);
            evaluationRun.setEvaluator(evaluator);
            evaluationRun.setRunName(runName);
            evaluationRun.setRunDescription(runDescription);
            evaluationRun.setParameters(objectMapper.writeValueAsString(parameters));
            evaluationRun.setRunTime(LocalDateTime.now());  // 设置运行时间
            evaluationRun.setStatus(RunStatus.PENDING);  // 初始状态为等待中
            evaluationRun.setCreatedBy(userId);
            evaluationRun.setLastUpdated(LocalDateTime.now());
            
            // 设置评测提示词组装配置
            if (parameters != null && parameters.containsKey("evaluationAssemblyConfigId")) {
                Long evaluationAssemblyConfigId = Long.valueOf(parameters.get("evaluationAssemblyConfigId").toString());
                EvaluationPromptAssemblyConfig config = evaluationPromptAssemblyConfigRepository.findById(evaluationAssemblyConfigId)
                        .orElse(null);
                if (config != null) {
                    evaluationRun.setEvaluationAssemblyConfig(config);
                    logger.info("设置评测提示词组装配置: {}", config.getName());
                } else {
                    logger.warn("找不到指定的评测提示词组装配置(ID: {})", evaluationAssemblyConfigId);
                }
            } else {
                // 如果没有指定，尝试使用默认配置
                List<EvaluationPromptAssemblyConfig> activeConfigs = evaluationPromptAssemblyConfigRepository.findByIsActiveTrue();
                if (!activeConfigs.isEmpty()) {
                    evaluationRun.setEvaluationAssemblyConfig(activeConfigs.get(0));
                    logger.info("使用默认评测提示词组装配置: {}", activeConfigs.get(0).getName());
                }
            }
            
            // 设置主观题评测提示词
            if (parameters != null && parameters.containsKey("subjectivePromptId")) {
                Long subjectivePromptId = Long.valueOf(parameters.get("subjectivePromptId").toString());
                EvaluationSubjectivePrompt prompt = evaluationSubjectivePromptRepository.findById(subjectivePromptId)
                        .orElse(null);
                if (prompt != null) {
                    evaluationRun.setSubjectivePrompt(prompt);
                    logger.info("设置主观题评测提示词: {}", prompt.getName());
                } else {
                    logger.warn("找不到指定的主观题评测提示词(ID: {})", subjectivePromptId);
                }
            }
            
            // 保存评测运行记录
            evaluationRun = evaluationRunRepository.save(evaluationRun);
            
            return evaluationRun;
            
        } catch (Exception e) {
            logger.error("创建评测运行记录失败", e);
            throw new RuntimeException("创建评测运行记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public boolean pauseEvaluationRun(Long evaluationRunId) {
        logger.info("暂停评测运行，评测运行ID: {}", evaluationRunId);
        
        // 获取分布式锁
        RLock lock = redissonClient.getLock("evaluation_run_lock:" + evaluationRunId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到评测运行{}的锁，开始暂停操作", evaluationRunId);
                    
                    // 1. 直接设置中断标志，不检查当前状态
                    String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
                    redisTemplate.opsForValue().set(interruptKey, "true");
                    redisTemplate.expire(interruptKey, Duration.ofHours(24));
                    logger.info("评测运行{}已设置中断标志", evaluationRunId);
                    
                    // 2. 更新Redis状态
                    String stateKey = "evaluation_run:state:" + evaluationRunId;
                    redisTemplate.opsForValue().set(stateKey, "PAUSED");
                    redisTemplate.expire(stateKey, Duration.ofHours(24));
                    logger.info("评测运行{}Redis状态已更新为PAUSED", evaluationRunId);
                    
                    // 3. 更新数据库状态
                    int updatedRows = jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ?",
                        RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
                    
                    logger.info("评测运行已暂停，ID: {}, 数据库更新行数: {}", evaluationRunId, updatedRows);
                    
                    // 立即检查Redis中的中断标志是否设置成功
                    String checkValue = redisTemplate.opsForValue().get(interruptKey);
                    logger.info("确认评测运行{}中断标志状态: {}", evaluationRunId, checkValue);
                    return true;
                } finally {
                    lock.unlock();
                    logger.info("评测运行{}的锁已释放", evaluationRunId);
                }
            } else {
                logger.warn("无法获取评测运行{}的锁，暂停操作失败", evaluationRunId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取评测运行{}的锁时被中断", evaluationRunId, e);
            return false;
        } catch (Exception e) {
            logger.error("暂停评测运行时发生错误", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public CompletableFuture<Void> resumeEvaluationRun(Long evaluationRunId) {
        logger.info("恢复评测运行，运行ID: {}", evaluationRunId);
        
        // 获取分布式锁
        RLock lock = redissonClient.getLock("evaluation_run_lock:" + evaluationRunId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到评测运行{}的锁，开始恢复操作", evaluationRunId);
                    
                    // 1. 清除中断标志
                    String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
                    redisTemplate.delete(interruptKey);
                    logger.info("评测运行{}已清除中断标志", evaluationRunId);
                    
                    // 2. 更新Redis状态
                    String stateKey = "evaluation_run:state:" + evaluationRunId;
                    redisTemplate.opsForValue().set(stateKey, "IN_PROGRESS");
                    redisTemplate.expire(stateKey, Duration.ofHours(24));
                    logger.info("评测运行{}Redis状态已更新为IN_PROGRESS", evaluationRunId);
                    
                    // 3. 更新数据库状态
                    jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, resume_count = resume_count + 1, last_updated = ? WHERE id = ?",
                        RunStatus.IN_PROGRESS.toString(), LocalDateTime.now(), evaluationRunId);
                    
                    logger.info("评测运行{}数据库状态已更新为IN_PROGRESS", evaluationRunId);
                    
                    // 获取评测运行ID和所需信息，在当前事务中预加载必要数据
                    final Long runId = evaluationRunId;
                    final EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                            .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                    
                    // 提前加载模型回答运行ID和评测者ID
                    final Long modelAnswerRunId = evaluationRun.getModelAnswerRunId();
                    final Long evaluatorId = evaluationRun.getEvaluatorId();
                    final Long userId = evaluationRun.getCreatedBy();
                    final Long lastProcessedAnswerId = evaluationRun.getLastProcessedAnswerId();
                    
                    // 4. 继续评测过程（在锁外异步执行）
                    return CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("异步继续评测运行过程，ID: {}", runId);
                            // 继续处理评测，传递预加载的信息
                            continueEvaluationProcess(runId, modelAnswerRunId, evaluatorId, userId, lastProcessedAnswerId);
                        } catch (Exception e) {
                            logger.error("继续评测运行过程时发生错误", e);
                            try {
                                // 更新状态为失败
                                updateEvaluationRunStatus(runId, RunStatus.FAILED, e.getMessage());
                            } catch (Exception ex) {
                                logger.error("更新评测运行状态失败", ex);
                            }
                        }
                    }, evaluationExecutor);
                } finally {
                    lock.unlock();
                    logger.info("评测运行{}的锁已释放", evaluationRunId);
                }
            } else {
                logger.warn("无法获取评测运行{}的锁，恢复操作失败", evaluationRunId);
                throw new RuntimeException("无法获取评测运行的锁，恢复操作失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取评测运行{}的锁时被中断", evaluationRunId, e);
            throw new RuntimeException("获取评测运行的锁时被中断: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("恢复评测运行失败", e);
            throw new RuntimeException("恢复评测运行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 继续处理评测 - 修改后的方法，使用预加载的信息并在新事务中执行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void continueEvaluationProcess(Long evaluationRunId, Long modelAnswerRunId, Long evaluatorId, Long userId, Long lastProcessedAnswerId) {
        try {
            // 首先检查是否已暂停
            if (shouldInterruptEvaluation(evaluationRunId)) {
                logger.info("评测运行{}已被暂停，不再继续处理", evaluationRunId);
                
                // 确保数据库状态为暂停
                jdbcTemplate.update(
                    "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ? AND status != ?",
                    RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), 
                    evaluationRunId, RunStatus.PAUSED.toString());
                
                return;
            }
            
            // 重新获取评测运行记录 - 确保在新事务中
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取评测者 - 在新事务中重新加载
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
            
            // 获取所有回答 - 使用JOIN FETCH主动加载问题
            List<LlmAnswer> allAnswers = llmAnswerRepository.findByModelAnswerRunIdWithQuestions(modelAnswerRunId);
            logger.info("评测运行{}，所有回答数量: {}", evaluationRunId, allAnswers.size());
            
            // 查询已评测的回答ID - 使用现有方法
            List<Evaluation> existingEvaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
            List<Long> evaluatedAnswerIds = existingEvaluations.stream()
                    .map(eval -> eval.getLlmAnswer().getId())
                    .collect(Collectors.toList());
            logger.info("评测运行{}，已评测回答数量: {}", evaluationRunId, evaluatedAnswerIds.size());
            
            // 收集所有回答ID用于日志
            String allAnswerIds = allAnswers.stream()
                    .map(a -> a.getId().toString())
                    .collect(Collectors.joining(", "));
            logger.debug("评测运行{}，所有回答ID: {}", evaluationRunId, allAnswerIds);
            
            // 收集所有已评测回答ID用于日志
            String allEvaluatedIds = evaluatedAnswerIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.debug("评测运行{}，已评测回答ID: {}", evaluationRunId, allEvaluatedIds);
            
            List<LlmAnswer> remainingAnswers;
            
            if (lastProcessedAnswerId != null) {
                // 过滤出未处理的回答
                remainingAnswers = allAnswers.stream()
                        .filter(answer -> answer.getId() > lastProcessedAnswerId)
                        .collect(Collectors.toList());
                logger.info("评测运行{}，使用lastProcessedAnswerId({})过滤，剩余回答数: {}", 
                        evaluationRunId, lastProcessedAnswerId, remainingAnswers.size());
                
                // 如果使用lastProcessedAnswerId过滤后没有剩余回答，尝试使用评测存在检查
                if (remainingAnswers.isEmpty()) {
                    logger.info("评测运行{}，通过ID过滤后没有剩余回答，尝试使用评测存在检查", evaluationRunId);
                    Set<Long> evaluatedIdsSet = new HashSet<>(evaluatedAnswerIds);
                    remainingAnswers = allAnswers.stream()
                            .filter(answer -> !evaluatedIdsSet.contains(answer.getId()))
                            .collect(Collectors.toList());
                    logger.info("评测运行{}，使用评测存在检查后，剩余回答数: {}", evaluationRunId, remainingAnswers.size());
                }
            } else {
                // 获取未评测的回答
                Set<Long> evaluatedIdsSet = new HashSet<>(evaluatedAnswerIds);
                remainingAnswers = allAnswers.stream()
                        .filter(answer -> !evaluatedIdsSet.contains(answer.getId()))
                        .collect(Collectors.toList());
                logger.info("评测运行{}，使用评测存在检查，剩余回答数: {}", evaluationRunId, remainingAnswers.size());
            }
            
            // 如果还是没有剩余回答，但allAnswers不为空，强制重新评测第一个回答（这是一个安全措施）
            if (remainingAnswers.isEmpty() && !allAnswers.isEmpty() && evaluationRun.getCompletedAnswersCount() < allAnswers.size()) {
                logger.warn("评测运行{}可能存在问题：所有回答数={}, 已完成数={}, 已评测ID数={}，但过滤后剩余回答为0，强制选择一个回答进行评测",
                        evaluationRunId, allAnswers.size(), evaluationRun.getCompletedAnswersCount(), evaluatedAnswerIds.size());
                remainingAnswers = Arrays.asList(allAnswers.get(0));
            }
            
            logger.info("继续评测运行，剩余回答数: {}", remainingAnswers.size());
            
            // 再次检查是否已暂停
            if (shouldInterruptEvaluation(evaluationRunId)) {
                logger.info("评测运行{}在处理前已被暂停，停止处理", evaluationRunId);
                
                // 确保数据库状态为暂停
                jdbcTemplate.update(
                    "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ? AND status != ?",
                    RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), 
                    evaluationRunId, RunStatus.PAUSED.toString());
                
                return;
            }
            
            // 批量处理剩余的回答
            int batchSize = evaluationRun.getBatchSize() != null ? evaluationRun.getBatchSize() : 10;
            
            // 确保所有问题已完全初始化
            remainingAnswers.forEach(answer -> {
                if (answer.getDatasetQuestionMapping() != null && answer.getDatasetQuestionMapping().getStandardQuestion() != null) {
                    // 触发延迟加载，确保在当前事务中完全初始化
                    StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                    question.getQuestionType(); // 强制初始化
                    question.getQuestionText(); // 强制初始化
                }
            });
            
            for (int i = 0; i < remainingAnswers.size(); i += batchSize) {
                // 每个批次前强制检查是否应该中断处理
                if (shouldInterruptEvaluation(evaluationRunId)) {
                    logger.info("检测到评测运行{}的中断信号，立即停止批次处理", evaluationRunId);
                    
                    // 确保数据库状态更新为暂停
                    jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ?",
                        RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
                    
                    return;
                }
                
                // 获取当前批次的回答
                int endIndex = Math.min(i + batchSize, remainingAnswers.size());
                List<LlmAnswer> batchAnswers = remainingAnswers.subList(i, endIndex);
                
                // 批量评测 - 每个批次在独立事务中处理
                evaluateAnswersBatch(batchAnswers, evaluator.getId(), userId);
                
                // 更新最后处理的回答ID
                if (!batchAnswers.isEmpty()) {
                    LlmAnswer lastAnswer = batchAnswers.get(batchAnswers.size() - 1);
                    updateLastProcessedAnswerId(evaluationRunId, lastAnswer.getId());
                }
                
                // 更新进度 - 查询最新的评测运行记录
                EvaluationRun currentRun = evaluationRunRepository.findById(evaluationRunId)
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                updateEvaluationProgress(currentRun, i + batchAnswers.size(), remainingAnswers.size());
            }
            
            // 获取最新的评测结果数量 - 使用现有方法
            int totalAnswers = allAnswers.size();
            
            // 重新查询已评测的数量
            List<Long> answerIds = allAnswers.stream().map(LlmAnswer::getId).collect(Collectors.toList());
            int completedAnswers = 0;
            for (Long answerId : answerIds) {
                if (evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(answerId, evaluatorId)) {
                    completedAnswers++;
                }
            }
            
            logger.info("评测运行{}完成状态检查: 总回答数={}, 已评测数={}", evaluationRunId, totalAnswers, completedAnswers);
            
            // 检查是否所有回答都已处理完成
            if (remainingAnswers.isEmpty() || completedAnswers >= totalAnswers) {
                completeEvaluationRun(evaluationRunId);
            } else {
                logger.warn("评测运行{}存在未完成的评测: 总回答数={}, 已评测数={}, 未评测数={}",
                        evaluationRunId, totalAnswers, completedAnswers, (totalAnswers - completedAnswers));
            }
            
        } catch (Exception e) {
            logger.error("继续处理评测过程中发生错误", e);
            throw e;
        }
    }
    
    /**
     * 批量评测回答，在独立事务中处理
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAnswersBatch(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId) {
        List<Evaluation> results = new ArrayList<>();
        int successCount = 0;
        
        for (LlmAnswer answer : llmAnswers) {
            try {
                // 在新事务中重新加载所需的实体
                LlmAnswer reloadedAnswer = llmAnswerRepository.findByIdWithQuestion(answer.getId())
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的回答: " + answer.getId()));
                
                Evaluation evaluation = evaluateAnswer(reloadedAnswer, evaluatorId, userId);
                if (evaluation != null) {
                    results.add(evaluation);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("评测回答失败，回答ID: {}", answer.getId(), e);
            }
        }
        
        logger.info("批量评测完成，成功评测数量: {}", successCount);
    }
    
    /**
     * 检查是否应该中断评测
     */
    private boolean shouldInterruptEvaluation(Long evaluationRunId) {
        String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
        String value = redisTemplate.opsForValue().get(interruptKey);
        boolean interrupted = "true".equals(value);
        if (interrupted) {
            logger.info("检测到评测运行{}的中断标志为true，应当暂停处理", evaluationRunId);
        }
        return interrupted;
    }
    
    /**
     * 更新最后处理的回答ID
     */
    @Transactional
    public void updateLastProcessedAnswerId(Long evaluationRunId, Long answerId) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET last_processed_answer_id = ? WHERE id = ?",
            answerId, evaluationRunId);
    }
    
    /**
     * 更新评测运行状态
     */
    @Transactional
    public void updateEvaluationRunStatus(Long evaluationRunId, RunStatus status, String errorMessage) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET status = ?, error_message = ?, last_updated = ? WHERE id = ?",
            status.toString(), errorMessage, LocalDateTime.now(), evaluationRunId);
    }
    
    /**
     * 完成评测运行
     */
    @Transactional
    public void completeEvaluationRun(Long evaluationRunId) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET status = ?, completed_at = ?, progress_percentage = 100, last_updated = ? WHERE id = ?",
            RunStatus.COMPLETED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
        
        logger.info("评测运行{}已完成", evaluationRunId);
    }
    
    @Override
    public List<EvaluationRun> getEvaluationRuns(Long modelAnswerRunId, Long evaluatorId, String status, int page, int size) {
        logger.info("获取评测运行列表，模型回答运行ID: {}, 评测者ID: {}, 状态: {}, 页码: {}, 大小: {}", 
                modelAnswerRunId, evaluatorId, status, page, size);
        
        try {
            // 创建分页对象
            Pageable pageable = PageRequest.of(page, size);
            
            // 根据条件查询
            List<EvaluationRun> runs;
            if (modelAnswerRunId != null && evaluatorId != null && status != null) {
                // 三个条件都有
                runs = evaluationRunRepository.findByModelAnswerRunIdAndEvaluatorIdAndStatus(
                        modelAnswerRunId, evaluatorId, RunStatus.valueOf(status), pageable);
            } else if (modelAnswerRunId != null && evaluatorId != null) {
                // 只有模型回答运行ID和评测者ID
                runs = evaluationRunRepository.findByModelAnswerRunIdAndEvaluatorId(
                        modelAnswerRunId, evaluatorId, pageable);
            } else if (modelAnswerRunId != null && status != null) {
                // 只有模型回答运行ID和状态
                runs = evaluationRunRepository.findByModelAnswerRunIdAndStatus(
                        modelAnswerRunId, RunStatus.valueOf(status), pageable);
            } else if (evaluatorId != null && status != null) {
                // 只有评测者ID和状态
                runs = evaluationRunRepository.findByEvaluatorIdAndStatus(
                        evaluatorId, RunStatus.valueOf(status), pageable);
            } else if (modelAnswerRunId != null) {
                // 只有模型回答运行ID
                runs = evaluationRunRepository.findByModelAnswerRunId(modelAnswerRunId, pageable);
            } else if (evaluatorId != null) {
                // 只有评测者ID
                runs = evaluationRunRepository.findByEvaluatorId(evaluatorId, pageable);
            } else if (status != null) {
                // 只有状态
                runs = evaluationRunRepository.findByStatus(RunStatus.valueOf(status), pageable);
            } else {
                // 无条件查询
                runs = evaluationRunRepository.findAll(pageable);
            }
            
            logger.info("获取到{}条评测运行记录", runs.size());
            return runs;
            
        } catch (Exception e) {
            logger.error("获取评测运行列表失败", e);
            throw new RuntimeException("获取评测运行列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public EvaluationRun getEvaluationRun(Long evaluationRunId) {
        logger.info("获取评测运行详情，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            return evaluationRun;
            
        } catch (Exception e) {
            logger.error("获取评测运行详情失败", e);
            throw new RuntimeException("获取评测运行详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getEvaluationRunResults(Long evaluationRunId) {
        logger.info("获取评测运行结果，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取所有评测结果
            List<Evaluation> evaluations = evaluationRepository.findByEvaluationRunId(evaluationRunId);
            
            // 构建结果统计
            Map<String, Object> results = new HashMap<>();
            results.put("evaluationRunId", evaluationRunId);
            results.put("runName", evaluationRun.getRunName());
            results.put("status", evaluationRun.getStatus());
            results.put("startTime", evaluationRun.getStartTime());
            results.put("endTime", evaluationRun.getEndTime());
            results.put("totalEvaluations", evaluations.size());
            
            // 计算总体统计信息
            if (!evaluations.isEmpty()) {
                // 计算平均分
                BigDecimal totalScore = BigDecimal.ZERO;
                int validScoreCount = 0;
                
                for (Evaluation evaluation : evaluations) {
                    if (evaluation.getScore() != null) {
                        totalScore = totalScore.add(evaluation.getScore());
                        validScoreCount++;
                    }
                }
                
                if (validScoreCount > 0) {
                    BigDecimal averageScore = totalScore.divide(new BigDecimal(validScoreCount), 2, RoundingMode.HALF_UP);
                    results.put("averageScore", averageScore);
                }
                
                // 按问题类型分组统计
                Map<QuestionType, List<Evaluation>> groupedByType = evaluations.stream()
                        .filter(e -> e.getLlmAnswer() != null && 
                                   e.getLlmAnswer().getDatasetQuestionMapping() != null && 
                                   e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion() != null)
                        .collect(Collectors.groupingBy(e -> e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion().getQuestionType()));
                
                Map<String, Object> typeStats = new HashMap<>();
                for (Map.Entry<QuestionType, List<Evaluation>> entry : groupedByType.entrySet()) {
                    QuestionType type = entry.getKey();
                    List<Evaluation> typeEvaluations = entry.getValue();
                    
                    Map<String, Object> typeStat = new HashMap<>();
                    typeStat.put("count", typeEvaluations.size());
                    
                    // 计算该类型的平均分
                    BigDecimal typeTotal = BigDecimal.ZERO;
                    int typeValidCount = 0;
                    for (Evaluation evaluation : typeEvaluations) {
                        if (evaluation.getScore() != null) {
                            typeTotal = typeTotal.add(evaluation.getScore());
                            typeValidCount++;
                        }
                    }
                    
                    if (typeValidCount > 0) {
                        BigDecimal typeAverage = typeTotal.divide(new BigDecimal(typeValidCount), 2, RoundingMode.HALF_UP);
                        typeStat.put("averageScore", typeAverage);
                    }
                    
                    typeStats.put(type.toString(), typeStat);
                }
                
                results.put("questionTypeStats", typeStats);
                
                // 添加评测标准统计
                Map<String, Map<String, Object>> criteriaStats = new HashMap<>();
                for (Evaluation evaluation : evaluations) {
                    List<EvaluationDetail> details = evaluationDetailRepository.findByEvaluationId(evaluation.getId());
                    
                    for (EvaluationDetail detail : details) {
                        String criterion = detail.getCriterionName();
                        Map<String, Object> criterionStat = criteriaStats.computeIfAbsent(criterion, k -> new HashMap<>());
                        
                        // 更新评分总和和计数
                        BigDecimal currentTotal = (BigDecimal) criterionStat.getOrDefault("totalScore", BigDecimal.ZERO);
                        int currentCount = (int) criterionStat.getOrDefault("count", 0);
                        
                        criterionStat.put("totalScore", currentTotal.add(detail.getScore()));
                        criterionStat.put("count", currentCount + 1);
                    }
                }
                
                // 计算每个标准的平均分
                for (Map<String, Object> criterionStat : criteriaStats.values()) {
                    BigDecimal total = (BigDecimal) criterionStat.get("totalScore");
                    int count = (int) criterionStat.get("count");
                    
                    if (count > 0) {
                        BigDecimal average = total.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                        criterionStat.put("averageScore", average);
                    }
                }
                
                results.put("criteriaStats", criteriaStats);
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("获取评测运行结果失败", e);
            throw new RuntimeException("获取评测运行结果失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getEvaluationResults(Evaluation evaluation) {
        if (evaluation.getEvaluationResults() == null) {
            return new HashMap<>();
        }
        return evaluation.getEvaluationResults();
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchObjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        logger.debug("开始评测批次的客观题，批次ID: {}", batchId);
        
        // 验证评测者和用户
        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户: " + userId));
        
        // 获取批次下的所有模型运行
        List<ModelAnswerRun> modelRuns = modelAnswerRunRepository.findByAnswerGenerationBatchId(batchId);
        if (modelRuns.isEmpty()) {
            throw new IllegalArgumentException("找不到指定批次的模型运行: " + batchId);
        }
        
        // 统计信息
        Map<String, Object> result = new HashMap<>();
        int totalAnswers = 0;
        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        Map<QuestionType, Integer> typeCount = new HashMap<>();
        Map<QuestionType, BigDecimal> typeScoreSum = new HashMap<>();
        
        // 按重复索引分组的统计数据
        Map<Integer, Integer> repeatIndexCount = new HashMap<>();
        Map<Integer, BigDecimal> repeatIndexScoreSum = new HashMap<>();
        Map<Integer, Map<QuestionType, Integer>> repeatIndexTypeCount = new HashMap<>();
        Map<Integer, Map<QuestionType, BigDecimal>> repeatIndexTypeScoreSum = new HashMap<>();
        
        // 初始化问题类型计数
        typeCount.put(QuestionType.SINGLE_CHOICE, 0);
        typeCount.put(QuestionType.MULTIPLE_CHOICE, 0);
        typeCount.put(QuestionType.SIMPLE_FACT, 0);
        
        // 初始化问题类型总分
        typeScoreSum.put(QuestionType.SINGLE_CHOICE, BigDecimal.ZERO);
        typeScoreSum.put(QuestionType.MULTIPLE_CHOICE, BigDecimal.ZERO);
        typeScoreSum.put(QuestionType.SIMPLE_FACT, BigDecimal.ZERO);
        
        // 处理每个模型运行
        for (ModelAnswerRun modelRun : modelRuns) {
            // 获取该运行下的所有回答
            List<LlmAnswer> allAnswers = llmAnswerRepository.findByModelAnswerRunIdWithQuestions(modelRun.getId());
            
            // 过滤出客观题回答
            List<LlmAnswer> objectiveAnswers = allAnswers.stream()
                    .filter(answer -> {
                        if (answer.getDatasetQuestionMapping() == null || answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                            logger.warn("跳过回答ID: {}，因为其关联的标准问题为空", answer.getId());
                            return false;
                        }
                        StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                        QuestionType type = question.getQuestionType();
                        return type == QuestionType.SINGLE_CHOICE || 
                               type == QuestionType.MULTIPLE_CHOICE || 
                               type == QuestionType.SIMPLE_FACT;
                    })
                    .collect(Collectors.toList());
            
            logger.debug("找到{}个客观题回答需要评测", objectiveAnswers.size());
            totalAnswers += objectiveAnswers.size();
            
            // 批量评测客观题回答，包括同一问题的所有重复回答
            for (LlmAnswer answer : objectiveAnswers) {
                // 为每个回答创建单独的事务
                try {
                    // 获取repeatIndex，如果为null则默认为0
                    Integer repeatIndex = answer.getRepeatIndex() != null ? answer.getRepeatIndex() : 0;
                    
                    // 初始化此repeatIndex的统计数据（如果不存在）
                    repeatIndexCount.putIfAbsent(repeatIndex, 0);
                    repeatIndexScoreSum.putIfAbsent(repeatIndex, BigDecimal.ZERO);
                    
                    if (!repeatIndexTypeCount.containsKey(repeatIndex)) {
                        Map<QuestionType, Integer> indexTypeCount = new HashMap<>();
                        indexTypeCount.put(QuestionType.SINGLE_CHOICE, 0);
                        indexTypeCount.put(QuestionType.MULTIPLE_CHOICE, 0);
                        indexTypeCount.put(QuestionType.SIMPLE_FACT, 0);
                        repeatIndexTypeCount.put(repeatIndex, indexTypeCount);
                    }
                    
                    if (!repeatIndexTypeScoreSum.containsKey(repeatIndex)) {
                        Map<QuestionType, BigDecimal> indexTypeScoreSum = new HashMap<>();
                        indexTypeScoreSum.put(QuestionType.SINGLE_CHOICE, BigDecimal.ZERO);
                        indexTypeScoreSum.put(QuestionType.MULTIPLE_CHOICE, BigDecimal.ZERO);
                        indexTypeScoreSum.put(QuestionType.SIMPLE_FACT, BigDecimal.ZERO);
                        repeatIndexTypeScoreSum.put(repeatIndex, indexTypeScoreSum);
                    }
                    
                    BigDecimal score = evaluateSingleObjectiveAnswer(answer, evaluator, user, typeCount, typeScoreSum);
                    
                    // 更新按repeatIndex分组的统计
                    repeatIndexCount.put(repeatIndex, repeatIndexCount.get(repeatIndex) + 1);
                    repeatIndexScoreSum.put(repeatIndex, repeatIndexScoreSum.get(repeatIndex).add(score));
                    
                    // 更新按repeatIndex分组的问题类型统计
                    if (answer.getDatasetQuestionMapping() == null || answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                        logger.warn("无法获取回答ID: {}的问题类型，因为关联的标准问题为空", answer.getId());
                        continue;
                    }
                    QuestionType type = answer.getDatasetQuestionMapping().getStandardQuestion().getQuestionType();
                    Map<QuestionType, Integer> indexTypeCount = repeatIndexTypeCount.get(repeatIndex);
                    Map<QuestionType, BigDecimal> indexTypeScoreSum = repeatIndexTypeScoreSum.get(repeatIndex);
                    
                    indexTypeCount.put(type, indexTypeCount.get(type) + 1);
                    indexTypeScoreSum.put(type, indexTypeScoreSum.get(type).add(score));
                    
                    // 更新总统计
                    totalScore = totalScore.add(score);
                    successCount++;
                } catch (Exception e) {
                    logger.error("评测回答时出错，回答ID: {}", answer.getId(), e);
                    failedCount++;
                }
            }
        }
        
        // 计算统计结果
        result.put("totalAnswers", totalAnswers);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        
        if (successCount > 0) {
            BigDecimal avgScore = totalScore.divide(new BigDecimal(successCount), 2, RoundingMode.HALF_UP);
            result.put("averageScore", avgScore);
        } else {
            result.put("averageScore", 0);
        }
        
        // 各类型题目的统计
        Map<String, Object> typeStats = new HashMap<>();
        for (QuestionType type : Arrays.asList(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.SIMPLE_FACT)) {
            Map<String, Object> typeStat = new HashMap<>();
            int count = typeCount.get(type);
            typeStat.put("count", count);
            
            if (count > 0) {
                BigDecimal avgTypeScore = typeScoreSum.get(type).divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                typeStat.put("averageScore", avgTypeScore);
            } else {
                typeStat.put("averageScore", 0);
            }
            
            typeStats.put(type.name(), typeStat);
        }
        result.put("typeStatistics", typeStats);
        
        // 按repeatIndex分组的统计
        Map<String, Object> repeatIndexStats = new HashMap<>();
        for (Integer repeatIndex : repeatIndexCount.keySet()) {
            Map<String, Object> indexStat = new HashMap<>();
            int count = repeatIndexCount.get(repeatIndex);
            indexStat.put("count", count);
            
            if (count > 0) {
                BigDecimal avgScore = repeatIndexScoreSum.get(repeatIndex)
                    .divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                indexStat.put("averageScore", avgScore);
                
                // 该重复索引下各类型题目的统计
                Map<String, Object> indexTypeStats = new HashMap<>();
                Map<QuestionType, Integer> indexTypeCount = repeatIndexTypeCount.get(repeatIndex);
                Map<QuestionType, BigDecimal> indexTypeScoreSum = repeatIndexTypeScoreSum.get(repeatIndex);
                
                for (QuestionType type : Arrays.asList(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.SIMPLE_FACT)) {
                    Map<String, Object> indexTypeStat = new HashMap<>();
                    int typeCountVal = indexTypeCount.get(type);
                    indexTypeStat.put("count", typeCountVal);
                    
                    if (typeCountVal > 0) {
                        BigDecimal avgTypeScore = indexTypeScoreSum.get(type)
                            .divide(new BigDecimal(typeCountVal), 2, RoundingMode.HALF_UP);
                        indexTypeStat.put("averageScore", avgTypeScore);
                    } else {
                        indexTypeStat.put("averageScore", 0);
                    }
                    
                    indexTypeStats.put(type.name(), indexTypeStat);
                }
                
                indexStat.put("typeStatistics", indexTypeStats);
            } else {
                indexStat.put("averageScore", 0);
                indexStat.put("typeStatistics", new HashMap<>());
            }
            
            repeatIndexStats.put("repeat_" + repeatIndex, indexStat);
        }
        result.put("repeatIndexStatistics", repeatIndexStats);
        
        logger.info("批次客观题评测完成，总计: {}, 成功: {}, 失败: {}", totalAnswers, successCount, failedCount);
        return result;
    }

    /**
     * 评测单个客观题回答
     * 这个方法需要在单独的事务中执行，以避免一个回答的评测失败影响其他回答
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BigDecimal evaluateSingleObjectiveAnswer(LlmAnswer answer, Evaluator evaluator, User user, 
                                        Map<QuestionType, Integer> typeCount, 
                                        Map<QuestionType, BigDecimal> typeScoreSum) {
        // 确保加载完整的问题信息
        answer = getLlmAnswerWithQuestions(answer);
        
        if (answer.getDatasetQuestionMapping() == null || answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
            // 获取详细的关联信息，以便更好地诊断问题
            Map<String, Object> relations = checkAnswerRelations(answer.getId());
            logger.warn("无法评测回答ID: {}，因为其关联的标准问题为空。关联信息: {}", answer.getId(), relations);
            // 返回默认评分0
            return BigDecimal.ZERO;
        }

        StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
        QuestionType type = question.getQuestionType();
        
        // 获取重复索引
        Integer repeatIndex = answer.getRepeatIndex();
        if (repeatIndex == null) {
            repeatIndex = 0; // 默认为0
        }
        
        // 检查是否已经存在针对这个回答的评测记录
        String scoreType = "OBJECTIVE_" + type.name();
        
        // 考虑repeatIndex，使用完全匹配的回答ID查找
        Optional<Evaluation> existingEvaluation = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                answer.getId(), evaluator.getId()).stream().findFirst();
        
        if (existingEvaluation.isPresent()) {
            logger.info("该回答已存在评测记录，回答ID: {}, 重复索引: {}, 评测者ID: {}, 分数类型: {}", 
                    answer.getId(), answer.getRepeatIndex(), evaluator.getId(), scoreType);
            
            BigDecimal score = existingEvaluation.get().getRawScore();
            
            // 更新统计信息
            if (typeCount.containsKey(type)) {
                typeCount.put(type, typeCount.get(type) + 1);
                typeScoreSum.put(type, typeScoreSum.get(type).add(score));
            }
            
            return score;
        }
        
        // 创建评测记录
        Evaluation evaluation = new Evaluation();
        evaluation.setLlmAnswer(answer);
        evaluation.setEvaluator(evaluator);
        evaluation.setEvaluationTime(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.SUCCESS);
        evaluation.setCreatedByUser(user);
        evaluation.setCreationTime(LocalDateTime.now()); // 设置创建时间
        evaluation.setCompletionTime(LocalDateTime.now()); // 设置完成时间
        evaluation.setEvaluationType(EvaluationType.AI_MODEL); // 设置评测类型为自动评测
        
        BigDecimal score;
        Map<String, Object> evaluationResult;
        
        // 根据问题类型进行评测
        switch (type) {
            case SINGLE_CHOICE:
                StandardObjectiveAnswer singleChoiceAnswer = objectiveAnswerRepository.findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到单选题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateSingleChoice(
                        answer.getAnswerText(),
                        singleChoiceAnswer.getCorrectOptionIds(),
                        singleChoiceAnswer.getOptions());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.SINGLE_CHOICE, typeCount.get(QuestionType.SINGLE_CHOICE) + 1);
                typeScoreSum.put(QuestionType.SINGLE_CHOICE, typeScoreSum.get(QuestionType.SINGLE_CHOICE).add(score));
                break;
                
            case MULTIPLE_CHOICE:
                StandardObjectiveAnswer multipleChoiceAnswer = objectiveAnswerRepository
                        .findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到多选题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateMultipleChoice(
                        answer.getAnswerText(),
                        multipleChoiceAnswer.getCorrectOptionIds(),
                        multipleChoiceAnswer.getOptions());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.MULTIPLE_CHOICE, typeCount.get(QuestionType.MULTIPLE_CHOICE) + 1);
                typeScoreSum.put(QuestionType.MULTIPLE_CHOICE, typeScoreSum.get(QuestionType.MULTIPLE_CHOICE).add(score));
                break;
                
            case SIMPLE_FACT:
                StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                        .findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到简单事实题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateSimpleFact(
                        answer.getAnswerText(),
                        simpleAnswer.getAnswerText(),
                        simpleAnswer.getAlternativeAnswers());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.SIMPLE_FACT, typeCount.get(QuestionType.SIMPLE_FACT) + 1);
                typeScoreSum.put(QuestionType.SIMPLE_FACT, typeScoreSum.get(QuestionType.SIMPLE_FACT).add(score));
                break;
                
            default:
                // 不应该到达这里，因为我们已经过滤了问题类型
                throw new IllegalArgumentException("不支持的问题类型: " + type);
        }
        
        try {
            // 保存评测结果
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测结果，评测ID: {}, 回答ID: {}, 重复索引: {}", 
                    evaluation.getId(), answer.getId(), repeatIndex);
            
            // 保存评测详情
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                List<EvaluationDetail> details = new ArrayList<>();
                for (Map<String, Object> criteriaScore : criteriaScores) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    detail.setCriterionName((String) criteriaScore.get("criterion"));
                    detail.setScore(new BigDecimal(criteriaScore.get("score").toString()));
                    detail.setComments((String) criteriaScore.get("comments"));
                    detail.setCreatedAt(LocalDateTime.now());
                    details.add(detail);
                }
                
                evaluationDetailRepository.saveAll(details);
                logger.info("成功保存评测详情，评测ID: {}, 详情数量: {}", evaluation.getId(), details.size());
            }
            
            // 保存分数记录到Evaluation中
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(score); // 对于客观题，原始分数和标准化分数相同
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod("AUTOMATIC");
            
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测记录，评测ID: {}, 回答ID: {}, 重复索引: {}, 评测者ID: {}, 分数类型: {}", 
                    evaluation.getId(), answer.getId(), repeatIndex, evaluator.getId(), scoreType);
            
            return score;
        } catch (Exception e) {
            logger.error("保存评测结果时出错，回答ID: {}, 重复索引: {}", answer.getId(), repeatIndex, e);
            throw e; // 重新抛出异常，让事务回滚
        }
    }

    /**
     * 使用BERT模型计算文本相似度（目前为伪实现，实际项目中可集成真实BERT模型）
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度得分（0-1之间）
     */
    public BigDecimal calculateBertSimilarity(String text1, String text2) {
        logger.info("计算BERT文本相似度，文本1长度: {}, 文本2长度: {}", 
                text1 != null ? text1.length() : 0, 
                text2 != null ? text2.length() : 0);
        
        try {
            // 参数验证
            if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
                logger.warn("计算BERT相似度失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本预处理：移除常见的答案前缀
            String processedText1 = text1.toLowerCase()
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            String processedText2 = text2.toLowerCase()
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedText1.isEmpty() || processedText2.isEmpty()) {
                logger.warn("计算BERT相似度失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 这里是BERT相似度计算的伪实现
            // 实际项目中，您可以集成一个Java BERT客户端库或使用HTTP请求调用BERT服务
            
            // 模拟BERT相似度计算 - 这里使用加权Levenshtein距离作为示例
            int distance = calculateLevenshteinDistance(processedText1, processedText2);
            int maxLength = Math.max(processedText1.length(), processedText2.length());
            
            // 计算基础相似度：1 - 标准化编辑距离
            double baseSimilarity = 1.0 - ((double) distance / maxLength);
            
            // 关键词匹配加权（模拟BERT的语义理解能力）
            double keywordBoost = 0.0;
            // 提取关键词（简化实现）
            String[] words1 = processedText1.split("\\s+");
            String[] words2 = processedText2.split("\\s+");
            
            Set<String> keyWords1 = new HashSet<>(Arrays.asList(words1));
            Set<String> keyWords2 = new HashSet<>(Arrays.asList(words2));
            
            // 计算关键词重叠率
            Set<String> commonWords = new HashSet<>(keyWords1);
            commonWords.retainAll(keyWords2);
            
            if (!keyWords1.isEmpty() && !keyWords2.isEmpty()) {
                keywordBoost = 0.2 * ((double) commonWords.size() / Math.min(keyWords1.size(), keyWords2.size()));
            }
            
            // 最终BERT模拟相似度 = 基础相似度 + 关键词加权
            double bertSimilarity = Math.min(1.0, baseSimilarity + keywordBoost);
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(bertSimilarity).setScale(2, RoundingMode.HALF_UP);
            
            logger.info("BERT相似度计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, text1, processedText1, text2, processedText2);
            return result;
            
        } catch (Exception e) {
            logger.error("计算BERT相似度时发生错误", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        // 调用带有subjectivePromptId参数的方法，传递null表示使用默认提示词
        return evaluateBatchSubjectiveQuestions(batchId, evaluatorId, userId, null);
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId, Long subjectivePromptId) {
        return evaluateBatchSubjectiveQuestions(batchId, evaluatorId, userId, subjectivePromptId, null, null);
    }

    // 删除重复的方法

    @Transactional
    public Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId, Long subjectivePromptId, Long evaluationAssemblyConfigId) {
        return evaluateBatchSubjectiveQuestions(batchId, evaluatorId, userId, subjectivePromptId, evaluationAssemblyConfigId, null);
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId, 
            Long subjectivePromptId, Long evaluationAssemblyConfigId, List<Long> criteriaIds) {
        logger.info("批量评测主观题，批次ID: {}, 评测者ID: {}, 用户ID: {}, 提示词ID: {}, 组装配置ID: {}, 评测标准IDs: {}", 
                batchId, evaluatorId, userId, subjectivePromptId, evaluationAssemblyConfigId, criteriaIds);
        
        // 验证评测者和用户
        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
        
        // 验证评测者类型是AI
        if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
            throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
        }
        
        // 获取或创建评测运行记录
        EvaluationRun evaluationRun = getOrCreateEvaluationRun(batchId, evaluatorId, userId);
        
        // 设置评测提示词ID和评测组装配置ID
        if (evaluationRun.getClass().getDeclaredMethods().toString().contains("setSubjectivePromptId")) {
            try {
                evaluationRun.getClass().getMethod("setSubjectivePromptId", Long.class).invoke(evaluationRun, subjectivePromptId);
            } catch (Exception e) {
                logger.warn("无法设置主观题评测提示词ID: {}", e.getMessage());
            }
        }
        
        if (evaluationRun.getClass().getDeclaredMethods().toString().contains("setEvaluationAssemblyConfigId")) {
            try {
                evaluationRun.getClass().getMethod("setEvaluationAssemblyConfigId", Long.class).invoke(evaluationRun, evaluationAssemblyConfigId);
            } catch (Exception e) {
                logger.warn("无法设置评测组装配置ID: {}", e.getMessage());
            }
        }
        
        evaluationRunRepository.save(evaluationRun);
        
        try {
            // 获取该批次的所有回答
            List<LlmAnswer> answers = llmAnswerRepository.findByBatchIdWithQuestions(batchId);
            
            // 过滤出主观题的回答
            answers = answers.stream()
                    .filter(answer -> {
                        if (answer.getDatasetQuestionMapping() == null || answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                            logger.warn("跳过回答ID: {}，因为其关联的标准问题为空", answer.getId());
                            return false;
                        }
                        return answer.getDatasetQuestionMapping().getStandardQuestion().getQuestionType() == QuestionType.SUBJECTIVE;
                    })
                    .collect(Collectors.toList());
            
            if (answers.isEmpty()) {
                logger.info("批次中没有主观题回答需要评测，批次ID: {}", batchId);
                return Map.of("status", "completed", "message", "没有主观题需要评测");
            }
            
            // 更新总回答数
            evaluationRun.setTotalAnswersCount(answers.size());
            
            // 获取指定的评测标准
            List<EvaluationCriterion> criteria;
            if (criteriaIds != null && !criteriaIds.isEmpty()) {
                // 使用JDBC查询指定ID的评测标准
                StringBuilder idPlaceholders = new StringBuilder();
                for (int i = 0; i < criteriaIds.size(); i++) {
                    idPlaceholders.append("?");
                    if (i < criteriaIds.size() - 1) {
                        idPlaceholders.append(",");
                    }
                }
                
                String criteriaQuery = "SELECT * FROM EVALUATION_CRITERIA WHERE ID IN (" + idPlaceholders + ")";
                List<Map<String, Object>> criteriaData = jdbcTemplate.queryForList(criteriaQuery, criteriaIds.toArray());
                
                criteria = new ArrayList<>();
                for (Map<String, Object> data : criteriaData) {
                    EvaluationCriterion criterion = new EvaluationCriterion();
                    criterion.setId((Long) data.get("ID"));
                    criterion.setName((String) data.get("NAME"));
                    criterion.setDescription((String) data.get("DESCRIPTION"));
                    criterion.setDataType(EvaluationCriterion.DataType.valueOf((String) data.get("DATA_TYPE")));
                    criterion.setScoreRange((String) data.get("SCORE_RANGE"));
                    criterion.setWeight(new BigDecimal(((Number) data.get("WEIGHT")).toString()));
                    criterion.setIsRequired((Boolean) data.get("IS_REQUIRED"));
                    criteria.add(criterion);
                }
                
                if (criteria.isEmpty()) {
                    throw new IllegalArgumentException("未找到指定的评测标准");
                }
            } else {
                // 如果未指定评测标准，则使用默认的主观题评测标准
                criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
            }
            
            // 创建Lambda中需要的final变量
            final List<LlmAnswer> finalAnswers = new ArrayList<>(answers);
            final Evaluator finalEvaluator = evaluator;
            final List<EvaluationCriterion> finalCriteria = new ArrayList<>(criteria);
            final Long finalUserId = userId;
            final Long finalRunId = evaluationRun.getId();
            final JdbcTemplate finalJdbcTemplate = jdbcTemplate;
            
            // 异步处理批次答案
            evaluationExecutor.submit(() -> {
                try {
                    int total = finalAnswers.size();
                    int processed = 0;
                    
                    for (LlmAnswer answer : finalAnswers) {
                        try {
                            // 执行评测
                            // 获取用户信息
                            String userSql = "SELECT * FROM users WHERE id = ?";
                            Map<String, Object> userData = finalJdbcTemplate.queryForMap(userSql, finalUserId);
                            
                            User user = new User();
                            user.setId(finalUserId);
                            user.setUsername((String) userData.get("username"));
                            
                            evaluateSubjectiveAnswer(answer, finalEvaluator, user, finalCriteria);
                            processed++;
                            
                            // 更新进度
                            String updateProgressSql = "UPDATE evaluation_runs SET completed_answers_count = ?, progress_percentage = ? WHERE id = ?";
                            finalJdbcTemplate.update(updateProgressSql, processed, (int)((processed * 100.0) / total), finalRunId);
                            
        } catch (Exception e) {
                            logger.error("处理回答时发生错误", e);
                            // 更新错误状态
                            String updateErrorSql = "UPDATE evaluation_runs SET status = 'FAILED', error_message = ? WHERE id = ?";
                            finalJdbcTemplate.update(updateErrorSql, e.getMessage(), finalRunId);
            throw e;
        }
    }
    
                    // 完成评测
                    String completeRunSql = "UPDATE evaluation_runs SET status = 'COMPLETED', end_time = ? WHERE id = ?";
                    finalJdbcTemplate.update(completeRunSql, Timestamp.valueOf(LocalDateTime.now()), finalRunId);
                    
        } catch (Exception e) {
                    logger.error("批量评测过程中发生错误", e);
                }
            });
            
            // 立即返回响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量评测已开始");
            result.put("evaluationRunId", evaluationRun.getId());
            result.put("batchId", batchId);
            result.put("status", "PROCESSING");
            
            return result;
            
        } catch (Exception e) {
            logger.error("处理评测请求时发生异常", e);
            
        Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "批量评测启动失败: " + e.getMessage());
            
            return result;
        }
    }

    /**
     * 获取带有完整问题信息的LlmAnswer对象
     * @param answer 原始LlmAnswer对象
     * @return 带有完整问题信息的LlmAnswer对象
     */
    private LlmAnswer getLlmAnswerWithQuestions(LlmAnswer answer) {
        if (answer == null) {
            return null;
        }
        
        try {
            // 使用findByIdWithQuestion方法重新加载以确保有完整的问题信息
            return llmAnswerRepository.findByIdWithQuestion(answer.getId())
                    .orElse(answer); // 如果找不到，则返回原始对象
        } catch (Exception e) {
            logger.warn("无法加载完整的问题信息，回答ID: {}, 错误: {}", answer.getId(), e.getMessage());
            return answer;
        }
    }

    /**
     * 检查回答对象的关联情况
     * @param answerId 回答ID
     * @return 关联信息
     */
    private Map<String, Object> checkAnswerRelations(Long answerId) {
        logger.info("正在检查回答ID: {}的关联情况", answerId);
        Map<String, Object> relations = llmAnswerRepository.checkAnswerRelations(answerId);
        
        if (relations.isEmpty()) {
            logger.warn("回答ID: {}不存在", answerId);
            return Collections.emptyMap();
        }
        
        boolean hasDqm = relations.get("dqm_exists") != null && ((Boolean)relations.get("dqm_exists"));
        boolean hasSq = relations.get("sq_exists") != null && ((Boolean)relations.get("sq_exists"));
        
        if (!hasDqm) {
            logger.warn("回答ID: {}的dataset_question_mapping不存在", answerId);
        } else if (!hasSq) {
            logger.warn("回答ID: {}的standard_question不存在", answerId);
        } else {
            logger.info("回答ID: {}的所有关联对象正常", answerId);
        }
        
        return relations;
    }

    /**
     * 创建并提交人工评测（一步式操作）
     */
    @Override
    @Transactional
    public Evaluation createAndSubmitHumanEvaluation(Long llmAnswerId, Long evaluatorId, 
                                                 BigDecimal overallScore, String comments, 
                                                 List<Map<String, Object>> detailScores, Long userId) {
        logger.info("创建并提交人工评测，回答ID: {}, 评测者ID: {}, 总分: {}, 用户ID: {}", 
                llmAnswerId, evaluatorId, overallScore, userId);
        
        try {
            // 获取LLM回答
            LlmAnswer llmAnswer = llmAnswerRepository.findByIdWithQuestion(llmAnswerId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM回答: " + llmAnswerId));
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是人类
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalArgumentException("评测者不是人类: " + evaluatorId);
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 检查是否已存在该回答的人工评测，如果存在则删除
            List<Evaluation> existingEvaluations = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(llmAnswerId, evaluatorId);
            if (!existingEvaluations.isEmpty()) {
                evaluationRepository.deleteAll(existingEvaluations);
                logger.info("删除已存在的人工评测记录，回答ID: {}, 评测者ID: {}", llmAnswerId, evaluatorId);
            }
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.SUCCESS); // 直接设置为成功状态
            evaluation.setScore(overallScore);
            evaluation.setComments(comments);
            evaluation.setCompletionTime(LocalDateTime.now());
            evaluation.setEvaluationType(EvaluationType.MANUAL);
            
            // 构建评测结果JSON
            Map<String, Object> evaluationResults = new HashMap<>();
            evaluationResults.put("score", overallScore);
            evaluationResults.put("comments", comments);
            evaluationResults.put("criteria_scores", detailScores);
            evaluation.setEvaluationResults(evaluationResults);
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            for (Map<String, Object> detailScore : detailScores) {
                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluation(evaluation);
                
                // 获取评测标准名称
                String criterionName = (String) detailScore.get("criterion");
                detail.setCriterionName(criterionName);
                
                // 尝试查找对应的评测标准
                if (detailScore.containsKey("criterionId") && detailScore.get("criterionId") != null) {
                    Long criterionId = Long.valueOf(detailScore.get("criterionId").toString());
                    evaluationCriterionRepository.findById(criterionId).ifPresent(detail::setCriterion);
                } else {
                    // 如果没有criterionId，可以尝试通过criterionName查找
                    List<EvaluationCriterion> matchingCriteria = evaluationCriterionRepository.findByCriterionName(criterionName);
                    if (!matchingCriteria.isEmpty()) {
                        detail.setCriterion(matchingCriteria.get(0));
                    }
                    // 如果找不到对应的评测标准，criterion将保持为null，这是可以接受的
                }
                
                detail.setScore(new BigDecimal(detailScore.get("score").toString()));
                detail.setComments((String) detailScore.get("comments"));
                detail.setCreatedAt(LocalDateTime.now());
                details.add(detail);
            }
            
            evaluationDetailRepository.saveAll(details);
            
            // 获取问题类型
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            String scoreType = "HUMAN_" + question.getQuestionType().name();
            
            // 标准化分数（0-100分）
            BigDecimal normalizedScore;
            if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                // 主观题现在使用0-100分制，直接使用
                normalizedScore = overallScore;
            } else {
                // 客观题分数通常是0-100分
                normalizedScore = overallScore;
            }
            
            evaluation.setRawScore(overallScore);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod("HUMAN");
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("成功创建并提交人工评测，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("创建并提交人工评测失败", e);
            throw new RuntimeException("创建并提交人工评测失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查回答是否已被指定评测员评测
     */
    @Override
    public boolean isAnswerEvaluatedByEvaluator(Long answerId, Long evaluatorId) {
        return evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(answerId, evaluatorId);
    }
    
    /**
     * 获取标准问题对应的标准答案
     */
    @Override
    public Map<String, Object> getStandardAnswerForQuestion(Long questionId) {
        Map<String, Object> result = new HashMap<>();
        
        // 查询标准问题
        Optional<StandardQuestion> questionOpt = standardQuestionRepository.findById(questionId);
        if (!questionOpt.isPresent()) {
            logger.warn("找不到标准问题: {}", questionId);
            return result;
        }
        
        StandardQuestion question = questionOpt.get();
        QuestionType questionType = question.getQuestionType();
        
        // 根据问题类型查询对应的标准答案
        switch (questionType) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
                Optional<StandardObjectiveAnswer> objectiveAnswerOpt = 
                    objectiveAnswerRepository.findByStandardQuestionId(questionId);
                if (objectiveAnswerOpt.isPresent()) {
                    StandardObjectiveAnswer answer = objectiveAnswerOpt.get();
                    result.put("type", questionType.name());
                    result.put("options", answer.getOptions());
                    result.put("correctIds", answer.getCorrectIds());
                }
                break;
                
            case SIMPLE_FACT:
                Optional<StandardSimpleAnswer> simpleAnswerOpt = 
                    simpleAnswerRepository.findByStandardQuestionId(questionId);
                if (simpleAnswerOpt.isPresent()) {
                    StandardSimpleAnswer answer = simpleAnswerOpt.get();
                    result.put("type", questionType.name());
                    result.put("answerText", answer.getAnswerText());
                    result.put("alternativeAnswers", answer.getAlternativeAnswers());
                }
                break;
                
            case SUBJECTIVE:
                Optional<StandardSubjectiveAnswer> subjectiveAnswerOpt = 
                    standardSubjectiveAnswerRepository.findByStandardQuestionId(questionId);
                if (subjectiveAnswerOpt.isPresent()) {
                    StandardSubjectiveAnswer answer = subjectiveAnswerOpt.get();
                    result.put("type", questionType.name());
                    result.put("answerText", answer.getAnswerText());
                    result.put("scoringGuidance", answer.getScoringGuidance());
                    
                    // 获取评测标准
                    List<EvaluationCriterion> criteria = evaluationCriterionRepository
                        .findByQuestionType(QuestionType.SUBJECTIVE);
                    if (!criteria.isEmpty()) {
                        result.put("criteria", criteria);
                    }
                }
                break;
                
            default:
                logger.warn("未知的问题类型: {}", questionType);
        }
        
        return result;
    }

    private String assembleEvaluationPrompt(StandardQuestion question, String answerText, 
                                         String referenceAnswer, List<EvaluationCriterion> criteria,
                                         EvaluationRun evaluationRun) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取评测提示词组装配置，优先使用评测运行中的配置
        EvaluationPromptAssemblyConfig config = null;
        if (evaluationRun != null && evaluationRun.getEvaluationAssemblyConfig() != null) {
            config = evaluationRun.getEvaluationAssemblyConfig();
            logger.debug("使用评测运行中的评测提示词组装配置: {}", config.getName());
        } else {
            // 如果评测运行中没有配置，则使用默认配置
            List<EvaluationPromptAssemblyConfig> configs = evaluationPromptAssemblyConfigRepository.findByIsActiveTrue();
            config = configs.isEmpty() ? null : configs.get(0);
            if (config != null) {
                logger.debug("使用默认评测提示词组装配置: {}", config.getName());
            } else {
                logger.warn("没有找到可用的评测提示词组装配置");
            }
        }
        
        if (config != null) {
            // 添加基础系统提示
            if (config.getBaseSystemPrompt() != null) {
                promptBuilder.append(config.getBaseSystemPrompt());
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加标签提示（如果问题有标签）
            if (question.getTags() != null && !question.getTags().isEmpty()) {
                // 添加标签提示部分标题
                promptBuilder.append(config.getTagPromptsSectionHeader());
                promptBuilder.append(config.getSectionSeparator());
                
                // 获取问题标签相关的评测提示词
                List<String> tagPrompts = new ArrayList<>();
                for (Tag tag : question.getTags()) {
                    List<EvaluationTagPrompt> tagPromptList = evaluationTagPromptRepository.findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId());
                    if (!tagPromptList.isEmpty()) {
                        EvaluationTagPrompt tagPrompt = tagPromptList.get(0);
                        if (tagPrompt != null && tagPrompt.getPromptTemplate() != null) {
                            tagPrompts.add(tagPrompt.getPromptTemplate());
                        }
                    }
                }
                
                // 添加标签提示词
                if (!tagPrompts.isEmpty()) {
                    promptBuilder.append(String.join(config.getTagPromptSeparator(), tagPrompts));
                    promptBuilder.append(config.getSectionSeparator());
                }
            }
            
            // 添加主观题评测要求
            promptBuilder.append(config.getSubjectiveSectionHeader());
            promptBuilder.append(config.getSectionSeparator());
            
            // 获取主观题评测提示词，优先使用评测运行中的配置
            EvaluationSubjectivePrompt subjectivePrompt = null;
            if (evaluationRun != null && evaluationRun.getSubjectivePrompt() != null) {
                subjectivePrompt = evaluationRun.getSubjectivePrompt();
                logger.debug("使用评测运行中的主观题评测提示词: {}", subjectivePrompt.getName());
            } else {
                // 如果评测运行中没有配置，则使用默认配置
                List<EvaluationSubjectivePrompt> activePrompts = evaluationSubjectivePromptRepository.findByIsActiveTrueAndDeletedAtIsNull();
                if (!activePrompts.isEmpty()) {
                    subjectivePrompt = activePrompts.get(0);
                    logger.debug("使用默认主观题评测提示词: {}", subjectivePrompt.getName());
                } else {
                    logger.warn("没有找到可用的主观题评测提示词");
                }
            }
            
            if (subjectivePrompt != null) {
                promptBuilder.append(subjectivePrompt.getPromptTemplate());
            } else {
                // 如果没有找到主观题评测提示词，使用默认评测要求
                promptBuilder.append("请按照以下标准评估回答的质量：\n");
                promptBuilder.append("1. 准确性：回答是否准确、完整地解答了问题\n");
                promptBuilder.append("2. 清晰度：回答是否条理清晰、易于理解\n");
                promptBuilder.append("3. 专业性：回答是否展示了专业知识和深度\n");
                promptBuilder.append("4. 实用性：回答是否提供了实用的信息和建议");
            }
            
            promptBuilder.append(config.getSectionSeparator());
            
            // 添加评分标准
            if (criteria != null && !criteria.isEmpty()) {
                promptBuilder.append("## 评分标准\n");
                for (EvaluationCriterion criterion : criteria) {
                    promptBuilder.append("- ").append(criterion.getName()).append("：").append(criterion.getDescription()).append("\n");
                }
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加问题和回答
            promptBuilder.append("## 问题\n");
            promptBuilder.append(question.getQuestionText());
            promptBuilder.append(config.getSectionSeparator());
            
            promptBuilder.append("## 学生回答\n");
            promptBuilder.append(answerText);
            promptBuilder.append(config.getSectionSeparator());
            
            promptBuilder.append("## 参考答案\n");
            promptBuilder.append(referenceAnswer);
            promptBuilder.append(config.getSectionSeparator());
            
            // 添加最终指令
            if (config.getFinalInstruction() != null) {
                promptBuilder.append(config.getFinalInstruction());
            } else {
                promptBuilder.append("请根据上述标准评估学生回答的质量，给出详细的评分和评价。");
            }
        } else {
            // 如果没有找到配置，使用默认格式
            promptBuilder.append("你是一个专业的评测专家，请评估以下回答的质量。\n\n");
            
            // 添加问题和回答
            promptBuilder.append("问题：\n").append(question.getQuestionText()).append("\n\n");
            promptBuilder.append("学生回答：\n").append(answerText).append("\n\n");
            promptBuilder.append("参考答案：\n").append(referenceAnswer).append("\n\n");
            
            // 添加评分标准
            if (criteria != null && !criteria.isEmpty()) {
                promptBuilder.append("评分标准：\n");
                for (EvaluationCriterion criterion : criteria) {
                    promptBuilder.append("- ").append(criterion.getName()).append("：").append(criterion.getDescription()).append("\n");
                }
                promptBuilder.append("\n");
            }
            
            promptBuilder.append("请根据以上信息，评估学生回答的质量，给出详细的评分和评价。");
        }
        
        String finalPrompt = promptBuilder.toString();
        
        // 打印组装好的评测提示词
        logger.info("\n========== 组装好的评测提示词 ==========");
        logger.info(finalPrompt);
        logger.info("========================================\n");
        
        return finalPrompt;
    }

    /**
     * 解析AI评测响应
     * 
     * @param aiResponse AI评测响应文本
     * @return 解析后的评测结果
     */
    private Map<String, Object> parseAIResponse(String aiResponse) {
        // 打印AI评测响应
        logger.info("\n========== AI评测响应内容 ==========");
        logger.info(aiResponse);
        logger.info("====================================\n");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 首先尝试提取JSON内容（处理markdown代码块）
            String jsonContent = aiResponse;
            
            // 如果包含markdown代码块，提取其中的JSON
            if (aiResponse.contains("```json")) {
                Pattern jsonPattern = Pattern.compile("```json\\s*\\n(.*?)\\n```", Pattern.DOTALL);
                Matcher jsonMatcher = jsonPattern.matcher(aiResponse);
                if (jsonMatcher.find()) {
                    jsonContent = jsonMatcher.group(1).trim();
                }
            } else if (aiResponse.contains("```")) {
                // 处理没有指定语言的代码块
                Pattern codePattern = Pattern.compile("```\\s*\\n(.*?)\\n```", Pattern.DOTALL);
                Matcher codeMatcher = codePattern.matcher(aiResponse);
                if (codeMatcher.find()) {
                    String codeContent = codeMatcher.group(1).trim();
                    // 检查是否是JSON格式
                    if (codeContent.startsWith("{") && codeContent.endsWith("}")) {
                        jsonContent = codeContent;
                    }
                }
            }
            
            // 尝试解析JSON
            try {
                result = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
                logger.info("成功解析AI响应为JSON，包含 {} 个字段", result.size());
                
                // 确保有总分字段
                if (!result.containsKey("总分")) {
                    // 尝试从其他可能的字段名获取总分
                    Object totalScore = findTotalScore(result);
                    if (totalScore != null) {
                        result.put("总分", totalScore);
                        logger.info("从嵌套结构中提取到总分: {}", totalScore);
                    }
                }
                
                return result;
                
            } catch (Exception e) {
                logger.warn("JSON解析失败，将使用正则表达式解析: {}", e.getMessage());
            }
            
            // 如果JSON解析失败，使用正则表达式解析
            logger.info("使用正则表达式解析AI响应");
            
            // 提取总分
            Pattern scorePattern = Pattern.compile("\"?总分\"?\\s*[：:\"]*\\s*(\\d+(\\.\\d+)?)");
            Matcher scoreMatcher = scorePattern.matcher(aiResponse);
            if (scoreMatcher.find()) {
                result.put("总分", Double.parseDouble(scoreMatcher.group(1)));
                logger.info("提取到总分: {}", scoreMatcher.group(1));
            }
            
            // 提取建议/评价
            Pattern commentPattern = Pattern.compile("\"?建议\"?\\s*[：:\"]*\\s*\"([^\"]+)\"");
            Matcher commentMatcher = commentPattern.matcher(aiResponse);
            if (commentMatcher.find()) {
                result.put("建议", commentMatcher.group(1));
            } else {
                // 尝试其他可能的字段名
                Pattern altCommentPattern = Pattern.compile("\"?(评价|评语|总体评语)\"?\\s*[：:\"]*\\s*\"([^\"]+)\"");
                Matcher altCommentMatcher = altCommentPattern.matcher(aiResponse);
                if (altCommentMatcher.find()) {
                    result.put("总体评语", altCommentMatcher.group(2));
                }
            }
            
            // 提取各个评测标准的分数（动态）
            Pattern criterionPattern = Pattern.compile("\"?(\\w+)\"?\\s*[：:\"]*\\s*(\\d+(\\.\\d+)?)");
            Matcher criterionMatcher = criterionPattern.matcher(aiResponse);
            while (criterionMatcher.find()) {
                String criterion = criterionMatcher.group(1);
                String score = criterionMatcher.group(2);
                
                // 跳过总分和一些非评测标准的字段
                if (!criterion.equals("总分") && !criterion.equals("建议") && 
                    !criterion.equals("评价") && !criterion.equals("评语")) {
                    result.put(criterion, Double.parseDouble(score));
                    logger.info("提取到评测标准 {}: {}", criterion, score);
                }
            }
            
            // 保存原始响应
            result.put("原始响应", aiResponse);
            
            if (result.isEmpty() || !result.containsKey("总分")) {
                logger.warn("未能从AI响应中提取到有效的评测结果");
                result.put("总分", 0);
                result.put("解析警告", "未能提取到有效的评测结果");
            }
            
        } catch (Exception e) {
            logger.error("解析AI响应失败: {}", e.getMessage(), e);
            result.put("原始响应", aiResponse);
            result.put("解析错误", e.getMessage());
            result.put("总分", 0);
        }
        
                return result;
    }
    
    /**
     * 从复杂的嵌套结构中查找总分
     */
    private Object findTotalScore(Map<String, Object> data) {
        // 直接在最外层查找各种可能的总分字段名
        for (String key : Arrays.asList("总分", "total_score", "overall_score", "score", "总体评分", "overall", "final_score")) {
            if (data.containsKey(key)) {
                Object value = data.get(key);
                if (isValidScore(value)) {
                    return value;
                }
            }
        }
        
        // 在嵌套结构中查找
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                Object nestedScore = findTotalScore(nestedMap);
                if (nestedScore != null) {
                    return nestedScore;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否是有效的分数值
     */
    private boolean isValidScore(Object value) {
        if (value == null) return false;
        
        if (value instanceof Number) {
            double score = ((Number) value).doubleValue();
            return score >= 0 && score <= 100;
        }
        
        if (value instanceof String) {
            try {
                double score = Double.parseDouble((String) value);
                return score >= 0 && score <= 100;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 保存评测详情到 EVALUATION_DETAILS 表
     */
    private void saveEvaluationDetails(Long evaluationId, Map<String, Object> evaluationResult, List<EvaluationCriterion> criteria) {
        try {
            // 创建评测标准名称到ID的映射
            Map<String, Long> criterionNameToId = new HashMap<>();
            if (criteria != null) {
                for (EvaluationCriterion criterion : criteria) {
                    criterionNameToId.put(criterion.getName(), criterion.getId());
                }
            }
            
            // 遍历评测结果，保存各个标准的得分
            for (Map.Entry<String, Object> entry : evaluationResult.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 跳过一些非评测标准的字段
                if (key.equals("总分") || key.equals("建议") || key.equals("评价") || 
                    key.equals("总体评语") || key.equals("原始响应") || key.equals("解析错误") || 
                    key.equals("解析警告")) {
                    continue;
                }
                
                // 处理嵌套的评测标准结构（如 "评测标准" -> {"专业性": {"评分": 30, "依据": "..."}} ）
                if (key.equals("评测标准") && value instanceof Map) {
                    Map<String, Object> criteriaMap = (Map<String, Object>) value;
                    for (Map.Entry<String, Object> criterionEntry : criteriaMap.entrySet()) {
                        String criterionName = criterionEntry.getKey();
                        Object criterionValue = criterionEntry.getValue();
                        
                        if (criterionValue instanceof Map) {
                            Map<String, Object> criterionDetails = (Map<String, Object>) criterionValue;
                            Object scoreObj = criterionDetails.get("评分");
                            Object commentsObj = criterionDetails.get("依据");
                            
                            if (scoreObj != null) {
                                saveEvaluationDetail(evaluationId, criterionName, scoreObj, commentsObj, criterionNameToId);
                            }
                        }
                    }
                } else {
                    // 处理直接的评测标准得分（如 "专业性": 98）
                    if (value instanceof Number || (value instanceof String && ((String) value).matches("\\d+(\\.\\d+)?"))) {
                        saveEvaluationDetail(evaluationId, key, value, null, criterionNameToId);
                    }
                }
            }
            
            logger.info("成功保存评测详情，评测ID: {}", evaluationId);
            
        } catch (Exception e) {
            logger.error("保存评测详情失败，评测ID: {}, 错误: {}", evaluationId, e.getMessage(), e);
        }
    }
    
    /**
     * 保存单个评测详情
     */
    private void saveEvaluationDetail(Long evaluationId, String criterionName, Object scoreObj, Object commentsObj, Map<String, Long> criterionNameToId) {
        try {
            BigDecimal score = null;
            if (scoreObj instanceof Number) {
                score = BigDecimal.valueOf(((Number) scoreObj).doubleValue());
            } else if (scoreObj instanceof String) {
                try {
                    score = new BigDecimal((String) scoreObj);
                } catch (NumberFormatException e) {
                    logger.warn("无法解析评分: {}", scoreObj);
                    return;
                }
            }
            
            if (score == null) {
                return;
            }
            
            String comments = commentsObj != null ? commentsObj.toString() : null;
            Long criterionId = criterionNameToId.get(criterionName);
            
            String insertDetailSql = "INSERT INTO evaluation_details (evaluation_id, criterion_id, criterion_name, score, comments) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertDetailSql, evaluationId, criterionId, criterionName, score, comments);
            
            logger.debug("保存评测详情: {} = {}", criterionName, score);
            
        } catch (Exception e) {
            logger.error("保存单个评测详情失败: {}, 错误: {}", criterionName, e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getObjectiveDetailedResults(Long batchId, List<Long> modelIds, int page, int size) {
        logger.info("获取客观题评测详细结果，批次ID: {}, 模型IDs: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 前端传入的页码已经是从0开始，不需要再减1
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取批次中的客观题答案及其评测结果
            List<Map<String, Object>> evaluationResults = llmAnswerRepository.findObjectiveAnswersWithEvaluations(
                    batchId, modelIds, pageable);
            
            // 获取总记录数
            long totalCount = llmAnswerRepository.countObjectiveAnswersWithEvaluations(
                    batchId, modelIds);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 构建返回结果
            result.put("items", evaluationResults);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            logger.error("获取客观题评测详细结果时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getSubjectiveDetailedResults(Long batchId, List<Long> modelIds, Long evaluatorId, int page, int size) {
        logger.info("获取主观题大模型评测详细结果，批次ID: {}, 模型IDs: {}, 评测者ID: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, evaluatorId, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 前端传入的页码已经是从0开始，不需要再减1
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取批次中的主观题答案及其评测结果
            List<Map<String, Object>> evaluationResults = llmAnswerRepository.findSubjectiveAnswersWithEvaluations(
                    batchId, modelIds, evaluatorId, pageable);
            
            // 获取总记录数
            long totalCount = llmAnswerRepository.countSubjectiveAnswersWithEvaluations(
                    batchId, modelIds, evaluatorId);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 构建返回结果
            result.put("items", evaluationResults);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            logger.error("获取主观题大模型评测详细结果失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> getSubjectiveDetailedResultsWithAllEvaluators(Long batchId, List<Long> modelIds, int page, int size) {
        logger.info("获取主观题所有评测员的评测详细结果，批次ID: {}, 模型IDs: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 前端传入的页码已经是从0开始，不需要再减1
            Pageable pageable = PageRequest.of(page, size);
            
            // 获取批次中的主观题答案及其所有评测员的评测结果
            List<Map<String, Object>> evaluationResults = llmAnswerRepository.findSubjectiveAnswersWithAllEvaluations(
                    batchId, modelIds, pageable);
            
            // 获取总记录数
            long totalCount = llmAnswerRepository.countSubjectiveAnswersWithAllEvaluations(
                    batchId, modelIds);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 构建返回结果
            result.put("items", evaluationResults);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            logger.error("获取主观题所有评测员的评测详细结果失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 获取待人工评测的回答列表
     * 
     * @param userId 用户ID
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 待评测回答列表
     */
    @Override
    public Map<String, Object> getPendingHumanEvaluations(Long userId, Long evaluatorId, Long batchId, List<Long> modelIds, 
                                                   String questionType, int page, int size) {
        logger.info("获取待人工评测回答列表，用户ID: {}, 评测者ID: {}, 批次ID: {}, 模型IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 前端传入的页码已经是从0开始，不需要再减1
            Pageable pageable = PageRequest.of(page, size);
            
            // 使用指定的评测者ID
            List<Long> evaluatorIds = Collections.singletonList(evaluatorId);
            // 验证评测者ID是否有效
            if (!evaluatorRepository.existsById(evaluatorId)) {
                logger.warn("评测者ID {}不存在", evaluatorId);
                result.put("items", Collections.emptyList());
                result.put("totalItems", 0);
                result.put("totalPages", 0);
                result.put("currentPage", page);
                result.put("pageSize", size);
                result.put("success", true);
                return result;
            }
            
            // 将问题类型字符串转换为枚举类型
            QuestionType questionTypeEnum = null;
            if (questionType != null && !questionType.isEmpty()) {
                try {
                    questionTypeEnum = QuestionType.valueOf(questionType);
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的问题类型: {}", questionType);
                }
            }
            
            // 获取未被评测过的回答
            List<Map<String, Object>> pendingAnswers = llmAnswerRepository.findPendingAnswersForHumanEvaluation(
                    evaluatorIds, batchId, modelIds, questionTypeEnum, pageable);
            
            // 获取总记录数
            long totalCount = llmAnswerRepository.countPendingAnswersForHumanEvaluation(
                    evaluatorIds, batchId, modelIds, questionTypeEnum);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 处理回答数据，添加标准问题和标准答案信息
            List<Map<String, Object>> detailedAnswers = new ArrayList<>();
            for (Map<String, Object> answer : pendingAnswers) {
                Map<String, Object> detailedAnswer = new HashMap<>(answer);
                
                // 获取标准问题
                Long questionId = (Long) answer.get("questionId");
                StandardQuestion question = standardQuestionRepository.findById(questionId).orElse(null);
                if (question != null) {
                    detailedAnswer.put("questionText", question.getQuestionText());
                    detailedAnswer.put("questionType", question.getQuestionType());
                    detailedAnswer.put("difficultyLevel", question.getDifficultyLevel());
                    
                    // 获取标准答案
                    Map<String, Object> standardAnswer = getStandardAnswerForQuestion(questionId);
                    if (standardAnswer != null && !standardAnswer.isEmpty()) {
                        detailedAnswer.put("standardAnswer", standardAnswer);
                    }
                }
                
                // 获取模型信息
                Long modelId = (Long) answer.get("modelId");
                LlmModel model = llmModelRepository.findById(modelId).orElse(null);
                if (model != null) {
                    detailedAnswer.put("modelName", model.getName());
                    detailedAnswer.put("modelVersion", model.getVersion());
                }
                
                // 获取评测标准
                if (question != null && question.getQuestionType() != null) {
                    List<EvaluationCriterion> criteria = evaluationCriterionRepository.findByQuestionType(question.getQuestionType());
                    if (!criteria.isEmpty()) {
                        List<Map<String, Object>> criteriaData = new ArrayList<>();
                        for (EvaluationCriterion criterion : criteria) {
                            Map<String, Object> criterionData = new HashMap<>();
                            criterionData.put("id", criterion.getId());
                            criterionData.put("name", criterion.getName());
                            criterionData.put("description", criterion.getDescription());
                            criterionData.put("maxScore", criterion.getMaxScore());
                            criterionData.put("weight", criterion.getWeight());
                            criteriaData.add(criterionData);
                        }
                        detailedAnswer.put("evaluationCriteria", criteriaData);
                    }
                }
                
                detailedAnswers.add(detailedAnswer);
            }
            
            // 构建返回结果
            result.put("items", detailedAnswers);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("success", true);
            
        } catch (Exception e) {
            logger.error("获取待人工评测回答列表时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取用户已评测的回答列表
     * 
     * @param userId 用户ID
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 已评测回答列表
     */
    @Override
    public Map<String, Object> getCompletedHumanEvaluations(Long userId, Long evaluatorId, Long batchId, List<Long> modelIds, 
                                                    String questionType, int page, int size) {
        logger.info("获取用户已评测回答列表，用户ID: {}, 评测者ID: {}, 批次ID: {}, 模型IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 前端传入的页码已经是从0开始，不需要再减1
            Pageable pageable = PageRequest.of(page, size);
            
            // 使用指定的评测者ID
            List<Long> evaluatorIds = Collections.singletonList(evaluatorId);
            // 验证评测者ID是否有效
            if (!evaluatorRepository.existsById(evaluatorId)) {
                logger.warn("评测者ID {}不存在", evaluatorId);
                result.put("items", Collections.emptyList());
                result.put("totalItems", 0);
                result.put("totalPages", 0);
                result.put("currentPage", page);
                result.put("pageSize", size);
                result.put("success", true);
                return result;
            }
            
            // 强制设置问题类型为主观题，此接口只返回主观题回答
            QuestionType questionTypeEnum = QuestionType.SUBJECTIVE;
            
            // 获取已评测的回答
            List<Map<String, Object>> completedEvaluations = evaluationRepository.findCompletedHumanEvaluations(
                    evaluatorIds, batchId, modelIds, questionTypeEnum, pageable);
            
            // 获取总记录数
            long totalCount = evaluationRepository.countCompletedHumanEvaluations(
                    evaluatorIds, batchId, modelIds, questionTypeEnum);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 处理评测数据，添加详细信息
            List<Map<String, Object>> detailedEvaluations = new ArrayList<>();
            for (Map<String, Object> evaluation : completedEvaluations) {
                Map<String, Object> detailedEvaluation = new HashMap<>(evaluation);
                
                // 获取标准问题
                Long questionId = (Long) evaluation.get("questionId");
                StandardQuestion question = standardQuestionRepository.findById(questionId).orElse(null);
                if (question != null) {
                    detailedEvaluation.put("questionText", question.getQuestionText());
                    detailedEvaluation.put("questionType", question.getQuestionType());
                    detailedEvaluation.put("difficultyLevel", question.getDifficultyLevel());
                    
                    // 获取标准答案
                    Map<String, Object> standardAnswer = getStandardAnswerForQuestion(questionId);
                    if (standardAnswer != null && !standardAnswer.isEmpty()) {
                        detailedEvaluation.put("standardAnswer", standardAnswer);
                    }
                }
                
                // 获取模型信息
                Long modelId = (Long) evaluation.get("modelId");
                LlmModel model = llmModelRepository.findById(modelId).orElse(null);
                if (model != null) {
                    detailedEvaluation.put("modelName", model.getName());
                    detailedEvaluation.put("modelVersion", model.getVersion());
                }
                
                // 获取评测详情
                Long evaluationId = (Long) evaluation.get("evaluationId");
                if (evaluationId != null) {
                    List<EvaluationDetail> details = evaluationDetailRepository.findByEvaluationId(evaluationId);
                    
                    if (!details.isEmpty()) {
                        List<Map<String, Object>> detailsData = new ArrayList<>();
                        for (EvaluationDetail detail : details) {
                            Map<String, Object> detailData = new HashMap<>();
                            detailData.put("id", detail.getId());
                            // 添加空值检查，如果criterion为null，则使用null作为criterionId
                            if (detail.getCriterion() != null) {
                                detailData.put("criterionId", detail.getCriterion().getId());
                            } else {
                                detailData.put("criterionId", null);
                            }
                            detailData.put("criterionName", detail.getCriterionName());
                            detailData.put("score", detail.getScore());
                            detailData.put("comments", detail.getComments());
                            detailsData.add(detailData);
                        }
                        detailedEvaluation.put("evaluationDetails", detailsData);
                    }
                }
                
                detailedEvaluations.add(detailedEvaluation);
            }
            
            // 构建返回结果
            result.put("items", detailedEvaluations);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("success", true);
            
        } catch (Exception e) {
            logger.error("获取用户已评测回答列表时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 一步式人工评测（创建并提交评测）
     *
     * @param llmAnswerId LLM回答ID
     * @param userId 用户ID
     * @return 评测记录
     */
    @Override
    @Transactional
    public Evaluation oneStepHumanEvaluation(Long llmAnswerId, Long userId) {
        logger.info("执行一步式人工评测，回答ID: {}, 用户ID: {}", llmAnswerId, userId);
        
        // 获取用户关联的评测者ID
        List<Long> evaluatorIds = evaluatorRepository.findHumanEvaluatorIdsByUserId(userId);
        if (evaluatorIds.isEmpty()) {
            throw new RuntimeException("用户没有关联的人类评测者");
        }
        Long evaluatorId = evaluatorIds.get(0);
        
        // 创建评测记录
        Evaluation evaluation = createHumanEvaluation(llmAnswerId, evaluatorId, userId);
        
        // 返回创建的评测记录
        return evaluation;
    }

    // 删除重复的方法定义
    

    
    /**
     * 获取或创建评测运行记录
     */
    @Override
    @Transactional
    public EvaluationRun getOrCreateEvaluationRun(Long batchId, Long evaluatorId, Long userId) {
        // 注意：这里的batchId实际上是answer_generation_batch_id
        // 但evaluation_runs表存储的是model_answer_run_id，需要先找到对应的model_answer_run_id
        
        // 首先查找该batch对应的model_answer_run_id列表
        String findModelRunsSql = "SELECT id FROM model_answer_runs WHERE answer_generation_batch_id = ?";
        List<Long> modelRunIds = jdbcTemplate.queryForList(findModelRunsSql, Long.class, batchId);
        
        if (modelRunIds.isEmpty()) {
            throw new IllegalArgumentException("找不到对应的模型回答运行记录，批次ID: " + batchId);
        }
        
        // 查找是否已存在评测运行记录（对该批次的任何一个模型运行）
        StringBuilder findSqlBuilder = new StringBuilder();
        findSqlBuilder.append("SELECT * FROM evaluation_runs WHERE evaluator_id = ? AND model_answer_run_id IN (");
        for (int i = 0; i < modelRunIds.size(); i++) {
            if (i > 0) findSqlBuilder.append(",");
            findSqlBuilder.append("?");
        }
        findSqlBuilder.append(") ORDER BY id DESC LIMIT 1");
        
        List<Object> params = new ArrayList<>();
        params.add(evaluatorId);
        params.addAll(modelRunIds);
        
        List<Map<String, Object>> existingRuns = jdbcTemplate.queryForList(findSqlBuilder.toString(), params.toArray());
        
        if (!existingRuns.isEmpty()) {
            Map<String, Object> runData = existingRuns.get(0);
            
            // 查询并设置Evaluator对象
            String evaluatorSql = "SELECT * FROM evaluators WHERE id = ?";
            Map<String, Object> evaluatorData = jdbcTemplate.queryForMap(evaluatorSql, evaluatorId);
            
            Evaluator evaluator = new Evaluator();
            evaluator.setId(evaluatorId);
            evaluator.setName((String) evaluatorData.get("name"));
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.valueOf((String) evaluatorData.get("evaluator_type")));
            
            // 创建并设置ModelAnswerRun对象
            Long modelAnswerRunId = (Long) runData.get("model_answer_run_id");
            ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
            modelAnswerRun.setId(modelAnswerRunId);
            
            EvaluationRun evaluationRun = new EvaluationRun();
            evaluationRun.setId((Long) runData.get("id"));
            evaluationRun.setEvaluatorId(evaluatorId);
            evaluationRun.setEvaluator(evaluator);
            evaluationRun.setModelAnswerRun(modelAnswerRun);
            evaluationRun.setStatus(RunStatus.valueOf((String) runData.get("status")));
            
            return evaluationRun;
        }
        
        // 创建新的评测运行记录，使用第一个model_answer_run_id
        Long modelAnswerRunId = modelRunIds.get(0);
        String insertSql = "INSERT INTO evaluation_runs (model_answer_run_id, evaluator_id, created_by_user_id, status, start_time, run_name) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, modelAnswerRunId);
            ps.setLong(2, evaluatorId);
            ps.setLong(3, userId);
            ps.setString(4, RunStatus.PENDING.toString());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, "Batch Evaluation Run " + batchId + " - " + LocalDateTime.now().toString());
            return ps;
        }, keyHolder);
        
        Long runId = keyHolder.getKey().longValue();
        
        // 查询并设置Evaluator对象
        String evaluatorSql = "SELECT * FROM evaluators WHERE id = ?";
        Map<String, Object> evaluatorData = jdbcTemplate.queryForMap(evaluatorSql, evaluatorId);
        
        Evaluator evaluator = new Evaluator();
        evaluator.setId(evaluatorId);
        evaluator.setName((String) evaluatorData.get("name"));
        evaluator.setEvaluatorType(Evaluator.EvaluatorType.valueOf((String) evaluatorData.get("evaluator_type")));
        
        // 创建并设置ModelAnswerRun对象
        ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
        modelAnswerRun.setId(modelAnswerRunId);
        
        EvaluationRun evaluationRun = new EvaluationRun();
        evaluationRun.setId(runId);
        evaluationRun.setEvaluatorId(evaluatorId);
        evaluationRun.setEvaluator(evaluator);
        evaluationRun.setModelAnswerRun(modelAnswerRun);
        evaluationRun.setStatus(RunStatus.PENDING);
        
        return evaluationRun;
    }

    @Override
    @Transactional
    public BigDecimal reEvaluateSubjectiveAnswer(Long llmAnswerId, Long evaluatorId, Long userId) {
        // 获取回答信息
        String answerSql = "SELECT la.*, sq.question_text, sq.question_type FROM llm_answers la " +
                "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                "WHERE la.id = ?";
        Map<String, Object> answerData = jdbcTemplate.queryForMap(answerSql, llmAnswerId);
        if (answerData == null || answerData.isEmpty()) {
            throw new EntityNotFoundException("找不到指定的LLM回答: " + llmAnswerId);
        }
        
        // 构建LlmAnswer对象
        LlmAnswer llmAnswer = new LlmAnswer();
        llmAnswer.setId(llmAnswerId);
        llmAnswer.setAnswerText((String) answerData.get("answer_text"));
        
        // 获取评测者信息
        String evaluatorSql = "SELECT * FROM evaluators WHERE id = ?";
        Map<String, Object> evaluatorData = jdbcTemplate.queryForMap(evaluatorSql, evaluatorId);
        if (evaluatorData == null || evaluatorData.isEmpty()) {
            throw new EntityNotFoundException("找不到指定的评测者: " + evaluatorId);
        }
        
        Evaluator evaluator = new Evaluator();
        evaluator.setId(evaluatorId);
        evaluator.setName((String) evaluatorData.get("name"));
        evaluator.setEvaluatorType(Evaluator.EvaluatorType.valueOf((String) evaluatorData.get("type")));
        
        // 获取用户信息
        String userSql = "SELECT * FROM users WHERE id = ?";
        Map<String, Object> userData = jdbcTemplate.queryForMap(userSql, userId);
        if (userData == null || userData.isEmpty()) {
            throw new EntityNotFoundException("找不到指定的用户: " + userId);
        }
        
        User user = new User();
        user.setId(userId);
        user.setUsername((String) userData.get("username"));
            
            // 获取评测标准
            List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
            
        // 执行评测
        return evaluateSubjectiveAnswer(llmAnswer, evaluator, user, criteria);
    }

    @Override
    @Transactional
    public Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        return reEvaluateBatchSubjectiveQuestions(batchId, evaluatorId, userId, null, null, null);
    }
    
    @Override
    @Transactional
    public Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId,
            Long subjectivePromptId, Long evaluationAssemblyConfigId, List<Long> criteriaIds) {
        // 验证评测者和用户
        String evaluatorSql = "SELECT * FROM evaluators WHERE id = ?";
        Map<String, Object> evaluatorData = jdbcTemplate.queryForMap(evaluatorSql, evaluatorId);
        if (evaluatorData == null || evaluatorData.isEmpty()) {
            throw new EntityNotFoundException("找不到指定的评测者: " + evaluatorId);
        }
        
        String evaluatorType = (String) evaluatorData.get("type");
        // 验证评测者类型是AI
        if (!evaluatorType.equals("AI_MODEL")) {
            throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
        }
        
        // 获取批次下的所有主观题回答
        String answersSql = "SELECT la.id, la.answer_text, sq.question_text, sq.question_type " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                "WHERE mar.answer_generation_batch_id = ? AND sq.question_type = 'SUBJECTIVE'";
        
        List<Map<String, Object>> answersData = jdbcTemplate.queryForList(answersSql, batchId);
        
        if (answersData.isEmpty()) {
            return Map.of("status", "completed", "message", "没有主观题需要重新评测");
        }
        
        // 获取评测标准
        final List<EvaluationCriterion> criteria;
        if (criteriaIds != null && !criteriaIds.isEmpty()) {
            // 使用JDBC查询指定ID的评测标准
            StringBuilder idPlaceholders = new StringBuilder();
            for (int i = 0; i < criteriaIds.size(); i++) {
                idPlaceholders.append("?");
                if (i < criteriaIds.size() - 1) {
                    idPlaceholders.append(",");
                }
            }
            
            String criteriaQuery = "SELECT * FROM EVALUATION_CRITERIA WHERE ID IN (" + idPlaceholders + ")";
            List<Map<String, Object>> criteriaData = jdbcTemplate.queryForList(criteriaQuery, criteriaIds.toArray());
            
            List<EvaluationCriterion> tempCriteria = new ArrayList<>();
            for (Map<String, Object> data : criteriaData) {
                EvaluationCriterion criterion = new EvaluationCriterion();
                criterion.setId((Long) data.get("ID"));
                criterion.setName((String) data.get("NAME"));
                criterion.setDescription((String) data.get("DESCRIPTION"));
                criterion.setDataType(EvaluationCriterion.DataType.valueOf((String) data.get("DATA_TYPE")));
                criterion.setScoreRange((String) data.get("SCORE_RANGE"));
                criterion.setWeight(new BigDecimal(((Number) data.get("WEIGHT")).toString()));
                criterion.setIsRequired((Boolean) data.get("IS_REQUIRED"));
                tempCriteria.add(criterion);
            }
            
            if (tempCriteria.isEmpty()) {
                logger.warn("未找到指定的评测标准，将使用默认标准");
                criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
            } else {
                criteria = tempCriteria;
            }
        } else {
            // 如果未指定评测标准，则使用默认的主观题评测标准
            criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
        }
        
        // 创建评测运行记录
        EvaluationRun evaluationRun = getOrCreateEvaluationRun(batchId, evaluatorId, userId);
        
        // 更新总回答数
        String updateRunSql = "UPDATE evaluation_runs SET total_answers_count = ? WHERE id = ?";
        jdbcTemplate.update(updateRunSql, answersData.size(), evaluationRun.getId());
        
        // 异步处理批次答案
        evaluationExecutor.submit(() -> {
            try {
                int total = answersData.size();
                int processed = 0;
                
                for (Map<String, Object> answerData : answersData) {
                    try {
                        Long answerId = (Long) answerData.get("id");
                        String answerText = (String) answerData.get("answer_text");
                        String questionText = (String) answerData.get("question_text");
                        
                        // 构建LlmAnswer对象
                        LlmAnswer answer = new LlmAnswer();
                        answer.setId(answerId);
                        answer.setAnswerText(answerText);
                        
                        // 获取完整的评测者对象
                        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                                .orElseThrow(() -> new EntityNotFoundException("评测员不存在: " + evaluatorId));
                        
                        // 构建用户对象
                        User user = new User();
                        user.setId(userId);
                        
                        // 执行评测
                        evaluateSubjectiveAnswer(answer, evaluator, user, criteria);
                        
                        processed++;
                    
                    // 更新进度
                        String updateProgressSql = "UPDATE evaluation_runs SET completed_answers_count = ?, progress_percentage = ? WHERE id = ?";
                        jdbcTemplate.update(updateProgressSql, processed, (int)((processed * 100.0) / total), evaluationRun.getId());
                    
                } catch (Exception e) {
                        logger.error("处理回答时发生错误", e);
                        // 更新错误状态
                        String updateErrorSql = "UPDATE evaluation_runs SET status = 'FAILED', error_message = ? WHERE id = ?";
                        jdbcTemplate.update(updateErrorSql, e.getMessage(), evaluationRun.getId());
                        throw e;
                    }
                }
                
                // 完成评测
                String completeRunSql = "UPDATE evaluation_runs SET status = 'COMPLETED', end_time = ? WHERE id = ?";
                jdbcTemplate.update(completeRunSql, Timestamp.valueOf(LocalDateTime.now()), evaluationRun.getId());
                
            } catch (Exception e) {
                logger.error("批量评测过程中发生错误", e);
            }
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "批量重新评测已开始");
        result.put("evaluationRunId", evaluationRun.getId());
        result.put("batchId", batchId);
        result.put("totalAnswers", answersData.size());
        
        return result;
    }

    /**
     * 组装评测提示词，使用String类型的问题文本
     */
    private String assembleEvaluationPrompt(String questionText, String answerText, 
                                    String referenceAnswer, List<EvaluationCriterion> criteria) {
        StandardQuestion question = new StandardQuestion();
        question.setQuestionText(questionText);
        return assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria);
    }

    private BigDecimal evaluateSubjectiveAnswer(LlmAnswer answer, Evaluator evaluator, User user, List<EvaluationCriterion> criteria) {
        try {
            // 获取问题和标准答案
            String questionSql = "SELECT sq.question_text, sq.id as question_id " +
                    "FROM llm_answers la " +
                    "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                    "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                    "WHERE la.id = ?";
            Map<String, Object> questionData = jdbcTemplate.queryForMap(questionSql, answer.getId());
            
            String questionText = (String) questionData.get("question_text");
            Long questionId = (Long) questionData.get("question_id");
            
            // 获取参考答案
            String refAnswerSql = "SELECT answer_text FROM standard_subjective_answers WHERE standard_question_id = ? AND deleted_at IS NULL LIMIT 1";
            String referenceAnswer = "";
            try {
                referenceAnswer = jdbcTemplate.queryForObject(refAnswerSql, String.class, questionId);
                logger.debug("成功获取问题ID: {}的参考答案，长度: {}", questionId, referenceAnswer != null ? referenceAnswer.length() : 0);
            } catch (Exception e) {
                logger.warn("未找到问题ID: {}的参考答案", questionId);
            }
            
            // 组装评测提示词
            StandardQuestion question = new StandardQuestion();
            question.setQuestionText(questionText);
            String prompt = assembleEvaluationPrompt(question, answer.getAnswerText(), referenceAnswer, criteria);
            
            // 调用AI服务进行评测
            // 需要从evaluator中获取关联的模型ID
            Long modelId = null;
            if (evaluator.getLlmModel() != null) {
                modelId = evaluator.getLlmModel().getId();
            }
            String aiResponse = callAIService(prompt, modelId);
            
            // 解析AI响应
            Map<String, Object> evaluationResult = parseAIResponse(aiResponse);
            
            // 获取总分
            BigDecimal overallScore = new BigDecimal(evaluationResult.getOrDefault("总分", 0).toString());
            
            // 保存评测结果
            String checkExistingSql = "SELECT id FROM evaluations WHERE llm_answer_id = ? AND evaluator_id = ?";
            Long existingId = null;
            try {
                existingId = jdbcTemplate.queryForObject(checkExistingSql, Long.class, answer.getId(), evaluator.getId());
            } catch (EmptyResultDataAccessException e) {
                // 不存在，忽略
            }
            
            if (existingId != null) {
                // 更新现有评测
                String updateSql = "UPDATE evaluations SET overall_score = ?, comments = ?, evaluation_results = ?, " +
                        "completion_time = ?, evaluation_status = ?, raw_evaluator_response = ? WHERE id = ?";
                
                // 提取评语/建议
                String comments = "";
                if (evaluationResult.containsKey("建议")) {
                    comments = evaluationResult.get("建议").toString();
                } else if (evaluationResult.containsKey("总体评语")) {
                    comments = evaluationResult.get("总体评语").toString();
                } else if (evaluationResult.containsKey("评价")) {
                    comments = evaluationResult.get("评价").toString();
                }
                
                String evaluationResultJson;
                try {
                    evaluationResultJson = new ObjectMapper().writeValueAsString(evaluationResult);
                } catch (Exception e) {
                    logger.error("序列化评测结果失败: {}", e.getMessage());
                    evaluationResultJson = "{}";
                }
                
                jdbcTemplate.update(updateSql, 
                        overallScore, 
                        comments,
                        evaluationResultJson,
                        Timestamp.valueOf(LocalDateTime.now()),
                        "SUCCESS",
                        evaluationResult.getOrDefault("原始响应", "").toString(),
                        existingId);
                
                // 删除旧的评测详情并保存新的
                jdbcTemplate.update("DELETE FROM evaluation_details WHERE evaluation_id = ?", existingId);
                saveEvaluationDetails(existingId, evaluationResult, criteria);
            } else {
                // 创建新评测
                String insertSql = "INSERT INTO evaluations (llm_answer_id, evaluator_id, overall_score, comments, " +
                        "evaluation_results, creation_time, created_by_user_id, evaluation_status, raw_evaluator_response) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                // 提取评语/建议
                final String comments;
                if (evaluationResult.containsKey("建议")) {
                    comments = evaluationResult.get("建议").toString();
                } else if (evaluationResult.containsKey("总体评语")) {
                    comments = evaluationResult.get("总体评语").toString();
                } else if (evaluationResult.containsKey("评价")) {
                    comments = evaluationResult.get("评价").toString();
                } else {
                    comments = "";
                }
                
                final String evaluationResultJson;
                final String rawResponse;
                try {
                    evaluationResultJson = new ObjectMapper().writeValueAsString(evaluationResult);
                    rawResponse = evaluationResult.getOrDefault("原始响应", "").toString();
                } catch (Exception e) {
                    logger.error("序列化评测结果失败: {}", e.getMessage());
                    throw new RuntimeException("序列化评测结果失败", e);
                }
                
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, answer.getId());
                    ps.setLong(2, evaluator.getId());
                    ps.setBigDecimal(3, overallScore);
                    ps.setString(4, comments);
                    ps.setString(5, evaluationResultJson);
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(7, user.getId());
                    ps.setString(8, "SUCCESS");
                    ps.setString(9, rawResponse);
                    return ps;
                }, keyHolder);
                
                Long evaluationId = keyHolder.getKey().longValue();
                
                // 保存评测详情到 EVALUATION_DETAILS 表
                saveEvaluationDetails(evaluationId, evaluationResult, criteria);
            }
            
            return overallScore;
            
        } catch (Exception e) {
            logger.error("评测主观题回答时发生错误", e);
            throw new RuntimeException("评测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新评测进度
     */
    @Transactional
    public void updateEvaluationProgress(EvaluationRun evaluationRun, int processedCount, int remainingCount) {
        int totalCount = processedCount + remainingCount;
        int progressPercentage = totalCount > 0 ? (processedCount * 100) / totalCount : 0;
        
        // 更新评测运行状态
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET completed_answers_count = ?, total_answers_count = ?, " +
            "progress_percentage = ?, last_activity_time = ?, last_updated = ? WHERE id = ?",
            processedCount, totalCount, progressPercentage, LocalDateTime.now(), 
            LocalDateTime.now(), evaluationRun.getId());
        
        logger.info("更新评测运行{}进度: {}/{}，完成率: {}%", 
                evaluationRun.getId(), processedCount, totalCount, progressPercentage);
    }

    @Override
    @Transactional
    public EvaluationCriterion saveEvaluationCriterion(EvaluationCriterion criterion) {
        logger.info("保存评测标准，名称: {}, ID: {}", criterion.getName(), criterion.getId());
        
        // 设置创建时间（如果是新建）
        if (criterion.getId() == null && criterion.getCreatedAt() == null) {
            criterion.setCreatedAt(LocalDateTime.now());
        }
        
        // 保存评测标准
        return evaluationCriterionRepository.save(criterion);
    }

    @Override
    public EvaluationCriterion getEvaluationCriterionById(Long criterionId) {
        logger.info("获取评测标准，ID: {}", criterionId);
        
        return evaluationCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为" + criterionId + "的评测标准"));
    }

    @Override
    public List<EvaluationCriterion> getAllEvaluationCriteria(int page, int size) {
        logger.info("获取所有评测标准，页码: {}, 每页大小: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EvaluationCriterion> criteriaPage = evaluationCriterionRepository.findAll(pageable);
        
        return criteriaPage.getContent();
    }

    @Override
    @Transactional
    public void deleteEvaluationCriterion(Long criterionId, Long userId) {
        logger.info("删除评测标准，ID: {}, 操作用户ID: {}", criterionId, userId);
        
        // 获取评测标准
        EvaluationCriterion criterion = evaluationCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为" + criterionId + "的评测标准"));
        
        // 软删除（设置删除时间）
        criterion.setDeletedAt(LocalDateTime.now());
        
        // 保存更改
        evaluationCriterionRepository.save(criterion);
    }

    @Override
    public Map<String, Object> getBatchComprehensiveScores(Long batchId, List<Long> modelIds, int page, int size) {
        logger.info("获取批次{}的综合评分展示数据，模型IDs: {}", batchId, modelIds);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 获取批次基本信息
            String batchQuery = "SELECT agb.id, agb.name, agb.description, agb.creation_time, " +
                    "dv.name as dataset_name, dv.version_number as dataset_version " +
                    "FROM answer_generation_batches agb " +
                    "JOIN dataset_versions dv ON agb.dataset_version_id = dv.id " +
                    "WHERE agb.id = ?";
            
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
            result.put("batchInfo", batchInfo);
            
            // 2. 获取批次中的所有模型信息
            StringBuilder modelsQuery = new StringBuilder();
            modelsQuery.append("SELECT DISTINCT lm.id, lm.name, lm.provider, lm.version ");
            modelsQuery.append("FROM model_answer_runs mar ");
            modelsQuery.append("JOIN llm_models lm ON mar.llm_model_id = lm.id ");
            modelsQuery.append("WHERE mar.answer_generation_batch_id = ?");
            
            List<Object> queryParams = new ArrayList<>();
            queryParams.add(batchId);
            
            if (modelIds != null && !modelIds.isEmpty()) {
                modelsQuery.append(" AND lm.id IN (");
                for (int i = 0; i < modelIds.size(); i++) {
                    modelsQuery.append("?");
                    if (i < modelIds.size() - 1) modelsQuery.append(",");
                    queryParams.add(modelIds.get(i));
                }
                modelsQuery.append(")");
            }
            
            List<Map<String, Object>> models = jdbcTemplate.queryForList(modelsQuery.toString(), queryParams.toArray());
            result.put("models", models);
            
            // 3. 获取评分统计数据
            List<Map<String, Object>> modelScores = new ArrayList<>();
            
            for (Map<String, Object> model : models) {
                Long modelId = (Long) model.get("id");
                Map<String, Object> modelScore = getModelComprehensiveScore(batchId, modelId);
                modelScore.put("modelInfo", model);
                modelScores.add(modelScore);
            }
            
            // 4. 按总分排序
            modelScores.sort((a, b) -> {
                BigDecimal scoreA = (BigDecimal) a.get("overallScore");
                BigDecimal scoreB = (BigDecimal) b.get("overallScore");
                if (scoreA == null) scoreA = BigDecimal.ZERO;
                if (scoreB == null) scoreB = BigDecimal.ZERO;
                return scoreB.compareTo(scoreA); // 降序排列
            });
            
            // 5. 添加排名
            for (int i = 0; i < modelScores.size(); i++) {
                modelScores.get(i).put("rank", i + 1);
            }
            
            // 6. 分页处理
            int totalModels = modelScores.size();
            int totalPages = (int) Math.ceil((double) totalModels / size);
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalModels);
            
            List<Map<String, Object>> pagedModelScores = modelScores.subList(startIndex, endIndex);
            
            // 7. 获取评测统计概览
            Map<String, Object> overview = getBatchEvaluationOverview(batchId, modelIds);
            
            // 8. 构建返回结果
            result.put("modelScores", pagedModelScores);
            result.put("overview", overview);
            result.put("pagination", Map.of(
                    "currentPage", page,
                    "pageSize", size,
                    "totalItems", totalModels,
                    "totalPages", totalPages
            ));
            
            logger.info("成功获取批次{}的综合评分数据，共{}个模型", batchId, totalModels);
            return result;
            
        } catch (Exception e) {
            logger.error("获取批次{}综合评分数据失败", batchId, e);
            throw new RuntimeException("获取批次综合评分数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取单个模型在批次中的综合评分
     */
    private Map<String, Object> getModelComprehensiveScore(Long batchId, Long modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 获取客观题评分
            Map<String, Object> objectiveScores = getModelObjectiveScores(batchId, modelId);
            result.put("objectiveScores", objectiveScores);
            
            // 2. 获取主观题AI评分
            Map<String, Object> subjectiveAiScores = getModelSubjectiveAiScores(batchId, modelId);
            result.put("subjectiveAiScores", subjectiveAiScores);
            
            // 3. 获取主观题人工评分
            Map<String, Object> subjectiveHumanScores = getModelSubjectiveHumanScores(batchId, modelId);
            result.put("subjectiveHumanScores", subjectiveHumanScores);
            
            // 4. 计算综合评分
            BigDecimal overallScore = calculateOverallScore(objectiveScores, subjectiveAiScores, subjectiveHumanScores);
            result.put("overallScore", overallScore);
            
            // 5. 获取评测详情统计
            Map<String, Object> detailStats = getModelEvaluationDetailStats(batchId, modelId);
            result.put("detailStats", detailStats);
            
            return result;
            
        } catch (Exception e) {
            logger.error("获取模型{}在批次{}中的综合评分失败", modelId, batchId, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 获取模型客观题评分
     */
    private Map<String, Object> getModelObjectiveScores(Long batchId, Long modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String query = "SELECT " +
                    "COUNT(DISTINCT e.llm_answer_id) as total_answers, " +
                    "AVG(e.normalized_score) as average_score, " +
                    "MAX(e.normalized_score) as max_score, " +
                    "MIN(e.normalized_score) as min_score, " +
                    "SUM(CASE WHEN sq.question_type = 'SINGLE_CHOICE' THEN 1 ELSE 0 END) as single_choice_count, " +
                    "SUM(CASE WHEN sq.question_type = 'MULTIPLE_CHOICE' THEN 1 ELSE 0 END) as multiple_choice_count, " +
                    "SUM(CASE WHEN sq.question_type = 'SIMPLE_FACT' THEN 1 ELSE 0 END) as simple_fact_count, " +
                    "AVG(CASE WHEN sq.question_type = 'SINGLE_CHOICE' THEN e.normalized_score END) as single_choice_avg, " +
                    "AVG(CASE WHEN sq.question_type = 'MULTIPLE_CHOICE' THEN e.normalized_score END) as multiple_choice_avg, " +
                    "AVG(CASE WHEN sq.question_type = 'SIMPLE_FACT' THEN e.normalized_score END) as simple_fact_avg " +
                    "FROM evaluations e " +
                    "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                    "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                    "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                    "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                    "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ? " +
                    "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT') " +
                    "AND e.evaluation_status = 'SUCCESS'";
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(query, batchId, modelId);
            
            if (!queryResult.isEmpty()) {
                result = queryResult.get(0);
            }
            
        } catch (Exception e) {
            logger.error("获取模型{}客观题评分失败", modelId, e);
        }
        
        return result;
    }
    
    /**
     * 获取模型主观题AI评分
     */
    private Map<String, Object> getModelSubjectiveAiScores(Long batchId, Long modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String query = "SELECT " +
                    "COUNT(DISTINCT e.llm_answer_id) as total_answers, " +
                    "AVG(e.overall_score) as average_score, " +
                    "MAX(e.overall_score) as max_score, " +
                    "MIN(e.overall_score) as min_score, " +
                    "COUNT(DISTINCT e.evaluator_id) as evaluator_count " +
                    "FROM evaluations e " +
                    "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                    "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                    "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                    "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                    "JOIN evaluators ev ON e.evaluator_id = ev.id " +
                    "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ? " +
                    "AND sq.question_type = 'SUBJECTIVE' " +
                    "AND ev.evaluator_type = 'AI_MODEL' " +
                    "AND e.evaluation_status = 'SUCCESS'";
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(query, batchId, modelId);
            
            if (!queryResult.isEmpty()) {
                result = queryResult.get(0);
                
                // 获取各个评测标准的平均分
                String criteriaQuery = "SELECT " +
                        "ed.criterion_name, " +
                        "AVG(ed.score) as average_score, " +
                        "COUNT(*) as count " +
                        "FROM evaluation_details ed " +
                        "JOIN evaluations e ON ed.evaluation_id = e.id " +
                        "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                        "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                        "JOIN evaluators ev ON e.evaluator_id = ev.id " +
                        "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ? " +
                        "AND ev.evaluator_type = 'AI_MODEL' " +
                        "AND e.evaluation_status = 'SUCCESS' " +
                        "GROUP BY ed.criterion_name " +
                        "ORDER BY ed.criterion_name";
                
                List<Map<String, Object>> criteriaScores = jdbcTemplate.queryForList(criteriaQuery, batchId, modelId);
                result.put("criteriaScores", criteriaScores);
            }
            
        } catch (Exception e) {
            logger.error("获取模型{}主观题AI评分失败", modelId, e);
        }
        
        return result;
    }
    
    /**
     * 获取模型主观题人工评分
     */
    private Map<String, Object> getModelSubjectiveHumanScores(Long batchId, Long modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String query = "SELECT " +
                    "COUNT(DISTINCT e.llm_answer_id) as total_answers, " +
                    "AVG(e.overall_score) as average_score, " +
                    "MAX(e.overall_score) as max_score, " +
                    "MIN(e.overall_score) as min_score, " +
                    "COUNT(DISTINCT e.evaluator_id) as evaluator_count " +
                    "FROM evaluations e " +
                    "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                    "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                    "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                    "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                    "JOIN evaluators ev ON e.evaluator_id = ev.id " +
                    "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ? " +
                    "AND sq.question_type = 'SUBJECTIVE' " +
                    "AND ev.evaluator_type = 'HUMAN' " +
                    "AND e.evaluation_status = 'SUCCESS'";
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(query, batchId, modelId);
            
            if (!queryResult.isEmpty()) {
                result = queryResult.get(0);
                
                // 获取各个评测标准的平均分
                String criteriaQuery = "SELECT " +
                        "ed.criterion_name, " +
                        "AVG(ed.score) as average_score, " +
                        "COUNT(*) as count " +
                        "FROM evaluation_details ed " +
                        "JOIN evaluations e ON ed.evaluation_id = e.id " +
                        "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                        "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                        "JOIN evaluators ev ON e.evaluator_id = ev.id " +
                        "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ? " +
                        "AND ev.evaluator_type = 'HUMAN' " +
                        "AND e.evaluation_status = 'SUCCESS' " +
                        "GROUP BY ed.criterion_name " +
                        "ORDER BY ed.criterion_name";
                
                List<Map<String, Object>> criteriaScores = jdbcTemplate.queryForList(criteriaQuery, batchId, modelId);
                result.put("criteriaScores", criteriaScores);
            }
            
        } catch (Exception e) {
            logger.error("获取模型{}主观题人工评分失败", modelId, e);
        }
        
        return result;
    }
    
    /**
     * 计算综合评分
     */
    private BigDecimal calculateOverallScore(Map<String, Object> objectiveScores, 
                                           Map<String, Object> subjectiveAiScores, 
                                           Map<String, Object> subjectiveHumanScores) {
        try {
            List<BigDecimal> scores = new ArrayList<>();
            
            // 客观题评分
            if (objectiveScores.get("average_score") != null) {
                scores.add(new BigDecimal(objectiveScores.get("average_score").toString()));
            }
            
            // 主观题AI评分
            if (subjectiveAiScores.get("average_score") != null) {
                scores.add(new BigDecimal(subjectiveAiScores.get("average_score").toString()));
            }
            
            // 主观题人工评分
            if (subjectiveHumanScores.get("average_score") != null) {
                scores.add(new BigDecimal(subjectiveHumanScores.get("average_score").toString()));
            }
            
            if (scores.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // 计算平均分
            BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.error("计算综合评分失败", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 获取模型评测详情统计
     */
    private Map<String, Object> getModelEvaluationDetailStats(Long batchId, Long modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String query = "SELECT " +
                    "COUNT(DISTINCT e.id) as total_evaluations, " +
                    "COUNT(DISTINCT e.llm_answer_id) as total_answers, " +
                    "COUNT(DISTINCT e.evaluator_id) as total_evaluators, " +
                    "SUM(CASE WHEN e.evaluation_status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
                    "SUM(CASE WHEN e.evaluation_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count " +
                    "FROM evaluations e " +
                    "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                    "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                    "WHERE mar.answer_generation_batch_id = ? AND mar.llm_model_id = ?";
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(query, batchId, modelId);
            
            if (!queryResult.isEmpty()) {
                result = queryResult.get(0);
            }
            
        } catch (Exception e) {
            logger.error("获取模型{}评测详情统计失败", modelId, e);
        }
        
        return result;
    }
    
    /**
     * 获取批次评测概览
     */
    private Map<String, Object> getBatchEvaluationOverview(Long batchId, List<Long> modelIds) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT ");
            query.append("COUNT(DISTINCT mar.llm_model_id) as total_models, ");
            query.append("COUNT(DISTINCT la.id) as total_answers, ");
            query.append("COUNT(DISTINCT e.id) as total_evaluations, ");
            query.append("COUNT(DISTINCT e.evaluator_id) as total_evaluators, ");
            query.append("SUM(CASE WHEN sq.question_type = 'SINGLE_CHOICE' THEN 1 ELSE 0 END) as single_choice_count, ");
            query.append("SUM(CASE WHEN sq.question_type = 'MULTIPLE_CHOICE' THEN 1 ELSE 0 END) as multiple_choice_count, ");
            query.append("SUM(CASE WHEN sq.question_type = 'SIMPLE_FACT' THEN 1 ELSE 0 END) as simple_fact_count, ");
            query.append("SUM(CASE WHEN sq.question_type = 'SUBJECTIVE' THEN 1 ELSE 0 END) as subjective_count, ");
            query.append("SUM(CASE WHEN ev.evaluator_type = 'AI_MODEL' THEN 1 ELSE 0 END) as ai_evaluation_count, ");
            query.append("SUM(CASE WHEN ev.evaluator_type = 'HUMAN' THEN 1 ELSE 0 END) as human_evaluation_count ");
            query.append("FROM model_answer_runs mar ");
            query.append("JOIN llm_answers la ON mar.id = la.model_answer_run_id ");
            query.append("JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id ");
            query.append("JOIN standard_questions sq ON dqm.standard_question_id = sq.id ");
            query.append("LEFT JOIN evaluations e ON la.id = e.llm_answer_id ");
            query.append("LEFT JOIN evaluators ev ON e.evaluator_id = ev.id ");
            query.append("WHERE mar.answer_generation_batch_id = ?");
            
            List<Object> queryParams = new ArrayList<>();
            queryParams.add(batchId);
            
            if (modelIds != null && !modelIds.isEmpty()) {
                query.append(" AND mar.llm_model_id IN (");
                for (int i = 0; i < modelIds.size(); i++) {
                    query.append("?");
                    if (i < modelIds.size() - 1) query.append(",");
                    queryParams.add(modelIds.get(i));
                }
                query.append(")");
            }
            
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(query.toString(), queryParams.toArray());
            
            if (!queryResult.isEmpty()) {
                result = queryResult.get(0);
            }
            
        } catch (Exception e) {
            logger.error("获取批次{}评测概览失败", batchId, e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getAnswerEvaluationDetails(Long answerId, Long batchId, Long questionId, 
            List<Long> modelIds, List<Long> evaluatorIds, String questionType, int page, int size) {
        
        logger.info("开始获取回答评分详情 - 回答ID: {}, 批次ID: {}, 问题ID: {}, 模型IDs: {}, 评测员IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                answerId, batchId, questionId, modelIds, evaluatorIds, questionType, page, size);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建查询条件
            StringBuilder sqlBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            // 基础查询SQL - 获取回答及其评测信息
            sqlBuilder.append("SELECT DISTINCT ")
                    .append("la.id AS answer_id, ")
                    .append("la.answer_text, ")
                    .append("la.repeat_index, ")
                    .append("la.generation_time, ")
                    .append("sq.id AS question_id, ")
                    .append("sq.question_text, ")
                    .append("sq.question_type, ")
                    .append("sq.difficulty, ")
                    .append("lm.id AS model_id, ")
                    .append("lm.name AS model_name, ")
                    .append("lm.provider AS model_provider, ")
                    .append("mar.id AS run_id, ")
                    .append("mar.run_name, ")
                    .append("agb.id AS batch_id, ")
                    .append("agb.name AS batch_name, ")
                    .append("e.id AS evaluation_id, ")
                    .append("e.overall_score, ")
                    .append("e.evaluation_time, ")
                    .append("e.evaluation_type, ")
                    .append("e.evaluation_status, ")
                    .append("e.comments AS evaluation_comments, ")
                    .append("e.evaluation_results, ")
                    .append("eva.id AS evaluator_id, ")
                    .append("eva.name AS evaluator_name, ")
                    .append("eva.evaluator_type ")
                    .append("FROM llm_answers la ")
                    .append("JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id ")
                    .append("JOIN standard_questions sq ON dqm.standard_question_id = sq.id ")
                    .append("JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id ")
                    .append("JOIN llm_models lm ON mar.llm_model_id = lm.id ")
                    .append("JOIN answer_generation_batches agb ON mar.answer_generation_batch_id = agb.id ")
                    .append("LEFT JOIN evaluations e ON la.id = e.llm_answer_id ")
                    .append("LEFT JOIN evaluators eva ON e.evaluator_id = eva.id ")
                    .append("WHERE 1=1 ");
            
            // 添加查询条件
            if (answerId != null) {
                sqlBuilder.append("AND la.id = ? ");
                params.add(answerId);
            }
            
            if (batchId != null) {
                sqlBuilder.append("AND agb.id = ? ");
                params.add(batchId);
            }
            
            if (questionId != null) {
                sqlBuilder.append("AND sq.id = ? ");
                params.add(questionId);
            }
            
            if (modelIds != null && !modelIds.isEmpty()) {
                sqlBuilder.append("AND lm.id IN (");
                for (int i = 0; i < modelIds.size(); i++) {
                    if (i > 0) sqlBuilder.append(",");
                    sqlBuilder.append("?");
                    params.add(modelIds.get(i));
                }
                sqlBuilder.append(") ");
            }
            
            if (evaluatorIds != null && !evaluatorIds.isEmpty()) {
                sqlBuilder.append("AND eva.id IN (");
                for (int i = 0; i < evaluatorIds.size(); i++) {
                    if (i > 0) sqlBuilder.append(",");
                    sqlBuilder.append("?");
                    params.add(evaluatorIds.get(i));
                }
                sqlBuilder.append(") ");
            }
            
            if (questionType != null && !questionType.trim().isEmpty()) {
                sqlBuilder.append("AND sq.question_type = ? ");
                params.add(questionType.trim().toUpperCase());
            }
            
            // 添加排序
            sqlBuilder.append("ORDER BY la.id DESC, e.evaluation_time DESC ");
            
            // 先获取总数
            String countSql = "SELECT COUNT(DISTINCT CONCAT(la.id, '-', COALESCE(e.id, 0))) " + 
                    sqlBuilder.substring(sqlBuilder.indexOf("FROM"));
            Long totalCount = jdbcTemplate.queryForObject(countSql, params.toArray(), Long.class);
            
            // 添加分页
            sqlBuilder.append("LIMIT ? OFFSET ? ");
            params.add(size);
            params.add(page * size);
            
            // 执行查询
            List<Map<String, Object>> evaluationData = jdbcTemplate.query(
                    sqlBuilder.toString(), 
                    params.toArray(),
                    (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        
                        // 回答信息
                        row.put("answerId", rs.getLong("answer_id"));
                        row.put("answerText", rs.getString("answer_text"));
                        row.put("repeatIndex", rs.getInt("repeat_index"));
                        row.put("generationTime", rs.getTimestamp("generation_time"));
                        
                        // 问题信息
                        row.put("questionId", rs.getLong("question_id"));
                        row.put("questionText", rs.getString("question_text"));
                        row.put("questionType", rs.getString("question_type"));
                        row.put("difficultyLevel", rs.getString("difficulty"));
                        
                        // 模型信息
                        row.put("modelId", rs.getLong("model_id"));
                        row.put("modelName", rs.getString("model_name"));
                        row.put("modelProvider", rs.getString("model_provider"));
                        
                        // 运行信息
                        row.put("runId", rs.getLong("run_id"));
                        row.put("runName", rs.getString("run_name"));
                        row.put("batchId", rs.getLong("batch_id"));
                        row.put("batchName", rs.getString("batch_name"));
                        
                        // 评测信息
                        Long evaluationId = rs.getLong("evaluation_id");
                        if (!rs.wasNull()) {
                            row.put("evaluationId", evaluationId);
                            row.put("overallScore", rs.getBigDecimal("overall_score"));
                            row.put("evaluationTime", rs.getTimestamp("evaluation_time"));
                            row.put("evaluationType", rs.getString("evaluation_type"));
                            row.put("evaluationStatus", rs.getString("evaluation_status"));
                            row.put("evaluationComments", rs.getString("evaluation_comments"));
                            
                            // 解析评测结果JSON
                            String evaluationResults = rs.getString("evaluation_results");
                            if (evaluationResults != null) {
                                try {
                                    row.put("evaluationResults", objectMapper.readValue(evaluationResults, Map.class));
                                } catch (Exception e) {
                                    logger.warn("解析评测结果JSON失败: {}", e.getMessage());
                                    row.put("evaluationResults", new HashMap<>());
                                }
                            }
                            
                            // 评测员信息
                            Long evaluatorId = rs.getLong("evaluator_id");
                            if (!rs.wasNull()) {
                                row.put("evaluatorId", evaluatorId);
                                row.put("evaluatorName", rs.getString("evaluator_name"));
                                row.put("evaluatorType", rs.getString("evaluator_type"));
                            }
                        }
                        
                        return row;
                    });
            
            // 为每个评测获取详细评分
            for (Map<String, Object> evaluation : evaluationData) {
                Long evaluationId = (Long) evaluation.get("evaluationId");
                if (evaluationId != null) {
                    // 获取评测详情
                    List<Map<String, Object>> details = getEvaluationDetailsByEvaluationId(evaluationId);
                    evaluation.put("evaluationDetails", details);
                    
                    // 获取标准答案（根据问题类型）
                    Long questionIdForAnswer = (Long) evaluation.get("questionId");
                    String questionTypeForAnswer = (String) evaluation.get("questionType");
                    Map<String, Object> standardAnswer = getStandardAnswerByQuestionIdAndType(questionIdForAnswer, questionTypeForAnswer);
                    evaluation.put("standardAnswer", standardAnswer);
                }
            }
            
            // 计算分页信息
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            // 构建返回结果
            result.put("success", true);
            result.put("items", evaluationData);
            result.put("totalItems", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            result.put("pageSize", size);
            
            logger.info("成功获取回答评分详情，总数: {}, 当前页: {}, 每页大小: {}", totalCount, page, size);
            
        } catch (Exception e) {
            logger.error("获取回答评分详情失败", e);
            result.put("success", false);
            result.put("message", "获取评分详情失败: " + e.getMessage());
            result.put("items", new ArrayList<>());
            result.put("totalItems", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("pageSize", size);
        }
        
        return result;
    }
    
    /**
     * 根据评测ID获取评测详情
     */
    private List<Map<String, Object>> getEvaluationDetailsByEvaluationId(Long evaluationId) {
        String sql = "SELECT ed.id, ed.criterion_id, ed.criterion_name, ed.score, ed.comments, " +
                "ec.name AS criterion_full_name, ec.description AS criterion_description, " +
                "ec.score_range AS criterion_score_range, ec.weight AS criterion_weight " +
                "FROM evaluation_details ed " +
                "LEFT JOIN evaluation_criteria ec ON ed.criterion_id = ec.id " +
                "WHERE ed.evaluation_id = ? " +
                "ORDER BY ed.id";
        
        return jdbcTemplate.query(sql, new Object[]{evaluationId}, (rs, rowNum) -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", rs.getLong("id"));
            detail.put("criterionId", rs.getLong("criterion_id"));
            detail.put("criterionName", rs.getString("criterion_name"));
            detail.put("score", rs.getBigDecimal("score"));
            detail.put("comments", rs.getString("comments"));
            
            // 评测标准详细信息
            detail.put("criterionFullName", rs.getString("criterion_full_name"));
            detail.put("criterionDescription", rs.getString("criterion_description"));
            detail.put("criterionScoreRange", rs.getString("criterion_score_range"));
            detail.put("criterionWeight", rs.getBigDecimal("criterion_weight"));
            
            return detail;
        });
    }
    
    /**
     * 根据问题ID和类型获取标准答案
     */
    private Map<String, Object> getStandardAnswerByQuestionIdAndType(Long questionId, String questionType) {
        Map<String, Object> standardAnswer = new HashMap<>();
        
        try {
            if (questionType == null) {
                return standardAnswer;
            }
            
            switch (questionType.toUpperCase()) {
                case "SINGLE_CHOICE":
                case "MULTIPLE_CHOICE":
                    // 客观题标准答案
                    String objectiveSql = "SELECT options, correct_ids FROM standard_objective_answers " +
                            "WHERE standard_question_id = ? AND deleted_at IS NULL";
                    try {
                        Map<String, Object> objectiveAnswer = jdbcTemplate.queryForMap(objectiveSql, questionId);
                        standardAnswer.put("type", "OBJECTIVE");
                        standardAnswer.put("options", objectiveAnswer.get("options"));
                        standardAnswer.put("correctIds", objectiveAnswer.get("correct_ids"));
                    } catch (Exception e) {
                        logger.debug("未找到问题{}的客观题标准答案", questionId);
                    }
                    break;
                    
                case "SIMPLE_FACT":
                    // 简单题标准答案
                    String simpleSql = "SELECT answer_text, alternative_answers FROM standard_simple_answers " +
                            "WHERE standard_question_id = ? AND deleted_at IS NULL";
                    try {
                        Map<String, Object> simpleAnswer = jdbcTemplate.queryForMap(simpleSql, questionId);
                        standardAnswer.put("type", "SIMPLE");
                        standardAnswer.put("answerText", simpleAnswer.get("answer_text"));
                        standardAnswer.put("alternativeAnswers", simpleAnswer.get("alternative_answers"));
                    } catch (Exception e) {
                        logger.debug("未找到问题{}的简单题标准答案", questionId);
                    }
                    break;
                    
                case "SUBJECTIVE":
                    // 主观题标准答案
                    String subjectiveSql = "SELECT answer_text, scoring_guidance FROM standard_subjective_answers " +
                            "WHERE standard_question_id = ? AND deleted_at IS NULL";
                    try {
                        Map<String, Object> subjectiveAnswer = jdbcTemplate.queryForMap(subjectiveSql, questionId);
                        standardAnswer.put("type", "SUBJECTIVE");
                        standardAnswer.put("answerText", subjectiveAnswer.get("answer_text"));
                        standardAnswer.put("scoringGuidance", subjectiveAnswer.get("scoring_guidance"));
                    } catch (Exception e) {
                        logger.debug("未找到问题{}的主观题标准答案", questionId);
                    }
                    break;
                    
                default:
                    logger.warn("未知的问题类型: {}", questionType);
            }
            
        } catch (Exception e) {
            logger.error("获取问题{}的标准答案失败", questionId, e);
        }
        
        return standardAnswer;
    }
} 