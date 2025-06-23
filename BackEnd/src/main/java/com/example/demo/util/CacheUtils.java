package com.example.demo.util;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis缓存工具类
 * 提供自定义缓存操作方法
 */
@Component
public class CacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存查询结果
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（分钟）
     */
    public void cacheQueryResult(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.MINUTES);
            logger.debug("缓存查询结果: key={}, timeout={}分钟", key, timeout);
        } catch (Exception e) {
            logger.error("缓存查询结果失败: key={}", key, e);
        }
    }

    /**
     * 获取缓存的查询结果
     * @param key 缓存键
     * @param type 返回类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedQueryResult(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                logger.debug("命中缓存: key={}", key);
                return (T) value;
            }
        } catch (Exception e) {
            logger.error("获取缓存失败: key={}", key, e);
        }
        return null;
    }

    /**
     * 删除缓存
     * @param key 缓存键
     */
    public void evictCache(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("删除缓存: key={}", key);
        } catch (Exception e) {
            logger.error("删除缓存失败: key={}", key, e);
        }
    }

    /**
     * 批量删除缓存
     * @param pattern 缓存键模式
     */
    public void evictCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("批量删除缓存: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            logger.error("批量删除缓存失败: pattern={}", pattern, e);
        }
    }

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean hasCache(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void setExpire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            logger.debug("设置缓存过期时间: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("设置缓存过期时间失败: key={}", key, e);
        }
    }

    /**
     * 获取缓存剩余过期时间
     * @param key 缓存键
     * @return 剩余时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            logger.error("获取缓存过期时间失败: key={}", key, e);
            return -2;
        }
    }

    /**
     * 生成查询缓存键
     * @param prefix 前缀
     * @param params 参数
     * @return 缓存键
     */
    public String generateQueryCacheKey(String prefix, Object... params) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object param : params) {
            keyBuilder.append(":").append(param != null ? param.toString() : "null");
        }
        return keyBuilder.toString();
    }
} 