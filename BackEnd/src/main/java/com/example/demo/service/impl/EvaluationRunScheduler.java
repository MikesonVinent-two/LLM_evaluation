package com.example.demo.service.impl;

import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.EvaluationRun.RunStatus;
import com.example.demo.repository.jdbc.EvaluationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class EvaluationRunScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRunScheduler.class);
    
    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationServiceImpl evaluationService;
    
    @Autowired
    public EvaluationRunScheduler(
            EvaluationRunRepository evaluationRunRepository,
            EvaluationServiceImpl evaluationService) {
        this.evaluationRunRepository = evaluationRunRepository;
        this.evaluationService = evaluationService;
    }
    
    /**
     * 每分钟检查一次超时的评测运行
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkTimeoutRuns() {
        logger.trace("开始检查超时的评测运行");
        
        LocalDateTime now = LocalDateTime.now();
        List<RunStatus> activeStatuses = Arrays.asList(
                RunStatus.IN_PROGRESS, RunStatus.PENDING, RunStatus.RESUMING);
        
        List<EvaluationRun> staleRuns = evaluationRunRepository.findStaleRuns(activeStatuses, 
                now.minusSeconds(3600)); // 默认1小时超时
        
        for (EvaluationRun run : staleRuns) {
            handleStaleRun(run);
        }
    }
    
    /**
     * 每30秒检查一次需要自动恢复的评测运行
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkAutoResumeRuns() {
        logger.trace("开始检查需要自动恢复的评测运行");
        
        LocalDateTime now = LocalDateTime.now();
        List<EvaluationRun> staleRuns = evaluationRunRepository.findStaleRunsForAutoResume(
                RunStatus.PAUSED, now.minusMinutes(5)); // 暂停5分钟后自动恢复
        
        for (EvaluationRun run : staleRuns) {
            try {
                evaluationService.resumeEvaluationRun(run.getId());
                logger.info("自动恢复评测运行，运行ID: {}", run.getId());
            } catch (Exception e) {
                logger.error("自动恢复评测运行失败，运行ID: " + run.getId(), e);
            }
        }
    }
    
    /**
     * 处理超时或停滞的评测运行
     */
    private void handleStaleRun(EvaluationRun run) {
        LocalDateTime lastActivity = run.getLastActivityTime();
        if (lastActivity == null) {
            lastActivity = run.getRunTime();
        }
        
        LocalDateTime timeoutThreshold = LocalDateTime.now()
                .minusSeconds(run.getTimeoutSeconds() != null ? run.getTimeoutSeconds() : 3600);
        
        if (lastActivity.isBefore(timeoutThreshold)) {
            // 如果超时，标记为失败
            evaluationRunRepository.updateRunStatus(
                    run.getId(),
                    RunStatus.FAILED,
                    "评测运行超时: 超过 " + (run.getTimeoutSeconds() / 60) + " 分钟没有活动"
            );
            logger.warn("评测运行超时，已标记为失败。运行ID: {}", run.getId());
        } else {
            // 如果只是停滞，尝试恢复
            if (run.getIsAutoResume() && run.getStatus() != RunStatus.FAILED) {
                try {
                    evaluationService.resumeEvaluationRun(run.getId());
                    logger.info("已尝试恢复停滞的评测运行，运行ID: {}", run.getId());
                } catch (Exception e) {
                    logger.error("恢复停滞的评测运行失败，运行ID: " + run.getId(), e);
                }
            }
        }
    }
} 