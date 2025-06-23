package com.example.demo.controller;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CreateEvaluationRunRequest;
import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.EvaluationDetail;
import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.LlmAnswerRepository;
import com.example.demo.service.EvaluationService;
import com.example.demo.util.ApiConstants;

import lombok.Data;

@RestController
@RequestMapping("/evaluations")
@CrossOrigin(origins = "*")
public class EvaluationController {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationController.class);
    
    private final EvaluationService evaluationService;
    private final LlmAnswerRepository llmAnswerRepository;
    
    public EvaluationController(
            EvaluationService evaluationService,
            LlmAnswerRepository llmAnswerRepository) {
        this.evaluationService = evaluationService;
        this.llmAnswerRepository = llmAnswerRepository;
    }
    
    /**
     * 评测单个LLM回答
     */
    @PostMapping("/answers/{answerId}")
    public ResponseEntity<Evaluation> evaluateAnswer(
            @PathVariable Long answerId,
            @RequestParam Long evaluatorId,
            @RequestParam Long userId) {
        
        logger.info("接收到评测请求，回答ID: {}, 评测者ID: {}, 用户ID: {}", answerId, evaluatorId, userId);
        
        LlmAnswer answer = llmAnswerRepository.findByIdWithQuestion(answerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的回答: " + answerId));
        
        Evaluation evaluation = evaluationService.evaluateAnswer(answer, evaluatorId, userId);
        
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * 批量评测LLM回答
     */
    @PostMapping("/batch")
    public ResponseEntity<List<Evaluation>> evaluateAnswers(
            @RequestBody BatchAnswerEvaluationRequest request) {
        
        logger.info("接收到批量评测请求，回答数量: {}, 评测者ID: {}, 用户ID: {}",
                request.getAnswerIds().size(), request.getEvaluatorId(), request.getUserId());
        
        List<LlmAnswer> answers = llmAnswerRepository.findAllByIdWithQuestions(request.getAnswerIds());
        
        if (answers.size() != request.getAnswerIds().size()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Evaluation> evaluations = evaluationService.evaluateAnswers(
                answers, request.getEvaluatorId(), request.getUserId());
        
        return ResponseEntity.ok(evaluations);
    }
    
    /**
     * 手动评测单选题
     */
    @PostMapping("/manual/single-choice")
    public ResponseEntity<Map<String, Object>> evaluateSingleChoice(
            @RequestBody ManualEvaluationRequest request) {
        
        logger.info("接收到手动单选题评测请求");
        
        Map<String, Object> result = evaluationService.evaluateSingleChoice(
                request.getAnswerText(), request.getCorrectIds(), request.getOptions());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动评测多选题
     */
    @PostMapping("/manual/multiple-choice")
    public ResponseEntity<Map<String, Object>> evaluateMultipleChoice(
            @RequestBody ManualEvaluationRequest request) {
        
        logger.info("接收到手动多选题评测请求");
        
        Map<String, Object> result = evaluationService.evaluateMultipleChoice(
                request.getAnswerText(), request.getCorrectIds(), request.getOptions());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动评测简单事实题
     */
    @PostMapping("/manual/simple-fact")
    public ResponseEntity<Map<String, Object>> evaluateSimpleFact(
            @RequestBody ManualEvaluationRequest request) {
        
        logger.info("接收到手动简单事实题评测请求");
        
        Map<String, Object> result = evaluationService.evaluateSimpleFact(
                request.getAnswerText(), request.getStandardAnswer(), request.getAlternativeAnswers());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 使用AI评测主观题
     */
    @PostMapping("/manual/subjective/ai")
    public ResponseEntity<Map<String, Object>> evaluateSubjectiveWithAI(
            @RequestBody SubjectiveAIEvaluationRequest request) {
        
        logger.info("接收到AI主观题评测请求");
        
        Map<String, Object> result = evaluationService.evaluateSubjectiveWithAI(
                request.getAnswerText(), 
                request.getQuestionText(), 
                request.getReferenceAnswer(), 
                request.getCriteria(),
                request.getEvaluatorId());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 创建人工评测记录（用于主观题人工评分）
     */
    @PostMapping("/human/create")
    public ResponseEntity<Evaluation> createHumanEvaluation(
            @RequestBody CreateHumanEvaluationRequest request) {
        
        logger.info("接收到创建人工评测记录请求，回答ID: {}", request.getLlmAnswerId());
        
        Evaluation evaluation = evaluationService.createHumanEvaluation(
                request.getLlmAnswerId(), 
                request.getEvaluatorId(), 
                request.getUserId());
        
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * 提交人工评测结果
     */
    @PostMapping("/human/submit")
    public ResponseEntity<Evaluation> submitHumanEvaluation(
            @RequestBody SubmitHumanEvaluationRequest request) {
        
        logger.info("接收到提交人工评测结果请求，评测ID: {}", request.getEvaluationId());
        
        Evaluation evaluation = evaluationService.submitHumanEvaluation(
                request.getEvaluationId(), 
                request.getOverallScore(), 
                request.getComments(), 
                request.getDetailScores(), 
                request.getUserId());
        
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * 重新评测主观题答案（强制覆盖已有评测）
     */
    @PostMapping("/subjective/re-evaluate/{answerId}")
    public ResponseEntity<Map<String, Object>> reEvaluateSubjectiveAnswer(
            @PathVariable Long answerId,
            @RequestParam Long evaluatorId,
            @RequestParam Long userId) {
        
        logger.info("接收到重新评测主观题请求，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                answerId, evaluatorId, userId);
        
        try {
            BigDecimal score = evaluationService.reEvaluateSubjectiveAnswer(answerId, evaluatorId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("answerId", answerId);
            result.put(ApiConstants.KEY_SCORE, score);
            result.put(ApiConstants.KEY_SUCCESS, true);
            result.put(ApiConstants.KEY_MESSAGE, "重新评测成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("处理重新评测请求时发生异常", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("answerId", answerId);
            result.put(ApiConstants.KEY_SUCCESS, false);
            result.put(ApiConstants.KEY_MESSAGE, "重新评测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取评测详情
     */
    @GetMapping("/{evaluationId}/details")
    public ResponseEntity<List<EvaluationDetail>> getEvaluationDetails(
            @PathVariable Long evaluationId) {
        
        logger.info("接收到获取评测详情请求，评测ID: {}", evaluationId);
        
        List<EvaluationDetail> details = evaluationService.getEvaluationDetails(evaluationId);
        
        return ResponseEntity.ok(details);
    }
    
    /**
     * 获取回答的所有评分详情（分页）
     * 支持查看每道题的所有评测员评分，包括各个评测标准的详细得分
     * 
     * @param answerId 回答ID（可选）
     * @param batchId 批次ID（可选）
     * @param questionId 问题ID（可选）
     * @param modelIds 模型ID列表（可选）
     * @param evaluatorIds 评测员ID列表（可选）
     * @param questionType 问题类型（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 评分详情分页结果
     */
    @GetMapping("/answer-evaluations")
    public ResponseEntity<Map<String, Object>> getAnswerEvaluationDetails(
            @RequestParam(required = false) Long answerId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(required = false) List<Long> evaluatorIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取回答评分详情请求 - 回答ID: {}, 批次ID: {}, 问题ID: {}, 模型IDs: {}, 评测员IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                answerId, batchId, questionId, modelIds, evaluatorIds, questionType, page, size);
        
        try {
            Map<String, Object> result = evaluationService.getAnswerEvaluationDetails(
                    answerId, batchId, questionId, modelIds, evaluatorIds, questionType, page, size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取回答评分详情失败", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "获取评分详情失败: " + e.getMessage());
            errorResult.put("items", Collections.emptyList());
            errorResult.put("totalItems", 0);
            errorResult.put("totalPages", 0);
            errorResult.put("currentPage", page);
            errorResult.put("pageSize", size);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * 获取特定问题类型的评测标准
     */
    @GetMapping("/criteria")
    public ResponseEntity<List<EvaluationCriterion>> getEvaluationCriteria(
            @RequestParam QuestionType questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取评测标准请求，问题类型: {}, 页码: {}, 每页大小: {}", questionType, page, size);
        
        List<EvaluationCriterion> criteria = evaluationService.getCriteriaForQuestionType(questionType, page, size);
        
        return ResponseEntity.ok(criteria);
    }
    
    /**
     * 创建评测标准
     */
    @PostMapping("/criteria")
    public ResponseEntity<EvaluationCriterion> createEvaluationCriterion(
            @RequestBody EvaluationCriterion criterion,
            @RequestParam Long userId) {
        
        logger.info("接收到创建评测标准请求，标准名称: {}", criterion.getName());
        
        // 设置创建者
        User user = new User();
        user.setId(userId);
        criterion.setCreatedByUser(user);
        
        EvaluationCriterion savedCriterion = evaluationService.saveEvaluationCriterion(criterion);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCriterion);
    }
    
    /**
     * 更新评测标准
     */
    @PutMapping("/criteria/{criterionId}")
    public ResponseEntity<EvaluationCriterion> updateEvaluationCriterion(
            @PathVariable Long criterionId,
            @RequestBody EvaluationCriterion criterion,
            @RequestParam Long userId) {
        
        logger.info("接收到更新评测标准请求，标准ID: {}", criterionId);
        
        // 确保ID一致
        criterion.setId(criterionId);
        
        // 设置更新者
        User user = new User();
        user.setId(userId);
        criterion.setCreatedByUser(user);
        
        EvaluationCriterion updatedCriterion = evaluationService.saveEvaluationCriterion(criterion);
        
        return ResponseEntity.ok(updatedCriterion);
    }
    
    /**
     * 删除评测标准
     */
    @DeleteMapping("/criteria/{criterionId}")
    public ResponseEntity<Map<String, Object>> deleteEvaluationCriterion(
            @PathVariable Long criterionId,
            @RequestParam Long userId) {
        
        logger.info("接收到删除评测标准请求，标准ID: {}", criterionId);
        
        evaluationService.deleteEvaluationCriterion(criterionId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "评测标准已删除");
        response.put("criterionId", criterionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取评测标准详情
     */
    @GetMapping("/criteria/{criterionId}")
    public ResponseEntity<EvaluationCriterion> getEvaluationCriterionById(
            @PathVariable Long criterionId) {
        
        logger.info("接收到获取评测标准详情请求，标准ID: {}", criterionId);
        
        EvaluationCriterion criterion = evaluationService.getEvaluationCriterionById(criterionId);
        
        return ResponseEntity.ok(criterion);
    }
    
    /**
     * 获取所有评测标准
     */
    @GetMapping("/criteria/all")
    public ResponseEntity<List<EvaluationCriterion>> getAllEvaluationCriteria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取所有评测标准请求，页码: {}, 每页大小: {}", page, size);
        
        List<EvaluationCriterion> criteria = evaluationService.getAllEvaluationCriteria(page, size);
        
        return ResponseEntity.ok(criteria);
    }
    
    /**
     * 创建评测运行
     */
    @PostMapping("/runs")
    public ResponseEntity<EvaluationRun> createEvaluationRun(@RequestBody CreateEvaluationRunRequest request) {
        logger.info("接收到创建评测运行请求");
        
        EvaluationRun evaluationRun = evaluationService.createEvaluationRun(
                request.getModelAnswerRunId(),
                request.getEvaluatorId(),
                request.getRunName(),
                request.getRunDescription(),
                request.getParameters(),
                request.getUserId());
        
        return ResponseEntity.ok(evaluationRun);
    }
    
    /**
     * 启动评测运行
     */
    @PostMapping("/runs/{runId}/start")
    public ResponseEntity<Map<String, Object>> startEvaluationRun(@PathVariable("runId") Long runId) {
        logger.info("接收到启动评测运行请求，运行ID: {}", runId);
        
        // 异步启动评测运行
        evaluationService.startEvaluationRun(runId);
        
        Map<String, Object> response = new HashMap<>();
        response.put(ApiConstants.KEY_MESSAGE, "评测运行已启动");
        response.put(ApiConstants.KEY_RUN_ID, runId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 暂停评测运行
     */
    @PostMapping("/runs/{runId}/pause")
    public ResponseEntity<Map<String, Object>> pauseEvaluationRun(@PathVariable("runId") Long runId) {
        logger.info("接收到暂停评测运行请求，运行ID: {}", runId);
        
        boolean paused = evaluationService.pauseEvaluationRun(runId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("paused", paused);
        response.put(ApiConstants.KEY_RUN_ID, runId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 恢复评测运行
     */
    @PostMapping("/runs/{runId}/resume")
    public ResponseEntity<Map<String, Object>> resumeEvaluationRun(@PathVariable("runId") Long runId) {
        logger.info("接收到恢复评测运行请求，运行ID: {}", runId);
        
        // 异步恢复评测运行
        evaluationService.resumeEvaluationRun(runId);
        
        Map<String, Object> response = new HashMap<>();
        response.put(ApiConstants.KEY_MESSAGE, "评测运行已恢复");
        response.put(ApiConstants.KEY_RUN_ID, runId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取评测运行进度
     */
    @GetMapping("/runs/{runId}/progress")
    public ResponseEntity<Map<String, Object>> getEvaluationRunProgress(@PathVariable("runId") Long runId) {
        logger.info("接收到获取评测运行进度请求，运行ID: {}", runId);
        
        Map<String, Object> progress = evaluationService.getEvaluationRunProgress(runId);
        
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 获取评测运行列表
     */
    @GetMapping("/runs")
    public ResponseEntity<List<EvaluationRun>> getEvaluationRuns(
            @RequestParam(value = "modelAnswerRunId", required = false) Long modelAnswerRunId,
            @RequestParam(value = "evaluatorId", required = false) Long evaluatorId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("接收到获取评测运行列表请求");
        
        // 这里需要实现查询评测运行列表的方法
        List<EvaluationRun> runs = evaluationService.getEvaluationRuns(modelAnswerRunId, evaluatorId, status, page, size);
        
        return ResponseEntity.ok(runs);
    }
    
    /**
     * 获取评测运行详情
     */
    @GetMapping("/runs/{runId}")
    public ResponseEntity<EvaluationRun> getEvaluationRun(@PathVariable("runId") Long runId) {
        logger.info("接收到获取评测运行详情请求，运行ID: {}", runId);
        
        EvaluationRun evaluationRun = evaluationService.getEvaluationRun(runId);
        
        return ResponseEntity.ok(evaluationRun);
    }
    
    /**
     * 获取评测运行结果
     */
    @GetMapping("/runs/{runId}/results")
    public ResponseEntity<Map<String, Object>> getEvaluationRunResults(@PathVariable("runId") Long runId) {
        logger.info("接收到获取评测运行结果请求，运行ID: {}", runId);
        
        Map<String, Object> results = evaluationService.getEvaluationRunResults(runId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取客观题机器评测详细结果
     * 返回客观题评测详细结果，包括每个问题的模型回答、标准答案和评分，以及每个模型的平均分
     */
    @GetMapping("/objective/results")
    public ResponseEntity<Map<String, Object>> getObjectiveDetailedResults(
            @RequestParam Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取客观题评测详细结果请求，批次ID: {}, 模型IDs: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, page, size);
        
        Map<String, Object> results = evaluationService.getObjectiveDetailedResults(batchId, modelIds, page, size);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取主观题大模型评测详细结果
     * 返回主观题大模型评测详细结果，包括评测标准得分、总体评语和改进建议
     */
    @GetMapping("/subjective/results")
    public ResponseEntity<Map<String, Object>> getSubjectiveDetailedResults(
            @RequestParam Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(required = false) Long evaluatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取主观题评测详细结果请求，批次ID: {}, 模型IDs: {}, 评测者ID: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, evaluatorId, page, size);
        
        Map<String, Object> results;
        if (evaluatorId != null) {
            // 如果指定了评测者ID，返回该评测者的结果
            results = evaluationService.getSubjectiveDetailedResults(batchId, modelIds, evaluatorId, page, size);
        } else {
            // 如果没有指定评测者ID，返回所有评测者的结果
            results = evaluationService.getSubjectiveDetailedResultsWithAllEvaluators(batchId, modelIds, page, size);
        }
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取主观题所有评测员的评测详细结果
     * 返回主观题所有评测员的评测详细结果，包括每个评测员的评分、评语和评测详情
     */
    @GetMapping("/subjective/results/all-evaluators")
    public ResponseEntity<Map<String, Object>> getSubjectiveDetailedResultsWithAllEvaluators(
            @RequestParam Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取主观题所有评测员的评测详细结果请求，批次ID: {}, 模型IDs: {}, 页码: {}, 每页大小: {}", 
                batchId, modelIds, page, size);
        
        Map<String, Object> results = evaluationService.getSubjectiveDetailedResultsWithAllEvaluators(batchId, modelIds, page, size);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 评测一个批次中的所有客观题（单选题、多选题和简单事实题）
     */
    @PostMapping("/batch/{batchId}/objective-questions")
    public ResponseEntity<Map<String, Object>> evaluateBatchObjectiveQuestions(
            @PathVariable Long batchId,
            @RequestParam Long evaluatorId,
            @RequestParam Long userId) {
        
        logger.info("接收到批次客观题评测请求，批次ID: {}, 评测者ID: {}, 用户ID: {}", batchId, evaluatorId, userId);
        
        Map<String, Object> result = evaluationService.evaluateBatchObjectiveQuestions(batchId, evaluatorId, userId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量评测客观题
     */
    @PostMapping("/batch/objective")
    public ResponseEntity<Map<String, Object>> evaluateBatchObjectiveQuestions(
            @RequestBody BatchEvaluationRequest request) {
        
        logger.info("接收到批量评测客观题请求，批次ID: {}", request.getBatchId());
        
        Map<String, Object> result = evaluationService.evaluateBatchObjectiveQuestions(
                request.getBatchId(), request.getEvaluatorId(), request.getUserId());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量评测主观题
     */
    @PostMapping("/batch/subjective")
    public ResponseEntity<Map<String, Object>> evaluateBatchSubjectiveQuestions(
            @RequestBody BatchEvaluationRequest request) {
        
        logger.info("接收到批量评测主观题请求，批次ID: {}", request.getBatchId());
        
        try {
            // 创建评测运行记录
            EvaluationRun evaluationRun = evaluationService.getOrCreateEvaluationRun(
                    request.getBatchId(), request.getEvaluatorId(), request.getUserId());
            
            // 异步执行评测任务
            CompletableFuture.runAsync(() -> {
                try {
                    evaluationService.evaluateBatchSubjectiveQuestions(
                            request.getBatchId(), 
                            request.getEvaluatorId(), 
                            request.getUserId(),
                            request.getSubjectivePromptId(),
                            request.getEvaluationAssemblyConfigId(),
                            request.getCriteriaIds());
                } catch (Exception e) {
                    logger.error("异步批量评测主观题失败", e);
                }
            });
            
            // 立即返回响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "批量评测已开始");
            result.put("evaluationRunId", evaluationRun.getId());
            result.put("batchId", request.getBatchId());
            result.put("status", "PROCESSING");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("处理评测请求时发生异常", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "批量评测启动失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取待人工评测的回答列表
     * 返回未被指定用户评测过的回答，并附上对应的标准问题和标准答案
     */
    @GetMapping("/human/pending")
    public ResponseEntity<Map<String, Object>> getPendingHumanEvaluations(
            @RequestParam Long userId,
            @RequestParam Long evaluatorId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("接收到获取待人工评测回答列表请求，用户ID: {}, 评测者ID: {}, 批次ID: {}, 模型IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        Map<String, Object> results = evaluationService.getPendingHumanEvaluations(
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取用户已评测的回答列表
     * 返回用户已评测过的回答列表及评测结果
     */
    @GetMapping("/human/completed")
    public ResponseEntity<Map<String, Object>> getCompletedHumanEvaluations(
            @RequestParam Long userId,
            @RequestParam Long evaluatorId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("接收到获取已完成人工评测回答列表请求，用户ID: {}, 评测者ID: {}, 批次ID: {}, 模型IDs: {}, 问题类型: {}, 页码: {}, 每页大小: {}", 
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        Map<String, Object> results = evaluationService.getCompletedHumanEvaluations(
                userId, evaluatorId, batchId, modelIds, questionType, page, size);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 一步式人工评测
     */
    @PostMapping("/human/one-step")
    public ResponseEntity<Map<String, Object>> oneStepHumanEvaluation(
            @RequestBody CreateAndSubmitHumanEvaluationRequest request) {
        
        logger.info("接收到一步式人工评测请求，回答ID: {}, 用户ID: {}", 
                request.getLlmAnswerId(), request.getUserId());

        try {
            Evaluation evaluation = evaluationService.oneStepHumanEvaluation(
                    request.getLlmAnswerId(), request.getUserId());
            
            // 构建API响应
            Map<String, Object> response = new HashMap<>();
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "评测提交成功");
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("evaluationId", evaluation.getId());
            data.put("llmAnswerId", evaluation.getLlmAnswer().getId());
            data.put("evaluatorId", evaluation.getEvaluator().getId());
            data.put("overallScore", evaluation.getScore());
            data.put(ApiConstants.KEY_COMMENTS, evaluation.getComments());
            
            // 获取详细评分项
            List<EvaluationDetail> details = evaluationService.getEvaluationDetails(evaluation.getId());
            List<Map<String, Object>> detailScores = new ArrayList<>();
            
            for (EvaluationDetail detail : details) {
                Map<String, Object> detailScore = new HashMap<>();
                detailScore.put("id", detail.getId());
                // 添加空值检查，如果criterion为null，则使用criterionName作为标识
                if (detail.getCriterion() != null) {
                    detailScore.put("criterionId", detail.getCriterion().getId());
                } else {
                    // 使用criterionName作为标识，或者提供一个默认值
                    detailScore.put("criterionId", null);
                }
                detailScore.put("criterionName", detail.getCriterionName());
                detailScore.put(ApiConstants.KEY_SCORE, detail.getScore());
                detailScore.put(ApiConstants.KEY_COMMENTS, detail.getComments());
                detailScores.add(detailScore);
            }
            
            data.put("detailScores", detailScores);
            data.put("createdBy", evaluation.getCreatedByUser().getId());
            data.put("createdAt", evaluation.getCreationTime());
            data.put(ApiConstants.KEY_STATUS, evaluation.getStatus());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理一步式人工评测请求时发生异常", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ApiConstants.KEY_SUCCESS, false);
            errorResponse.put(ApiConstants.KEY_MESSAGE, "评测提交失败");
            errorResponse.put(ApiConstants.KEY_ERROR, Map.of(
                "code", "EVALUATION_ERROR",
                "details", e.getMessage()
            ));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 批量重新评测主观题（强制覆盖已有评测）
     */
    @PostMapping("/batch/subjective/re-evaluate")
    public ResponseEntity<Map<String, Object>> reEvaluateBatchSubjectiveQuestions(
            @RequestBody BatchEvaluationRequest request) {
        
        logger.info("接收到批量重新评测主观题请求，批次ID: {}", request.getBatchId());
        
        try {
            Map<String, Object> result = evaluationService.reEvaluateBatchSubjectiveQuestions(
                    request.getBatchId(), 
                    request.getEvaluatorId(), 
                    request.getUserId(),
                    request.getSubjectivePromptId(),
                    request.getEvaluationAssemblyConfigId(),
                    request.getCriteriaIds());
            
            result.put("success", true);
            result.put("message", "批量重新评测成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("处理批量重新评测请求时发生异常", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", request.getBatchId());
            result.put("success", false);
            result.put("message", "批量重新评测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 直接通过路径参数进行批量重新评测
     */
    @PostMapping("/batch/{batchId}/subjective/re-evaluate")
    public ResponseEntity<Map<String, Object>> reEvaluateBatchSubjectiveQuestionsByPath(
            @PathVariable Long batchId,
            @RequestParam Long evaluatorId,
            @RequestParam Long userId) {
        
        logger.info("接收到批量重新评测主观题请求，批次ID: {}, 评测者ID: {}, 用户ID: {}", 
                batchId, evaluatorId, userId);
        
        try {
            Map<String, Object> result = evaluationService.reEvaluateBatchSubjectiveQuestions(
                    batchId, evaluatorId, userId);
            
            result.put("success", true);
            result.put("message", "批量重新评测成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("处理批量重新评测请求时发生异常", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", batchId);
            result.put("success", false);
            result.put("message", "批量重新评测失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 创建并提交人工评测（一步式操作）
     */
    @PostMapping("/human")
    public ResponseEntity<Map<String, Object>> createAndSubmitHumanEvaluation(
            @RequestBody CreateAndSubmitHumanEvaluationRequest request) {
        
        logger.info("接收到一步式提交人工评测请求，回答ID: {}", request.getLlmAnswerId());
        
        try {
            Evaluation evaluation = evaluationService.createAndSubmitHumanEvaluation(
                    request.getLlmAnswerId(), 
                    request.getEvaluatorId(), 
                    request.getOverallScore(), 
                    request.getComments(), 
                    request.getDetailScores(), 
                    request.getUserId());
            
            // 构建API响应
            Map<String, Object> response = new HashMap<>();
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "评测提交成功");
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("evaluationId", evaluation.getId());
            data.put("llmAnswerId", evaluation.getLlmAnswer().getId());
            data.put("evaluatorId", evaluation.getEvaluator().getId());
            data.put("overallScore", evaluation.getScore());
            data.put(ApiConstants.KEY_COMMENTS, evaluation.getComments());
            
            // 获取详细评分项
            List<EvaluationDetail> details = evaluationService.getEvaluationDetails(evaluation.getId());
            List<Map<String, Object>> detailScores = new ArrayList<>();
            
            for (EvaluationDetail detail : details) {
                Map<String, Object> detailScore = new HashMap<>();
                detailScore.put("id", detail.getId());
                // 添加空值检查，如果criterion为null，则使用criterionName作为标识
                if (detail.getCriterion() != null) {
                    detailScore.put("criterionId", detail.getCriterion().getId());
                } else {
                    // 使用criterionName作为标识，或者提供一个默认值
                    detailScore.put("criterionId", null);
                }
                detailScore.put("criterionName", detail.getCriterionName());
                detailScore.put(ApiConstants.KEY_SCORE, detail.getScore());
                detailScore.put(ApiConstants.KEY_COMMENTS, detail.getComments());
                detailScores.add(detailScore);
            }
            
            data.put("detailScores", detailScores);
            data.put("createdBy", evaluation.getCreatedByUser().getId());
            data.put("createdAt", evaluation.getCreationTime());
            data.put(ApiConstants.KEY_STATUS, evaluation.getStatus());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理一步式人工评测请求时发生异常", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ApiConstants.KEY_SUCCESS, false);
            errorResponse.put(ApiConstants.KEY_MESSAGE, "评测提交失败");
            errorResponse.put(ApiConstants.KEY_ERROR, Map.of(
                "code", "EVALUATION_ERROR",
                "details", e.getMessage()
            ));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 修改人工评测结果
     */
    @PutMapping("/human/{evaluationId}")
    public ResponseEntity<Map<String, Object>> updateHumanEvaluation(
            @PathVariable Long evaluationId,
            @RequestBody UpdateHumanEvaluationRequest request) {
        
        logger.info("接收到修改人工评测请求，评测ID: {}", evaluationId);
        
        try {
            // 使用已有的submitHumanEvaluation方法，而不是不存在的updateHumanEvaluation方法
            Evaluation evaluation = evaluationService.submitHumanEvaluation(
                    evaluationId,
                    request.getOverallScore(), 
                    request.getComments(), 
                    request.getDetailScores(), 
                    request.getUserId());
            
            // 构建API响应
            Map<String, Object> response = new HashMap<>();
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "评测修改成功");
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("evaluationId", evaluation.getId());
            data.put("llmAnswerId", evaluation.getLlmAnswer().getId());
            data.put("evaluatorId", evaluation.getEvaluator().getId());
            data.put("overallScore", evaluation.getScore());
            data.put(ApiConstants.KEY_COMMENTS, evaluation.getComments());
            
            // 获取详细评分项
            List<EvaluationDetail> details = evaluationService.getEvaluationDetails(evaluation.getId());
            List<Map<String, Object>> detailScores = new ArrayList<>();
            
            for (EvaluationDetail detail : details) {
                Map<String, Object> detailScore = new HashMap<>();
                detailScore.put("id", detail.getId());
                // 添加空值检查，如果criterion为null，则使用criterionName作为标识
                if (detail.getCriterion() != null) {
                    detailScore.put("criterionId", detail.getCriterion().getId());
                } else {
                    // 使用criterionName作为标识，或者提供一个默认值
                    detailScore.put("criterionId", null);
                }
                detailScore.put("criterionName", detail.getCriterionName());
                detailScore.put(ApiConstants.KEY_SCORE, detail.getScore());
                detailScore.put(ApiConstants.KEY_COMMENTS, detail.getComments());
                detailScores.add(detailScore);
            }
            
            data.put("detailScores", detailScores);
            data.put("createdBy", evaluation.getCreatedByUser().getId());
            data.put("createdAt", evaluation.getCreationTime());
            data.put(ApiConstants.KEY_STATUS, evaluation.getStatus());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理修改人工评测请求时发生异常", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ApiConstants.KEY_SUCCESS, false);
            errorResponse.put(ApiConstants.KEY_MESSAGE, "评测修改失败");
            errorResponse.put(ApiConstants.KEY_ERROR, Map.of(
                "code", "EVALUATION_ERROR",
                "details", e.getMessage()
            ));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 修改人工评测请求类
     */
    @Data
    public static class UpdateHumanEvaluationRequest {
        private BigDecimal overallScore;    // 总体评分
        private String comments;            // 评语
        private List<Map<String, Object>> detailScores;  // 详细评分项
        private Long userId;                // 用户ID
        
        // Getters and Setters
        public BigDecimal getOverallScore() {
            return overallScore;
        }
        
        public void setOverallScore(BigDecimal overallScore) {
            this.overallScore = overallScore;
        }
        
        public String getComments() {
            return comments;
        }
        
        public void setComments(String comments) {
            this.comments = comments;
        }
        
        public List<Map<String, Object>> getDetailScores() {
            return detailScores;
        }
        
        public void setDetailScores(List<Map<String, Object>> detailScores) {
            this.detailScores = detailScores;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
    
    /**
     * 批量评测请求DTO
     */
    @Data
    public static class BatchAnswerEvaluationRequest {
        private List<Long> answerIds;
        private Long evaluatorId;
        private Long userId;
        
        public List<Long> getAnswerIds() {
            return answerIds;
        }
        
        public void setAnswerIds(List<Long> answerIds) {
            this.answerIds = answerIds;
        }
        
        public Long getEvaluatorId() {
            return evaluatorId;
        }
        
        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
    
    /**
     * 手动评测请求类
     */
    public static class ManualEvaluationRequest {
        private String answerText;
        private String correctIds;
        private String options;
        private String standardAnswer;
        private String alternativeAnswers;
        
        // Getters and Setters
        public String getAnswerText() {
            return answerText;
        }
        
        public void setAnswerText(String answerText) {
            this.answerText = answerText;
        }
        
        public String getCorrectIds() {
            return correctIds;
        }
        
        public void setCorrectIds(String correctIds) {
            this.correctIds = correctIds;
        }
        
        public String getOptions() {
            return options;
        }
        
        public void setOptions(String options) {
            this.options = options;
        }
        
        public String getStandardAnswer() {
            return standardAnswer;
        }
        
        public void setStandardAnswer(String standardAnswer) {
            this.standardAnswer = standardAnswer;
        }
        
        public String getAlternativeAnswers() {
            return alternativeAnswers;
        }
        
        public void setAlternativeAnswers(String alternativeAnswers) {
            this.alternativeAnswers = alternativeAnswers;
        }
    }
    
    /**
     * AI主观题评测请求类
     */
    public static class SubjectiveAIEvaluationRequest {
        private String answerText;
        private String questionText;
        private String referenceAnswer;
        private List<EvaluationCriterion> criteria;
        private Long evaluatorId;
        
        // Getters and Setters
        public String getAnswerText() {
            return answerText;
        }
        
        public void setAnswerText(String answerText) {
            this.answerText = answerText;
        }
        
        public String getQuestionText() {
            return questionText;
        }
        
        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }
        
        public String getReferenceAnswer() {
            return referenceAnswer;
        }
        
        public void setReferenceAnswer(String referenceAnswer) {
            this.referenceAnswer = referenceAnswer;
        }
        
        public List<EvaluationCriterion> getCriteria() {
            return criteria;
        }
        
        public void setCriteria(List<EvaluationCriterion> criteria) {
            this.criteria = criteria;
        }
        
        public Long getEvaluatorId() {
            return evaluatorId;
        }
        
        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
    }
    
    /**
     * 创建人工评测请求类
     */
    public static class CreateHumanEvaluationRequest {
        private Long llmAnswerId;
        private Long evaluatorId;
        private Long userId;
        
        // Getters and Setters
        public Long getLlmAnswerId() {
            return llmAnswerId;
        }
        
        public void setLlmAnswerId(Long llmAnswerId) {
            this.llmAnswerId = llmAnswerId;
        }
        
        public Long getEvaluatorId() {
            return evaluatorId;
        }
        
        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
    
    /**
     * 提交人工评测结果请求类
     */
    public static class SubmitHumanEvaluationRequest {
        private Long evaluationId;
        private BigDecimal overallScore;
        private String comments;
        private List<Map<String, Object>> detailScores;
        private Long userId;
        
        // Getters and Setters
        public Long getEvaluationId() {
            return evaluationId;
        }
        
        public void setEvaluationId(Long evaluationId) {
            this.evaluationId = evaluationId;
        }
        
        public BigDecimal getOverallScore() {
            return overallScore;
        }
        
        public void setOverallScore(BigDecimal overallScore) {
            this.overallScore = overallScore;
        }
        
        public String getComments() {
            return comments;
        }
        
        public void setComments(String comments) {
            this.comments = comments;
        }
        
        public List<Map<String, Object>> getDetailScores() {
            return detailScores;
        }
        
        public void setDetailScores(List<Map<String, Object>> detailScores) {
            this.detailScores = detailScores;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
    
    /**
     * 一步式人工评测请求类
     */
    @Data
    public static class CreateAndSubmitHumanEvaluationRequest {
        private Long llmAnswerId;           // 大模型回答ID
        private Long evaluatorId;           // 评测者ID
        private BigDecimal overallScore;    // 总体评分
        private String comments;            // 评语
        private List<Map<String, Object>> detailScores;  // 详细评分项
        private Long userId;                // 用户ID
        
        // Getters and Setters
        public Long getLlmAnswerId() {
            return llmAnswerId;
        }
        
        public void setLlmAnswerId(Long llmAnswerId) {
            this.llmAnswerId = llmAnswerId;
        }
        
        public Long getEvaluatorId() {
            return evaluatorId;
        }
        
        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
        
        public BigDecimal getOverallScore() {
            return overallScore;
        }
        
        public void setOverallScore(BigDecimal overallScore) {
            this.overallScore = overallScore;
        }
        
        public String getComments() {
            return comments;
        }
        
        public void setComments(String comments) {
            this.comments = comments;
        }
        
        public List<Map<String, Object>> getDetailScores() {
            return detailScores;
        }
        
        public void setDetailScores(List<Map<String, Object>> detailScores) {
            this.detailScores = detailScores;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
    
    /**
     * 批量评测请求DTO
     */
    @Data
    public static class BatchEvaluationRequest {
        private Long batchId;
        private Long evaluatorId;
        private Long userId;
        private Long subjectivePromptId;
        private Long evaluationAssemblyConfigId;
        private List<Long> criteriaIds;
        
        public Long getBatchId() {
            return batchId;
        }
        
        public void setBatchId(Long batchId) {
            this.batchId = batchId;
        }
        
        public Long getEvaluatorId() {
            return evaluatorId;
        }
        
        public void setEvaluatorId(Long evaluatorId) {
            this.evaluatorId = evaluatorId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public Long getSubjectivePromptId() {
            return subjectivePromptId;
        }
        
        public void setSubjectivePromptId(Long subjectivePromptId) {
            this.subjectivePromptId = subjectivePromptId;
        }
        
        public Long getEvaluationAssemblyConfigId() {
            return evaluationAssemblyConfigId;
        }
        
        public void setEvaluationAssemblyConfigId(Long evaluationAssemblyConfigId) {
            this.evaluationAssemblyConfigId = evaluationAssemblyConfigId;
        }
        
        public List<Long> getCriteriaIds() {
            return criteriaIds;
        }
        
        public void setCriteriaIds(List<Long> criteriaIds) {
            this.criteriaIds = criteriaIds;
        }
    }
    
    /**
     * 获取指定批次中某评测员尚未评测的回答列表
     * 同时返回对应的标准回答和标准问题
     */
    @GetMapping("/batch/{batchId}/unevaluated")
    public ResponseEntity<Map<String, Object>> getUnevaluatedAnswers(
            @PathVariable Long batchId,
            @RequestParam(required = false) Long evaluatorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        logger.info("接收到获取未评测回答请求，批次ID: {}, 评测者ID: {}, 页码: {}, 每页大小: {}", 
                batchId, evaluatorId, page, size);
        
        // 查询该批次的所有回答
        List<LlmAnswer> allAnswers = llmAnswerRepository.findByBatchIdWithQuestions(batchId);
        if (allAnswers.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("unevaluatedAnswers", new ArrayList<>());
            emptyResult.put("totalCount", 0);
            emptyResult.put("page", page);
            emptyResult.put("size", size);
            emptyResult.put("totalPages", 0);
            return ResponseEntity.ok(emptyResult);
        }
        
        // 筛选出未被该评测员评测的回答
        List<Map<String, Object>> allUnevaluatedAnswers = new ArrayList<>();
        for (LlmAnswer answer : allAnswers) {
            // 如果没有指定评测者ID，则返回所有回答；否则检查该回答是否已被该评测员评测
            boolean shouldInclude = true;
            if (evaluatorId != null) {
                boolean evaluated = evaluationService.isAnswerEvaluatedByEvaluator(answer.getId(), evaluatorId);
                shouldInclude = !evaluated;
            }
            
            if (shouldInclude) {
                // 获取标准问题和标准答案
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("answer", answer);
                
                // 如果有关联的标准问题
                if (answer.getDatasetQuestionMapping() != null && 
                    answer.getDatasetQuestionMapping().getStandardQuestion() != null) {
                    
                    StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                    answerData.put("standardQuestion", question);
                    
                    // 根据问题类型获取标准答案
                    Map<String, Object> standardAnswer = evaluationService.getStandardAnswerForQuestion(question.getId());
                    if (standardAnswer != null && !standardAnswer.isEmpty()) {
                        answerData.put("standardAnswer", standardAnswer);
                    }
                }
                
                allUnevaluatedAnswers.add(answerData);
            }
        }
        
        // 计算分页信息
        int totalCount = allUnevaluatedAnswers.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        
        // 分页处理
        List<Map<String, Object>> pagedAnswers;
        if (totalCount > 0) {
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalCount);
            
            // 确保起始索引不超过列表大小
            if (startIndex >= totalCount) {
                startIndex = Math.max(0, totalCount - size);
                endIndex = totalCount;
                page = Math.max(0, totalPages - 1); // 调整页码为最后一页
            }
            
            pagedAnswers = allUnevaluatedAnswers.subList(startIndex, endIndex);
        } else {
            pagedAnswers = new ArrayList<>();
        }
        
        // 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("unevaluatedAnswers", pagedAnswers);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        
        logger.info("找到{}个未评测的回答，当前页{}显示{}条", totalCount, page, pagedAnswers.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取批量主观题评测的进度
     */
    @GetMapping("/batch/subjective/{batchId}/progress")
    public ResponseEntity<Map<String, Object>> getSubjectiveEvaluationProgress(
            @PathVariable Long batchId,
            @RequestParam Long evaluatorId) {
        
        logger.info("接收到查询批量主观题评测进度请求，批次ID: {}, 评测者ID: {}", batchId, evaluatorId);
        
        try {
            // 查找评测运行记录
            List<EvaluationRun> runs = evaluationService.getEvaluationRuns(batchId, evaluatorId, null, 0, 1);
            
            if (runs.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "未找到对应的评测运行记录");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            EvaluationRun run = runs.get(0);
            
            // 获取评测进度
            Map<String, Object> progress = evaluationService.getEvaluationRunProgress(run.getId());
            
            // 添加额外信息
            progress.put("success", true);
            progress.put("batchId", batchId);
            progress.put("evaluatorId", evaluatorId);
            
            return ResponseEntity.ok(progress);
            
        } catch (Exception e) {
            logger.error("查询批量主观题评测进度失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "查询评测进度失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 取消批量主观题评测任务
     */
    @PostMapping("/batch/subjective/{batchId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSubjectiveEvaluation(
            @PathVariable Long batchId,
            @RequestParam Long evaluatorId) {
        
        logger.info("接收到取消批量主观题评测请求，批次ID: {}, 评测者ID: {}", batchId, evaluatorId);
        
        try {
            // 查找评测运行记录
            List<EvaluationRun> runs = evaluationService.getEvaluationRuns(batchId, evaluatorId, null, 0, 1);
            
            if (runs.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "未找到对应的评测运行记录");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            EvaluationRun run = runs.get(0);
            
            // 暂停评测任务
            boolean paused = evaluationService.pauseEvaluationRun(run.getId());
            
            Map<String, Object> result = new HashMap<>();
            if (paused) {
                result.put("success", true);
                result.put("message", "评测任务已取消");
            } else {
                result.put("success", false);
                result.put("message", "无法取消评测任务，可能任务已完成或已取消");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("取消批量主观题评测失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "取消评测任务失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取批量主观题评测的结果
     */
    @GetMapping("/batch/subjective/{batchId}/results")
    public ResponseEntity<Map<String, Object>> getSubjectiveEvaluationResults(
            @PathVariable Long batchId,
            @RequestParam Long evaluatorId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取批量主观题评测结果请求，批次ID: {}, 评测者ID: {}", batchId, evaluatorId);
        
        try {
            // 获取评测结果
            Map<String, Object> results = evaluationService.getSubjectiveDetailedResults(
                    batchId, modelIds, evaluatorId, page, size);
            
            // 添加额外信息
            results.put("success", true);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("获取批量主观题评测结果失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取评测结果失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 获取批次综合评分展示数据
     * 包含客观题、主观题的详细评分统计和模型排名
     */
    @GetMapping("/batch/{batchId}/comprehensive-scores")
    public ResponseEntity<Map<String, Object>> getBatchComprehensiveScores(
            @PathVariable Long batchId,
            @RequestParam(required = false) List<Long> modelIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("接收到获取批次{}综合评分展示数据请求", batchId);
        
        try {
            Map<String, Object> result = evaluationService.getBatchComprehensiveScores(batchId, modelIds, page, size);
            result.put("success", true);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取批次{}综合评分展示数据失败", batchId, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取批次评分数据失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("处理评测请求时发生异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ApiConstants.KEY_ERROR, e.getMessage()));
    }
} 