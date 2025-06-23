package com.example.demo.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
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

import com.example.demo.entity.jdbc.CrowdsourcedAnswer;

/**
 * 基于JDBC的众包回答仓库实?
 */
@Repository
public class CrowdsourcedAnswerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final StandardQuestionRepository standardQuestionRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO CROWDSOURCED_ANSWERS (STANDARD_QUESTION_ID, USER_ID, ANSWER_TEXT, " +
            "SUBMISSION_TIME, TASK_BATCH_ID, QUALITY_REVIEW_STATUS, REVIEWED_BY_USER_ID, " +
            "REVIEW_TIME, REVIEW_FEEDBACK, OTHER_METADATA) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE CROWDSOURCED_ANSWERS SET STANDARD_QUESTION_ID=?, USER_ID=?, ANSWER_TEXT=?, " +
            "SUBMISSION_TIME=?, TASK_BATCH_ID=?, QUALITY_REVIEW_STATUS=?, REVIEWED_BY_USER_ID=?, " +
            "REVIEW_TIME=?, REVIEW_FEEDBACK=?, OTHER_METADATA=? WHERE ID=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM CROWDSOURCED_ANSWERS WHERE ID=?";
    
    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID = 
            "SELECT * FROM CROWDSOURCED_ANSWERS WHERE STANDARD_QUESTION_ID=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_STANDARD_QUESTION_ID = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE STANDARD_QUESTION_ID=?";
    
    private static final String SQL_FIND_BY_USER_ID = 
            "SELECT * FROM CROWDSOURCED_ANSWERS WHERE USER_ID=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_USER_ID = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE USER_ID=?";
    
    private static final String SQL_FIND_BY_QUALITY_REVIEW_STATUS = 
            "SELECT * FROM CROWDSOURCED_ANSWERS WHERE QUALITY_REVIEW_STATUS=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_QUALITY_REVIEW_STATUS = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE QUALITY_REVIEW_STATUS=?";
    
    private static final String SQL_FIND_BY_STANDARD_QUESTION_ID_AND_QUALITY_REVIEW_STATUS = 
            "SELECT * FROM CROWDSOURCED_ANSWERS " +
            "WHERE STANDARD_QUESTION_ID=? AND QUALITY_REVIEW_STATUS=? " +
            "ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_STANDARD_QUESTION_ID_AND_QUALITY_REVIEW_STATUS = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS " +
            "WHERE STANDARD_QUESTION_ID=? AND QUALITY_REVIEW_STATUS=?";
    
    private static final String SQL_EXISTS_BY_STANDARD_QUESTION_ID_AND_USER_ID_AND_TASK_BATCH_ID = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS " +
            "WHERE STANDARD_QUESTION_ID=? AND USER_ID=? AND (TASK_BATCH_ID=? OR (TASK_BATCH_ID IS NULL AND ? IS NULL))";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM CROWDSOURCED_ANSWERS ORDER BY SUBMISSION_TIME DESC";
            
    private static final String SQL_FIND_ALL_PAGEABLE = 
            "SELECT * FROM CROWDSOURCED_ANSWERS ORDER BY SUBMISSION_TIME DESC LIMIT ? OFFSET ?";
            
    private static final String SQL_COUNT_ALL = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS";
    
    private static final String SQL_DELETE = 
            "DELETE FROM CROWDSOURCED_ANSWERS WHERE ID=?";

    private static final String SQL_FIND_BY_REVIEWED_BY_USER_ID = 
            "SELECT * FROM CROWDSOURCED_ANSWERS WHERE REVIEWED_BY_USER_ID=? " +
            "ORDER BY REVIEW_TIME DESC LIMIT ? OFFSET ?";
    
    private static final String SQL_COUNT_BY_REVIEWED_BY_USER_ID = 
            "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE REVIEWED_BY_USER_ID=?";

    @Autowired
    public CrowdsourcedAnswerRepository(JdbcTemplate jdbcTemplate, 
                                          UserRepository userRepository,
                                          StandardQuestionRepository standardQuestionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.standardQuestionRepository = standardQuestionRepository;
    }

    /**
     * 保存众包回答
     *
     * @param crowdsourcedAnswer 众包回答对象
     * @return 带有ID的众包回答对?
     */
    public CrowdsourcedAnswer save(CrowdsourcedAnswer crowdsourcedAnswer) {
        if (crowdsourcedAnswer.getId() == null) {
            return insert(crowdsourcedAnswer);
        } else {
            return update(crowdsourcedAnswer);
        }
    }

    /**
     * 插入新众包回?
     *
     * @param crowdsourcedAnswer 众包回答对象
     * @return 带有ID的众包回答对?
     */
    private CrowdsourcedAnswer insert(CrowdsourcedAnswer crowdsourcedAnswer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 设置默认提交时间
        if (crowdsourcedAnswer.getSubmissionTime() == null) {
            crowdsourcedAnswer.setSubmissionTime(LocalDateTime.now());
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置标准问题ID
            if (crowdsourcedAnswer.getStandardQuestion() != null && crowdsourcedAnswer.getStandardQuestion().getId() != null) {
                ps.setLong(1, crowdsourcedAnswer.getStandardQuestion().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置用户ID
            if (crowdsourcedAnswer.getUser() != null && crowdsourcedAnswer.getUser().getId() != null) {
                ps.setLong(2, crowdsourcedAnswer.getUser().getId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            
            // 设置回答文本
            ps.setString(3, crowdsourcedAnswer.getAnswerText());
            
            // 设置提交时间
            ps.setTimestamp(4, Timestamp.valueOf(crowdsourcedAnswer.getSubmissionTime()));
            
            // 设置任务批次ID
            if (crowdsourcedAnswer.getTaskBatchId() != null) {
                ps.setLong(5, crowdsourcedAnswer.getTaskBatchId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置质量审核状?
            if (crowdsourcedAnswer.getQualityReviewStatus() != null) {
                ps.setString(6, crowdsourcedAnswer.getQualityReviewStatus().name());
            } else {
                ps.setString(6, CrowdsourcedAnswer.QualityReviewStatus.PENDING.name());
            }
            
            // 设置审核者用户ID
            if (crowdsourcedAnswer.getReviewedByUser() != null && crowdsourcedAnswer.getReviewedByUser().getId() != null) {
                ps.setLong(7, crowdsourcedAnswer.getReviewedByUser().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置审核时间
            if (crowdsourcedAnswer.getReviewTime() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(crowdsourcedAnswer.getReviewTime()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            
            // 设置审核反馈
            if (crowdsourcedAnswer.getReviewFeedback() != null) {
                ps.setString(9, crowdsourcedAnswer.getReviewFeedback());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }
            
            // 设置其他元数?
            if (crowdsourcedAnswer.getOtherMetadata() != null) {
                ps.setString(10, crowdsourcedAnswer.getOtherMetadata());
            } else {
                ps.setString(10, "{}");
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            crowdsourcedAnswer.setId(key.longValue());
        }

        return crowdsourcedAnswer;
    }

    /**
     * 更新众包回答
     *
     * @param crowdsourcedAnswer 众包回答对象
     * @return 更新后的众包回答对象
     */
    private CrowdsourcedAnswer update(CrowdsourcedAnswer crowdsourcedAnswer) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置标准问题ID
            if (crowdsourcedAnswer.getStandardQuestion() != null && crowdsourcedAnswer.getStandardQuestion().getId() != null) {
                ps.setLong(1, crowdsourcedAnswer.getStandardQuestion().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            
            // 设置用户ID
            if (crowdsourcedAnswer.getUser() != null && crowdsourcedAnswer.getUser().getId() != null) {
                ps.setLong(2, crowdsourcedAnswer.getUser().getId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            
            // 设置回答文本
            ps.setString(3, crowdsourcedAnswer.getAnswerText());
            
            // 设置提交时间
            ps.setTimestamp(4, Timestamp.valueOf(crowdsourcedAnswer.getSubmissionTime()));
            
            // 设置任务批次ID
            if (crowdsourcedAnswer.getTaskBatchId() != null) {
                ps.setLong(5, crowdsourcedAnswer.getTaskBatchId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            
            // 设置质量审核状?
            if (crowdsourcedAnswer.getQualityReviewStatus() != null) {
                ps.setString(6, crowdsourcedAnswer.getQualityReviewStatus().name());
            } else {
                ps.setString(6, CrowdsourcedAnswer.QualityReviewStatus.PENDING.name());
            }
            
            // 设置审核者用户ID
            if (crowdsourcedAnswer.getReviewedByUser() != null && crowdsourcedAnswer.getReviewedByUser().getId() != null) {
                ps.setLong(7, crowdsourcedAnswer.getReviewedByUser().getId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            
            // 设置审核时间
            if (crowdsourcedAnswer.getReviewTime() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(crowdsourcedAnswer.getReviewTime()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            
            // 设置审核反馈
            if (crowdsourcedAnswer.getReviewFeedback() != null) {
                ps.setString(9, crowdsourcedAnswer.getReviewFeedback());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }
            
            // 设置其他元数?
            if (crowdsourcedAnswer.getOtherMetadata() != null) {
                ps.setString(10, crowdsourcedAnswer.getOtherMetadata());
            } else {
                ps.setString(10, "{}");
            }
            
            // 设置ID
            ps.setLong(11, crowdsourcedAnswer.getId());
            
            return ps;
        });

        return crowdsourcedAnswer;
    }

    /**
     * 根据ID查找众包回答
     *
     * @param id 众包回答ID
     * @return 众包回答对象
     */
    public Optional<CrowdsourcedAnswer> findById(Long id) {
        try {
            CrowdsourcedAnswer crowdsourcedAnswer = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID, 
                new CrowdsourcedAnswerRowMapper(), 
                id
            );
            return Optional.ofNullable(crowdsourcedAnswer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据标准问题ID查询众包回答
     *
     * @param standardQuestionId 标准问题ID
     * @param pageable 分页参数
     * @return 众包回答分页列表
     */
    public Page<CrowdsourcedAnswer> findByStandardQuestionId(Long standardQuestionId, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_STANDARD_QUESTION_ID, 
            Integer.class, 
            standardQuestionId
        );
        
        // 查询数据
        List<CrowdsourcedAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_STANDARD_QUESTION_ID, 
            new CrowdsourcedAnswerRowMapper(), 
            standardQuestionId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据用户ID查询众包回答
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 众包回答分页列表
     */
    public Page<CrowdsourcedAnswer> findByUserId(Long userId, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_USER_ID, 
            Integer.class, 
            userId
        );
        
        // 查询数据
        List<CrowdsourcedAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_USER_ID, 
            new CrowdsourcedAnswerRowMapper(), 
            userId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据审核状态查询众包回?
     *
     * @param status 审核状?
     * @param pageable 分页参数
     * @return 众包回答分页列表
     */
    public Page<CrowdsourcedAnswer> findByQualityReviewStatus(
            CrowdsourcedAnswer.QualityReviewStatus status, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_QUALITY_REVIEW_STATUS, 
            Integer.class, 
            status.name()
        );
        
        // 查询数据
        List<CrowdsourcedAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_QUALITY_REVIEW_STATUS, 
            new CrowdsourcedAnswerRowMapper(), 
            status.name(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 根据标准问题ID和审核状态查询众包回?
     *
     * @param standardQuestionId 标准问题ID
     * @param status 审核状?
     * @param pageable 分页参数
     * @return 众包回答分页列表
     */
    public Page<CrowdsourcedAnswer> findByStandardQuestionIdAndQualityReviewStatus(
            Long standardQuestionId, CrowdsourcedAnswer.QualityReviewStatus status, Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_STANDARD_QUESTION_ID_AND_QUALITY_REVIEW_STATUS, 
            Integer.class, 
            standardQuestionId,
            status.name()
        );
        
        // 查询数据
        List<CrowdsourcedAnswer> content = jdbcTemplate.query(
            SQL_FIND_BY_STANDARD_QUESTION_ID_AND_QUALITY_REVIEW_STATUS, 
            new CrowdsourcedAnswerRowMapper(), 
            standardQuestionId,
            status.name(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 检查用户是否已经在特定任务批次中为特定问题提交回答
     *
     * @param standardQuestionId 标准问题ID
     * @param userId 用户ID
     * @param taskBatchId 任务批次ID
     * @return 是否存在
     */
    public boolean existsByStandardQuestionIdAndUserIdAndTaskBatchId(
            Long standardQuestionId, Long userId, Long taskBatchId) {
        String sql;
        Object[] params;
        
        if (taskBatchId == null) {
            // 如果taskBatchId为null，则查询所有该用户对该问题的回答，不考虑批次
            sql = "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE STANDARD_QUESTION_ID=? AND USER_ID=?";
            params = new Object[] { standardQuestionId, userId };
        } else {
            // 如果taskBatchId不为null，则查询特定批次中该用户对该问题的回答
            sql = "SELECT COUNT(*) FROM CROWDSOURCED_ANSWERS WHERE STANDARD_QUESTION_ID=? AND USER_ID=? AND TASK_BATCH_ID=?";
            params = new Object[] { standardQuestionId, userId, taskBatchId };
        }
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count != null && count > 0;
    }

    /**
     * 查找所有众包回答（分页）
     *
     * @param pageable 分页参数
     * @return 分页众包回答列表
     */
    public Page<CrowdsourcedAnswer> findAll(Pageable pageable) {
        // 查询总数
        Integer total = jdbcTemplate.queryForObject(SQL_COUNT_ALL, Integer.class);
        
        // 查询数据
        List<CrowdsourcedAnswer> content = jdbcTemplate.query(
            SQL_FIND_ALL_PAGEABLE,
            new CrowdsourcedAnswerRowMapper(),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    /**
     * 删除众包回答
     *
     * @param crowdsourcedAnswer 众包回答对象
     */
    public void delete(CrowdsourcedAnswer crowdsourcedAnswer) {
        jdbcTemplate.update(SQL_DELETE, crowdsourcedAnswer.getId());
    }

    /**
     * 根据审核者用户ID查找众包回答（分页）
     *
     * @param reviewedByUserId 审核者用户ID
     * @param pageable 分页参数
     * @return 众包回答分页结果
     */
    public Page<CrowdsourcedAnswer> findByReviewedByUserId(Long reviewedByUserId, Pageable pageable) {
        int total = jdbcTemplate.queryForObject(
            SQL_COUNT_BY_REVIEWED_BY_USER_ID, Integer.class, reviewedByUserId);
        
        List<CrowdsourcedAnswer> answers = jdbcTemplate.query(
            SQL_FIND_BY_REVIEWED_BY_USER_ID,
            new CrowdsourcedAnswerRowMapper(),
            reviewedByUserId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        
        return new PageImpl<>(answers, pageable, total);
    }

    /**
     * 众包回答行映射器
     */
    private class CrowdsourcedAnswerRowMapper implements RowMapper<CrowdsourcedAnswer> {
        @Override
        public CrowdsourcedAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
            CrowdsourcedAnswer crowdsourcedAnswer = new CrowdsourcedAnswer();
            
            // 设置ID和基本属?
            crowdsourcedAnswer.setId(rs.getLong("ID"));
            crowdsourcedAnswer.setAnswerText(rs.getString("ANSWER_TEXT"));
            
            // 设置任务批次ID
            Long taskBatchId = rs.getLong("TASK_BATCH_ID");
            if (!rs.wasNull()) {
                crowdsourcedAnswer.setTaskBatchId(taskBatchId);
            }
            
            // 设置审核反馈
            crowdsourcedAnswer.setReviewFeedback(rs.getString("REVIEW_FEEDBACK"));
            
            // 设置其他元数?
            crowdsourcedAnswer.setOtherMetadata(rs.getString("OTHER_METADATA"));
            
            // 设置质量审核状?
            String qualityReviewStatusStr = rs.getString("QUALITY_REVIEW_STATUS");
            if (qualityReviewStatusStr != null) {
                crowdsourcedAnswer.setQualityReviewStatus(
                    CrowdsourcedAnswer.QualityReviewStatus.valueOf(qualityReviewStatusStr)
                );
            }
            
            // 设置提交时间
            Timestamp submissionTime = rs.getTimestamp("SUBMISSION_TIME");
            if (submissionTime != null) {
                crowdsourcedAnswer.setSubmissionTime(submissionTime.toLocalDateTime());
            }
            
            // 设置审核时间
            Timestamp reviewTime = rs.getTimestamp("REVIEW_TIME");
            if (reviewTime != null) {
                crowdsourcedAnswer.setReviewTime(reviewTime.toLocalDateTime());
            }
            
            // 获取并设置标准问?
            Long standardQuestionId = rs.getLong("STANDARD_QUESTION_ID");
            if (!rs.wasNull()) {
                standardQuestionRepository.findById(standardQuestionId)
                    .ifPresent(crowdsourcedAnswer::setStandardQuestion);
            }
            
            // 获取并设置用?
            Long userId = rs.getLong("USER_ID");
            if (!rs.wasNull()) {
                userRepository.findById(userId)
                    .ifPresent(crowdsourcedAnswer::setUser);
            }
            
            // 获取并设置审核者用?
            Long reviewedByUserId = rs.getLong("REVIEWED_BY_USER_ID");
            if (!rs.wasNull()) {
                userRepository.findById(reviewedByUserId)
                    .ifPresent(crowdsourcedAnswer::setReviewedByUser);
            }
            
            return crowdsourcedAnswer;
        }
    }
} 
