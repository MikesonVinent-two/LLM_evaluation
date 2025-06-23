package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.EvaluationTagPrompt;
import com.example.demo.entity.jdbc.Tag;
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
import java.util.Optional;

/**
 * 基于JDBC的评测标签提示词仓库实现
 */
@Repository
public class EvaluationTagPromptRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TagRepository TagRepository;
    private final UserRepository UserRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_tag_prompts (tag_id, name, prompt_template, description, is_active, " +
            "prompt_priority, version, created_at, updated_at, created_by_user_id, parent_prompt_id, " +
            "created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_tag_prompts SET tag_id=?, name=?, prompt_template=?, description=?, is_active=?, " +
            "prompt_priority=?, version=?, created_at=?, updated_at=?, created_by_user_id=?, parent_prompt_id=?, " +
            "created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_tag_prompts WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluation_tag_prompts";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM evaluation_tag_prompts LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM evaluation_tag_prompts";
    
    private static final String SQL_FIND_BY_TAG_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL_ORDER_BY_PROMPT_PRIORITY_ASC = 
            "SELECT * FROM evaluation_tag_prompts WHERE tag_id=? AND is_active=TRUE AND deleted_at IS NULL " +
            "ORDER BY prompt_priority ASC";
    
    private static final String SQL_FIND_BY_TAG_ID_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL_ORDER_BY_PROMPT_PRIORITY_ASC = 
            "SELECT * FROM evaluation_tag_prompts WHERE tag_id=? AND is_active=TRUE AND deleted_at IS NULL " +
            "ORDER BY prompt_priority ASC";
    
    private static final String SQL_FIND_BY_TAG_ID_AND_VERSION_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_tag_prompts WHERE tag_id=? AND version=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_DELETED_AT_IS_NULL = 
            "SELECT * FROM evaluation_tag_prompts WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_LATEST_BY_TAG_ID = 
            "SELECT * FROM evaluation_tag_prompts WHERE tag_id=? AND deleted_at IS NULL ORDER BY created_at DESC";

    @Autowired
    public EvaluationTagPromptRepository(JdbcTemplate jdbcTemplate, TagRepository TagRepository, UserRepository UserRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.TagRepository = TagRepository;
        this.UserRepository = UserRepository;
    }

    /**
     * 保存评测标签提示?
     *
     * @param prompt 评测标签提示词对?
     * @return 带有ID的评测标签提示词对象
     */
    public EvaluationTagPrompt save(EvaluationTagPrompt prompt) {
        if (prompt.getId() == null) {
            return insert(prompt);
        } else {
            return update(prompt);
        }
    }

    /**
     * 插入新评测标签提示词
     *
     * @param prompt 评测标签提示词对?
     * @return 带有ID的评测标签提示词对象
     */
    private EvaluationTagPrompt insert(EvaluationTagPrompt prompt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标签ID
            if (prompt.getTag() != null && prompt.getTag().getId() != null) {
                ps.setLong(1, prompt.getTag().getId());
            } else {
                throw new IllegalArgumentException("评测标签提示词必须关联一标签?");
            }
            
            ps.setString(2, prompt.getName());
            ps.setString(3, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(4, prompt.getDescription());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(5, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            ps.setInt(6, prompt.getPromptPriority() != null ? prompt.getPromptPriority() : 50);
            
            if (prompt.getVersion() != null) {
                ps.setString(7, prompt.getVersion());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            LocalDateTime now = LocalDateTime.now();
            if (prompt.getCreatedAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(prompt.getCreatedAt()));
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(now));
            }
            
            if (prompt.getUpdatedAt() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(prompt.getUpdatedAt()));
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(now));
            }
            
            if (prompt.getCreatedByUser() != null && prompt.getCreatedByUser().getId() != null) {
                ps.setLong(10, prompt.getCreatedByUser().getId());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            
            if (prompt.getParentPrompt() != null && prompt.getParentPrompt().getId() != null) {
                ps.setLong(11, prompt.getParentPrompt().getId());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
            }
            
            if (prompt.getCreatedChangeLog() != null && prompt.getCreatedChangeLog().getId() != null) {
                ps.setLong(12, prompt.getCreatedChangeLog().getId());
            } else {
                ps.setNull(12, java.sql.Types.BIGINT);
            }
            
            if (prompt.getDeletedAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(prompt.getDeletedAt()));
            } else {
                ps.setNull(13, java.sql.Types.TIMESTAMP);
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
     * 更新评测标签提示?
     *
     * @param prompt 评测标签提示词对?
     * @return 更新后的评测标签提示词对?
     */
    private EvaluationTagPrompt update(EvaluationTagPrompt prompt) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置标签ID
            if (prompt.getTag() != null && prompt.getTag().getId() != null) {
                ps.setLong(1, prompt.getTag().getId());
            } else {
                throw new IllegalArgumentException("评测标签提示词必须关联一个标?");
            }
            
            ps.setString(2, prompt.getName());
            ps.setString(3, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(4, prompt.getDescription());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(5, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            ps.setInt(6, prompt.getPromptPriority() != null ? prompt.getPromptPriority() : 50);
            
            if (prompt.getVersion() != null) {
                ps.setString(7, prompt.getVersion());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getCreatedAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(prompt.getCreatedAt()));
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            
            if (prompt.getCreatedByUser() != null && prompt.getCreatedByUser().getId() != null) {
                ps.setLong(10, prompt.getCreatedByUser().getId());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            
            if (prompt.getParentPrompt() != null && prompt.getParentPrompt().getId() != null) {
                ps.setLong(11, prompt.getParentPrompt().getId());
            } else {
                ps.setNull(11, java.sql.Types.BIGINT);
            }
            
            if (prompt.getCreatedChangeLog() != null && prompt.getCreatedChangeLog().getId() != null) {
                ps.setLong(12, prompt.getCreatedChangeLog().getId());
            } else {
                ps.setNull(12, java.sql.Types.BIGINT);
            }
            
            if (prompt.getDeletedAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(prompt.getDeletedAt()));
            } else {
                ps.setNull(13, java.sql.Types.TIMESTAMP);
            }
            
            ps.setLong(14, prompt.getId());
            
            return ps;
        });

        return prompt;
    }

    /**
     * 根据ID查找评测标签提示?
     *
     * @param id 评测标签提示词ID
     * @return 评测标签提示词对象（可选）
     */
    public Optional<EvaluationTagPrompt> findById(Long id) {
        try {
            EvaluationTagPrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new EvaluationTagPromptRowMapper(),
                id
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有评测标签提示词
     *
     * @return 评测标签提示词列?
     */
    public List<EvaluationTagPrompt> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationTagPromptRowMapper());
    }

    /**
     * 分页查找所有评测标签提示词
     *
     * @param pageable 分页参数
     * @return 评测标签提示词分页结?
     */
    public Page<EvaluationTagPrompt> findAll(Pageable pageable) {
        List<EvaluationTagPrompt> prompts = jdbcTemplate.query(
            SQL_FIND_ALL_PAGEABLE,
            new EvaluationTagPromptRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        Integer total = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(prompts, pageable, total != null ? total : 0);
    }

    /**
     * 查找指定标签的所有激活状态的评测提示词，按优先级排序
     *
     * @param tag 标签
     * @return 评测标签提示词列?
     */
    public List<EvaluationTagPrompt> findByTagAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Tag tag) {
        if (tag == null || tag.getId() == null) {
            return List.of();
        }
        
        return jdbcTemplate.query(
            SQL_FIND_BY_TAG_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL_ORDER_BY_PROMPT_PRIORITY_ASC,
            new EvaluationTagPromptRowMapper(),
            tag.getId()
        );
    }

    /**
     * 按标签ID查询激活状态的评测提示?
     *
     * @param tagId 标签ID
     * @return 评测标签提示词列?
     */
    public List<EvaluationTagPrompt> findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Long tagId) {
        return jdbcTemplate.query(
            SQL_FIND_BY_TAG_ID_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL_ORDER_BY_PROMPT_PRIORITY_ASC,
            new EvaluationTagPromptRowMapper(),
            tagId
        );
    }

    /**
     * 按版本号查询特定标签的评测提示词
     *
     * @param tagId 标签ID
     * @param version 版本?
     * @return 评测标签提示词对象（可选）
     */
    public Optional<EvaluationTagPrompt> findByTagIdAndVersionAndDeletedAtIsNull(Long tagId, String version) {
        try {
            EvaluationTagPrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_TAG_ID_AND_VERSION_AND_DELETED_AT_IS_NULL,
                new EvaluationTagPromptRowMapper(),
                tagId, version
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有未删除的标签评测提示词
     *
     * @return 评测标签提示词列?
     */
    public List<EvaluationTagPrompt> findByDeletedAtIsNull() {
        return jdbcTemplate.query(
            SQL_FIND_BY_DELETED_AT_IS_NULL,
            new EvaluationTagPromptRowMapper()
        );
    }

    /**
     * 查找最新版本的标签评测提示?
     *
     * @param tagId 标签ID
     * @return 评测标签提示词列?
     */
    public List<EvaluationTagPrompt> findLatestByTagId(Long tagId) {
        return jdbcTemplate.query(
            SQL_FIND_LATEST_BY_TAG_ID,
            new EvaluationTagPromptRowMapper(),
            tagId
        );
    }

    /**
     * 删除评测标签提示?
     *
     * @param id 评测标签提示词ID
     */
    public void deleteById(Long id) {
        // 实现软删?
        jdbcTemplate.update(
            "UPDATE evaluation_tag_prompts SET deleted_at=? WHERE id=?",
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    /**
     * 评测标签提示词行映射?
     */
    private class EvaluationTagPromptRowMapper implements RowMapper<EvaluationTagPrompt> {
        @Override
        public EvaluationTagPrompt mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationTagPrompt prompt = new EvaluationTagPrompt();
            
            prompt.setId(rs.getLong("id"));
            
            // 设置标签
            Long tagId = rs.getLong("tag_id");
            if (!rs.wasNull()) {
                TagRepository.findById(tagId).ifPresent(tag -> prompt.setTag(tag));
            }
            
            prompt.setName(rs.getString("name"));
            prompt.setPromptTemplate(rs.getString("prompt_template"));
            prompt.setDescription(rs.getString("description"));
            prompt.setIsActive(rs.getBoolean("is_active"));
            prompt.setPromptPriority(rs.getInt("prompt_priority"));
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
                EvaluationTagPrompt parentPrompt = new EvaluationTagPrompt();
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
