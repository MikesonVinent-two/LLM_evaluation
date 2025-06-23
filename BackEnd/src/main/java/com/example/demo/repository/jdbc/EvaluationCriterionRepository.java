package com.example.demo.repository.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.User;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的评测标准仓库实?
 */
@Repository
public class EvaluationCriterionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;
    private final ObjectMapper objectMapper;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_criteria (name, version, description, question_type, data_type, score_range, " +
            "applicable_question_types, weight, is_required, order_index, options, created_at, created_by_user_id, " +
            "parent_criterion_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_criteria SET name=?, version=?, description=?, question_type=?, data_type=?, " +
            "score_range=?, applicable_question_types=?, weight=?, is_required=?, order_index=?, options=?, created_at=?, " +
            "created_by_user_id=?, parent_criterion_id=?, created_change_log_id=?, deleted_at=? WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_criteria WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluation_criteria WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM evaluation_criteria WHERE deleted_at IS NULL LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM evaluation_criteria WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_QUESTION_TYPE_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_criteria WHERE question_type=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_ACTIVE_BY_QUESTION_TYPE_ORDER_BY_ORDER_INDEX = 
            "SELECT * FROM evaluation_criteria WHERE question_type=? AND deleted_at IS NULL ORDER BY order_index ASC";
    
    private static final String SQL_FIND_ACTIVE_BY_QUESTION_TYPE_ORDER_BY_ORDER_INDEX_PAGED = 
            "SELECT * FROM evaluation_criteria WHERE question_type=? AND deleted_at IS NULL ORDER BY order_index ASC LIMIT ? OFFSET ?";
    
    private static final String SQL_FIND_BY_NAME_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_criteria WHERE name=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_ID_AND_NAME_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_criteria WHERE id=? AND name=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_QUESTION_TYPE = 
            "SELECT * FROM evaluation_criteria WHERE question_type=?";
    
    private static final String SQL_FIND_BY_CRITERION_NAME = 
            "SELECT * FROM evaluation_criteria WHERE name=? AND deleted_at IS NULL";

    @Autowired
    public EvaluationCriterionRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存评测标准
     *
     * @param criterion 评测标准对象
     * @return 带有ID的评测标准对?
     */
    public EvaluationCriterion save(EvaluationCriterion criterion) {
        if (criterion.getId() == null) {
            return insert(criterion);
        } else {
            return update(criterion);
        }
    }

    /**
     * 插入新评测标?
     *
     * @param criterion 评测标准对象
     * @return 带有ID的评测标准对?
     */
    private EvaluationCriterion insert(EvaluationCriterion criterion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, criterion.getName());
            
            // 设置版本
            if (criterion.getVersion() != null) {
                ps.setString(2, criterion.getVersion());
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getDescription() != null) {
                ps.setString(3, criterion.getDescription());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getQuestionType() != null) {
                ps.setString(4, criterion.getQuestionType().name());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getDataType() != null) {
                ps.setString(5, criterion.getDataType().name());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            ps.setString(6, criterion.getScoreRange());
            
            // 设置适用问题类型列表
            try {
                if (criterion.getApplicableQuestionTypes() != null) {
                    ps.setString(7, objectMapper.writeValueAsString(criterion.getApplicableQuestionTypes()));
                } else {
                    ps.setNull(7, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getWeight() != null) {
                ps.setBigDecimal(8, criterion.getWeight());
            } else {
                ps.setNull(8, java.sql.Types.DECIMAL);
            }
            
            ps.setBoolean(9, criterion.getIsRequired() != null ? criterion.getIsRequired() : true);
            
            if (criterion.getOrderIndex() != null) {
                ps.setInt(10, criterion.getOrderIndex());
            } else {
                ps.setNull(10, java.sql.Types.INTEGER);
            }
            
            try {
                if (criterion.getOptions() != null) {
                    ps.setString(11, objectMapper.writeValueAsString(criterion.getOptions()));
                } else {
                    ps.setNull(11, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getCreatedAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(criterion.getCreatedAt()));
            } else {
                ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            if (criterion.getCreatedByUser() != null && criterion.getCreatedByUser().getId() != null) {
                ps.setLong(13, criterion.getCreatedByUser().getId());
            } else {
                ps.setNull(13, java.sql.Types.BIGINT);
            }
            
            // 设置父标准ID
            if (criterion.getParentCriterion() != null && criterion.getParentCriterion().getId() != null) {
                ps.setLong(14, criterion.getParentCriterion().getId());
            } else {
                ps.setNull(14, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (criterion.getCreatedChangeLog() != null && criterion.getCreatedChangeLog().getId() != null) {
                ps.setLong(15, criterion.getCreatedChangeLog().getId());
            } else {
                ps.setNull(15, java.sql.Types.BIGINT);
            }
            
            if (criterion.getDeletedAt() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(criterion.getDeletedAt()));
            } else {
                ps.setNull(16, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            criterion.setId(key.longValue());
        }

        return criterion;
    }

    /**
     * 更新评测标准
     *
     * @param criterion 评测标准对象
     * @return 更新后的评测标准对象
     */
    private EvaluationCriterion update(EvaluationCriterion criterion) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            ps.setString(1, criterion.getName());
            
            // 设置版本
            if (criterion.getVersion() != null) {
                ps.setString(2, criterion.getVersion());
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getDescription() != null) {
                ps.setString(3, criterion.getDescription());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getQuestionType() != null) {
                ps.setString(4, criterion.getQuestionType().name());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getDataType() != null) {
                ps.setString(5, criterion.getDataType().name());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            ps.setString(6, criterion.getScoreRange());
            
            // 设置适用问题类型列表
            try {
                if (criterion.getApplicableQuestionTypes() != null) {
                    ps.setString(7, objectMapper.writeValueAsString(criterion.getApplicableQuestionTypes()));
                } else {
                    ps.setNull(7, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getWeight() != null) {
                ps.setBigDecimal(8, criterion.getWeight());
            } else {
                ps.setNull(8, java.sql.Types.DECIMAL);
            }
            
            ps.setBoolean(9, criterion.getIsRequired() != null ? criterion.getIsRequired() : true);
            
            if (criterion.getOrderIndex() != null) {
                ps.setInt(10, criterion.getOrderIndex());
            } else {
                ps.setNull(10, java.sql.Types.INTEGER);
            }
            
            try {
                if (criterion.getOptions() != null) {
                    ps.setString(11, objectMapper.writeValueAsString(criterion.getOptions()));
                } else {
                    ps.setNull(11, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            
            if (criterion.getCreatedAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(criterion.getCreatedAt()));
            } else {
                ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            if (criterion.getCreatedByUser() != null && criterion.getCreatedByUser().getId() != null) {
                ps.setLong(13, criterion.getCreatedByUser().getId());
            } else {
                ps.setNull(13, java.sql.Types.BIGINT);
            }
            
            // 设置父标准ID
            if (criterion.getParentCriterion() != null && criterion.getParentCriterion().getId() != null) {
                ps.setLong(14, criterion.getParentCriterion().getId());
            } else {
                ps.setNull(14, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (criterion.getCreatedChangeLog() != null && criterion.getCreatedChangeLog().getId() != null) {
                ps.setLong(15, criterion.getCreatedChangeLog().getId());
            } else {
                ps.setNull(15, java.sql.Types.BIGINT);
            }
            
            if (criterion.getDeletedAt() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(criterion.getDeletedAt()));
            } else {
                ps.setNull(16, java.sql.Types.TIMESTAMP);
            }
            
            ps.setLong(17, criterion.getId());
            
            return ps;
        });

        return criterion;
    }

    /**
     * 根据ID查找评测标准
     *
     * @param id 评测标准ID
     * @return 评测标准对象（可选）
     */
    public Optional<EvaluationCriterion> findById(Long id) {
        try {
            EvaluationCriterion criterion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new EvaluationCriterionRowMapper(),
                id
            );
            return Optional.ofNullable(criterion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有评测标?
     *
     * @return 评测标准列表
     */
    public List<EvaluationCriterion> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationCriterionRowMapper());
    }

    /**
     * 分页查找所有评测标?
     *
     * @param pageable 分页参数
     * @return 评测标准分页结果
     */
    public Page<EvaluationCriterion> findAll(Pageable pageable) {
        List<EvaluationCriterion> criteria = jdbcTemplate.query(
            SQL_FIND_ALL_PAGEABLE,
            new EvaluationCriterionRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        Integer total = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(criteria, pageable, total != null ? total : 0);
    }

    /**
     * 根据问题类型查找评测标准
     *
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    public List<EvaluationCriterion> findByQuestionTypeAndDeletedAtIsNull(QuestionType questionType) {
        return jdbcTemplate.query(
            SQL_FIND_BY_QUESTION_TYPE_AND_DELETED_AT_IS_NULL,
            new EvaluationCriterionRowMapper(),
            questionType.name()
        );
    }

    /**
     * 根据问题类型查找激活的评测标准，按顺序排序
     *
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    public List<EvaluationCriterion> findActiveByQuestionTypeOrderByOrderIndex(QuestionType questionType) {
        return jdbcTemplate.query(
            SQL_FIND_ACTIVE_BY_QUESTION_TYPE_ORDER_BY_ORDER_INDEX,
            new EvaluationCriterionRowMapper(),
            questionType.name()
        );
    }

    /**
     * 根据问题类型查找激活的评测标准，按顺序排序（分页）
     *
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 评测标准列表
     */
    public List<EvaluationCriterion> findActiveByQuestionTypeOrderByOrderIndexPaged(QuestionType questionType, int page, int size) {
        int offset = page * size;
        return jdbcTemplate.query(
            SQL_FIND_ACTIVE_BY_QUESTION_TYPE_ORDER_BY_ORDER_INDEX_PAGED,
            new EvaluationCriterionRowMapper(),
            questionType.name(),
            size,
            offset
        );
    }

    /**
     * 根据名称查找评测标准
     *
     * @param name 标准名称
     * @return 评测标准
     */
    public Optional<EvaluationCriterion> findByNameAndDeletedAtIsNull(String name) {
        try {
            EvaluationCriterion criterion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_NAME_AND_DELETED_AT_IS_NULL,
                new EvaluationCriterionRowMapper(),
                name
            );
            return Optional.ofNullable(criterion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据ID和名称查找评测标?
     *
     * @param id 标准ID
     * @param name 标准名称
     * @return 评测标准
     */
    public Optional<EvaluationCriterion> findByIdAndNameAndDeletedAtIsNull(Long id, String name) {
        try {
            EvaluationCriterion criterion = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID_AND_NAME_AND_DELETED_AT_IS_NULL,
                new EvaluationCriterionRowMapper(),
                id, name
            );
            return Optional.ofNullable(criterion);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据问题类型查找评测标准
     *
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    public List<EvaluationCriterion> findByQuestionType(QuestionType questionType) {
        return jdbcTemplate.query(
            SQL_FIND_BY_QUESTION_TYPE,
            new EvaluationCriterionRowMapper(),
            questionType.name()
        );
    }

    /**
     * 根据标准名称查找评测标准
     *
     * @param criterionName 标准名称
     * @return 匹配的评测标准列表
     */
    public List<EvaluationCriterion> findByCriterionName(String criterionName) {
        try {
            return jdbcTemplate.query(SQL_FIND_BY_CRITERION_NAME, new EvaluationCriterionRowMapper(), criterionName);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 删除评测标准
     *
     * @param id 评测标准ID
     */
    public void deleteById(Long id) {
        // 实现软删?
        jdbcTemplate.update(
            "UPDATE evaluation_criteria SET deleted_at=? WHERE id=?",
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    /**
     * 评测标准行映射器
     */
    private class EvaluationCriterionRowMapper implements RowMapper<EvaluationCriterion> {
        @Override
        public EvaluationCriterion mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationCriterion criterion = new EvaluationCriterion();
            
            criterion.setId(rs.getLong("id"));
            criterion.setName(rs.getString("name"));
            
            // 设置版本
            criterion.setVersion(rs.getString("version"));
            
            criterion.setDescription(rs.getString("description"));
            
            String questionTypeStr = rs.getString("question_type");
            if (questionTypeStr != null) {
                criterion.setQuestionType(QuestionType.valueOf(questionTypeStr));
            }
            
            String dataTypeStr = rs.getString("data_type");
            if (dataTypeStr != null) {
                criterion.setDataType(EvaluationCriterion.DataType.valueOf(dataTypeStr));
            }
            
            criterion.setScoreRange(rs.getString("score_range"));
            
            // 处理适用问题类型列表
            String applicableQuestionTypesJson = rs.getString("applicable_question_types");
            if (applicableQuestionTypesJson != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> applicableQuestionTypes = objectMapper.readValue(applicableQuestionTypesJson, List.class);
                    criterion.setApplicableQuestionTypes(applicableQuestionTypes);
                } catch (Exception e) {
                    // 忽略解析错误，保持applicableQuestionTypes为null
                }
            }
            
            BigDecimal weight = rs.getBigDecimal("weight");
            if (!rs.wasNull()) {
                criterion.setWeight(weight);
            }
            
            criterion.setIsRequired(rs.getBoolean("is_required"));
            
            Integer orderIndex = rs.getInt("order_index");
            if (!rs.wasNull()) {
                criterion.setOrderIndex(orderIndex);
            }
            
            // 处理JSON类型的options字段
            String optionsJson = rs.getString("options");
            if (optionsJson != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> options = objectMapper.readValue(optionsJson, Map.class);
                    criterion.setOptions(options);
                } catch (Exception e) {
                    // 忽略解析错误，保持options为null
                }
            }
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                criterion.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(createdByUserId);
                criterion.setCreatedByUser(user);
                
                // 可选：加载完整的用户信?
                UserRepository.findById(createdByUserId).ifPresent(criterion::setCreatedByUser);
            }
            
            // 设置父标准ID
            Long parentCriterionId = rs.getLong("parent_criterion_id");
            if (!rs.wasNull()) {
                EvaluationCriterion parentCriterion = new EvaluationCriterion();
                parentCriterion.setId(parentCriterionId);
                criterion.setParentCriterion(parentCriterion);
            }
            
            // 设置创建变更日志ID
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                criterion.setCreatedChangeLog(changeLog);
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                criterion.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            return criterion;
        }
    }
} 
