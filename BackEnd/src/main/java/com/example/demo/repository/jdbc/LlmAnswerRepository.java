package com.example.demo.repository.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的LLM回答仓库实现
 */
@Repository
public class LlmAnswerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String SQL_INSERT = 
            "INSERT INTO llm_answers (model_answer_run_id, dataset_question_mapping_id, answer_text, " +
            "generation_status, error_message, generation_time, prompt_used, raw_model_response, other_metadata, repeat_index) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE llm_answers SET model_answer_run_id=?, dataset_question_mapping_id=?, answer_text=?, " +
            "generation_status=?, error_message=?, generation_time=?, prompt_used=?, raw_model_response=?, other_metadata=?, repeat_index=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM llm_answers WHERE id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_ID_WITH_QUESTION = 
            "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id, " +
            "sq.question_text, sq.question_type " +
            "FROM llm_answers a " +
            "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "LEFT JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE a.id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_WITH_QUESTIONS = 
            "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id, " +
            "sq.question_text, sq.question_type " +
            "FROM llm_answers a " +
            "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "LEFT JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE a.model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_DATASET_QUESTION_MAPPING_ID = 
            "SELECT * FROM llm_answers WHERE dataset_question_mapping_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND dataset_question_mapping_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID_AND_REPEAT_INDEX = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND dataset_question_mapping_id=? AND repeat_index=?";
    
    private static final String SQL_COUNT_BY_RUN_ID = 
            "SELECT COUNT(*) FROM llm_answers WHERE model_answer_run_id=?";
    
    private static final String SQL_FIND_BY_BATCH_ID = 
            "SELECT a.* FROM llm_answers a " +
            "JOIN model_answer_runs mar ON a.model_answer_run_id = mar.id " +
            "WHERE mar.answer_generation_batch_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_ID_GREATER_THAN = 
            "SELECT * FROM llm_answers WHERE model_answer_run_id=? AND id>? ORDER BY id";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM llm_answers";

    private static final String SQL_FIND_BY_IDS = 
            "SELECT * FROM llm_answers WHERE id IN (%s)";

    private static final String SQL_FIND_OBJECTIVE_ANSWERS_WITH_EVALUATIONS = 
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, e.overall_score as score " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT') " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "ORDER BY sq.id, lm.id " +
            "LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_OBJECTIVE_ANSWERS_WITH_EVALUATIONS = 
            "SELECT COUNT(*) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT') " +
            "AND (? IS NULL OR lm.id IN (%s))";
            
    private static final String SQL_FIND_SUBJECTIVE_ANSWERS_WITH_EVALUATIONS = 
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, e.overall_score as score, " +
            "e.comments, e.evaluation_results " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id AND e.evaluator_id = ? " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "ORDER BY sq.id, lm.id " +
            "LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_SUBJECTIVE_ANSWERS_WITH_EVALUATIONS = 
            "SELECT COUNT(*) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' " +
            "AND (? IS NULL OR lm.id IN (%s))";
            
    private static final String SQL_FIND_PENDING_ANSWERS_FOR_HUMAN_EVALUATION = 
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, sq.difficulty as difficulty_level " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id AND e.evaluator_id IN (%s) " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND e.id IS NULL " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "AND (? IS NULL OR sq.question_type = ?) " +
            "ORDER BY sq.id, lm.id " +
            "LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_PENDING_ANSWERS_FOR_HUMAN_EVALUATION = 
            "SELECT COUNT(*) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id AND e.evaluator_id IN (%s) " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND e.id IS NULL " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "AND (? IS NULL OR sq.question_type = ?)";

    private static final String SQL_FIND_SUBJECTIVE_ANSWERS_WITH_ALL_EVALUATIONS = 
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, " +
            "e.id as evaluation_id, e.overall_score as score, e.comments, e.evaluation_results, " +
            "ev.id as evaluator_id, ev.name as evaluator_name, ev.evaluator_type as evaluator_type, " +
            "u.id as user_id, u.username as username " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id " +
            "LEFT JOIN evaluators ev ON e.evaluator_id = ev.id " +
            "LEFT JOIN users u ON e.created_by_user_id = u.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' " +
            "AND (? IS NULL OR lm.id IN (%s)) " +
            "ORDER BY sq.id, lm.id, ev.id " +
            "LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_SUBJECTIVE_ANSWERS_WITH_ALL_EVALUATIONS = 
            "SELECT COUNT(DISTINCT la.id) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' " +
            "AND (? IS NULL OR lm.id IN (%s))";

    @Autowired
    public LlmAnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 保存LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 带有ID的LLM回答对象
     */
    public LlmAnswer save(LlmAnswer llmAnswer) {
        if (llmAnswer.getId() == null) {
            return insert(llmAnswer);
        } else {
            return update(llmAnswer);
        }
    }

    /**
     * 插入新LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 带有ID的LLM回答对象
     */
    private LlmAnswer insert(LlmAnswer llmAnswer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置模型回答运行ID
            ps.setLong(1, llmAnswer.getModelAnswerRun().getId());
            
            // 设置数据集问题映射ID
            ps.setLong(2, llmAnswer.getDatasetQuestionMapping().getId());
            
            // 设置回答文本
            if (llmAnswer.getAnswerText() != null) {
                ps.setString(3, llmAnswer.getAnswerText());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            // 设置生成状态
            ps.setString(4, llmAnswer.getGenerationStatus().name());
            
            // 设置错误信息
            if (llmAnswer.getErrorMessage() != null) {
                ps.setString(5, llmAnswer.getErrorMessage());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            // 设置生成时间
            if (llmAnswer.getGenerationTime() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(llmAnswer.getGenerationTime()));
            } else {
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            }
            
            // 设置使用的提示词
            if (llmAnswer.getPromptUsed() != null) {
                ps.setString(7, llmAnswer.getPromptUsed());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            // 设置原始模型响应
            if (llmAnswer.getRawModelResponse() != null) {
                ps.setString(8, llmAnswer.getRawModelResponse());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            
            // 设置其他元数据
            if (llmAnswer.getOtherMetadata() != null) {
                ps.setString(9, llmAnswer.getOtherMetadata());
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置重复索引
            ps.setInt(10, llmAnswer.getRepeatIndex() != null ? llmAnswer.getRepeatIndex() : 0);
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            llmAnswer.setId(key.longValue());
        }
        return llmAnswer;
    }

    /**
     * 更新LLM回答
     *
     * @param llmAnswer LLM回答对象
     * @return 更新后的LLM回答对象
     */
    private LlmAnswer update(LlmAnswer llmAnswer) {
        jdbcTemplate.update(SQL_UPDATE,
                llmAnswer.getModelAnswerRun().getId(),
                llmAnswer.getDatasetQuestionMapping().getId(),
                llmAnswer.getAnswerText(),
                llmAnswer.getGenerationStatus().name(),
                llmAnswer.getErrorMessage(),
                llmAnswer.getGenerationTime() != null ? Timestamp.valueOf(llmAnswer.getGenerationTime()) : null,
                llmAnswer.getPromptUsed(),
                llmAnswer.getRawModelResponse(),
                llmAnswer.getOtherMetadata() != null ? llmAnswer.getOtherMetadata() : "{}",
                llmAnswer.getRepeatIndex(),
                llmAnswer.getId());

        return llmAnswer;
    }

    /**
     * 根据ID查找LLM回答
     *
     * @param id LLM回答ID
     * @return LLM回答的Optional包装
     */
    public Optional<LlmAnswer> findById(Long id) {
        try {
            LlmAnswer llmAnswer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new LlmAnswerRowMapper());
            return Optional.ofNullable(llmAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据ID查找LLM回答，同时预加载问题
     *
     * @param id 回答ID
     * @return 回答的Optional包装，包含预加载的问题
     */
    public Optional<LlmAnswer> findByIdWithQuestion(Long id) {
        System.out.println("执行findByIdWithQuestion查询，id=" + id + ", SQL=" + SQL_FIND_BY_ID_WITH_QUESTION);
        
        try {
            LlmAnswer llmAnswer = jdbcTemplate.queryForObject(SQL_FIND_BY_ID_WITH_QUESTION, new Object[]{id}, new LlmAnswerWithFullQuestionRowMapper());
            
            if (llmAnswer != null) {
                if (llmAnswer.getDatasetQuestionMapping() == null) {
                    System.out.println("回答ID: " + llmAnswer.getId() + " 的dataset_question_mapping为null");
                } else if (llmAnswer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                    System.out.println("回答ID: " + llmAnswer.getId() + " 的standard_question为null");
                }
            }
            
            return Optional.ofNullable(llmAnswer);
        } catch (EmptyResultDataAccessException e) {
            System.out.println("回答ID: " + id + " 未找到");
            return Optional.empty();
        }
    }
    
    /**
     * 根据运行ID查找回答
     *
     * @param modelAnswerRunId 运行ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunId(Long modelAnswerRunId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID,
                new Object[]{modelAnswerRunId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID查找回答，同时预加载问题
     *
     * @param modelAnswerRunId 运行ID
     * @return 回答列表，包含预加载的问题
     */
    public List<LlmAnswer> findByModelAnswerRunIdWithQuestions(Long modelAnswerRunId) {
        System.out.println("执行findByModelAnswerRunIdWithQuestions查询，modelAnswerRunId=" + modelAnswerRunId + ", SQL=" + SQL_FIND_BY_MODEL_ANSWER_RUN_ID_WITH_QUESTIONS);
        
        List<LlmAnswer> answers = jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_WITH_QUESTIONS,
                new Object[]{modelAnswerRunId},
                new LlmAnswerWithFullQuestionRowMapper()
        );
        
        // 记录找到的回答和它们的问题状态
        for (LlmAnswer answer : answers) {
            if (answer.getDatasetQuestionMapping() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的dataset_question_mapping为null");
            } else if (answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的standard_question为null");
            }
        }
        
        return answers;
    }
    
    /**
     * 根据数据集映射问题ID查找回答
     *
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByDatasetQuestionMappingId(Long datasetQuestionMappingId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_DATASET_QUESTION_MAPPING_ID,
                new Object[]{datasetQuestionMappingId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID和数据集映射问题ID查找回答
     *
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunIdAndDatasetQuestionMappingId(Long runId, Long datasetQuestionMappingId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID,
                new Object[]{runId, datasetQuestionMappingId},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 根据运行ID和数据集映射问题ID及重复索引查找回答
     *
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @param repeatIndex 重复索引
     * @return 回答
     */
    public LlmAnswer findByModelAnswerRunIdAndDatasetQuestionMappingIdAndRepeatIndex(
            Long runId, Long datasetQuestionMappingId, Integer repeatIndex) {
        try {
            return jdbcTemplate.queryForObject(
                    SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_DATASET_QUESTION_MAPPING_ID_AND_REPEAT_INDEX,
                    new Object[]{runId, datasetQuestionMappingId, repeatIndex},
                    new LlmAnswerRowMapper()
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 统计运行的已完成回答数量
     *
     * @param runId 运行ID
     * @return 回答数量
     */
    public int countByRunId(Long runId) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_COUNT_BY_RUN_ID,
                Integer.class,
                runId
        );
        return count != null ? count : 0;
    }
    
    /**
     * 按批次ID查找所有回答
     *
     * @param batchId 批次ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByBatchId(Long batchId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_BATCH_ID,
                new Object[]{batchId},
                new LlmAnswerRowMapper()
        );
    }

    /**
     * 按批次ID查找所有回答，同时预加载问题
     *
     * @param batchId 批次ID
     * @return 回答列表，包含预加载的问题
     */
    public List<LlmAnswer> findByBatchIdWithQuestions(Long batchId) {
        String sql = "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id, " +
                     "sq.question_text, sq.question_type " +
                     "FROM llm_answers a " +
                     "JOIN model_answer_runs mar ON a.model_answer_run_id = mar.id " +
                     "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
                     "LEFT JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
                     "WHERE mar.answer_generation_batch_id=?";
        
        System.out.println("执行findByBatchIdWithQuestions查询，batchId=" + batchId + ", SQL=" + sql);
        
        List<LlmAnswer> answers = jdbcTemplate.query(
                sql,
                new Object[]{batchId},
                new LlmAnswerWithFullQuestionRowMapper()
        );
        
        // 记录找到的回答和它们的问题状态
        for (LlmAnswer answer : answers) {
            if (answer.getDatasetQuestionMapping() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的dataset_question_mapping为null");
            } else if (answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的standard_question为null");
            }
        }
        
        return answers;
    }
    
    /**
     * 根据模型回答运行ID和回答ID查询大于指定ID的回答列表
     *
     * @param modelAnswerRunId 模型回答运行ID
     * @param id 回答ID
     * @return 回答列表
     */
    public List<LlmAnswer> findByModelAnswerRunIdAndIdGreaterThan(Long modelAnswerRunId, Long id) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_ID_GREATER_THAN,
                new Object[]{modelAnswerRunId, id},
                new LlmAnswerRowMapper()
        );
    }
    
    /**
     * 查找所有LLM回答
     *
     * @return LLM回答列表
     */
    public List<LlmAnswer> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new LlmAnswerRowMapper());
    }

    /**
     * 根据ID列表查找LLM回答
     *
     * @param ids LLM回答ID列表
     * @return LLM回答列表
     */
    public List<LlmAnswer> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        
        // 构建IN子句的占位符
        String placeholders = String.join(",", ids.stream()
                .map(id -> "?")
                .collect(Collectors.toList()));
        
        // 构建完整的SQL查询
        String sql = String.format(SQL_FIND_BY_IDS, placeholders);
        
        // 将ID列表转换为Object数组
        Object[] params = ids.toArray();
        
        return jdbcTemplate.query(sql, params, new LlmAnswerRowMapper());
    }

    /**
     * 根据ID列表查找LLM回答，同时预加载问题信息
     *
     * @param ids LLM回答ID列表
     * @return LLM回答列表，包含完整的问题信息
     */
    public List<LlmAnswer> findAllByIdWithQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        
        // 构建IN子句的占位符
        String placeholders = String.join(",", ids.stream()
                .map(id -> "?")
                .collect(Collectors.toList()));
        
        // 构建完整的SQL查询，使用LEFT JOIN标准问题表
        String sql = String.format(
            "SELECT a.*, dqm.id as dqm_id, dqm.standard_question_id as sq_id, " +
            "sq.question_text, sq.question_type " +
            "FROM llm_answers a " +
            "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "LEFT JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE a.id IN (%s)", placeholders);
        
        System.out.println("执行findAllByIdWithQuestions查询，ids=" + ids + ", SQL=" + sql);
        
        // 将ID列表转换为Object数组
        Object[] params = ids.toArray();
        
        List<LlmAnswer> answers = jdbcTemplate.query(sql, params, new LlmAnswerWithFullQuestionRowMapper());
        
        // 记录找到的回答和它们的问题状态
        for (LlmAnswer answer : answers) {
            if (answer.getDatasetQuestionMapping() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的dataset_question_mapping为null");
            } else if (answer.getDatasetQuestionMapping().getStandardQuestion() == null) {
                System.out.println("回答ID: " + answer.getId() + " 的standard_question为null");
            }
        }
        
        return answers;
    }

    /**
     * LLM回答行映射器
     */
    private class LlmAnswerRowMapper implements RowMapper<LlmAnswer> {
        @Override
        public LlmAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            LlmAnswer llmAnswer = new LlmAnswer();
            llmAnswer.setId(rs.getLong("id"));
            
            // 设置模型回答运行
            Long modelAnswerRunId = rs.getLong("model_answer_run_id");
            if (!rs.wasNull()) {
                ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
                modelAnswerRun.setId(modelAnswerRunId);
                llmAnswer.setModelAnswerRun(modelAnswerRun);
            }
            
            // 设置数据集问题映射
            Long datasetQuestionMappingId = rs.getLong("dataset_question_mapping_id");
            if (!rs.wasNull()) {
                DatasetQuestionMapping datasetQuestionMapping = new DatasetQuestionMapping();
                datasetQuestionMapping.setId(datasetQuestionMappingId);
                llmAnswer.setDatasetQuestionMapping(datasetQuestionMapping);
            }
            
            // 设置回答文本
            llmAnswer.setAnswerText(rs.getString("answer_text"));
            
            // 设置生成状态
            String generationStatusStr = rs.getString("generation_status");
            if (generationStatusStr != null) {
                llmAnswer.setGenerationStatus(LlmAnswer.GenerationStatus.valueOf(generationStatusStr));
            }
            
            // 设置错误信息
            llmAnswer.setErrorMessage(rs.getString("error_message"));
            
            // 设置生成时间
            Timestamp generationTime = rs.getTimestamp("generation_time");
            if (generationTime != null) {
                llmAnswer.setGenerationTime(generationTime.toLocalDateTime());
            }
            
            // 设置使用的提示词
            llmAnswer.setPromptUsed(rs.getString("prompt_used"));
            
            // 设置原始模型响应
            llmAnswer.setRawModelResponse(rs.getString("raw_model_response"));
            
            // 设置其他元数据
            llmAnswer.setOtherMetadata(rs.getString("other_metadata"));
            
            // 设置重复索引
            llmAnswer.setRepeatIndex(rs.getInt("repeat_index"));
            
            return llmAnswer;
        }
    }
    
    /**
     * 带问题的LLM回答行映射器
     */
    private class LlmAnswerWithQuestionRowMapper extends LlmAnswerRowMapper {
        @Override
        public LlmAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 首先获取基本的LLM回答对象
            LlmAnswer llmAnswer = super.mapRow(rs, rowNum);
            
            // 进一步填充问题相关信息
            try {
                Long dqmId = rs.getLong("dqm_id");
                Long sqId = rs.getLong("sq_id");
                
                if (!rs.wasNull()) {
                    // 这里仅设置ID，实际使用时可能需要加载完整的StandardQuestion对象
                    DatasetQuestionMapping dqm = new DatasetQuestionMapping();
                    dqm.setId(dqmId);
                    llmAnswer.setDatasetQuestionMapping(dqm);
                }
            } catch (SQLException e) {
                // 如果查询中没有这些列，则忽略异常
            }
            
            return llmAnswer;
        }
    }
    
    /**
     * 带完整问题信息的LLM回答行映射器
     */
    private class LlmAnswerWithFullQuestionRowMapper extends LlmAnswerRowMapper {
        @Override
        public LlmAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 首先获取基本的LLM回答对象
            LlmAnswer llmAnswer = super.mapRow(rs, rowNum);
            
            try {
                // 检查dqm_id是否为空
                Long dqmId = rs.getLong("dqm_id");
                if (rs.wasNull()) {
                    System.out.println("回答ID: " + llmAnswer.getId() + ", dqm_id为空");
                    return llmAnswer; // 如果dataset_question_mapping_id为空，直接返回
                }
                
                // 检查sq_id是否为空
                Long sqId = rs.getLong("sq_id");
                if (rs.wasNull()) {
                    System.out.println("回答ID: " + llmAnswer.getId() + ", sq_id为空");
                    // 创建DatasetQuestionMapping，但不包含StandardQuestion
                    DatasetQuestionMapping dqm = new DatasetQuestionMapping();
                    dqm.setId(dqmId);
                    llmAnswer.setDatasetQuestionMapping(dqm);
                    return llmAnswer;
                }
                
                // 都不为空时，创建并填充完整的对象
                DatasetQuestionMapping dqm = new DatasetQuestionMapping();
                dqm.setId(dqmId);
                
                // 创建并填充StandardQuestion对象
                com.example.demo.entity.jdbc.StandardQuestion sq = new com.example.demo.entity.jdbc.StandardQuestion();
                sq.setId(sqId);
                
                // 获取问题文本
                String questionText = rs.getString("question_text");
                if (questionText != null) {
                    sq.setQuestionText(questionText);
                } else {
                    System.out.println("回答ID: " + llmAnswer.getId() + ", question_text为空");
                }
                
                // 处理问题类型
                String questionTypeStr = rs.getString("question_type");
                if (questionTypeStr != null) {
                    try {
                        sq.setQuestionType(com.example.demo.entity.jdbc.QuestionType.valueOf(questionTypeStr));
                    } catch (IllegalArgumentException e) {
                        System.out.println("回答ID: " + llmAnswer.getId() + ", 问题类型转换错误: " + questionTypeStr);
                        // 如果问题类型无效，设置为默认类型
                        sq.setQuestionType(com.example.demo.entity.jdbc.QuestionType.SUBJECTIVE);
                    }
                } else {
                    System.out.println("回答ID: " + llmAnswer.getId() + ", question_type为空");
                    sq.setQuestionType(com.example.demo.entity.jdbc.QuestionType.SUBJECTIVE);
                }
                
                // 将StandardQuestion设置到DatasetQuestionMapping中
                dqm.setStandardQuestion(sq);
                
                // 将DatasetQuestionMapping设置到LlmAnswer中
                llmAnswer.setDatasetQuestionMapping(dqm);
                
            } catch (SQLException e) {
                // 如果查询中没有这些列，记录异常但不中断处理
                System.err.println("回答ID: " + llmAnswer.getId() + ", 加载完整问题信息时出错: " + e.getMessage());
            }
            
            return llmAnswer;
        }
    }

    /**
     * 查找数据关联不完整的回答记录
     * 
     * @return 关联不完整的回答列表
     */
    public List<Map<String, Object>> findIncompleteAnswers() {
        String sql = 
            "SELECT a.id, a.model_answer_run_id, a.dataset_question_mapping_id, " +
            "a.answer_text, a.generation_status, " +
            "EXISTS(SELECT 1 FROM dataset_question_mapping dqm WHERE dqm.id = a.dataset_question_mapping_id) as dqm_exists, " +
            "dqm.standard_question_id, " +
            "EXISTS(SELECT 1 FROM standard_questions sq WHERE sq.id = dqm.standard_question_id) as sq_exists " +
            "FROM llm_answers a " +
            "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "WHERE a.dataset_question_mapping_id IS NULL " +
            "   OR dqm.id IS NULL " +
            "   OR dqm.standard_question_id IS NULL " +
            "   OR NOT EXISTS(SELECT 1 FROM standard_questions sq WHERE sq.id = dqm.standard_question_id)";
            
        System.out.println("执行findIncompleteAnswers查询，SQL=" + sql);
        
        return jdbcTemplate.queryForList(sql);
    }
    
    /**
     * 查找指定ID的回答记录的关联情况
     * 
     * @param answerId 回答ID
     * @return 关联信息
     */
    public Map<String, Object> checkAnswerRelations(Long answerId) {
        String sql = 
            "SELECT a.id, a.model_answer_run_id, a.dataset_question_mapping_id, " +
            "a.answer_text, a.generation_status, " +
            "EXISTS(SELECT 1 FROM dataset_question_mapping dqm WHERE dqm.id = a.dataset_question_mapping_id) as dqm_exists, " +
            "dqm.standard_question_id, " +
            "EXISTS(SELECT 1 FROM standard_questions sq WHERE sq.id = dqm.standard_question_id) as sq_exists " +
            "FROM llm_answers a " +
            "LEFT JOIN dataset_question_mapping dqm ON a.dataset_question_mapping_id = dqm.id " +
            "WHERE a.id = ?";
            
        System.out.println("执行checkAnswerRelations查询，answerId=" + answerId + ", SQL=" + sql);
        
        try {
            return jdbcTemplate.queryForMap(sql, answerId);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 获取客观题回答及其评测结果
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param pageable 分页信息
     * @return 包含回答和评测信息的Map列表
     */
    public List<Map<String, Object>> findObjectiveAnswersWithEvaluations(Long batchId, List<Long> modelIds, org.springframework.data.domain.Pageable pageable) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, e.overall_score as score " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT') "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        sqlBuilder.append("ORDER BY sq.id, lm.id LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("answerId", rs.getLong("answer_id"));
            result.put("answerText", rs.getString("answer_text"));
            result.put("runId", rs.getLong("run_id"));
            result.put("modelId", rs.getLong("model_id"));
            result.put("modelName", rs.getString("model_name"));
            result.put("questionId", rs.getLong("question_id"));
            result.put("questionText", rs.getString("question_text"));
            result.put("questionType", rs.getString("question_type"));
            result.put("score", rs.getBigDecimal("score"));
            return result;
        });
    }
    
    /**
     * 统计客观题回答及其评测结果的总数
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @return 总记录数
     */
    public long countObjectiveAnswersWithEvaluations(Long batchId, List<Long> modelIds) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SIMPLE_FACT') "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.queryForObject(sql, params.toArray(), Long.class);
    }
    
    /**
     * 获取主观题回答及其评测结果
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param evaluatorId 评测者ID
     * @param pageable 分页信息
     * @return 包含回答和评测信息的Map列表
     */
    public List<Map<String, Object>> findSubjectiveAnswersWithEvaluations(Long batchId, List<Long> modelIds, Long evaluatorId, org.springframework.data.domain.Pageable pageable) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, e.overall_score as score, " +
            "e.comments, e.evaluation_results " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id AND e.evaluator_id = ? " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(evaluatorId);
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        sqlBuilder.append("ORDER BY sq.id, lm.id LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("answerId", rs.getLong("answer_id"));
            result.put("answerText", rs.getString("answer_text"));
            result.put("runId", rs.getLong("run_id"));
            result.put("modelId", rs.getLong("model_id"));
            result.put("modelName", rs.getString("model_name"));
            result.put("questionId", rs.getLong("question_id"));
            result.put("questionText", rs.getString("question_text"));
            result.put("questionType", rs.getString("question_type"));
            result.put("score", rs.getBigDecimal("score"));
            result.put("comments", rs.getString("comments"));
            
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
     * 统计主观题回答及其评测结果的总数
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param evaluatorId 评测者ID
     * @return 总记录数
     */
    public long countSubjectiveAnswersWithEvaluations(Long batchId, List<Long> modelIds, Long evaluatorId) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.queryForObject(sql, params.toArray(), Long.class);
    }
    
    /**
     * 获取待人工评测的回答
     *
     * @param evaluatorIds 评测者ID列表
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param questionType 问题类型，如果为null则查询所有类型
     * @param pageable 分页信息
     * @return 包含待评测回答信息的Map列表
     */
    public List<Map<String, Object>> findPendingAnswersForHumanEvaluation(
            List<Long> evaluatorIds, Long batchId, List<Long> modelIds, 
            com.example.demo.entity.jdbc.QuestionType questionType, 
            org.springframework.data.domain.Pageable pageable) {
        
        String evaluatorInClause = String.join(",", Collections.nCopies(evaluatorIds.size(), "?"));
        String sql;
        Object[] params;
        
        if (modelIds != null && !modelIds.isEmpty()) {
            // 有模型ID列表，使用 IN 子句
            String modelInClause = String.join(",", Collections.nCopies(modelIds.size(), "?"));
            sql = String.format(SQL_FIND_PENDING_ANSWERS_FOR_HUMAN_EVALUATION, evaluatorInClause, modelInClause);
            
            params = new Object[evaluatorIds.size() + modelIds.size() + 6];
            
            int paramIndex = 0;
            for (Long evaluatorId : evaluatorIds) {
                params[paramIndex++] = evaluatorId;
            }
            
            params[paramIndex++] = batchId;
            params[paramIndex++] = 1; // 非空标志
            
            for (Long modelId : modelIds) {
                params[paramIndex++] = modelId;
            }
            
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
            params[paramIndex++] = pageable.getPageSize();
            params[paramIndex++] = pageable.getOffset();
        } else {
            // 没有模型ID列表，不使用 IN 子句
            sql = String.format(SQL_FIND_PENDING_ANSWERS_FOR_HUMAN_EVALUATION, evaluatorInClause, "");
            
            params = new Object[evaluatorIds.size() + 5];
            
            int paramIndex = 0;
            for (Long evaluatorId : evaluatorIds) {
                params[paramIndex++] = evaluatorId;
            }
            
            params[paramIndex++] = batchId;
            params[paramIndex++] = null;
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
            params[paramIndex++] = pageable.getPageSize();
            params[paramIndex++] = pageable.getOffset();
        }
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("answerId", rs.getLong("answer_id"));
            result.put("answerText", rs.getString("answer_text"));
            result.put("runId", rs.getLong("run_id"));
            result.put("modelId", rs.getLong("model_id"));
            result.put("modelName", rs.getString("model_name"));
            result.put("questionId", rs.getLong("question_id"));
            result.put("questionText", rs.getString("question_text"));
            result.put("questionType", rs.getString("question_type"));
            result.put("difficultyLevel", rs.getString("difficulty_level"));
            return result;
        });
    }
    
    /**
     * 统计待人工评测的回答总数
     *
     * @param evaluatorIds 评测者ID列表
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param questionType 问题类型，如果为null则查询所有类型
     * @return 总记录数
     */
    public long countPendingAnswersForHumanEvaluation(
            List<Long> evaluatorIds, Long batchId, List<Long> modelIds, 
            com.example.demo.entity.jdbc.QuestionType questionType) {
        
        String evaluatorInClause = String.join(",", Collections.nCopies(evaluatorIds.size(), "?"));
        String sql;
        Object[] params;
        
        if (modelIds != null && !modelIds.isEmpty()) {
            // 有模型ID列表，使用 IN 子句
            String modelInClause = String.join(",", Collections.nCopies(modelIds.size(), "?"));
            sql = String.format(SQL_COUNT_PENDING_ANSWERS_FOR_HUMAN_EVALUATION, evaluatorInClause, modelInClause);
            
            params = new Object[evaluatorIds.size() + modelIds.size() + 4];
            
            int paramIndex = 0;
            for (Long evaluatorId : evaluatorIds) {
                params[paramIndex++] = evaluatorId;
            }
            
            params[paramIndex++] = batchId;
            params[paramIndex++] = 1; // 非空标志
            
            for (Long modelId : modelIds) {
                params[paramIndex++] = modelId;
            }
            
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
        } else {
            // 没有模型ID列表，不使用 IN 子句
            sql = String.format(SQL_COUNT_PENDING_ANSWERS_FOR_HUMAN_EVALUATION, evaluatorInClause, "");
            
            params = new Object[evaluatorIds.size() + 3];
            
            int paramIndex = 0;
            for (Long evaluatorId : evaluatorIds) {
                params[paramIndex++] = evaluatorId;
            }
            
            params[paramIndex++] = batchId;
            params[paramIndex++] = null;
            params[paramIndex++] = questionType == null ? null : 1; // 非空标志
            params[paramIndex++] = questionType == null ? null : questionType.name();
        }
        
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    /**
     * 获取主观题回答及其所有评测员的评测结果
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @param pageable 分页信息
     * @return 包含回答和所有评测员评测信息的Map列表
     */
    public List<Map<String, Object>> findSubjectiveAnswersWithAllEvaluations(Long batchId, List<Long> modelIds, org.springframework.data.domain.Pageable pageable) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT la.id as answer_id, la.answer_text, mar.id as run_id, lm.id as model_id, lm.name as model_name, " +
            "sq.id as question_id, sq.question_text, sq.question_type, " +
            "e.id as evaluation_id, e.overall_score as score, e.comments, e.evaluation_results, " +
            "ev.id as evaluator_id, ev.name as evaluator_name, ev.evaluator_type as evaluator_type, " +
            "u.id as user_id, u.username as username " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "LEFT JOIN evaluations e ON la.id = e.llm_answer_id " +
            "LEFT JOIN evaluators ev ON e.evaluator_id = ev.id " +
            "LEFT JOIN users u ON e.created_by_user_id = u.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        sqlBuilder.append("ORDER BY sq.id, lm.id, ev.id LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        
        String sql = sqlBuilder.toString();
        
        // 使用Map来按答案ID分组
        Map<Long, Map<String, Object>> answerMap = new HashMap<>();
        
        jdbcTemplate.query(sql, params.toArray(), (rs) -> {
            Long answerId = rs.getLong("answer_id");
            
            // 如果这个答案还没有处理过，创建基础信息
            if (!answerMap.containsKey(answerId)) {
                Map<String, Object> answer = new HashMap<>();
                answer.put("answerId", answerId);
                answer.put("answerText", rs.getString("answer_text"));
                answer.put("runId", rs.getLong("run_id"));
                answer.put("modelId", rs.getLong("model_id"));
                answer.put("modelName", rs.getString("model_name"));
                answer.put("questionId", rs.getLong("question_id"));
                answer.put("questionText", rs.getString("question_text"));
                answer.put("questionType", rs.getString("question_type"));
                answer.put("evaluations", new ArrayList<Map<String, Object>>());
                answerMap.put(answerId, answer);
            }
            
            // 获取当前答案的评测列表
            Map<String, Object> currentAnswer = answerMap.get(answerId);
            List<Map<String, Object>> evaluations = (List<Map<String, Object>>) currentAnswer.get("evaluations");
            
            // 如果有评测结果，添加到评测列表中
            Long evaluationId = rs.getLong("evaluation_id");
            if (!rs.wasNull()) {
                Map<String, Object> evaluation = new HashMap<>();
                evaluation.put("evaluationId", evaluationId);
                evaluation.put("score", rs.getBigDecimal("score"));
                evaluation.put("comments", rs.getString("comments"));
                evaluation.put("evaluatorId", rs.getLong("evaluator_id"));
                evaluation.put("evaluatorName", rs.getString("evaluator_name"));
                evaluation.put("evaluatorType", rs.getString("evaluator_type"));
                evaluation.put("userId", rs.getLong("user_id"));
                evaluation.put("username", rs.getString("username"));
                
                String evaluationResults = rs.getString("evaluation_results");
                if (evaluationResults != null) {
                    try {
                        evaluation.put("evaluationResults", objectMapper.readValue(evaluationResults, Map.class));
                    } catch (Exception e) {
                        evaluation.put("evaluationResults", new HashMap<>());
                    }
                } else {
                    evaluation.put("evaluationResults", new HashMap<>());
                }
                
                evaluations.add(evaluation);
            }
        });
        
        // 将Map转换为List返回
        return new ArrayList<>(answerMap.values());
    }
    
    /**
     * 统计主观题回答的总数（用于获取所有评测员的评测结果）
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表，如果为null则查询所有模型
     * @return 总记录数
     */
    public long countSubjectiveAnswersWithAllEvaluations(Long batchId, List<Long> modelIds) {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT COUNT(DISTINCT la.id) " +
            "FROM llm_answers la " +
            "JOIN model_answer_runs mar ON la.model_answer_run_id = mar.id " +
            "JOIN llm_models lm ON mar.llm_model_id = lm.id " +
            "JOIN dataset_question_mapping dqm ON la.dataset_question_mapping_id = dqm.id " +
            "JOIN standard_questions sq ON dqm.standard_question_id = sq.id " +
            "WHERE mar.answer_generation_batch_id = ? " +
            "AND sq.question_type = 'SUBJECTIVE' "
        );
        
        List<Object> params = new ArrayList<>();
        params.add(batchId);
        
        // 只有在有模型ID列表且不为空时才添加 IN 子句
        if (modelIds != null && !modelIds.isEmpty()) {
            sqlBuilder.append("AND lm.id IN (");
            for (int i = 0; i < modelIds.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append("?");
                params.add(modelIds.get(i));
            }
            sqlBuilder.append(") ");
        }
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.queryForObject(sql, params.toArray(), Long.class);
    }
} 
