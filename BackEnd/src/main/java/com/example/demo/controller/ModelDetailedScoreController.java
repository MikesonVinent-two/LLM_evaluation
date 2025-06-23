package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型详细评分控制器
 * 提供模型在批次中更细粒度的评分数据查询API
 */
@RestController
@RequestMapping("/llm-models")
@CrossOrigin(origins = "*")
public class ModelDetailedScoreController {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelDetailedScoreController.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取批次中所有模型的排名情况
     * 
     * @param batchId 批次ID
     * @param scoreType 评分类型，默认为OVERALL
     * @return 模型排名列表
     */
    @GetMapping("/batch/{batchId}/rankings")
    public ResponseEntity<?> getModelRankingsInBatch(
            @PathVariable Long batchId,
            @RequestParam(required = false, defaultValue = "OVERALL") String scoreType) {
        logger.info("获取批次ID{}中模型的{}评分排名", batchId, scoreType);
        
        try {
            String rankingsQuery = 
                "SELECT mbs.model_id, lm.name AS model_name, lm.provider, lm.version, " +
                "mbs.average_score, mbs.total_answers, mbs.scored_answers, " +
                "mbs.max_score, mbs.min_score, mbs.calculated_at " +
                "FROM model_batch_scores mbs " +
                "JOIN llm_models lm ON mbs.model_id = lm.id " +
                "WHERE mbs.batch_id = ? AND mbs.score_type = ? " +
                "AND mbs.evaluator_id IS NULL AND mbs.repeat_index = -1 " +
                "ORDER BY mbs.average_score DESC";
            
            List<Map<String, Object>> rankings = jdbcTemplate.queryForList(rankingsQuery, batchId, scoreType);
            
            // 添加排名信息
            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).put("rank", i + 1);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("scoreType", scoreType);
            response.put("rankings", rankings);
            response.put("totalModels", rankings.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型排名时发生错误", batchId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模型排名失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取模型在多个批次中的表现趋势
     * 
     * @param modelId 模型ID
     * @param scoreType 评分类型，默认为OVERALL
     * @return 模型在多个批次中的表现趋势
     */
    @GetMapping("/{modelId}/performance-trend")
    public ResponseEntity<?> getModelPerformanceTrend(
            @PathVariable Long modelId,
            @RequestParam(required = false, defaultValue = "OVERALL") String scoreType) {
        logger.info("获取模型ID{}的{}评分表现趋势", modelId, scoreType);
        
        try {
            String trendQuery = 
                "SELECT mbs.batch_id, agb.name AS batch_name, agb.created_at AS batch_date, " +
                "mbs.average_score, mbs.total_answers, mbs.scored_answers " +
                "FROM model_batch_scores mbs " +
                "JOIN answer_generation_batches agb ON mbs.batch_id = agb.id " +
                "WHERE mbs.model_id = ? AND mbs.score_type = ? " +
                "AND mbs.evaluator_id IS NULL AND mbs.repeat_index = -1 " +
                "ORDER BY agb.created_at ASC";
            
            List<Map<String, Object>> trend = jdbcTemplate.queryForList(trendQuery, modelId, scoreType);
            
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("scoreType", scoreType);
            response.put("trend", trend);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取模型ID{}的表现趋势时发生错误", modelId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模型表现趋势失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取模型在批次中按问题类型的表现
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 模型在不同问题类型上的表现
     */
    @GetMapping("/batch/{batchId}/model/{modelId}/performance-by-question-type")
    public ResponseEntity<?> getModelPerformanceByQuestionType(
            @PathVariable Long batchId,
            @PathVariable Long modelId) {
        logger.info("获取批次ID{}中模型ID{}按问题类型的表现", batchId, modelId);
        
        try {
            // 获取模型在该批次中的所有回答评分，按问题类型分组
            String queryByType = 
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
            
            List<Map<String, Object>> performanceByType = jdbcTemplate.queryForList(queryByType, modelId, batchId);
            
            // 获取模型和批次信息
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            String batchQuery = "SELECT name FROM answer_generation_batches WHERE id = ?";
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("batchName", batchInfo.get("name"));
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("performanceByQuestionType", performanceByType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型ID{}按问题类型的表现时发生错误", batchId, modelId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模型按问题类型的表现失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取模型在批次中按评分维度的表现
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 模型在不同评分维度上的表现
     */
    @GetMapping("/batch/{batchId}/model/{modelId}/performance-by-score-type")
    public ResponseEntity<?> getModelPerformanceByScoreType(
            @PathVariable Long batchId,
            @PathVariable Long modelId) {
        logger.info("获取批次ID{}中模型ID{}按评分维度的表现", batchId, modelId);
        
        try {
            String queryByScoreType = 
                "SELECT score_type, average_score, total_answers, scored_answers " +
                "FROM model_batch_scores " +
                "WHERE batch_id = ? AND model_id = ? " +
                "AND evaluator_id IS NULL AND repeat_index = -1 " +
                "ORDER BY score_type";
            
            List<Map<String, Object>> performanceByScoreType = jdbcTemplate.queryForList(
                    queryByScoreType, batchId, modelId);
            
            // 获取评分维度的描述
            String dimensionsQuery = 
                "SELECT criteria_key, display_name, description " +
                "FROM evaluation_criteria";
            
            List<Map<String, Object>> dimensions = jdbcTemplate.queryForList(dimensionsQuery);
            
            // 整合评分维度的描述
            Map<String, Map<String, Object>> dimensionMap = new HashMap<>();
            for (Map<String, Object> dimension : dimensions) {
                dimensionMap.put((String) dimension.get("criteria_key"), dimension);
            }
            
            for (Map<String, Object> performance : performanceByScoreType) {
                String scoreType = (String) performance.get("score_type");
                if (dimensionMap.containsKey(scoreType)) {
                    performance.put("display_name", dimensionMap.get(scoreType).get("display_name"));
                    performance.put("description", dimensionMap.get(scoreType).get("description"));
                } else {
                    performance.put("display_name", scoreType);
                    performance.put("description", "");
                }
            }
            
            // 获取模型和批次信息
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            String batchQuery = "SELECT name FROM answer_generation_batches WHERE id = ?";
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("batchName", batchInfo.get("name"));
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("performanceByScoreType", performanceByScoreType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型ID{}按评分维度的表现时发生错误", batchId, modelId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模型按评分维度的表现失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取模型在批次中特定问题的详细得分
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @param questionId 问题ID
     * @return 模型在该问题上的详细得分
     */
    @GetMapping("/batch/{batchId}/model/{modelId}/questions/{questionId}/detailed-scores")
    public ResponseEntity<?> getModelQuestionDetailedScores(
            @PathVariable Long batchId,
            @PathVariable Long modelId,
            @PathVariable Long questionId) {
        logger.info("获取批次ID{}中模型ID{}关于问题ID{}的详细得分", batchId, modelId, questionId);
        
        try {
            // 获取问题信息
            String questionQuery = 
                "SELECT sq.id, sq.question_content, sq.question_type " +
                "FROM standard_questions sq " +
                "WHERE sq.id = ?";
            
            Map<String, Object> questionInfo = jdbcTemplate.queryForMap(questionQuery, questionId);
            
            // 获取模型在该批次该问题的所有回答
            String answersQuery = 
                "SELECT la.id, la.answer_content, la.repeat_index " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ? " +
                "AND dqm.standard_question_id = ? " +
                "ORDER BY la.repeat_index";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(
                    answersQuery, modelId, batchId, questionId);
            
            if (answers.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "未找到模型在该批次该问题的回答");
                return ResponseEntity.ok(response);
            }
            
            // 获取每个回答的评分
            List<Map<String, Object>> detailedScores = new ArrayList<>();
            
            for (Map<String, Object> answer : answers) {
                Long answerId = (Long) answer.get("id");
                Integer repeatIndex = ((Number) answer.get("repeat_index")).intValue();
                
                String scoresQuery = 
                    "SELECT e.id, e.evaluator_id, ev.name AS evaluator_name, " +
                    "e.score_type, e.raw_score, e.normalized_score, " +
                    "e.comment, e.created_at " +
                    "FROM evaluations e " +
                    "LEFT JOIN evaluators ev ON e.evaluator_id = ev.id " +
                    "WHERE e.llm_answer_id = ? AND e.evaluation_status = 'SUCCESS' " +
                    "ORDER BY e.score_type, e.evaluator_id";
                
                List<Map<String, Object>> scores = jdbcTemplate.queryForList(scoresQuery, answerId);
                
                // 按评分维度分组
                Map<String, List<Map<String, Object>>> scoresByType = new HashMap<>();
                for (Map<String, Object> score : scores) {
                    String scoreType = (String) score.get("score_type");
                    if (!scoresByType.containsKey(scoreType)) {
                        scoresByType.put(scoreType, new ArrayList<>());
                    }
                    scoresByType.get(scoreType).add(score);
                }
                
                // 计算每个评分维度的平均分
                Map<String, Double> avgScoresByType = new HashMap<>();
                for (Map.Entry<String, List<Map<String, Object>>> entry : scoresByType.entrySet()) {
                    String scoreType = entry.getKey();
                    List<Map<String, Object>> typeScores = entry.getValue();
                    
                    double sum = 0;
                    for (Map<String, Object> score : typeScores) {
                        sum += ((Number) score.get("normalized_score")).doubleValue();
                    }
                    
                    avgScoresByType.put(scoreType, sum / typeScores.size());
                }
                
                Map<String, Object> detailedScore = new HashMap<>();
                detailedScore.put("answerId", answerId);
                detailedScore.put("repeatIndex", repeatIndex);
                detailedScore.put("answerContent", answer.get("answer_content"));
                detailedScore.put("scores", scores);
                detailedScore.put("scoresByType", scoresByType);
                detailedScore.put("averageScoresByType", avgScoresByType);
                
                detailedScores.add(detailedScore);
            }
            
            // 计算每个重复回答的总平均分
            for (Map<String, Object> detailedScore : detailedScores) {
                Map<String, Double> avgScoresByType = (Map<String, Double>) detailedScore.get("averageScoresByType");
                
                double sum = 0;
                for (Double avg : avgScoresByType.values()) {
                    sum += avg;
                }
                
                double overallAvg = avgScoresByType.size() > 0 ? sum / avgScoresByType.size() : 0;
                detailedScore.put("overallAverageScore", overallAvg);
            }
            
            // 获取模型和批次信息
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            String batchQuery = "SELECT name FROM answer_generation_batches WHERE id = ?";
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("batchName", batchInfo.get("name"));
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("questionId", questionId);
            response.put("questionContent", questionInfo.get("question_content"));
            response.put("questionType", questionInfo.get("question_type"));
            response.put("detailedScores", detailedScores);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型ID{}关于问题ID{}的详细得分时发生错误", batchId, modelId, questionId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取模型问题详细得分失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 