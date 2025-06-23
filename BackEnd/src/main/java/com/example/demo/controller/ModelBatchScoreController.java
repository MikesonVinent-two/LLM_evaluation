package com.example.demo.controller;

import com.example.demo.service.ModelBatchScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型批次评分控制器
 * 提供模型在批次中的评分统计相关API
 */
@RestController
@RequestMapping("/model-batch-scores")
@CrossOrigin(origins = "*")
public class ModelBatchScoreController {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelBatchScoreController.class);
    
    @Autowired
    private ModelBatchScoreService modelBatchScoreService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 计算指定批次中所有模型的评分统计
     * 
     * @param batchId 批次ID
     * @return 计算结果
     */
    @PostMapping("/batches/{batchId}/calculate")
    public ResponseEntity<?> calculateBatchScores(@PathVariable Long batchId) {
        logger.info("接收到计算批次ID{}的所有模型评分统计请求", batchId);
        
        try {
            boolean success = modelBatchScoreService.calculateBatchScores(batchId);
            
            if (success) {
                // 获取批次信息
                String batchQuery = "SELECT name, description FROM answer_generation_batches WHERE id = ?";
                Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
                
                // 获取批次中所有模型的评分摘要
                String modelsQuery = 
                    "SELECT mbs.model_id, lm.name AS model_name, lm.provider, " +
                    "mbs.score_type, mbs.average_score " +
                    "FROM model_batch_scores mbs " +
                    "JOIN llm_models lm ON mbs.model_id = lm.id " +
                    "WHERE mbs.batch_id = ? " +
                    "AND mbs.evaluator_id IS NULL AND mbs.repeat_index = -1 " +
                    "ORDER BY mbs.model_id, mbs.score_type";
                
                List<Map<String, Object>> allScores = jdbcTemplate.queryForList(modelsQuery, batchId);
                
                // 按模型ID分组处理评分数据
                Map<Long, Map<String, Object>> modelScoreSummary = new HashMap<>();
                
                for (Map<String, Object> score : allScores) {
                    Long modelId = (Long) score.get("model_id");
                    String modelName = (String) score.get("model_name");
                    String provider = (String) score.get("provider");
                    String scoreType = (String) score.get("score_type");
                    Double averageScore = ((Number) score.get("average_score")).doubleValue();
                    
                    if (!modelScoreSummary.containsKey(modelId)) {
                        Map<String, Object> modelInfo = new HashMap<>();
                        modelInfo.put("modelId", modelId);
                        modelInfo.put("modelName", modelName);
                        modelInfo.put("provider", provider);
                        modelInfo.put("scores", new HashMap<String, Double>());
                        
                        modelScoreSummary.put(modelId, modelInfo);
                    }
                    
                    Map<String, Object> modelInfo = modelScoreSummary.get(modelId);
                    Map<String, Double> scores = (Map<String, Double>) modelInfo.get("scores");
                    scores.put(scoreType, averageScore);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", success);
                response.put("batchId", batchId);
                response.put("batchName", batchInfo.get("name"));
                response.put("batchDescription", batchInfo.get("description"));
                response.put("message", "成功计算批次的模型评分统计");
                response.put("modelScores", modelScoreSummary.values());
                response.put("totalModels", modelScoreSummary.size());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", success);
                response.put("batchId", batchId);
                response.put("message", "计算批次的模型评分统计失败");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("计算批次ID{}的模型评分统计时发生错误", batchId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("message", "计算批次的模型评分统计时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 计算指定批次中特定模型的评分统计
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 计算结果
     */
    @PostMapping("/batches/{batchId}/models/{modelId}/calculate")
    public ResponseEntity<?> calculateModelScoresInBatch(
            @PathVariable Long batchId,
            @PathVariable Long modelId) {
        logger.info("接收到计算批次ID{}中模型ID{}的评分统计请求", batchId, modelId);
        
        try {
            boolean success = modelBatchScoreService.calculateModelScoresInBatch(batchId, modelId);
            
            if (success) {
                // 计算成功后，立即获取计算结果
                String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
                Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
                
                // 获取所有评分维度的统计数据
                String scoresQuery = 
                    "SELECT score_type, average_score, total_answers, scored_answers, max_score, min_score, calculated_at " +
                    "FROM model_batch_scores " +
                    "WHERE batch_id = ? AND model_id = ? " +
                    "AND evaluator_id IS NULL AND repeat_index = -1 " +
                    "ORDER BY score_type";
                
                List<Map<String, Object>> scoresList = jdbcTemplate.queryForList(scoresQuery, batchId, modelId);
                
                // 按问题类型获取统计数据
                String questionTypeQuery = 
                    "SELECT sq.question_type, AVG(e.normalized_score) AS average_score, " +
                    "COUNT(DISTINCT e.llm_answer_id) AS total_answers " +
                    "FROM evaluations e " +
                    "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                    "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                    "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                    "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                    "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ? " +
                    "AND e.evaluation_status = 'SUCCESS' " +
                    "GROUP BY sq.question_type";
                
                List<Map<String, Object>> questionTypeStats = jdbcTemplate.queryForList(questionTypeQuery, modelId, batchId);
                
                // 处理评分数据
                Map<String, Object> scores = new HashMap<>();
                String calculatedAt = null;
                
                for (Map<String, Object> score : scoresList) {
                    String scoreType = (String) score.get("score_type");
                    
                    Map<String, Object> scoreData = new HashMap<>();
                    scoreData.put("average_score", score.get("average_score"));
                    scoreData.put("total_answers", score.get("total_answers"));
                    scoreData.put("scored_answers", score.get("scored_answers"));
                    scoreData.put("max_score", score.get("max_score"));
                    scoreData.put("min_score", score.get("min_score"));
                    
                    scores.put(scoreType, scoreData);
                    
                    if (calculatedAt == null && score.get("calculated_at") != null) {
                        calculatedAt = score.get("calculated_at").toString();
                    }
                }
                
                // 处理问题类型数据
                Map<String, Object> byQuestionType = new HashMap<>();
                for (Map<String, Object> qtStat : questionTypeStats) {
                    String questionType = (String) qtStat.get("question_type");
                    
                    Map<String, Object> qtData = new HashMap<>();
                    qtData.put("average_score", qtStat.get("average_score"));
                    qtData.put("total_answers", qtStat.get("total_answers"));
                    
                    byQuestionType.put(questionType, qtData);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", success);
                response.put("batchId", batchId);
                response.put("modelId", modelId);
                response.put("message", "成功计算模型在批次中的评分统计");
                response.put("modelName", modelInfo.get("name"));
                response.put("provider", modelInfo.get("provider"));
                response.put("version", modelInfo.get("version"));
                response.put("scores", scores);
                response.put("byQuestionType", byQuestionType);
                response.put("calculatedAt", calculatedAt);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", success);
                response.put("batchId", batchId);
                response.put("modelId", modelId);
                response.put("message", "计算模型在批次中的评分统计失败");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的评分统计时发生错误", batchId, modelId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("modelId", modelId);
            response.put("message", "计算模型在批次中的评分统计时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清除指定批次的所有评分统计数据
     * 
     * @param batchId 批次ID
     * @return 清除结果
     */
    @DeleteMapping("/batches/{batchId}")
    public ResponseEntity<?> clearBatchScores(@PathVariable Long batchId) {
        logger.info("接收到清除批次ID{}的所有评分统计数据请求", batchId);
        
        try {
            boolean success = modelBatchScoreService.clearBatchScores(batchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("batchId", batchId);
            response.put("message", success ? 
                    "成功清除批次的评分统计数据" : 
                    "清除批次的评分统计数据失败");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除批次ID{}的评分统计数据时发生错误", batchId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("message", "清除批次的评分统计数据时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定批次中所有模型的评分排名
     * 
     * @param batchId 批次ID
     * @param scoreType 评分类型（可选）
     * @return 模型评分排名列表
     */
    @GetMapping("/batches/{batchId}/rankings")
    public ResponseEntity<?> getModelRankingsInBatch(
            @PathVariable Long batchId,
            @RequestParam(required = false) String scoreType) {
        logger.info("接收到获取批次ID{}中所有模型的评分排名请求，评分类型：{}", batchId, scoreType);
        
        try {
            List<Map<String, Object>> rankings = modelBatchScoreService.getModelRankingsInBatch(batchId, scoreType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("scoreType", scoreType != null ? scoreType : "OVERALL");
            response.put("rankings", rankings);
            response.put("totalModels", rankings.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型评分排名时发生错误", batchId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("message", "获取批次中模型评分排名时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定批次和模型的详细评分统计
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 评分统计详情
     */
    @GetMapping("/batches/{batchId}/models/{modelId}")
    public ResponseEntity<?> getModelScoreDetails(
            @PathVariable Long batchId,
            @PathVariable Long modelId) {
        logger.info("接收到获取批次ID{}中模型ID{}的详细评分统计请求", batchId, modelId);
        
        try {
            Map<String, Object> details = modelBatchScoreService.getModelScoreDetails(batchId, modelId);
            
            if (details.containsKey("error")) {
                return ResponseEntity.internalServerError().body(details);
            }
            
            Map<String, Object> response = new HashMap<>(details);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型ID{}的评分详情时发生错误", batchId, modelId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("modelId", modelId);
            response.put("message", "获取模型在批次中的评分详情时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 