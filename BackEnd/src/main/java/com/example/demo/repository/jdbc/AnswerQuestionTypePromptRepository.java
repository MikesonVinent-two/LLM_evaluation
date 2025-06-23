package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.QuestionType;
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
 * 基于JDBC的回答题型提示词仓库实现
 */
@Repository
public class AnswerQuestionTypePromptRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO answer_question_type_prompts (name, question_type, prompt_template, description, is_active, " +
            "response_format_instruction, response_example, version, created_at, updated_at, " +
            "created_by_user_id, parent_prompt_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE answer_question_type_prompts SET name=?, question_type=?, prompt_template=?, description=?, is_active=?, " +
            "response_format_instruction=?, response_example=?, version=?, created_at=?, updated_at=?, " +
            "created_by_user_id=?, parent_prompt_id=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM answer_question_type_prompts WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM answer_question_type_prompts";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM answer_question_type_prompts LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM answer_question_type_prompts";
    
    private static final String SQL_FIND_BY_QUESTION_TYPE_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM answer_question_type_prompts WHERE question_type=? AND is_active=TRUE AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_QUESTION_TYPE_AND_VERSION_AND_DELETED_AT_IS_NULL = 
            "SELECT * FROM answer_question_type_prompts WHERE question_type=? AND version=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_DELETED_AT_IS_NULL = 
            "SELECT * FROM answer_question_type_prompts WHERE deleted_at IS NULL";
    
    private static final String SQL_FIND_LATEST_BY_QUESTION_TYPE = 
            "SELECT * FROM answer_question_type_prompts WHERE question_type=? AND deleted_at IS NULL ORDER BY created_at DESC";
    
    private static final String SQL_FIND_ALL_ACTIVE_GROUP_BY_QUESTION_TYPE = 
            "SELECT * FROM answer_question_type_prompts WHERE is_active=TRUE AND deleted_at IS NULL ORDER BY question_type, created_at DESC";

    @Autowired
    public AnswerQuestionTypePromptRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存回答题型提示?
     *
     * @param prompt 回答题型提示词对?
     * @return 带有ID的回答题型提示词对象
     */
    public AnswerQuestionTypePrompt save(AnswerQuestionTypePrompt prompt) {
        if (prompt.getId() == null) {
            return insert(prompt);
        } else {
            return update(prompt);
        }
    }

    /**
     * 插入新回答题型提示词
     *
     * @param prompt 回答题型提示词对?
     * @return 带有ID的回答题型提示词对象
     */
    private AnswerQuestionTypePrompt insert(AnswerQuestionTypePrompt prompt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, prompt.getName());
            
            if (prompt.getQuestionType() != null) {
                ps.setString(2, prompt.getQuestionType().name());
            } else {
                throw new IllegalArgumentException("回答题型提示词必须指定题型");
            }
            
            ps.setString(3, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(4, prompt.getDescription());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(5, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            
            if (prompt.getResponseFormatInstruction() != null) {
                ps.setString(6, prompt.getResponseFormatInstruction());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getResponseExample() != null) {
                ps.setString(7, prompt.getResponseExample());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
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
     * 更新回答题型提示?
     *
     * @param prompt 回答题型提示词对?
     * @return 更新后的回答题型提示词对?
     */
    private AnswerQuestionTypePrompt update(AnswerQuestionTypePrompt prompt) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            ps.setString(1, prompt.getName());
            
            if (prompt.getQuestionType() != null) {
                ps.setString(2, prompt.getQuestionType().name());
            } else {
                throw new IllegalArgumentException("回答题型提示词必须指定题型");
            }
            
            ps.setString(3, prompt.getPromptTemplate());
            
            if (prompt.getDescription() != null) {
                ps.setString(4, prompt.getDescription());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            ps.setBoolean(5, prompt.getIsActive() != null ? prompt.getIsActive() : true);
            
            if (prompt.getResponseFormatInstruction() != null) {
                ps.setString(6, prompt.getResponseFormatInstruction());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            
            if (prompt.getResponseExample() != null) {
                ps.setString(7, prompt.getResponseExample());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
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
     * 根据ID查找回答题型提示?
     *
     * @param id 回答题型提示词ID
     * @return 回答题型提示词对象（可选）
     */
    public Optional<AnswerQuestionTypePrompt> findById(Long id) {
        try {
            AnswerQuestionTypePrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new AnswerQuestionTypePromptRowMapper(),
                id
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有回答题型提示词
     *
     * @return 回答题型提示词列?
     */
    public List<AnswerQuestionTypePrompt> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new AnswerQuestionTypePromptRowMapper());
    }

    /**
     * 分页查找所有回答题型提示词
     *
     * @param pageable 分页参数
     * @return 回答题型提示词分页结?
     */
    public Page<AnswerQuestionTypePrompt> findAll(Pageable pageable) {
        List<AnswerQuestionTypePrompt> prompts = jdbcTemplate.query(
            SQL_FIND_ALL_PAGEABLE,
            new AnswerQuestionTypePromptRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        Integer total = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(prompts, pageable, total != null ? total : 0);
    }

    /**
     * 按题型查找激活状态的提示?
     *
     * @param questionType 题型
     * @return 回答题型提示词列?
     */
    public List<AnswerQuestionTypePrompt> findByQuestionTypeAndIsActiveTrueAndDeletedAtIsNull(QuestionType questionType) {
        return jdbcTemplate.query(
            SQL_FIND_BY_QUESTION_TYPE_AND_IS_ACTIVE_TRUE_AND_DELETED_AT_IS_NULL,
            new AnswerQuestionTypePromptRowMapper(),
            questionType.name()
        );
    }

    /**
     * 按版本号查询特定题型的提示词
     *
     * @param questionType 题型
     * @param version 版本?
     * @return 回答题型提示词对象（可选）
     */
    public Optional<AnswerQuestionTypePrompt> findByQuestionTypeAndVersionAndDeletedAtIsNull(QuestionType questionType, String version) {
        try {
            AnswerQuestionTypePrompt prompt = jdbcTemplate.queryForObject(
                SQL_FIND_BY_QUESTION_TYPE_AND_VERSION_AND_DELETED_AT_IS_NULL,
                new AnswerQuestionTypePromptRowMapper(),
                questionType.name(), version
            );
            return Optional.ofNullable(prompt);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有未删除的题型提示词
     *
     * @return 回答题型提示词列?
     */
    public List<AnswerQuestionTypePrompt> findByDeletedAtIsNull() {
        return jdbcTemplate.query(
            SQL_FIND_BY_DELETED_AT_IS_NULL,
            new AnswerQuestionTypePromptRowMapper()
        );
    }

    /**
     * 查找最新版本的题型提示?
     *
     * @param questionType 题型
     * @return 回答题型提示词列?
     */
    public List<AnswerQuestionTypePrompt> findLatestByQuestionType(QuestionType questionType) {
        return jdbcTemplate.query(
            SQL_FIND_LATEST_BY_QUESTION_TYPE,
            new AnswerQuestionTypePromptRowMapper(),
            questionType.name()
        );
    }

    /**
     * 查找所有已启用的题型提示词，按题型分组
     *
     * @return 回答题型提示词列?
     */
    public List<AnswerQuestionTypePrompt> findAllActiveGroupByQuestionType() {
        return jdbcTemplate.query(
            SQL_FIND_ALL_ACTIVE_GROUP_BY_QUESTION_TYPE,
            new AnswerQuestionTypePromptRowMapper()
        );
    }

    /**
     * 删除回答题型提示?
     *
     * @param id 回答题型提示词ID
     */
    public void deleteById(Long id) {
        // 实现软删?
        jdbcTemplate.update(
            "UPDATE answer_question_type_prompts SET deleted_at=? WHERE id=?",
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    /**
     * 回答题型提示词行映射?
     */
    private class AnswerQuestionTypePromptRowMapper implements RowMapper<AnswerQuestionTypePrompt> {
        @Override
        public AnswerQuestionTypePrompt mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
            
            prompt.setId(rs.getLong("id"));
            prompt.setName(rs.getString("name"));
            
            String questionTypeStr = rs.getString("question_type");
            if (questionTypeStr != null) {
                prompt.setQuestionType(QuestionType.valueOf(questionTypeStr));
            }
            
            prompt.setPromptTemplate(rs.getString("prompt_template"));
            prompt.setDescription(rs.getString("description"));
            prompt.setIsActive(rs.getBoolean("is_active"));
            prompt.setResponseFormatInstruction(rs.getString("response_format_instruction"));
            prompt.setResponseExample(rs.getString("response_example"));
            prompt.setVersion(rs.getString("version"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                prompt.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                prompt.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            // 设置创建用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(createdByUserId);
                prompt.setCreatedByUser(user);
                
                // 可选：加载完整的用户信?
                userRepository.findById(createdByUserId).ifPresent(prompt::setCreatedByUser);
            }
            
            // 设置父提示词
            Long parentPromptId = rs.getLong("parent_prompt_id");
            if (!rs.wasNull()) {
                AnswerQuestionTypePrompt parentPrompt = new AnswerQuestionTypePrompt();
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
