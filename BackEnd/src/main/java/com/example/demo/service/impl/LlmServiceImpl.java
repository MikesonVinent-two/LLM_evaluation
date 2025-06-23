package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.config.LlmConfig;
import com.example.demo.config.RestTemplateConfig;
import com.example.demo.dto.LlmRequestDTO;
import com.example.demo.dto.LlmResponseDTO;
import com.example.demo.dto.ModelInfoDTO;
import com.example.demo.service.LlmService;

@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final RestTemplateConfig restTemplateConfig;
    private final LlmConfig llmConfig;

    @Autowired
    public LlmServiceImpl(RestTemplate restTemplate, RestTemplateBuilder restTemplateBuilder, 
                         RestTemplateConfig restTemplateConfig, LlmConfig llmConfig) {
        this.restTemplate = restTemplate;
        this.restTemplateBuilder = restTemplateBuilder;
        this.restTemplateConfig = restTemplateConfig;
        this.llmConfig = llmConfig;
    }

    /**
     * 根据API类型和基础URL构建完整的API端点URL
     */
    private String buildApiUrl(String baseUrl) {
        // 移除URL末尾的斜杠
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // 根据API类型添加对应的端点路径
        String apiType = "openai_compatible"; // 默认为OpenAI兼容格式
        
        // 尝试从URL判断API类型
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
        }
        
        // 根据API类型返回不同的端点路径
        switch (apiType.toLowerCase()) {
            case "openai":
            case "openai_compatible":
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

    @Override
    @Retryable(
        value = {RestClientException.class},
        maxAttemptsExpression = "#{@llmConfig.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@llmConfig.retry.backoffDelay}")
    )
    public LlmResponseDTO sendRequest(LlmRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        // 如果API URL为空，使用默认值
        if (request.getApiUrl() == null || request.getApiUrl().isEmpty()) {
            request.setApiUrl(llmConfig.getDefaultApiUrl());
            logger.info("使用默认API URL: {}", llmConfig.getDefaultApiUrl());
        }
        
        // 如果模型为空，使用默认值
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(llmConfig.getDefaultModel());
            logger.info("使用默认模型: {}", llmConfig.getDefaultModel());
        }
        
        // 根据API类型和基础URL构建完整的API端点URL
        String apiType = request.getApi().toLowerCase();
        String apiUrl;
        
        switch (apiType) {
            case "openai":
            case "openai_compatible":
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "v1/chat/completions" 
                    : request.getApiUrl() + "/v1/chat/completions";
                break;
            case "anthropic":
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "v1/messages" 
                    : request.getApiUrl() + "/v1/messages";
                break;
            case "baidu":
            case "wenxin":
            case "ernie":
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions" 
                    : request.getApiUrl() + "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
                break;
            case "aliyun":
            case "tongyi":
            case "dashscope":
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "v1/services/aigc/text-generation/generation" 
                    : request.getApiUrl() + "/v1/services/aigc/text-generation/generation";
                break;
            case "zhipu":
            case "glm":
            case "chatglm":
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "v1/chat/completions" 
                    : request.getApiUrl() + "/v1/chat/completions";
                break;
            case "azure":
                if (request.getApiUrl().contains("deployments")) {
                    apiUrl = request.getApiUrl();
                } else {
                    // 注意：Azure API需要提供部署ID和API版本
                    apiUrl = request.getApiUrl().endsWith("/") 
                        ? request.getApiUrl() + "deployments/" + request.getModel() + "/chat/completions?api-version=2023-07-01-preview" 
                        : request.getApiUrl() + "/deployments/" + request.getModel() + "/chat/completions?api-version=2023-07-01-preview";
                }
                break;
            default:
                // 默认使用通用的chat completions端点
                apiUrl = request.getApiUrl().endsWith("/") 
                    ? request.getApiUrl() + "v1/chat/completions" 
                    : request.getApiUrl() + "/v1/chat/completions";
                break;
        }
        
        logger.info("构建完整API URL: {}", apiUrl);
        
        try {
            logger.info("发送请求到LLM API: {}, 模型: {}", apiUrl, request.getModel());
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 不同API类型可能有不同的认证方式
            if (apiType.equals("aliyun") || apiType.equals("tongyi") || apiType.equals("dashscope")) {
                headers.set("Authorization", "Bearer " + request.getApiKey());
                headers.set("X-DashScope-ApiKey", request.getApiKey());
            } else if (apiType.equals("zhipu") || apiType.equals("glm") || apiType.equals("chatglm")) {
                headers.set("Authorization", "Bearer " + request.getApiKey());
            } else if (apiType.equals("azure")) {
                headers.set("api-key", request.getApiKey());
            } else {
                // 默认使用Bearer认证
            headers.set("Authorization", "Bearer " + request.getApiKey());
            }
            
            // 根据不同的API调用方式构建不同的请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 创建HTTP实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 根据模型类型获取特定的RestTemplate
            RestTemplate modelSpecificRestTemplate = 
                restTemplateConfig.getModelSpecificRestTemplate(restTemplateBuilder, request.getModel());
            
            // 记录实际使用的超时设置
            logger.info("模型 {} 使用的超时设置: {}", request.getModel(), 
                       modelSpecificRestTemplate.getRequestFactory().toString());
            
            // 发送请求
            ResponseEntity<Map> response = modelSpecificRestTemplate.postForEntity(
                    apiUrl,
                    entity,
                    Map.class
            );
            
            // 处理响应
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            logger.info("LLM API响应成功，耗时: {}ms", responseTime);
            
            return parseResponse(response.getBody(), request.getModel(), responseTime);
            
        } catch (RestClientException e) {
            logger.error("API请求失败: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "API请求失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理请求时发生错误: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "处理请求时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 根据不同的API调用方式构建请求体
     */
    private Map<String, Object> buildRequestBody(LlmRequestDTO request) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 判断API调用方式并构建相应的请求体
        String apiType = request.getApi().toLowerCase();
        
        if (apiType.equals("openai_compatible") || apiType.equals("openai")) {
            // OpenAI API格式
            requestBody.put("model", request.getModel());
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            } else {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个有用的AI助手。");
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
        } else if (apiType.equals("anthropic")) {
            // Anthropic API格式
            requestBody.put("model", request.getModel());
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                Map<String, String> systemPrompt = request.getSystemPrompts().get(0);
                if (systemPrompt.containsKey("content")) {
                    requestBody.put("system", systemPrompt.get("content"));
                }
            }
            
            // 添加用户消息
            requestBody.put("prompt", request.getMessage());
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
        } else if (apiType.equals("baidu") || apiType.equals("wenxin") || apiType.equals("ernie")) {
            // 百度文心一言API格式
            logger.info("使用百度文心一言API格式");
            
            Map<String, Object> messages = new HashMap<>();
            messages.put("role", "user");
            messages.put("content", request.getMessage());
            
            List<Map<String, Object>> messageList = new ArrayList<>();
            messageList.add(messages);
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                for (Map<String, String> prompt : request.getSystemPrompts()) {
                    if (prompt.containsKey("role") && prompt.containsKey("content")) {
                        Map<String, Object> systemMsg = new HashMap<>();
                        systemMsg.put("role", prompt.get("role"));
                        systemMsg.put("content", prompt.get("content"));
                        messageList.add(0, systemMsg); // 系统消息放在最前面
                    }
                }
            }
            
            requestBody.put("messages", messageList);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("top_p", 0.8); // 百度API通常使用top_p
            }
            
            // 添加百度特有参数
            requestBody.put("stream", false);
            requestBody.put("user_id", "user_" + System.currentTimeMillis());
        } else if (apiType.equals("aliyun") || apiType.equals("tongyi") || apiType.equals("dashscope")) {
            // 阿里通义千问API格式
            logger.info("使用阿里通义千问API格式");
            
            Map<String, Object> parameters = new HashMap<>();
            
            // 添加参数
            if (request.getTemperature() != null) {
                parameters.put("temperature", request.getTemperature());
            } else {
                parameters.put("temperature", 0.7);
            }
            
            if (request.getMaxTokens() != null) {
                parameters.put("max_tokens", request.getMaxTokens());
            } else {
                parameters.put("max_tokens", 2048);
            }
            
            parameters.put("top_p", 0.8);
            parameters.put("result_format", "text");
            
            requestBody.put("model", request.getModel());
            requestBody.put("parameters", parameters);
            
            // 构建输入内容
            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            input.put("messages", messages);
            requestBody.put("input", input);
        } else if (apiType.equals("zhipu") || apiType.equals("glm") || apiType.equals("chatglm")) {
            // 智谱GLM API格式
            logger.info("使用智谱GLM API格式");
            
            requestBody.put("model", request.getModel());
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                Map<String, String> systemMessage = request.getSystemPrompts().get(0);
                if (systemMessage.containsKey("content")) {
                    Map<String, String> sysMsg = new HashMap<>();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", systemMessage.get("content"));
                    messages.add(sysMsg);
                }
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            } else {
                requestBody.put("temperature", 0.7);
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            } else {
                requestBody.put("max_tokens", 2048);
            }
            
            // 智谱特有参数
            requestBody.put("stream", false);
            requestBody.put("top_p", 0.7);
        } else if (apiType.equals("azure")) {
            // Azure OpenAI API格式
            logger.info("使用Azure OpenAI API格式");
            
            requestBody.put("model", request.getModel());
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            } else {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个有用的AI助手。");
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            
            // Azure特有参数
            requestBody.put("stream", false);
        } else {
            // 默认格式，使用通用的chat completions格式
            logger.info("使用默认格式处理API类型: {}", apiType);
            requestBody.put("model", request.getModel());
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            } else {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个有用的AI助手。");
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            
            // 添加其他自定义参数
            if (request.getAdditionalParams() != null) {
                requestBody.putAll(request.getAdditionalParams());
            }
        }
        
        return requestBody;
    }
    
    /**
     * 解析不同API的响应
     */
    private LlmResponseDTO parseResponse(Map<String, Object> responseBody, String model, long responseTime) {
        if (responseBody == null) {
            logger.warn("API返回空响应");
            return new LlmResponseDTO(false, "API返回空响应");
        }
        
        try {
            String content = "";
            Integer tokenCount = null;
            
            // 解析不同API的响应格式
            if (responseBody.containsKey("choices")) {
                // OpenAI格式
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    
                    if (choice.containsKey("message")) {
                        Map<String, String> message = (Map<String, String>) choice.get("message");
                        content = message.get("content");
                    } else if (choice.containsKey("text")) {
                        content = (String) choice.get("text");
                    } else if (choice.containsKey("delta")) {
                        Map<String, String> delta = (Map<String, String>) choice.get("delta");
                        if (delta.containsKey("content")) {
                            content = delta.get("content");
                        }
                    }
                }
                
                // 获取token计数
                if (responseBody.containsKey("usage")) {
                    Map<String, Integer> usage = (Map<String, Integer>) responseBody.get("usage");
                    tokenCount = usage.get("total_tokens");
                }
            } else if (responseBody.containsKey("content")) {
                // 简单格式
                content = (String) responseBody.get("content");
            } else if (responseBody.containsKey("completion")) {
                // 某些API使用completion字段
                content = (String) responseBody.get("completion");
            } else if (responseBody.containsKey("result")) {
                // 百度文心一言格式
                content = (String) responseBody.get("result");
                
                // 获取token计数（如果有）
                if (responseBody.containsKey("usage")) {
                    Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                    if (usage.containsKey("total_tokens")) {
                        tokenCount = (Integer) usage.get("total_tokens");
                    }
                }
            } else if (responseBody.containsKey("output")) {
                // 阿里通义千问格式
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                if (output.containsKey("text")) {
                    content = (String) output.get("text");
                } else if (output.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                    if (!choices.isEmpty() && choices.get(0).containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        if (message.containsKey("content")) {
                            content = (String) message.get("content");
                        }
                    }
                }
                
                // 获取token计数（如果有）
                if (responseBody.containsKey("usage")) {
                    Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                    if (usage.containsKey("total_tokens")) {
                        tokenCount = (Integer) usage.get("total_tokens");
                    }
                }
            } else if (responseBody.containsKey("response") && responseBody.get("response") instanceof String) {
                // 智谱GLM格式
                content = (String) responseBody.get("response");
                
                // 获取token计数（如果有）
                if (responseBody.containsKey("usage")) {
                    Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                    if (usage.containsKey("total_tokens")) {
                        tokenCount = (Integer) usage.get("total_tokens");
                    }
                }
            } else if (responseBody.containsKey("data")) {
                // 一些API将结果包装在data字段中
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                if (data.containsKey("choices")) {
                    // 类似OpenAI格式
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        
                        if (choice.containsKey("message")) {
                            Map<String, String> message = (Map<String, String>) choice.get("message");
                            content = message.get("content");
                        } else if (choice.containsKey("text")) {
                            content = (String) choice.get("text");
                        }
                    }
                } else if (data.containsKey("content")) {
                    content = (String) data.get("content");
                } else if (data.containsKey("result")) {
                    content = (String) data.get("result");
                }
            }
            
            if (content == null || content.isEmpty()) {
                // 在找不到特定格式时，尝试递归遍历所有字段
                content = extractContentRecursively(responseBody);
            }
            
            if (content == null || content.isEmpty()) {
                logger.warn("无法从响应中提取内容: {}", responseBody);
                return new LlmResponseDTO(false, "无法从响应中提取内容");
            }
            
            LlmResponseDTO response = new LlmResponseDTO(content, model, tokenCount, responseTime, true);
            response.setMetadata(responseBody);
            
            return response;
        } catch (Exception e) {
            logger.error("解析API响应时发生错误: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "解析API响应时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 递归遍历响应体，尝试提取内容
     */
    private String extractContentRecursively(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof String) {
            return (String) obj;
        }
        
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            
            // 优先查找可能包含内容的关键字段
            String[] contentKeys = {"content", "text", "result", "response", "message", "answer", "completion"};
            for (String key : contentKeys) {
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    if (value instanceof String) {
                        return (String) value;
                    } else {
                        String content = extractContentRecursively(value);
                        if (content != null && !content.isEmpty()) {
                            return content;
                        }
                    }
                }
            }
            
            // 如果找不到关键字段，遍历所有字段
            for (Object value : map.values()) {
                String content = extractContentRecursively(value);
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }
        }
        
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty()) {
                // 尝试获取第一个元素
                String content = extractContentRecursively(list.get(0));
                if (content != null && !content.isEmpty()) {
                    return content;
                }
                
                // 如果第一个元素没有内容，遍历整个列表
                for (Object item : list) {
                    content = extractContentRecursively(item);
                    if (content != null && !content.isEmpty()) {
                        return content;
                    }
                }
            }
        }
        
        return null;
    }

    @Override
    @Retryable(
        value = {RestClientException.class},
        maxAttemptsExpression = "#{@llmConfig.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@llmConfig.retry.backoffDelay}")
    )
    public List<ModelInfoDTO> getAvailableModels(String apiUrl, String apiKey) {
        logger.info("正在获取可用模型列表，API URL: {}", apiUrl);
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求实体
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 构建模型列表API URL（保持与OpenAI兼容的路径）
            String modelsUrl = apiUrl.endsWith("/") ? apiUrl + "v1/models" : apiUrl + "/v1/models";
            
            // 使用默认RestTemplate发送请求 - 这里不需要模型特定的超时设置
            ResponseEntity<Map> response = restTemplate.exchange(
                modelsUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            // 解析响应（假设响应格式与OpenAI兼容）
            List<ModelInfoDTO> models = new ArrayList<>();
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> modelData = (List<Map<String, Object>>) response.getBody().get("data");
                
                for (Map<String, Object> model : modelData) {
                    ModelInfoDTO modelInfo = new ModelInfoDTO(
                        (String) model.get("id"),
                        (String) model.get("id"), // 使用id作为名称
                        getProviderFromUrl(apiUrl)
                    );
                    
                    // 设置其他属性
                    if (model.containsKey("description")) {
                        modelInfo.setDescription((String) model.get("description"));
                    }
                    
                    // 设置最大token数（如果有）
                    if (model.containsKey("max_tokens")) {
                        modelInfo.setMaxTokens((Integer) model.get("max_tokens"));
                    }
                    
                    // 设置可用性
                    modelInfo.setAvailable(true);
                    
                    models.add(modelInfo);
                }
            }
            
            logger.info("成功获取到{}个可用模型", models.size());
            return models;
            
        } catch (Exception e) {
            logger.error("获取模型列表时发生错误: {}", e.getMessage(), e);
            throw new RestClientException("获取模型列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从API URL中获取提供商名称
     */
    private String getProviderFromUrl(String apiUrl) {
            try {
                String host = new java.net.URL(apiUrl).getHost();
                // 移除常见的域名后缀
                host = host.replaceAll("(?i)\\.com|\\.cn|\\.org|\\.net|\\.ai", "");
                // 获取最后一个部分作为提供商名称
                String[] parts = host.split("\\.");
                String provider = parts[parts.length - 1];
                // 首字母大写
            if (provider.length() > 0) {
                return provider.substring(0, 1).toUpperCase() + provider.substring(1);
            } else {
                return "OpenAI Compatible";
            }
        } catch (Exception e) {
            return "OpenAI Compatible";
        }
    }
} 