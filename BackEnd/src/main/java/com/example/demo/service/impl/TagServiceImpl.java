package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import com.example.demo.dto.TagDTO;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.AnswerTagPromptRepository;
import com.example.demo.repository.jdbc.EvaluationTagPromptRepository;
import com.example.demo.repository.jdbc.TagRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.TagService;
import com.example.demo.exception.EntityNotFoundException;

@Service
public class TagServiceImpl implements TagService {
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AnswerTagPromptRepository answerTagPromptRepository;
    
    @Autowired
    private EvaluationTagPromptRepository evaluationTagPromptRepository;
    
    @Override
    public List<TagDTO> getAllTagsWithPromptStatus() {
        List<Tag> tags = tagRepository.findAll();
        List<TagDTO> tagDTOs = new ArrayList<>();
        
        for (Tag tag : tags) {
            // 排除已删除的标签
            if (tag.getDeletedAt() != null) {
                continue;
            }
            
            TagDTO dto = new TagDTO();
            dto.setId(tag.getId());
            dto.setTagName(tag.getTagName());
            dto.setTagType(tag.getTagType());
            dto.setCreatedAt(tag.getCreatedAt());
            
            // 检查是否有对应的回答prompt
            boolean hasAnswerPrompt = !answerTagPromptRepository
                    .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId())
                    .isEmpty();
            dto.setHasAnswerPrompt(hasAnswerPrompt);
            
            // 检查是否有对应的评测prompt
            boolean hasEvaluationPrompt = !evaluationTagPromptRepository
                    .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId())
                    .isEmpty();
            dto.setHasEvaluationPrompt(hasEvaluationPrompt);
            
            tagDTOs.add(dto);
        }
        
        return tagDTOs;
    }
    
    @Override
    public Optional<Tag> getTagById(Long id) {
        return tagRepository.findById(id);
    }
    
    @Override
    @Cacheable(value = "tags", key = "#tagName")
    public Optional<Tag> getTagByName(String tagName) {
        return tagRepository.findByTagName(tagName);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public Tag createTag(Tag tag, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        tag.setCreatedAt(LocalDateTime.now());
        tag.setCreatedByUser(user);
        return tagRepository.save(tag);
    }
    
    @Override
    @Transactional
    @CachePut(value = "tags", key = "#tag.id")
    public Tag updateTag(Tag tag) {
        return tagRepository.save(tag);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + id));
        tag.setDeletedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }

    /**
     * 获取所有标签（添加缓存）
     */
    @Cacheable(value = "tags", key = "'all_tags'")
    public List<Tag> findAllTags() {
        return tagRepository.findAll();
    }

    /**
     * 保存标签的内部方法（用于缓存管理）
     */
    @CachePut(value = "tags", key = "#tag.id")
    public Tag saveTag(Tag tag) {
        return tagRepository.save(tag);
    }

    /**
     * 删除标签的内部方法（用于缓存管理）
     */
    @CacheEvict(value = "tags", allEntries = true)
    public void deleteTagById(Long tagId) {
        // 使用软删除方式
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + tagId));
        tag.setDeletedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }
} 