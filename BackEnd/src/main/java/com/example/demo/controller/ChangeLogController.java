package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.ChangeLogDTO;
import com.example.demo.dto.ChangeLogDetailDTO;
import com.example.demo.dto.CreateChangeLogRequest;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.ChangeLogService;

import jakarta.validation.Valid;

/**
 * 变更日志控制器
 */
@RestController
@RequestMapping("/changelogs")
public class ChangeLogController {

    private final ChangeLogService changeLogService;
    private final UserRepository userRepository;

    @Autowired
    public ChangeLogController(ChangeLogService changeLogService, UserRepository userRepository) {
        this.changeLogService = changeLogService;
        this.userRepository = userRepository;
    }

    /**
     * 获取所有变更日志
     */
    @GetMapping
    public ResponseEntity<List<ChangeLogDTO>> getAllChangeLogs() {
        List<ChangeLog> changeLogs = changeLogService.findAllChangeLogs();
        return ResponseEntity.ok(ChangeLogDTO.fromEntityList(changeLogs));
    }

    /**
     * 根据ID获取变更日志
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChangeLogDTO> getChangeLogById(@PathVariable Long id) {
        Optional<ChangeLog> changeLogOpt = changeLogService.findChangeLogById(id);
        if (changeLogOpt.isPresent()) {
            return ResponseEntity.ok(new ChangeLogDTO(changeLogOpt.get()));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "变更日志不存在");
        }
    }

    /**
     * 获取变更日志的详情列表
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<List<ChangeLogDetailDTO>> getChangeLogDetails(@PathVariable Long id) {
        List<ChangeLogDetail> details = changeLogService.findDetailsByChangeLogId(id);
        return ResponseEntity.ok(ChangeLogDetailDTO.fromEntityList(details));
    }

    /**
     * 创建变更日志
     */
    @PostMapping
    public ResponseEntity<ChangeLogDTO> createChangeLog(@Valid @RequestBody CreateChangeLogRequest request) {
        // 查找用户
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户不存在"));

        // 解析变更类型
        ChangeType changeType;
        try {
            changeType = ChangeType.valueOf(request.getChangeType());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的变更类型");
        }

        // 创建变更日志
        ChangeLog changeLog = changeLogService.createChangeLog(changeType, user, request.getCommitMessage());

        // 如果关联了标准问题
        if (request.getAssociatedStandardQuestionId() != null) {
            StandardQuestion question = new StandardQuestion();
            question.setId(request.getAssociatedStandardQuestionId());
            changeLog.setAssociatedStandardQuestion(question);
        }

        // 保存变更日志
        changeLog = changeLogService.findChangeLogById(changeLog.getId()).orElseThrow();

        // 处理变更详情
        for (CreateChangeLogRequest.ChangeDetailRequest detailRequest : request.getDetails()) {
            // 解析实体类型
            EntityType entityType;
            try {
                entityType = EntityType.valueOf(detailRequest.getEntityType());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的实体类型: " + detailRequest.getEntityType());
            }

            // 创建变更详情
            changeLogService.createChangeLogDetail(
                    changeLog,
                    entityType,
                    detailRequest.getEntityId(),
                    detailRequest.getAttributeName(),
                    detailRequest.getOldValue(),
                    detailRequest.getNewValue()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new ChangeLogDTO(changeLog));
    }

    /**
     * 删除变更日志
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteChangeLog(@PathVariable Long id) {
        boolean deleted = changeLogService.deleteChangeLog(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", deleted);
        return ResponseEntity.ok(response);
    }

    /**
     * 按实体类型和ID查询变更历史
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<ChangeLogDetailDTO>> getChangeLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        // 解析实体类型
        EntityType entityTypeEnum;
        try {
            entityTypeEnum = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的实体类型");
        }
        
        List<ChangeLogDetail> details = changeLogService.findDetailsByEntityTypeAndEntityId(entityTypeEnum, entityId);
        return ResponseEntity.ok(ChangeLogDetailDTO.fromEntityList(details));
    }
} 