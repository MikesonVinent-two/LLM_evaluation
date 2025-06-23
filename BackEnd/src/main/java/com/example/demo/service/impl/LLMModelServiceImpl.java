package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.LLMModelDTO;
import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.LlmModelRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.LLMModelService;

@Service
public class LLMModelServiceImpl implements LLMModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMModelServiceImpl.class);
    
    @Autowired
    private LlmModelRepository llmModelRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public LLMModelRegistrationResponse registerModels(LLMModelRegistrationRequest request) {
        try {
            // 1. 验证用户是否存在
            if (!userRepository.existsById(request.getUserId())) {
                return LLMModelRegistrationResponse.error("用户不存在", null);
            }
            
            // 获取用户对象
            User user = userRepository.findById(request.getUserId()).orElse(null);

            // 2. 调用API获取可用模型列表
            List<Map<String, Object>> availableModels = fetchAvailableModels(request.getApiUrl(), request.getApiKey());
            if (availableModels == null || availableModels.isEmpty()) {
                return LLMModelRegistrationResponse.error("从API无法获取有效的模型列表", null);
            }
            
            // 3. 将获取的模型信息保存到数据库
            List<LLMModelDTO> registeredModels = new ArrayList<>();
            
            for (Map<String, Object> modelData : availableModels) {
                String modelId = (String) modelData.get("id");
                String modelName = modelData.containsKey("name") ? (String) modelData.get("name") : modelId;
                
                // 检查模型是否已存在
                if (llmModelRepository.existsByNameAndApiUrl(modelName, request.getApiUrl())) {
                    logger.info("模型已存在: {}", modelName);
                    continue;
                }
                
                LlmModel llmModel = new LlmModel();
                llmModel.setName(modelName);
                llmModel.setProvider(extractProviderFromModelId(modelId));
                llmModel.setApiUrl(request.getApiUrl());
                llmModel.setApiKey(request.getApiKey());
                llmModel.setApiType(request.getApiType()); // 设置API类型
                llmModel.setCreatedByUser(user);
                llmModel.setCreatedAt(LocalDateTime.now());
                
                LlmModel savedModel = llmModelRepository.save(llmModel);
                registeredModels.add(convertToDTO(savedModel));
            }
            
            if (registeredModels.isEmpty()) {
                return LLMModelRegistrationResponse.error("没有新模型被注册", null);
            }
            
            return LLMModelRegistrationResponse.success("成功注册 " + registeredModels.size() + " 个模型", registeredModels);
            
        } catch (Exception e) {
            logger.error("注册模型时发生错误", e);
            return LLMModelRegistrationResponse.error("注册模型时发生错误: " + e.getMessage(), null);
        }
    }

    private List<Map<String, Object>> fetchAvailableModels(String apiUrl, String apiKey) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/v1/models",  // 假设这是获取模型列表的端点
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }

            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("调用API获取模型列表时发生错误", e);
            throw e;
        }
    }

    private String extractProviderFromModelId(String modelId) {
        // 根据模型ID推断提供商
        if (modelId.startsWith("gpt-")) {
            return "OpenAI";
        } else if (modelId.startsWith("claude-")) {
            return "Anthropic";
        } else {
            return "Unknown";
        }
    }

    private LLMModelDTO convertToDTO(LlmModel model) {
        LLMModelDTO dto = new LLMModelDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setProvider(model.getProvider());
        dto.setVersion(model.getVersion());
        dto.setDescription(model.getDescription());
        dto.setApiType(model.getApiType()); // 添加API类型
        return dto;
    }

    @Override
    public List<LLMModelDTO> getAllModels() {
        logger.debug("获取所有已注册的LLM模型");
        
        try {
            // 获取所有未删除的模型
            List<LlmModel> models = llmModelRepository.findByDeletedAtIsNull();
            
            // 转换为DTO列表并返回
            List<LLMModelDTO> modelDTOs = models.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("成功获取所有已注册的LLM模型，共 {} 个", modelDTOs.size());
            return modelDTOs;
        } catch (Exception e) {
            logger.error("获取已注册的LLM模型时发生错误", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @Transactional
    public Map<String, Object> deleteModel(Long modelId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("尝试删除模型，ID: {}", modelId);
            
            // 1. 检查模型是否存在
            if (!llmModelRepository.findById(modelId).isPresent()) {
                logger.warn("模型不存在，ID: {}", modelId);
                response.put("success", false);
                response.put("message", "模型不存在");
                return response;
            }
            
            // 2. 执行软删除操作
            boolean deleted = llmModelRepository.softDelete(modelId);
            
            if (deleted) {
                logger.info("成功删除模型，ID: {}", modelId);
                response.put("success", true);
                response.put("message", "模型已成功删除");
            } else {
                logger.error("删除模型失败，ID: {}", modelId);
                response.put("success", false);
                response.put("message", "删除模型失败");
            }
            
            return response;
        } catch (Exception e) {
            logger.error("删除模型时发生错误", e);
            response.put("success", false);
            response.put("message", "删除模型时发生错误: " + e.getMessage());
            return response;
        }
    }
} 