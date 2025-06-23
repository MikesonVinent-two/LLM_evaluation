package com.example.demo.service.impl;

import com.example.demo.entity.jdbc.AnswerScore;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.AnswerScoreRepository;
import com.example.demo.service.AnswerScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 回答分数服务实现类
 */
@Service
public class AnswerScoreServiceImpl implements AnswerScoreService {

    private static final Logger logger = LoggerFactory.getLogger(AnswerScoreServiceImpl.class);
    
    private final AnswerScoreRepository answerScoreRepository;

    @Autowired
    public AnswerScoreServiceImpl(AnswerScoreRepository answerScoreRepository) {
        this.answerScoreRepository = answerScoreRepository;
    }

    @Override
    public List<AnswerScore> getScoresByAnswerId(Long answerId) {
        logger.debug("获取回答ID为{}的所有分数", answerId);
        return answerScoreRepository.findByLlmAnswerId(answerId);
    }

    @Override
    public List<AnswerScore> getScoresByAnswerIdAndEvaluatorId(Long answerId, Long evaluatorId) {
        logger.debug("获取回答ID为{}、评测者ID为{}的所有分数", answerId, evaluatorId);
        return answerScoreRepository.findByLlmAnswerIdAndEvaluatorId(answerId, evaluatorId);
    }

    @Override
    public AnswerScore getScoreByAnswerIdAndEvaluatorIdAndType(Long answerId, Long evaluatorId, String scoreType) {
        logger.debug("获取回答ID为{}、评测者ID为{}、分数类型为{}的分数", answerId, evaluatorId, scoreType);
        return answerScoreRepository.findByLlmAnswerIdAndEvaluatorIdAndScoreType(answerId, evaluatorId, scoreType)
                .orElse(null);
    }

    @Override
    @Transactional
    public AnswerScore saveScore(AnswerScore score) {
        logger.debug("保存分数记录");
        return answerScoreRepository.save(score);
    }

    @Override
    @Transactional
    public AnswerScore createScore(LlmAnswer answer, Evaluator evaluator, BigDecimal rawScore, 
                                 BigDecimal normalizedScore, String scoreType, String scoringMethod, 
                                 User createdByUser, String comments) {
        logger.debug("创建分数记录，回答ID: {}, 评测者ID: {}, 分数类型: {}", 
                answer.getId(), evaluator.getId(), scoreType);
        
        AnswerScore score = new AnswerScore();
        score.setLlmAnswer(answer);
        score.setEvaluator(evaluator);
        score.setRawScore(rawScore);
        score.setNormalizedScore(normalizedScore);
        score.setScoreType(scoreType);
        score.setScoringMethod(scoringMethod);
        score.setCreatedByUser(createdByUser);
        score.setComments(comments);
        score.setScoringTime(LocalDateTime.now());
        
        return answerScoreRepository.save(score);
    }

    @Override
    @Transactional
    public List<AnswerScore> saveAllScores(List<AnswerScore> scores) {
        logger.debug("批量保存{}条分数记录", scores.size());
        return answerScoreRepository.saveAll(scores);
    }

    @Override
    @Transactional
    public void deleteScore(Long scoreId) {
        logger.debug("删除分数记录，ID: {}", scoreId);
        answerScoreRepository.deleteById(scoreId);
    }

    @Override
    @Transactional
    public void deleteScoresByAnswerId(Long answerId) {
        logger.debug("删除回答ID为{}的所有分数记录", answerId);
        List<AnswerScore> scores = answerScoreRepository.findByLlmAnswerId(answerId);
        answerScoreRepository.deleteAll(scores);
    }

    @Override
    public BigDecimal calculateAverageScore(Long answerId, String scoreType) {
        logger.debug("计算回答ID为{}、分数类型为{}的平均分", answerId, scoreType);
        Double avgScore = answerScoreRepository.findAverageScoreByLlmAnswerIdAndScoreType(answerId, scoreType);
        return avgScore != null ? new BigDecimal(avgScore).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    public Map<String, Object> getScoreStatistics(Long answerId) {
        logger.debug("获取回答ID为{}的评分统计信息", answerId);
        
        List<AnswerScore> scores = answerScoreRepository.findByLlmAnswerId(answerId);
        Map<String, Object> statistics = new HashMap<>();
        
        if (scores.isEmpty()) {
            statistics.put("count", 0);
            statistics.put("average", BigDecimal.ZERO);
            statistics.put("highest", null);
            statistics.put("lowest", null);
            statistics.put("byType", new HashMap<>());
            return statistics;
        }
        
        // 计算总体统计
        int count = scores.size();
        BigDecimal totalNormalizedScore = scores.stream()
                .filter(s -> s.getNormalizedScore() != null)
                .map(AnswerScore::getNormalizedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal average = count > 0 ? 
                totalNormalizedScore.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
        
        AnswerScore highest = scores.stream()
                .filter(s -> s.getNormalizedScore() != null)
                .max(Comparator.comparing(AnswerScore::getNormalizedScore))
                .orElse(null);
        
        AnswerScore lowest = scores.stream()
                .filter(s -> s.getNormalizedScore() != null)
                .min(Comparator.comparing(AnswerScore::getNormalizedScore))
                .orElse(null);
        
        // 按类型分组统计
        Map<String, List<AnswerScore>> scoresByType = scores.stream()
                .collect(Collectors.groupingBy(AnswerScore::getScoreType));
        
        Map<String, Object> statsByType = new HashMap<>();
        
        for (Map.Entry<String, List<AnswerScore>> entry : scoresByType.entrySet()) {
            String type = entry.getKey();
            List<AnswerScore> typeScores = entry.getValue();
            
            int typeCount = typeScores.size();
            BigDecimal typeTotalScore = typeScores.stream()
                    .filter(s -> s.getNormalizedScore() != null)
                    .map(AnswerScore::getNormalizedScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal typeAverage = typeCount > 0 ? 
                    typeTotalScore.divide(new BigDecimal(typeCount), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
            
            AnswerScore typeHighest = typeScores.stream()
                    .filter(s -> s.getNormalizedScore() != null)
                    .max(Comparator.comparing(AnswerScore::getNormalizedScore))
                    .orElse(null);
            
            AnswerScore typeLowest = typeScores.stream()
                    .filter(s -> s.getNormalizedScore() != null)
                    .min(Comparator.comparing(AnswerScore::getNormalizedScore))
                    .orElse(null);
            
            Map<String, Object> typeStats = new HashMap<>();
            typeStats.put("count", typeCount);
            typeStats.put("average", typeAverage);
            typeStats.put("highest", typeHighest != null ? typeHighest.getNormalizedScore() : null);
            typeStats.put("lowest", typeLowest != null ? typeLowest.getNormalizedScore() : null);
            
            statsByType.put(type, typeStats);
        }
        
        // 组装结果
        statistics.put("count", count);
        statistics.put("average", average);
        statistics.put("highest", highest != null ? highest.getNormalizedScore() : null);
        statistics.put("lowest", lowest != null ? lowest.getNormalizedScore() : null);
        statistics.put("byType", statsByType);
        
        return statistics;
    }

    @Override
    public AnswerScore getHighestScore(Long answerId, String scoreType) {
        logger.debug("获取回答ID为{}、分数类型为{}的最高分", answerId, scoreType);
        
        List<AnswerScore> scores = answerScoreRepository.findByLlmAnswerIdAndScoreType(answerId, scoreType);
        return scores.stream()
                .filter(s -> s.getNormalizedScore() != null)
                .max(Comparator.comparing(AnswerScore::getNormalizedScore))
                .orElse(null);
    }

    @Override
    public AnswerScore getLowestScore(Long answerId, String scoreType) {
        logger.debug("获取回答ID为{}、分数类型为{}的最低分", answerId, scoreType);
        
        List<AnswerScore> scores = answerScoreRepository.findByLlmAnswerIdAndScoreType(answerId, scoreType);
        return scores.stream()
                .filter(s -> s.getNormalizedScore() != null)
                .min(Comparator.comparing(AnswerScore::getNormalizedScore))
                .orElse(null);
    }
} 