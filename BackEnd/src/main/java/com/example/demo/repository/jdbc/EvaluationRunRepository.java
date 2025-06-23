package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.EvaluationRun.RunStatus;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的评测运行仓库实现
 */
@Repository
public class EvaluationRunRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EvaluatorRepository EvaluatorRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_runs (model_answer_run_id, evaluator_id, run_name, run_description, " +
            "run_time, start_time, end_time, status, parameters, error_message, created_by_user_id, " +
            "last_processed_answer_id, progress_percentage, last_activity_time, completed_answers_count, " +
            "total_answers_count, failed_evaluations_count, resume_count, completed_at, " +
            "pause_reason, pause_time, paused_by_user_id, timeout_seconds, is_auto_resume, " +
            "auto_checkpoint_interval, current_batch_start_id, current_batch_end_id, batch_size, " +
            "retry_count, max_retries, last_error_time, consecutive_errors, last_updated) " +
            "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_runs SET model_answer_run_id=?, evaluator_id=?, run_name=?, run_description=?, " +
            "run_time=?, start_time=?, end_time=?, status=?, parameters=?, error_message=?, created_by_user_id=?, " +
            "last_processed_answer_id=?, progress_percentage=?, last_activity_time=?, completed_answers_count=?, " +
            "total_answers_count=?, failed_evaluations_count=?, resume_count=?, completed_at=?, " +
            "pause_reason=?, pause_time=?, paused_by_user_id=?, timeout_seconds=?, is_auto_resume=?, " +
            "auto_checkpoint_interval=?, current_batch_start_id=?, current_batch_end_id=?, batch_size=?, " +
            "retry_count=?, max_retries=?, last_error_time=?, consecutive_errors=?, last_updated=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_runs WHERE id=?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID = 
            "SELECT * FROM evaluation_runs WHERE model_answer_run_id=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_EVALUATOR_ID = 
            "SELECT * FROM evaluation_runs WHERE evaluator_id=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_STATUS = 
            "SELECT * FROM evaluation_runs WHERE status=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID = 
            "SELECT * FROM evaluation_runs WHERE model_answer_run_id=? AND evaluator_id=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_STATUS = 
            "SELECT * FROM evaluation_runs WHERE model_answer_run_id=? AND status=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_EVALUATOR_ID_AND_STATUS = 
            "SELECT * FROM evaluation_runs WHERE evaluator_id=? AND status=? ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID_AND_STATUS = 
            "SELECT * FROM evaluation_runs WHERE model_answer_run_id=? AND evaluator_id=? AND status=? " +
            "ORDER BY id LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID_AND_STATUS_NOT = 
            "SELECT * FROM evaluation_runs WHERE model_answer_run_id=? AND evaluator_id=? AND status!=?";
    
    private static final String SQL_FIND_BY_STATUS_AND_LAST_ACTIVITY_TIME_BEFORE = 
            "SELECT * FROM evaluation_runs WHERE status=? AND last_activity_time<?";
    
    private static final String SQL_FIND_STALE_RUNS_FOR_AUTO_RESUME = 
            "SELECT * FROM evaluation_runs WHERE status=? AND last_activity_time<? AND is_auto_resume=true";
    
    private static final String SQL_FIND_STALE_RUNS = 
            "SELECT * FROM evaluation_runs WHERE status IN (%s) AND last_activity_time<?";
    
    private static final String SQL_UPDATE_RUN_STATUS = 
            "UPDATE evaluation_runs SET status=?, last_activity_time=CURRENT_TIMESTAMP, error_message=? " +
            "WHERE id=?";

    private static final String SQL_FIND_ALL_PAGED = 
            "SELECT * FROM evaluation_runs ORDER BY id LIMIT ? OFFSET ?";

    @Autowired
    public EvaluationRunRepository(JdbcTemplate jdbcTemplate, EvaluatorRepository EvaluatorRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.EvaluatorRepository = EvaluatorRepository;
    }

    /**
     * 保存评测运行
     *
     * @param evaluationRun 评测运行对象
     * @return 带有ID的评测运行对象
     */
    public EvaluationRun save(EvaluationRun evaluationRun) {
        if (evaluationRun.getId() == null) {
            return insert(evaluationRun);
        } else {
            return update(evaluationRun);
        }
    }

    /**
     * 插入新评测运行
     *
     * @param evaluationRun 评测运行对象
     * @return 带有ID的评测运行对象
     */
    private EvaluationRun insert(EvaluationRun evaluationRun) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置模型回答运行ID
            if (evaluationRun.getModelAnswerRun() != null && evaluationRun.getModelAnswerRun().getId() != null) {
                ps.setLong(1, evaluationRun.getModelAnswerRun().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置评测者ID
            ps.setLong(2, evaluationRun.getEvaluator().getId());
            
            // 设置运行名称
            ps.setString(3, evaluationRun.getRunName() != null ? 
                    evaluationRun.getRunName() : "Evaluation Run");
            
            // 设置运行描述
            if (evaluationRun.getRunDescription() != null) {
                ps.setString(4, evaluationRun.getRunDescription());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // run_time 使用数据库的 CURRENT_TIMESTAMP 设置
            
            // 设置开始时间
            ps.setTimestamp(5, evaluationRun.getStartTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getStartTime()) : null);
            
            // 设置结束时间 (end_time)
            ps.setTimestamp(6, evaluationRun.getCompletedAt() != null ? 
                    Timestamp.valueOf(evaluationRun.getCompletedAt()) : null);
            
            // 设置状态
            ps.setString(7, evaluationRun.getStatus().name());
            
            // 设置参数
            if (evaluationRun.getParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(8, objectMapper.writeValueAsString(evaluationRun.getParameters()));
                } catch (Exception e) {
                    ps.setString(8, "{}");
                }
            } else {
                ps.setString(8, "{}");
            }
            
            // 设置错误消息
            ps.setString(9, evaluationRun.getErrorMessage());
            
            // 设置创建者用户ID
            if (evaluationRun.getCreatedBy() != null) {
                ps.setLong(10, evaluationRun.getCreatedBy());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            
            // 设置上次处理的答案ID
            if (evaluationRun.getLastProcessedAnswerId() != null) {
                ps.setLong(11, evaluationRun.getLastProcessedAnswerId());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            
            // 设置进度百分比
            if (evaluationRun.getProgressPercentage() != null) {
                ps.setBigDecimal(12, evaluationRun.getProgressPercentage());
            } else {
                ps.setNull(12, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            ps.setTimestamp(13, evaluationRun.getLastActivityTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastActivityTime()) : null);
            
            // 设置已完成项目数
            if (evaluationRun.getCompletedAnswersCount() != null) {
                ps.setInt(14, evaluationRun.getCompletedAnswersCount());
            } else {
                ps.setInt(14, 0);
            }
            
            // 设置总项目数
            if (evaluationRun.getTotalAnswersCount() != null) {
                ps.setInt(15, evaluationRun.getTotalAnswersCount());
            } else {
                ps.setNull(15, Types.INTEGER);
            }
            
            // 设置失败项目数
            if (evaluationRun.getFailedEvaluationsCount() != null) {
                ps.setInt(16, evaluationRun.getFailedEvaluationsCount());
            } else {
                ps.setInt(16, 0);
            }
            
            // 设置恢复次数
            if (evaluationRun.getResumeCount() != null) {
                ps.setInt(17, evaluationRun.getResumeCount());
            } else {
                ps.setInt(17, 0);
            }
            
            // 设置完成时间
            ps.setTimestamp(18, evaluationRun.getCompletedAt() != null ? 
                    Timestamp.valueOf(evaluationRun.getCompletedAt()) : null);
            
            // 设置暂停原因
            ps.setString(19, evaluationRun.getPauseReason());
            
            // 设置暂停时间
            ps.setTimestamp(20, evaluationRun.getPauseTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getPauseTime()) : null);
            
            // 设置暂停操作用户ID
            if (evaluationRun.getPausedByUserId() != null) {
                ps.setLong(21, evaluationRun.getPausedByUserId());
            } else {
                ps.setNull(21, Types.BIGINT);
            }
            
            // 设置超时时间
            if (evaluationRun.getTimeoutSeconds() != null) {
                ps.setInt(22, evaluationRun.getTimeoutSeconds());
            } else {
                ps.setInt(22, 3600); // 默认1小时
            }
            
            // 设置是否自动恢复
            ps.setBoolean(23, evaluationRun.getIsAutoResume());
            
            // 设置自动检查点间隔
            if (evaluationRun.getAutoCheckpointInterval() != null) {
                ps.setInt(24, evaluationRun.getAutoCheckpointInterval());
            } else {
                ps.setInt(24, 60); // 默认60分钟
            }
            
            // 设置当前批次起始ID
            if (evaluationRun.getCurrentBatchStartId() != null) {
                ps.setLong(25, evaluationRun.getCurrentBatchStartId());
            } else {
                ps.setNull(25, Types.BIGINT);
            }
            
            // 设置当前批次结束ID
            if (evaluationRun.getCurrentBatchEndId() != null) {
                ps.setLong(26, evaluationRun.getCurrentBatchEndId());
            } else {
                ps.setNull(26, Types.BIGINT);
            }
            
            // 设置批次大小
            if (evaluationRun.getBatchSize() != null) {
                ps.setInt(27, evaluationRun.getBatchSize());
            } else {
                ps.setInt(27, 50); // 默认50
            }
            
            // 设置重试次数
            if (evaluationRun.getRetryCount() != null) {
                ps.setInt(28, evaluationRun.getRetryCount());
            } else {
                ps.setInt(28, 0);
            }
            
            // 设置最大重试次数
            if (evaluationRun.getMaxRetries() != null) {
                ps.setInt(29, evaluationRun.getMaxRetries());
            } else {
                ps.setInt(29, 3); // 默认3次
            }
            
            // 设置最后错误时间
            ps.setTimestamp(30, evaluationRun.getLastErrorTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastErrorTime()) : null);
            
            // 设置连续错误次数
            if (evaluationRun.getConsecutiveErrors() != null) {
                ps.setInt(31, evaluationRun.getConsecutiveErrors());
            } else {
                ps.setInt(31, 0);
            }
            
            // 设置最后更新时间
            ps.setTimestamp(32, evaluationRun.getLastUpdated() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastUpdated()) : null);
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            evaluationRun.setId(key.longValue());
        }

        return evaluationRun;
    }

    /**
     * 更新评测运行
     *
     * @param evaluationRun 评测运行对象
     * @return 更新后的评测运行对象
     */
    private EvaluationRun update(EvaluationRun evaluationRun) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置模型回答运行ID
            if (evaluationRun.getModelAnswerRun() != null && evaluationRun.getModelAnswerRun().getId() != null) {
                ps.setLong(1, evaluationRun.getModelAnswerRun().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置评测者ID
            ps.setLong(2, evaluationRun.getEvaluator().getId());
            
            // 设置运行名称
            ps.setString(3, evaluationRun.getRunName() != null ? 
                    evaluationRun.getRunName() : "Evaluation Run");
            
            // 设置运行描述
            if (evaluationRun.getRunDescription() != null) {
                ps.setString(4, evaluationRun.getRunDescription());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            // 设置运行时间
            if (evaluationRun.getRunTime() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(evaluationRun.getRunTime()));
            } else {
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            // 设置开始时间
            ps.setTimestamp(6, evaluationRun.getStartTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getStartTime()) : null);
            
            // 设置结束时间 (end_time)
            ps.setTimestamp(7, evaluationRun.getCompletedAt() != null ? 
                    Timestamp.valueOf(evaluationRun.getCompletedAt()) : null);
            
            // 设置状态
            ps.setString(8, evaluationRun.getStatus().name());
            
            // 设置参数
            if (evaluationRun.getParameters() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ps.setString(9, objectMapper.writeValueAsString(evaluationRun.getParameters()));
                } catch (Exception e) {
                    ps.setString(9, "{}");
                }
            } else {
                ps.setString(9, "{}");
            }
            
            // 设置错误消息
            ps.setString(10, evaluationRun.getErrorMessage());
            
            // 设置创建者用户ID
            if (evaluationRun.getCreatedBy() != null) {
                ps.setLong(11, evaluationRun.getCreatedBy());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            
            // 设置上次处理的答案ID
            if (evaluationRun.getLastProcessedAnswerId() != null) {
                ps.setLong(12, evaluationRun.getLastProcessedAnswerId());
            } else {
                ps.setNull(12, Types.BIGINT);
            }
            
            // 设置进度百分比
            if (evaluationRun.getProgressPercentage() != null) {
                ps.setBigDecimal(13, evaluationRun.getProgressPercentage());
            } else {
                ps.setNull(13, Types.DECIMAL);
            }
            
            // 设置最后活动时间
            ps.setTimestamp(14, evaluationRun.getLastActivityTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastActivityTime()) : null);
            
            // 设置已完成项目数
            if (evaluationRun.getCompletedAnswersCount() != null) {
                ps.setInt(15, evaluationRun.getCompletedAnswersCount());
            } else {
                ps.setInt(15, 0);
            }
            
            // 设置总项目数
            if (evaluationRun.getTotalAnswersCount() != null) {
                ps.setInt(16, evaluationRun.getTotalAnswersCount());
            } else {
                ps.setNull(16, Types.INTEGER);
            }
            
            // 设置失败项目数
            if (evaluationRun.getFailedEvaluationsCount() != null) {
                ps.setInt(17, evaluationRun.getFailedEvaluationsCount());
            } else {
                ps.setInt(17, 0);
            }
            
            // 设置恢复次数
            if (evaluationRun.getResumeCount() != null) {
                ps.setInt(18, evaluationRun.getResumeCount());
            } else {
                ps.setInt(18, 0);
            }
            
            // 设置完成时间
            ps.setTimestamp(19, evaluationRun.getCompletedAt() != null ? 
                    Timestamp.valueOf(evaluationRun.getCompletedAt()) : null);
            
            // 设置暂停原因
            ps.setString(20, evaluationRun.getPauseReason());
            
            // 设置暂停时间
            ps.setTimestamp(21, evaluationRun.getPauseTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getPauseTime()) : null);
            
            // 设置暂停操作用户ID
            if (evaluationRun.getPausedByUserId() != null) {
                ps.setLong(22, evaluationRun.getPausedByUserId());
            } else {
                ps.setNull(22, Types.BIGINT);
            }
            
            // 设置超时时间
            if (evaluationRun.getTimeoutSeconds() != null) {
                ps.setInt(23, evaluationRun.getTimeoutSeconds());
            } else {
                ps.setInt(23, 3600); // 默认1小时
            }
            
            // 设置是否自动恢复
            ps.setBoolean(24, evaluationRun.getIsAutoResume());
            
            // 设置自动检查点间隔
            if (evaluationRun.getAutoCheckpointInterval() != null) {
                ps.setInt(25, evaluationRun.getAutoCheckpointInterval());
            } else {
                ps.setInt(25, 60); // 默认60分钟
            }
            
            // 设置当前批次起始ID
            if (evaluationRun.getCurrentBatchStartId() != null) {
                ps.setLong(26, evaluationRun.getCurrentBatchStartId());
            } else {
                ps.setNull(26, Types.BIGINT);
            }
            
            // 设置当前批次结束ID
            if (evaluationRun.getCurrentBatchEndId() != null) {
                ps.setLong(27, evaluationRun.getCurrentBatchEndId());
            } else {
                ps.setNull(27, Types.BIGINT);
            }
            
            // 设置批次大小
            if (evaluationRun.getBatchSize() != null) {
                ps.setInt(28, evaluationRun.getBatchSize());
            } else {
                ps.setInt(28, 50); // 默认50
            }
            
            // 设置重试次数
            if (evaluationRun.getRetryCount() != null) {
                ps.setInt(29, evaluationRun.getRetryCount());
            } else {
                ps.setInt(29, 0);
            }
            
            // 设置最大重试次数
            if (evaluationRun.getMaxRetries() != null) {
                ps.setInt(30, evaluationRun.getMaxRetries());
            } else {
                ps.setInt(30, 3); // 默认3次
            }
            
            // 设置最后错误时间
            ps.setTimestamp(31, evaluationRun.getLastErrorTime() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastErrorTime()) : null);
            
            // 设置连续错误次数
            if (evaluationRun.getConsecutiveErrors() != null) {
                ps.setInt(32, evaluationRun.getConsecutiveErrors());
            } else {
                ps.setInt(32, 0);
            }
            
            // 设置最后更新时间
            ps.setTimestamp(33, evaluationRun.getLastUpdated() != null ? 
                    Timestamp.valueOf(evaluationRun.getLastUpdated()) : null);
            
            // 设置ID
            ps.setLong(34, evaluationRun.getId());
            
            return ps;
        });

        return evaluationRun;
    }

    /**
     * 根据ID查找评测运行
     *
     * @param id 评测运行ID
     * @return 评测运行对象
     */
    public Optional<EvaluationRun> findById(Long id) {
        try {
            EvaluationRun evaluationRun = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new EvaluationRunRowMapper(), id);
            return Optional.ofNullable(evaluationRun);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据模型回答运行ID查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByModelAnswerRunId(Long modelAnswerRunId, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID, 
                new EvaluationRunRowMapper(),
                modelAnswerRunId, 
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据评测者ID查询评测运行
     * 
     * @param evaluatorId 评测者ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByEvaluatorId(Long evaluatorId, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_ID, 
                new EvaluationRunRowMapper(),
                evaluatorId,
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据状态查询评测运行
     * 
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByStatus(RunStatus status, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_STATUS, 
                new EvaluationRunRowMapper(),
                status.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据模型回答运行ID和评测者ID查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorId(Long modelAnswerRunId, Long evaluatorId, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID, 
                new EvaluationRunRowMapper(),
                modelAnswerRunId,
                evaluatorId,
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据模型回答运行ID和状态查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByModelAnswerRunIdAndStatus(Long modelAnswerRunId, RunStatus status, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_STATUS, 
                new EvaluationRunRowMapper(),
                modelAnswerRunId,
                status.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据评测者ID和状态查询评测运行
     * 
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByEvaluatorIdAndStatus(Long evaluatorId, RunStatus status, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_ID_AND_STATUS, 
                new EvaluationRunRowMapper(),
                evaluatorId,
                status.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据模型回答运行ID、评测者ID和状态查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorIdAndStatus(
            Long modelAnswerRunId, Long evaluatorId, RunStatus status, Pageable pageable) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID_AND_STATUS, 
                new EvaluationRunRowMapper(),
                modelAnswerRunId,
                evaluatorId,
                status.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );
    }
    
    /**
     * 根据模型回答运行ID、评测者ID和非指定状态查询评测运行
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param status 不包含的状态
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByModelAnswerRunIdAndEvaluatorIdAndStatusNot(
            Long modelAnswerRunId, Long evaluatorId, RunStatus status) {
        return jdbcTemplate.query(
                SQL_FIND_BY_MODEL_ANSWER_RUN_ID_AND_EVALUATOR_ID_AND_STATUS_NOT, 
                new EvaluationRunRowMapper(),
                modelAnswerRunId,
                evaluatorId,
                status.name()
        );
    }
    
    /**
     * 根据状态和最后活动时间查询评测运行
     * 
     * @param status 状态
     * @param time 最后活动时间
     * @return 评测运行列表
     */
    public List<EvaluationRun> findByStatusAndLastActivityTimeBefore(RunStatus status, LocalDateTime time) {
        return jdbcTemplate.query(
                SQL_FIND_BY_STATUS_AND_LAST_ACTIVITY_TIME_BEFORE, 
                new EvaluationRunRowMapper(),
                status.name(),
                Timestamp.valueOf(time)
        );
    }
    
    /**
     * 查找适合自动恢复的过期运行
     * 
     * @param status 状态
     * @param time 时间阈值
     * @return 评测运行列表
     */
    public List<EvaluationRun> findStaleRunsForAutoResume(RunStatus status, LocalDateTime time) {
        return jdbcTemplate.query(
                SQL_FIND_STALE_RUNS_FOR_AUTO_RESUME, 
                new EvaluationRunRowMapper(),
                status.name(),
                Timestamp.valueOf(time)
        );
    }
    
    /**
     * 查找过期运行
     * 
     * @param statuses 状态列表
     * @param time 时间阈值
     * @return 评测运行列表
     */
    public List<EvaluationRun> findStaleRuns(List<RunStatus> statuses, LocalDateTime time) {
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        for (int i = 0; i < statuses.size(); i++) {
            placeholders.append(i > 0 ? ", ?" : "?");
            params.add(statuses.get(i).name());
        }
        
        params.add(Timestamp.valueOf(time));
        
        String sql = String.format(SQL_FIND_STALE_RUNS, placeholders);
        
        return jdbcTemplate.query(
                sql, 
                new EvaluationRunRowMapper(),
                params.toArray()
        );
    }
    
    /**
     * 更新运行状态
     * 
     * @param runId 运行ID
     * @param newStatus 新状态
     * @param errorMessage 错误消息
     */
    public void updateRunStatus(Long runId, RunStatus newStatus, String errorMessage) {
        jdbcTemplate.update(
                SQL_UPDATE_RUN_STATUS,
                newStatus.name(),
                errorMessage,
                runId
        );
    }

    /**
     * 删除评测运行
     *
     * @param evaluationRun 评测运行对象
     */
    public void delete(EvaluationRun evaluationRun) {
        jdbcTemplate.update("DELETE FROM evaluation_runs WHERE id=?", evaluationRun.getId());
    }

    /**
     * 查找所有评测运行
     *
     * @return 评测运行列表
     */
    public List<EvaluationRun> findAll() {
        return jdbcTemplate.query("SELECT * FROM evaluation_runs", new EvaluationRunRowMapper());
    }

    /**
     * 分页查询所有评测运行
     *
     * @param pageable 分页参数
     * @return 评测运行列表
     */
    public List<EvaluationRun> findAll(Pageable pageable) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        
        return jdbcTemplate.query(
                SQL_FIND_ALL_PAGED,
                new EvaluationRunRowMapper(),
                limit,
                offset
        );
    }

    /**
     * 评测运行行映射器
     */
    private class EvaluationRunRowMapper implements RowMapper<EvaluationRun> {
        @Override
        public EvaluationRun mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationRun evaluationRun = new EvaluationRun();
            evaluationRun.setId(rs.getLong("id"));
            
            // 设置模型回答运行
            Long modelAnswerRunId = rs.getLong("model_answer_run_id");
            if (!rs.wasNull()) {
                ModelAnswerRun modelAnswerRun = new ModelAnswerRun();
                modelAnswerRun.setId(modelAnswerRunId);
                evaluationRun.setModelAnswerRun(modelAnswerRun);
            }
            
            // 设置评估者
            Long evaluatorId = rs.getLong("evaluator_id");
            if (!rs.wasNull()) {
                EvaluatorRepository.findById(evaluatorId).ifPresent(evaluator -> evaluationRun.setEvaluator(evaluator));
            }
            
            // 设置状态
            evaluationRun.setStatus(RunStatus.valueOf(rs.getString("status")));
            
            // 设置时间字段
            Timestamp startTime = rs.getTimestamp("start_time");
            if (startTime != null) {
                evaluationRun.setStartTime(startTime.toLocalDateTime());
            }
            
            Timestamp completionTime = rs.getTimestamp("completed_at");
            if (completionTime != null) {
                evaluationRun.setCompletedAt(completionTime.toLocalDateTime());
            }
            
            Timestamp lastActivityTime = rs.getTimestamp("last_activity_time");
            if (lastActivityTime != null) {
                evaluationRun.setLastActivityTime(lastActivityTime.toLocalDateTime());
            }
            
            // 设置错误消息
            evaluationRun.setErrorMessage(rs.getString("error_message"));
            
            // 设置是否自动恢复
            evaluationRun.setIsAutoResume(rs.getBoolean("is_auto_resume"));
            
            // 设置当前位置
            Long currentPosition = rs.getLong("current_batch_start_id");
            if (!rs.wasNull()) {
                evaluationRun.setCurrentBatchStartId(currentPosition);
            }
            
            // 设置配置
            String configJson = rs.getString("parameters");
            if (configJson != null) {
                evaluationRun.setParameters(configJson);
            }
            
            // 设置项目数量
            int totalItems = rs.getInt("total_answers_count");
            if (!rs.wasNull()) {
                evaluationRun.setTotalAnswersCount(totalItems);
            }
            
            int completedItems = rs.getInt("completed_answers_count");
            if (!rs.wasNull()) {
                evaluationRun.setCompletedAnswersCount(completedItems);
            }
            
            int failedItems = rs.getInt("failed_evaluations_count");
            if (!rs.wasNull()) {
                evaluationRun.setFailedEvaluationsCount(failedItems);
            }
            
            // 设置运行名称
            evaluationRun.setRunName(rs.getString("run_name"));
            
            // 设置运行描述
            evaluationRun.setRunDescription(rs.getString("run_description"));
            
            return evaluationRun;
        }
    }
} 
