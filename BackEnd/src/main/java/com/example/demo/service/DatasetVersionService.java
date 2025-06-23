package com.example.demo.service;

import com.example.demo.dto.CreateDatasetVersionRequest;
import com.example.demo.dto.DatasetVersionDTO;
import com.example.demo.dto.UpdateDatasetVersionRequest;

import java.util.List;

public interface DatasetVersionService {
    
    /**
     * 创建新的数据集版本
     * @param request 创建请求
     * @param userId 创建用户ID
     * @return 创建的数据集版本
     */
    DatasetVersionDTO createDatasetVersion(CreateDatasetVersionRequest request, Long userId);
    
    /**
     * 更新数据集版本
     * @param id 数据集版本ID
     * @param request 更新请求
     * @param userId 操作用户ID
     * @return 更新后的数据集版本
     */
    DatasetVersionDTO updateDatasetVersion(Long id, UpdateDatasetVersionRequest request, Long userId);
    
    /**
     * 获取数据集版本详情
     * @param id 数据集版本ID
     * @return 数据集版本详情
     */
    DatasetVersionDTO getDatasetVersionById(Long id);
    
    /**
     * 获取数据集版本列表
     * @param name 可选的名称筛选条件
     * @return 数据集版本列表
     */
    List<DatasetVersionDTO> getAllDatasetVersions(String name);
    
    /**
     * 删除数据集版本（软删除）
     * @param id 数据集版本ID
     * @param userId 操作用户ID
     */
    void deleteDatasetVersion(Long id, Long userId);
    
    /**
     * 克隆数据集版本
     * @param sourceId 源数据集版本ID
     * @param newVersionNumber 新版本号
     * @param newName 新名称
     * @param description 描述
     * @param userId 操作用户ID
     * @return 新建的数据集版本
     */
    DatasetVersionDTO cloneDatasetVersion(Long sourceId, String newVersionNumber, String newName, 
                                        String description, Long userId);
} 