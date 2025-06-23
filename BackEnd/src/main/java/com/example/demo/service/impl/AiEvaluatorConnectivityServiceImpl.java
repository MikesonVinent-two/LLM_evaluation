package com.example.demo.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.repository.jdbc.EvaluatorRepository;
import com.example.demo.repository.jdbc.LlmModelRepository;
import com.example.demo.service.AiEvaluatorConnectivityService;

/**
 * 大模型评测员连通性测试服务实现类
 */
@Service
public class AiEvaluatorConnectivityServiceImpl implements AiEvaluatorConnectivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiEvaluatorConnectivityServiceImpl.class);
    
    private final EvaluatorRepository evaluatorRepository;
    private final LlmModelRepository llmModelRepository;
    private final RestTemplate restTemplate;
    
    // 用于测试的简单提示词
    private static final String TEST_PROMPT = "请用一句话回答：今天天气怎么样？";
    
    @Autowired
    public AiEvaluatorConnectivityServiceImpl(
            EvaluatorRepository evaluatorRepository,
            LlmModelRepository llmModelRepository,
            RestTemplate restTemplate) {
        this.evaluatorRepository = evaluatorRepository;
        this.llmModelRepository = llmModelRepository;
        this.restTemplate = restTemplate;
    }
    
    @Override
    public Map<String, Object> testConnectivity(Long evaluatorId) {
        logger.info("开始测试大模型评测员连通性，评测员ID: {}", evaluatorId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取评测员信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new IllegalArgumentException("未找到指定的评测员"));
            
            // 检查是否为AI评测员
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
                throw new IllegalArgumentException("指定的评测员不是大模型评测员");
            }
            
            // 获取关联的LLM模型
            LlmModel llmModelRef = evaluator.getLlmModel();
            if (llmModelRef == null) {
                throw new IllegalArgumentException("评测员没有关联的大模型");
            }
            
            // 使用LlmModelRepository加载完整的LlmModel对象
            final LlmModel llmModel = llmModelRepository.findById(llmModelRef.getId())
                .orElseThrow(() -> new IllegalArgumentException("未找到关联的大模型，ID: " + llmModelRef.getId()));
            
            // 记录模型信息
            Map<String, Object> modelInfo = new HashMap<>();
            modelInfo.put("id", llmModel.getId());
            modelInfo.put("name", llmModel.getName());
            modelInfo.put("provider", llmModel.getProvider());
            modelInfo.put("version", llmModel.getVersion());
            modelInfo.put("apiType", llmModel.getApiType() != null ? llmModel.getApiType() : "openai_compatible");
            result.put("modelInfo", modelInfo);
            
            // 确定API URL和密钥
            String apiUrl = llmModel.getApiUrl();
            String apiKey = llmModel.getApiKey();
            String model = llmModel.getName();
            String apiType = llmModel.getApiType() != null ? llmModel.getApiType() : "openai_compatible";
            
            // 检查API URL是否有效
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                logger.error("大模型API URL为空");
                result.put("success", false);
                result.put("message", "大模型API URL未配置");
                return result;
            }
            
            // 补全API URL路径
            apiUrl = buildApiUrl(apiUrl, apiType);
            
            logger.info("测试连接到API: {}, 模型: {}", apiUrl, model);
            
            // 检查模型名称是否有效
            if (model == null || model.trim().isEmpty()) {
                logger.error("大模型名称为空");
                result.put("success", false);
                result.put("message", "大模型名称未配置");
                return result;
            }
            
            // 检查API密钥是否有效
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.error("大模型API密钥为空");
                result.put("success", false);
                result.put("message", "大模型API密钥未配置");
                return result;
            }
            
            // 验证URL格式是否正确
            try {
                new java.net.URL(apiUrl);
            } catch (Exception e) {
                logger.error("大模型API URL格式无效: {}", apiUrl);
                result.put("success", false);
                result.put("message", "大模型API URL格式无效: " + e.getMessage());
                return result;
            }
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 准备请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位简洁的助手，请用简短的一句话回答问题。");
            messages.add(systemMessage);
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", TEST_PROMPT);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 100);
            
            // 发送请求并记录时间
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            Instant start = Instant.now();
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            Instant end = Instant.now();
            long responseTimeMs = Duration.between(start, end).toMillis();
            
            // 处理响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null && message.containsKey("content")) {
                        String content = (String) message.get("content");
                        logger.info("大模型连通性测试成功，响应内容: {}", content);
                        
                        result.put("success", true);
                        result.put("responseTime", responseTimeMs);
                        result.put("response", content);
                        result.put("message", "连通性测试成功");
                        return result;
                    }
                }
            }
            
            // 如果无法解析响应内容
            logger.warn("大模型返回了无效的响应格式");
            result.put("success", false);
            result.put("responseTime", responseTimeMs);
            result.put("message", "大模型返回了无效的响应格式");
            result.put("rawResponse", response.getBody());
            
        } catch (Exception e) {
            logger.error("大模型连通性测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "连通性测试失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据API类型和基础URL构建完整的API端点URL
     */
    private String buildApiUrl(String baseUrl, String apiType) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }
        
        // 移除URL末尾的斜杠
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // 如果apiType为空，尝试从URL判断API类型
        if (apiType == null || apiType.isEmpty()) {
            if (baseUrl.contains("openai.com")) {
                apiType = "openai";
            } else if (baseUrl.contains("anthropic.com")) {
                apiType = "anthropic";
            } else if (baseUrl.contains("baidu.com") || baseUrl.contains("wenxin")) {
                apiType = "baidu";
            } else if (baseUrl.contains("aliyun") || baseUrl.contains("tongyi") || baseUrl.contains("dashscope")) {
                apiType = "aliyun";
            } else if (baseUrl.contains("zhipu") || baseUrl.contains("chatglm")) {
                apiType = "zhipu";
            } else if (baseUrl.contains("azure")) {
                apiType = "azure";
            } else {
                apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            }
        }
        
        // 根据API类型返回不同的端点路径
        switch (apiType.toLowerCase()) {
            case "openai":
            case "openai_compatible":
                // 检查是否已包含完整路径
                if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/v1/chat/completions")) {
                    return baseUrl;
                }
                return baseUrl + "/v1/chat/completions";
            case "anthropic":
                return baseUrl + "/v1/messages";
            case "baidu":
                return baseUrl + "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            case "aliyun":
            case "tongyi":
            case "dashscope":
                return baseUrl + "/v1/services/aigc/text-generation/generation";
            case "zhipu":
            case "glm":
            case "chatglm":
                return baseUrl + "/v1/chat/completions";
            case "azure":
                // Azure OpenAI API通常需要在URL中包含部署ID
                if (baseUrl.contains("deployments")) {
                    return baseUrl;
                } else {
                    return baseUrl + "/deployments/{deployment-id}/chat/completions?api-version=2023-07-01-preview";
                }
            default:
                return baseUrl + "/v1/chat/completions"; // 默认使用OpenAI格式
        }
    }
} 