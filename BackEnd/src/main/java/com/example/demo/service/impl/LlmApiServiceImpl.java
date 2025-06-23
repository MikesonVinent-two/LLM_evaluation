package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.config.RestTemplateConfig;
import com.example.demo.service.LlmApiService;
import com.example.demo.entity.jdbc.LlmModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;
import java.util.HashMap;

/**
 * LLM API服务实现类
 */
@Service
public class LlmApiServiceImpl implements LlmApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmApiServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplateConfig restTemplateConfig;
    private final RestTemplateBuilder restTemplateBuilder;
    
    @Value("${llm.default-model:gpt-3.5-turbo}")
    private String defaultModelName;
    
    public LlmApiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, 
                            RestTemplateConfig restTemplateConfig, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.restTemplateConfig = restTemplateConfig;
        this.restTemplateBuilder = restTemplateBuilder;
    }
    
    @Override
    public String generateAnswer(String apiUrl, String apiKey, String prompt, Map<String, Object> parameters) {
        return generateAnswer(apiUrl, apiKey, null, prompt, parameters);
    }
    
    @Override
    public String generateAnswer(String apiUrl, String apiKey, String apiType, String prompt, Map<String, Object> parameters) {
        logger.debug("调用LLM API生成回答, URL: {}, API类型: {}", apiUrl, apiType);
        
        try {
            // 根据API类型补全API路径
            if (apiUrl != null && !apiUrl.isEmpty()) {
                if (apiType != null) {
                    switch (apiType.toLowerCase()) {
                        case "openai":
                        case "openai_compatible":
                            // 检查并补全OpenAI API路径
                            if (!apiUrl.endsWith("/v1/chat/completions")) {
                                if (!apiUrl.endsWith("/v1")) {
                                    apiUrl = apiUrl.endsWith("/") 
                                        ? apiUrl + "v1/chat/completions" 
                                        : apiUrl + "/v1/chat/completions";
                                } else {
                                    apiUrl = apiUrl + "/chat/completions";
                                }
                            }
                            break;
                        case "anthropic":
                            // 检查并补全Anthropic API路径
                            if (!apiUrl.endsWith("/v1/complete")) {
                                apiUrl = apiUrl.endsWith("/") 
                                    ? apiUrl + "v1/complete" 
                                    : apiUrl + "/v1/complete";
                            }
                            break;
                        case "google":
                            // 检查并补全Google API路径
                            if (!apiUrl.contains("/v1/models") && !apiUrl.contains("/generateContent")) {
                                apiUrl = apiUrl.endsWith("/") 
                                    ? apiUrl + "v1/models/gemini-pro:generateContent" 
                                    : apiUrl + "/v1/models/gemini-pro:generateContent";
                            }
                            break;
                    }
                    logger.debug("完整API URL: {}", apiUrl);
                }
            }
            
            // 准备HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (apiKey != null && !apiKey.isEmpty()) {
                // 根据API类型设置不同的认证头
                if (apiType != null) {
                    switch (apiType.toLowerCase()) {
                        case "openai":
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        case "openai_compatible":
                            // OpenAI兼容类型，使用相同的认证方式
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        case "azure":
                            headers.set("api-key", apiKey);
                            break;
                        case "anthropic":
                            headers.set("x-api-key", apiKey);
                            break;
                        case "google":
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        default:
                            // 默认Bearer认证
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                    }
                } else {
                    // 兼容旧代码，根据URL推断
                    if (apiUrl.contains("openai.com")) {
                        headers.set("Authorization", "Bearer " + apiKey);
                    } else if (apiUrl.contains("azure.com")) {
                        headers.set("api-key", apiKey);
                    } else if (apiUrl.contains("anthropic.com")) {
                        headers.set("x-api-key", apiKey);
                    } else {
                        // 默认Bearer认证
                        headers.set("Authorization", "Bearer " + apiKey);
                    }
                }
            }
            
            // 构建请求体
            ObjectNode requestBody = createRequestBody(prompt, parameters, apiType);
            
            // 获取模型名称，用于配置特定的超时时间
            String modelName = null;
            if (parameters != null && parameters.containsKey("model")) {
                modelName = parameters.get("model").toString();
            }
            
            // 根据模型类型获取特定的RestTemplate，以支持不同的超时设置
            RestTemplate modelSpecificRestTemplate;
            if (modelName != null) {
                logger.info("使用模型特定的RestTemplate，模型: {}", modelName);
                // 创建适合该模型特性的RestTemplate，支持更长的超时时间
                modelSpecificRestTemplate = restTemplateConfig.getModelSpecificRestTemplate(restTemplateBuilder, modelName);
            } else {
                // 使用默认RestTemplate
                modelSpecificRestTemplate = restTemplate;
            }
            
            // 打印问题内容
            logger.info("向LLM发送问题: {}", prompt);
            
            // 创建HTTP实体
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // 使用模型特定的RestTemplate发送请求
            ResponseEntity<String> responseEntity = modelSpecificRestTemplate.postForEntity(apiUrl, requestEntity, String.class);
            
            // 解析响应
            return parseResponse(responseEntity.getBody(), apiType);
            
        } catch (Exception e) {
            logger.error("LLM API调用失败", e);
            throw new RuntimeException("LLM API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据API类型和参数创建请求体
     */
    private ObjectNode createRequestBody(String prompt, Map<String, Object> parameters, String apiType) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // 默认参数
            Map<String, Object> defaultParams = new HashMap<>();
            defaultParams.put("temperature", 0.7);
            defaultParams.put("max_tokens", 1000);
            
            // 用提供的参数覆盖默认参数
            if (parameters != null) {
                defaultParams.putAll(parameters);
            }
            
            // 根据API类型构建不同的请求体
            if (apiType != null) {
                switch (apiType.toLowerCase()) {
                    case "openai":
                    case "openai_compatible":
                        // OpenAI API格式或兼容格式
                        if (!defaultParams.containsKey("model")) {
                            // 抛出异常，不使用默认模型名称
                            throw new IllegalArgumentException("缺少必要的参数: model - 在调用OpenAI/兼容API时必须指定模型名称");
                        } else {
                            requestBody.put("model", defaultParams.get("model").toString());
                        }
                        
                        // 修复messages数组格式
                        var messagesArray = objectMapper.createArrayNode();
                        var messageObject = objectMapper.createObjectNode();
                        messageObject.put("role", "user");
                        messageObject.put("content", prompt);
                        messagesArray.add(messageObject);
                        requestBody.set("messages", messagesArray);
                        break;
                        
                    case "anthropic":
                        // Anthropic Claude API格式
                        if (!defaultParams.containsKey("model")) {
                            // 抛出异常，不使用默认模型名称
                            throw new IllegalArgumentException("缺少必要的参数: model - 在调用Anthropic API时必须指定模型名称");
                        } else {
                            requestBody.put("model", defaultParams.get("model").toString());
                        }
                        
                        requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant: ");
                        break;
                        
                    case "google":
                        // Google PaLM2/Gemini API格式
                        requestBody.put("prompt", prompt);
                        break;
                        
                    default:
                        // 默认简单格式
                        requestBody.put("prompt", prompt);
                        break;
                }
            } else {
                // 兼容旧代码逻辑
                requestBody.put("prompt", prompt);
            }
            
            // 添加其他参数
            for (Map.Entry<String, Object> entry : defaultParams.entrySet()) {
                if (!entry.getKey().equals("model") && !entry.getKey().equals("messages") && !entry.getKey().equals("prompt")) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 对于特定参数使用整数类型
                    switch (key) {
                        case "max_tokens":
                        case "n":
                        case "top_k":
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).intValue());
                            }
                            break;
                        case "presence_penalty":
                        case "frequency_penalty":
                        case "temperature":
                        case "top_p":
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).doubleValue());
                            }
                            break;
                        default:
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).doubleValue());
                            } else if (value != null) {
                                requestBody.put(key, value.toString());
                            }
                    }
                }
            }
            
            return requestBody;
        } catch (Exception e) {
            logger.error("创建LLM API请求体失败", e);
            throw new RuntimeException("创建LLM API请求体失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析API响应
     */
    private String parseResponse(String responseJson, String apiType) {
        try {
            if (responseJson == null || responseJson.isEmpty()) {
                return "";
            }
            
            JsonNode responseNode = objectMapper.readTree(responseJson);
            String result = "";
            
            // 根据API类型解析不同格式的响应
            if (apiType != null) {
                switch (apiType.toLowerCase()) {
                    case "openai":
                    case "openai_compatible":
                        // OpenAI响应格式或兼容格式
                        if (responseNode.has("choices") && responseNode.get("choices").size() > 0) {
                            JsonNode choice = responseNode.get("choices").get(0);
                            if (choice.has("message") && choice.get("message").has("content")) {
                                result = choice.get("message").get("content").asText();
                            }
                        }
                        break;
                        
                    case "anthropic":
                        // Anthropic Claude响应格式
                        if (responseNode.has("completion")) {
                            result = responseNode.get("completion").asText();
                        }
                        break;
                        
                    case "google":
                        // Google PaLM2/Gemini响应格式
                        if (responseNode.has("candidates") && responseNode.get("candidates").size() > 0) {
                            result = responseNode.get("candidates").get(0).get("content").asText();
                        }
                        break;
                        
                    default:
                        // 尝试通用解析
                        break;
                }
            }
            
            // 如果特定解析没有结果，尝试通用响应解析逻辑
            if (result.isEmpty()) {
                // 通用响应解析逻辑，尝试从各种常见格式中提取
                if (responseNode.has("choices") && responseNode.get("choices").size() > 0) {
                    // OpenAI或类似格式
                    JsonNode choice = responseNode.get("choices").get(0);
                    if (choice.has("message") && choice.get("message").has("content")) {
                        result = choice.get("message").get("content").asText();
                    } else if (choice.has("text")) {
                        result = choice.get("text").asText();
                    }
                } else if (responseNode.has("completion")) {
                    // Anthropic或类似格式
                    result = responseNode.get("completion").asText();
                } else if (responseNode.has("generated_text")) {
                    // 某些模型的格式
                    result = responseNode.get("generated_text").asText();
                } else if (responseNode.has("result") && responseNode.get("result").isTextual()) {
                    // 通用格式
                    result = responseNode.get("result").asText();
                }
            }
            
            // 如果无法提取，记录并返回原始响应
            if (result.isEmpty()) {
                logger.warn("无法从响应中提取文本内容，返回空字符串。原始响应: {}", responseJson);
                return "";
            }
            
            // 使用TextPreprocessor处理文本，移除思考过程标记
            String cleanedResult = com.example.demo.utils.TextPreprocessor.cleanText(result);
            
            // 如果处理前后文本长度不同，记录日志
            if (cleanedResult.length() != result.length()) {
                logger.info("已清理模型回答中的思考过程标记和特殊字符，原长度: {}，处理后长度: {}", 
                          result.length(), cleanedResult.length());
            }
            
            return cleanedResult;
            
        } catch (Exception e) {
            logger.error("解析API响应失败", e);
            return "";
        }
    }
    
    /**
     * 测试模型连通性（改进版本）
     * @param apiUrl API地址
     * @param apiKey API密钥
     * @param apiType API类型
     * @return 是否连接成功
     */
    @Override
    public boolean testModelConnectivity(String apiUrl, String apiKey, String apiType) {
        // 调用新方法，但不指定模型名称
        return testModelConnectivity(apiUrl, apiKey, apiType, null);
    }
    
    @Override
    public boolean testModelConnectivity(String apiUrl, String apiKey, String apiType, String modelName) {
        logger.info("测试模型连通性: URL={}, 类型={}, 模型名称={}", apiUrl, apiType, modelName);
        
        // 确保API URL不为空
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            logger.error("API URL为空，无法测试连通性");
            return false;
        }
        
        // 规范化apiType，确保后续处理一致性
        String normalizedApiType = apiType;
        if (normalizedApiType == null) {
            normalizedApiType = "GENERIC";
        } else {
            normalizedApiType = normalizedApiType.toUpperCase();
        }
        
        try {
            // 尝试简单的连接测试 - 首先验证服务器是否可达
            boolean simpleConnectivityTest = testSimpleConnectivity(apiUrl);
            if (!simpleConnectivityTest) {
                logger.error("无法连接到服务器: {}", apiUrl);
                return false;
            }
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 根据API类型设置不同的认证头
            if (apiKey != null && !apiKey.isEmpty()) {
                switch (normalizedApiType) {
                    case "AZURE_OPENAI":
                        logger.debug("使用Azure OpenAI认证方式");
                        headers.set("api-key", apiKey);
                        break;
                    case "ANTHROPIC":
                        logger.debug("使用Anthropic认证方式");
                        headers.set("x-api-key", apiKey);
                        break;
                    case "ZHIPU":
                    case "GLM":
                        logger.debug("使用智谱/GLM认证方式");
                        headers.set("Authorization", apiKey); // 智谱API可能直接使用token
                        break;
                    default:
                        logger.debug("使用标准Bearer认证方式");
                        headers.set("Authorization", "Bearer " + apiKey);
                        break;
                }
                
                if (!normalizedApiType.equals("ZHIPU") && !normalizedApiType.equals("GLM")) {
                    logger.debug("添加认证头: {} [部分隐藏]", 
                        apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
                }
            }
            
            // 准备最小化请求体，适应不同API类型
            ObjectNode requestBody = objectMapper.createObjectNode();
            String endpointUrl = apiUrl;
            
            // 根据不同API类型准备测试请求
            switch (normalizedApiType) {
                case "OPENAI":
                case "OPENAI_COMPATIBLE":
                    // OpenAI格式请求体
                    logger.debug("准备OpenAI格式测试请求");
                    
                    // 使用传入的模型名称，如果为空则返回错误
                    if (modelName == null || modelName.isEmpty()) {
                        logger.error("测试连接失败: 在测试OpenAI/兼容API连接时必须指定模型名称");
                        return false;
                    }
                    requestBody.put("model", modelName);
                    
                    // 构建消息数组
                    ArrayNode messagesArray = requestBody.putArray("messages");
                    ObjectNode message = objectMapper.createObjectNode();
                    message.put("role", "user");
                    message.put("content", "Hello");
                    messagesArray.add(message);
                    
                    // 添加其他必要参数
                    requestBody.put("max_tokens", 5);
                    requestBody.put("temperature", 0.1);
                    
                    // 检查并修复API端点
                    if (!apiUrl.contains("/v1/chat/completions")) {
                        endpointUrl = apiUrl.endsWith("/") ? 
                            apiUrl + "v1/chat/completions" : 
                            apiUrl + "/v1/chat/completions";
                    }
                    break;
                    
                case "AZURE_OPENAI":
                    // Azure OpenAI格式请求体
                    logger.debug("准备Azure OpenAI格式测试请求");
                    messagesArray = requestBody.putArray("messages");
                    message = objectMapper.createObjectNode();
                    message.put("role", "user");
                    message.put("content", "Hello");
                    messagesArray.add(message);
                    requestBody.put("max_tokens", 5);
                    
                    // Azure OpenAI通常需要特定的部署名称在URL中
                    if (!apiUrl.contains("/deployments/")) {
                        logger.warn("Azure OpenAI URL可能不完整，可能需要包含部署名称");
                    }
                    break;
                    
                case "ANTHROPIC":
                    // Anthropic Claude格式请求体
                    logger.debug("准备Anthropic格式测试请求");
                    requestBody.put("prompt", "Human: Hello\nAssistant:");
                    
                    // 使用传入的模型名称，如果为空则返回错误
                    if (modelName == null || modelName.isEmpty()) {
                        logger.error("测试连接失败: 在测试Anthropic API连接时必须指定模型名称");
                        return false;
                    }
                    requestBody.put("model", modelName);
                    
                    requestBody.put("max_tokens_to_sample", 5);
                    
                    // 检查并修复API端点
                    if (!apiUrl.contains("/complete")) {
                        if (apiUrl.contains("/v1")) {
                            endpointUrl = apiUrl.endsWith("/v1") ? 
                                apiUrl + "/complete" : 
                                apiUrl + (apiUrl.endsWith("/") ? "v1/complete" : "/v1/complete");
                        } else {
                            endpointUrl = apiUrl.endsWith("/") ? 
                                apiUrl + "v1/complete" : 
                                apiUrl + "/v1/complete";
                        }
                    }
                    break;
                    
                case "GOOGLE":
                    // Google PaLM格式请求体
                    logger.debug("准备Google PaLM格式测试请求");
                    
                    // 使用传入的模型名称，如果为空则返回错误
                    if (modelName == null || modelName.isEmpty()) {
                        logger.error("测试连接失败: 在测试Google API连接时必须指定模型名称");
                        return false;
                    }
                    String googleModel = modelName;
                    
                    ObjectNode instance = objectMapper.createObjectNode();
                    instance.put("prompt", "Hello");
                    
                    ArrayNode instances = objectMapper.createArrayNode();
                    instances.add(instance);
                    
                    ObjectNode parameters = objectMapper.createObjectNode();
                    parameters.put("maxOutputTokens", 5);
                    parameters.put("temperature", 0.1);
                    
                    requestBody.set("instances", instances);
                    requestBody.set("parameters", parameters);
                    
                    // 检查并修复API端点 - Google通常需要模型名称在URL中
                    if (!apiUrl.contains(googleModel)) {
                        endpointUrl = apiUrl.endsWith("/") ? 
                            apiUrl + googleModel + ":predict" : 
                            apiUrl + "/" + googleModel + ":predict";
                    }
                    break;
                    
                case "ZHIPU":
                case "GLM":
                    // 智谱格式请求体
                    logger.debug("准备智谱/GLM格式测试请求");
                    
                    // 使用传入的模型名称，如果为空则返回错误
                    if (modelName == null || modelName.isEmpty()) {
                        logger.error("测试连接失败: 在测试智谱/GLM API连接时必须指定模型名称");
                        return false;
                    }
                    requestBody.put("model", modelName);
                    
                    // ChatGLM支持多种接口，尝试使用通用的聊天接口
                    requestBody.put("prompt", "Hello");
                    requestBody.put("temperature", 0.7);
                    requestBody.put("top_p", 0.7);
                    requestBody.put("max_tokens", 5);
                    
                    // 智谱API接口检查
                    if (!apiUrl.contains("/chat") && !apiUrl.contains("/generate")) {
                        endpointUrl = apiUrl.endsWith("/") ? 
                            apiUrl + "chat" : 
                            apiUrl + "/chat";
                    }
                    break;
                    
                default:
                    // 通用格式，发送最少的请求体
                    logger.debug("准备通用格式测试请求: {}", normalizedApiType);
                    
                    // 如果提供了模型名称，则使用它，否则返回错误
                    if (modelName == null || modelName.isEmpty()) {
                        logger.error("测试连接失败: 必须指定模型名称");
                        return false;
                    }
                    requestBody.put("model", modelName);
                    
                    requestBody.put("prompt", "Hello");
                    requestBody.put("max_tokens", 5);
                    break;
            }
            
            // 创建HTTP实体
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // 记录详细的请求信息，帮助调试
            logger.info("发送测试POST请求到 {}", endpointUrl);
            logger.debug("请求头: {}", headers);
            logger.debug("请求体: {}", requestBody.toString());
            
            // 获取模型特定的RestTemplate，支持更长的超时设置
            RestTemplate modelSpecificRestTemplate = restTemplateConfig.getModelSpecificRestTemplate(
                    restTemplateBuilder, modelName);
                
            try {
                // 发送POST请求，使用模型特定的RestTemplate
                ResponseEntity<String> response = modelSpecificRestTemplate.postForEntity(
                        endpointUrl, requestEntity, String.class);
                
                int statusCode = response.getStatusCodeValue();
                String responseBody = response.getBody();
                
                // 放宽成功条件: 2xx成功，401/403表示认证问题但API可达
                boolean apiReachable = response.getStatusCode().is2xxSuccessful() || 
                                      statusCode == 401 || statusCode == 403;
                boolean apiUsable = response.getStatusCode().is2xxSuccessful();
                
                logger.info("模型API连通性测试结果 - 端点可达: {}, API可用: {}, 状态码: {}", 
                        apiReachable, apiUsable, statusCode);
                
                if (apiReachable) {
                    if (!apiUsable) {
                        logger.warn("API端点可达但认证失败，状态码: {}，这通常表示API密钥有问题", statusCode);
                    }
                    // 考虑端点可达就算成功，认证问题由用户检查API密钥解决
                    return true;
                } else {
                    logger.error("API端点请求失败，状态码: {}", statusCode);
                    return false;
                }
            } catch (Exception e) {
                logger.error("发送API请求失败: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("模型连通性测试过程中出现异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 简单连接测试 - 检查服务器是否可达
     * @param apiUrl API地址
     * @return 是否连接成功
     */
    private boolean testSimpleConnectivity(String apiUrl) {
        try {
            // 提取主机部分
            String baseUrl = apiUrl;
            if (baseUrl.contains("://")) {
                baseUrl = baseUrl.substring(baseUrl.indexOf("://") + 3);
            }
            if (baseUrl.contains("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.indexOf("/"));
            }
            
            // 如果URL包含端口，则提取域名部分
            String host = baseUrl;
            int port = 443; // 默认HTTPS端口
            if (baseUrl.contains(":")) {
                host = baseUrl.substring(0, baseUrl.indexOf(":"));
                try {
                    port = Integer.parseInt(baseUrl.substring(baseUrl.indexOf(":") + 1));
                } catch (NumberFormatException e) {
                    // 忽略解析错误，使用默认端口
                }
            }
            
            logger.debug("尝试连接到主机: {}:{}", host, port);
            
            // 尝试简单的Socket连接
            try (java.net.Socket socket = new java.net.Socket()) {
                // 设置3秒连接超时
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                logger.debug("成功连接到主机 {}:{}", host, port);
                return true;
            }
        } catch (Exception e) {
            logger.error("连接服务器失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 使用LLM模型生成回答
     */
    @Override
    public String generateModelAnswer(LlmModel model, String prompt, Map<String, Object> contextVariables) {
        logger.debug("调用LLM模型生成回答, 模型: {}, API类型: {}", model.getName(), model.getApiType());
        
        // 组装参数
        Map<String, Object> parameters = new HashMap<>();
        
        // 添加模型默认参数
        if (model.getModelParameters() != null) {
            parameters.putAll(model.getModelParameters());
        }
        
        // 添加上下文变量（优先级更高）
        if (contextVariables != null) {
            parameters.putAll(contextVariables);
        }
        
        // 确保参数中包含模型名称
        if (!parameters.containsKey("model") && model.getName() != null) {
            logger.debug("添加模型名称到请求参数: {}", model.getName());
            parameters.put("model", model.getName());
        }
        
        // 调用生成回答
        return generateAnswer(
            model.getApiUrl(),
            model.getApiKey(),
            model.getApiType(),
            prompt,
            parameters
        );
    }

    /**
     * 探测正确的API端点路径
     * @param baseUrl 基础API URL
     * @param apiKey API密钥
     * @param modelName 模型名称
     * @return 找到的有效端点路径，如果没找到返回原始URL
     */
    private String probeApiEndpoint(String baseUrl, String apiKey, String modelName) {
        logger.info("开始探测API端点: {}, 使用模型: {}", baseUrl, modelName);
        
        // 确保有模型名称
        String testModelName = modelName;
        if (testModelName == null || testModelName.isEmpty()) {
            testModelName = "gpt-3.5-turbo"; // 仅用于探测API端点
            logger.warn("未提供模型名称，使用通用测试模型名称: {}", testModelName);
        }
        
        // 常见的API路径组合
        String[] commonPaths = {
            "/v1/chat/completions",
            "/chat/completions", 
            "/v1/completions",
            "/completions",
            "/v1/generate",
            "/generate",
            "/api/chat",
            "/api/generate"
        };
        
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        
        // 尝试所有常见路径
        for (String path : commonPaths) {
            String testUrl = baseUrl;
            
            // 确保URL拼接正确
            if (baseUrl.endsWith("/")) {
                testUrl = baseUrl + path.substring(1);
            } else {
                testUrl = baseUrl + path;
            }
            
            try {
                logger.debug("探测API端点: {}", testUrl);
                
                // 创建最小请求体，使用模型名称
                ObjectNode requestBody = objectMapper.createObjectNode();
                ArrayNode messagesNode = requestBody.putArray("messages");
                ObjectNode messageObject = objectMapper.createObjectNode();
                messageObject.put("role", "user");
                messageObject.put("content", "test");
                messagesNode.add(messageObject);
                requestBody.put("max_tokens", 1);
                requestBody.put("model", testModelName);
                
                // 创建HTTP实体
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
                
                // 发送请求
                ResponseEntity<String> response = restTemplate.postForEntity(testUrl, requestEntity, String.class);
                
                // 检查响应 - 对于端点探测，我们认为400也是有效的（表示API存在但缺少必要参数）
                if (response.getStatusCode().is2xxSuccessful() || response.getStatusCodeValue() == 400 || response.getStatusCodeValue() == 401) {
                    logger.info("找到可能的API端点: {}, 状态码: {}", testUrl, response.getStatusCodeValue());
                    return testUrl;
                }
            } catch (Exception e) {
                // 忽略错误，继续尝试下一个路径
                logger.debug("探测路径 {} 失败: {}", testUrl, e.getMessage());
            }
        }
        
        // 如果没有找到有效端点，返回原始URL
        logger.warn("未找到有效的API端点，使用原始URL: {}", baseUrl);
        return baseUrl;
    }
} 