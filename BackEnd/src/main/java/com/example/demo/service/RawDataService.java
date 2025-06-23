package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionDisplayDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;

public interface RawDataService {
    RawQuestion createQuestion(RawQuestion question);
    RawQuestion createQuestionFromDTO(RawQuestionDTO questionDTO);
    RawAnswer createAnswer(RawAnswerDTO answerDTO);
    RawQuestion createQuestionWithAnswers(RawQuestionWithAnswersDTO dto);
    
    // 分页查询原始问题
    Page<RawQuestionDisplayDTO> findAllRawQuestions(Pageable pageable);
    
    // 根据标准化状态分页查询
    Page<RawQuestionDisplayDTO> findRawQuestionsByStandardizedStatus(boolean isStandardized, Pageable pageable);
    
    // 根据来源网站分页查询
    Page<RawQuestionDisplayDTO> findRawQuestionsBySourceSite(String sourceSite, Pageable pageable);
    
    // 根据标题或内容模糊搜索
    Page<RawQuestionDisplayDTO> searchRawQuestions(String keyword, Pageable pageable);
    
    /**
     * 高级搜索原始问题
     * @param keyword 关键词（可选）
     * @param tags 标签列表（可选）
     * @param unStandardized 是否只返回未标准化的问题
     * @param pageable 分页参数
     * @return 符合条件的问题列表
     */
    Page<RawQuestionDisplayDTO> advancedSearchRawQuestions(String keyword, List<String> tags, 
            Boolean unStandardized, Pageable pageable);
    
    /**
     * 根据多个标签查询问题
     * @param tags 标签列表
     * @param pageable 分页参数
     * @return 满足所有标签的问题列表
     */
    Page<RawQuestionDisplayDTO> findQuestionsByTags(List<String> tags, Pageable pageable);
    
    /**
     * 删除原始问题
     * @param questionId 问题ID
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果问题不存在
     * @throws IllegalStateException 如果问题已被标准化，不能删除
     */
    boolean deleteRawQuestion(Long questionId);
    
    /**
     * 删除原始回答
     * @param answerId 回答ID
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果回答不存在
     */
    boolean deleteRawAnswer(Long answerId);
    
    /**
     * 根据原始问题ID查询其所有原始回答（分页）
     * @param questionId 原始问题ID
     * @param pageable 分页参数
     * @return 原始回答分页列表
     * @throws IllegalArgumentException 如果问题不存在
     */
    Page<RawAnswer> findRawAnswersByQuestionId(Long questionId, Pageable pageable);
} 