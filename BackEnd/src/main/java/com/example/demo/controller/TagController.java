package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.TagDTO;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.service.TagService;
import com.example.demo.exception.EntityNotFoundException;
import com.example.demo.dto.TagRecommendRequest;
import com.example.demo.dto.TagRecommendResponse;
import com.example.demo.service.TagRecommendationService;

@RestController
@RequestMapping("/tags")
public class TagController {

    private static final Logger logger = LoggerFactory.getLogger(TagController.class);
    
    @Autowired
    private TagService tagService;
    
    @Autowired
    private TagRecommendationService tagRecommendationService;
    
    /**
     * 获取所有标签，并包含是否有对应prompt的状态
     */
    @GetMapping
    public ResponseEntity<List<TagDTO>> getAllTagsWithPromptStatus() {
        logger.info("接收到获取所有标签请求");
        List<TagDTO> tags = tagService.getAllTagsWithPromptStatus();
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 根据ID获取标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tag> getTagById(@PathVariable Long id) {
        logger.info("接收到获取标签请求，ID: {}", id);
        return tagService.getTagById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建新标签
     */
    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody Map<String, Object> requestBody) {
        logger.info("接收到创建标签请求");
        
        String tagName = (String) requestBody.get("tagName");
        String tagType = (String) requestBody.get("tagType");
        Long userId = Long.valueOf(requestBody.get("userId").toString());
        
        // 检查标签名是否已存在
        Optional<Tag> existingTag = tagService.getTagByName(tagName);
        if (existingTag.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        
        Tag createdTag = tagService.createTag(tag, userId);
        return new ResponseEntity<>(createdTag, HttpStatus.CREATED);
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<Tag> updateTag(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        logger.info("接收到更新标签请求，ID: {}", id);
        
        String tagName = (String) requestBody.get("tagName");
        String tagType = (String) requestBody.get("tagType");
        
        // 检查标签是否存在
        Optional<Tag> optionalTag = tagService.getTagById(id);
        if (!optionalTag.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        // 检查新标签名是否与其他标签重复
        Optional<Tag> existingTag = tagService.getTagByName(tagName);
        if (existingTag.isPresent() && !existingTag.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        
        Tag tag = optionalTag.get();
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        
        Tag updatedTag = tagService.updateTag(tag);
        return ResponseEntity.ok(updatedTag);
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        logger.info("接收到删除标签请求，ID: {}", id);
        try {
            tagService.deleteTag(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/check/{tagName}")
    public ResponseEntity<Boolean> checkTagExists(@PathVariable String tagName) {
        logger.info("检查标签是否存在: {}", tagName);
        boolean exists = tagService.getTagByName(tagName).isPresent();
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/recommend")
    public TagRecommendResponse recommendTags(@RequestBody TagRecommendRequest request) {
        return tagRecommendationService.recommendTags(request);
    }
} 