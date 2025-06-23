package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.EvaluationSubjectivePrompt;
import com.example.demo.entity.jdbc.User;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于JDBC的主观题评测提示词仓库实?
 */
@Repository
public class EvaluationSubjectivePromptRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository UserRepository;
    private final ObjectMapper objectMapper;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_subjective_prompts (name, prompt_template, description, evaluation_criteria_focus, " +
            "scoring_instruction, output_format_instruction, is_active, version, created_at, updated_at, " +
            "created_by_user_id, parent_prompt_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_subjective_prompts SET name=?, prompt_template=?, description=?, evaluation_criteria_focus=?, " +
            "scoring_instruction=?, output_format_instruction=?, is_active=?, version=?, created_at=?, updated_at=?, " +
            "created_by_user_id=?, parent_prompt_id=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_subjective_prompts WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluation_subjective_prompts";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM evaluation_subjective_prompts LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM evaluation_subjective_prompts";
    
    private static final String SQL_FIND_BY_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_subjective_prompts WHERE is_active=TRUE AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_VERSION_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_subjective_prompts WHERE version=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_subjective_prompts WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_LATEST_PROMPTS = 
            "SELECT * FROM evaluation_subjective_prompts WHERE deleted_at IS NULL ORDER BY created_at DESC";
    
    private static final String SQL_FIND_BY_NAME_CONTAINING_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_subjective_prompts WHERE name LIKE ? AND deleted_at IS NULL";

    @Autowired
    public EvaluationSubjectivePromptRepository(JdbcTemplate jdbcTemplate, UserRepository UserRepository, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.UserRepository = UserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存主观题评测提示词
     *
     * @param prompt 主观题评测提示词对象
     * @return 带有ID的主观题评测提示词对?
     */
    public EvaluationSubjectivePrompt save(EvaluationSubjectivePrompt prompt) {
        if (prompt.getId() == null) {
            return insert(prompt);
        } else {
            return update(prompt);
        }
    }

    /**
     * 插入新主观题评测提示?
     *
     * @param prompt 主观题评测提示词对象
     * @return 带有ID的主观题评测提示词对?
     */
    private EvaluationSubjectivePrompt insert(EvaluationSubjectivePrompt prompt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, prompt.getName());
            ps.setString(2, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(3, prompt.getDescription());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            try {
                if (prompt.getEvaluationCriteriaFocus() != null) {
                    ps.setString(4, objectMapper.writeValueAsString(prompt.getEvaluationCriteriaFocus()));
                } else {
                    ps.setNull(4, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getScoringInstruction() != null) {
                ps.setString(5, prompt.getScoringInstruction());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getOutputFormatInstruction() != null) {
                ps.setString(6, prompt.getOutputFormatInstruction());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(7, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            
            if (prompt.getVersion() != null) {
                ps.setString(8, prompt.getVersion());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            
            LocalDateTime now = LocalDateTime.now();
            if (prompt.getCreatedAt() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(prompt.getCreatedAt()));
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(now));
            }
            
            if (prompt.getUpdatedAt() != null) {
                ps.setTimestamp(10, Timestamp.valueOf(prompt.getUpdatedAt()));
            } else {
                ps.setTimestamp(10, Timestamp.valueOf(now));
            }
            
            if (prompt.getCreatedByUser() != null && prompt.getCreatedByUser().getId() != null) {
                ps.setLong(11, prompt.getCreatedByUser().getId());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
            }
            
            if (prompt.getParentPrompt() != null && prompt.getParentPrompt().getId() != null) {
                ps.setLong(12, prompt.getParentPrompt().getId());
            } else {
                ps.setNull(12, java.sql.Types.BIGINT);
            }
            
            if (prompt.getCreatedChangeLog() != null && prompt.getCreatedChangeLog().getId() != null) {
                ps.setLong(13, prompt.getCreatedChangeLog().getId());
            } else {
                ps.setNull(13, java.sql.Types.BIGINT);
            }
            
            if (prompt.getDeletedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(prompt.getDeletedAt()));
            } else {
                ps.setNull(14, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            prompt.setId(key.longValue());
        }

        return prompt;
    }

    /**
     * 更新主观题评测提示词
     *
     * @param prompt 主观题评测提示词对象
     * @return 更新后的主观题评测提示词对象
     */
    private EvaluationSubjectivePrompt update(EvaluationSubjectivePrompt prompt) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            ps.setString(1, prompt.getName());
            ps.setString(2, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(3, prompt.getDescription());
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            
            try {
                if (prompt.getEvaluationCriteriaFocus() != null) {
                    ps.setString(4, objectMapper.writeValueAsString(prompt.getEvaluationCriteriaFocus()));
                } else {
                    ps.setNull(4, java.sql.Types.VARCHAR);
                }
            } catch (Exception e) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getScoringInstruction() != null) {
                ps.setString(5, prompt.getScoringInstruction());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getOutputFormatInstruction() != null) {
                ps.setString(6, prompt.getOutputFormatInstruction());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(7, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            
            if (prompt.getVersion() != null) {
                ps.setString(8, prompt.getVersion());
            } else {
                ps.setNull(8, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getCreatedAt() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(prompt.getCreatedAt()));
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            
            if (prompt.getCreatedByUser() != null && prompt.getCreatedByUser().getId() != null) {
                ps.setLong(11, prompt.getCreatedByUser().getId());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
            }
            
            if (prompt.getParentPrompt() != null && prompt.getParentPrompt().getId() != null) {
                ps.setLong(12, prompt.getParentPrompt().getId());
            } else {
                ps.setNull(12, java.sql.Types.BIGINT);
            }
            
            if (prompt.getCreatedChangeLog() != null && prompt.getCreatedChangeLog().getId() != null) {
                ps.setLong(13, prompt.getCreatedChangeLog().getId());
            } else {
                ps.setNull(13, java.sql.Types.BIGINT);
            }
            
            if (prompt.getDeletedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(prompt.getDeletedAt()));
            } else {
                ps.setNull(14, java.sql.Types.TIMESTAMP);
            }
            
            ps.setLong(15, prompt.getId());
            
            return ps;
        });

        return prompt;
    }

    /**
     * 根据ID查找主观题评测提示词
     *
     * @param id 主观题评测提示词ID
     * @return 主观题评测提示词对象（可选）
     */
    public Optional<EvaluationSubjectivePrompt> findById(Long id) {
        try {
            EvaluationSubjectivePrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new EvaluationSubjectivePromptRowMapper(),
                id
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有主观题评测提示?
     *
     * @return 主观题评测提示词列表
     */
    public List<EvaluationSubjectivePrompt> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationSubjectivePromptRowMapper());
    }

    /**
     * 分页查找所有主观题评测提示?
     *
     * @param pageable 分页参数
     * @return 主观题评测提示词分页结果
     */
    public Page<EvaluationSubjectivePrompt> findAll(Pageable pageable) {
        List<EvaluationSubjectivePrompt> prompts = jdbcTemplate.query(
            SQL_FIND_ALL_PAGEABLE,
            new EvaluationSubjectivePromptRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        Integer total = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(prompts, pageable, total != null ? total : 0);
    }

    /**
     * 查找所有激活状态的主观题评测提示词
     *
     * @return 主观题评测提示词列表
     */
    public List<EvaluationSubjectivePrompt> findByIsActiveTrueAndDeletedAtIsNull() {
        return jdbcTemplate.query(
            SQL_FIND_BY_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL,
            new EvaluationSubjectivePromptRowMapper()
        );
    }

    /**
     * 按版本号查询主观题评测提示词
     *
     * @param version 版本?
     * @return 主观题评测提示词对象（可选）
     */
    public Optional<EvaluationSubjectivePrompt> findByVersionAndDeletedAtIsNull(String version) {
        try {
            EvaluationSubjectivePrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_VERSION_AND_DELETED_AT_IS_NULL,
                new EvaluationSubjectivePromptRowMapper(),
                version
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有未删除的主观题评测提示?
     *
     * @return 主观题评测提示词列表
     */
    public List<EvaluationSubjectivePrompt> findByDeletedAtIsNull() {
        return jdbcTemplate.query(
            SQL_FIND_BY_DELETED_AT_IS_NULL,
            new EvaluationSubjectivePromptRowMapper()
        );
    }

    /**
     * 查找最新版本的主观题评测提示词
     *
     * @return 主观题评测提示词列表
     */
    public List<EvaluationSubjectivePrompt> findLatestPrompts() {
        return jdbcTemplate.query(
            SQL_FIND_LATEST_PROMPTS,
            new EvaluationSubjectivePromptRowMapper()
        );
    }

    /**
     * 按名称查找主观题评测提示?
     *
     * @param name 名称
     * @return 主观题评测提示词列表
     */
    public List<EvaluationSubjectivePrompt> findByNameContainingAndDeletedAtIsNull(String name) {
        return jdbcTemplate.query(
            SQL_FIND_BY_NAME_CONTAINING_AND_DELETED_AT_IS_NULL,
            new EvaluationSubjectivePromptRowMapper(),
            "%" + name + "%"
        );
    }

    /**
     * 删除主观题评测提示词
     *
     * @param id 主观题评测提示词ID
     */
    public void deleteById(Long id) {
        // 实现软删?
        jdbcTemplate.update(
            "UPDATE evaluation_subjective_prompts SET deleted_at=? WHERE id=?",
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    /**
     * 主观题评测提示词行映射器
     */
    private class EvaluationSubjectivePromptRowMapper implements RowMapper<EvaluationSubjectivePrompt> {
        @Override
        public EvaluationSubjectivePrompt mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationSubjectivePrompt prompt = new EvaluationSubjectivePrompt();
            
            prompt.setId(rs.getLong("id"));
            prompt.setName(rs.getString("name"));
            prompt.setPromptTemplate(rs.getString("prompt_template"));
            prompt.setDescription(rs.getString("description"));
            
            // 处理JSON类型的evaluationCriteriaFocus字段
            String evaluationCriteriaFocusJson = rs.getString("evaluation_criteria_focus");
            if (evaluationCriteriaFocusJson != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evaluationCriteriaFocus = objectMapper.readValue(evaluationCriteriaFocusJson, Map.class);
                    prompt.setEvaluationCriteriaFocus(evaluationCriteriaFocus);
                } catch (Exception e) {
                    // 忽略解析错误，保持evaluationCriteriaFocus为null
                }
            }
            
            prompt.setScoringInstruction(rs.getString("scoring_instruction"));
            prompt.setOutputFormatInstruction(rs.getString("output_format_instruction"));
            prompt.setIsActive(rs.getBoolean("is_active"));
            prompt.setVersion(rs.getString("version"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                prompt.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                prompt.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            // 设置创建者用?
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                UserRepository.findById(createdByUserId).ifPresent(user -> prompt.setCreatedByUser(user));
            }
            
            // 设置父提示词
            Long parentPromptId = rs.getLong("parent_prompt_id");
            if (!rs.wasNull()) {
                EvaluationSubjectivePrompt parentPrompt = new EvaluationSubjectivePrompt();
                parentPrompt.setId(parentPromptId);
                prompt.setParentPrompt(parentPrompt);
                
                // 注意：这里不递归加载父提示词的完整信息，以避免无限循?
            }
            
            // 设置创建变更日志
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                prompt.setCreatedChangeLog(changeLog);
            }
            
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                prompt.setDeletedAt(deletedAt.toLocalDateTime());
            }
            
            return prompt;
        }
    }
} 
