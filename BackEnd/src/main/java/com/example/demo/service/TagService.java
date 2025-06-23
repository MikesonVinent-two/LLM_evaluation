package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import com.example.demo.dto.TagDTO;
import com.example.demo.entity.jdbc.Tag;

public interface TagService {
    
    /**
     * 获取所有标签，并检查每个标签是否有对应的回答prompt和评测prompt
     */
    List<TagDTO> getAllTagsWithPromptStatus();
    
    /**
     * 根据ID获取标签
     */
    Optional<Tag> getTagById(Long id);
    
    /**
     * 根据名称获取标签
     */
    Optional<Tag> getTagByName(String tagName);
    
    /**
     * 创建新标签
     */
    Tag createTag(Tag tag, Long userId);
    
    /**
     * 更新标签
     */
    Tag updateTag(Tag tag);
    
    /**
     * 删除标签（软删除）
     */
    void deleteTag(Long id);
} 