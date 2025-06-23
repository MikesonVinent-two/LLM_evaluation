package com.example.demo.service.impl;

import com.example.demo.service.ModelBatchScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelBatchScoreServiceImpl implements ModelBatchScoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelBatchScoreServiceImpl.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    @Transactional
    public boolean calculateBatchScores(Long batchId) {
        logger.info("开始计算批次ID{}的所有模型评分统计", batchId);
        try {
            // 获取批次中的所有模型
            String modelsQuery = 
                "SELECT DISTINCT lm.id FROM model_answer_runs mar " +
                "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
                "WHERE mar.answer_generation_batch_id = ?";
            
            List<Long> modelIds = jdbcTemplate.queryForList(modelsQuery, Long.class, batchId);
            
            if (modelIds.isEmpty()) {
                logger.warn("批次ID{}中没有找到任何模型", batchId);
                return false;
            }
            
            // 为每个模型计算评分统计
            for (Long modelId : modelIds) {
                calculateModelScoresInBatch(batchId, modelId);
            }
            
            logger.info("成功计算批次ID{}的所有模型评分统计", batchId);
            return true;
        } catch (Exception e) {
            logger.error("计算批次ID{}的模型评分统计时发生错误", batchId, e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public boolean calculateModelScoresInBatch(Long batchId, Long modelId) {
        logger.info("开始计算批次ID{}中模型ID{}的评分统计", batchId, modelId);
        try {
            // 1. 清除该模型在该批次中的现有统计数据
            String clearQuery = "DELETE FROM model_batch_scores WHERE batch_id = ? AND model_id = ?";
            jdbcTemplate.update(clearQuery, batchId, modelId);
            
            // 2. 计算总体评分（不区分评测者）
            calculateOverallScores(batchId, modelId);
            
            // 3. 计算客观题评分
            calculateObjectiveScores(batchId, modelId);
            
            // 4. 计算主观题评分（只考虑完成所有评测的评测者）
            calculateSubjectiveScores(batchId, modelId);
            
            // 5. 计算不同评分维度的统计
            calculateScoresByDimension(batchId, modelId);
            
            logger.info("成功计算批次ID{}中模型ID{}的评分统计", batchId, modelId);
            return true;
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的评分统计时发生错误", batchId, modelId, e);
            throw e;
        }
    }
    
    @Override
    public boolean clearBatchScores(Long batchId) {
        logger.info("开始清除批次ID{}的所有评分统计数据", batchId);
        try {
            String clearQuery = "DELETE FROM model_batch_scores WHERE batch_id = ?";
            int rowsAffected = jdbcTemplate.update(clearQuery, batchId);
            logger.info("成功清除批次ID{}的所有评分统计数据，共{}条记录", batchId, rowsAffected);
            return true;
        } catch (Exception e) {
            logger.error("清除批次ID{}的评分统计数据时发生错误", batchId, e);
            throw e;
        }
    }
    
    @Override
    public List<Map<String, Object>> getModelRankingsInBatch(Long batchId, String scoreType) {
        if (scoreType == null || scoreType.isEmpty()) {
            scoreType = "OVERALL";
        }
        
        logger.info("获取批次ID{}中所有模型的{}评分排名", batchId, scoreType);
        
        try {
            String rankingsQuery = 
                "SELECT mbs.model_id, lm.name AS model_name, lm.provider, lm.version, " +
                "mbs.average_score, mbs.total_answers, mbs.scored_answers, " +
                "mbs.max_score, mbs.min_score, mbs.calculated_at " +
                "FROM model_batch_scores mbs " +
                "JOIN llm_models lm ON mbs.model_id = lm.id " +
                "WHERE mbs.batch_id = ? AND mbs.score_type = ? AND mbs.evaluator_id IS NULL AND mbs.repeat_index = -1 " +
                "ORDER BY mbs.average_score DESC";
            
            return jdbcTemplate.queryForList(rankingsQuery, batchId, scoreType);
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型排名时发生错误", batchId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getModelScoreDetails(Long batchId, Long modelId) {
        logger.info("获取批次ID{}中模型ID{}的详细评分统计", batchId, modelId);
        
        try {
            // 1. 获取批次和模型的基本信息
            String batchQuery = "SELECT name, description FROM answer_generation_batches WHERE id = ?";
            Map<String, Object> batchInfo = jdbcTemplate.queryForMap(batchQuery, batchId);
            
            String modelQuery = "SELECT name, provider, version FROM llm_models WHERE id = ?";
            Map<String, Object> modelInfo = jdbcTemplate.queryForMap(modelQuery, modelId);
            
            // 2. 获取该模型在该批次中的所有评分统计
            String scoresQuery = 
                "SELECT score_type, evaluator_id, repeat_index, average_score, " +
                "total_answers, scored_answers, max_score, min_score, calculated_at " +
                "FROM model_batch_scores " +
                "WHERE batch_id = ? AND model_id = ? " +
                "ORDER BY score_type, evaluator_id, repeat_index";
            
            List<Map<String, Object>> scores = jdbcTemplate.queryForList(scoresQuery, batchId, modelId);
            
            // 3. 按评分类型分组
            Map<String, List<Map<String, Object>>> scoresByType = new HashMap<>();
            for (Map<String, Object> score : scores) {
                String type = (String) score.get("score_type");
                if (!scoresByType.containsKey(type)) {
                    scoresByType.put(type, new ArrayList<>());
                }
                scoresByType.get(type).add(score);
            }
            
            // 4. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", batchId);
            result.put("batchName", batchInfo.get("name"));
            result.put("batchDescription", batchInfo.get("description"));
            result.put("modelId", modelId);
            result.put("modelName", modelInfo.get("name"));
            result.put("provider", modelInfo.get("provider"));
            result.put("version", modelInfo.get("version"));
            result.put("scoresByType", scoresByType);
            
            return result;
        } catch (Exception e) {
            logger.error("获取批次ID{}中模型ID{}的评分详情时发生错误", batchId, modelId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取评分详情失败: " + e.getMessage());
            return errorResult;
        }
    }
    
    // 辅助方法: 计算总体评分
    private void calculateOverallScores(Long batchId, Long modelId) {
        logger.info("计算批次ID{}中模型ID{}的总体评分", batchId, modelId);
        
        try {
            // 1. 获取该模型在该批次中的所有回答
            String answersQuery = 
                "SELECT la.id, la.repeat_index " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ?";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(answersQuery, modelId, batchId);
            
            if (answers.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}没有找到任何回答", batchId, modelId);
                return;
            }
            else{
                logger.info("批次ID{}中模型ID{}找到{}个回答", batchId, modelId, answers.size());
            }
            
            // 获取回答ID列表
            List<Long> answerIds = new ArrayList<>();
            for (Map<String, Object> answer : answers) {
                answerIds.add((Long) answer.get("id"));
            }
            
            // 2. 获取这些回答的评分
            StringBuilder scoresQuery = new StringBuilder();
            scoresQuery.append("SELECT e.llm_answer_id, e.normalized_score, e.score_type, la.repeat_index, e.evaluator_id ");
            scoresQuery.append("FROM evaluations e ");
            scoresQuery.append("JOIN llm_answers la ON e.llm_answer_id = la.id ");
            scoresQuery.append("WHERE e.llm_answer_id IN (");
            for (int i = 0; i < answerIds.size(); i++) {
                scoresQuery.append("?");
                if (i < answerIds.size() - 1) {
                    scoresQuery.append(",");
                }
            }
            scoresQuery.append(") AND e.evaluation_status = 'SUCCESS'");
            
            List<Map<String, Object>> scores = jdbcTemplate.queryForList(
                    scoresQuery.toString(), answerIds.toArray());
            
            if (scores.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}的回答没有任何评分", batchId, modelId);
                return;
            }
            else{
                logger.info("批次ID{}中模型ID{}找到{}个评分", batchId, modelId, scores.size());
            }
            // 3. 按评分类型和回答重复索引分组计算
            Map<String, Map<Integer, List<Map<String, Object>>>> scoresByTypeAndRepeat = new HashMap<>();
            
            for (Map<String, Object> score : scores) {
                String scoreType = (String) score.get("score_type");
                if (scoreType == null) {
                    scoreType = "OVERALL"; // 默认使用OVERALL作为评分类型
                }
                
                Integer repeatIndex = ((Number) score.get("repeat_index")).intValue();
                logger.info("批次ID{}中模型ID{}评分类型为{}，重复索引为{}", batchId, modelId, scoreType, repeatIndex);

                if (!scoresByTypeAndRepeat.containsKey(scoreType)) {
                    scoresByTypeAndRepeat.put(scoreType, new HashMap<>());
                }
                
                Map<Integer, List<Map<String, Object>>> scoresByRepeat = scoresByTypeAndRepeat.get(scoreType);
                if (!scoresByRepeat.containsKey(repeatIndex)) {
                    scoresByRepeat.put(repeatIndex, new ArrayList<>());
                }
                
                scoresByRepeat.get(repeatIndex).add(score);
            }
            
            // 4. 计算并保存每种评分类型的统计数据
            for (Map.Entry<String, Map<Integer, List<Map<String, Object>>>> typeEntry : scoresByTypeAndRepeat.entrySet()) {
                String scoreType = typeEntry.getKey();
                Map<Integer, List<Map<String, Object>>> scoresByRepeat = typeEntry.getValue();
                
                // 计算每个重复索引的统计数据
                for (Map.Entry<Integer, List<Map<String, Object>>> repeatEntry : scoresByRepeat.entrySet()) {
                    Integer repeatIndex = repeatEntry.getKey();
                    List<Map<String, Object>> repeatScores = repeatEntry.getValue();
                    
                    // 计算统计数据
                    double totalScore = 0;
                    double maxScore = Double.MIN_VALUE;
                    double minScore = Double.MAX_VALUE;
                    
                    for (Map<String, Object> score : repeatScores) {
                        double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                        totalScore += normalizedScore;
                        maxScore = Math.max(maxScore, normalizedScore);
                        minScore = Math.min(minScore, normalizedScore);
                    }
                    
                    double averageScore = totalScore / repeatScores.size();
                    
                    // 保存重复索引的统计数据
                    String insertQuery = 
                        "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                        "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                        "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, 1)";
                    
                    jdbcTemplate.update(
                        insertQuery,
                        batchId,
                        modelId,
                        scoreType,
                        averageScore,
                        repeatScores.size(),
                        repeatScores.size(),
                        maxScore,
                        minScore,
                        repeatIndex
                    );
                    logger.info("批次ID{}中模型ID{}评分类型为{}，重复索引为{}，平均分为{}", batchId, modelId, scoreType, repeatIndex, averageScore);
                }
                
                // 计算所有重复索引的平均值
                List<Map<String, Object>> allScores = new ArrayList<>();
                for (List<Map<String, Object>> repeatScores : scoresByRepeat.values()) {
                    allScores.addAll(repeatScores);
                }
                
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : allScores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / allScores.size();
                
                // 保存所有重复索引的平均统计数据
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    scoreType,
                    averageScore,
                    allScores.size(),
                    allScores.size(),
                    maxScore,
                    minScore
                );
            }
            
            // 5. 计算所有评分类型的综合平均分
            logger.info("计算批次ID{}中模型ID{}的所有评分类型综合平均分", batchId, modelId);
            
            // 收集所有评分
            List<Map<String, Object>> allTypeScores = new ArrayList<>();
            for (Map<String, Object> score : scores) {
                allTypeScores.add(score);
            }
            
            if (!allTypeScores.isEmpty()) {
                // 按重复索引分组
                Map<Integer, List<Map<String, Object>>> allScoresByRepeat = new HashMap<>();
                
                for (Map<String, Object> score : allTypeScores) {
                    Integer repeatIndex = ((Number) score.get("repeat_index")).intValue();
                    
                    if (!allScoresByRepeat.containsKey(repeatIndex)) {
                        allScoresByRepeat.put(repeatIndex, new ArrayList<>());
                    }
                    
                    allScoresByRepeat.get(repeatIndex).add(score);
                }
                
                // 计算每个重复索引的综合统计数据
                for (Map.Entry<Integer, List<Map<String, Object>>> entry : allScoresByRepeat.entrySet()) {
                    Integer repeatIndex = entry.getKey();
                    List<Map<String, Object>> repeatScores = entry.getValue();
                    
                    double totalScore = 0;
                    double maxScore = Double.MIN_VALUE;
                    double minScore = Double.MAX_VALUE;
                    
                    for (Map<String, Object> score : repeatScores) {
                        double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                        totalScore += normalizedScore;
                        maxScore = Math.max(maxScore, normalizedScore);
                        minScore = Math.min(minScore, normalizedScore);
                    }
                    
                    double averageScore = totalScore / repeatScores.size();
                    
                    // 保存每个重复索引的综合评分统计
                    String insertQuery = 
                        "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                        "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                        "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, 1)";
                    
                    jdbcTemplate.update(
                        insertQuery,
                        batchId,
                        modelId,
                        "OVERALL",
                        averageScore,
                        repeatScores.size(),
                        repeatScores.size(),
                        maxScore,
                        minScore,
                        repeatIndex
                    );
                    
                    logger.info("批次ID{}中模型ID{}的综合评分，重复索引为{}，平均分为{}", batchId, modelId, repeatIndex, averageScore);
                }
                
                // 计算所有重复索引的综合平均值
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : allTypeScores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / allTypeScores.size();
                
                // 保存所有重复索引的综合平均统计数据
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    "OVERALL",
                    averageScore,
                    allTypeScores.size(),
                    allTypeScores.size(),
                    maxScore,
                    minScore
                );
                
                logger.info("批次ID{}中模型ID{}的综合评分，所有重复索引，平均分为{}", batchId, modelId, averageScore);
            }
            
            logger.info("成功计算批次ID{}中模型ID{}的总体评分", batchId, modelId);
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的总体评分时发生错误", batchId, modelId, e);
            throw e;
        }
    }
    
    // 辅助方法: 计算客观题评分
    private void calculateObjectiveScores(Long batchId, Long modelId) {
        logger.info("计算批次ID{}中模型ID{}的客观题评分", batchId, modelId);
        
        try {
            // 1. 获取该模型在该批次中的所有客观题回答
            String answersQuery = 
                "SELECT la.id, la.repeat_index " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ? " +
                "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE')";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(answersQuery, modelId, batchId);
            
            if (answers.isEmpty()) {
                logger.info("批次ID{}中模型ID{}没有找到任何客观题回答", batchId, modelId);
                return;
            }
            
            // 获取回答ID列表
            List<Long> answerIds = new ArrayList<>();
            for (Map<String, Object> answer : answers) {
                answerIds.add((Long) answer.get("id"));
            }
            
            // 2. 获取这些回答的评分
            StringBuilder scoresQuery = new StringBuilder();
            scoresQuery.append("SELECT e.llm_answer_id, e.normalized_score, la.repeat_index ");
            scoresQuery.append("FROM evaluations e ");
            scoresQuery.append("JOIN llm_answers la ON e.llm_answer_id = la.id ");
            scoresQuery.append("WHERE e.llm_answer_id IN (");
            for (int i = 0; i < answerIds.size(); i++) {
                scoresQuery.append("?");
                if (i < answerIds.size() - 1) {
                    scoresQuery.append(",");
                }
            }
            scoresQuery.append(") AND e.evaluation_status = 'SUCCESS'");
            
            List<Map<String, Object>> scores = jdbcTemplate.queryForList(
                    scoresQuery.toString(), answerIds.toArray());
            
            if (scores.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}的客观题回答没有任何评分", batchId, modelId);
                return;
            }
            
            // 3. 按回答重复索引分组计算
            Map<Integer, List<Map<String, Object>>> scoresByRepeat = new HashMap<>();
            
            for (Map<String, Object> score : scores) {
                Integer repeatIndex = ((Number) score.get("repeat_index")).intValue();
                
                if (!scoresByRepeat.containsKey(repeatIndex)) {
                    scoresByRepeat.put(repeatIndex, new ArrayList<>());
                }
                
                scoresByRepeat.get(repeatIndex).add(score);
            }
            
            // 4. 计算并保存客观题评分统计
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : scoresByRepeat.entrySet()) {
                Integer repeatIndex = entry.getKey();
                List<Map<String, Object>> repeatScores = entry.getValue();
                
                // 计算统计数据
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : repeatScores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / repeatScores.size();
                
                // 保存客观题评分统计
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    "OBJECTIVE",
                    averageScore,
                    repeatScores.size(),
                    repeatScores.size(),
                    maxScore,
                    minScore,
                    repeatIndex
                );
            }
            
            // 5. 计算所有重复索引的平均值
            List<Map<String, Object>> allScores = new ArrayList<>();
            for (List<Map<String, Object>> repeatScores : scoresByRepeat.values()) {
                allScores.addAll(repeatScores);
            }
            
            if (!allScores.isEmpty()) {
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : allScores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / allScores.size();
                
                // 保存所有重复索引的平均统计数据
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    "OBJECTIVE",
                    averageScore,
                    allScores.size(),
                    allScores.size(),
                    maxScore,
                    minScore
                );
            }
            
            logger.info("成功计算批次ID{}中模型ID{}的客观题评分", batchId, modelId);
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的客观题评分时发生错误", batchId, modelId, e);
            throw e;
        }
    }
    
    // 辅助方法: 计算主观题评分
    private void calculateSubjectiveScores(Long batchId, Long modelId) {
        logger.info("计算批次ID{}中模型ID{}的主观题评分", batchId, modelId);
        
        try {
            // 1. 获取该模型在该批次中的所有主观题回答
            String answersQuery = 
                "SELECT la.id, la.repeat_index " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
                "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ? " +
                "AND sq.question_type IN ('SUBJECTIVE', 'SIMPLE_FACT')";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(answersQuery, modelId, batchId);
            
            if (answers.isEmpty()) {
                logger.info("批次ID{}中模型ID{}没有找到任何主观题回答", batchId, modelId);
                return;
            }
            
            // 2. 获取所有回答ID和评测者ID
            List<Long> answerIds = new ArrayList<>();
            for (Map<String, Object> answer : answers) {
                answerIds.add((Long) answer.get("id"));
            }
            
            if (answerIds.isEmpty()) {
                return;
            }
            
            // 3. 找出完成了所有主观题评测的评测者
            String evaluatorsQuery = "SELECT e.evaluator_id, COUNT(DISTINCT e.llm_answer_id) as answer_count, ev.name as evaluator_name " +
                    "FROM evaluations e " +
                    "JOIN evaluators ev ON e.evaluator_id = ev.id " +
                    "WHERE e.llm_answer_id IN (";
            
            for (int i = 0; i < answerIds.size(); i++) {
                evaluatorsQuery += "?";
                if (i < answerIds.size() - 1) {
                    evaluatorsQuery += ",";
                }
            }
            
            evaluatorsQuery += ") AND e.evaluation_status = 'SUCCESS' " +
                    "GROUP BY e.evaluator_id " +
                    "HAVING answer_count = ?";
            
            Object[] params = new Object[answerIds.size() + 1];
            for (int i = 0; i < answerIds.size(); i++) {
                params[i] = answerIds.get(i);
            }
            params[answerIds.size()] = answerIds.size();
            
            List<Map<String, Object>> completeEvaluators = jdbcTemplate.queryForList(evaluatorsQuery, params);
            
            if (completeEvaluators.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}的主观题回答没有任何评测者完成全部评测", batchId, modelId);
                return;
            }
            
            // 4. 对每个完成所有评测的评测者，计算主观题评分
            List<Double> evaluatorAvgScores = new ArrayList<>();
            
            for (Map<String, Object> evaluator : completeEvaluators) {
                Long evaluatorId = (Long) evaluator.get("evaluator_id");
                String evaluatorName = (String) evaluator.get("evaluator_name");
                
                // 获取该评测者的所有评分
                String scoresQuery = "SELECT e.llm_answer_id, e.normalized_score, la.repeat_index " +
                        "FROM evaluations e " +
                        "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                        "WHERE e.llm_answer_id IN (";
                
                for (int i = 0; i < answerIds.size(); i++) {
                    scoresQuery += "?";
                    if (i < answerIds.size() - 1) {
                        scoresQuery += ",";
                    }
                }
                
                scoresQuery += ") AND e.evaluator_id = ? AND e.evaluation_status = 'SUCCESS'";
                
                Object[] scoreParams = new Object[answerIds.size() + 1];
                for (int i = 0; i < answerIds.size(); i++) {
                    scoreParams[i] = answerIds.get(i);
                }
                scoreParams[answerIds.size()] = evaluatorId;
                
                List<Map<String, Object>> evaluatorScores = jdbcTemplate.queryForList(scoresQuery, scoreParams);
                
                if (evaluatorScores.isEmpty()) {
                    continue;
                }
                
                // 按回答重复索引分组
                Map<Integer, List<Map<String, Object>>> scoresByRepeat = new HashMap<>();
                
                for (Map<String, Object> score : evaluatorScores) {
                    Integer repeatIndex = ((Number) score.get("repeat_index")).intValue();
                    
                    if (!scoresByRepeat.containsKey(repeatIndex)) {
                        scoresByRepeat.put(repeatIndex, new ArrayList<>());
                    }
                    
                    scoresByRepeat.get(repeatIndex).add(score);
                }
                
                // 计算每个重复索引的统计数据
                for (Map.Entry<Integer, List<Map<String, Object>>> entry : scoresByRepeat.entrySet()) {
                    Integer repeatIndex = entry.getKey();
                    List<Map<String, Object>> repeatScores = entry.getValue();
                    
                    // 计算统计数据
                    double totalScore = 0;
                    double maxScore = Double.MIN_VALUE;
                    double minScore = Double.MAX_VALUE;
                    
                    for (Map<String, Object> score : repeatScores) {
                        double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                        totalScore += normalizedScore;
                        maxScore = Math.max(maxScore, normalizedScore);
                        minScore = Math.min(minScore, normalizedScore);
                    }
                    
                    double averageScore = totalScore / repeatScores.size();
                    
                    // 保存该评测者对特定重复索引的主观题评分统计
                    String insertQuery = 
                        "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                        "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
                    
                    jdbcTemplate.update(
                        insertQuery,
                        batchId,
                        modelId,
                        evaluatorId,
                        "SUBJECTIVE",
                        averageScore,
                        repeatScores.size(),
                        repeatScores.size(),
                        maxScore,
                        minScore,
                        repeatIndex
                    );
                }
                
                // 计算该评测者所有重复索引的平均值
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : evaluatorScores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / evaluatorScores.size();
                evaluatorAvgScores.add(averageScore);
                
                // 保存该评测者所有重复索引的平均统计数据
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    evaluatorId,
                    "SUBJECTIVE",
                    averageScore,
                    evaluatorScores.size(),
                    evaluatorScores.size(),
                    maxScore,
                    minScore
                );
                
                logger.info("成功计算批次ID{}中模型ID{}的评测者ID{}({})的主观题评分", batchId, modelId, evaluatorId, evaluatorName);
            }
            
            // 5. 计算所有评测者的平均分
            if (!evaluatorAvgScores.isEmpty()) {
                double totalAvg = 0;
                for (Double avg : evaluatorAvgScores) {
                    totalAvg += avg;
                }
                
                double overallAvg = totalAvg / evaluatorAvgScores.size();
                
                // 保存所有评测者的平均主观题评分
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, NULL, NULL, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    "SUBJECTIVE",
                    overallAvg,
                    answerIds.size(),
                    answerIds.size()
                );
            }
            
            logger.info("成功计算批次ID{}中模型ID{}的主观题评分", batchId, modelId);
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的主观题评分时发生错误", batchId, modelId, e);
            throw e;
        }
    }
    
    // 辅助方法: 计算不同评分维度的统计
    private void calculateScoresByDimension(Long batchId, Long modelId) {
        logger.info("计算批次ID{}中模型ID{}的不同评分维度统计", batchId, modelId);
        
        try {
            // 1. 获取该模型在该批次中的所有回答
            String answersQuery = 
                "SELECT la.id, la.repeat_index " +
                "FROM llm_answers la " +
                "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
                "WHERE mar.llm_model_id = ? AND mar.answer_generation_batch_id = ?";
            
            List<Map<String, Object>> answers = jdbcTemplate.queryForList(answersQuery, modelId, batchId);
            
            if (answers.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}没有找到任何回答", batchId, modelId);
                return;
            }
            
            // 获取回答ID列表
            List<Long> answerIds = new ArrayList<>();
            for (Map<String, Object> answer : answers) {
                answerIds.add((Long) answer.get("id"));
            }
            
            // 2. 获取常见的评分维度
            String dimensionsQuery = "SELECT DISTINCT score_type FROM evaluations " +
                    "WHERE llm_answer_id IN (";
            
            for (int i = 0; i < answerIds.size(); i++) {
                dimensionsQuery += "?";
                if (i < answerIds.size() - 1) {
                    dimensionsQuery += ",";
                }
            }
            
            dimensionsQuery += ") AND score_type IS NOT NULL AND evaluation_status = 'SUCCESS'";
            
            List<String> dimensions = jdbcTemplate.queryForList(dimensionsQuery, String.class, answerIds.toArray());
            
            if (dimensions.isEmpty()) {
                logger.warn("批次ID{}中模型ID{}的回答没有找到任何评分维度", batchId, modelId);
                return;
            }
            
            // 3. 对每个评分维度，计算统计数据
            for (String dimension : dimensions) {
                // 跳过已经在其他方法中处理的总体评分类型
                if (dimension.equals("OVERALL") || dimension.equals("OBJECTIVE") || dimension.equals("SUBJECTIVE")) {
                    continue;
                }
                
                // 获取该维度的所有评分
                String scoresQuery = "SELECT e.llm_answer_id, e.normalized_score, la.repeat_index " +
                        "FROM evaluations e " +
                        "JOIN llm_answers la ON e.llm_answer_id = la.id " +
                        "WHERE e.llm_answer_id IN (";
                
                for (int i = 0; i < answerIds.size(); i++) {
                    scoresQuery += "?";
                    if (i < answerIds.size() - 1) {
                        scoresQuery += ",";
                    }
                }
                
                scoresQuery += ") AND e.score_type = ? AND e.evaluation_status = 'SUCCESS'";
                
                Object[] params = new Object[answerIds.size() + 1];
                for (int i = 0; i < answerIds.size(); i++) {
                    params[i] = answerIds.get(i);
                }
                params[answerIds.size()] = dimension;
                
                List<Map<String, Object>> scores = jdbcTemplate.queryForList(scoresQuery, params);
                
                if (scores.isEmpty()) {
                    continue;
                }
                
                // 按回答重复索引分组
                Map<Integer, List<Map<String, Object>>> scoresByRepeat = new HashMap<>();
                
                for (Map<String, Object> score : scores) {
                    Integer repeatIndex = ((Number) score.get("repeat_index")).intValue();
                    
                    if (!scoresByRepeat.containsKey(repeatIndex)) {
                        scoresByRepeat.put(repeatIndex, new ArrayList<>());
                    }
                    
                    scoresByRepeat.get(repeatIndex).add(score);
                }
                
                // 计算每个重复索引的统计数据
                for (Map.Entry<Integer, List<Map<String, Object>>> entry : scoresByRepeat.entrySet()) {
                    Integer repeatIndex = entry.getKey();
                    List<Map<String, Object>> repeatScores = entry.getValue();
                    
                    // 计算统计数据
                    double totalScore = 0;
                    double maxScore = Double.MIN_VALUE;
                    double minScore = Double.MAX_VALUE;
                    
                    for (Map<String, Object> score : repeatScores) {
                        double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                        totalScore += normalizedScore;
                        maxScore = Math.max(maxScore, normalizedScore);
                        minScore = Math.min(minScore, normalizedScore);
                    }
                    
                    double averageScore = totalScore / repeatScores.size();
                    
                    // 保存该维度对特定重复索引的评分统计
                    String insertQuery = 
                        "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                        "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                        "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, 1)";
                    
                    jdbcTemplate.update(
                        insertQuery,
                        batchId,
                        modelId,
                        dimension,
                        averageScore,
                        repeatScores.size(),
                        repeatScores.size(),
                        maxScore,
                        minScore,
                        repeatIndex
                    );
                }
                
                // 计算所有重复索引的平均值
                double totalScore = 0;
                double maxScore = Double.MIN_VALUE;
                double minScore = Double.MAX_VALUE;
                
                for (Map<String, Object> score : scores) {
                    double normalizedScore = ((Number) score.get("normalized_score")).doubleValue();
                    totalScore += normalizedScore;
                    maxScore = Math.max(maxScore, normalizedScore);
                    minScore = Math.min(minScore, normalizedScore);
                }
                
                double averageScore = totalScore / scores.size();
                
                // 保存所有重复索引的平均统计数据
                String insertQuery = 
                    "INSERT INTO model_batch_scores (batch_id, model_id, evaluator_id, score_type, " +
                    "average_score, total_answers, scored_answers, max_score, min_score, repeat_index, created_by_user_id) " +
                    "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, -1, 1)";
                
                jdbcTemplate.update(
                    insertQuery,
                    batchId,
                    modelId,
                    dimension,
                    averageScore,
                    scores.size(),
                    scores.size(),
                    maxScore,
                    minScore
                );
                
                logger.info("成功计算批次ID{}中模型ID{}的{}评分维度统计", batchId, modelId, dimension);
            }
            
            logger.info("成功计算批次ID{}中模型ID{}的所有评分维度统计", batchId, modelId);
        } catch (Exception e) {
            logger.error("计算批次ID{}中模型ID{}的评分维度统计时发生错误", batchId, modelId, e);
            throw e;
        }
    }
} 