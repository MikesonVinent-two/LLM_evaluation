package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.TagOperationDTO;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.repository.jdbc.DatasetQuestionMappingRepository;
import com.example.demo.service.StandardQuestionService;

import jakarta.validation.Valid;

/**
 * 标准问题管理控制器
 */
@RestController
@RequestMapping("/standard-questions")
@CrossOrigin(origins = "*")
public class StandardQuestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionController.class);
    
    private final StandardQuestionService standardQuestionService;
    
    @Autowired
    private DatasetQuestionMappingRepository datasetQuestionMappingRepository;
    
    // 显式构造函数
    public StandardQuestionController(StandardQuestionService standardQuestionService) {
        this.standardQuestionService = standardQuestionService;
    }
    
    /**
     * 获取所有标准问题，支持分页
     * @param pageable 分页参数
     * @return 标准问题分页列表
     */
    @GetMapping
    public ResponseEntity<Page<StandardQuestionDTO>> getAllStandardQuestions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取所有标准问题请求 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestionDTO> questions = standardQuestionService.findAllStandardQuestions(pageable);
            logger.info("成功获取标准问题 - 总数: {}", questions.getTotalElements());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("获取标准问题失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有最新版本的标准问题，支持分页
     * 只返回版本树中的叶子节点，即没有子版本的问题
     * 
     * @param pageable 分页参数
     * @return 最新版本的标准问题分页列表
     */
    @GetMapping("/latest")
    public ResponseEntity<Page<StandardQuestionDTO>> getLatestStandardQuestions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取最新版本标准问题请求 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestionDTO> questions = standardQuestionService.findLatestStandardQuestions(pageable);
            logger.info("成功获取最新版本标准问题 - 总数: {}", questions.getTotalElements());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("获取最新版本标准问题失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<StandardQuestionDTO> createStandardQuestion(
            @RequestBody @Valid StandardQuestionDTO questionDTO) {
        StandardQuestionDTO createdQuestion = standardQuestionService.createStandardQuestion(questionDTO, questionDTO.getUserId());
        return ResponseEntity.ok(createdQuestion);
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<StandardQuestionDTO> updateStandardQuestion(
            @PathVariable Long questionId,
            @RequestBody @Valid StandardQuestionDTO questionDTO) {
        StandardQuestionDTO updatedQuestion = standardQuestionService.updateStandardQuestion(questionId, questionDTO, questionDTO.getUserId());
        return ResponseEntity.ok(updatedQuestion);
    }

    /**
     * 获取标准问题的修改历史
     * 
     * @param questionId 问题ID
     * @return 问题的所有历史版本信息
     */
    @GetMapping("/{questionId}/history")
    public ResponseEntity<List<QuestionHistoryDTO>> getQuestionHistory(
            @PathVariable Long questionId) {
        logger.info("接收到获取问题修改历史请求 - 问题ID: {}", questionId);
        try {
            List<QuestionHistoryDTO> history = standardQuestionService.getQuestionHistory(questionId);
            if (history.isEmpty()) {
                logger.warn("未找到问题的修改历史 - 问题ID: {}", questionId);
                return ResponseEntity.notFound().build();
            }
            logger.info("成功获取问题修改历史 - 问题ID: {}, 版本数量: {}", questionId, history.size());
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            logger.error("获取问题修改历史失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (Exception e) {
            logger.error("获取问题修改历史失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 获取问题的版本树结构
     * 
     * @param questionId 问题ID
     * @return 问题的版本树结构
     */
    @GetMapping("/{questionId}/version-tree")
    public ResponseEntity<List<QuestionHistoryDTO>> getVersionTree(
            @PathVariable Long questionId) {
        logger.info("接收到获取问题版本树请求 - 问题ID: {}", questionId);
        try {
            List<QuestionHistoryDTO> versionTree = standardQuestionService.getVersionTree(questionId);
            if (versionTree.isEmpty()) {
                logger.warn("未找到问题的版本树 - 问题ID: {}", questionId);
                return ResponseEntity.notFound().build();
            }
            
            // 添加详细日志，记录版本树的节点信息
            logger.info("成功获取问题版本树 - 问题ID: {}, 版本数量: {}", questionId, versionTree.size());
            for (QuestionHistoryDTO version : versionTree) {
                logger.info("版本节点 - ID: {}, 父ID: {}, 创建时间: {}, 变更日志ID: {}", 
                    version.getId(), 
                    version.getParentQuestionId(), 
                    version.getCreationTime(),
                    version.getChangeLogId());
            }
            
            // 记录版本树的具体父子关系
            logger.info("版本树父子关系:");
            Map<Long, List<Long>> childrenMap = new HashMap<>();
            for (QuestionHistoryDTO version : versionTree) {
                Long parentId = version.getParentQuestionId();
                if (parentId != null) {
                    if (!childrenMap.containsKey(parentId)) {
                        childrenMap.put(parentId, new ArrayList<>());
                    }
                    childrenMap.get(parentId).add(version.getId());
                }
            }
            
            for (Map.Entry<Long, List<Long>> entry : childrenMap.entrySet()) {
                logger.info("父节点ID: {}, 子节点IDs: {}", entry.getKey(), entry.getValue());
            }
            
            return ResponseEntity.ok(versionTree);
        } catch (IllegalArgumentException e) {
            logger.error("获取问题版本树失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (Exception e) {
            logger.error("获取问题版本树失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 更新标准问题的标签
     * 
     * @param operationDTO 标签操作请求
     * @return 更新后的标准问题
     */
    @PostMapping("/tags")
    public ResponseEntity<?> updateQuestionTags(
            @RequestBody @Valid TagOperationDTO operationDTO) {
        logger.info("接收到更新标准问题标签请求 - 问题ID: {}, 操作类型: {}", 
            operationDTO.getQuestionId(), operationDTO.getOperationType());
        
        try {
            StandardQuestionDTO updatedQuestion = standardQuestionService.updateQuestionTags(operationDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "标签更新成功");
            response.put("question", updatedQuestion);
            
            logger.info("成功更新标准问题标签 - 问题ID: {}", operationDTO.getQuestionId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("更新标准问题标签失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("更新标准问题标签失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "更新标签时发生错误");
            response.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 批量更新多个标准问题的标签
     * 
     * @param batchOperationsDTO 批量标签操作请求
     * @return 批量操作结果
     */
    @PostMapping("/batch-tags")
    public ResponseEntity<?> batchUpdateQuestionTags(
            @RequestBody @Valid BatchTagOperationsDTO batchOperationsDTO) {
        logger.info("接收到批量更新标准问题标签请求 - 操作数量: {}", 
            batchOperationsDTO.getOperations() != null ? batchOperationsDTO.getOperations().size() : 0);
        
        try {
            Map<Long, Boolean> results = standardQuestionService.batchUpdateQuestionTags(batchOperationsDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "批量标签更新完成");
            response.put("results", results);
            response.put("total", results.size());
            response.put("success", results.values().stream().filter(v -> v).count());
            response.put("failed", results.values().stream().filter(v -> !v).count());
            
            logger.info("成功批量更新标准问题标签 - 总数: {}, 成功: {}", 
                results.size(), results.values().stream().filter(v -> v).count());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("批量更新标准问题标签失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("批量更新标准问题标签失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "批量更新标签时发生错误");
            response.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 搜索标准问题，支持按标签和关键词搜索
     * 
     * @param tags 标签列表，多个标签用逗号分隔
     * @param keyword 关键词
     * @param userId 当前用户ID，用于判断用户是否已回答
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyWithStandardAnswers 是否只返回有标准答案的问题
     * @param pageable 分页参数
     * @return 匹配的标准问题列表，包含额外信息
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchStandardQuestions(
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatest,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyWithStandardAnswers,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到搜索标准问题请求 - 标签: {}, 关键词: {}, 用户ID: {}, 仅最新版本: {}, 仅有标准答案: {}", 
            tags, keyword, userId, onlyLatest, onlyWithStandardAnswers);
        
        try {
            // 将逗号分隔的标签转换为列表
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = Arrays.asList(tags.split(","));
            }
            
            // 调用服务层方法执行搜索
            Map<String, Object> result = standardQuestionService.searchQuestions(
                    tagList, keyword, userId, onlyLatest, onlyWithStandardAnswers, pageable);
            
            logger.info("成功搜索标准问题 - 总数: {}", result.get("total"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("搜索标准问题失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索标准问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根据标准问题ID查看原始问题和原始回答列表（支持分页）
     * 
     * @param questionId 标准问题ID
     * @param pageable 分页参数
     * @return 原始问题和回答列表信息
     */
    @GetMapping("/{questionId}/original-data")
    public ResponseEntity<?> getOriginalQuestionAndAnswers(
            @PathVariable Long questionId,
            @PageableDefault(size = 10) Pageable pageable) {
        logger.info("接收到获取原始问题和回答请求 - 标准问题ID: {}, 页码: {}, 每页大小: {}", 
            questionId, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Map<String, Object> result = standardQuestionService.getOriginalQuestionAndAnswers(questionId, pageable);
            
            if (result.isEmpty()) {
                logger.warn("未找到原始问题数据 - 标准问题ID: {}", questionId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("成功获取原始问题和回答 - 标准问题ID: {}", questionId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("获取原始问题和回答失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("获取原始问题和回答失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取原始问题和回答时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取所有没有标准回答的标准问题
     * 
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param pageable 分页参数
     * @return 没有标准回答的标准问题列表
     */
    @GetMapping("/without-standard-answers")
    public ResponseEntity<?> getQuestionsWithoutStandardAnswers(
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatest,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatestVersion,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取无标准回答问题请求 - 仅最新版本: {}, 仅最新标准问题: {}, 页码: {}, 每页大小: {}", 
            onlyLatest, onlyLatestVersion, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Map<String, Object> result = standardQuestionService.findQuestionsWithoutStandardAnswers(onlyLatest, onlyLatestVersion, pageable);
            
            logger.info("成功获取无标准回答问题 - 总数: {}", result.get("total"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取无标准回答问题失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取无标准回答问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 回退标准问题到指定版本
     * 
     * @param versionId 要回退到的版本ID（变更日志ID）
     * @param questionDTO 包含用户ID和提交信息的DTO
     * @return 回退后的标准问题
     */
    @PostMapping("/version/{versionId}/rollback")
    public ResponseEntity<?> rollbackQuestion(
            @PathVariable Long versionId,
            @RequestBody StandardQuestionDTO questionDTO) {
        logger.debug("接收到回退标准问题请求 - 目标版本ID: {}, 用户ID: {}", 
            versionId, questionDTO.getUserId());
        try {
            // 设置默认的提交信息
            if (questionDTO.getCommitMessage() == null || questionDTO.getCommitMessage().trim().isEmpty()) {
                questionDTO.setCommitMessage("回退到版本 " + versionId);
            }

            StandardQuestionDTO result = standardQuestionService.rollbackQuestion(versionId, 
                questionDTO.getUserId(), questionDTO.getCommitMessage());
            
            logger.info("成功回退标准问题 - 版本ID: {}, 用户ID: {}", versionId, questionDTO.getUserId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("回退标准问题失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            logger.error("回退标准问题失败 - 状态错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            logger.error("回退标准问题失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "回退标准问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取所有有标准答案的标准问题，支持分页
     * 
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param pageable 分页参数
     * @return 有标准答案的标准问题列表
     */
    @GetMapping("/with-standard-answers")
    public ResponseEntity<?> getQuestionsWithStandardAnswers(
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatest,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatestVersion,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取有标准回答问题请求 - 仅最新版本: {}, 仅最新标准问题: {}, 页码: {}, 每页大小: {}", 
            onlyLatest, onlyLatestVersion, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Map<String, Object> result = standardQuestionService.findQuestionsWithStandardAnswers(onlyLatest, onlyLatestVersion, pageable);
            
            logger.info("成功获取有标准回答问题 - 总数: {}", result.get("total"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取有标准回答问题失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取有标准回答问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根据数据集ID查询问题，支持数据集内或数据集外的查询
     * 
     * @param datasetId 数据集ID
     * @param inOrOut 查询方向 (in - 数据集内, out - 数据集外)
     * @param onlyLatest 是否只返回最新版本（叶子节点）
     * @param onlyLatestVersion 是否只显示最新的标准问题（没有子版本的问题）
     * @param onlyWithStandardAnswers 是否只返回有标准答案的问题
     * @param tags 标签列表，多个标签用逗号分隔
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 匹配的标准问题列表
     */
    @GetMapping("/by-dataset/{datasetId}")
    public ResponseEntity<?> getQuestionsByDataset(
            @PathVariable Long datasetId,
            @RequestParam(required = true) String inOrOut,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatest,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyLatestVersion,
            @RequestParam(required = false, defaultValue = "true") Boolean onlyWithStandardAnswers,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        
        logger.info("接收到基于数据集的问题查询请求 - 数据集ID: {}, 查询方向: {}, 仅最新版本: {}, 仅最新标准问题: {}, 仅有标准答案: {}, 标签: {}, 关键词: {}", 
            datasetId, inOrOut, onlyLatest, onlyLatestVersion, onlyWithStandardAnswers, tags, keyword);
        
        try {
            // 验证 inOrOut 参数
            if (!"in".equals(inOrOut) && !"out".equals(inOrOut)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "inOrOut 参数必须是 'in' 或 'out'"
                ));
            }
            
            // 将逗号分隔的标签转换为列表
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = Arrays.asList(tags.split(","));
            }
            
            // 调用服务层方法执行查询
            Map<String, Object> result = standardQuestionService.findQuestionsByDataset(
                    datasetId, Boolean.valueOf("in".equals(inOrOut)), onlyLatest, onlyLatestVersion, onlyWithStandardAnswers, tagList, keyword, pageable);
            
            logger.info("成功查询基于数据集的问题 - 数据集ID: {}, 查询方向: {}, 总数: {}", 
                datasetId, inOrOut, result.get("total"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("查询基于数据集的问题失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询基于数据集的问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 删除标准问题
     * 
     * @param questionId 要删除的问题ID
     * @param userId 执行删除操作的用户ID
     * @param permanent 是否永久删除，true为物理删除，false为逻辑删除（默认）
     * @return 删除结果
     */
    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> deleteStandardQuestion(
            @PathVariable Long questionId,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean permanent) {
        logger.info("接收到删除标准问题请求 - 问题ID: {}, 用户ID: {}, 永久删除: {}", 
            questionId, userId, permanent);
        
        try {
            Map<String, Object> result = standardQuestionService.deleteStandardQuestion(questionId, userId, permanent);
            
            if ((Boolean) result.get("success")) {
                logger.info("成功删除标准问题 - 问题ID: {}", questionId);
                return ResponseEntity.ok(result);
            } else {
                logger.warn("删除标准问题失败 - 问题ID: {}, 原因: {}", questionId, result.get("message"));
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("删除标准问题时发生错误 - 问题ID: {}", questionId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除标准问题时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Map<String, Object> convertToDTO(StandardQuestion question) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", question.getId());
        dto.put("questionText", question.getQuestionText());
        dto.put("questionType", question.getQuestionType());
        dto.put("difficulty", question.getDifficulty());
        dto.put("creationTime", question.getCreationTime());
        dto.put("createdByUserId", question.getCreatedByUser().getId());
        
        if (question.getParentStandardQuestion() != null) {
            dto.put("parentQuestionId", question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.put("originalRawQuestionId", question.getOriginalRawQuestion().getId());
        }
        
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.put("tags", tags);
        
        return dto;
    }
} 