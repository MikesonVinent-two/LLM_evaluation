package com.example.demo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * RestTemplate配置类，用于创建和配置RestTemplate实例
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    /**
     * 创建RestTemplate Bean
     * 
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(500))
                .setReadTimeout(Duration.ofSeconds(3000))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }
    
    /**
     * 创建ClientHttpRequestFactory
     * 
     * @return ClientHttpRequestFactory实例
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时，单位毫秒
        factory.setReadTimeout(30000);   // 读取超时，单位毫秒
        return factory;
    }
    
    /**
     * 根据模型类型获取具有特定超时设置的RestTemplate实例
     * 
     * @param builder RestTemplateBuilder
     * @param modelName 模型名称
     * @return 自定义的RestTemplate实例
     */
    public RestTemplate getModelSpecificRestTemplate(RestTemplateBuilder builder, String modelName) {
        // 根据模型名称设置不同的超时时间
        int readTimeoutSeconds = 1200; // 默认2分钟
        
        // 大型模型需要更长的超时时间
        if (modelName != null && !modelName.isEmpty()) {
            String modelNameLower = modelName.toLowerCase();
            
            // 思考类模型 - 最长超时时间（10分钟）
            if (modelNameLower.contains("gpt-4-turbo") || 
                modelNameLower.contains("gpt-4o") ||
                modelNameLower.contains("claude-3-opus") ||
                modelNameLower.contains("claude-3-sonnet") ||
                modelNameLower.contains("gemini-pro") ||
                modelNameLower.contains("llama-3") ||
                modelNameLower.contains("mixtral") ||
                modelNameLower.contains("qwen") ||
                modelNameLower.contains("glm-4") ||
                modelNameLower.contains("deepseek-r1") ||
                modelNameLower.contains("grok")) {
                readTimeoutSeconds = 6000; // 10分钟
                
            // GPT-4系列其他模型 - 长超时时间（5分钟）
            } else if (modelNameLower.contains("gpt-4")) {
                readTimeoutSeconds = 3000; // 5分钟
                
            // Claude系列其他模型 - 中等超时时间（4分钟）
            } else if (modelNameLower.contains("claude")) {
                readTimeoutSeconds = 2400; // 4分钟
                
            // 常规模型 - 标准超时时间（3分钟）
            } else if (modelNameLower.contains("gpt-3.5") ||
                       modelNameLower.contains("chatglm") ||
                       modelNameLower.contains("spark") ||
                       modelNameLower.contains("ernie") ||
                       modelNameLower.contains("baichuan") ||
                       modelNameLower.contains("qwen-turbo")) {
                readTimeoutSeconds = 1800; // 3分钟
            }
        }
        
        logger.info("为模型 [{}] 配置超时时间: {}秒", modelName, readTimeoutSeconds);
        
        // 创建并配置RestTemplate
        return builder
                .setConnectTimeout(Duration.ofSeconds(30)) // 连接超时30秒
                .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }
} 