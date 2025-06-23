package com.example.demo.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionDisplayDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.serializer.RawQuestionSerializer;
import com.example.demo.service.RawDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/raw-data")
@CrossOrigin(origins = "*")
public class RawDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataService rawDataService;
    
    @Autowired
    public RawDataController(RawDataService rawDataService) {
        this.rawDataService = rawDataService;
    }
    
    @PostMapping("/questions")
    public ResponseEntity<RawQuestion> createQuestion(@Valid @RequestBody RawQuestion question) {
        RawQuestion savedQuestion = rawDataService.createQuestion(question);
        return ResponseEntity.ok(savedQuestion);
    }
    
    @PostMapping("/questions-dto")
    public ResponseEntity<RawQuestion> createQuestionFromDTO(@Valid @RequestBody RawQuestionDTO questionDTO) {
        RawQuestion savedQuestion = rawDataService.createQuestionFromDTO(questionDTO);
        
        // 使用ObjectMapper配置，防止循环引用
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(RawQuestion.class, new RawQuestionSerializer());
        objectMapper.registerModule(module);
        
        try {
            // 先转为JSON字符串，再转回对象，避免循环引用
            String json = objectMapper.writeValueAsString(savedQuestion);
            RawQuestion result = objectMapper.readValue(json, RawQuestion.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("序列化问题对象失败", e);
            return ResponseEntity.ok(savedQuestion); // 如果失败，返回原始对象
        }
    }
    
    /**
     * 创建原始回答
     * 
     * 支持两种格式的otherMetadata:
     * 1. JSON字符串: {"otherMetadata": "{\"source\":\"网站A\",\"category\":\"医疗\"}"}
     * 2. JSON对象: {"otherMetadata": {"source":"网站A","category":"医疗","tags":["内科","外科"]}}
     *
     * @param answerDTO 回答DTO
     * @return 保存的回答
     */
    @PostMapping("/answers")
    public ResponseEntity<RawAnswer> createAnswer(@Valid @RequestBody RawAnswerDTO answerDTO) {
        RawAnswer savedAnswer = rawDataService.createAnswer(answerDTO);
        return ResponseEntity.ok(savedAnswer);
    }
    
    /**
     * 创建带有回答的原始问题
     * 
     * 支持两种格式的otherMetadata:
     * 1. JSON字符串: {"otherMetadata": "{\"source\":\"网站A\",\"category\":\"医疗\"}"}
     * 2. JSON对象: {"otherMetadata": {"source":"网站A","category":"医疗","tags":["内科","外科"]}}
     *
     * @param dto 问题和回答DTO
     * @return 保存的问题
     */
    @PostMapping("/questions-with-answers")
    public ResponseEntity<RawQuestion> createQuestionWithAnswers(@Valid @RequestBody RawQuestionWithAnswersDTO dto) {
        RawQuestion savedQuestion = rawDataService.createQuestionWithAnswers(dto);
        return ResponseEntity.ok(savedQuestion);
    }
    
    // 获取所有原始问题（分页）
    @GetMapping("/questions")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getAllRawQuestions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findAllRawQuestions(pageable));
    }
    
    // 根据标准化状态查询（分页）
    @GetMapping("/questions/by-status")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getRawQuestionsByStatus(
            @RequestParam(required = false, defaultValue = "false") boolean standardized,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findRawQuestionsByStandardizedStatus(standardized, pageable));
    }
    
    // 根据来源网站查询（分页）
    @GetMapping("/questions/by-source")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getRawQuestionsBySource(
            @RequestParam String sourceSite,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findRawQuestionsBySourceSite(sourceSite, pageable));
    }
    
    // 搜索原始问题（分页）
    @GetMapping("/questions/search")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> searchRawQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "keyword[]", required = false) String keywordArray,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "tags[]", required = false) List<String> tagsArray,
            @RequestParam(required = false) Boolean unStandardized,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        
        // 合并两种格式的关键词参数
        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = keyword;
        } else if (keywordArray != null && !keywordArray.trim().isEmpty()) {
            finalKeyword = keywordArray;
        }
        
        // 合并两种格式的标签参数
        List<String> finalTags = null;
        if (tags != null && !tags.isEmpty()) {
            finalTags = tags;
        } else if (tagsArray != null && !tagsArray.isEmpty()) {
            finalTags = tagsArray;
        }
        
        logger.info("接收到搜索请求 - keyword: {}, keywordArray: {}, finalKeyword: {}, tags: {}, tagsArray: {}, finalTags: {}, unStandardized: {}", 
                   keyword, keywordArray, finalKeyword, tags, tagsArray, finalTags, unStandardized);
        System.out.println("DEBUG: 接收到搜索请求 - finalKeyword: " + finalKeyword + ", finalTags: " + finalTags);
        
        try {
            // 清理和验证参数
            if (finalKeyword != null) {
                finalKeyword = finalKeyword.trim();
                if (finalKeyword.isEmpty()) {
                    finalKeyword = null;
                }
            }
            
            if (finalTags != null) {
                // 清理标签列表，移除空白标签
                finalTags = finalTags.stream()
                        .filter(tag -> tag != null && !tag.trim().isEmpty())
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.toList());
                if (finalTags.isEmpty()) {
                    finalTags = null;
                }
            }
            
            logger.debug("清理后的参数 - finalKeyword: {}, finalTags: {}, unStandardized: {}", finalKeyword, finalTags, unStandardized);
            
            // 如果只有关键词，使用简单搜索
            if (finalKeyword != null && !finalKeyword.isEmpty() && (finalTags == null || finalTags.isEmpty()) && unStandardized == null) {
                logger.debug("使用简单搜索");
                return ResponseEntity.ok(rawDataService.searchRawQuestions(finalKeyword, pageable));
            }
            
            // 否则使用高级搜索
            logger.debug("使用高级搜索");
            return ResponseEntity.ok(rawDataService.advancedSearchRawQuestions(finalKeyword, finalTags, unStandardized, pageable));
            
        } catch (Exception e) {
            logger.error("搜索原始问题时发生错误", e);
            throw e; // 让全局异常处理器处理
        }
    }
    
    // 添加测试数据
    @PostMapping("/questions/test-data")
    public ResponseEntity<String> addTestData() {
        try {
            // 创建测试问题1
            RawQuestionDTO question1 = new RawQuestionDTO();
            question1.setSourceUrl("http://example.com/q1");
            question1.setSourceSite("测试网站");
            question1.setTitle("测试问题1");
            question1.setContent("这是一个测试问题的内容");
            question1.setTags(Arrays.asList("测试", "示例"));
            rawDataService.createQuestionFromDTO(question1);
            
            // 创建测试问题2
            RawQuestionDTO question2 = new RawQuestionDTO();
            question2.setSourceUrl("http://example.com/q2");
            question2.setSourceSite("测试网站");
            question2.setTitle("测试问题2");
            question2.setContent("这是另一个测试问题的内容");
            question2.setTags(Arrays.asList("测试", "示例2"));
            rawDataService.createQuestionFromDTO(question2);
            
            return ResponseEntity.ok("测试数据添加成功");
        } catch (Exception e) {
            logger.error("添加测试数据失败", e);
            return ResponseEntity.badRequest().body("添加测试数据失败: " + e.getMessage());
        }
    }
    
    // 根据标签查询问题
    @GetMapping("/questions/by-tags")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getQuestionsByTags(
            @RequestParam List<String> tags,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(rawDataService.findQuestionsByTags(tags, pageable));
    }
    
    // 测试参数绑定
    @GetMapping("/test-params")
    public ResponseEntity<Map<String, Object>> testParams(
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "tags[]", required = false) List<String> tagsArray) {
        Map<String, Object> result = new HashMap<>();
        result.put("tags", tags);
        result.put("tagsArray", tagsArray);
        System.out.println("DEBUG: tags = " + tags);
        System.out.println("DEBUG: tagsArray = " + tagsArray);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除原始问题
     * @param questionId 问题ID
     * @return 操作结果
     */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteRawQuestion(@PathVariable Long questionId) {
        logger.info("接收到删除原始问题请求 - ID: {}", questionId);
        
        try {
            boolean result = rawDataService.deleteRawQuestion(questionId);
            
            if (result) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "原始问题删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "删除操作未完成");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (IllegalArgumentException e) {
            // 问题不存在等参数错误
            logger.error("删除原始问题失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            // 问题已被标准化等状态错误
            logger.error("删除原始问题失败 - 状态错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "OPERATION_NOT_ALLOWED");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            // 其他服务器错误
            logger.error("删除原始问题失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除原始回答
     * @param answerId 回答ID
     * @return 操作结果
     */
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<?> deleteRawAnswer(@PathVariable Long answerId) {
        logger.info("接收到删除原始回答请求 - ID: {}", answerId);
        
        try {
            boolean result = rawDataService.deleteRawAnswer(answerId);
            
            if (result) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "原始回答删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "删除操作未完成");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (IllegalArgumentException e) {
            // 回答不存在等参数错误
            logger.error("删除原始回答失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            // 其他服务器错误
            logger.error("删除原始回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有原始问题（DTO格式，避免循环引用）
     */
    @GetMapping("/questions-dto")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getAllRawQuestionsDTO(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取原始问题DTO请求");
        return ResponseEntity.ok(rawDataService.findAllRawQuestions(pageable));
    }
    
    /**
     * 根据原始问题ID获取所有回答（分页）
     * 
     * @param questionId 原始问题ID
     * @param pageable 分页参数
     * @return 回答列表
     */
    @GetMapping("/questions/{questionId}/answers")
    public ResponseEntity<?> getAnswersByQuestionId(
            @PathVariable Long questionId,
            @PageableDefault(size = 10, sort = "publishTime", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("接收到获取原始问题回答请求 - 问题ID: {}", questionId);
        
        try {
            Page<RawAnswer> answers = rawDataService.findRawAnswersByQuestionId(questionId, pageable);
            return ResponseEntity.ok(answers);
        } catch (IllegalArgumentException e) {
            logger.error("获取原始问题回答失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("获取原始问题回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "服务器处理请求时发生错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 