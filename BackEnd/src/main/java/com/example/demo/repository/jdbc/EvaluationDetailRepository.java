package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.EvaluationDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
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
import java.math.BigDecimal;

/**
 * 基于JDBC的评测详情仓库实?
 */
@Repository
public class EvaluationDetailRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EvaluationRepository EvaluationRepository;
    private final EvaluationCriterionRepository EvaluationCriterionRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO evaluation_details (evaluation_id, criterion_id, criterion_name, score, comments, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE evaluation_details SET evaluation_id=?, criterion_id=?, criterion_name=?, score=?, comments=?, created_at=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM evaluation_details WHERE id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM evaluation_details";
    
    private static final String SQL_FIND_BY_EVALUATION_ID = 
            "SELECT * FROM evaluation_details WHERE evaluation_id=?";
    
    private static final String SQL_FIND_BY_EVALUATION_RUN_ID = 
            "SELECT ed.* FROM evaluation_details ed " +
            "JOIN evaluations e ON ed.evaluation_id = e.id " +
            "WHERE e.evaluation_run_id=?";
    
    private static final String SQL_DELETE_BY_EVALUATION_ID = 
            "DELETE FROM evaluation_details WHERE evaluation_id=?";

    @Autowired
    public EvaluationDetailRepository(
            JdbcTemplate jdbcTemplate, 
            EvaluationRepository EvaluationRepository,
            EvaluationCriterionRepository EvaluationCriterionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.EvaluationRepository = EvaluationRepository;
        this.EvaluationCriterionRepository = EvaluationCriterionRepository;
    }

    /**
     * 保存评测详情
     *
     * @param detail 评测详情对象
     * @return 带有ID的评测详情对?
     */
    public EvaluationDetail save(EvaluationDetail detail) {
        if (detail.getId() == null) {
            return insert(detail);
        } else {
            return update(detail);
        }
    }

    /**
     * 批量保存评测详情
     *
     * @param details 评测详情列表
     * @return 带有ID的评测详情列?
     */
    public List<EvaluationDetail> saveAll(List<EvaluationDetail> details) {
        for (int i = 0; i < details.size(); i++) {
            details.set(i, save(details.get(i)));
        }
        return details;
    }

    /**
     * 插入新评测详?
     *
     * @param detail 评测详情对象
     * @return 带有ID的评测详情对?
     */
    private EvaluationDetail insert(EvaluationDetail detail) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置评测ID
            if (detail.getEvaluation() != null && detail.getEvaluation().getId() != null) {
                ps.setLong(1, detail.getEvaluation().getId());
            } else {
                throw new IllegalArgumentException("评测详情必须关联一个评测");
            }
            
            // 设置评测标准ID
            if (detail.getCriterion() != null && detail.getCriterion().getId() != null) {
                ps.setLong(2, detail.getCriterion().getId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            
            // 设置评测标准名称
            ps.setString(3, detail.getCriterionName());
            
            // 设置分数
            if (detail.getScore() != null) {
                ps.setBigDecimal(4, detail.getScore());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            
            // 设置评论
            if (detail.getComments() != null) {
                ps.setString(5, detail.getComments());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            // 设置创建时间
            if (detail.getCreatedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(detail.getCreatedAt()));
            } else {
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            detail.setId(key.longValue());
        }

        return detail;
    }

    /**
     * 更新评测详情
     *
     * @param detail 评测详情对象
     * @return 更新后的评测详情对象
     */
    private EvaluationDetail update(EvaluationDetail detail) {
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_UPDATE);
            
            // 设置评测ID
            if (detail.getEvaluation() != null && detail.getEvaluation().getId() != null) {
                ps.setLong(1, detail.getEvaluation().getId());
            } else {
                throw new IllegalArgumentException("评测详情必须关联一个评测");
            }
            
            // 设置评测标准ID
            if (detail.getCriterion() != null && detail.getCriterion().getId() != null) {
                ps.setLong(2, detail.getCriterion().getId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            
            // 设置评测标准名称
            ps.setString(3, detail.getCriterionName());
            
            // 设置分数
            if (detail.getScore() != null) {
                ps.setBigDecimal(4, detail.getScore());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            
            // 设置评论
            if (detail.getComments() != null) {
                ps.setString(5, detail.getComments());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            
            // 设置创建时间
            if (detail.getCreatedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(detail.getCreatedAt()));
            } else {
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            ps.setLong(7, detail.getId());
            
            return ps;
        });

        return detail;
    }

    /**
     * 根据ID查找评测详情
     *
     * @param id 评测详情ID
     * @return 评测详情对象（可选）
     */
    public Optional<EvaluationDetail> findById(Long id) {
        try {
            EvaluationDetail detail = jdbcTemplate.queryForObject(
                SQL_FIND_BY_ID,
                new EvaluationDetailRowMapper(),
                id
            );
            return Optional.ofNullable(detail);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查找所有评测详?
     *
     * @return 评测详情列表
     */
    public List<EvaluationDetail> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new EvaluationDetailRowMapper());
    }

    /**
     * 根据评测ID查找评测详情
     *
     * @param evaluationId 评测ID
     * @return 评测详情列表
     */
    public List<EvaluationDetail> findByEvaluationId(Long evaluationId) {
        return jdbcTemplate.query(
            SQL_FIND_BY_EVALUATION_ID,
            new EvaluationDetailRowMapper(),
            evaluationId
        );
    }

    /**
     * 根据评测实体查找评测详情
     *
     * @param evaluation 评测实体
     * @return 评测详情列表
     */
    public List<EvaluationDetail> findByEvaluation(Evaluation evaluation) {
        if (evaluation == null || evaluation.getId() == null) {
            return List.of();
        }
        return findByEvaluationId(evaluation.getId());
    }

    /**
     * 根据评测运行ID查找评测详情
     *
     * @param evaluationRunId 评测运行ID
     * @return 评测详情列表
     */
    public List<EvaluationDetail> findByEvaluationRunId(Long evaluationRunId) {
        return jdbcTemplate.query(
            SQL_FIND_BY_EVALUATION_RUN_ID,
            new EvaluationDetailRowMapper(),
            evaluationRunId
        );
    }

    /**
     * 删除指定评测的所有评测详?
     *
     * @param evaluationId 评测ID
     */
    public void deleteByEvaluationId(Long evaluationId) {
        jdbcTemplate.update(SQL_DELETE_BY_EVALUATION_ID, evaluationId);
    }

    /**
     * 根据ID删除评测详情
     *
     * @param id 评测详情ID
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM evaluation_details WHERE id=?", id);
    }

    /**
     * 评测详情行映射器
     */
    private class EvaluationDetailRowMapper implements RowMapper<EvaluationDetail> {
        @Override
        public EvaluationDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
            EvaluationDetail detail = new EvaluationDetail();
            
            detail.setId(rs.getLong("id"));
            
            // 设置评测
            Long evaluationId = rs.getLong("evaluation_id");
            if (!rs.wasNull()) {
                Evaluation evaluation = new Evaluation();
                evaluation.setId(evaluationId);
                detail.setEvaluation(evaluation);
                
                // 可选：加载完整的评测信?
                EvaluationRepository.findById(evaluationId).ifPresent(detail::setEvaluation);
            }
            
            // 设置评测标准
            Long criterionId = rs.getLong("criterion_id");
            if (!rs.wasNull()) {
                EvaluationCriterion criterion = new EvaluationCriterion();
                criterion.setId(criterionId);
                detail.setCriterion(criterion);
                
                // 可选：加载完整的评测标准信?
                EvaluationCriterionRepository.findById(criterionId).ifPresent(detail::setCriterion);
            }
            
            detail.setCriterionName(rs.getString("criterion_name"));
            
            BigDecimal score = rs.getBigDecimal("score");
            if (!rs.wasNull()) {
                detail.setScore(score);
            }
            
            detail.setComments(rs.getString("comments"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                detail.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            return detail;
        }
    }
} 
