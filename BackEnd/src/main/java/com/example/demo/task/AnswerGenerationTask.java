package com.example.demo.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.entity.jdbc.AnswerGenerationBatch;
import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.AnswerTagPrompt;
import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.example.demo.entity.jdbc.ModelAnswerRun.RunStatus;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.manager.BatchStateManager;
import com.example.demo.repository.jdbc.AnswerGenerationBatchRepository;
import com.example.demo.repository.jdbc.AnswerQuestionTypePromptRepository;
import com.example.demo.repository.jdbc.AnswerTagPromptRepository;
import com.example.demo.repository.jdbc.LlmAnswerRepository;
import com.example.demo.repository.jdbc.ModelAnswerRunRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.StandardQuestionTagsRepository;
import com.example.demo.service.LlmApiService;
import com.example.demo.service.WebSocketService;
import com.example.demo.utils.TextPreprocessor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;



/**
 * 回答生成异步任务
 */
@Component
public class AnswerGenerationTask {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationTask.class);
    
    private final AnswerGenerationBatchRepository batchRepository;
    private final ModelAnswerRunRepository runRepository;
    private final StandardQuestionRepository questionRepository;
    private final LlmAnswerRepository answerRepository;
    private final WebSocketService webSocketService;
    private final LlmApiService llmApiService;
    private final AnswerTagPromptRepository answerTagPromptRepository;
    private final AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository;
    private final StandardQuestionTagsRepository standardQuestionTagsRepository;
    private final JdbcTemplate jdbcTemplate;
    private BatchStateManager batchStateManager;
    // 添加事务管理器
    private final PlatformTransactionManager transactionManager;
    
    // 添加中断控制器
    private final ConcurrentHashMap<Long, AtomicBoolean> interruptionFlags = new ConcurrentHashMap<>();
    private final ScheduledExecutorService interruptionMonitor = Executors.newScheduledThreadPool(1);
    
    // 添加中断标志来源跟踪
    private final ConcurrentHashMap<Long, String> interruptionSource = new ConcurrentHashMap<>();
    
    @Autowired
    public AnswerGenerationTask(
            AnswerGenerationBatchRepository batchRepository,
            ModelAnswerRunRepository runRepository,
            StandardQuestionRepository questionRepository,
            LlmAnswerRepository answerRepository,
            WebSocketService webSocketService,
            LlmApiService llmApiService,
            AnswerTagPromptRepository answerTagPromptRepository,
            AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository,
            StandardQuestionTagsRepository standardQuestionTagsRepository,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.batchRepository = batchRepository;
        this.runRepository = runRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.webSocketService = webSocketService;
        this.llmApiService = llmApiService;
        this.answerTagPromptRepository = answerTagPromptRepository;
        this.answerQuestionTypePromptRepository = answerQuestionTypePromptRepository;
        this.standardQuestionTagsRepository = standardQuestionTagsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }
    
    @Autowired
    @Lazy
    public void setBatchStateManager(BatchStateManager batchStateManager) {
        this.batchStateManager = batchStateManager;
    }
    
    /**
     * 初始化方法，启动中断监控
     */
    @PostConstruct
    public void init() {
        logger.info("初始化回答生成任务管理器");
        
        // 启动定期检查Redis中断标志的任务
        interruptionMonitor.scheduleAtFixedRate(() -> {
            try {
                // 检查所有有中断标志的批次
                for (Long batchId : interruptionFlags.keySet()) {
                    try {
                        // 检查Redis中的中断标志
                        if (batchStateManager != null) {
                            boolean redisInterruptFlag = batchStateManager.isInterrupted(batchId);
                            boolean memoryInterruptFlag = interruptionFlags.get(batchId).get();
                            
                            // 同步Redis和内存中的中断标志
                            if (redisInterruptFlag && !memoryInterruptFlag) {
                                logger.info("批次{}在Redis中有中断标志，同步到内存", batchId);
                                markForInterruption(batchId, "REDIS_SYNC");
                            } else if (!redisInterruptFlag && memoryInterruptFlag) {
                                // 检查是否是手动暂停
                                String source = interruptionSource.getOrDefault(batchId, "UNKNOWN");
                                if (!"MANUAL_PAUSE".equals(source)) {
                                    logger.info("批次{}在Redis中无中断标志，清除内存中的中断标志", batchId);
                                    clearInterruptionFlag(batchId);
                                } else {
                                    logger.info("批次{}有手动暂停标志，保持中断状态", batchId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("检查批次{}中断状态时出错", batchId, e);
                    }
                }
            } catch (Exception e) {
                logger.error("中断监控任务出错", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 销毁方法，关闭中断监控
     */
    @PreDestroy
    public void destroy() {
        logger.info("关闭回答生成任务管理器");
        interruptionMonitor.shutdownNow();
    }
    
    /**
     * 标记批次需要中断
     */
    public void markForInterruption(Long batchId) {
        logger.info("批次{}已被标记为需要中断", batchId);
        interruptionFlags.computeIfAbsent(batchId, k -> new AtomicBoolean(false)).set(true);
        interruptionSource.put(batchId, "TASK_MANAGER");
    }
    
    /**
     * 标记批次需要中断（带来源）
     */
    public void markForInterruption(Long batchId, String source) {
        logger.info("批次{}已被标记为需要中断，来源: {}", batchId, source);
        interruptionFlags.computeIfAbsent(batchId, k -> new AtomicBoolean(false)).set(true);
        interruptionSource.put(batchId, source);
    }
    
    /**
     * 清除批次中断标志
     */
    public void clearInterruptionFlag(Long batchId) {
        logger.info("批次{}的中断标志已清除", batchId);
        AtomicBoolean flag = interruptionFlags.get(batchId);
        if (flag != null) {
            flag.set(false);
        }
        interruptionSource.remove(batchId);
    }
    
    /**
     * 检查批次是否应该中断
     */
    public boolean shouldInterrupt(Long batchId) {
        // 只检查内存中的中断标志
        AtomicBoolean flag = interruptionFlags.get(batchId);
        if (flag != null && flag.get()) {
            logger.debug("批次{}有内存中断标志，需要中断", batchId);
            return true;
        }
        
        // 如果Redis中有中断标志，也应该中断
        if (batchStateManager != null && batchStateManager.isInterrupted(batchId)) {
            logger.debug("批次{}在Redis中有中断标志，需要中断", batchId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查批次是否应该暂停
     */
    public boolean shouldPauseBatch(Long batchId) {
        // 直接调用shouldInterrupt方法
        return shouldInterrupt(batchId);
    }
    
    /**
     * 开始处理单个批次
     */
    public void startBatchAnswerGeneration(Long batchId) {
        logger.info("开始处理批次: {}", batchId);
        
        try {
            // 获取当前状态但不用于判断是否处理
            String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
                
            logger.info("批次{}当前状态为{}，开始处理", batchId, currentStatus);
            
            // 直接更新状态为GENERATING_ANSWERS，不做状态检查
            int updated = jdbcTemplate.update(
                "UPDATE answer_generation_batches SET status = 'GENERATING_ANSWERS', last_activity_time = ?, " + 
                "last_check_time = ?, processing_instance = ? WHERE id = ?",
                LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID().toString(), batchId);
                    
            logger.info("已将批次{}状态更新为GENERATING_ANSWERS并获取处理权", batchId);
            
            // 同步Redis状态
            if (batchStateManager != null) {
                batchStateManager.setBatchState(batchId, "GENERATING_ANSWERS");
                batchStateManager.setInterruptFlag(batchId, false);
            }
            
            // 清除内存中断标志
            clearInterruptionFlag(batchId);
            
            // 获取批次信息
            AnswerGenerationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次: " + batchId));
            
            // 获取批次关联的所有运行
            List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
            if (runs.isEmpty()) {
                logger.warn("批次{}没有关联的运行，无法启动处理", batchId);
                return;
            }
            logger.info("批次{}共有{}个运行", batchId, runs.size());
            
            // 获取批次关联的所有问题
            List<StandardQuestion> questions = questionRepository.findByDatasetVersionId(batch.getDatasetVersion().getId());
            if (questions.isEmpty()) {
                logger.warn("批次{}关联的数据集版本没有问题，无法启动处理", batchId);
                return;
            }
            logger.info("批次{}关联的数据集共有{}个问题", batchId, questions.size());
            
            // 预加载问题ID
            List<Long> questionIds = questions.stream().map(StandardQuestion::getId).collect(java.util.stream.Collectors.toList());
            
            // 预加载数据集映射（不会覆盖已加载的标签）
            if (!questionIds.isEmpty()) {
                logger.info("预加载批次{}的问题映射关系", batchId);
                List<StandardQuestion> questionsWithMappings = questionRepository.findByIdsWithDatasetMappings(questionIds);
                
                // 创建ID到预加载问题的映射，用于替换原始列表中的问题
                Map<Long, StandardQuestion> questionMap = new HashMap<>();
                for (StandardQuestion q : questionsWithMappings) {
                    questionMap.put(q.getId(), q);
                }
                
                // 使用预加载的问题替换原始列表中的问题
                for (int i = 0; i < questions.size(); i++) {
                    Long id = questions.get(i).getId();
                    if (questionMap.containsKey(id)) {
                        questions.set(i, questionMap.get(id));
                    }
                }
                logger.info("批次{}问题映射关系加载完成", batchId);
            }
            
            // 更新批次的总问题数
            int totalQuestions = questions.size() * runs.size() * batch.getAnswerRepeatCount();
            logger.info("批次{}总问题数: {}", batchId, totalQuestions);
            
            // 检查是否有上次处理到的运行记录
            Long lastProcessedRunId = batch.getLastProcessedRunId();
            ModelAnswerRun lastProcessedRun = null;
            int startIndex = 0;
            
            if (lastProcessedRunId != null) {
                lastProcessedRun = runRepository.findById(lastProcessedRunId).orElse(null);
                if (lastProcessedRun != null) {
                    logger.info("批次{}有上次处理记录，从运行ID={}之后开始处理", batchId, lastProcessedRunId);
                    // 找到上次处理的运行在列表中的位置
                    for (int i = 0; i < runs.size(); i++) {
                        if (runs.get(i).getId().equals(lastProcessedRunId)) {
                            startIndex = i; // 从下一个运行开始
                            break;
                        }
                    }
                    logger.info("将从运行列表的索引{}开始处理", startIndex);
                }
            }
            
            // 启动每个运行的处理，从startIndex开始
            for (int i = startIndex; i < runs.size(); i++) {
                ModelAnswerRun run = runs.get(i);
                Long runId = run.getId();
                
                // 获取当前状态但不用于判断是否处理
                String runStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM model_answer_runs WHERE id = ?", 
                    String.class, runId);
                
                logger.info("开始处理批次{}的运行: {}，模型: {}, 当前状态: {}", 
                        batchId, runId, run.getLlmModel().getName(), runStatus);
                
                // 直接更新运行状态为GENERATING_ANSWERS
                jdbcTemplate.update(
                    "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                    LocalDateTime.now(), runId);
                    
                logger.info("运行{}状态已更新为GENERATING_ANSWERS", runId);
                
                // 更新批次的lastProcessedRun为当前运行
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET last_processed_run_id = ? WHERE id = ?",
                    runId, batchId);
                logger.info("批次{}的last_processed_run_id已更新为{}", batchId, runId);
                
                // 检查是否有断点信息
                Long lastProcessedQuestionId = run.getLastProcessedQuestionId();
                Integer lastProcessedQuestionIndex = run.getLastProcessedQuestionIndex();
                
                if (lastProcessedQuestionId != null && lastProcessedQuestionIndex != null && lastProcessedQuestionIndex >= 0) {
                    logger.info("运行{}有断点信息，将从断点处继续: 问题ID={}, 索引={}", 
                                runId, lastProcessedQuestionId, lastProcessedQuestionIndex);
                    
                    // 从断点处继续处理
                    startRunAnswerGenerationFromCheckpoint(run, questions, batch.getAnswerRepeatCount(), new AtomicBoolean(false));
                } else {
                    logger.info("运行{}没有断点信息，将从头开始处理", runId);
                    
                    // 从头开始处理
                    startRunAnswerGeneration(run, questions, batch.getAnswerRepeatCount(), new AtomicBoolean(false));
                }
                
                logger.info("批次{}的运行{}处理已启动", batchId, runId);
            }
            
            // 批次处理完成后，检查所有运行状态并更新批次状态
            checkAndUpdateBatchCompletion(batch);
            
            // 处理完成后，清除处理标记
            jdbcTemplate.update(
                "UPDATE answer_generation_batches SET processing_instance = NULL WHERE id = ?",
                batchId);
                
            logger.info("批次{}处理完成，已清除处理标记", batchId);
        } catch (Exception e) {
            logger.error("处理批次{}失败: {}", batchId, e.getMessage(), e);
            
            try {
                // 更新批次状态为失败
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET status = 'FAILED', error_message = ?, " +
                    "last_activity_time = ?, processing_instance = NULL WHERE id = ?",
                    e.getMessage(), LocalDateTime.now(), batchId);
                
                if (batchStateManager != null) {
                    batchStateManager.setBatchState(batchId, "FAILED");
                }
                
                // 发送错误通知
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("batchId", batchId);
                errorData.put("error", "批次处理失败: " + e.getMessage());
                errorData.put("timestamp", System.currentTimeMillis());
                webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
            } catch (Exception ex) {
                logger.error("更新批次{}失败状态时出错", batchId, ex);
            }
        }
    }
    
    /**
     * 从断点处开始处理单个运行的回答生成
     */
    private void startRunAnswerGenerationFromCheckpoint(ModelAnswerRun run, List<StandardQuestion> questions, int repeatCount, AtomicBoolean shouldStop) {
        Long runId = run.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        logger.info("从断点处恢复运行: {}, 模型: {}", runId, run.getLlmModel().getName());
        
        // 获取批次状态但不用于判断是否处理
        String batchStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM answer_generation_batches WHERE id = ?", 
            String.class, batchId);
        
        logger.info("断点恢复前批次{}状态: {}，继续处理", batchId, batchStatus);
        
        try {
            // 直接更新运行状态为GENERATING_ANSWERS，不做状态检查
            jdbcTemplate.update(
                "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), runId);
            
            logger.info("运行{}状态已更新为GENERATING_ANSWERS", runId);
            
            // 初始化计数器
            int totalQuestions = questions.size() * repeatCount;
            int completedQuestions = run.getCompletedQuestionsCount();
            int failedQuestions = run.getFailedQuestionsCount();
            
            logger.info("运行{}已完成的问题数: {}, 失败的问题数: {}, 总问题数: {}", 
                runId, completedQuestions, failedQuestions, totalQuestions);
            
            // 查找上次处理的位置
            int lastProcessedIndex = run.getLastProcessedQuestionIndex();
            Long lastProcessedId = run.getLastProcessedQuestionId();
            
            logger.info("运行{}上次处理位置: 问题ID={}, 索引={}", runId, lastProcessedId, lastProcessedIndex);
            
            // 确定开始处理的重复索引和问题索引
            int startRepeatIndex = 0;
            int startQuestionIndex = 0;
            
            if (lastProcessedIndex >= 0) {
                // 计算重复索引和问题索引
                startRepeatIndex = lastProcessedIndex / questions.size();
                startQuestionIndex = lastProcessedIndex % questions.size();
                
                logger.info("运行{}从断点处恢复：重复索引={}, 问题索引={}", runId, startRepeatIndex, startQuestionIndex);
            } else {
                logger.info("运行{}没有有效的断点位置，从头开始处理", runId);
            }
            
            long startTime = System.currentTimeMillis();
            logger.info("开始从断点处恢复运行{}的处理", runId);
            
            // 处理每个问题（考虑重复次数），从断点处开始
            for (int r = startRepeatIndex; r < repeatCount; r++) {
                for (int q = (r == startRepeatIndex ? startQuestionIndex : 0); q < questions.size(); q++) {
                    StandardQuestion question = questions.get(q);
                    
                    logger.debug("运行{}处理问题: ID={}, 重复索引={}, 问题索引={}", 
                        runId, question.getId(), r, q);
                    
                    // 每次处理一个问题前检查批次是否应该中断
                    if (shouldInterrupt(batchId) || shouldStop.get()) {
                        logger.info("检测到批次{}已标记为中断，停止处理运行{}中的新问题", batchId, runId);
                        
                        // 保存当前处理位置
                        jdbcTemplate.update(
                            "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ?, " +
                            "last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                            LocalDateTime.now(), question.getId(), (r * questions.size() + q), runId);
                            
                        logger.info("运行{}已暂停，当前处理位置已保存: 问题ID={}, 索引={}", 
                            runId, question.getId(), (r * questions.size() + q));
                            
                        // 发送状态变更通知
                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("runId", runId);
                        statusData.put("status", "PAUSED");
                        statusData.put("completedQuestions", completedQuestions);
                        statusData.put("failedQuestions", failedQuestions);
                        statusData.put("totalQuestions", totalQuestions);
                        statusData.put("message", "运行已暂停");
                        
                        webSocketService.sendRunMessage(runId, MessageType.STATUS_CHANGE, statusData);
                        
                        return;
                    }
                    
                    // 处理单个问题
                    logger.debug("运行{}开始处理问题: ID={}", runId, question.getId());
                    boolean success = processQuestion(run, question, r, completedQuestions + 1);
                    
                    // 更新计数器
                    if (success) {
                        completedQuestions++;
                        logger.debug("运行{}问题处理成功: ID={}, 已完成问题数={}", 
                            runId, question.getId(), completedQuestions);
                    } else {
                        failedQuestions++;
                        logger.debug("运行{}问题处理失败: ID={}, 失败问题数={}", 
                            runId, question.getId(), failedQuestions);
                    }
                    
                    // 更新处理位置
                    jdbcTemplate.update(
                        "UPDATE model_answer_runs SET last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                        question.getId(), (r * questions.size() + q), runId);
                    
                    logger.debug("运行{}处理位置已更新: 问题ID={}, 索引={}", 
                        runId, question.getId(), (r * questions.size() + q));
                    
                    // 更新进度
                    updateRunProgress(run, completedQuestions, failedQuestions, totalQuestions);
                    
                    // 每处理5个问题后，再次检查中断状态
                    if ((completedQuestions + failedQuestions) % 5 == 0) {
                        if (shouldInterrupt(batchId) || shouldStop.get()) {
                            logger.info("批量处理过程中检测到批次{}已标记为中断，停止处理", batchId);
                            return;
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("运行{}处理完成，总耗时: {}毫秒", runId, (endTime - startTime));
            
            // 更新运行状态为COMPLETED
            updateRunStatus(run, RunStatus.COMPLETED, null);
            
            // 检查批次是否所有运行都已完成
            checkAndUpdateBatchCompletion(run.getAnswerGenerationBatch());
            
        } catch (Exception e) {
            logger.error("处理运行{}失败: {}", runId, e.getMessage(), e);
            
            // 更新运行状态为FAILED
            updateRunStatus(run, RunStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 开始处理单个运行的回答生成
     */
    private void startRunAnswerGeneration(ModelAnswerRun run, List<StandardQuestion> questions, int repeatCount, AtomicBoolean shouldStop) {
        Long runId = run.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        logger.info("开始处理运行: {}, 模型: {}", runId, run.getLlmModel().getName());
        
        // 获取批次状态但不用于判断是否处理
        String batchStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM answer_generation_batches WHERE id = ?", 
            String.class, batchId);
        
        logger.info("批次{}当前状态为{}，继续处理运行{}", batchId, batchStatus, runId);
        
        try {
            // 直接更新运行状态为GENERATING_ANSWERS，不做状态检查
            jdbcTemplate.update(
                "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), runId);
            
            // 初始化计数器
            int totalQuestions = questions.size() * repeatCount;
            int completedQuestions = 0;
            int failedQuestions = 0;
            
            long startTime = System.currentTimeMillis();
            
            // 处理每个问题（考虑重复次数）
            for (int r = 0; r < repeatCount; r++) {
                for (int q = 0; q < questions.size(); q++) {
                    StandardQuestion question = questions.get(q);
                    
                    // 每次处理一个问题前检查批次是否应该中断
                    if (shouldInterrupt(batchId) || shouldStop.get()) {
                        logger.info("检测到批次{}已标记为中断，停止处理运行{}中的新问题", batchId, runId);
                        
                        // 更新运行状态为PAUSED，同时保存当前处理位置
                        jdbcTemplate.update(
                            "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ?, " +
                            "last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                            LocalDateTime.now(), question.getId(), (r * questions.size() + q), runId);
                            
                        // 发送状态变更通知
                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("runId", runId);
                        statusData.put("status", "PAUSED");
                        statusData.put("completedQuestions", completedQuestions);
                        statusData.put("failedQuestions", failedQuestions);
                        statusData.put("totalQuestions", totalQuestions);
                        statusData.put("message", "运行已暂停");
                        
                        webSocketService.sendRunMessage(runId, MessageType.STATUS_CHANGE, statusData);
                        
                        return;
                    }
                    
                    // 处理单个问题
                    boolean success = processQuestion(run, question, r, completedQuestions + 1);
                    
                    // 更新计数器
                    if (success) {
                        completedQuestions++;
                    } else {
                        failedQuestions++;
                    }
                    
                    // 更新处理位置
                    jdbcTemplate.update(
                        "UPDATE model_answer_runs SET last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                        question.getId(), (r * questions.size() + q), runId);
                    
                    // 更新进度
                    updateRunProgress(run, completedQuestions, failedQuestions, totalQuestions);
                    
                    // 每处理5个问题后，再次检查中断状态
                    if ((completedQuestions + failedQuestions) % 5 == 0) {
                        if (shouldInterrupt(batchId) || shouldStop.get()) {
                            logger.info("批量处理过程中检测到批次{}已标记为中断，停止处理", batchId);
                            return;
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("运行{}处理完成，总耗时: {}毫秒", runId, (endTime - startTime));
            
            // 更新运行状态为COMPLETED
            updateRunStatus(run, RunStatus.COMPLETED, null);
            
            // 检查批次是否所有运行都已完成
            checkAndUpdateBatchCompletion(run.getAnswerGenerationBatch());
            
        } catch (Exception e) {
            logger.error("处理运行{}失败: {}", runId, e.getMessage(), e);
            
            // 更新运行状态为FAILED
            updateRunStatus(run, RunStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 处理单个问题
     */
    public boolean processQuestion(ModelAnswerRun run, StandardQuestion question, int repeatIndex, int expectedCompletedCount) {
        Long runId = run.getId();
        Long questionId = question.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        
        logger.info("开始处理问题: 运行={}, 问题ID={}, 重复索引={}", runId, questionId, repeatIndex);
        
        // 每次处理问题前检查中断标志
        if (shouldInterrupt(batchId)) {
            logger.info("检测到批次{}的中断信号，跳过问题处理", batchId);
            
            // 发送问题跳过通知
            Map<String, Object> payload = new HashMap<>();
            payload.put("runId", runId);
            payload.put("questionId", questionId);
            payload.put("questionText", question.getQuestionText());
            payload.put("repeatIndex", repeatIndex);
            payload.put("reason", "批次中断");
            payload.put("timestamp", System.currentTimeMillis());
            
            webSocketService.sendRunMessage(runId, MessageType.NOTIFICATION, payload);
            logger.debug("问题跳过通知已发送: 运行={}, 问题ID={}", runId, questionId);
            
            return false; // 立即返回，不处理当前问题
        }
        
        logger.debug("处理问题: 运行={}, 问题={}, 重复索引={}", runId, questionId, repeatIndex);
        
        // 创建事务定义
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            // 在问题处理过程中定期检查中断标志
            if (shouldInterrupt(batchId)) {
                logger.info("问题处理前检测到批次{}的中断信号，中止处理", batchId);
                transactionManager.rollback(status);
                return false;
            }
            
            // 发送问题开始处理通知
            sendQuestionStartedNotification(run, question, repeatIndex);
            logger.debug("问题处理开始通知已发送: 运行={}, 问题ID={}", runId, questionId);
            
            // 组装Prompt
            logger.debug("开始组装问题Prompt: 运行={}, 问题ID={}", runId, questionId);
            String prompt = assemblePrompt(run, question);
            logger.debug("问题Prompt组装完成: 运行={}, 问题ID={}, Prompt长度={}", runId, questionId, prompt.length());
            
            // 调用LLM API生成回答，并支持中断检查
            logger.info("开始调用LLM API生成回答: 运行={}, 问题ID={}, 模型={}", 
                runId, questionId, run.getLlmModel().getName());
            String answer = generateAnswerWithInterruptCheck(run, prompt, new AtomicBoolean(false));
            
            // 如果已被中断，则不继续处理
            if (answer == null) {
                logger.info("批次{}在生成回答过程中被中断，不保存结果: 运行={}, 问题={}", batchId, runId, questionId);
                jdbcTemplate.update(
                    "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ? WHERE id = ?",
                    LocalDateTime.now(), runId);
                transactionManager.rollback(status);
                return false;
            }
            
            logger.info("LLM API生成回答成功: 运行={}, 问题ID={}, 回答长度={}", 
                runId, questionId, answer.length());
            
            // 使用TextPreprocessor处理模型回答，移除思考过程标记
            String cleanedAnswer = TextPreprocessor.cleanText(answer);
            
            // 检查是否有思考过程标记被移除
            if (cleanedAnswer.length() != answer.length()) {
                // 提取并保存思考过程（可选）
                String thinkingProcess = TextPreprocessor.extractThinkingProcess(answer);
                if (!thinkingProcess.isEmpty()) {
                    // 保存思考过程（如果系统有相应字段）
                    // answer.setThinkingProcess(thinkingProcess);  // 假设系统有该字段
                    
                    logger.info("检测到并提取了思考过程，长度: {}, 回答ID: {}", 
                        thinkingProcess.length(), questionId);
                }
            }
            
            // 保存清理后的回答
            answer = cleanedAnswer;
            
            // 保存回答结果
            logger.debug("开始保存回答结果: 运行={}, 问题ID={}", runId, questionId);
            saveModelAnswer(run, question, answer, repeatIndex);
            logger.info("回答结果保存成功: 运行={}, 问题ID={}", runId, questionId);
            
            // 提交事务
            transactionManager.commit(status);
            
            // 事务提交成功后发送问题完成处理通知
            sendQuestionCompletedNotification(run, question, repeatIndex, expectedCompletedCount);
            logger.debug("问题处理完成通知已发送: 运行={}, 问题ID={}, 完成数量={}", runId, questionId, expectedCompletedCount);
            
            return true;
        } catch (Exception e) {
            // 回滚事务
            transactionManager.rollback(status);
            
            logger.error("处理问题失败: 运行={}, 问题={}, 错误={}", runId, questionId, e.getMessage(), e);
            
            // 记录失败信息
            recordFailedQuestion(run, questionId);
            
            // 发送问题处理失败通知
            sendQuestionFailedNotification(run, question, repeatIndex, e.getMessage());
            
            return false;
        }
    }
    
    /**
     * 组装Prompt
     */
    private String assemblePrompt(ModelAnswerRun run, StandardQuestion question) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取回答Prompt组装配置
        AnswerGenerationBatch batch = run.getAnswerGenerationBatch();
        if (batch == null) {
            logger.error("ModelAnswerRun {}的关联批次为null，无法组装prompt", run.getId());
            return "Error: 无法找到批次配置";
        }
        
        AnswerPromptAssemblyConfig config = batch.getAnswerAssemblyConfig();
        
        // 详细记录批次和配置信息
        logger.info("组装prompt - 批次ID：{}，名称：{}，关联配置ID：{}", 
            batch.getId(), batch.getName(), 
            batch.getAnswerAssemblyConfig() != null ? batch.getAnswerAssemblyConfig().getId() : "null");
        
        if (config == null) {
            logger.warn("未找到Prompt组装配置，尝试使用默认系统提示词");
            // 使用一个默认的系统提示词
            promptBuilder.append("你是一个专业的医学AI助手，请基于专业医学知识回答以下问题：\n\n");
            promptBuilder.append(question.getQuestionText());
            
            logger.warn("使用默认提示词：{}", promptBuilder.toString());
            return promptBuilder.toString();
        }
        
        logger.debug("使用配置：名称={}，系统提示词={}", config.getName(), config.getBaseSystemPrompt());
        
        // 添加系统提示词
        if (config.getBaseSystemPrompt() != null && !config.getBaseSystemPrompt().trim().isEmpty()) {
            promptBuilder.append(config.getBaseSystemPrompt()).append("\n\n");
        } else {
            logger.debug("系统提示词为空，跳过添加");
        }
        
        // 获取问题的标签
        List<Tag> tags = standardQuestionTagsRepository.findTagsByQuestionId(question.getId());
        logger.debug("问题ID={}关联标签数量：{}", question.getId(), tags.size());
        
        // 只有存在标签时才添加标签提示词部分
        if (!tags.isEmpty()) {
            // 添加标签提示词部分标题
            if (config.getTagPromptsSectionHeader() != null) {
                promptBuilder.append(config.getTagPromptsSectionHeader()).append("\n");
            }
            
            StringBuilder tagPromptsBuilder = new StringBuilder();
            List<AnswerTagPrompt> tagPrompts = new ArrayList<>();
            
            // 收集标签相关的prompt
            for (Tag tag : tags) {
                List<AnswerTagPrompt> prompts = answerTagPromptRepository.findActivePromptsByTagId(tag.getId());
                if (!prompts.isEmpty()) {
                    logger.debug("标签「{}」(ID={})找到{}个提示词", tag.getTagName(), tag.getId(), prompts.size());
                    tagPrompts.addAll(prompts);
                } else {
                    logger.debug("标签「{}」(ID={})无相关提示词", tag.getTagName(), tag.getId());
                }
            }
            
            // 按优先级排序
            tagPrompts.sort(Comparator.comparing(AnswerTagPrompt::getPromptPriority));
            
            // 添加标签prompt
            boolean isFirst = true;
            for (AnswerTagPrompt tagPrompt : tagPrompts) {
                if (!isFirst) {
                    // 使用配置的分隔符或默认分隔符
                    tagPromptsBuilder.append(
                        config.getTagPromptSeparator() != null ? 
                        config.getTagPromptSeparator() : "\n\n"
                    );
                }
                tagPromptsBuilder.append(tagPrompt.getPromptTemplate());
                isFirst = false;
                
                logger.debug("添加标签提示词：ID={}, 名称={}", tagPrompt.getId(), tagPrompt.getName());
            }
            
            if (tagPromptsBuilder.length() > 0) {
                promptBuilder.append(tagPromptsBuilder);
                
                // 添加部分分隔符
                promptBuilder.append(
                    config.getSectionSeparator() != null ? 
                    config.getSectionSeparator() : "\n\n"
                );
            } else {
                logger.debug("所有标签均无可用提示词，跳过添加标签提示部分");
            }
        } else {
            logger.debug("问题无关联标签，跳过添加标签提示部分");
        }
        
        // 添加题型提示词部分
        AnswerQuestionTypePrompt questionTypePrompt = getQuestionTypePrompt(batch, question.getQuestionType());
        
        if (questionTypePrompt != null) {
            if (config.getQuestionTypeSectionHeader() != null) {
                promptBuilder.append(config.getQuestionTypeSectionHeader()).append("\n");
            }
            
            promptBuilder.append(questionTypePrompt.getPromptTemplate());
            
            // 添加格式说明
            if (questionTypePrompt.getResponseFormatInstruction() != null && 
                !questionTypePrompt.getResponseFormatInstruction().trim().isEmpty()) {
                promptBuilder.append("\n\n").append(questionTypePrompt.getResponseFormatInstruction());
            }
            
            // 添加示例（可选）
            if (questionTypePrompt.getResponseExample() != null && 
                !questionTypePrompt.getResponseExample().trim().isEmpty()) {
                promptBuilder.append("\n例如：").append(questionTypePrompt.getResponseExample());
            }
            
            logger.debug("添加题型提示词：题型={}，提示词ID={}", 
                question.getQuestionType(), questionTypePrompt.getId());
        } else {
            logger.warn("找不到题型={}的提示词", question.getQuestionType());
        }
        
        // 添加最终指令和问题
        if (config.getFinalInstruction() != null && !config.getFinalInstruction().trim().isEmpty()) {
            promptBuilder.append("\n\n").append(config.getFinalInstruction());
        }
        
        // 添加问题文本
        promptBuilder.append("\n\n问题：").append(question.getQuestionText());
        
        // 记录完整的prompt
        String fullPrompt = promptBuilder.toString();
        logger.info("组装完成，问题ID={}的prompt长度：{}", question.getId(), fullPrompt.length());
        logger.debug("完整prompt内容：{}", fullPrompt);
        
        return fullPrompt;
    }
    
    /**
     * 调用LLM API生成回答，并支持中断检查
     */
    private String generateAnswerWithInterruptCheck(ModelAnswerRun run, String prompt, AtomicBoolean interrupted) {
        Long batchId = run.getAnswerGenerationBatch().getId();
        LlmModel model = run.getLlmModel();
        
        // API调用前检查中断状态
        if (shouldInterrupt(batchId)) {
            logger.info("API调用前检测到批次{}的中断信号，不执行API调用", batchId);
            return null;
        }
        
        // 如果外部已经设置中断标志，也直接返回
        if (interrupted.get()) {
            logger.info("检测到外部中断标志，不执行API调用");
            return null;
        }
        
        try {
            // 获取上下文变量
            Map<String, Object> contextVariables = getContextVariables(run);
            
            // 使用LlmApiService生成回答，同时传入中断检查回调
            return llmApiService.generateModelAnswer(model, prompt, contextVariables);
        } catch (Exception e) {
            logger.error("生成模型回答失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 获取上下文变量，包括批次全局参数和运行特定参数
     */
    private Map<String, Object> getContextVariables(ModelAnswerRun run) {
        Map<String, Object> parameters = new HashMap<>();
        
        // 添加批次全局参数
        if (run.getAnswerGenerationBatch().getGlobalParameters() != null) {
            parameters.putAll(run.getAnswerGenerationBatch().getGlobalParameters());
        }
        
        // 添加运行特定参数（优先级最高）
        if (run.getParameters() != null) {
            parameters.putAll(run.getParameters());
        }
        
        return parameters;
    }
    
    /**
     * 保存模型回答
     */
    public void saveModelAnswer(ModelAnswerRun run, StandardQuestion question, String answerText, int repeatIndex) {
        // 创建事务模板
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        
        txTemplate.execute(status -> {
            try {
                // 先记录原始批次信息用于调试
                logger.debug("原始批次信息: runId={}, batchId={}", 
                    run.getId(),
                    run.getAnswerGenerationBatch() != null ? run.getAnswerGenerationBatch().getId() : "NULL");
                
                // 强制重新加载批次对象，避免使用可能部分加载的引用
                Long originalBatchId = run.getAnswerGenerationBatch().getId();
                AnswerGenerationBatch freshBatch = batchRepository.findById(originalBatchId)
                    .orElseThrow(() -> new IllegalStateException("找不到ID为" + originalBatchId + "的批次"));
                
                // 用新加载的批次替换运行对象中的引用
                run.setAnswerGenerationBatch(freshBatch);
                logger.debug("重新加载的批次信息: id={}, name={}", 
                    freshBatch.getId(), freshBatch.getName());
                
                // 确保数据集版本已正确加载
                if (freshBatch.getDatasetVersion() == null) {
                    // 尝试直接从数据库查询数据集版本ID
                    Long datasetVersionId = jdbcTemplate.queryForObject(
                        "SELECT dataset_version_id FROM answer_generation_batches WHERE id = ?", 
                        Long.class, freshBatch.getId());
                    
                    if (datasetVersionId != null) {
                        // 从数据库加载完整的数据集版本
                        DatasetVersion version = jdbcTemplate.queryForObject(
                            "SELECT * FROM dataset_versions WHERE id = ?",
                            (rs, rowNum) -> {
                                DatasetVersion dv = new DatasetVersion();
                                dv.setId(rs.getLong("id"));
                                dv.setName(rs.getString("name"));
                                dv.setVersionNumber(rs.getString("version_number"));
                                return dv;
                            },
                            datasetVersionId);
                            
                        if (version != null) {
                            freshBatch.setDatasetVersion(version);
                            logger.info("从数据库直接加载数据集版本成功: versionId={}, name={}", 
                                version.getId(), version.getName());
                        }
                    }
                }
                
                LlmAnswer answer = new LlmAnswer();
                answer.setModelAnswerRun(run);
                
                // 检查批次的数据集版本是否为空
                if (run.getAnswerGenerationBatch() == null) {
                    logger.info("批次为空: runId={}, questionId={}", run.getId(), question.getId());
                    throw new IllegalStateException("批次为空，无法保存回答");
                }

                logger.debug("批次信息: id={}, name={}", 
                    run.getAnswerGenerationBatch().getId(),
                    run.getAnswerGenerationBatch().getName());

                if (run.getAnswerGenerationBatch().getDatasetVersion() == null) {
                    logger.info("数据集版本为空: runId={}, questionId={}, batchId={}", 
                        run.getId(), question.getId(), run.getAnswerGenerationBatch().getId());
                    throw new IllegalStateException("批次的数据集版本为空，无法保存回答");
                }

                logger.debug("数据集版本信息: id={}, name={}, version={}", 
                    run.getAnswerGenerationBatch().getDatasetVersion().getId(),
                    run.getAnswerGenerationBatch().getDatasetVersion().getName(),
                    run.getAnswerGenerationBatch().getDatasetVersion().getVersionNumber());
                
                // 需要从StandardQuestion获取对应的DatasetQuestionMapping
                // 直接通过ID查询，避免使用懒加载的集合
                Long datasetVersionId = run.getAnswerGenerationBatch().getDatasetVersion().getId();
                
                // 使用JDBC直接查询，替代EntityManager查询
                Map<String, Object> mappingData = jdbcTemplate.queryForMap(
                    "SELECT dqm.* FROM dataset_question_mapping dqm " +
                    "WHERE dqm.standard_question_id = ? " +
                    "AND dqm.dataset_version_id = ?", 
                    question.getId(), datasetVersionId);
                    
                // 构建DatasetQuestionMapping对象
                DatasetQuestionMapping mapping = new DatasetQuestionMapping();
                mapping.setId(((Number) mappingData.get("id")).longValue());
                
                // 设置必要的关联
                DatasetVersion datasetVersion = new DatasetVersion();
                datasetVersion.setId(datasetVersionId);
                mapping.setDatasetVersion(datasetVersion);
                
                mapping.setStandardQuestion(question);
                // 设置其他需要的字段
                
                answer.setDatasetQuestionMapping(mapping);
                answer.setAnswerText(answerText);
                answer.setRepeatIndex(repeatIndex);
                answer.setGenerationTime(LocalDateTime.now());
                answer.setGenerationStatus(LlmAnswer.GenerationStatus.SUCCESS);
                
                // 可以添加其他字段
                answer.setPromptUsed(assemblePrompt(run, question));
                
                answerRepository.save(answer);
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("保存模型回答失败: questionId={}, runId={}", question.getId(), run.getId(), e);
                throw e;
            }
        });
    }
    
    /**
     * 记录失败的问题
     */
    public void recordFailedQuestion(ModelAnswerRun run, Long questionId) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            try {
                run.setFailedQuestionsCount(run.getFailedQuestionsCount() + 1);
                
                // 添加到失败问题ID列表
                List<Long> failedIds = run.getFailedQuestionsIds();
                if (failedIds == null) {
                    failedIds = new ArrayList<>();
                }
                failedIds.add(questionId);
                run.setFailedQuestionsIds(failedIds);
                
                runRepository.save(run);
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("记录失败问题时出错: runId={}, questionId={}", run.getId(), questionId, e);
                throw e;
            }
        });
    }
    
    /**
     * 刷新运行状态
     */
    private ModelAnswerRun refreshRunStatus(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("运行不存在: " + runId));
    }
    
    /**
     * 更新运行进度
     */
    public void updateRunProgress(ModelAnswerRun run, int completedQuestions, int failedQuestions, int totalQuestions) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            try {
                // 更新运行进度信息
                run.setLastProcessedQuestionId(null);
                run.setLastProcessedQuestionIndex(-1);
                run.setLastActivityTime(LocalDateTime.now());
                
                run.setCompletedQuestionsCount(completedQuestions);
                run.setFailedQuestionsCount(failedQuestions);
                
                // 计算进度百分比
                BigDecimal progressPercentage = BigDecimal.valueOf((double) completedQuestions / totalQuestions * 100)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                run.setProgressPercentage(progressPercentage);
                
                runRepository.save(run);
                
                // 发送WebSocket进度更新通知
                sendRunProgressNotification(run, progressPercentage.doubleValue(), 
                        "已处理 " + completedQuestions + "/" + totalQuestions + " 个问题");
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("更新运行{}进度失败", run.getId(), e);
                throw e;
            }
        });
    }
    
    /**
     * 更新运行状态
     */
    public void updateRunStatus(ModelAnswerRun run, RunStatus status, String errorMessage) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(transStatus -> {
            try {
                run.setStatus(status);
                run.setLastActivityTime(LocalDateTime.now());
                
                if (errorMessage != null) {
                    run.setErrorMessage(errorMessage);
                }
                
                if (status == RunStatus.COMPLETED) {
                    run.setProgressPercentage(BigDecimal.valueOf(100));
                }
                
                runRepository.saveAndFlush(run);
                
                // 发送状态变更通知
                webSocketService.sendStatusChangeMessage(run.getId(), status.name(), 
                        "运行状态变更为: " + status.name());
                return null;
            } catch (Exception e) {
                transStatus.setRollbackOnly();
                logger.error("更新运行{}状态失败", run.getId(), e);
                throw e;
            }
        });
    }
    
    /**
     * 更新批次状态
     */
    public void updateBatchStatus(AnswerGenerationBatch batch, BatchStatus status, String errorMessage) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(transStatus -> {
            try {
                batch.setStatus(status);
                batch.setLastActivityTime(LocalDateTime.now());
                
                if (errorMessage != null) {
                    batch.setErrorMessage(errorMessage);
                }
                
                if (status == BatchStatus.COMPLETED) {
                    batch.setCompletedAt(LocalDateTime.now());
                    batch.setProgressPercentage(BigDecimal.valueOf(100));
                }
                
                batchRepository.saveAndFlush(batch);
                
                // 同步状态到Redis
                if (batchStateManager != null) {
                    batchStateManager.setBatchState(batch.getId(), status.name());
                    
                    // 根据状态设置中断标志
                    if (status == BatchStatus.PAUSED) {
                        batchStateManager.setInterruptFlag(batch.getId(), true);
                    } else {
                        batchStateManager.setInterruptFlag(batch.getId(), false);
                    }
                    
                    logger.debug("批次{}状态已同步到Redis: {}", batch.getId(), status);
                }
                
                // 发送批次状态变更通知
                Map<String, Object> payload = new HashMap<>();
                payload.put("batchId", batch.getId());
                payload.put("status", status.name());
                payload.put("timestamp", System.currentTimeMillis());
                
                if (errorMessage != null) {
                    payload.put("error", errorMessage);
                }
                
                MessageType messageType = (status == BatchStatus.COMPLETED) ? 
                        MessageType.TASK_COMPLETED : MessageType.STATUS_CHANGE;
                
                webSocketService.sendBatchMessage(batch.getId(), messageType, payload);
                
                logger.info("批次{}状态已更新为: {}", batch.getId(), status);
                return null;
            } catch (Exception e) {
                transStatus.setRollbackOnly();
                logger.error("更新批次{}状态失败", batch.getId(), e);
                throw e;
            }
        });
    }
    
    /**
     * 检查并更新批次完成状态
     */
    public boolean checkAndUpdateBatchCompletion(AnswerGenerationBatch batch) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate.execute(status -> {
            try {
                List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batch.getId());
                
                // 检查所有运行是否完成
                boolean allCompleted = runs.stream()
                        .allMatch(run -> run.getStatus() == RunStatus.COMPLETED || run.getStatus() == RunStatus.FAILED);
                
                if (allCompleted) {
                    // 计算批次总体进度 - 添加空值检查
                    BigDecimal totalProgress = BigDecimal.ZERO;
                    int validRuns = 0;
                    
                    // 遍历所有运行，统计有效进度值
                    for (ModelAnswerRun run : runs) {
                        BigDecimal progress = run.getProgressPercentage();
                        if (progress != null) {
                            totalProgress = totalProgress.add(progress);
                            validRuns++;
                        }
                    }
                    
                    // 只有当有有效运行时才计算平均值
                    if (validRuns > 0) {
                        totalProgress = totalProgress.divide(
                            BigDecimal.valueOf(validRuns), 
                            2, 
                            java.math.RoundingMode.HALF_UP
                        );
                    } else {
                        // 如果没有有效进度，设置默认值
                        totalProgress = BigDecimal.valueOf(100.00);
                        logger.warn("批次{}没有有效的进度数据，设置默认进度为100%", batch.getId());
                    }
                    
                    batch.setProgressPercentage(totalProgress);
                    
                    // 检查是否存在失败的运行
                    boolean hasFailed = runs.stream().anyMatch(run -> run.getStatus() == RunStatus.FAILED);
                    
                    // 检查当前批次状态，避免从PENDING直接变为COMPLETED
                    if (batch.getStatus() == BatchStatus.PENDING) {
                        logger.warn("批次{}状态异常: 从PENDING直接尝试变为COMPLETED", batch.getId());
                        updateBatchStatus(batch, BatchStatus.GENERATING_ANSWERS, null);
                        
                        // 添加延迟确保状态能被正确更新
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        // 重新获取批次状态
                        AnswerGenerationBatch refreshedBatch = batchRepository.findById(batch.getId()).orElse(batch);
                        
                        if (hasFailed) {
                            updateBatchStatus(refreshedBatch, BatchStatus.FAILED, "部分运行失败");
                        } else {
                            updateBatchStatus(refreshedBatch, BatchStatus.COMPLETED, null);
                        }
                    } else {
                        if (hasFailed) {
                            updateBatchStatus(batch, BatchStatus.FAILED, "部分运行失败");
                        } else {
                            updateBatchStatus(batch, BatchStatus.COMPLETED, null);
                        }
                    }
                }
                
                return allCompleted;
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("检查批次{}完成状态失败", batch.getId(), e);
                throw e;
            }
        });
    }
    
    /**
     * 发送运行开始通知
     */
    private void sendRunStartNotification(ModelAnswerRun run) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("runName", run.getRunName());
        payload.put("modelName", run.getLlmModel().getName());
        payload.put("startTime", LocalDateTime.now());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.TASK_STARTED, payload);
    }
    
    /**
     * 发送运行完成通知
     */
    private void sendRunCompletionNotification(ModelAnswerRun run) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("runName", run.getRunName());
        payload.put("modelName", run.getLlmModel().getName());
        payload.put("completedTime", LocalDateTime.now());
        payload.put("completedCount", run.getCompletedQuestionsCount());
        payload.put("failedCount", run.getFailedQuestionsCount());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.TASK_COMPLETED, payload);
    }
    
    /**
     * 发送运行进度通知
     */
    private void sendRunProgressNotification(ModelAnswerRun run, double progress, String message) {
        webSocketService.sendRunProgressMessage(run.getId(), progress, message);
    }
    
    /**
     * 发送问题开始处理通知
     */
    private void sendQuestionStartedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_STARTED, payload);
    }
    
    /**
     * 发送问题完成处理通知
     */
    private void sendQuestionCompletedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex, int currentCompletedCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        payload.put("completedCount", currentCompletedCount);
        payload.put("timestamp", System.currentTimeMillis());
        
        // 使用异步方式发送消息，避免线程上下文问题
        try {
            // 添加短暂延迟，确保事务完全提交
            Thread.sleep(10);
            
            logger.info("即将发送问题完成通知: 运行ID={}, 问题ID={}, 完成数量={}", 
                run.getId(), question.getId(), currentCompletedCount);
            
            webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_COMPLETED, payload);
            
            logger.info("问题完成通知发送完成: 运行ID={}, 问题ID={}", 
                run.getId(), question.getId());
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("发送问题完成通知时被中断: 运行ID={}, 问题ID={}", 
                run.getId(), question.getId());
        } catch (Exception e) {
            logger.error("发送问题完成通知失败: 运行ID={}, 问题ID={}, 错误={}", 
                run.getId(), question.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送问题处理失败通知
     */
    private void sendQuestionFailedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("error", error);
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_FAILED, payload);
    }

    /**
     * 发送批次完成通知
     */
    private void sendBatchCompletedNotification(AnswerGenerationBatch batch) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", batch.getId());
        payload.put("status", "COMPLETED");
        payload.put("timestamp", System.currentTimeMillis());
        
        webSocketService.sendBatchMessage(batch.getId(), MessageType.TASK_COMPLETED, payload);
    }

    /**
     * 更新批次内存状态
     * @param batchId 批次ID
     * @param status 状态
     */
    public void updateBatchMemoryState(Long batchId, String status) {
        logger.info("更新批次{}内存状态为: {}", batchId, status);
        
        // 根据状态设置中断标志
        if ("PAUSED".equals(status)) {
            markForInterruption(batchId, "MEMORY_STATE_UPDATE");
        } else if ("GENERATING_ANSWERS".equals(status) || "RESUMING".equals(status)) {
            clearInterruptionFlag(batchId);
        }
        
        // 如果batchStateManager未初始化，跳过Redis操作
        if (batchStateManager == null) {
            logger.warn("batchStateManager尚未注入，跳过Redis状态同步");
            return;
        }
        
        // 同步Redis状态
        batchStateManager.setBatchState(batchId, status);
        
        if ("PAUSED".equals(status)) {
            batchStateManager.setInterruptFlag(batchId, true);
        } else {
            batchStateManager.setInterruptFlag(batchId, false);
        }
    }

    /**
     * 根据题型获取对应的提示词
     */
    private AnswerQuestionTypePrompt getQuestionTypePrompt(AnswerGenerationBatch batch, QuestionType questionType) {
        if (questionType == null) {
            logger.warn("题型为null，无法获取对应提示词");
            return null;
        }
        
        AnswerQuestionTypePrompt prompt = null;
        
        // 首先尝试从批次中获取预设的题型提示词
        switch (questionType) {
            case SINGLE_CHOICE:
                prompt = batch.getSingleChoicePrompt();
                break;
            case MULTIPLE_CHOICE:
                prompt = batch.getMultipleChoicePrompt();
                break;
            case SIMPLE_FACT:
                prompt = batch.getSimpleFactPrompt();
                break;
            case SUBJECTIVE:
                prompt = batch.getSubjectivePrompt();
                break;
            default:
                logger.warn("未知题型: {}", questionType);
                return null;
        }
        
        // 如果批次没有预设提示词，则从仓库中查询激活的提示词
        if (prompt == null) {
            logger.info("批次{}未设置{}题型提示词，尝试从仓库获取", batch.getId(), questionType);
            List<AnswerQuestionTypePrompt> prompts = answerQuestionTypePromptRepository
                .findByQuestionTypeAndIsActiveTrueAndDeletedAtIsNull(questionType);
            
            if (!prompts.isEmpty()) {
                prompt = prompts.get(0);
                logger.info("从仓库获取到{}题型的提示词：ID={}，名称={}", 
                    questionType, prompt.getId(), prompt.getName());
                
                // 将提示词设置到批次对象中，提高后续使用效率
                switch (questionType) {
                    case SINGLE_CHOICE:
                        batch.setSingleChoicePrompt(prompt);
                        break;
                    case MULTIPLE_CHOICE:
                        batch.setMultipleChoicePrompt(prompt);
                        break;
                    case SIMPLE_FACT:
                        batch.setSimpleFactPrompt(prompt);
                        break;
                    case SUBJECTIVE:
                        batch.setSubjectivePrompt(prompt);
                        break;
                }
                
                // 更新批次对象到数据库
                try {
                    batchRepository.save(batch);
                } catch (Exception e) {
                    logger.warn("保存批次{}的题型提示词失败: {}", batch.getId(), e.getMessage());
                }
            } else {
                logger.warn("在仓库中未找到{}题型的提示词", questionType);
            }
        }
        
        return prompt;
    }
} 