package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.User;

/**
 * 基于JDBC的评测者仓库实现
 */
@Repository
public class EvaluatorRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final LlmModelRepository llmModelRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluators (evaluator_type, user_id, llm_model_id, name, created_at, created_by_user_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluators SET evaluator_type=?, user_id=?, llm_model_id=?, name=?, created_by_user_id=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluators WHERE id=?";
    
    private static final String SQL_FIND_BY_NAME = 
            "SELECT * FROM evaluators WHERE name=?";
    
    private static final String SQL_FIND_BY_EVALUATOR_TYPE = 
            "SELECT * FROM evaluators WHERE evaluator_type=?";
    
    private static final String SQL_FIND_BY_EVALUATOR_TYPE_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluators WHERE evaluator_type=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluators";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE evaluators SET deleted_at=? WHERE id=?";
            
    private static final String SQL_EXISTS_BY_ID = 
            "SELECT COUNT(*) FROM evaluators WHERE id=?";
            
    private static final String SQL_FIND_HUMAN_EVALUATOR_IDS_BY_USER_ID = 
            "SELECT id FROM evaluators WHERE evaluator_type='HUMAN' AND user_id=? AND deleted_at IS NULL";

    @Autowired
    public EvaluatorRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository, LlmModelRepository llmModelRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.llmModelRepository = llmModelRepository;
    }

    /**
     * 保存评测者
     *
     * @param evaluator 评测者对象
     * @return 带有ID的评测者对象
     */
    public Evaluator save(Evaluator evaluator) {
        if (evaluator.getId() == null) {
            return insert(evaluator);
        } else {
            return update(evaluator);
        }
    }

    /**
     * 插入新评测者
     *
     * @param evaluator 评测者对象
     * @return 带有ID的评测者对象
     */
    private Evaluator insert(Evaluator evaluator) {
        if (evaluator.getCreatedAt() == null) {
            evaluator.setCreatedAt(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置评测者类型
            ps.setString(1, evaluator.getEvaluatorType().name());
            
            // 设置用户ID
            if (evaluator.getUser() != null && evaluator.getUser().getId() != null) {
                ps.setLong(2, evaluator.getUser().getId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            
            // 设置LLM模型ID
            if (evaluator.getLlmModel() != null && evaluator.getLlmModel().getId() != null) {
                ps.setLong(3, evaluator.getLlmModel().getId());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            
            ps.setString(4, evaluator.getName());
            ps.setTimestamp(5, Timestamp.valueOf(evaluator.getCreatedAt()));
            
            // 设置创建用户ID
            if (evaluator.getCreatedByUser() != null && evaluator.getCreatedByUser().getId() != null) {
                ps.setLong(6, evaluator.getCreatedByUser().getId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (evaluator.getCreatedChangeLog() != null && evaluator.getCreatedChangeLog().getId() != null) {
                ps.setLong(7, evaluator.getCreatedChangeLog().getId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            
            // 设置删除时间
            if (evaluator.getDeletedAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(evaluator.getDeletedAt()));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            evaluator.setId(key.longValue());
        }
        return evaluator;
    }

    /**
     * 更新评测者
     *
     * @param evaluator 评测者对象
     * @return 更新后的评测者对象
     */
    private Evaluator update(Evaluator evaluator) {
        jdbcTemplate.update(SQL_UPDATE,
                evaluator.getEvaluatorType().name(),
                evaluator.getUser() != null ? evaluator.getUser().getId() : null,
                evaluator.getLlmModel() != null ? evaluator.getLlmModel().getId() : null,
                evaluator.getName(),
                evaluator.getCreatedByUser() != null ? evaluator.getCreatedByUser().getId() : null,
                evaluator.getCreatedChangeLog() != null ? evaluator.getCreatedChangeLog().getId() : null,
                evaluator.getDeletedAt() != null ? Timestamp.valueOf(evaluator.getDeletedAt()) : null,
                evaluator.getId());

        return evaluator;
    }

    /**
     * 根据ID查找评测者
     *
     * @param id 评测者ID
     * @return 评测者的Optional包装
     */
    public Optional<Evaluator> findById(Long id) {
        try {
            Evaluator evaluator = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new EvaluatorRowMapper());
            return Optional.ofNullable(evaluator);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据名称查找评测者
     *
     * @param name 评测者名称
     * @return 评测者的Optional包装
     */
    public Optional<Evaluator> findByName(String name) {
        try {
            Evaluator evaluator = jdbcTemplate.queryForObject(SQL_FIND_BY_NAME, new Object[]{name}, new EvaluatorRowMapper());
            return Optional.ofNullable(evaluator);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据评测者类型查找评测者
     *
     * @param evaluatorType 评测者类型
     * @return 评测者列表
     */
    public List<Evaluator> findByEvaluatorType(Evaluator.EvaluatorType evaluatorType) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_TYPE,
                new Object[]{evaluatorType.name()},
                new EvaluatorRowMapper()
        );
    }

    /**
     * 根据评测者类型查找未删除的评测者
     *
     * @param evaluatorType 评测者类型
     * @return 评测者列表
     */
    public List<Evaluator> findByEvaluatorTypeAndDeletedAtIsNull(Evaluator.EvaluatorType evaluatorType) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_TYPE_AND_DELETED_AT_IS_NULL,
                new Object[]{evaluatorType.name()},
                new EvaluatorRowMapper()
        );
    }
    
    /**
     * 查找所有评测者
     *
     * @return 评测者列表
     */
    public List<Evaluator> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluatorRowMapper());
    }

    /**
     * 软删除评测者
     *
     * @param id 评测者ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }
    
    /**
     * 删除评测者（通过ID）
     * 
     * @param id 评测者ID
     */
    public void deleteById(Long id) {
        softDelete(id);
    }
    
    /**
     * 检查指定ID的评测者是否存在
     *
     * @param id 评测者ID
     * @return 是否存在
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * 根据用户ID查找人类评测者ID列表
     *
     * @param userId 用户ID
     * @return 人类评测者ID列表
     */
    public List<Long> findHumanEvaluatorIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(SQL_FIND_HUMAN_EVALUATOR_IDS_BY_USER_ID, Long.class, userId);
    }

    /**
     * 评测者行映射器
     */
    private class EvaluatorRowMapper implements RowMapper<Evaluator> {
        @Override
        public Evaluator mapRow(ResultSet rs, int rowNum) throws SQLException {
            Evaluator evaluator = new Evaluator();
            evaluator.setId(rs.getLong("id"));
            
            // 解析枚举
            String evaluatorTypeStr = rs.getString("evaluator_type");
            if (evaluatorTypeStr != null) {
                evaluator.setEvaluatorType(Evaluator.EvaluatorType.valueOf(evaluatorTypeStr));
            }
            
            evaluator.setName(rs.getString("name"));
            
            // 设置时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                evaluator.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                evaluator.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            // 处理外键关联
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                evaluator.setUser(user);
            }
            
            Long llmModelId = rs.getLong("llm_model_id");
            if (!rs.wasNull()) {
                // 加载完整的LlmModel信息，而不是只设置ID
                llmModelRepository.findById(llmModelId).ifPresent(llmModel -> {
                    evaluator.setLlmModel(llmModel);
                });
            }
            
            // 设置创建者用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId).ifPresent(user -> evaluator.setCreatedByUser(user));
            }
            
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                evaluator.setCreatedChangeLog(changeLog);
            }
            
            return evaluator;
        }
    }
} 
