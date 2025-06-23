package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.demo.entity.jdbc.DifficultyLevel;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardQuestionTag;
import com.example.demo.entity.jdbc.Tag;

/**
 * 基于JDBC的标准问题仓库实现
 */
@Repository
public class StandardQuestionRepository {

    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final StandardQuestionTagRepository standardQuestionTagRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO standard_questions (original_raw_question_id, question_text, question_type, difficulty, " +
            "creation_time, created_by_user_id, parent_standard_question_id, created_change_log_id, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE standard_questions SET original_raw_question_id=?, question_text=?, question_type=?, difficulty=?, " +
            "created_by_user_id=?, parent_standard_question_id=?, created_change_log_id=?, deleted_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT sq.*, t.id as tag_id, t.tag_name, t.tag_type " +
            "FROM standard_questions sq " +
            "LEFT JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id " +
            "LEFT JOIN tags t ON sqt.tag_id = t.id " +
            "WHERE sq.id = ? AND sq.deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL = 
            "SELECT sq.*, t.id as tag_id, t.tag_name, t.tag_type " +
            "FROM standard_questions sq " +
            "LEFT JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id " +
            "LEFT JOIN tags t ON sqt.tag_id = t.id " +
            "WHERE sq.deleted_at IS NULL";
    
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT sq.*, t.id as tag_id, t.tag_name, t.tag_type " +
            "FROM standard_questions sq " +
            "LEFT JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id " +
            "LEFT JOIN tags t ON sqt.tag_id = t.id " +
            "WHERE sq.deleted_at IS NULL " +
            "LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM standard_questions WHERE deleted_at IS NULL";
    
    private static final String SQL_SOFT_DELETE = 
            "UPDATE standard_questions SET deleted_at=? WHERE id=?";
    
    private static final String SQL_FIND_DISTINCT_ORIGINAL_RAW_QUESTION_IDS = 
            "SELECT DISTINCT original_raw_question_id FROM standard_questions " +
            "WHERE original_raw_question_id IS NOT NULL AND deleted_at IS NULL";
    
    private static final String SQL_FIND_FIRST_BY_ORIGINAL_RAW_QUESTION_ID_ORDER_BY_CREATION_TIME_DESC = 
            "SELECT * FROM standard_questions " +
            "WHERE original_raw_question_id=? AND deleted_at IS NULL " +
            "ORDER BY creation_time DESC LIMIT 1";
    
    private static final String SQL_FIND_BY_PARENT_STANDARD_QUESTION_ID = 
            "SELECT * FROM standard_questions " +
            "WHERE parent_standard_question_id=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_ORIGINAL_RAW_QUESTION_ID = 
            "SELECT * FROM standard_questions " +
            "WHERE original_raw_question_id=? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_DATASET_VERSION_ID = 
            "SELECT sq.* FROM standard_questions sq " +
            "JOIN dataset_question_mapping dqm ON sq.id = dqm.standard_question_id " +
            "WHERE dqm.dataset_version_id=? AND sq.deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_DATASET_VERSION_ID_WITH_TAGS = 
            "SELECT DISTINCT sq.* FROM standard_questions sq " +
            "JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id " +
            "JOIN tags t ON sqt.tag_id = t.id " +
            "JOIN dataset_question_mapping dqm ON sq.id = dqm.standard_question_id " +
            "WHERE dqm.dataset_version_id=? AND sq.deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_QUESTION_TEXT_CONTAINING = 
            "SELECT * FROM standard_questions " +
            "WHERE question_text LIKE ? AND deleted_at IS NULL";
    
    private static final String SQL_FIND_LATEST_VERSIONS = 
            "SELECT sq.*, t.id as tag_id, t.tag_name, t.tag_type " +
            "FROM standard_questions sq " +
            "LEFT JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id " +
            "LEFT JOIN tags t ON sqt.tag_id = t.id " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM standard_questions child " +
            "  WHERE child.parent_standard_question_id = sq.id AND child.deleted_at IS NULL" +
            ") AND sq.deleted_at IS NULL " +
            "LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_LATEST_VERSIONS = 
            "SELECT COUNT(*) FROM standard_questions sq " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM standard_questions child " +
            "  WHERE child.parent_standard_question_id = sq.id AND child.deleted_at IS NULL" +
            ") AND sq.deleted_at IS NULL";
    
    private static final String SQL_FIND_BY_IDS_WITH_DATASET_MAPPINGS = 
            "SELECT DISTINCT sq.* FROM standard_questions sq " +
            "JOIN dataset_question_mapping dqm ON sq.id = dqm.standard_question_id " +
            "WHERE sq.id IN (%s) AND sq.deleted_at IS NULL";

    @Autowired
    public StandardQuestionRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository, StandardQuestionTagRepository standardQuestionTagRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.standardQuestionTagRepository = standardQuestionTagRepository;
    }

    /**
     * 保存标准问题
     *
     * @param question 标准问题对象
     * @return 带有ID的标准问题对象
     */
    public StandardQuestion save(StandardQuestion question) {
        if (question.getId() == null) {
            return insert(question);
        } else {
            return update(question);
        }
    }

    /**
     * 插入新标准问题
     *
     * @param question 标准问题对象
     * @return 带有ID的标准问题对象
     */
    private StandardQuestion insert(StandardQuestion question) {
        LocalDateTime now = LocalDateTime.now();
        if (question.getCreationTime() == null) {
            question.setCreationTime(now);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置原始原始问题ID
            if (question.getOriginalRawQuestion() != null && question.getOriginalRawQuestion().getId() != null) {
                ps.setLong(1, question.getOriginalRawQuestion().getId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            
            ps.setString(2, question.getQuestionText());
            ps.setString(3, question.getQuestionType().name());
            
            if (question.getDifficulty() != null) {
                ps.setString(4, question.getDifficulty().name());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            
            ps.setTimestamp(5, Timestamp.valueOf(question.getCreationTime()));
            
            // 设置创建用户ID
            if (question.getCreatedByUser() != null && question.getCreatedByUser().getId() != null) {
                ps.setLong(6, question.getCreatedByUser().getId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            
            // 设置父标准问题ID
            if (question.getParentStandardQuestion() != null && question.getParentStandardQuestion().getId() != null) {
                ps.setLong(7, question.getParentStandardQuestion().getId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            
            // 设置创建变更日志ID
            if (question.getCreatedChangeLog() != null && question.getCreatedChangeLog().getId() != null) {
                ps.setLong(8, question.getCreatedChangeLog().getId());
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }
            
            // 设置删除时间
            if (question.getDeletedAt() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(question.getDeletedAt()));
            } else {
                ps.setNull(9, java.sql.Types.TIMESTAMP);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            question.setId(key.longValue());
        }
        return question;
    }

    /**
     * 更新标准问题
     *
     * @param question 标准问题对象
     * @return 更新后的标准问题对象
     */
    private StandardQuestion update(StandardQuestion question) {
        jdbcTemplate.update(SQL_UPDATE,
                question.getOriginalRawQuestion() != null ? question.getOriginalRawQuestion().getId() : null,
                question.getQuestionText(),
                question.getQuestionType().name(),
                question.getDifficulty() != null ? question.getDifficulty().name() : null,
                question.getCreatedByUser() != null ? question.getCreatedByUser().getId() : null,
                question.getParentStandardQuestion() != null ? question.getParentStandardQuestion().getId() : null,
                question.getCreatedChangeLog() != null ? question.getCreatedChangeLog().getId() : null,
                question.getDeletedAt(),
                question.getId());

        return question;
    }

    /**
     * 根据ID查找标准问题
     *
     * @param id 标准问题ID
     * @return 标准问题的Optional包装
     */
    public Optional<StandardQuestion> findById(Long id) {
        try {
            List<StandardQuestion> results = jdbcTemplate.query(SQL_FIND_BY_ID, new Object[]{id}, new StandardQuestionRowMapper());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 查找所有标准问题
     *
     * @return 标准问题列表
     */
    public List<StandardQuestion> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new StandardQuestionRowMapper());
    }
    
    /**
     * 分页查找标准问题
     *
     * @param pageable 分页对象
     * @return 分页标准问题列表
     */
    public Page<StandardQuestion> findAll(Pageable pageable) {
        List<StandardQuestion> questions = jdbcTemplate.query(
                SQL_FIND_ALL_PAGEABLE,
                new Object[]{pageable.getPageSize(), pageable.getOffset()},
                new StandardQuestionRowMapper()
        );
        
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        return new PageImpl<>(questions, pageable, count != null ? count : 0);
    }
    
    /**
     * 软删除标准问题
     *
     * @param id 标准问题ID
     * @return 是否成功
     */
    public boolean softDelete(Long id) {
        int affected = jdbcTemplate.update(SQL_SOFT_DELETE, Timestamp.valueOf(LocalDateTime.now()), id);
        return affected > 0;
    }
    
    /**
     * 获取所有已标准化的原始问题ID
     *
     * @return 原始问题ID列表
     */
    public List<Long> findDistinctOriginalRawQuestionIds() {
        return jdbcTemplate.queryForList(SQL_FIND_DISTINCT_ORIGINAL_RAW_QUESTION_IDS, Long.class);
    }
    
    /**
     * 根据原始问题ID查找最新的标准问题
     *
     * @param rawQuestionId 原始问题ID
     * @return 标准问题的Optional包装
     */
    public Optional<StandardQuestion> findFirstByOriginalRawQuestionIdOrderByCreationTimeDesc(Long rawQuestionId) {
        try {
            StandardQuestion question = jdbcTemplate.queryForObject(
                    SQL_FIND_FIRST_BY_ORIGINAL_RAW_QUESTION_ID_ORDER_BY_CREATION_TIME_DESC,
                    new Object[]{rawQuestionId},
                    new StandardQuestionRowMapperWithoutTags()
            );
            return Optional.ofNullable(question);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据父标准问题ID查找标准问题
     *
     * @param parentId 父标准问题ID
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByParentStandardQuestionId(Long parentId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_PARENT_STANDARD_QUESTION_ID,
                new Object[]{parentId},
                new StandardQuestionRowMapperWithoutTags()
        );
    }
    
    /**
     * 根据原始问题ID查找所有关联的标准问题
     *
     * @param rawQuestionId 原始问题ID
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByOriginalRawQuestionId(Long rawQuestionId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_ORIGINAL_RAW_QUESTION_ID,
                new Object[]{rawQuestionId},
                new StandardQuestionRowMapperWithoutTags()
        );
    }
    
    /**
     * 根据数据集版本ID查找标准问题
     *
     * @param datasetVersionId 数据集版本ID
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByDatasetVersionId(Long datasetVersionId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_DATASET_VERSION_ID,
                new Object[]{datasetVersionId},
                new StandardQuestionRowMapperWithoutTags()
        );
    }
    
    /**
     * 预加载标签的数据集版本问题查询
     *
     * @param datasetVersionId 数据集版本ID
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByDatasetVersionIdWithTags(Long datasetVersionId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_DATASET_VERSION_ID_WITH_TAGS,
                new Object[]{datasetVersionId},
                new StandardQuestionRowMapper()
        );
    }
    
    /**
     * 根据问题文本内容查找标准问题
     *
     * @param questionText 问题文本
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByQuestionTextContaining(String questionText) {
        return jdbcTemplate.query(
                SQL_FIND_BY_QUESTION_TEXT_CONTAINING,
                new Object[]{"%" + questionText + "%"},
                new StandardQuestionRowMapperWithoutTags()
        );
    }
    
    /**
     * 查找所有最新版本的标准问题（没有子问题的问题，即版本树的叶子节点）
     *
     * @param pageable 分页对象
     * @return 分页标准问题列表
     */
    public Page<StandardQuestion> findLatestVersions(Pageable pageable) {
        List<StandardQuestion> questions = jdbcTemplate.query(
                SQL_FIND_LATEST_VERSIONS,
                new Object[]{pageable.getPageSize(), pageable.getOffset()},
                new StandardQuestionRowMapper()
        );
        
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_LATEST_VERSIONS, Integer.class);
        return new PageImpl<>(questions, pageable, count != null ? count : 0);
    }
    
    /**
     * 预加载数据集映射的查询
     *
     * @param questionIds 问题ID列表
     * @return 标准问题列表
     */
    public List<StandardQuestion> findByIdsWithDatasetMappings(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建 IN 语句所需的问号占位符
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < questionIds.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        
        String sql = String.format(SQL_FIND_BY_IDS_WITH_DATASET_MAPPINGS, placeholders.toString());
        return jdbcTemplate.query(sql, questionIds.toArray(), new StandardQuestionRowMapperWithoutTags());
    }

    /**
     * 根据标签列表查询标准问题
     * 这个方法会返回包含所有指定标签的标准问题
     * 
     * @param tagNames 标签名称列表
     * @return 满足条件的标准问题列表
     */
    public List<StandardQuestion> findByAllTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建查询SQL
        // 使用GROUP BY和HAVING COUNT确保问题包含所有指定的标签
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT sq.* FROM standard_questions sq ")
           .append("JOIN standard_question_tags sqt ON sq.id = sqt.standard_question_id ")
           .append("JOIN tags t ON sqt.tag_id = t.id ")
           .append("WHERE LOWER(t.tag_name) IN (");
        
        // 添加标签名占位符
        for (int i = 0; i < tagNames.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("LOWER(?)");
        }
        
        sql.append(") AND sq.deleted_at IS NULL ")
           .append("GROUP BY sq.id ")
           .append("HAVING COUNT(DISTINCT LOWER(t.tag_name)) = ?");
        
        // 准备参数
        Object[] params = new Object[tagNames.size() + 1];
        for (int i = 0; i < tagNames.size(); i++) {
            params[i] = tagNames.get(i);
        }
        // 最后一个参数是标签数量，用于HAVING子句
        params[tagNames.size()] = tagNames.size();
        
        // 执行查询
        return jdbcTemplate.query(sql.toString(), params, new StandardQuestionRowMapperWithoutTags());
    }

    /**
     * 获取所有最新版本的标准问题（没有子问题的问题，即版本树的叶子节点）
     * 
     * @return 最新版本的标准问题列表
     */
    public List<StandardQuestion> findAllLatestVersions() {
        return jdbcTemplate.query(
            "SELECT sq.* FROM standard_questions sq " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM standard_questions child " +
            "  WHERE child.parent_standard_question_id = sq.id AND child.deleted_at IS NULL" +
            ") AND sq.deleted_at IS NULL",
            new StandardQuestionRowMapperWithoutTags()
        );
    }

    /**
     * 检查是否存在指定父ID的标准问题
     * 
     * @param parentId 父标准问题ID
     * @return 是否存在子问题
     */
    public boolean existsByParentStandardQuestionId(Long parentId) {
        if (parentId == null) {
            return false;
        }
        
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM standard_questions " +
            "WHERE parent_standard_question_id = ? AND deleted_at IS NULL",
            new Object[]{parentId},
            Integer.class
        );
        
        return count != null && count > 0;
    }

    /**
     * 标准问题行映射器（不包含标签信息）
     */
    private class StandardQuestionRowMapperWithoutTags implements RowMapper<StandardQuestion> {
        private Map<Long, StandardQuestion> questionMap = new HashMap<>();
        
        @Override
        public StandardQuestion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long questionId = rs.getLong("id");
            StandardQuestion question = questionMap.get(questionId);
            
            if (question == null) {
                final StandardQuestion newQuestion = new StandardQuestion();
                newQuestion.setId(questionId);
                newQuestion.setQuestionText(rs.getString("question_text"));
                
                // 解析枚举
                String questionTypeStr = rs.getString("question_type");
                if (questionTypeStr != null) {
                    newQuestion.setQuestionType(QuestionType.valueOf(questionTypeStr));
                }
                
                String difficultyStr = rs.getString("difficulty");
                if (difficultyStr != null) {
                    newQuestion.setDifficulty(DifficultyLevel.valueOf(difficultyStr));
                }
                
                // 设置时间
                Timestamp creationTime = rs.getTimestamp("creation_time");
                if (creationTime != null) {
                    newQuestion.setCreationTime(creationTime.toLocalDateTime());
                }
                
                Timestamp deletedAt = rs.getTimestamp("deleted_at");
                if (deletedAt != null) {
                    newQuestion.setDeletedAt(deletedAt.toLocalDateTime());
                }
                
                // 处理外键关联
                Long originalRawQuestionId = rs.getLong("original_raw_question_id");
                if (!rs.wasNull()) {
                    RawQuestion rawQuestion = new RawQuestion();
                    rawQuestion.setId(originalRawQuestionId);
                    newQuestion.setOriginalRawQuestion(rawQuestion);
                }
                
                // 设置创建者用户
                Long createdByUserId = rs.getLong("created_by_user_id");
                if (!rs.wasNull()) {
                    userRepository.findById(createdByUserId).ifPresent(user -> newQuestion.setCreatedByUser(user));
                }
                
                // 修改处理父标准问题的方式，确保设置正确的父ID
                Long parentStandardQuestionId = rs.getLong("parent_standard_question_id");
                if (!rs.wasNull()) {
                    // 如果父问题已经在Map中，直接使用
                    StandardQuestion parentQuestion = questionMap.get(parentStandardQuestionId);
                    if (parentQuestion == null) {
                        // 否则创建一个新的父问题对象
                        parentQuestion = new StandardQuestion();
                        parentQuestion.setId(parentStandardQuestionId);
                        // 不要将父问题添加到questionMap中，避免潜在的循环引用问题
                    }
                    newQuestion.setParentStandardQuestion(parentQuestion);
                }
                
                Long createdChangeLogId = rs.getLong("created_change_log_id");
                if (!rs.wasNull()) {
                    ChangeLog changeLog = new ChangeLog();
                    changeLog.setId(createdChangeLogId);
                    newQuestion.setCreatedChangeLog(changeLog);
                }
                
                questionMap.put(questionId, newQuestion);
                question = newQuestion;
            }
            
            return question;
        }
    }

    /**
     * 标准问题行映射器
     */
    private class StandardQuestionRowMapper implements RowMapper<StandardQuestion> {
        private Map<Long, StandardQuestion> questionMap = new HashMap<>();
        
        @Override
        public StandardQuestion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long questionId = rs.getLong("id");
            StandardQuestion question = questionMap.get(questionId);
            
            if (question == null) {
                final StandardQuestion newQuestion = new StandardQuestion();
                newQuestion.setId(questionId);
                newQuestion.setQuestionText(rs.getString("question_text"));
                
                // 解析枚举
                String questionTypeStr = rs.getString("question_type");
                if (questionTypeStr != null) {
                    newQuestion.setQuestionType(QuestionType.valueOf(questionTypeStr));
                }
                
                String difficultyStr = rs.getString("difficulty");
                if (difficultyStr != null) {
                    newQuestion.setDifficulty(DifficultyLevel.valueOf(difficultyStr));
                }
                
                // 设置时间
                Timestamp creationTime = rs.getTimestamp("creation_time");
                if (creationTime != null) {
                    newQuestion.setCreationTime(creationTime.toLocalDateTime());
                }
                
                Timestamp deletedAt = rs.getTimestamp("deleted_at");
                if (deletedAt != null) {
                    newQuestion.setDeletedAt(deletedAt.toLocalDateTime());
                }
                
                // 处理外键关联
                Long originalRawQuestionId = rs.getLong("original_raw_question_id");
                if (!rs.wasNull()) {
                    RawQuestion rawQuestion = new RawQuestion();
                    rawQuestion.setId(originalRawQuestionId);
                    newQuestion.setOriginalRawQuestion(rawQuestion);
                }
                
                // 设置创建者用户
                Long createdByUserId = rs.getLong("created_by_user_id");
                if (!rs.wasNull()) {
                    userRepository.findById(createdByUserId).ifPresent(user -> newQuestion.setCreatedByUser(user));
                }
                
                // 修改处理父标准问题的方式，确保设置正确的父ID
                Long parentStandardQuestionId = rs.getLong("parent_standard_question_id");
                if (!rs.wasNull()) {
                    // 如果父问题已经在Map中，直接使用
                    StandardQuestion parentQuestion = questionMap.get(parentStandardQuestionId);
                    if (parentQuestion == null) {
                        // 否则创建一个新的父问题对象
                        parentQuestion = new StandardQuestion();
                        parentQuestion.setId(parentStandardQuestionId);
                        // 不要将父问题添加到questionMap中，避免潜在的循环引用问题
                    }
                    newQuestion.setParentStandardQuestion(parentQuestion);
                }
                
                Long createdChangeLogId = rs.getLong("created_change_log_id");
                if (!rs.wasNull()) {
                    ChangeLog changeLog = new ChangeLog();
                    changeLog.setId(createdChangeLogId);
                    newQuestion.setCreatedChangeLog(changeLog);
                }
                
                questionMap.put(questionId, newQuestion);
                question = newQuestion;
            }
            
            // 处理标签
            Long tagId = rs.getLong("tag_id");
            if (!rs.wasNull()) {
                Tag tag = new Tag();
                tag.setId(tagId);
                tag.setTagName(rs.getString("tag_name"));
                tag.setTagType(rs.getString("tag_type"));
                
                StandardQuestionTag questionTag = new StandardQuestionTag();
                questionTag.setStandardQuestion(question);
                questionTag.setTag(tag);
                
                question.getQuestionTags().add(questionTag);
            }
            
            return question;
        }
    }

    /**
     * 查找指定ID的标准问题，同时预加载父问题
     * 
     * @param questionId 标准问题ID
     * @return 标准问题（可能为空）
     */
    public Optional<StandardQuestion> findByIdWithParent(Long questionId) {
        String sql = 
            "WITH RECURSIVE question_hierarchy AS (" +
            "  SELECT sq.* " +
            "  FROM standard_questions sq " +
            "  WHERE sq.id = ? AND sq.deleted_at IS NULL " +
            "  UNION ALL " +
            "  SELECT p.* " +
            "  FROM standard_questions p " +
            "  JOIN question_hierarchy q ON p.id = q.parent_standard_question_id " +
            "  WHERE p.deleted_at IS NULL" +
            ") " +
            "SELECT qh.*, t.id as tag_id, t.tag_name, t.tag_type " +
            "FROM question_hierarchy qh " +
            "LEFT JOIN standard_question_tags sqt ON qh.id = sqt.standard_question_id " +
            "LEFT JOIN tags t ON sqt.tag_id = t.id";
        
        try {
            List<StandardQuestion> results = jdbcTemplate.query(
                sql,
                new Object[]{questionId},
                new StandardQuestionRowMapper()
            );
            
            if (results.isEmpty()) {
                return Optional.empty();
            }
            
            // 组织父子关系
            Map<Long, StandardQuestion> questionMap = new HashMap<>();
            for (StandardQuestion question : results) {
                questionMap.put(question.getId(), question);
            }
            
            return Optional.ofNullable(questionMap.get(questionId));
        } catch (Exception e) {
            logger.error("查询标准问题及其父问题失败 - 问题ID: {}", questionId, e);
            return Optional.empty();
        }
    }

    /**
     * 使用递归SQL查询问题历史或版本树
     * 
     * @param questionId 问题ID
     * @param sql 递归SQL查询语句
     * @param rowMapper 行映射器
     * @return 查询结果列表
     */
    public <T> List<T> findHistoryWithRecursiveQuery(Long questionId, String sql, RowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql, new Object[]{questionId}, rowMapper);
    }

    /**
     * 使用递归SQL查询问题的完整版本树
     * 
     * @param questionId 问题ID
     * @return 版本树中的所有问题
     */
    public List<StandardQuestion> findCompleteVersionTreeById(Long questionId) {
        String sql = 
            "WITH RECURSIVE version_tree AS (" +
            "  SELECT sq.* " +
            "  FROM standard_questions sq " +
            "  WHERE sq.id IN (" +
            "    WITH RECURSIVE parent_hierarchy AS (" +
            "      SELECT id FROM standard_questions WHERE id = ? AND deleted_at IS NULL " +
            "      UNION ALL " +
            "      SELECT p.id FROM standard_questions p JOIN parent_hierarchy ph ON p.id = " +
            "      (SELECT parent_standard_question_id FROM standard_questions WHERE id = ph.id AND deleted_at IS NULL)" +
            "      WHERE p.deleted_at IS NULL" +
            "    ) " +
            "    SELECT id FROM parent_hierarchy WHERE id = (SELECT MIN(id) FROM parent_hierarchy)" +
            "  ) AND sq.deleted_at IS NULL " +
            "  UNION ALL " +
            "  SELECT c.* " +
            "  FROM standard_questions c " +
            "  JOIN version_tree vt ON c.parent_standard_question_id = vt.id " +
            "  WHERE c.deleted_at IS NULL" +
            ") " +
            "SELECT vt.* FROM version_tree vt";
        
        try {
            return jdbcTemplate.query(sql, new Object[]{questionId}, new StandardQuestionRowMapperWithoutTags());
        } catch (Exception e) {
            logger.error("使用递归SQL查询版本树失败 - 问题ID: {}", questionId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 永久删除标准问题
     * 
     * @param questionId 标准问题ID
     */
    public void deleteById(Long questionId) {
        jdbcTemplate.update("DELETE FROM standard_questions WHERE id=?", questionId);
    }
    
    /**
     * 检查是否存在指定ID的标准问题
     * 
     * @param questionId 标准问题ID
     * @return 是否存在
     */
    public boolean existsById(Long questionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM standard_questions WHERE id = ? AND deleted_at IS NULL",
            new Object[]{questionId},
            Integer.class
        );
        
        return count != null && count > 0;
    }
} 
