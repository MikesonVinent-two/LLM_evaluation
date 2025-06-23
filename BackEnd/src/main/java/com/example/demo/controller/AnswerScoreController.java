package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.jdbc.AnswerScore;
import com.example.demo.service.AnswerScoreService;
import com.example.demo.util.ApiConstants;

/**
 * 回答分数控制器
 */
@RestController
@RequestMapping("/scores")
public class AnswerScoreController {

    private static final Logger logger = LoggerFactory.getLogger(AnswerScoreController.class);
    
    private final AnswerScoreService answerScoreService;

    public AnswerScoreController(AnswerScoreService answerScoreService) {
        this.answerScoreService = answerScoreService;
    }

    /**
     * 根据回答ID获取所有分数
     */
    @GetMapping("/answer/{answerId}")
    public ResponseEntity<List<AnswerScore>> getScoresByAnswerId(@PathVariable Long answerId) {
        logger.info("获取回答ID为{}的所有分数", answerId);
        List<AnswerScore> scores = answerScoreService.getScoresByAnswerId(answerId);
        return ResponseEntity.ok(scores);
    }

    /**
     * 根据回答ID和评测者ID获取分数
     */
    @GetMapping("/answer/{answerId}/evaluator/{evaluatorId}")
    public ResponseEntity<List<AnswerScore>> getScoresByAnswerIdAndEvaluatorId(
            @PathVariable Long answerId,
            @PathVariable Long evaluatorId) {
        logger.info("获取回答ID为{}、评测者ID为{}的所有分数", answerId, evaluatorId);
        List<AnswerScore> scores = answerScoreService.getScoresByAnswerIdAndEvaluatorId(answerId, evaluatorId);
        return ResponseEntity.ok(scores);
    }

    /**
     * 根据回答ID、评测者ID和分数类型获取分数
     */
    @GetMapping("/answer/{answerId}/evaluator/{evaluatorId}/type/{scoreType}")
    public ResponseEntity<AnswerScore> getScoreByAnswerIdAndEvaluatorIdAndType(
            @PathVariable Long answerId,
            @PathVariable Long evaluatorId,
            @PathVariable String scoreType) {
        logger.info("获取回答ID为{}、评测者ID为{}、分数类型为{}的分数", answerId, evaluatorId, scoreType);
        AnswerScore score = answerScoreService.getScoreByAnswerIdAndEvaluatorIdAndType(answerId, evaluatorId, scoreType);
        return score != null ? ResponseEntity.ok(score) : ResponseEntity.notFound().build();
    }

    /**
     * 获取回答的评分统计信息
     */
    @GetMapping("/answer/{answerId}/statistics")
    public ResponseEntity<Map<String, Object>> getScoreStatistics(@PathVariable Long answerId) {
        logger.info("获取回答ID为{}的评分统计信息", answerId);
        Map<String, Object> statistics = answerScoreService.getScoreStatistics(answerId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 删除分数
     */
    @DeleteMapping("/{scoreId}")
    public ResponseEntity<Map<String, Object>> deleteScore(@PathVariable Long scoreId) {
        logger.info("删除分数ID为{}的记录", scoreId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            answerScoreService.deleteScore(scoreId);
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "分数记录删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除分数记录失败", e);
            response.put(ApiConstants.KEY_SUCCESS, false);
            response.put(ApiConstants.KEY_MESSAGE, "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除回答的所有分数
     */
    @DeleteMapping("/answer/{answerId}")
    public ResponseEntity<Map<String, Object>> deleteScoresByAnswerId(@PathVariable Long answerId) {
        logger.info("删除回答ID为{}的所有分数记录", answerId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            answerScoreService.deleteScoresByAnswerId(answerId);
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "回答的所有分数记录删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除回答的所有分数记录失败", e);
            response.put(ApiConstants.KEY_SUCCESS, false);
            response.put(ApiConstants.KEY_MESSAGE, "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 