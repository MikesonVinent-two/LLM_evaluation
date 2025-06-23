package com.example.demo.manager;

import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class BatchStateManager {
    private static final Logger logger = LoggerFactory.getLogger(BatchStateManager.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final RedissonClient redissonClient;
    private com.example.demo.task.AnswerGenerationTask answerGenerationTask;

    private static final String BATCH_STATE_PREFIX = "batch:state:";
    private static final String BATCH_INTERRUPT_PREFIX = "batch:interrupt:";
    private static final String BATCH_LOCK_PREFIX = "batch:lock:";
    
    // 定义允许的状态转换
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();
    
    static {
        // 初始化PENDING状态的可转换状态
        Set<String> pendingTransitions = new HashSet<>();
        pendingTransitions.add("GENERATING_ANSWERS");
        pendingTransitions.add("PAUSED");
        pendingTransitions.add("FAILED");
        ALLOWED_TRANSITIONS.put("PENDING", pendingTransitions);
        
        // 初始化GENERATING_ANSWERS状态的可转换状态
        Set<String> generatingTransitions = new HashSet<>();
        generatingTransitions.add("PAUSED");
        generatingTransitions.add("COMPLETED");
        generatingTransitions.add("FAILED");
        ALLOWED_TRANSITIONS.put("GENERATING_ANSWERS", generatingTransitions);
        
        // 初始化PAUSED状态的可转换状态
        Set<String> pausedTransitions = new HashSet<>();
        pausedTransitions.add("RESUMING");
        pausedTransitions.add("FAILED");
        ALLOWED_TRANSITIONS.put("PAUSED", pausedTransitions);
        
        // 初始化RESUMING状态的可转换状态
        Set<String> resumingTransitions = new HashSet<>();
        resumingTransitions.add("GENERATING_ANSWERS");
        resumingTransitions.add("PAUSED");
        resumingTransitions.add("FAILED");
        ALLOWED_TRANSITIONS.put("RESUMING", resumingTransitions);
        
        // 初始化COMPLETED状态的可转换状态
        Set<String> completedTransitions = new HashSet<>();
        completedTransitions.add("FAILED");
        ALLOWED_TRANSITIONS.put("COMPLETED", completedTransitions);
        
        // 初始化FAILED状态的可转换状态
        ALLOWED_TRANSITIONS.put("FAILED", new HashSet<>());
    }

    @Autowired
    public BatchStateManager(RedisTemplate<String, String> redisTemplate, 
                            JdbcTemplate jdbcTemplate, 
                            RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.redissonClient = redissonClient;
    }
    
    @Autowired
    @Lazy
    public void setAnswerGenerationTask(com.example.demo.task.AnswerGenerationTask answerGenerationTask) {
        this.answerGenerationTask = answerGenerationTask;
    }

    /**
     * 获取批次状态锁
     * @param batchId 批次ID
     * @return 锁对象
     */
    public RLock getBatchLock(Long batchId) {
        return redissonClient.getLock(BATCH_LOCK_PREFIX + batchId);
    }

    /**
     * 暂停批次
     * @param batchId 批次ID
     * @param reason 暂停原因
     * @return 是否成功暂停
     */
    public boolean pauseBatch(Long batchId, String reason) {
        RLock lock = getBatchLock(batchId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到批次{}的锁，开始暂停操作", batchId);
                    
                    // 1. 检查当前状态
                    String currentDbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}当前数据库状态: {}", batchId, currentDbStatus);
                    
                    // 2. 验证状态转换是否合法
                    if (!ALLOWED_TRANSITIONS.containsKey(currentDbStatus) || 
                        !ALLOWED_TRANSITIONS.get(currentDbStatus).contains("PAUSED")) {
                        logger.warn("批次{}当前状态{}不允许转换为PAUSED", batchId, currentDbStatus);
                        return false;
                    }
                    
                    // 3. 设置中断标志
                    setInterruptFlag(batchId, true);
                    logger.info("批次{}已设置中断标志", batchId);
                    
                    // 4. 更新Redis状态
                    setBatchState(batchId, "PAUSED");
                    logger.info("批次{}Redis状态已更新为PAUSED", batchId);
                    
                    // 5. 更新数据库状态
                    int updated = jdbcTemplate.update(
                        "UPDATE answer_generation_batches SET status = ?, pause_time = ?, pause_reason = ?, last_activity_time = ? WHERE id = ?",
                        "PAUSED", LocalDateTime.now(), reason, LocalDateTime.now(), batchId);
                    
                    // 6. 更新运行状态
                    int runUpdated = jdbcTemplate.update(
                        "UPDATE model_answer_runs SET status = ?, pause_time = ?, pause_reason = ?, last_activity_time = ? " +
                        "WHERE answer_generation_batch_id = ? AND (status = 'GENERATING_ANSWERS' OR status = 'RESUMING' OR status = 'PENDING')",
                        "PAUSED", LocalDateTime.now(), reason, LocalDateTime.now(), batchId);
                    
                    logger.info("批次{}数据库状态更新结果: 批次={}, 运行={}", batchId, updated, runUpdated);
                    
                    // 7. 验证状态更新
                    String finalStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}最终状态: {}", batchId, finalStatus);
                    
                    return "PAUSED".equals(finalStatus);
                } finally {
                    lock.unlock();
                    logger.info("批次{}的锁已释放", batchId);
                }
            } else {
                logger.warn("无法获取批次{}的锁，暂停操作失败", batchId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取批次{}的锁时被中断", batchId, e);
            return false;
        } catch (Exception e) {
            logger.error("暂停批次{}时发生错误", batchId, e);
            return false;
        }
    }

    /**
     * 恢复批次
     * @param batchId 批次ID
     * @return 是否成功恢复
     */
    public boolean resumeBatch(Long batchId) {
        RLock lock = getBatchLock(batchId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到批次{}的锁，开始恢复操作", batchId);
                    
                    // 1. 检查当前状态
                    String currentDbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    logger.info("批次{}当前数据库状态: {}", batchId, currentDbStatus);
                    
                    // 修正可能的状态值异常
                    if (currentDbStatus != null && currentDbStatus.contains("bootRun")) {
                        String correctedStatus = currentDbStatus.replace("bootRun", "");
                        logger.warn("批次{}状态值异常: {}，修正为: {}", batchId, currentDbStatus, correctedStatus);
                        
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET status = ? WHERE id = ?",
                            correctedStatus, batchId);
                        
                        currentDbStatus = correctedStatus;
                    }
                    
                    // 2. 验证状态转换是否合法
                    if (!"PAUSED".equals(currentDbStatus)) {
                        logger.warn("批次{}当前状态{}不是PAUSED，无法恢复", batchId, currentDbStatus);
                        return false;
                    }
                    
                    // 3. 执行简化的状态更新流程：先清除中断标志，再更新状态
                    // 3.1 清除中断标志 (Redis)
                    setInterruptFlag(batchId, false);
                    logger.info("批次{}已清除中断标志", batchId);
                    
                    // 3.2 更新Redis状态
                    setBatchState(batchId, "RESUMING");
                    logger.info("批次{}Redis状态已更新为RESUMING", batchId);
                    
                    // 3.3 如果有任务实例，清除任务内存中的中断标志
                    if (answerGenerationTask != null) {
                        answerGenerationTask.clearInterruptionFlag(batchId);
                        answerGenerationTask.updateBatchMemoryState(batchId, "RESUMING");
                        logger.info("批次{}的任务内存状态已更新为RESUMING", batchId);
                    }
                    
                    // 4. 更新数据库状态
                    int updated = jdbcTemplate.update(
                        "UPDATE answer_generation_batches SET status = ?, last_activity_time = ? WHERE id = ?",
                        "RESUMING", LocalDateTime.now(), batchId);
                    
                    // 5. 更新运行状态
                    int runUpdated = jdbcTemplate.update(
                        "UPDATE model_answer_runs SET status = ?, last_activity_time = ? " +
                        "WHERE answer_generation_batch_id = ? AND status = 'PAUSED'",
                        "RESUMING", LocalDateTime.now(), batchId);
                    
                    logger.info("批次{}数据库状态更新结果: 批次={}, 运行={}", batchId, updated, runUpdated);
                    
                    // 6. 返回状态更新是否成功
                    boolean success = updated > 0;
                    
                    // 7. 二次确认Redis状态
                    if (success) {
                        String redisState = getBatchState(batchId);
                        if (!"RESUMING".equals(redisState)) {
                            logger.warn("批次{}数据库状态更新为RESUMING后，Redis状态仍为{}，再次更新Redis状态", batchId, redisState);
                            setBatchState(batchId, "RESUMING");
                        }
                    }
                    
                    return success;
                } finally {
                    lock.unlock();
                    logger.info("批次{}的锁已释放", batchId);
                }
            } else {
                logger.warn("无法获取批次{}的锁，恢复操作失败", batchId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取批次{}的锁时被中断", batchId, e);
            return false;
        } catch (Exception e) {
            logger.error("恢复批次{}时发生错误", batchId, e);
            return false;
        }
    }

    /**
     * 设置批次状态
     * @param batchId 批次ID
     * @param state 状态
     */
    public void setBatchState(Long batchId, String state) {
        String key = BATCH_STATE_PREFIX + batchId;
        redisTemplate.opsForValue().set(key, state);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    /**
     * 获取批次状态
     * @param batchId 批次ID
     * @return 状态
     */
    public String getBatchState(Long batchId) {
        String key = BATCH_STATE_PREFIX + batchId;
        String state = redisTemplate.opsForValue().get(key);
        if (state == null) {
            // 如果Redis中没有状态，从数据库获取
            try {
                state = jdbcTemplate.queryForObject(
                    "SELECT status FROM answer_generation_batches WHERE id = ?", 
                    String.class, batchId);
                if (state != null) {
                    // 更新Redis
                    setBatchState(batchId, state);
                }
            } catch (Exception e) {
                logger.error("从数据库获取批次{}状态失败", batchId, e);
            }
        }
        return state;
    }

    /**
     * 设置中断标志
     * @param batchId 批次ID
     * @param interrupted 是否中断
     */
    public void setInterruptFlag(Long batchId, boolean interrupted) {
        String key = BATCH_INTERRUPT_PREFIX + batchId;
        redisTemplate.opsForValue().set(key, interrupted ? "true" : "false");
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    /**
     * 检查批次是否被标记为中断
     * @param batchId 批次ID
     * @return 是否中断
     */
    public boolean isInterrupted(Long batchId) {
        String key = BATCH_INTERRUPT_PREFIX + batchId;
        String value = redisTemplate.opsForValue().get(key);
        return "true".equals(value);
    }

    /**
     * 同步批次状态
     * 确保Redis和数据库状态一致
     * @param batchId 批次ID
     */
    public void syncBatchState(Long batchId) {
        RLock lock = getBatchLock(batchId);
        try {
            if (lock.tryLock(2, 10, TimeUnit.SECONDS)) {
                try {
                    String dbStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM answer_generation_batches WHERE id = ?", 
                        String.class, batchId);
                    
                    String redisStatus = getBatchState(batchId);
                    
                    if (dbStatus != null && !dbStatus.equals(redisStatus)) {
                        logger.info("同步批次{}状态: 数据库={}, Redis={}", batchId, dbStatus, redisStatus);
                        setBatchState(batchId, dbStatus);
                        
                        // 如果数据库状态是PAUSED，确保设置中断标志
                        if ("PAUSED".equals(dbStatus)) {
                            setInterruptFlag(batchId, true);
                        } else if ("RESUMING".equals(dbStatus) || "GENERATING_ANSWERS".equals(dbStatus)) {
                            setInterruptFlag(batchId, false);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("同步批次{}状态时被中断", batchId, e);
        } catch (Exception e) {
            logger.error("同步批次{}状态时发生错误", batchId, e);
        }
    }
} 