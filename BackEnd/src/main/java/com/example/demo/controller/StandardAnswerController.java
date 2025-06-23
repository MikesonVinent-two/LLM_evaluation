package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StandardAnswerDTO;
import com.example.demo.service.StandardAnswerService;

@RestController
@RequestMapping("/standard/standard-answers")
public class StandardAnswerController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardAnswerController.class);
    
    @Autowired
    private StandardAnswerService standardAnswerService;
    
    @PostMapping
    public ResponseEntity<?> createOrUpdateStandardAnswer(@RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到创建/更新标准答案请求 - 用户ID: {}", answerDTO.getUserId());
        try {
            Object result = standardAnswerService.createOrUpdateStandardAnswer(answerDTO, answerDTO.getUserId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("创建/更新标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("创建/更新标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    @PutMapping("/{standardQuestionId}")
    public ResponseEntity<?> updateStandardAnswer(
            @PathVariable Long standardQuestionId, 
            @RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到修改标准答案请求 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, answerDTO.getUserId());
        try {
            // 确保DTO中的问题ID与路径参数一致
            if (answerDTO.getStandardQuestionId() == null) {
                answerDTO.setStandardQuestionId(standardQuestionId);
            } else if (!answerDTO.getStandardQuestionId().equals(standardQuestionId)) {
                return ResponseEntity.badRequest().body("路径参数中的问题ID与请求体中的问题ID不匹配");
            }
            
            Object result = standardAnswerService.updateStandardAnswer(standardQuestionId, answerDTO, answerDTO.getUserId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("修改标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("修改标准答案失败 - 状态错误", e);
            return ResponseEntity.status(409).body(e.getMessage());  // 409 Conflict
        } catch (Exception e) {
            logger.error("修改标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    @GetMapping("/{standardQuestionId}")
    public ResponseEntity<?> getStandardAnswer(@PathVariable Long standardQuestionId) {
        logger.debug("接收到获取标准答案请求 - 标准问题ID: {}", standardQuestionId);
        try {
            Object result = standardAnswerService.getStandardAnswer(standardQuestionId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("获取标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("获取标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    @DeleteMapping("/{standardQuestionId}")
    public ResponseEntity<?> deleteStandardAnswer(
            @PathVariable Long standardQuestionId,
            @RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到删除标准答案请求 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, answerDTO.getUserId());
        try {
            standardAnswerService.deleteStandardAnswer(standardQuestionId, answerDTO.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("删除标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("删除标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }

    /**
     * 获取标准答案的历史记录
     */
    @GetMapping("/{answerId}/history")
    public ResponseEntity<?> getAnswerHistory(@PathVariable Long answerId) {
        logger.debug("接收到获取标准答案历史记录请求 - 答案ID: {}", answerId);
        try {
            var result = standardAnswerService.getAnswerHistory(answerId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("获取标准答案历史记录失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("获取标准答案历史记录失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }

    /**
     * 获取标准答案的版本树
     */
    @GetMapping("/{answerId}/version-tree")
    public ResponseEntity<?> getAnswerVersionTree(@PathVariable Long answerId) {
        logger.debug("接收到获取标准答案版本树请求 - 答案ID: {}", answerId);
        try {
            var result = standardAnswerService.getAnswerVersionTree(answerId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("获取标准答案版本树失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("获取标准答案版本树失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }

    /**
     * 比较两个版本的标准答案
     */
    @GetMapping("/{baseVersionId}/compare/{compareVersionId}")
    public ResponseEntity<?> compareAnswerVersions(
            @PathVariable Long baseVersionId,
            @PathVariable Long compareVersionId) {
        logger.debug("接收到比较标准答案版本请求 - 基准版本ID: {}, 比较版本ID: {}", baseVersionId, compareVersionId);
        try {
            var result = standardAnswerService.compareAnswerVersions(baseVersionId, compareVersionId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("比较标准答案版本失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("比较标准答案版本失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }

    /**
     * 回滚标准答案到指定版本
     */
    @PostMapping("/{versionId}/rollback")
    public ResponseEntity<?> rollbackAnswer(
            @PathVariable Long versionId,
            @RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到回滚标准答案请求 - 目标版本ID: {}, 用户ID: {}", 
            versionId, answerDTO.getUserId());
        try {
            // 设置默认的提交信息
            if (answerDTO.getCommitMessage() == null || answerDTO.getCommitMessage().trim().isEmpty()) {
                answerDTO.setCommitMessage("回滚到版本 " + versionId);
            }

            Object result = standardAnswerService.rollbackAnswer(versionId, answerDTO.getUserId(), 
                answerDTO.getCommitMessage());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("回滚标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("回滚标准答案失败 - 版本冲突", e);
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            logger.error("回滚标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
} 