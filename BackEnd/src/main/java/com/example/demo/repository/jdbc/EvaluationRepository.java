package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.EvaluationType;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的评测仓库实现
 */
@Repository
public class EvaluationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;
    private final ObjectMapper objectMapper;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluations (llm_answer_id, evaluator_id, evaluation_run_id, evaluation_type, overall_score, " +
            "evaluation_time, evaluation_status, error_message, evaluation_results, prompt_used, comments, raw_evaluator_response, " +
            "created_by_user_id, created_change_log_id, creation_time, completion_time, raw_score, normalized_score, weighted_score, " +
            "score_type, scoring_method) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluations SET llm_answer_id=?, evaluator_id=?, evaluation_run_id=?, evaluation_type=?, overall_score=?, " +
            "evaluation_time=?, evaluation_status=?, error_message=?, evaluation_results=?, prompt_used=?, comments=?, " +
            "raw_evaluator_response=?, created_by_user_id=?, created_change_log_id=?, creation_time=?, completion_time=?, " +
            "raw_score=?, normalized_score=?, weighted_score=?, score_type=?, scoring_method=? " +
            "WHERE id=?";
    
    private static final String SQL_DELETE = "DELETE FROM evaluations WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluations WHERE id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID = 
            "SELECT * FROM evaluations WHERE llm_answer_id=?";
    
    private static final String SQL_FIND_BY_EVALUATION_RUN_ID = 
            "SELECT * FROM evaluations WHERE evaluation_run_id=?";
    
    private static final String SQL_FIND_BY_EVALUATOR_ID = 
            "SELECT * FROM evaluations WHERE evaluator_id=?";
    
    private static final String SQL_FIND_ANSWER_IDS_BY_EVALUATOR_ID = 
            "SELECT llm_answer_id FROM evaluations WHERE evaluator_id=?";
    
    private static final String SQL_COUNT_BY_EVALUATOR_ID_AND_LLM_ANSWER_IN_LIST = 
            "SELECT COUNT(*) FROM evaluations WHERE evaluator_id=? AND llm_answer_id IN (%s)";
    
    private static final String SQL_COUNT_COMPLETED_BY_EVALUATION_RUN_ID = 
            "SELECT COUNT(*) FROM evaluations WHERE evaluation_run_id=? AND evaluation_status='SUCCESS'";
    
    private static final String SQL_COUNT_FAILED_BY_EVALUATION_RUN_ID = 
            "SELECT COUNT(*) FROM evaluations WHERE evaluation_run_id=? AND evaluation_status='FAILED'";
    
    private static final String SQL_FIND_BY_ANSWER_GENERATION_BATCH_ID = 
            "SELECT e.* FROM evaluations e " +
            "JOIN llm_answers la ON e.llm_answer_id = la.id " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "WHERE mar.answer_generation_batch_id=?";
    
    private static final String SQL_COUNT_BY_EVALUATION_RUN_ID = 
            "SELECT COUNT(*) FROM evaluations WHERE evaluation_run_id=?";
    
    private static final String SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID = 
            "SELECT COUNT(*) FROM evaluations WHERE llm_answer_id=? AND evaluator_id=?";
    
    private static final String SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATION_RUN_ID = 
            "SELECT COUNT(*) FROM evaluations WHERE llm_answer_id=? AND evaluation_run_id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID = 
            "SELECT * FROM evaluations WHERE llm_answer_id=? AND evaluator_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluations";

    private static final String SQL_FIND_COMPLETED_HUMAN_EVALUATIONS = 
            "SELECT e.*, la.id as answer_id, la.answer_text, mar.id as run_id, " +
            "lm.id as model_id, lm.name as model_name, sq.id as question_id, " +
            "sq.question_text, sq.question_type, sq.difficulty as difficulty_level " +
            "FROM evaluations e " +
            "JOIN llm_answers la ON e.llm_answer_id = la.id " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "JOIN evaluators ev ON e.evaluator_id = ev.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND ev.evaluator_type = 'HUMAN' " +
            "AND e.evaluator_id = ? " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "AND (? IS NULL OR sq.question_type = ?) " +
            "ORDER BY e.evaluation_time DESC " +
            "LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_COMPLETED_HUMAN_EVALUATIONS = 
            "SELECT COUNT(*) " +
            "FROM evaluations e " +
            "JOIN llm_answers la ON e.llm_answer_id = la.id " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "JOIN evaluators ev ON e.evaluator_id = ev.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND ev.evaluator_type = 'HUMAN' " +
            "AND e.evaluator_id = ? " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "AND (? IS NULL OR sq.question_type = ?)";

    @Autowired
    public EvaluationRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存评测
     *
     * @param evaluation 评测对象
     * @return 带有ID的评测对象
     */
    public Evaluation save(Evaluation evaluation) {
        if (evaluation.getId() == null) {
            return insert(evaluation);
        } else {
            return update(evaluation);
        }
    }

    /**
     * 插入新评测
     *
     * @param evaluation 评测对象
     * @return 带有ID的评测对象
     */
    private Evaluation insert(Evaluation evaluation) {
        if (evaluation.getEvaluationTime() == null) {
            evaluation.setEvaluationTime(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置LLM回答ID
            ps.setLong(1, evaluation.getLlmAnswer().getId());
            
            // 设置评测者ID
            ps.setLong(2, evaluation.getEvaluator().getId());
            
            // 设置评测运行ID
            if (evaluation.getEvaluationRun() != null && evaluation.getEvaluationRun().getId() != null) {
                ps.setLong(3, evaluation.getEvaluationRun().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            
            // 设置评测类型
            ps.setString(4, evaluation.getEvaluationType().name());
            
            // 设置总体分数
            if (evaluation.getScore() != null) {
                ps.setBigDecimal(5, evaluation.getScore());
            } else {
                ps.setNull(5, Types.DECIMAL);
            }
            
            // 设置评测时间
            ps.setTimestamp(6, Timestamp.valueOf(evaluation.getEvaluationTime()));
            
            // 设置评测状态
            ps.setString(7, evaluation.getStatus().name());
            
            // 设置错误消息
            if (evaluation.getErrorMessage() != null) {
                ps.setString(8, evaluation.getErrorMessage());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            
            // 设置评测结果(JSON)
            if (evaluation.getEvaluationResults() != null) {
                try {
                    ps.setString(9, objectMapper.writeValueAsString(evaluation.getEvaluationResults()));
                } catch (JsonProcessingException e) {
                    ps.setString(9, "{}");
                }
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置使用的提示词
            if (evaluation.getPromptUsed() != null) {
                ps.setString(10, evaluation.getPromptUsed());
            } else {
                ps.setNull(10, Types.VARCHAR);
            }
            
            // 设置评论
            if (evaluation.getComments() != null) {
                ps.setString(11, evaluation.getComments());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }
            
            // 设置原始评测响应
            if (evaluation.getRawEvaluatorResponse() != null) {
                ps.setString(12, evaluation.getRawEvaluatorResponse());
            } else {
                ps.setNull(12, Types.VARCHAR);
            }
            
            // 设置创建用户ID
            if (evaluation.getCreatedByUser() != null && evaluation.getCreatedByUser().getId() != null) {
                ps.setLong(13, evaluation.getCreatedByUser().getId());
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (evaluation.getCreatedChangeLog() != null && evaluation.getCreatedChangeLog().getId() != null) {
                ps.setLong(14, evaluation.getCreatedChangeLog().getId());
            } else {
                ps.setNull(14, Types.BIGINT);
            }
            
            // 设置创建时间
            if (evaluation.getCreationTime() != null) {
                ps.setTimestamp(15, Timestamp.valueOf(evaluation.getCreationTime()));
            } else {
                ps.setNull(15, Types.TIMESTAMP);
            }
            
            // 设置完成时间
            if (evaluation.getCompletionTime() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(evaluation.getCompletionTime()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            
            // 设置原始分数
            if (evaluation.getRawScore() != null) {
                ps.setBigDecimal(17, evaluation.getRawScore());
            } else {
                ps.setNull(17, Types.DECIMAL);
            }
            
            // 设置标准化分数
            if (evaluation.getNormalizedScore() != null) {
                ps.setBigDecimal(18, evaluation.getNormalizedScore());
            } else {
                ps.setNull(18, Types.DECIMAL);
            }
            
            // 设置加权分数
            if (evaluation.getWeightedScore() != null) {
                ps.setBigDecimal(19, evaluation.getWeightedScore());
            } else {
                ps.setNull(19, Types.DECIMAL);
            }
            
            // 设置分数类型
            if (evaluation.getScoreType() != null) {
                ps.setString(20, evaluation.getScoreType());
            } else {
                ps.setNull(20, Types.VARCHAR);
            }
            
            // 设置打分方法
            if (evaluation.getScoringMethod() != null) {
                ps.setString(21, evaluation.getScoringMethod());
            } else {
                ps.setNull(21, Types.VARCHAR);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            evaluation.setId(key.longValue());
        }
        return evaluation;
    }

    /**
     * 更新评测
     *
     * @param evaluation 评测对象
     * @return 更新后的评测对象
     */
    private Evaluation update(Evaluation evaluation) {
        String evaluationResultsJson;
        try {
            evaluationResultsJson = evaluation.getEvaluationResults() != null ?
                    objectMapper.writeValueAsString(evaluation.getEvaluationResults()) : "{}";
        } catch (JsonProcessingException e) {
            evaluationResultsJson = "{}";
        }
        
        jdbcTemplate.update(SQL_UPDATE,
                evaluation.getLlmAnswer().getId(),
                evaluation.getEvaluator().getId(),
                evaluation.getEvaluationRun() != null ? evaluation.getEvaluationRun().getId() : null,
                evaluation.getEvaluationType().name(),
                evaluation.getScore(),
                Timestamp.valueOf(evaluation.getEvaluationTime()),
                evaluation.getStatus().name(),
                evaluation.getErrorMessage(),
                evaluationResultsJson,
                evaluation.getPromptUsed(),
                evaluation.getComments(),
                evaluation.getRawEvaluatorResponse(),
                evaluation.getCreatedByUser() != null ? evaluation.getCreatedByUser().getId() : null,
                evaluation.getCreatedChangeLog() != null ? evaluation.getCreatedChangeLog().getId() : null,
                evaluation.getCreationTime() != null ? Timestamp.valueOf(evaluation.getCreationTime()) : null,
                evaluation.getCompletionTime() != null ? Timestamp.valueOf(evaluation.getCompletionTime()) : null,
                evaluation.getRawScore(),
                evaluation.getNormalizedScore(),
                evaluation.getWeightedScore(),
                evaluation.getScoreType(),
                evaluation.getScoringMethod(),
                evaluation.getId());

        return evaluation;
    }

    /**
     * 根据ID查找评测
     *
     * @param id 评测ID
     * @return 评测的Optional包装
     */
    public Optional<Evaluation> findById(Long id) {
        try {
            Evaluation evaluation = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new EvaluationRowMapper());
            return Optional.ofNullable(evaluation);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据LLM回答ID查找评测
     *
     * @param llmAnswerId LLM回答ID
     * @return 评测列表
     */
    public List<Evaluation> findByLlmAnswerId(Long llmAnswerId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_LLM_ANSWER_ID,
                new Object[]{llmAnswerId},
                new EvaluationRowMapper()
        );
    }

    /**
     * 根据评测运行ID查找评测
     *
     * @param evaluationRunId 评测运行ID
     * @return 评测列表
     */
    public List<Evaluation> findByEvaluationRunId(Long evaluationRunId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATION_RUN_ID,
                new Object[]{evaluationRunId},
                new EvaluationRowMapper()
        );
    }

    /**
     * 根据评测者ID查找评测
     *
     * @param evaluatorId 评测者ID
     * @return 评测列表
     */
    public List<Evaluation> findByEvaluatorId(Long evaluatorId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_ID,
                new Object[]{evaluatorId},
                new EvaluationRowMapper()
        );
    }

    /**
     * 根据评测者ID查询已评测的回答ID列表
     *
     * @param evaluatorId 评测者ID
     * @return 已评测的回答ID列表
     */
    public List<Long> findAnswerIdsByEvaluatorId(Long evaluatorId) {
        return jdbcTemplate.queryForList(
                SQL_FIND_ANSWER_IDS_BY_EVALUATOR_ID,
                Long.class,
                evaluatorId
        );
    }

    /**
     * 根据评测者ID和回答ID列表统计评测数量
     *
     * @param evaluatorId 评测者ID
     * @param llmAnswerIds 回答ID列表
     * @return 评测数量
     */
    public int countByEvaluatorIdAndLlmAnswerInList(Long evaluatorId, List<Long> llmAnswerIds) {
        if (llmAnswerIds == null || llmAnswerIds.isEmpty()) {
            return 0;
        }
        
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < llmAnswerIds.size(); i++) {
            placeholders.append(i > 0 ? ", ?" : "?");
        }
        
        String sql = String.format(SQL_COUNT_BY_EVALUATOR_ID_AND_LLM_ANSWER_IN_LIST, placeholders.toString());
        
        Object[] params = new Object[llmAnswerIds.size() + 1];
        params[0] = evaluatorId;
        for (int i = 0; i < llmAnswerIds.size(); i++) {
            params[i + 1] = llmAnswerIds.get(i);
        }
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count != null ? count : 0;
    }

    /**
     * 统计评测运行中已完成的评测数量
     *
     * @param evaluationRunId 评测运行ID
     * @return 已完成评测数量
     */
    public int countCompletedByEvaluationRunId(Long evaluationRunId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_COMPLETED_BY_EVALUATION_RUN_ID,
                Integer.class,
                evaluationRunId
        );
        return count != null ? count : 0;
    }

    /**
     * 统计评测运行中失败的评测数量
     *
     * @param evaluationRunId 评测运行ID
     * @return 失败评测数量
     */
    public int countFailedByEvaluationRunId(Long evaluationRunId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_FAILED_BY_EVALUATION_RUN_ID,
                Integer.class,
                evaluationRunId
        );
        return count != null ? count : 0;
    }

    /**
     * 查找特定回答生成批次的所有评测
     *
     * @param batchId 回答生成批次ID
     * @return 评测列表
     */
    public List<Evaluation> findByAnswerGenerationBatchId(Long batchId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_ANSWER_GENERATION_BATCH_ID,
                new Object[]{batchId},
                new EvaluationRowMapper()
        );
    }

    /**
     * 统计评测运行中的总评测数量
     *
     * @param evaluationRunId 评测运行ID
     * @return 总评测数量
     */
    public int countByEvaluationRunId(Long evaluationRunId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_BY_EVALUATION_RUN_ID,
                Integer.class,
                evaluationRunId
        );
        return count != null ? count : 0;
    }

    /**
     * 检查指定回答ID和评测者ID的评测是否存在
     *
     * @param llmAnswerId 回答ID
     * @param evaluatorId 评测者ID
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID,
                Integer.class,
                llmAnswerId, evaluatorId
        );
        return count != null && count > 0;
    }

    /**
     * 检查指定回答ID和评测运行ID的评测是否存在
     *
     * @param llmAnswerId 回答ID
     * @param evaluationRunId 评测运行ID
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsByLlmAnswerIdAndEvaluationRunId(Long llmAnswerId, Long evaluationRunId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATION_RUN_ID,
                Integer.class,
                llmAnswerId, evaluationRunId
        );
        return count != null && count > 0;
    }

    /**
     * 根据回答ID和评测者ID查找评测
     *
     * @param llmAnswerId 回答ID
     * @param evaluatorId 评测者ID
     * @return 评测列表
     */
    public List<Evaluation> findByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID,
                new Object[]{llmAnswerId, evaluatorId},
                new EvaluationRowMapper()
        );
    }

    /**
     * 查找所有评测
     *
     * @return 评测列表
     */
    public List<Evaluation> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationRowMapper());
    }

    /**
     * 获取已完成的人工评测
     *
     * @param evaluatorIds 评测者ID列表
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param questionType 问题类型，如果为null则查询所有类型
     * @param pageable 分页信息
     * @return 包含已完成评测信息的Map列表
     */
    public List<Map<String, Object>> findCompletedHumanEvaluations(
            List<Long> evaluatorIds, Long batchId, List<Long> modelIds, 
            com.example.demo.entity.jdbc.QuestionType questionType, 
            org.springframework.data.domain.Pageable pageable) {
        
        String sql;
        Object[] params;
        
        if (modelIds != null && !modelIds.isEmpty()) {
            // 有模型ID列表，使用IN子句
            String modelInClause = String.join(",", Collections.nCopies(modelIds.size(), "?"));
            sql = String.format(SQL_FIND_COMPLETED_HUMAN_EVALUATIONS, modelInClause);
            
            params = new Object[modelIds.size() + 7];
            
            int paramIndex = 0;
            params[paramIndex++] = batchId;
            params[paramIndex++] = evaluatorIds.get(0); // 使用评测者ID
            params[paramIndex++] = 1; // 非空标志
            
            for (Long modelId : modelIds) {
                params[paramIndex++] = modelId;
            }
            
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
            params[paramIndex++] = pageable.getPageSize();
            params[paramIndex++] = pageable.getOffset();
        } else {
            // 没有模型ID列表，使用1=0条件（永假）代替空的IN子句
            sql = SQL_FIND_COMPLETED_HUMAN_EVALUATIONS.replace("lm.id IN (%s)", "1=1");
            
            params = new Object[7];
            
            int paramIndex = 0;
            params[paramIndex++] = batchId;
            params[paramIndex++] = evaluatorIds.get(0); // 使用评测者ID
            params[paramIndex++] = null; // 使用null表示不应用模型过滤
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
            params[paramIndex++] = pageable.getPageSize();
            params[paramIndex++] = pageable.getOffset();
        }
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("evaluationId", rs.getLong("id"));
            result.put("answerId", rs.getLong("answer_id"));
            result.put("answerText", rs.getString("answer_text"));
            result.put("runId", rs.getLong("run_id"));
            result.put("modelId", rs.getLong("model_id"));
            result.put("modelName", rs.getString("model_name"));
            result.put("questionId", rs.getLong("question_id"));
            result.put("questionText", rs.getString("question_text"));
            result.put("questionType", rs.getString("question_type"));
            result.put("difficultyLevel", rs.getString("difficulty_level"));
            result.put("score", rs.getBigDecimal("overall_score"));
            result.put("comments", rs.getString("comments"));
            result.put("evaluationTime", rs.getTimestamp("evaluation_time"));
            
            String evaluationResults = rs.getString("evaluation_results");
            if (evaluationResults != null) {
                try {
                    result.put("evaluationResults", objectMapper.readValue(evaluationResults, Map.class));
                } catch (Exception e) {
                    result.put("evaluationResults", new HashMap<>());
                }
            } else {
                result.put("evaluationResults", new HashMap<>());
            }
            
            return result;
        });
    }
    
    /**
     * 统计已完成的人工评测总数
     *
     * @param evaluatorIds 评测者ID列表
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param questionType 问题类型，如果为null则查询所有类型
     * @return 总记录数
     */
    public long countCompletedHumanEvaluations(
            List<Long> evaluatorIds, Long batchId, List<Long> modelIds, 
            com.example.demo.entity.jdbc.QuestionType questionType) {
        
        String sql;
        Object[] params;
        
        if (modelIds != null && !modelIds.isEmpty()) {
            // 有模型ID列表，使用IN子句
            String modelInClause = String.join(",", Collections.nCopies(modelIds.size(), "?"));
            sql = String.format(SQL_COUNT_COMPLETED_HUMAN_EVALUATIONS, modelInClause);
            
            params = new Object[modelIds.size() + 5];
            
            int paramIndex = 0;
            params[paramIndex++] = batchId;
            params[paramIndex++] = evaluatorIds.get(0); // 使用评测者ID
            params[paramIndex++] = 1; // 非空标志
            
            for (Long modelId : modelIds) {
                params[paramIndex++] = modelId;
            }
            
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
        } else {
            // 没有模型ID列表，使用1=1条件代替空的IN子句
            sql = SQL_COUNT_COMPLETED_HUMAN_EVALUATIONS.replace("lm.id IN (%s)", "1=1");
            
            params = new Object[5];
            
            int paramIndex = 0;
            params[paramIndex++] = batchId;
            params[paramIndex++] = evaluatorIds.get(0); // 使用评测者ID
            params[paramIndex++] = null; // 使用null表示不应用模型过滤
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
        }
        
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    /**
     * 评测行映射器
     */
    private class EvaluationRowMapper implements RowMapper<Evaluation> {
        @Override
        public Evaluation mapRow(ResultSet rs, int rowNum) throws SQLException {
            Evaluation evaluation = new Evaluation();
            evaluation.setId(rs.getLong("id"));
            
            // 设置LLM回答
            Long llmAnswerId = rs.getLong("llm_answer_id");
            if (!rs.wasNull()) {
                LlmAnswer llmAnswer = new LlmAnswer();
                llmAnswer.setId(llmAnswerId);
                evaluation.setLlmAnswer(llmAnswer);
            }
            
            // 设置评测者
            Long evaluatorId = rs.getLong("evaluator_id");
            if (!rs.wasNull()) {
                Evaluator evaluator = new Evaluator();
                evaluator.setId(evaluatorId);
                evaluation.setEvaluator(evaluator);
            }
            
            // 设置评测运行
            Long evaluationRunId = rs.getLong("evaluation_run_id");
            if (!rs.wasNull()) {
                EvaluationRun evaluationRun = new EvaluationRun();
                evaluationRun.setId(evaluationRunId);
                evaluation.setEvaluationRun(evaluationRun);
            }
            
            // 设置评测类型
            String evaluationTypeStr = rs.getString("evaluation_type");
            if (evaluationTypeStr != null) {
                evaluation.setEvaluationType(EvaluationType.valueOf(evaluationTypeStr));
            }
            
            // 设置总体分数
            evaluation.setScore(rs.getBigDecimal("overall_score"));
            
            // 设置评测时间
            Timestamp evaluationTime = rs.getTimestamp("evaluation_time");
            if (evaluationTime != null) {
                evaluation.setEvaluationTime(evaluationTime.toLocalDateTime());
            }
            
            // 设置评测状态
            String statusStr = rs.getString("evaluation_status");
            if (statusStr != null) {
                evaluation.setStatus(Evaluation.EvaluationStatus.valueOf(statusStr));
            }
            
            // 设置错误消息
            evaluation.setErrorMessage(rs.getString("error_message"));
            
            // 设置评测结果(JSON)
            String evaluationResultsJson = rs.getString("evaluation_results");
            if (evaluationResultsJson != null && !evaluationResultsJson.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evaluationResults = objectMapper.readValue(evaluationResultsJson, Map.class);
                    evaluation.setEvaluationResults(evaluationResults);
                } catch (JsonProcessingException e) {
                    evaluation.setEvaluationResults(new HashMap<>());
                }
            } else {
                evaluation.setEvaluationResults(new HashMap<>());
            }
            
            // 设置使用的提示词
            evaluation.setPromptUsed(rs.getString("prompt_used"));
            
            // 设置评论
            evaluation.setComments(rs.getString("comments"));
            
            // 设置原始评测响应
            evaluation.setRawEvaluatorResponse(rs.getString("raw_evaluator_response"));
            
            // 设置创建用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(createdByUserId).ifPresent(evaluation::setCreatedByUser);
            }
            
            // 设置创建变更日志
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                evaluation.setCreatedChangeLog(changeLog);
            }
            
            // 设置创建时间
            Timestamp creationTime = rs.getTimestamp("creation_time");
            if (creationTime != null) {
                evaluation.setCreationTime(creationTime.toLocalDateTime());
            }
            
            // 设置完成时间
            Timestamp completionTime = rs.getTimestamp("completion_time");
            if (completionTime != null) {
                evaluation.setCompletionTime(completionTime.toLocalDateTime());
            }
            
            // 设置原始分数、标准化分数、加权分数
            evaluation.setRawScore(rs.getBigDecimal("raw_score"));
            evaluation.setNormalizedScore(rs.getBigDecimal("normalized_score"));
            evaluation.setWeightedScore(rs.getBigDecimal("weighted_score"));
            
            // 设置分数类型和打分方法
            evaluation.setScoreType(rs.getString("score_type"));
            evaluation.setScoringMethod(rs.getString("scoring_method"));
            
            return evaluation;
        }
    }

    /**
     * 批量删除评测记录
     * 
     * @param evaluations 要删除的评测列表
     */
    public void deleteAll(List<Evaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return;
        }
        
        for (Evaluation evaluation : evaluations) {
            if (evaluation.getId() != null) {
                jdbcTemplate.update(SQL_DELETE, evaluation.getId());
            }
        }
    }
    
    /**
     * 刷新所有挂起的更改到数据库
     * 在JDBC中这个方法不需要实际操作，因为JDBC没有缓存机制
     * 但为了兼容性保留此方法
     */
    public void flush() {
        // JDBC没有缓存机制，所有操作都是立即执行的，无需额外刷新
        // 此方法仅为了兼容JPA接口而保留
    }
} 
