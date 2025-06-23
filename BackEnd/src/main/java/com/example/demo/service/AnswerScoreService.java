package com.example.demo.service;

import com.example.demo.entity.jdbc.AnswerScore;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 回答分数服务接口
 */
public interface AnswerScoreService {

    /**
     * 根据回答ID获取所有分数
     */
    List<AnswerScore> getScoresByAnswerId(Long answerId);

    /**
     * 根据回答ID和评测者ID获取分数
     */
    List<AnswerScore> getScoresByAnswerIdAndEvaluatorId(Long answerId, Long evaluatorId);

    /**
     * 根据回答ID、评测者ID和分数类型获取分数
     */
    AnswerScore getScoreByAnswerIdAndEvaluatorIdAndType(Long answerId, Long evaluatorId, String scoreType);

    /**
     * 创建或更新分数
     */
    AnswerScore saveScore(AnswerScore score);

    /**
     * 创建分数
     */
    AnswerScore createScore(LlmAnswer answer, Evaluator evaluator, BigDecimal rawScore, 
                          BigDecimal normalizedScore, String scoreType, String scoringMethod, 
                          User createdByUser, String comments);

    /**
     * 批量保存分数
     */
    List<AnswerScore> saveAllScores(List<AnswerScore> scores);

    /**
     * 删除分数
     */
    void deleteScore(Long scoreId);

    /**
     * 删除回答的所有分数
     */
    void deleteScoresByAnswerId(Long answerId);

    /**
     * 计算回答的平均分数
     */
    BigDecimal calculateAverageScore(Long answerId, String scoreType);

    /**
     * 获取回答的评分统计信息
     */
    Map<String, Object> getScoreStatistics(Long answerId);

    /**
     * 获取回答的最高分
     */
    AnswerScore getHighestScore(Long answerId, String scoreType);

    /**
     * 获取回答的最低分
     */
    AnswerScore getLowestScore(Long answerId, String scoreType);
} 