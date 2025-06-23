package com.example.demo.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        
        if (password != null && !password.isEmpty()) {
            config.useSingleServer()
                  .setAddress(address)
                  .setPassword(password);
        } else {
            config.useSingleServer()
                  .setAddress(address);
        }
        
        return Redisson.create(config);
    }

    /**
     * 配置Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认30分钟过期
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存空值

        // 针对不同缓存区域的个性化配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 标签缓存 - 长期缓存，因为标签相对稳定
        cacheConfigurations.put("tags", defaultCacheConfig.entryTtl(Duration.ofHours(2)));
        
        // 模型信息缓存 - 长期缓存
        cacheConfigurations.put("llm_models", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 问题标签关联缓存 - 中期缓存
        cacheConfigurations.put("question_tags", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 数据集版本缓存 - 长期缓存
        cacheConfigurations.put("dataset_versions", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 标准答案缓存 - 长期缓存
        cacheConfigurations.put("standard_answers", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // 用户信息缓存 - 短期缓存
        cacheConfigurations.put("users", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 查询结果缓存 - 短期缓存，用于复杂查询
        cacheConfigurations.put("query_results", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 统计数据缓存 - 短期缓存
        cacheConfigurations.put("statistics", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
} 
