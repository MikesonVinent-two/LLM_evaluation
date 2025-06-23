package com.example.demo.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeType;

/**
 * 基于JDBC的变更日志仓库实现
 */
@Repository
public class ChangeLogRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChangeLogRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final StandardQuestionRepository standardQuestionRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO CHANGE_LOG (COMMIT_MESSAGE, CHANGE_TIME, CHANGED_BY_USER_ID, ASSOCIATED_STANDARD_QUESTION_ID, CHANGE_TYPE) " +
            "VALUES (?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE CHANGE_LOG SET COMMIT_MESSAGE=?, CHANGE_TIME=?, CHANGED_BY_USER_ID=?, ASSOCIATED_STANDARD_QUESTION_ID=?, CHANGE_TYPE=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM CHANGE_LOG WHERE id=?";
    
    private static final String SQL_FIND_BY_ANSWER_ID = 
            "SELECT cl.* FROM CHANGE_LOG cl " +
            "LEFT JOIN standard_objective_answers soa ON soa.created_change_log_id = cl.id " +
            "LEFT JOIN standard_simple_answers ssa ON ssa.created_change_log_id = cl.id " +
            "LEFT JOIN standard_subjective_answers ssua ON ssua.created_change_log_id = cl.id " +
            "WHERE soa.id = ? OR ssa.id = ? OR ssua.id = ? " +
            "ORDER BY cl.CHANGE_TIME DESC";
    
    private static final String SQL_FIND_ROOT_VERSION = 
            "SELECT cl.* FROM CHANGE_LOG cl " +
            "LEFT JOIN standard_objective_answers soa ON soa.created_change_log_id = cl.id " +
            "LEFT JOIN standard_simple_answers ssa ON ssa.created_change_log_id = cl.id " +
            "LEFT JOIN standard_subjective_answers ssua ON ssua.created_change_log_id = cl.id " +
            "WHERE (soa.id = ? OR ssa.id = ? OR ssua.id = ?) " +
            "ORDER BY cl.CHANGE_TIME ASC LIMIT 1";
    
    private static final String SQL_FIND_CHILD_VERSIONS = 
            "SELECT cl2.* FROM CHANGE_LOG cl1 " +
            "JOIN CHANGE_LOG cl2 ON cl1.ASSOCIATED_STANDARD_QUESTION_ID = cl2.ASSOCIATED_STANDARD_QUESTION_ID " +
            "WHERE cl1.id = ? AND cl2.CHANGE_TIME > cl1.CHANGE_TIME " +
            "ORDER BY cl2.CHANGE_TIME ASC";

    private static final String SQL_FIND_ALL = 
            "SELECT * FROM CHANGE_LOG ORDER BY CHANGE_TIME DESC";

    @Autowired
    public ChangeLogRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository, StandardQuestionRepository standardQuestionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.standardQuestionRepository = standardQuestionRepository;
    }

    /**
     * 保存变更日志
     *
     * @param changeLog 变更日志对象
     * @return 带有ID的变更日志对象
     */
    public ChangeLog save(ChangeLog changeLog) {
        // 参数校验
        if (changeLog == null) {
            throw new IllegalArgumentException("变更日志对象不能为空");
        }
        
        // 检查并确保commitTime不为空
        if (changeLog.getCommitTime() == null) {
            logger.debug("提交时间为空，设置为当前时间");
            changeLog.setCommitTime(LocalDateTime.now());
        }
        
        // 检查用户对象
        if (changeLog.getUser() == null) {
            throw new IllegalArgumentException("变更日志的用户不能为空");
        }
        
        if (changeLog.getUser().getId() == null) {
            throw new IllegalArgumentException("变更日志的用户ID不能为空");
        }
        
        // 检查关联的标准问题
        if (changeLog.getAssociatedStandardQuestion() == null) {
            throw new IllegalArgumentException("变更日志关联的标准问题不能为空");
        }
        
        if (changeLog.getAssociatedStandardQuestion().getId() == null) {
            throw new IllegalArgumentException("变更日志关联的标准问题ID不能为空");
        }
        
        // 检查变更类型
        if (changeLog.getChangeType() == null) {
            throw new IllegalArgumentException("变更日志的变更类型不能为空");
        }
        
        if (changeLog.getId() == null) {
            // 新增
            try {
                jdbcTemplate.update(SQL_INSERT,
                    changeLog.getCommitMessage(),
                    Timestamp.valueOf(changeLog.getCommitTime()),
                    changeLog.getUser().getId(),
                    changeLog.getAssociatedStandardQuestion().getId(),
                    changeLog.getChangeType().toString());
                
                Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                changeLog.setId(id);
            } catch (Exception e) {
                logger.error("保存变更日志失败", e);
                throw new RuntimeException("保存变更日志失败: " + e.getMessage(), e);
            }
        } else {
            // 更新
            try {
                jdbcTemplate.update(SQL_UPDATE,
                    changeLog.getCommitMessage(),
                    Timestamp.valueOf(changeLog.getCommitTime()),
                    changeLog.getUser().getId(),
                    changeLog.getAssociatedStandardQuestion().getId(),
                    changeLog.getChangeType().toString(),
                    changeLog.getId());
            } catch (Exception e) {
                logger.error("更新变更日志失败", e);
                throw new RuntimeException("更新变更日志失败: " + e.getMessage(), e);
            }
        }
        return changeLog;
    }

    /**
     * 根据ID查找变更日志
     *
     * @param id 变更日志ID
     * @return 变更日志的Optional包装
     */
    public Optional<ChangeLog> findById(Long id) {
        try {
            ChangeLog changeLog = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new ChangeLogRowMapper(), id);
            return Optional.ofNullable(changeLog);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<ChangeLog> findByAnswerId(Long answerId) {
        return jdbcTemplate.query(SQL_FIND_BY_ANSWER_ID, new ChangeLogRowMapper(), answerId, answerId, answerId);
    }

    public ChangeLog findRootVersionByAnswerId(Long answerId) {
        try {
            return jdbcTemplate.queryForObject(SQL_FIND_ROOT_VERSION, new ChangeLogRowMapper(), answerId, answerId, answerId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<ChangeLog> findChildVersions(Long changeLogId) {
        return jdbcTemplate.query(SQL_FIND_CHILD_VERSIONS, new ChangeLogRowMapper(), changeLogId);
    }

    /**
     * 查找比指定版本更新的版本
     * @param versionId 版本ID
     * @return 更新的版本列表
     */
    public List<ChangeLog> findNewerVersions(Long versionId) {
        String sql = "SELECT cl.* FROM CHANGE_LOG cl "
                + "JOIN CHANGE_LOG target ON cl.ASSOCIATED_STANDARD_QUESTION_ID = target.ASSOCIATED_STANDARD_QUESTION_ID "
                + "WHERE target.id = ? AND cl.CHANGE_TIME > (SELECT CHANGE_TIME FROM CHANGE_LOG WHERE id = ?) "
                + "ORDER BY cl.CHANGE_TIME DESC";
        
        try {
            return jdbcTemplate.query(sql, new Object[]{versionId, versionId}, new ChangeLogRowMapper());
        } catch (Exception e) {
            logger.error("查询更新版本失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据关联标准问题ID查找变更日志
     * @param standardQuestionId 标准问题ID
     * @return 变更日志对象
     */
    public ChangeLog findByAssociatedStandardQuestionId(Long standardQuestionId) {
        String sql = "SELECT * FROM CHANGE_LOG WHERE ASSOCIATED_STANDARD_QUESTION_ID = ? ORDER BY CHANGE_TIME DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, new ChangeLogRowMapper(), standardQuestionId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 查找所有变更日志
     * @return 变更日志列表
     */
    public List<ChangeLog> findAll() {
        try {
            return jdbcTemplate.query(SQL_FIND_ALL, new ChangeLogRowMapper());
        } catch (Exception e) {
            logger.error("查询所有变更日志失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据ID删除变更日志
     * @param id 变更日志ID
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM CHANGE_LOG WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 变更日志行映射器
     */
    private class ChangeLogRowMapper implements RowMapper<ChangeLog> {
        @Override
        public ChangeLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChangeLog changeLog = new ChangeLog();
            changeLog.setId(rs.getLong("id"));
            changeLog.setCommitMessage(rs.getString("COMMIT_MESSAGE"));
            
            Timestamp commitTime = rs.getTimestamp("CHANGE_TIME");
            if (commitTime != null) {
                changeLog.setCommitTime(commitTime.toLocalDateTime());
            }
            
            // 设置变更类型
            String changeTypeStr = rs.getString("CHANGE_TYPE");
            if (changeTypeStr != null) {
                try {
                    changeLog.setChangeType(ChangeType.valueOf(changeTypeStr));
                } catch (IllegalArgumentException e) {
                    logger.warn("未识别的变更类型: {}", changeTypeStr);
                }
            }
            
            // 设置用户
            Long userId = rs.getLong("CHANGED_BY_USER_ID");
            if (!rs.wasNull()) {
                userRepository.findById(userId).ifPresent(user -> changeLog.setUser(user));
            }
            
            // 设置关联的标准问题
            Long standardQuestionId = rs.getLong("ASSOCIATED_STANDARD_QUESTION_ID");
            if (!rs.wasNull()) {
                standardQuestionRepository.findById(standardQuestionId)
                    .ifPresent(question -> changeLog.setAssociatedStandardQuestion(question));
            }
            
            return changeLog;
        }
    }
} 
