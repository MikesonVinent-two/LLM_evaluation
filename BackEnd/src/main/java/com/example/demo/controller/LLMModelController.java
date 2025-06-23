package com.example.demo.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LLMModelDTO;
import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;
import com.example.demo.service.LLMModelService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/llm-models")
@CrossOrigin(origins = "*")
public class LLMModelController {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMModelController.class);
    
    @Autowired
    private LLMModelService llmModelService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostMapping("/register")
    public ResponseEntity<LLMModelRegistrationResponse> registerModels(
            @Valid @RequestBody LLMModelRegistrationRequest request) {
        LLMModelRegistrationResponse response = llmModelService.registerModels(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有已注册的LLM模型
     * 
     * @return 已注册的LLM模型列表
     */
    @GetMapping
    public ResponseEntity<?> getAllModels() {
        logger.info("接收到获取所有LLM模型的请求");
        
        try {
            List<LLMModelDTO> models = llmModelService.getAllModels();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("models", models);
            response.put("total", models.size());
            
            logger.info("成功获取所有LLM模型，共 {} 个", models.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取LLM模型失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取LLM模型时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定模型的总体评分统计信息
     * 
     * @param modelId 模型ID
     * @return 模型的总体评分统计信息
     */
    @GetMapping("/{modelId}/performance")
    public ResponseEntity<?> getModelPerformanceStats(@PathVariable Long modelId) {
        logger.info("接收到获取模型ID为{}的总体评分统计信息请求", modelId);
        
        try {
            // 1. 获取模型信息
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            // 2. 获取该模型参与的所有运行
            String runsQuery = "SELECT id FROM model_answer_runs WHERE llm_model_id = ?";
            List<Long> runIds = jdbcTemplate.queryForList(runsQuery, Long.class, modelId);
            
            if (runIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("modelId", modelId);
                response.put("modelName", modelInfo.get("name"));
                response.put("provider", modelInfo.get("provider"));
                response.put("version", modelInfo.get("version"));
                response.put("message", "该模型尚无评分数据");
                response.put("hasScores", false);
                
                return ResponseEntity.ok(response);
            }
            
            // 3. 获取这些运行生成的所有答案
            StringBuilder answerIdsQuery = new StringBuilder();
            answerIdsQuery.append("SELECT id FROM llm_answers WHERE model_answer_run_id IN (");
            for (int i = 0; i < runIds.size(); i++) {
                answerIdsQuery.append("?");
                if (i < runIds.size() - 1) {
                    answerIdsQuery.append(",");
                }
            }
            answerIdsQuery.append(")");
            
            List<Long> answerIds = jdbcTemplate.queryForList(answerIdsQuery.toString(), Long.class, runIds.toArray());
            
            if (answerIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("modelId", modelId);
                response.put("modelName", modelInfo.get("name"));
                response.put("provider", modelInfo.get("provider"));
                response.put("version", modelInfo.get("version"));
                response.put("message", "该模型尚无生成答案");
                response.put("hasScores", false);
                
                return ResponseEntity.ok(response);
            }
            
            // 4. 获取这些答案的评分统计
            StringBuilder scoreQuery = new StringBuilder();
            scoreQuery.append("SELECT score_type, AVG(normalized_score) as avg_score, ");
            scoreQuery.append("COUNT(*) as count, MAX(normalized_score) as max_score, ");
            scoreQuery.append("MIN(normalized_score) as min_score ");
            scoreQuery.append("FROM answer_scores WHERE llm_answer_id IN (");
            for (int i = 0; i < answerIds.size(); i++) {
                scoreQuery.append("?");
                if (i < answerIds.size() - 1) {
                    scoreQuery.append(",");
                }
            }
            scoreQuery.append(") GROUP BY score_type");
            
            List<Map<String, Object>> scoreStats = jdbcTemplate.queryForList(
                    scoreQuery.toString(), answerIds.toArray());
            
            // 5. 计算总体平均分
            BigDecimal overallAvgScore = BigDecimal.ZERO;
            int totalScoreCount = 0;
            
            if (!scoreStats.isEmpty()) {
                BigDecimal totalScore = BigDecimal.ZERO;
                for (Map<String, Object> stat : scoreStats) {
                    BigDecimal avgScore = new BigDecimal(stat.get("avg_score").toString());
                    int count = ((Number) stat.get("count")).intValue();
                    
                    totalScore = totalScore.add(avgScore.multiply(new BigDecimal(count)));
                    totalScoreCount += count;
                }
                
                if (totalScoreCount > 0) {
                    overallAvgScore = totalScore.divide(new BigDecimal(totalScoreCount), 2, RoundingMode.HALF_UP);
                }
            }
            
            // 6. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("totalRuns", runIds.size());
            response.put("totalAnswers", answerIds.size());
            response.put("totalScores", totalScoreCount);
            response.put("overallAverageScore", overallAvgScore);
            response.put("scoresByType", scoreStats);
            response.put("hasScores", !scoreStats.isEmpty());
            
            logger.info("成功获取模型ID为{}的总体评分统计信息", modelId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取模型评分统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("modelId", modelId);
            response.put("message", "获取模型评分统计信息时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定模型对特定问题的所有回答及其评分详情
     * 
     * @param modelId 模型ID
     * @param questionId 问题ID
     * @return 模型对问题的回答及评分详情
     */
    @GetMapping("/{modelId}/questions/{questionId}/detailed-scores")
    public ResponseEntity<?> getModelQuestionDetailedScores(
            @PathVariable Long modelId,
            @PathVariable Long questionId) {
        logger.info("接收到获取模型ID{}对问题ID{}的详细回答及评分信息请求", modelId, questionId);
        
        try {
            // 1. 获取模型和问题的基本信息
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            String questionQuery = "SELECT question_text, question_type, difficulty FROM standard_questions WHERE id = ?";
            Map<String, Object> questionInfo;
            try {
                questionInfo = jdbcTemplate.queryForMap(questionQuery, questionId);
            } catch (Exception e) {
                // 如果标准问题找不到，尝试在数据集映射中查找
                String mappingQuery = "SELECT sq.question_text, sq.question_type, sq.difficulty " +
                                      "FROM dataset_question_mapping dqm " +
                                      "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                                      "WHERE dqm.id = ?";
                questionInfo = jdbcTemplate.queryForMap(mappingQuery, questionId);
            }
            
            // 2. 获取模型对该问题的所有回答
            String answersQuery = 
                "SELECT la.id, la.answer_text, la.repeat_index, la.generation_time, mar.id as run_id, mar.run_name " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "WHERE mar.llm_model_id = ? AND la.dataset_question_mapping_id = ? " +
                "ORDER BY la.repeat_index";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(answersQuery, modelId, questionId);
            
            if (answers.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("modelId", modelId);
                response.put("modelName", modelInfo.get("name"));
                response.put("questionId", questionId);
                response.put("questionText", questionInfo.get("question_text"));
                response.put("message", "该模型尚未对此问题生成回答");
                response.put("hasAnswers", false);
                
                return ResponseEntity.ok(response);
            }
            
            // 3. 获取每个回答的评分详情
            List<Map<String, Object>> detailedAnswers = new ArrayList<>();
            BigDecimal totalScore = BigDecimal.ZERO;
            int scoreCount = 0;
            
            for (Map<String, Object> answer : answers) {
                Long answerId = (Long) answer.get("id");
                
                // 获取该回答的所有评分
                String scoresQuery = 
                    "SELECT as.id, as.normalized_score, as.score_type, as.scoring_method, as.comments, " +
                    "e.name as evaluator_name, e.evaluator_type, u.username as scored_by_username " +
                    "FROM answer_scores as " +
                    "JOIN evaluators e ON as.evaluator_id = e.id " +
                    "LEFT JOIN users u ON as.created_by_user_id = u.id " +
                    "WHERE as.llm_answer_id = ?";
                
                List<Map<String, Object>> scores = jdbcTemplate.queryForList(scoresQuery, answerId);
                
                // 计算该回答的平均分
                BigDecimal answerAvgScore = BigDecimal.ZERO;
                if (!scores.isEmpty()) {
                    BigDecimal answerTotalScore = BigDecimal.ZERO;
                    for (Map<String, Object> score : scores) {
                        if (score.get("normalized_score") != null) {
                            BigDecimal normalizedScore = new BigDecimal(score.get("normalized_score").toString());
                            answerTotalScore = answerTotalScore.add(normalizedScore);
                        }
                    }
                    answerAvgScore = scores.size() > 0 ? 
                        answerTotalScore.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP) : 
                        BigDecimal.ZERO;
                    
                    totalScore = totalScore.add(answerAvgScore);
                    scoreCount++;
                }
                
                // 构建答案详情
                Map<String, Object> detailedAnswer = new HashMap<>(answer);
                detailedAnswer.put("scores", scores);
                detailedAnswer.put("averageScore", answerAvgScore);
                detailedAnswer.put("hasScores", !scores.isEmpty());
                
                detailedAnswers.add(detailedAnswer);
            }
            
            // 4. 计算所有回答的平均分
            BigDecimal overallAvgScore = scoreCount > 0 ? 
                totalScore.divide(new BigDecimal(scoreCount), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            // 5. 按照repeat_index分组统计
            Map<Integer, List<Map<String, Object>>> answersByRepeatIndex = 
                detailedAnswers.stream().collect(
                    Collectors.groupingBy(a -> ((Number)a.get("repeat_index")).intValue())
                );
            
            Map<String, Object> repeatIndexStats = new HashMap<>();
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : answersByRepeatIndex.entrySet()) {
                int repeatIndex = entry.getKey();
                List<Map<String, Object>> repeatAnswers = entry.getValue();
                
                BigDecimal repeatTotalScore = BigDecimal.ZERO;
                int repeatScoreCount = 0;
                
                for (Map<String, Object> answer : repeatAnswers) {
                    if (answer.containsKey("averageScore") && !((BigDecimal)answer.get("averageScore")).equals(BigDecimal.ZERO)) {
                        repeatTotalScore = repeatTotalScore.add((BigDecimal)answer.get("averageScore"));
                        repeatScoreCount++;
                    }
                }
                
                BigDecimal repeatAvgScore = repeatScoreCount > 0 ? 
                    repeatTotalScore.divide(new BigDecimal(repeatScoreCount), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
                
                Map<String, Object> repeatStats = new HashMap<>();
                repeatStats.put("count", repeatAnswers.size());
                repeatStats.put("scoreCount", repeatScoreCount);
                repeatStats.put("averageScore", repeatAvgScore);
                
                repeatIndexStats.put("repeat_" + repeatIndex, repeatStats);
            }
            
            // 6. 构建最终响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("modelId", modelId);
            response.put("modelName", modelInfo.get("name"));
            response.put("provider", modelInfo.get("provider"));
            response.put("version", modelInfo.get("version"));
            response.put("questionId", questionId);
            response.put("questionText", questionInfo.get("question_text"));
            response.put("questionType", questionInfo.get("question_type"));
            response.put("difficulty", questionInfo.get("difficulty"));
            response.put("totalAnswers", answers.size());
            response.put("overallAverageScore", overallAvgScore);
            response.put("answers", detailedAnswers);
            response.put("repeatIndexStatistics", repeatIndexStats);
            response.put("hasAnswers", true);
            
            logger.info("成功获取模型ID{}对问题ID{}的详细回答及评分信息", modelId, questionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取模型问题详细评分信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("modelId", modelId);
            response.put("questionId", questionId);
            response.put("message", "获取模型问题详细评分信息时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 删除指定的LLM模型
     * 
     * @param modelId 要删除的模型ID
     * @return 删除操作的结果
     */
    @DeleteMapping("/{modelId}")
    public ResponseEntity<?> deleteModel(@PathVariable Long modelId) {
        logger.info("接收到删除模型的请求，模型ID: {}", modelId);
        
        try {
            Map<String, Object> response = llmModelService.deleteModel(modelId);
            
            if ((Boolean) response.get("success")) {
                logger.info("成功删除模型，ID: {}", modelId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("删除模型失败，ID: {}, 原因: {}", modelId, response.get("message"));
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("删除模型时发生错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除模型时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 