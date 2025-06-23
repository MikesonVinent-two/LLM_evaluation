package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardQuestionTag;
import com.example.demo.entity.jdbc.Tag;

/**
 * 基于JDBC的标准问题标签仓库实?
 */
@Repository
public class StandardQuestionTagRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT IGNORE INTO standard_question_tags (standard_question_id, tag_id, created_at, created_by_user_id, created_change_log_id) " +
            "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM standard_question_tags WHERE id=?";
    
    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID = 
            "SELECT * FROM standard_question_tags WHERE standard_question_id=?";
    
    private static final String SQL_FIND_BY_TAG_ID = 
            "SELECT * FROM standard_question_tags WHERE tag_id=?";
    
    private static final String SQL_DELETE_BY_STANDARD_QUESTION_ID_AND_TAG_ID = 
            "DELETE FROM standard_question_tags WHERE standard_question_id=? AND tag_id=?";
    
    private static final String SQL_EXISTS_BY_STANDARD_QUESTION_ID_AND_TAG_ID = 
            "SELECT COUNT(*) FROM standard_question_tags WHERE standard_question_id=? AND tag_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM standard_question_tags";
    
    private static final String SQL_DELETE = 
            "DELETE FROM standard_question_tags WHERE id=?";

    @Autowired
    public StandardQuestionTagRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存标准问题标签
     *
     * @param standardQuestionTag 标准问题标签对象
     * @return 带有ID的标准问题标签对?
     */
    public StandardQuestionTag save(StandardQuestionTag standardQuestionTag) {
        if (standardQuestionTag.getId() == null) {
            // 先删除可能存在的关联
            deleteByStandardQuestionAndTag(
                standardQuestionTag.getStandardQuestion(),
                standardQuestionTag.getTag()
            );
            return insert(standardQuestionTag);
        } else {
            // 由于标准问题标签是简单的关联实体，通常不需要更新，只需插入和删除
            // 如果有需要，可以实现update方法
            return standardQuestionTag;
        }
    }

    /**
     * 插入新标准问题标?
     *
     * @param standardQuestionTag 标准问题标签对象
     * @return 带有ID的标准问题标签对?
     */
    private StandardQuestionTag insert(StandardQuestionTag standardQuestionTag) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标准问题ID
            ps.setLong(1, standardQuestionTag.getStandardQuestion().getId());
            
            // 设置标签ID
            ps.setLong(2, standardQuestionTag.getTag().getId());
            
            // 设置创建者用户ID
            if (standardQuestionTag.getCreatedByUser() != null && standardQuestionTag.getCreatedByUser().getId() != null) {
                ps.setLong(3, standardQuestionTag.getCreatedByUser().getId());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (standardQuestionTag.getCreatedChangeLog() != null && standardQuestionTag.getCreatedChangeLog().getId() != null) {
                ps.setLong(4, standardQuestionTag.getCreatedChangeLog().getId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            standardQuestionTag.setId(key.longValue());
        }

        return standardQuestionTag;
    }

    /**
     * 根据ID查找标准问题标签
     *
     * @param id 标准问题标签ID
     * @return 标准问题标签对象
     */
    public Optional<StandardQuestionTag> findById(Long id) {
        try {
            StandardQuestionTag standardQuestionTag = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new StandardQuestionTagRowMapper(), 
                id
            );
            return Optional.ofNullable(standardQuestionTag);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题查找标准问题标签
     *
     * @param standardQuestion 标准问题对象
     * @return 标准问题标签列表
     */
    public List<StandardQuestionTag> findByStandardQuestion(StandardQuestion standardQuestion) {
        return jdbcTemplate.query(
            SQL_FIND_BY_STANDARD_QUESTION_ID, 
            new StandardQuestionTagRowMapper(), 
            standardQuestion.getId()
        );
    }

    /**
     * 根据标签查找标准问题标签
     *
     * @param tag 标签对象
     * @return 标准问题标签列表
     */
    public List<StandardQuestionTag> findByTag(Tag tag) {
        return jdbcTemplate.query(
            SQL_FIND_BY_TAG_ID, 
            new StandardQuestionTagRowMapper(), 
            tag.getId()
        );
    }

    /**
     * 根据标准问题和标签删除标准问题标?
     *
     * @param standardQuestion 标准问题对象
     * @param tag 标签对象
     */
    public void deleteByStandardQuestionAndTag(StandardQuestion standardQuestion, Tag tag) {
        jdbcTemplate.update(
            SQL_DELETE_BY_STANDARD_QUESTION_ID_AND_TAG_ID, 
            standardQuestion.getId(), 
            tag.getId()
        );
    }

    /**
     * 检查标准问题和标签的关联是否存?
     *
     * @param standardQuestion 标准问题对象
     * @param tag 标签对象
     * @return 是否存在
     */
    public boolean existsByStandardQuestionAndTag(StandardQuestion standardQuestion, Tag tag) {
        Integer count = jdbcTemplate.queryForObject(
            SQL_EXISTS_BY_STANDARD_QUESTION_ID_AND_TAG_ID, 
            Integer.class, 
            standardQuestion.getId(), 
            tag.getId()
        );
        return count != null && count > 0;
    }

    /**
     * 查找所有标准问题标?
     *
     * @return 所有标准问题标签列?
     */
    public List<StandardQuestionTag> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new StandardQuestionTagRowMapper());
    }

    /**
     * 删除标准问题标签
     *
     * @param standardQuestionTag 标准问题标签对象
     */
    public void delete(StandardQuestionTag standardQuestionTag) {
        jdbcTemplate.update(SQL_DELETE, standardQuestionTag.getId());
    }

    /**
     * 批量保存标准问题标签
     *
     * @param questionTags 标准问题标签对象列表
     * @return 保存后的标准问题标签列表
     */
    public List<StandardQuestionTag> saveAll(List<StandardQuestionTag> questionTags) {
        if (questionTags == null || questionTags.isEmpty()) {
            return questionTags;
        }
        
        for (int i = 0; i < questionTags.size(); i++) {
            questionTags.set(i, save(questionTags.get(i)));
        }
        
        return questionTags;
    }
    
    /**
     * 批量删除标准问题标签
     *
     * @param questionTags 标准问题标签对象列表
     */
    public void deleteAll(List<StandardQuestionTag> questionTags) {
        if (questionTags == null || questionTags.isEmpty()) {
            return;
        }
        
        for (StandardQuestionTag tag : questionTags) {
            delete(tag);
        }
    }

    /**
     * 删除指定标准问题ID的所有标签关联
     *
     * @param standardQuestionId 标准问题ID
     */
    public void deleteByStandardQuestionId(Long standardQuestionId) {
        jdbcTemplate.update(
            "DELETE FROM standard_question_tags WHERE standard_question_id = ?", 
            standardQuestionId
        );
    }

    /**
     * 标准问题标签行映射器
     */
    private class StandardQuestionTagRowMapper implements RowMapper<StandardQuestionTag> {
        @Override
        public StandardQuestionTag mapRow(ResultSet rs, int rowNum) throws SQLException {
            StandardQuestionTag standardQuestionTag = new StandardQuestionTag();
            
            // 设置ID
            standardQuestionTag.setId(rs.getLong("id"));
            
            // 设置标准问题
            Long standardQuestionId = rs.getLong("standard_question_id");
            StandardQuestion standardQuestion = new StandardQuestion();
            standardQuestion.setId(standardQuestionId);
            standardQuestionTag.setStandardQuestion(standardQuestion);
            
            // 设置标签
            Long tagId = rs.getLong("tag_id");
            Tag tag = new Tag();
            tag.setId(tagId);
            standardQuestionTag.setTag(tag);
            
            // 设置创建时间
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                standardQuestionTag.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            // 设置创建者用?
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId).ifPresent(user -> standardQuestionTag.setCreatedByUser(user));
            }
            
            // 设置创建变更日志
            Long createdChangeLogId = rs.getLong("created_change_log_id");
            if (!rs.wasNull()) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId(createdChangeLogId);
                standardQuestionTag.setCreatedChangeLog(changeLog);
            }
            
            return standardQuestionTag;
        }
    }
} 
