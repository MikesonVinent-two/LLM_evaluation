package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.example.demo.dto.ExpertCandidateAnswerDTO;
import com.example.demo.service.ExpertCandidateAnswerService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/expert-candidate-answers")
@CrossOrigin(origins = "*")
public class ExpertCandidateAnswerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpertCandidateAnswerController.class);
    
    @Autowired
    private ExpertCandidateAnswerService expertCandidateAnswerService;
    
    // 提交专家候选回答
    @PostMapping
    public ResponseEntity<?> submitExpertCandidateAnswer(@Valid @RequestBody ExpertCandidateAnswerDTO answerDTO) {
        logger.info("接收到专家候选回答提交请求 - 问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        try {
            ExpertCandidateAnswerDTO savedAnswer = expertCandidateAnswerService.createExpertCandidateAnswer(answerDTO);
            return ResponseEntity.ok(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("提交专家候选回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("提交专家候选回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    // 根据问题ID获取专家候选回答
    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getAnswersByQuestionId(
            @PathVariable Long questionId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取问题ID为 {} 的专家候选回答", questionId);
        return ResponseEntity.ok(expertCandidateAnswerService.getAnswersByQuestionId(questionId, pageable));
    }
    
    // 根据用户ID获取专家候选回答
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getAnswersByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取用户ID为 {} 的专家候选回答", userId);
        return ResponseEntity.ok(expertCandidateAnswerService.getAnswersByUserId(userId, pageable));
    }
    
    /**
     * 获取所有专家候选回答（分页）
     * 
     * @param pageable 分页参数
     * @return 专家候选回答分页列表
     */
    @GetMapping
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getAllAnswers(
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取所有专家候选回答，分页参数: {}", pageable);
        return ResponseEntity.ok(expertCandidateAnswerService.getAllAnswers(pageable));
    }
    
    /**
     * 获取所有未评分的专家候选回答（分页）
     * 
     * @param pageable 分页参数
     * @return 未评分专家候选回答分页列表
     */
    @GetMapping("/unrated")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getUnratedAnswers(
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取所有未评分专家候选回答，分页参数: {}", pageable);
        return ResponseEntity.ok(expertCandidateAnswerService.getUnratedAnswers(pageable));
    }
    
    /**
     * 获取指定用户已评分的专家候选回答（分页）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 已评分专家候选回答分页列表
     */
    @GetMapping("/rated/user/{userId}")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getRatedAnswersByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取用户ID为 {} 的已评分专家候选回答，分页参数: {}", userId, pageable);
        try {
            Page<ExpertCandidateAnswerDTO> answers = expertCandidateAnswerService.getRatedAnswersByUserId(userId, pageable);
            return ResponseEntity.ok(answers);
        } catch (IllegalArgumentException e) {
            logger.error("获取已评分专家候选回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("获取已评分专家候选回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 更新质量评分和反馈
    @PutMapping("/{answerId}/quality")
    public ResponseEntity<?> updateQualityScoreAndFeedback(
            @PathVariable Long answerId,
            @RequestParam Integer qualityScore,
            @RequestParam(required = false) String feedback) {
        logger.info("更新专家候选回答的质量评分 - 回答ID: {}, 评分: {}", answerId, qualityScore);
        
        try {
            ExpertCandidateAnswerDTO updatedAnswer = expertCandidateAnswerService
                .updateQualityScoreAndFeedback(answerId, qualityScore, feedback);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("更新质量评分失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("更新质量评分失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    /**
     * 修改专家候选回答内容
     * @param answerId 回答ID
     * @param updateRequest 更新请求，包含userId和新的答案内容
     * @return 修改后的回答
     */
    @PutMapping("/{answerId}")
    public ResponseEntity<?> updateExpertCandidateAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody UpdateAnswerRequest updateRequest) {
        logger.info("接收到修改专家候选回答请求 - 回答ID: {}, 用户ID: {}", 
            answerId, updateRequest.getUserId());
        
        try {
            ExpertCandidateAnswerDTO updatedAnswer = expertCandidateAnswerService
                .updateExpertCandidateAnswer(
                    answerId, 
                    updateRequest.getUserId(), 
                    updateRequest.getAnswerText()
                );
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("修改专家候选回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("修改专家候选回答失败 - 权限错误", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("修改专家候选回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    /**
     * 删除专家候选回答
     * @param answerId 回答ID
     * @param userId 用户ID（权限验证）
     * @return 操作结果
     */
    @DeleteMapping("/{answerId}")
    public ResponseEntity<?> deleteExpertCandidateAnswer(
            @PathVariable Long answerId,
            @RequestParam Long userId) {
        logger.info("接收到删除专家候选回答请求 - 回答ID: {}, 用户ID: {}", answerId, userId);
        
        try {
            boolean result = expertCandidateAnswerService.deleteExpertCandidateAnswer(answerId, userId);
            
            if (result) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "专家候选回答删除成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body("删除操作未完成");
            }
        } catch (IllegalArgumentException e) {
            logger.error("删除专家候选回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("删除专家候选回答失败 - 权限错误", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("删除专家候选回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
}

/**
 * 更新专家回答的请求体
 */
class UpdateAnswerRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "回答内容不能为空")
    private String answerText;
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
} 