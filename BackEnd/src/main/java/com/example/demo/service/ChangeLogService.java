package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;

/**
 * 变更日志服务类
 */
@Service
public class ChangeLogService {

    private final ChangeLogRepository changeLogRepository;
    private final ChangeLogDetailRepository changeLogDetailRepository;

    @Autowired
    public ChangeLogService(ChangeLogRepository changeLogRepository, 
                          ChangeLogDetailRepository changeLogDetailRepository) {
        this.changeLogRepository = changeLogRepository;
        this.changeLogDetailRepository = changeLogDetailRepository;
    }
    
    /**
     * 查询所有变更日志
     * 
     * @return 变更日志列表
     */
    public List<ChangeLog> findAllChangeLogs() {
        return changeLogRepository.findAll();
    }
    
    /**
     * 根据ID查询变更日志
     * 
     * @param id 变更日志ID
     * @return 变更日志对象
     */
    public Optional<ChangeLog> findChangeLogById(Long id) {
        return changeLogRepository.findById(id);
    }
    
    /**
     * 查询变更日志详情
     * 
     * @param changeLogId 变更日志ID
     * @return 变更日志详情列表
     */
    public List<ChangeLogDetail> findDetailsByChangeLogId(Long changeLogId) {
        return changeLogDetailRepository.findByChangeLogId(changeLogId);
    }
    
    /**
     * 创建变更日志
     * 
     * @param changeType 变更类型
     * @param user 变更用户
     * @param commitMessage 提交信息
     * @return 创建的变更日志
     */
    @Transactional
    public ChangeLog createChangeLog(ChangeType changeType, User user, String commitMessage) {
        ChangeLog changeLog = new ChangeLog();
        changeLog.setChangeType(changeType);
        changeLog.setUser(user);
        changeLog.setCommitTime(LocalDateTime.now());
        changeLog.setCommitMessage(commitMessage);
        
        return changeLogRepository.save(changeLog);
    }
    
    /**
     * 创建变更日志详情
     * 
     * @param changeLog 变更日志
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param attributeName 属性名称
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 创建的变更日志详情
     */
    @Transactional
    public ChangeLogDetail createChangeLogDetail(ChangeLog changeLog, 
                                              EntityType entityType, 
                                              Long entityId, 
                                              String attributeName, 
                                              String oldValue, 
                                              String newValue) {
        ChangeLogDetail detail = new ChangeLogDetail();
        detail.setChangeLog(changeLog);
        detail.setEntityType(entityType);
        detail.setEntityId(entityId);
        detail.setAttributeName(attributeName);
        detail.setOldValue(oldValue);
        detail.setNewValue(newValue);
        
        return changeLogDetailRepository.save(detail);
    }
    
    /**
     * 删除变更日志
     * 
     * @param id 变更日志ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteChangeLog(Long id) {
        try {
            // 先删除相关的变更日志详情
            List<ChangeLogDetail> details = changeLogDetailRepository.findByChangeLogId(id);
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.delete(detail.getId());
            }
            // 然后删除变更日志本身
            changeLogRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 按实体类型和实体ID查询变更日志详情
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 变更日志详情列表
     */
    public List<ChangeLogDetail> findDetailsByEntityTypeAndEntityId(EntityType entityType, Long entityId) {
        List<ChangeLogDetail> allDetails = changeLogDetailRepository.findAll();
        return allDetails.stream()
                        .filter(detail -> detail.getEntityType() == entityType && 
                                         detail.getEntityId().equals(entityId))
                        .toList();
    }
} 