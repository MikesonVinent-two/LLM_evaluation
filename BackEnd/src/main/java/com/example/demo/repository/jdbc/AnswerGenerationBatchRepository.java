package com.example.demo.repository.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.AnswerGenerationBatch;
import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的答案生成批次仓库实现
 */
@Repository
public class AnswerGenerationBatchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationBatchRepository.class);

    private static final String SQL_INSERT = 
            "INSERT INTO answer_generation_batches " +
            "(name, description, dataset_version_id, creation_time, status, " +
            "answer_assembly_config_id, single_choice_prompt_id, " +
            "multiple_choice_prompt_id, simple_fact_prompt_id, subjective_prompt_id, " +
            "global_parameters, created_by_user_id, completed_at, progress_percentage, " +
            "last_activity_time, last_check_time, resume_count, pause_time, pause_reason, " +
            "answer_repeat_count, error_message, processing_instance, last_processed_run_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE answer_generation_batches SET " +
            "name=?, description=?, dataset_version_id=?, creation_time=?, status=?, " +
            "answer_assembly_config_id=?, single_choice_prompt_id=?, " +
            "multiple_choice_prompt_id=?, simple_fact_prompt_id=?, subjective_prompt_id=?, " +
            "global_parameters=?, created_by_user_id=?, completed_at=?, progress_percentage=?, " +
            "last_activity_time=?, last_check_time=?, resume_count=?, pause_time=?, pause_reason=?, " +
            "answer_repeat_count=?, error_message=?, processing_instance=?, last_processed_run_id=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM answer_generation_batches WHERE id=?";
    
    private static final String SQL_FIND_BY_STATUS = 
            "SELECT * FROM answer_generation_batches WHERE status=?";
    
    private static final String SQL_FIND_BY_CREATED_BY_USER_ID = 
            "SELECT * FROM answer_generation_batches WHERE created_by_user_id=?";
    
    private static final String SQL_FIND_BY_DATASET_VERSION_ID = 
            "SELECT * FROM answer_generation_batches WHERE dataset_version_id=?";
    
    private static final String SQL_COUNT_BY_STATUS = 
            "SELECT COUNT(*) FROM answer_generation_batches WHERE status=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM answer_generation_batches";
    
    private static final String SQL_DELETE = 
            "DELETE FROM answer_generation_batches WHERE id=?";

    @Autowired
    public AnswerGenerationBatchRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    public AnswerGenerationBatch save(AnswerGenerationBatch batch) {
        if (batch.getId() == null) {
            return insert(batch);
        } else {
            return update(batch);
        }
    }

    /**
     * 插入新答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    private AnswerGenerationBatch insert(AnswerGenerationBatch batch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置名称
            ps.setString(1, batch.getName());
            
            // 设置描述
            if (batch.getDescription() != null) {
                ps.setString(2, batch.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置数据集版本ID
            if (batch.getDatasetVersion() != null && batch.getDatasetVersion().getId() != null) {
                ps.setLong(3, batch.getDatasetVersion().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            
            // 设置创建时间
            if (batch.getCreationTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(batch.getCreationTime()));
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置状态
            ps.setString(5, batch.getStatus().name());
            
            // 设置回答组装配置ID
            if (batch.getAnswerAssemblyConfig() != null && batch.getAnswerAssemblyConfig().getId() != null) {
                ps.setLong(6, batch.getAnswerAssemblyConfig().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置单选题prompt ID
            if (batch.getSingleChoicePrompt() != null && batch.getSingleChoicePrompt().getId() != null) {
                ps.setLong(7, batch.getSingleChoicePrompt().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置多选题prompt ID
            if (batch.getMultipleChoicePrompt() != null && batch.getMultipleChoicePrompt().getId() != null) {
                ps.setLong(8, batch.getMultipleChoicePrompt().getId());
            } else {
                ps.setNull(8, Types.BIGINT);
            }
            
            // 设置简单事实题prompt ID
            if (batch.getSimpleFactPrompt() != null && batch.getSimpleFactPrompt().getId() != null) {
                ps.setLong(9, batch.getSimpleFactPrompt().getId());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            
            // 设置主观题prompt ID
            if (batch.getSubjectivePrompt() != null && batch.getSubjectivePrompt().getId() != null) {
                ps.setLong(10, batch.getSubjectivePrompt().getId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置全局参数
            if (batch.getGlobalParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(11, objectMapper.writeValueAsString(batch.getGlobalParameters()));
                } catch (Exception e) {
                    ps.setString(11, "{}");
                }
            } else {
                ps.setString(11, "{}");
            }
            
            // 设置创建者ID
            if (batch.getCreatedByUser() != null && batch.getCreatedByUser().getId() != null) {
                ps.setLong(12, batch.getCreatedByUser().getId());
            } else {
                ps.setNull(12, Types.BIGINT);
            }
            
            // 设置完成时间
            if (batch.getCompletedAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(batch.getCompletedAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            
            // 设置进度百分比
            if (batch.getProgressPercentage() != null) {
                ps.setBigDecimal(14, batch.getProgressPercentage());
            } else {
                ps.setNull(14, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            if (batch.getLastActivityTime() != null) {
                ps.setTimestamp(15, Timestamp.valueOf(batch.getLastActivityTime()));
            } else {
                ps.setNull(15, Types.TIMESTAMP);
            }
            
            // 设置最后检查时间
            if (batch.getLastCheckTime() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(batch.getLastCheckTime()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (batch.getResumeCount() != null) {
                ps.setInt(17, batch.getResumeCount());
            } else {
                ps.setInt(17, 0);
            }
            
            // 设置暂停时间
            if (batch.getPauseTime() != null) {
                ps.setTimestamp(18, Timestamp.valueOf(batch.getPauseTime()));
            } else {
                ps.setNull(18, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (batch.getPauseReason() != null) {
                ps.setString(19, batch.getPauseReason());
            } else {
                ps.setNull(19, Types.VARCHAR);
            }
            
            // 设置回答重复次数
            if (batch.getAnswerRepeatCount() != null) {
                ps.setInt(20, batch.getAnswerRepeatCount());
            } else {
                ps.setInt(20, 1); // 默认值
            }
            
            // 设置错误信息
            if (batch.getErrorMessage() != null) {
                ps.setString(21, batch.getErrorMessage());
            } else {
                ps.setNull(21, Types.VARCHAR);
            }
            
            // 设置处理实例标识
            if (batch.getProcessingInstance() != null) {
                ps.setString(22, batch.getProcessingInstance());
            } else {
                ps.setNull(22, Types.VARCHAR);
            }
            
            // 设置上次处理的运行ID
            if (batch.getLastProcessedRunId() != null) {
                ps.setLong(23, batch.getLastProcessedRunId());
            } else {
                ps.setNull(23, Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            batch.setId(key.longValue());
        }

        return batch;
    }

    /**
     * 更新答案生成批次
     *
     * @param batch 答案生成批次对象
     * @return 更新后的答案生成批次对象
     */
    private AnswerGenerationBatch update(AnswerGenerationBatch batch) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置名称
            ps.setString(1, batch.getName());
            
            // 设置描述
            if (batch.getDescription() != null) {
                ps.setString(2, batch.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            
            // 设置数据集版本ID
            if (batch.getDatasetVersion() != null && batch.getDatasetVersion().getId() != null) {
                ps.setLong(3, batch.getDatasetVersion().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            
            // 设置创建时间
            if (batch.getCreationTime() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(batch.getCreationTime()));
            } else {
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置状态
            ps.setString(5, batch.getStatus().name());
            
            // 设置回答组装配置ID
            if (batch.getAnswerAssemblyConfig() != null && batch.getAnswerAssemblyConfig().getId() != null) {
                ps.setLong(6, batch.getAnswerAssemblyConfig().getId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            
            // 设置单选题prompt ID
            if (batch.getSingleChoicePrompt() != null && batch.getSingleChoicePrompt().getId() != null) {
                ps.setLong(7, batch.getSingleChoicePrompt().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置多选题prompt ID
            if (batch.getMultipleChoicePrompt() != null && batch.getMultipleChoicePrompt().getId() != null) {
                ps.setLong(8, batch.getMultipleChoicePrompt().getId());
            } else {
                ps.setNull(8, Types.BIGINT);
            }
            
            // 设置简单事实题prompt ID
            if (batch.getSimpleFactPrompt() != null && batch.getSimpleFactPrompt().getId() != null) {
                ps.setLong(9, batch.getSimpleFactPrompt().getId());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            
            // 设置主观题prompt ID
            if (batch.getSubjectivePrompt() != null && batch.getSubjectivePrompt().getId() != null) {
                ps.setLong(10, batch.getSubjectivePrompt().getId());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置全局参数
            if (batch.getGlobalParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(11, objectMapper.writeValueAsString(batch.getGlobalParameters()));
                } catch (Exception e) {
                    ps.setString(11, "{}");
                }
            } else {
                ps.setString(11, "{}");
            }
            
            // 设置创建者ID
            if (batch.getCreatedByUser() != null && batch.getCreatedByUser().getId() != null) {
                ps.setLong(12, batch.getCreatedByUser().getId());
            } else {
                ps.setNull(12, Types.BIGINT);
            }
            
            // 设置完成时间
            if (batch.getCompletedAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(batch.getCompletedAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            
            // 设置进度百分比
            if (batch.getProgressPercentage() != null) {
                ps.setBigDecimal(14, batch.getProgressPercentage());
            } else {
                ps.setNull(14, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            if (batch.getLastActivityTime() != null) {
                ps.setTimestamp(15, Timestamp.valueOf(batch.getLastActivityTime()));
            } else {
                ps.setNull(15, Types.TIMESTAMP);
            }
            
            // 设置最后检查时间
            if (batch.getLastCheckTime() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(batch.getLastCheckTime()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            
            // 设置恢复次数
            if (batch.getResumeCount() != null) {
                ps.setInt(17, batch.getResumeCount());
            } else {
                ps.setInt(17, 0);
            }
            
            // 设置暂停时间
            if (batch.getPauseTime() != null) {
                ps.setTimestamp(18, Timestamp.valueOf(batch.getPauseTime()));
            } else {
                ps.setNull(18, Types.TIMESTAMP);
            }
            
            // 设置暂停原因
            if (batch.getPauseReason() != null) {
                ps.setString(19, batch.getPauseReason());
            } else {
                ps.setNull(19, Types.VARCHAR);
            }
            
            // 设置回答重复次数
            if (batch.getAnswerRepeatCount() != null) {
                ps.setInt(20, batch.getAnswerRepeatCount());
            } else {
                ps.setInt(20, 1); // 默认值
            }
            
            // 设置错误信息
            if (batch.getErrorMessage() != null) {
                ps.setString(21, batch.getErrorMessage());
            } else {
                ps.setNull(21, Types.VARCHAR);
            }
            
            // 设置处理实例标识
            if (batch.getProcessingInstance() != null) {
                ps.setString(22, batch.getProcessingInstance());
            } else {
                ps.setNull(22, Types.VARCHAR);
            }
            
            // 设置上次处理的运行ID
            if (batch.getLastProcessedRunId() != null) {
                ps.setLong(23, batch.getLastProcessedRunId());
            } else {
                ps.setNull(23, Types.BIGINT);
            }
            
            // 设置ID
            ps.setLong(24, batch.getId());
            
            return ps;
        });

        return batch;
    }

    /**
     * 根据ID查找答案生成批次
     *
     * @param id 答案生成批次ID
     * @return 答案生成批次对象
     */
    public Optional<AnswerGenerationBatch> findById(Long id) {
        try {
            AnswerGenerationBatch batch = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new AnswerGenerationBatchRowMapper(),
                id
            );
            return Optional.ofNullable(batch);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据状态查找答案生成批次
     *
     * @param status 状态
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByStatus(BatchStatus status) {
        return jdbcTemplate.query(SQL_FIND_BY_STATUS, new AnswerGenerationBatchRowMapper(), status.name());
    }

    /**
     * 根据创建者ID查找答案生成批次
     *
     * @param userId 用户ID
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByCreatedByUserId(Long userId) {
        return jdbcTemplate.query(SQL_FIND_BY_CREATED_BY_USER_ID, new AnswerGenerationBatchRowMapper(), userId);
    }

    /**
     * 根据数据集版本ID查找答案生成批次
     *
     * @param datasetVersionId 数据集版本ID
     * @return 答案生成批次列表
     */
    public List<AnswerGenerationBatch> findByDatasetVersionId(Long datasetVersionId) {
        return jdbcTemplate.query(SQL_FIND_BY_DATASET_VERSION_ID, new AnswerGenerationBatchRowMapper(), datasetVersionId);
    }

    /**
     * 按状态统计答案生成批次数
     *
     * @param status 状态
     * @return 数量
     */
    public long countByStatus(BatchStatus status) {
        return jdbcTemplate.queryForObject(SQL_COUNT_BY_STATUS, Long.class, status.name());
    }

    /**
     * 查找所有答案生成批次
     *
     * @return 所有答案生成批次列表
     */
    public List<AnswerGenerationBatch> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new AnswerGenerationBatchRowMapper());
    }

    /**
     * 删除答案生成批次
     *
     * @param batch 答案生成批次对象
     */
    public void delete(AnswerGenerationBatch batch) {
        jdbcTemplate.update(SQL_DELETE, batch.getId());
    }

    /**
     * 答案生成批次行映射器
     */
    private class AnswerGenerationBatchRowMapper implements RowMapper<AnswerGenerationBatch> {
        @Override
        public AnswerGenerationBatch mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnswerGenerationBatch batch = new AnswerGenerationBatch();
            
            // 设置ID和基本属性
            batch.setId(rs.getLong("id"));
            batch.setName(rs.getString("name"));
            batch.setDescription(rs.getString("description"));
            batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
            
            // 设置时间字段
            Timestamp creationTime = rs.getTimestamp("creation_time");
            if (creationTime != null) {
                batch.setCreationTime(creationTime.toLocalDateTime());
            }
            
            Timestamp completedAt = rs.getTimestamp("completed_at");
            if (completedAt != null) {
                batch.setCompletedAt(completedAt.toLocalDateTime());
            }
            
            // 设置创建者
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(createdByUserId);
                batch.setCreatedByUser(user);
                
                // 可选：通过UserRepository加载完整的用户对象
                // UserRepository.findById(createdByUserId).ifPresent(batch::setCreatedByUser);
            }
            
            // 设置数据集版本
            Long datasetVersionId = rs.getLong("dataset_version_id");
            if (!rs.wasNull()) {
                DatasetVersion datasetVersion = new DatasetVersion();
                datasetVersion.setId(datasetVersionId);
                
                // 改进版本信息加载方式
                try {
                    String versionQuery = "SELECT name, version_number FROM dataset_versions WHERE id = ?";
                    List<DatasetVersion> versions = jdbcTemplate.query(
                        versionQuery, 
                        new Object[]{datasetVersionId}, 
                        (ResultSet vrs, int rowIndex) -> {
                            DatasetVersion version = new DatasetVersion();
                            version.setId(datasetVersionId);
                            version.setName(vrs.getString("name"));
                            version.setVersionNumber(vrs.getString("version_number"));
                            return version;
                        }
                    );
                    
                    if (!versions.isEmpty()) {
                        // 使用查询到的完整对象替换原来只有ID的对象
                        datasetVersion = versions.get(0);
                    } else {
                        logger.warn("批次{}的数据集版本(ID={})在dataset_versions表中不存在", batch.getId(), datasetVersionId);
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的完整数据集版本信息失败: {}", batch.getId(), e.getMessage(), e);
                }
                
                batch.setDatasetVersion(datasetVersion);
                logger.debug("批次{}设置了数据集版本: ID={}, 名称={}", 
                    batch.getId(), datasetVersion.getId(), datasetVersion.getName());
            } else {
                logger.warn("批次{}的数据集版本ID为NULL", batch.getId());
                // 创建一个空的数据集版本对象，避免NPE
                DatasetVersion emptyVersion = new DatasetVersion();
                emptyVersion.setId(-1L); // 使用一个特殊值表示这是空对象
                emptyVersion.setName("未知数据集");
                emptyVersion.setVersionNumber("未知版本");
                batch.setDatasetVersion(emptyVersion);
            }
            
            // 设置回答prompt组装配置
            Long answerConfigId = rs.getLong("answer_assembly_config_id");
            if (!rs.wasNull()) {
                try {
                    String configQuery = "SELECT name, base_system_prompt, tag_prompts_section_header, question_type_section_header, tag_prompt_separator, section_separator, final_instruction FROM answer_prompt_assembly_configs WHERE id = ?";
                    List<AnswerPromptAssemblyConfig> configs = jdbcTemplate.query(
                        configQuery, 
                        new Object[]{answerConfigId}, 
                        (ResultSet crs, int cRowIndex) -> {
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
                    );
                    
                    if (!configs.isEmpty()) {
                        batch.setAnswerAssemblyConfig(configs.get(0));
                        logger.debug("批次{}设置了回答Prompt组装配置: ID={}, 名称={}", 
                            batch.getId(), configs.get(0).getId(), configs.get(0).getName());
                    } else {
                        logger.warn("批次{}的回答Prompt组装配置(ID={})在answer_prompt_assembly_configs表中不存在", 
                            batch.getId(), answerConfigId);
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的回答Prompt组装配置失败: {}", batch.getId(), e.getMessage(), e);
                }
            } else {
                logger.warn("批次{}的回答Prompt组装配置ID为NULL", batch.getId());
            }
            
            // 加载题型相关的提示词配置
            // 1. 单选题提示词
            Long singleChoicePromptId = rs.getLong("single_choice_prompt_id");
            if (!rs.wasNull()) {
                try {
                    String promptQuery = "SELECT name, prompt_template, response_format_instruction, response_example FROM answer_question_type_prompts WHERE id = ?";
                    List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
                        promptQuery, 
                        new Object[]{singleChoicePromptId}, 
                        (ResultSet prs, int pRowIndex) -> {
                            AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
                            prompt.setId(singleChoicePromptId);
                            prompt.setName(prs.getString("name"));
                            prompt.setPromptTemplate(prs.getString("prompt_template"));
                            prompt.setResponseFormatInstruction(prs.getString("response_format_instruction"));
                            prompt.setResponseExample(prs.getString("response_example"));
                            return prompt;
                        }
                    );
                    
                    if (!prompts.isEmpty()) {
                        batch.setSingleChoicePrompt(prompts.get(0));
                        logger.debug("批次{}设置了单选题提示词: ID={}, 名称={}", 
                            batch.getId(), prompts.get(0).getId(), prompts.get(0).getName());
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的单选题提示词失败: {}", batch.getId(), e.getMessage(), e);
                }
            }
            
            // 2. 多选题提示词
            Long multipleChoicePromptId = rs.getLong("multiple_choice_prompt_id");
            if (!rs.wasNull()) {
                try {
                    String promptQuery = "SELECT name, prompt_template, response_format_instruction, response_example FROM answer_question_type_prompts WHERE id = ?";
                    List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
                        promptQuery, 
                        new Object[]{multipleChoicePromptId}, 
                        (ResultSet prs, int pRowIndex) -> {
                            AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
                            prompt.setId(multipleChoicePromptId);
                            prompt.setName(prs.getString("name"));
                            prompt.setPromptTemplate(prs.getString("prompt_template"));
                            prompt.setResponseFormatInstruction(prs.getString("response_format_instruction"));
                            prompt.setResponseExample(prs.getString("response_example"));
                            return prompt;
                        }
                    );
                    
                    if (!prompts.isEmpty()) {
                        batch.setMultipleChoicePrompt(prompts.get(0));
                        logger.debug("批次{}设置了多选题提示词: ID={}, 名称={}", 
                            batch.getId(), prompts.get(0).getId(), prompts.get(0).getName());
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的多选题提示词失败: {}", batch.getId(), e.getMessage(), e);
                }
            }
            
            // 3. 简单事实题提示词
            Long simpleFactPromptId = rs.getLong("simple_fact_prompt_id");
            if (!rs.wasNull()) {
                try {
                    String promptQuery = "SELECT name, prompt_template, response_format_instruction, response_example FROM answer_question_type_prompts WHERE id = ?";
                    List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
                        promptQuery, 
                        new Object[]{simpleFactPromptId}, 
                        (ResultSet prs, int pRowIndex) -> {
                            AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
                            prompt.setId(simpleFactPromptId);
                            prompt.setName(prs.getString("name"));
                            prompt.setPromptTemplate(prs.getString("prompt_template"));
                            prompt.setResponseFormatInstruction(prs.getString("response_format_instruction"));
                            prompt.setResponseExample(prs.getString("response_example"));
                            return prompt;
                        }
                    );
                    
                    if (!prompts.isEmpty()) {
                        batch.setSimpleFactPrompt(prompts.get(0));
                        logger.debug("批次{}设置了简单事实题提示词: ID={}, 名称={}", 
                            batch.getId(), prompts.get(0).getId(), prompts.get(0).getName());
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的简单事实题提示词失败: {}", batch.getId(), e.getMessage(), e);
                }
            }
            
            // 4. 主观题提示词
            Long subjectivePromptId = rs.getLong("subjective_prompt_id");
            if (!rs.wasNull()) {
                try {
                    String promptQuery = "SELECT name, prompt_template, response_format_instruction, response_example FROM answer_question_type_prompts WHERE id = ?";
                    List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
                        promptQuery, 
                        new Object[]{subjectivePromptId}, 
                        (ResultSet prs, int pRowIndex) -> {
                            AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
                            prompt.setId(subjectivePromptId);
                            prompt.setName(prs.getString("name"));
                            prompt.setPromptTemplate(prs.getString("prompt_template"));
                            prompt.setResponseFormatInstruction(prs.getString("response_format_instruction"));
                            prompt.setResponseExample(prs.getString("response_example"));
                            return prompt;
                        }
                    );
                    
                    if (!prompts.isEmpty()) {
                        batch.setSubjectivePrompt(prompts.get(0));
                        logger.debug("批次{}设置了主观题提示词: ID={}, 名称={}", 
                            batch.getId(), prompts.get(0).getId(), prompts.get(0).getName());
                    }
                } catch (Exception e) {
                    logger.error("加载批次{}的主观题提示词失败: {}", batch.getId(), e.getMessage(), e);
                }
            }
            
            // 设置配置
            String configJson = rs.getString("global_parameters");
            if (configJson != null) {
                // 转换为Map<String, Object>
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> globalParams = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
                    batch.setGlobalParameters(globalParams);
                } catch (Exception e) {
                    // 处理JSON解析异常
                    Map<String, Object> emptyMap = new HashMap<>();
                    batch.setGlobalParameters(emptyMap);
                }
            }
            
            // 加载其他必要字段
            batch.setErrorMessage(rs.getString("error_message"));
            batch.setPauseReason(rs.getString("pause_reason"));
            batch.setProcessingInstance(rs.getString("processing_instance"));
            
            // 加载数值字段
            batch.setAnswerRepeatCount(rs.getInt("answer_repeat_count"));
            if (rs.wasNull()) {
                batch.setAnswerRepeatCount(1); // 默认值
            }
            
            batch.setResumeCount(rs.getInt("resume_count"));
            if (rs.wasNull()) {
                batch.setResumeCount(0);
            }
            
            // 加载最后处理的运行ID
            Long lastProcessedRunId = rs.getLong("last_processed_run_id");
            if (!rs.wasNull()) {
                batch.setLastProcessedRunId(lastProcessedRunId);
            }
            
            // 加载进度百分比
            BigDecimal progressPercentage = rs.getBigDecimal("progress_percentage");
            if (progressPercentage != null) {
                batch.setProgressPercentage(progressPercentage);
            }
            
            // 加载其他时间戳
            Timestamp lastActivityTime = rs.getTimestamp("last_activity_time");
            if (lastActivityTime != null) {
                batch.setLastActivityTime(lastActivityTime.toLocalDateTime());
            }
            
            Timestamp lastCheckTime = rs.getTimestamp("last_check_time");
            if (lastCheckTime != null) {
                batch.setLastCheckTime(lastCheckTime.toLocalDateTime());
            }
            
            Timestamp pauseTime = rs.getTimestamp("pause_time");
            if (pauseTime != null) {
                batch.setPauseTime(pauseTime.toLocalDateTime());
            }
            
            return batch;
        }
    }

    /**
     * 保存答案生成批次并立即刷新
     * 在JDBC实现中与save方法功能相同，但保持与JPA接口兼容
     *
     * @param batch 答案生成批次对象
     * @return 带有ID的答案生成批次对象
     */
    public AnswerGenerationBatch saveAndFlush(AnswerGenerationBatch batch) {
        return save(batch);
    }
} 
