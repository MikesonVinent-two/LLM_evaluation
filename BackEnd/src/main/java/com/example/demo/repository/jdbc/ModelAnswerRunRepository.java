package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.AnswerGenerationBatch;
import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.example.demo.entity.jdbc.ModelAnswerRun.RunStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于JDBC的模型回答运行仓库实?
 */
@Repository
public class ModelAnswerRunRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LlmModelRepository LlmModelRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO model_answer_runs (answer_generation_batch_id, llm_model_id, run_name, run_description, " +
            "run_index, status, run_time, error_message, parameters, last_processed_question_id, " +
            "last_processed_question_index, progress_percentage, last_activity_time, resume_count, " +
            "pause_time, pause_reason, total_questions_count, completed_questions_count, failed_questions_count, failed_questions_ids) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE model_answer_runs SET answer_generation_batch_id=?, llm_model_id=?, run_name=?, run_description=?, " +
            "run_index=?, status=?, run_time=?, error_message=?, parameters=?, last_processed_question_id=?, " +
            "last_processed_question_index=?, progress_percentage=?, last_activity_time=?, resume_count=?, " +
            "pause_time=?, pause_reason=?, total_questions_count=?, completed_questions_count=?, " +
            "failed_questions_count=?, failed_questions_ids=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM model_answer_runs WHERE id=?";
    
    private static final String SQL_FIND_BY_BATCH_ID = 
            "SELECT * FROM model_answer_runs WHERE answer_generation_batch_id=?";
    
    private static final String SQL_FIND_BY_MODEL_ID = 
            "SELECT * FROM model_answer_runs WHERE llm_model_id=?";
    
    private static final String SQL_FIND_BY_STATUS = 
            "SELECT * FROM model_answer_runs WHERE status=?";
    
    private static final String SQL_FIND_BY_BATCH_AND_MODEL = 
            "SELECT * FROM model_answer_runs WHERE answer_generation_batch_id=? AND llm_model_id=?";
    
    private static final String SQL_FIND_BY_BATCH_AND_STATUS = 
            "SELECT * FROM model_answer_runs WHERE answer_generation_batch_id=? AND status=?";
    
    private static final String SQL_COUNT_BY_BATCH_AND_STATUS = 
            "SELECT COUNT(*) FROM model_answer_runs WHERE answer_generation_batch_id=? AND status=?";
    
    private static final String SQL_FIND_BY_BATCH_MODEL_AND_RUN_INDEX = 
            "SELECT * FROM model_answer_runs WHERE answer_generation_batch_id=? AND llm_model_id=? AND run_index=?";
    
    private static final String SQL_FIND_BY_USER_ID = 
            "SELECT r.* FROM model_answer_runs r " +
            "JOIN answer_generation_batches b ON r.answer_generation_batch_id = b.id " +
            "WHERE b.created_by_user_id = ? " +
            "ORDER BY r.run_time DESC";

    @Autowired
    public ModelAnswerRunRepository(JdbcTemplate jdbcTemplate, LlmModelRepository LlmModelRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.LlmModelRepository = LlmModelRepository;
    }

    /**
     * 保存模型回答运行
     *
     * @param modelAnswerRun 模型回答运行对象
     * @return 带有ID的模型回答运行对?
     */
    public ModelAnswerRun save(ModelAnswerRun modelAnswerRun) {
        if (modelAnswerRun.getId() == null) {
            return insert(modelAnswerRun);
        } else {
            return update(modelAnswerRun);
        }
    }

    /**
     * 保存模型回答运行并立即刷新事务
     *
     * @param modelAnswerRun 模型回答运行对象
     * @return 带有ID的模型回答运行对象
     */
    public ModelAnswerRun saveAndFlush(ModelAnswerRun modelAnswerRun) {
        // 在JDBC实现中，save操作已经立即执行到数据库，无需额外刷新
        return save(modelAnswerRun);
    }

    /**
     * 插入新模型回答运行
     *
     * @param modelAnswerRun 模型回答运行对象
     * @return 带有ID的模型回答运行对象
     */
    private ModelAnswerRun insert(ModelAnswerRun modelAnswerRun) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置批次ID
            if (modelAnswerRun.getAnswerGenerationBatch() != null && modelAnswerRun.getAnswerGenerationBatch().getId() != null) {
                ps.setLong(1, modelAnswerRun.getAnswerGenerationBatch().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置模型ID
            if (modelAnswerRun.getLlmModel() != null && modelAnswerRun.getLlmModel().getId() != null) {
                ps.setLong(2, modelAnswerRun.getLlmModel().getId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            
            // 设置运行名称
            ps.setString(3, modelAnswerRun.getRunName() != null ? 
                    modelAnswerRun.getRunName() : "Model Answer Run");
            
            // 设置运行描述
            if (modelAnswerRun.getRunDescription() != null) {
                ps.setString(4, modelAnswerRun.getRunDescription());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // 设置运行索引
            if (modelAnswerRun.getRunIndex() != null) {
                ps.setInt(5, modelAnswerRun.getRunIndex());
            } else {
                ps.setInt(5, 0); // 默认?，表示第一次运?
            }
            
            // 设置状?
            ps.setString(6, modelAnswerRun.getStatus().name());
            
            // 设置开始时?
            if (modelAnswerRun.getRunTime() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(modelAnswerRun.getRunTime()));
            } else {
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置错误消息
            if (modelAnswerRun.getErrorMessage() != null) {
                ps.setString(8, modelAnswerRun.getErrorMessage());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            
            // 设置配置
            if (modelAnswerRun.getParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(9, objectMapper.writeValueAsString(modelAnswerRun.getParameters()));
                } catch (Exception e) {
                    ps.setString(9, "{}");
                }
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置上次处理的问题ID
            if (modelAnswerRun.getLastProcessedQuestionId() != null) {
                ps.setLong(10, modelAnswerRun.getLastProcessedQuestionId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置上次处理的问题索?
            if (modelAnswerRun.getLastProcessedQuestionIndex() != null) {
                ps.setInt(11, modelAnswerRun.getLastProcessedQuestionIndex());
            } else {
                ps.setNull(11, Types.INTEGER);
            }
            
            // 设置进度百分?
            if (modelAnswerRun.getProgressPercentage() != null) {
                ps.setBigDecimal(12, modelAnswerRun.getProgressPercentage());
            } else {
                ps.setNull(12, Types.DECIMAL);
            }
            
            // 设置最后活动时?
            if (modelAnswerRun.getLastActivityTime() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(modelAnswerRun.getLastActivityTime()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (modelAnswerRun.getResumeCount() != null) {
                ps.setInt(14, modelAnswerRun.getResumeCount());
            } else {
                ps.setInt(14, 0);
            }
            
            // 设置暂停时间
            if (modelAnswerRun.getPauseTime() != null) {
                ps.setTimestamp(15, Timestamp.valueOf(modelAnswerRun.getPauseTime()));
            } else {
                ps.setNull(15, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (modelAnswerRun.getPauseReason() != null) {
                ps.setString(16, modelAnswerRun.getPauseReason());
            } else {
                ps.setNull(16, Types.VARCHAR);
            }
            
            // 设置总问题数
            if (modelAnswerRun.getTotalQuestionsCount() != null) {
                ps.setInt(17, modelAnswerRun.getTotalQuestionsCount());
            } else {
                ps.setNull(17, Types.INTEGER);
            }
            
            // 设置完成数量
            if (modelAnswerRun.getCompletedQuestionsCount() != null) {
                ps.setInt(18, modelAnswerRun.getCompletedQuestionsCount());
            } else {
                ps.setNull(18, Types.INTEGER);
            }
            
            // 设置失败数量
            if (modelAnswerRun.getFailedQuestionsCount() != null) {
                ps.setInt(19, modelAnswerRun.getFailedQuestionsCount());
            } else {
                ps.setNull(19, Types.INTEGER);
            }
            
            // 设置失败问题ID列表
            if (modelAnswerRun.getFailedQuestionsIds() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(20, objectMapper.writeValueAsString(modelAnswerRun.getFailedQuestionsIds()));
                } catch (Exception e) {
                    ps.setString(20, "[]");
                }
            } else {
                ps.setString(20, "[]");
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            modelAnswerRun.setId(key.longValue());
        }

        return modelAnswerRun;
    }

    /**
     * 更新模型回答运行
     *
     * @param modelAnswerRun 模型回答运行对象
     * @return 更新后的模型回答运行对象
     */
    private ModelAnswerRun update(ModelAnswerRun modelAnswerRun) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置批次ID
            if (modelAnswerRun.getAnswerGenerationBatch() != null && modelAnswerRun.getAnswerGenerationBatch().getId() != null) {
                ps.setLong(1, modelAnswerRun.getAnswerGenerationBatch().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置模型ID
            if (modelAnswerRun.getLlmModel() != null && modelAnswerRun.getLlmModel().getId() != null) {
                ps.setLong(2, modelAnswerRun.getLlmModel().getId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            
            // 设置运行名称
            ps.setString(3, modelAnswerRun.getRunName() != null ? 
                    modelAnswerRun.getRunName() : "Model Answer Run");
            
            // 设置运行描述
            if (modelAnswerRun.getRunDescription() != null) {
                ps.setString(4, modelAnswerRun.getRunDescription());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // 设置运行索引
            if (modelAnswerRun.getRunIndex() != null) {
                ps.setInt(5, modelAnswerRun.getRunIndex());
            } else {
                ps.setInt(5, 0); // 默认?，表示第一次运?
            }
            
            // 设置状?
            ps.setString(6, modelAnswerRun.getStatus().name());
            
            // 设置运行时间
            if (modelAnswerRun.getRunTime() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(modelAnswerRun.getRunTime()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            
            // 设置错误消息
            if (modelAnswerRun.getErrorMessage() != null) {
                ps.setString(8, modelAnswerRun.getErrorMessage());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            
            // 设置配置
            if (modelAnswerRun.getParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(9, objectMapper.writeValueAsString(modelAnswerRun.getParameters()));
                } catch (Exception e) {
                    ps.setString(9, "{}");
                }
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置上次处理的问题ID
            if (modelAnswerRun.getLastProcessedQuestionId() != null) {
                ps.setLong(10, modelAnswerRun.getLastProcessedQuestionId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置上次处理的问题索?
            if (modelAnswerRun.getLastProcessedQuestionIndex() != null) {
                ps.setInt(11, modelAnswerRun.getLastProcessedQuestionIndex());
            } else {
                ps.setNull(11, Types.INTEGER);
            }
            
            // 设置进度百分?
            if (modelAnswerRun.getProgressPercentage() != null) {
                ps.setBigDecimal(12, modelAnswerRun.getProgressPercentage());
            } else {
                ps.setNull(12, Types.DECIMAL);
            }
            
            // 设置最后活动时?
            if (modelAnswerRun.getLastActivityTime() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(modelAnswerRun.getLastActivityTime()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (modelAnswerRun.getResumeCount() != null) {
                ps.setInt(14, modelAnswerRun.getResumeCount());
            } else {
                ps.setInt(14, 0);
            }
            
            // 设置暂停时间
            if (modelAnswerRun.getPauseTime() != null) {
                ps.setTimestamp(15, Timestamp.valueOf(modelAnswerRun.getPauseTime()));
            } else {
                ps.setNull(15, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (modelAnswerRun.getPauseReason() != null) {
                ps.setString(16, modelAnswerRun.getPauseReason());
            } else {
                ps.setNull(16, Types.VARCHAR);
            }
            
            // 设置总问题数
            if (modelAnswerRun.getTotalQuestionsCount() != null) {
                ps.setInt(17, modelAnswerRun.getTotalQuestionsCount());
            } else {
                ps.setNull(17, Types.INTEGER);
            }
            
            // 设置完成数量
            if (modelAnswerRun.getCompletedQuestionsCount() != null) {
                ps.setInt(18, modelAnswerRun.getCompletedQuestionsCount());
            } else {
                ps.setNull(18, Types.INTEGER);
            }
            
            // 设置失败数量
            if (modelAnswerRun.getFailedQuestionsCount() != null) {
                ps.setInt(19, modelAnswerRun.getFailedQuestionsCount());
            } else {
                ps.setNull(19, Types.INTEGER);
            }
            
            // 设置失败问题ID列表
            if (modelAnswerRun.getFailedQuestionsIds() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(20, objectMapper.writeValueAsString(modelAnswerRun.getFailedQuestionsIds()));
                } catch (Exception e) {
                    ps.setString(20, "[]");
                }
            } else {
                ps.setString(20, "[]");
            }
            
            // 设置ID
            ps.setLong(21, modelAnswerRun.getId());
            
            return ps;
        });

        return modelAnswerRun;
    }

    /**
     * 根据ID查找模型回答运行
     *
     * @param id 模型回答运行ID
     * @return 模型回答运行对象
     */
    public Optional<ModelAnswerRun> findById(Long id) {
        try {
            ModelAnswerRun modelAnswerRun = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new ModelAnswerRunRowMapper(), id);
            return Optional.ofNullable(modelAnswerRun);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据批次ID查找运行
     * 
     * @param batchId 批次ID
     * @return 该批次的所有运?
     */
    public List<ModelAnswerRun> findByAnswerGenerationBatchId(Long batchId) {
        return jdbcTemplate.query(SQL_FIND_BY_BATCH_ID, new ModelAnswerRunRowMapper(), batchId);
    }
    
    /**
     * 根据模型ID查找运行
     * 
     * @param modelId 模型ID
     * @return 该模型的所有运?
     */
    public List<ModelAnswerRun> findByLlmModelId(Long modelId) {
        return jdbcTemplate.query(SQL_FIND_BY_MODEL_ID, new ModelAnswerRunRowMapper(), modelId);
    }
    
    /**
     * 根据状态查找运?
     * 
     * @param status 运行状?
     * @return 指定状态的所有运?
     */
    public List<ModelAnswerRun> findByStatus(RunStatus status) {
        return jdbcTemplate.query(SQL_FIND_BY_STATUS, new ModelAnswerRunRowMapper(), status.name());
    }
    
    /**
     * 根据批次ID和模型ID查找运行
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 匹配的运行列?
     */
    public List<ModelAnswerRun> findByAnswerGenerationBatchIdAndLlmModelId(Long batchId, Long modelId) {
        return jdbcTemplate.query(SQL_FIND_BY_BATCH_AND_MODEL, new ModelAnswerRunRowMapper(), batchId, modelId);
    }
    
    /**
     * 根据批次ID和状态查找运?
     * 
     * @param batchId 批次ID
     * @param status 运行状?
     * @return 匹配的运行列?
     */
    public List<ModelAnswerRun> findByAnswerGenerationBatchIdAndStatus(Long batchId, RunStatus status) {
        return jdbcTemplate.query(SQL_FIND_BY_BATCH_AND_STATUS, new ModelAnswerRunRowMapper(), batchId, status.name());
    }
    
    /**
     * 统计批次中各状态的运行数量
     * 
     * @param batchId 批次ID
     * @param status 运行状?
     * @return 符合条件的运行数?
     */
    public long countByAnswerGenerationBatchIdAndStatus(Long batchId, RunStatus status) {
        return jdbcTemplate.queryForObject(SQL_COUNT_BY_BATCH_AND_STATUS, Long.class, batchId, status.name());
    }
    
    /**
     * 根据批次ID、模型ID和运行索引查找运?
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @param runIndex 运行索引
     * @return 匹配的运?
     */
    public ModelAnswerRun findByBatchModelAndRunIndex(Long batchId, Long modelId, Integer runIndex) {
        try {
            return jdbcTemplate.queryForObject(
                SQL_FIND_BY_BATCH_MODEL_AND_RUN_INDEX, 
                new ModelAnswerRunRowMapper(), 
                batchId, modelId, runIndex
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 根据用户ID查找运行
     * 
     * @param userId 用户ID
     * @return 该用户创建的批次的所有运?
     */
    public List<ModelAnswerRun> findByUserId(Long userId) {
        return jdbcTemplate.query(SQL_FIND_BY_USER_ID, new ModelAnswerRunRowMapper(), userId);
    }

    /**
     * 删除模型回答运行
     *
     * @param modelAnswerRun 模型回答运行对象
     */
    public void delete(ModelAnswerRun modelAnswerRun) {
        jdbcTemplate.update("DELETE FROM model_answer_runs WHERE id=?", modelAnswerRun.getId());
    }

    /**
     * 查找所有模型回答运?
     *
     * @return 模型回答运行列表
     */
    public List<ModelAnswerRun> findAll() {
        return jdbcTemplate.query("SELECT * FROM model_answer_runs", new ModelAnswerRunRowMapper());
    }

    /**
     * 模型回答运行行映射器
     */
    private class ModelAnswerRunRowMapper implements RowMapper<ModelAnswerRun> {
        
        /**
         * 辅助方法：加载题型提示词
         */
        private void loadQuestionTypePrompt(ResultSet brs, String columnName, Consumer<AnswerQuestionTypePrompt> setter) throws SQLException {
            try {
                Long promptId = brs.getLong(columnName);
                if (!brs.wasNull()) {
                    String promptQuery = "SELECT name, prompt_template, response_format_instruction, response_example FROM answer_question_type_prompts WHERE id = ?";
                    List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
                        promptQuery, 
                        new Object[]{promptId}, 
                        new RowMapper<AnswerQuestionTypePrompt>() {
                            @Override
                            public AnswerQuestionTypePrompt mapRow(ResultSet prs, int pRowNum) throws SQLException {
                                AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
                                prompt.setId(promptId);
                                prompt.setName(prs.getString("name"));
                                prompt.setPromptTemplate(prs.getString("prompt_template"));
                                prompt.setResponseFormatInstruction(prs.getString("response_format_instruction"));
                                prompt.setResponseExample(prs.getString("response_example"));
                                return prompt;
                            }
                        }
                    );
                    
                    if (!prompts.isEmpty()) {
                        setter.accept(prompts.get(0));
                    }
                }
            } catch (Exception e) {
                // 忽略加载提示词时的错误
            }
        }
        
        @Override
        public ModelAnswerRun mapRow(ResultSet rs, int rowNum) throws SQLException {
            ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
            modelAnswerRun.setId(rs.getLong("id"));
            
            // 设置批次
            Long batchId = rs.getLong("answer_generation_batch_id");
            if (!rs.wasNull()) {
                // 修改为加载完整批次对象
                try {
                    String batchQuery = "SELECT * FROM answer_generation_batches WHERE id = ?";
                    List<AnswerGenerationBatch> batches = jdbcTemplate.query(
                        batchQuery,
                        new Object[]{batchId},
                        new RowMapper<AnswerGenerationBatch>() {
                            @Override
                            public AnswerGenerationBatch mapRow(ResultSet brs, int bRowNum) throws SQLException {
                                // 使用一个简单的行映射器加载基本批次信息
                                AnswerGenerationBatch batch = new AnswerGenerationBatch();
                                batch.setId(brs.getLong("id"));
                                batch.setName(brs.getString("name"));
                                batch.setDescription(brs.getString("description"));
                                
                                // 加载配置ID
                                Long answerConfigId = brs.getLong("answer_assembly_config_id");
                                if (!brs.wasNull()) {
                                    // 加载配置对象
                                    try {
                                        String configQuery = "SELECT * FROM answer_prompt_assembly_configs WHERE id = ?";
                                        List<AnswerPromptAssemblyConfig> configs = jdbcTemplate.query(
                                            configQuery,
                                            new Object[]{answerConfigId},
                                            new RowMapper<AnswerPromptAssemblyConfig>() {
                                                @Override
                                                public AnswerPromptAssemblyConfig mapRow(ResultSet crs, int cRowNum) throws SQLException {
                                                    AnswerPromptAssemblyConfig config = new AnswerPromptAssemblyConfig();
                                                    config.setId(answerConfigId);
                                                    config.setName(crs.getString("name"));
                                                    config.setBaseSystemPrompt(crs.getString("base_system_prompt"));
                                                    config.setTagPromptsSectionHeader(crs.getString("tag_prompts_section_header"));
                                                    config.setQuestionTypeSectionHeader(crs.getString("question_type_section_header"));
                                                    config.setTagPromptSeparator(crs.getString("tag_prompt_separator"));
                                                    config.setSectionSeparator(crs.getString("section_separator"));
                                                    config.setFinalInstruction(crs.getString("final_instruction"));
                                                    return config;
                                                }
                                            }
                                        );
                                        
                                        if (!configs.isEmpty()) {
                                            batch.setAnswerAssemblyConfig(configs.get(0));
                                        }
                                    } catch (Exception e) {
                                        // 忽略加载配置时的错误
                                    }
                                }
                                
                                // 加载题型提示词
                                loadQuestionTypePrompt(brs, "single_choice_prompt_id", batch::setSingleChoicePrompt);
                                loadQuestionTypePrompt(brs, "multiple_choice_prompt_id", batch::setMultipleChoicePrompt);
                                loadQuestionTypePrompt(brs, "simple_fact_prompt_id", batch::setSimpleFactPrompt);
                                loadQuestionTypePrompt(brs, "subjective_prompt_id", batch::setSubjectivePrompt);
                                
                                return batch;
                            }
                        }
                    );
                    
                    if (!batches.isEmpty()) {
                        modelAnswerRun.setAnswerGenerationBatch(batches.get(0));
                    } else {
                        // 如果找不到批次，回退到只设置ID的方案
                        AnswerGenerationBatch batch = new AnswerGenerationBatch();
                        batch.setId(batchId);
                        modelAnswerRun.setAnswerGenerationBatch(batch);
                    }
                } catch (Exception e) {
                    // 如果加载批次失败，使用默认回退方案
                AnswerGenerationBatch batch = new AnswerGenerationBatch();
                batch.setId(batchId);
                modelAnswerRun.setAnswerGenerationBatch(batch);
                }
            }
            
            // 设置LLM模型
            Long modelId = rs.getLong("llm_model_id");
            if (!rs.wasNull()) {
                LlmModelRepository.findById(modelId).ifPresent(model -> modelAnswerRun.setLlmModel(model));
            }
            
            // 设置运行索引
            int runIndex = rs.getInt("run_index");
            if (!rs.wasNull()) {
                modelAnswerRun.setRunIndex(runIndex);
            }
            
            // 设置状?
            modelAnswerRun.setStatus(RunStatus.valueOf(rs.getString("status")));
            
            // 设置运行时间
            Timestamp runTime = rs.getTimestamp("run_time");
            if (runTime != null) {
                modelAnswerRun.setRunTime(runTime.toLocalDateTime());
            }
            
            // 设置错误消息
            modelAnswerRun.setErrorMessage(rs.getString("error_message"));
            
            // 设置配置
            String configJson = rs.getString("parameters");
            if (configJson != null) {
                // 将JSON字符串转换为Map
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> parameters = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
                    modelAnswerRun.setParameters(parameters);
                } catch (Exception e) {
                    // 处理JSON解析错误
                }
            }
            
            // 设置问题计数
            int totalQuestionsCount = rs.getInt("total_questions_count");
            if (!rs.wasNull()) {
                modelAnswerRun.setTotalQuestionsCount(totalQuestionsCount);
            }
            
            int completedQuestionsCount = rs.getInt("completed_questions_count");
            if (!rs.wasNull()) {
                modelAnswerRun.setCompletedQuestionsCount(completedQuestionsCount);
            }
            
            int failedQuestionsCount = rs.getInt("failed_questions_count");
            if (!rs.wasNull()) {
                modelAnswerRun.setFailedQuestionsCount(failedQuestionsCount);
            }
            
            // 设置其他可能在ModelAnswerRun中但未在表中的字?
            try {
                LocalDateTime lastActivityTime = rs.getTimestamp("last_activity_time") != null ? 
                    rs.getTimestamp("last_activity_time").toLocalDateTime() : null;
                if (lastActivityTime != null) {
                    modelAnswerRun.setLastActivityTime(lastActivityTime);
                }
            } catch (SQLException e) {
                // 忽略不存在的?
            }
            
            return modelAnswerRun;
        }
    }
} 
