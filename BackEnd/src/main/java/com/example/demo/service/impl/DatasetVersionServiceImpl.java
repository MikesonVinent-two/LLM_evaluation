package com.example.demo.service.impl;

import com.example.demo.dto.CreateDatasetVersionRequest;
import com.example.demo.dto.DatasetVersionDTO;
import com.example.demo.dto.UpdateDatasetVersionRequest;
import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.repository.jdbc.DatasetQuestionMappingRepository;
import com.example.demo.repository.jdbc.DatasetVersionRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.DatasetVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatasetVersionServiceImpl implements DatasetVersionService {

    @Autowired
    private DatasetVersionRepository datasetVersionRepository;
    
    @Autowired
    private DatasetQuestionMappingRepository mappingRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public DatasetVersionDTO createDatasetVersion(CreateDatasetVersionRequest request, Long userId) {
        // 检查版本号是否已存在
        if (datasetVersionRepository.existsByVersionNumber(request.getVersionNumber())) {
            throw new BadRequestException("版本号已存在，请使用不同的版本号");
        }
        
        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 创建数据集版本
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionNumber(request.getVersionNumber());
        datasetVersion.setName(request.getName());
        datasetVersion.setDescription(request.getDescription());
        datasetVersion.setCreationTime(LocalDateTime.now());
        datasetVersion.setCreatedByUser(user);
        
        // 保存数据集版本
        datasetVersion = datasetVersionRepository.save(datasetVersion);
        
        // 如果提供了标准问题ID列表，添加到数据集中
        if (request.getStandardQuestionIds() != null && !request.getStandardQuestionIds().isEmpty()) {
            addQuestionsToDataset(datasetVersion, request.getStandardQuestionIds(), userId);
        }
        
        // 构建并返回DTO
        return convertToDTO(datasetVersion);
    }
    
    @Override
    @Transactional
    public DatasetVersionDTO updateDatasetVersion(Long id, UpdateDatasetVersionRequest request, Long userId) {
        // 获取数据集版本
        DatasetVersion datasetVersion = datasetVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetVersion", "id", id));
        
        // 更新基本信息
        if (request.getName() != null) {
            datasetVersion.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            datasetVersion.setDescription(request.getDescription());
        }
        
        // 保存更新
        datasetVersion = datasetVersionRepository.save(datasetVersion);
        
        // 处理问题的添加
        if (request.getStandardQuestionsToAdd() != null && !request.getStandardQuestionsToAdd().isEmpty()) {
            addQuestionsToDataset(datasetVersion, request.getStandardQuestionsToAdd(), userId);
        }
        
        // 处理问题的移除
        if (request.getStandardQuestionsToRemove() != null && !request.getStandardQuestionsToRemove().isEmpty()) {
            removeQuestionsFromDataset(datasetVersion.getId(), request.getStandardQuestionsToRemove());
        }
        
        // 构建并返回DTO
        return convertToDTO(datasetVersion);
    }
    
    @Override
    public DatasetVersionDTO getDatasetVersionById(Long id) {
        DatasetVersion datasetVersion = datasetVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetVersion", "id", id));
        
        return convertToDTO(datasetVersion);
    }
    
    @Override
    public List<DatasetVersionDTO> getAllDatasetVersions(String name) {
        List<DatasetVersion> versions;
        
        if (name != null && !name.trim().isEmpty()) {
            versions = datasetVersionRepository.findByNameContaining(name);
        } else {
            versions = datasetVersionRepository.findAllActiveVersions();
        }
        
        return versions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteDatasetVersion(Long id, Long userId) {
        DatasetVersion datasetVersion = datasetVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetVersion", "id", id));
        
        // 软删除 - 设置删除时间
        datasetVersion.setDeletedAt(LocalDateTime.now());
        datasetVersionRepository.save(datasetVersion);
    }
    
    @Override
    @Transactional
    public DatasetVersionDTO cloneDatasetVersion(Long sourceId, String newVersionNumber, String newName, 
                                               String description, Long userId) {
        // 检查版本号是否已存在
        if (datasetVersionRepository.existsByVersionNumber(newVersionNumber)) {
            throw new BadRequestException("版本号已存在，请使用不同的版本号");
        }
        
        // 获取源数据集版本
        DatasetVersion sourceVersion = datasetVersionRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetVersion", "id", sourceId));
        
        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 创建新的数据集版本
        DatasetVersion newVersion = new DatasetVersion();
        newVersion.setVersionNumber(newVersionNumber);
        newVersion.setName(newName);
        newVersion.setDescription(description);
        newVersion.setCreationTime(LocalDateTime.now());
        newVersion.setCreatedByUser(user);
        
        // 保存新版本
        newVersion = datasetVersionRepository.save(newVersion);
        
        // 复制问题映射
        List<DatasetQuestionMapping> sourceMappings = 
                mappingRepository.findByDatasetVersionOrderByOrderInDataset(sourceVersion);
        
        for (DatasetQuestionMapping sourceMapping : sourceMappings) {
            DatasetQuestionMapping newMapping = new DatasetQuestionMapping();
            newMapping.setDatasetVersion(newVersion);
            newMapping.setStandardQuestion(sourceMapping.getStandardQuestion());
            newMapping.setOrderInDataset(sourceMapping.getOrderInDataset());
            newMapping.setCreatedByUser(user);
            newMapping.setCreatedAt(LocalDateTime.now());
            
            mappingRepository.save(newMapping);
        }
        
        // 返回新版本的DTO
        return convertToDTO(newVersion);
    }
    
    // 辅助方法 - 添加问题到数据集
    private void addQuestionsToDataset(DatasetVersion datasetVersion, List<Long> questionIds, Long userId) {
        // 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 获取数据集中的最大顺序号
        Integer maxOrder = mappingRepository.findMaxOrderInDataset(datasetVersion.getId());
        int nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;
        
        for (Long questionId : questionIds) {
            // 检查问题是否已经在数据集中
            if (mappingRepository.existsByDatasetVersionIdAndStandardQuestionId(datasetVersion.getId(), questionId)) {
                continue; // 跳过已存在的问题
            }
            
            // 检查问题是否存在
            StandardQuestion question = standardQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("StandardQuestion", "id", questionId));
            
            // 创建新的映射
            DatasetQuestionMapping mapping = new DatasetQuestionMapping();
            mapping.setDatasetVersion(datasetVersion);
            mapping.setStandardQuestion(question);
            mapping.setOrderInDataset(nextOrder++);
            mapping.setCreatedByUser(user);
            mapping.setCreatedAt(LocalDateTime.now());
            
            mappingRepository.save(mapping);
        }
    }
    
    // 辅助方法 - 从数据集中移除问题
    private void removeQuestionsFromDataset(Long datasetVersionId, List<Long> questionIds) {
        for (Long questionId : questionIds) {
            List<DatasetQuestionMapping> mappings = mappingRepository.findByDatasetVersionId(datasetVersionId);
            
            for (DatasetQuestionMapping mapping : mappings) {
                if (mapping.getStandardQuestion().getId().equals(questionId)) {
                    mappingRepository.delete(mapping);
                    break;
                }
            }
        }
    }
    
    // 辅助方法 - 转换为DTO
    private DatasetVersionDTO convertToDTO(DatasetVersion datasetVersion) {
        long questionCount = mappingRepository.countByDatasetVersionId(datasetVersion.getId());
        
        return new DatasetVersionDTO(
                datasetVersion.getId(),
                datasetVersion.getVersionNumber(),
                datasetVersion.getName(),
                datasetVersion.getDescription(),
                datasetVersion.getCreationTime(),
                datasetVersion.getCreatedByUser().getId(),
                datasetVersion.getCreatedByUser().getUsername(),
                (int) questionCount
        );
    }
} 