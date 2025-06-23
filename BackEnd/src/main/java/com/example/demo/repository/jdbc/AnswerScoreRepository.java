package com.example.demo.repository.jdbc;

import com.example.demo.entity.jdbc.AnswerScore;
import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC实现的回答分数仓库
 * 
 * @deprecated 此表已被废弃，ANSWER_SCORES表的字段已被合并到EVALUATIONS表中。
 * 请使用{@link EvaluationRepository}代替，查看EVALUATIONS表中的RAW_SCORE, NORMALIZED_SCORE, 
 * WEIGHTED_SCORE, SCORE_TYPE, SCORING_METHOD等字段。
 */
@Deprecated
@Repository
public class AnswerScoreRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    private static final String SQL_INSERT = 
            "INSERT INTO answer_scores (llm_answer_id, evaluator_id, raw_score, normalized_score, weighted_score, score_type, " +
            "scoring_method, evaluation_id, scoring_time, created_by_user_id, comments) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SQL_UPDATE = 
            "UPDATE answer_scores SET llm_answer_id=?, evaluator_id=?, raw_score=?, normalized_score=?, weighted_score=?, " +
            "score_type=?, scoring_method=?, evaluation_id=?, scoring_time=?, created_by_user_id=?, comments=? " +
            "WHERE id=?";
    
    private static final String SQL_FIND_BY_ID = 
            "SELECT * FROM answer_scores WHERE id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID = 
            "SELECT * FROM answer_scores WHERE llm_answer_id=? AND evaluator_id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID_AND_SCORE_TYPE = 
            "SELECT * FROM answer_scores WHERE llm_answer_id=? AND evaluator_id=? AND score_type=?";
    
    private static final String SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID_AND_SCORE_TYPE = 
            "SELECT COUNT(*) FROM answer_scores WHERE llm_answer_id=? AND evaluator_id=? AND score_type=?";
    
    private static final String SQL_FIND_BY_EVALUATION_ID = 
            "SELECT * FROM answer_scores WHERE evaluation_id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID = 
            "SELECT * FROM answer_scores WHERE llm_answer_id=?";
    
    private static final String SQL_FIND_BY_LLM_ANSWER_ID_AND_SCORE_TYPE = 
            "SELECT * FROM answer_scores WHERE llm_answer_id=? AND score_type=?";
    
    private static final String SQL_FIND_AVERAGE_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE = 
            "SELECT AVG(normalized_score) FROM answer_scores WHERE llm_answer_id=? AND score_type=?";
    
    private static final String SQL_FIND_MAX_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE = 
            "SELECT MAX(normalized_score) FROM answer_scores WHERE llm_answer_id=? AND score_type=?";
    
    private static final String SQL_FIND_MIN_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE = 
            "SELECT MIN(normalized_score) FROM answer_scores WHERE llm_answer_id=? AND score_type=?";
    
    private static final String SQL_FIND_BY_SCORE_TYPE = 
            "SELECT * FROM answer_scores WHERE score_type=?";
    
    private static final String SQL_FIND_BY_EVALUATOR_ID = 
            "SELECT * FROM answer_scores WHERE evaluator_id=?";
    
    private static final String SQL_FIND_BY_EVALUATOR_ID_AND_SCORE_TYPE = 
            "SELECT * FROM answer_scores WHERE evaluator_id=? AND score_type=?";
    
    private static final String SQL_DELETE_BY_EVALUATION_ID = 
            "DELETE FROM answer_scores WHERE evaluation_id=?";
    
    private static final String SQL_FIND_ALL = 
            "SELECT * FROM answer_scores";

    private static final String SQL_DELETE_BY_ID = 
            "DELETE FROM answer_scores WHERE id=?";

    @Autowired
    public AnswerScoreRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    /**
     * 保存回答分数
     *
     * @param answerScore 回答分数对象
     * @return 带有ID的回答分数对象
     */
    public AnswerScore save(AnswerScore answerScore) {
        if (answerScore.getId() == null) {
            return insert(answerScore);
        } else {
            return update(answerScore);
        }
    }

    /**
     * 插入新回答分数
     *
     * @param answerScore 回答分数对象
     * @return 带有ID的回答分数对象
     */
    private AnswerScore insert(AnswerScore answerScore) {
        if (answerScore.getScoringTime() == null) {
            answerScore.setScoringTime(LocalDateTime.now());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            // 设置LLM答案ID
            ps.setLong(1, answerScore.getLlmAnswer().getId());
            
            // 设置评测者ID
            ps.setLong(2, answerScore.getEvaluator().getId());
            
            // 设置原始分数
            ps.setBigDecimal(3, answerScore.getRawScore());
            
            // 设置标准化分数
            if (answerScore.getNormalizedScore() != null) {
                ps.setBigDecimal(4, answerScore.getNormalizedScore());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            
            // 设置加权分数
            if (answerScore.getWeightedScore() != null) {
                ps.setBigDecimal(5, answerScore.getWeightedScore());
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            
            // 设置分数类型
            ps.setString(6, answerScore.getScoreType());
            
            // 设置打分方法
            if (answerScore.getScoringMethod() != null) {
                ps.setString(7, answerScore.getScoringMethod());
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            
            // 设置评测ID
            if (answerScore.getEvaluation() != null && answerScore.getEvaluation().getId() != null) {
                ps.setLong(8, answerScore.getEvaluation().getId());
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }
            
            // 设置打分时间
            ps.setTimestamp(9, Timestamp.valueOf(answerScore.getScoringTime()));
            
            // 设置创建用户ID
            if (answerScore.getCreatedByUser() != null && answerScore.getCreatedByUser().getId() != null) {
                ps.setLong(10, answerScore.getCreatedByUser().getId());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            
            // 设置评论
            if (answerScore.getComments() != null) {
                ps.setString(11, answerScore.getComments());
            } else {
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            answerScore.setId(key.longValue());
        }
        return answerScore;
    }

    /**
     * 更新回答分数
     *
     * @param answerScore 回答分数对象
     * @return 更新后的回答分数对象
     */
    private AnswerScore update(AnswerScore answerScore) {
        jdbcTemplate.update(SQL_UPDATE,
                answerScore.getLlmAnswer().getId(),
                answerScore.getEvaluator().getId(),
                answerScore.getRawScore(),
                answerScore.getNormalizedScore(),
                answerScore.getWeightedScore(),
                answerScore.getScoreType(),
                answerScore.getScoringMethod(),
                answerScore.getEvaluation() != null ? answerScore.getEvaluation().getId() : null,
                Timestamp.valueOf(answerScore.getScoringTime()),
                answerScore.getCreatedByUser() != null ? answerScore.getCreatedByUser().getId() : null,
                answerScore.getComments(),
                answerScore.getId());

        return answerScore;
    }

    /**
     * 根据ID查找回答分数
     *
     * @param id 回答分数ID
     * @return 回答分数的Optional包装
     */
    public Optional<AnswerScore> findById(Long id) {
        try {
            AnswerScore answerScore = jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{id}, new AnswerScoreRowMapper());
            return Optional.ofNullable(answerScore);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据回答ID和评测者ID查找分数记录
     *
     * @param llmAnswerId 回答ID
     * @param evaluatorId 评测者ID
     * @return 分数记录列表
     */
    public List<AnswerScore> findByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID,
                new Object[]{llmAnswerId, evaluatorId},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据回答ID、评测者ID和分数类型查找分数记录
     *
     * @param llmAnswerId 回答ID
     * @param evaluatorId 评测者ID
     * @param scoreType 分数类型
     * @return 分数记录的Optional包装
     */
    public Optional<AnswerScore> findByLlmAnswerIdAndEvaluatorIdAndScoreType(Long llmAnswerId, Long evaluatorId, String scoreType) {
        try {
            AnswerScore answerScore = jdbcTemplate.queryForObject(
                    SQL_FIND_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID_AND_SCORE_TYPE,
                    new Object[]{llmAnswerId, evaluatorId, scoreType},
                    new AnswerScoreRowMapper()
            );
            return Optional.ofNullable(answerScore);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 检查指定回答、评测者和分数类型的分数记录是否存在
     *
     * @param llmAnswerId 回答ID
     * @param evaluatorId 评测者ID
     * @param scoreType 分数类型
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsByLlmAnswerIdAndEvaluatorIdAndScoreType(Long llmAnswerId, Long evaluatorId, String scoreType) {
        Integer count = jdbcTemplate.queryForObject(
                SQL_EXISTS_BY_LLM_ANSWER_ID_AND_EVALUATOR_ID_AND_SCORE_TYPE,
                Integer.class,
                llmAnswerId, evaluatorId, scoreType
        );
        return count != null && count > 0;
    }

    /**
     * 根据评测ID查找所有分数记录
     *
     * @param evaluationId 评测ID
     * @return 分数记录列表
     */
    public List<AnswerScore> findByEvaluationId(Long evaluationId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATION_ID,
                new Object[]{evaluationId},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据回答ID查找所有分数记录
     *
     * @param llmAnswerId 回答ID
     * @return 分数记录列表
     */
    public List<AnswerScore> findByLlmAnswerId(Long llmAnswerId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_LLM_ANSWER_ID,
                new Object[]{llmAnswerId},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据回答ID和分数类型查找所有分数记录
     *
     * @param llmAnswerId 回答ID
     * @param scoreType 分数类型
     * @return 分数记录列表
     */
    public List<AnswerScore> findByLlmAnswerIdAndScoreType(Long llmAnswerId, String scoreType) {
        return jdbcTemplate.query(
                SQL_FIND_BY_LLM_ANSWER_ID_AND_SCORE_TYPE,
                new Object[]{llmAnswerId, scoreType},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 查询指定回答的平均分数
     *
     * @param llmAnswerId 回答ID
     * @param scoreType 分数类型
     * @return 平均分数
     */
    public Double findAverageScoreByLlmAnswerIdAndScoreType(Long llmAnswerId, String scoreType) {
        return jdbcTemplate.queryForObject(
                SQL_FIND_AVERAGE_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE,
                Double.class,
                llmAnswerId, scoreType
        );
    }

    /**
     * 查询指定回答的最高分数
     *
     * @param llmAnswerId 回答ID
     * @param scoreType 分数类型
     * @return 最高分数
     */
    public Double findMaxScoreByLlmAnswerIdAndScoreType(Long llmAnswerId, String scoreType) {
        return jdbcTemplate.queryForObject(
                SQL_FIND_MAX_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE,
                Double.class,
                llmAnswerId, scoreType
        );
    }

    /**
     * 查询指定回答的最低分数
     *
     * @param llmAnswerId 回答ID
     * @param scoreType 分数类型
     * @return 最低分数
     */
    public Double findMinScoreByLlmAnswerIdAndScoreType(Long llmAnswerId, String scoreType) {
        return jdbcTemplate.queryForObject(
                SQL_FIND_MIN_SCORE_BY_LLM_ANSWER_ID_AND_SCORE_TYPE,
                Double.class,
                llmAnswerId, scoreType
        );
    }

    /**
     * 根据分数类型查找所有分数记录
     *
     * @param scoreType 分数类型
     * @return 分数记录列表
     */
    public List<AnswerScore> findByScoreType(String scoreType) {
        return jdbcTemplate.query(
                SQL_FIND_BY_SCORE_TYPE,
                new Object[]{scoreType},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据评测者ID查找所有分数记录
     *
     * @param evaluatorId 评测者ID
     * @return 分数记录列表
     */
    public List<AnswerScore> findByEvaluatorId(Long evaluatorId) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_ID,
                new Object[]{evaluatorId},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据评测者ID和分数类型查找所有分数记录
     *
     * @param evaluatorId 评测者ID
     * @param scoreType 分数类型
     * @return 分数记录列表
     */
    public List<AnswerScore> findByEvaluatorIdAndScoreType(Long evaluatorId, String scoreType) {
        return jdbcTemplate.query(
                SQL_FIND_BY_EVALUATOR_ID_AND_SCORE_TYPE,
                new Object[]{evaluatorId, scoreType},
                new AnswerScoreRowMapper()
        );
    }

    /**
     * 根据评测ID删除所有分数记录
     *
     * @param evaluationId 评测ID
     */
    public void deleteByEvaluationId(Long evaluationId) {
        jdbcTemplate.update(SQL_DELETE_BY_EVALUATION_ID, evaluationId);
    }
    
    /**
     * 查找所有分数记录
     *
     * @return 分数记录列表
     */
    public List<AnswerScore> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, new AnswerScoreRowMapper());
    }

    /**
     * 回答分数行映射器
     */
    private class AnswerScoreRowMapper implements RowMapper<AnswerScore> {
        @Override
        public AnswerScore mapRow(ResultSet rs, int rowNum) throws SQLException {
            AnswerScore answerScore = new AnswerScore();
            answerScore.setId(rs.getLong("id"));
            
            // 设置原始分数
            answerScore.setRawScore(rs.getBigDecimal("raw_score"));
            
            // 设置标准化分数
            BigDecimal normalizedScore = rs.getBigDecimal("normalized_score");
            if (!rs.wasNull()) {
                answerScore.setNormalizedScore(normalizedScore);
            }
            
            // 设置加权分数
            BigDecimal weightedScore = rs.getBigDecimal("weighted_score");
            if (!rs.wasNull()) {
                answerScore.setWeightedScore(weightedScore);
            }
            
            // 设置分数类型
            answerScore.setScoreType(rs.getString("score_type"));
            
            // 设置打分方法
            answerScore.setScoringMethod(rs.getString("scoring_method"));
            
            // 设置评论
            answerScore.setComments(rs.getString("comments"));
            
            // 设置打分时间
            Timestamp scoringTime = rs.getTimestamp("scoring_time");
            if (scoringTime != null) {
                answerScore.setScoringTime(scoringTime.toLocalDateTime());
            }
            
            // 处理外键关联
            // 设置LLM答案
            Long llmAnswerId = rs.getLong("llm_answer_id");
            if (!rs.wasNull()) {
                LlmAnswer llmAnswer = new LlmAnswer();
                llmAnswer.setId(llmAnswerId);
                answerScore.setLlmAnswer(llmAnswer);
            }
            
            // 设置评测者
            Long evaluatorId = rs.getLong("evaluator_id");
            if (!rs.wasNull()) {
                Evaluator evaluator = new Evaluator();
                evaluator.setId(evaluatorId);
                answerScore.setEvaluator(evaluator);
            }
            
            // 设置评测
            Long evaluationId = rs.getLong("evaluation_id");
            if (!rs.wasNull()) {
                Evaluation evaluation = new Evaluation();
                evaluation.setId(evaluationId);
                answerScore.setEvaluation(evaluation);
            }
            
            // 设置创建用户
            Long createdByUserId = rs.getLong("created_by_user_id");
            if (!rs.wasNull()) {
                userRepository.findById(createdByUserId).ifPresent(user -> answerScore.setCreatedByUser(user));
            }
            
            return answerScore;
        }
    }

    /**
     * 批量保存回答分数
     *
     * @param answerScores 回答分数对象列表
     * @return 带有ID的回答分数对象列表
     */
    public List<AnswerScore> saveAll(List<AnswerScore> answerScores) {
        return answerScores.stream()
                .map(this::save)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据ID删除回答分数
     *
     * @param id 回答分数ID
     */
    public void deleteById(Long id) {
        jdbcTemplate.update(SQL_DELETE_BY_ID, id);
    }

    /**
     * 批量删除回答分数
     *
     * @param answerScores 回答分数对象列表
     */
    public void deleteAll(List<AnswerScore> answerScores) {
        answerScores.forEach(answerScore -> deleteById(answerScore.getId()));
    }
} 
