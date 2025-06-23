package com.example.demo.service.impl;

import com.example.demo.dto.AnswerQuestionTypePromptDTO;
import com.example.demo.dto.AnswerTagPromptDTO;
import com.example.demo.dto.EvaluationSubjectivePromptDTO;
import com.example.demo.dto.EvaluationTagPromptDTO;
import com.example.demo.entity.jdbc.*;
import com.example.demo.repository.jdbc.*;
import com.example.demo.service.PromptService;
import com.example.demo.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromptServiceImpl implements PromptService {

    private static final Logger logger = LoggerFactory.getLogger(PromptServiceImpl.class);
    
    @Autowired
    private AnswerTagPromptRepository answerTagPromptRepository;
    
    @Autowired
    private AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository;
    
    @Autowired
    private EvaluationTagPromptRepository evaluationTagPromptRepository;
    
    @Autowired
    private EvaluationSubjectivePromptRepository evaluationSubjectivePromptRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public AnswerTagPrompt createAnswerTagPrompt(AnswerTagPromptDTO dto, Long userId) {
        logger.info("创建标签提示词: {}", dto.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        Tag tag = tagRepository.findById(dto.getTagId())
                .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + dto.getTagId()));
        
        AnswerTagPrompt prompt = new AnswerTagPrompt();
        prompt.setName(dto.getName());
        prompt.setTag(tag);
        prompt.setPromptTemplate(dto.getPromptTemplate());
        prompt.setDescription(dto.getDescription());
        prompt.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        prompt.setPromptPriority(dto.getPromptPriority() != null ? dto.getPromptPriority() : 50);
        prompt.setVersion(dto.getVersion());
        prompt.setCreatedByUser(user);
        
        if (dto.getParentPromptId() != null) {
            AnswerTagPrompt parentPrompt = answerTagPromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return answerTagPromptRepository.save(prompt);
    }

    @Override
    @Transactional
    public AnswerTagPrompt updateAnswerTagPrompt(Long id, AnswerTagPromptDTO dto, Long userId) {
        logger.info("更新标签提示词: {}", id);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        AnswerTagPrompt prompt = answerTagPromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("标签提示词不存在: " + id));
        
        if (dto.getTagId() != null && !dto.getTagId().equals(prompt.getTag().getId())) {
            Tag tag = tagRepository.findById(dto.getTagId())
                    .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + dto.getTagId()));
            prompt.setTag(tag);
        }
        
        if (dto.getName() != null) {
            prompt.setName(dto.getName());
        }
        
        if (dto.getPromptTemplate() != null) {
            prompt.setPromptTemplate(dto.getPromptTemplate());
        }
        
        prompt.setDescription(dto.getDescription());
        
        if (dto.getIsActive() != null) {
            prompt.setIsActive(dto.getIsActive());
        }
        
        if (dto.getPromptPriority() != null) {
            prompt.setPromptPriority(dto.getPromptPriority());
        }
        
        if (dto.getVersion() != null) {
            prompt.setVersion(dto.getVersion());
        }
        
        prompt.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getParentPromptId() != null && !dto.getParentPromptId().equals(
                prompt.getParentPrompt() != null ? prompt.getParentPrompt().getId() : null)) {
            AnswerTagPrompt parentPrompt = answerTagPromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return answerTagPromptRepository.save(prompt);
    }

    @Override
    public Optional<AnswerTagPrompt> getAnswerTagPromptById(Long id) {
        return answerTagPromptRepository.findById(id);
    }

    @Override
    public List<AnswerTagPrompt> getActiveAnswerTagPromptsByTagId(Long tagId) {
        return answerTagPromptRepository.findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tagId);
    }

    @Override
    public List<AnswerTagPrompt> getAllAnswerTagPrompts() {
        return answerTagPromptRepository.findByDeletedAtIsNull();
    }

    @Override
    @Transactional
    public void deleteAnswerTagPrompt(Long id, Long userId) {
        logger.info("删除标签提示词: {}", id);
        
        AnswerTagPrompt prompt = answerTagPromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("标签提示词不存在: " + id));
        
        prompt.setDeletedAt(LocalDateTime.now());
        answerTagPromptRepository.save(prompt);
    }

    @Override
    @Transactional
    public AnswerQuestionTypePrompt createAnswerQuestionTypePrompt(AnswerQuestionTypePromptDTO dto, Long userId) {
        logger.info("创建题型提示词: {}", dto.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        AnswerQuestionTypePrompt prompt = new AnswerQuestionTypePrompt();
        prompt.setName(dto.getName());
        prompt.setQuestionType(dto.getQuestionType());
        prompt.setPromptTemplate(dto.getPromptTemplate());
        prompt.setDescription(dto.getDescription());
        prompt.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        prompt.setResponseFormatInstruction(dto.getResponseFormatInstruction());
        prompt.setResponseExample(dto.getResponseExample());
        prompt.setVersion(dto.getVersion());
        prompt.setCreatedByUser(user);
        
        if (dto.getParentPromptId() != null) {
            AnswerQuestionTypePrompt parentPrompt = answerQuestionTypePromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return answerQuestionTypePromptRepository.save(prompt);
    }

    @Override
    @Transactional
    public AnswerQuestionTypePrompt updateAnswerQuestionTypePrompt(Long id, AnswerQuestionTypePromptDTO dto, Long userId) {
        logger.info("更新题型提示词: {}", id);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        AnswerQuestionTypePrompt prompt = answerQuestionTypePromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("题型提示词不存在: " + id));
        
        if (dto.getName() != null) {
            prompt.setName(dto.getName());
        }
        
        if (dto.getQuestionType() != null) {
            prompt.setQuestionType(dto.getQuestionType());
        }
        
        if (dto.getPromptTemplate() != null) {
            prompt.setPromptTemplate(dto.getPromptTemplate());
        }
        
        prompt.setDescription(dto.getDescription());
        
        if (dto.getIsActive() != null) {
            prompt.setIsActive(dto.getIsActive());
        }
        
        prompt.setResponseFormatInstruction(dto.getResponseFormatInstruction());
        prompt.setResponseExample(dto.getResponseExample());
        
        if (dto.getVersion() != null) {
            prompt.setVersion(dto.getVersion());
        }
        
        prompt.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getParentPromptId() != null && !dto.getParentPromptId().equals(
                prompt.getParentPrompt() != null ? prompt.getParentPrompt().getId() : null)) {
            AnswerQuestionTypePrompt parentPrompt = answerQuestionTypePromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return answerQuestionTypePromptRepository.save(prompt);
    }

    @Override
    public Optional<AnswerQuestionTypePrompt> getAnswerQuestionTypePromptById(Long id) {
        return answerQuestionTypePromptRepository.findById(id);
    }

    @Override
    public List<AnswerQuestionTypePrompt> getActiveAnswerQuestionTypePromptsByType(QuestionType questionType) {
        return answerQuestionTypePromptRepository.findByQuestionTypeAndIsActiveTrueAndDeletedAtIsNull(questionType);
    }

    @Override
    public List<AnswerQuestionTypePrompt> getAllAnswerQuestionTypePrompts() {
        return answerQuestionTypePromptRepository.findByDeletedAtIsNull();
    }

    @Override
    @Transactional
    public void deleteAnswerQuestionTypePrompt(Long id, Long userId) {
        logger.info("删除题型提示词: {}", id);
        
        AnswerQuestionTypePrompt prompt = answerQuestionTypePromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("题型提示词不存在: " + id));
        
        prompt.setDeletedAt(LocalDateTime.now());
        answerQuestionTypePromptRepository.save(prompt);
    }
    
    // ===== 评测标签提示词相关方法实现 =====
    
    @Override
    @Transactional
    public EvaluationTagPrompt createEvaluationTagPrompt(EvaluationTagPromptDTO dto, Long userId) {
        logger.info("创建评测标签提示词: {}", dto.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        Tag tag = tagRepository.findById(dto.getTagId())
                .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + dto.getTagId()));
        
        EvaluationTagPrompt prompt = new EvaluationTagPrompt();
        prompt.setName(dto.getName());
        prompt.setTag(tag);
        prompt.setPromptTemplate(dto.getPromptTemplate());
        prompt.setDescription(dto.getDescription());
        prompt.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        prompt.setPromptPriority(dto.getPromptPriority() != null ? dto.getPromptPriority() : 50);
        prompt.setVersion(dto.getVersion());
        prompt.setCreatedByUser(user);
        
        if (dto.getParentPromptId() != null) {
            EvaluationTagPrompt parentPrompt = evaluationTagPromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return evaluationTagPromptRepository.save(prompt);
    }

    @Override
    @Transactional
    public EvaluationTagPrompt updateEvaluationTagPrompt(Long id, EvaluationTagPromptDTO dto, Long userId) {
        logger.info("更新评测标签提示词: {}", id);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        EvaluationTagPrompt prompt = evaluationTagPromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评测标签提示词不存在: " + id));
        
        if (dto.getTagId() != null && !dto.getTagId().equals(prompt.getTag().getId())) {
            Tag tag = tagRepository.findById(dto.getTagId())
                    .orElseThrow(() -> new EntityNotFoundException("标签不存在: " + dto.getTagId()));
            prompt.setTag(tag);
        }
        
        if (dto.getName() != null) {
            prompt.setName(dto.getName());
        }
        
        if (dto.getPromptTemplate() != null) {
            prompt.setPromptTemplate(dto.getPromptTemplate());
        }
        
        prompt.setDescription(dto.getDescription());
        
        if (dto.getIsActive() != null) {
            prompt.setIsActive(dto.getIsActive());
        }
        
        if (dto.getPromptPriority() != null) {
            prompt.setPromptPriority(dto.getPromptPriority());
        }
        
        if (dto.getVersion() != null) {
            prompt.setVersion(dto.getVersion());
        }
        
        prompt.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getParentPromptId() != null && !dto.getParentPromptId().equals(
                prompt.getParentPrompt() != null ? prompt.getParentPrompt().getId() : null)) {
            EvaluationTagPrompt parentPrompt = evaluationTagPromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return evaluationTagPromptRepository.save(prompt);
    }

    @Override
    public Optional<EvaluationTagPrompt> getEvaluationTagPromptById(Long id) {
        return evaluationTagPromptRepository.findById(id);
    }

    @Override
    public List<EvaluationTagPrompt> getActiveEvaluationTagPromptsByTagId(Long tagId) {
        return evaluationTagPromptRepository.findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tagId);
    }

    @Override
    public List<EvaluationTagPrompt> getAllEvaluationTagPrompts() {
        return evaluationTagPromptRepository.findByDeletedAtIsNull();
    }

    @Override
    @Transactional
    public void deleteEvaluationTagPrompt(Long id, Long userId) {
        logger.info("删除评测标签提示词: {}", id);
        
        EvaluationTagPrompt prompt = evaluationTagPromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评测标签提示词不存在: " + id));
        
        prompt.setDeletedAt(LocalDateTime.now());
        evaluationTagPromptRepository.save(prompt);
    }
    
    // ===== 评测主观题提示词相关方法实现 =====
    
    @Override
    @Transactional
    public EvaluationSubjectivePrompt createEvaluationSubjectivePrompt(EvaluationSubjectivePromptDTO dto, Long userId) {
        logger.info("创建评测主观题提示词: {}", dto.getName());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        EvaluationSubjectivePrompt prompt = new EvaluationSubjectivePrompt();
        prompt.setName(dto.getName());
        prompt.setPromptTemplate(dto.getPromptTemplate());
        prompt.setDescription(dto.getDescription());
        prompt.setEvaluationCriteriaFocus(dto.getEvaluationCriteriaFocus());
        prompt.setScoringInstruction(dto.getScoringInstruction());
        prompt.setOutputFormatInstruction(dto.getOutputFormatInstruction());
        prompt.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        prompt.setVersion(dto.getVersion());
        prompt.setCreatedByUser(user);
        
        if (dto.getParentPromptId() != null) {
            EvaluationSubjectivePrompt parentPrompt = evaluationSubjectivePromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return evaluationSubjectivePromptRepository.save(prompt);
    }

    @Override
    @Transactional
    public EvaluationSubjectivePrompt updateEvaluationSubjectivePrompt(Long id, EvaluationSubjectivePromptDTO dto, Long userId) {
        logger.info("更新评测主观题提示词: {}", id);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
        EvaluationSubjectivePrompt prompt = evaluationSubjectivePromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评测主观题提示词不存在: " + id));
        
        if (dto.getName() != null) {
            prompt.setName(dto.getName());
        }
        
        if (dto.getPromptTemplate() != null) {
            prompt.setPromptTemplate(dto.getPromptTemplate());
        }
        
        prompt.setDescription(dto.getDescription());
        prompt.setEvaluationCriteriaFocus(dto.getEvaluationCriteriaFocus());
        prompt.setScoringInstruction(dto.getScoringInstruction());
        prompt.setOutputFormatInstruction(dto.getOutputFormatInstruction());
        
        if (dto.getIsActive() != null) {
            prompt.setIsActive(dto.getIsActive());
        }
        
        if (dto.getVersion() != null) {
            prompt.setVersion(dto.getVersion());
        }
        
        prompt.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getParentPromptId() != null && !dto.getParentPromptId().equals(
                prompt.getParentPrompt() != null ? prompt.getParentPrompt().getId() : null)) {
            EvaluationSubjectivePrompt parentPrompt = evaluationSubjectivePromptRepository.findById(dto.getParentPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("父提示词不存在: " + dto.getParentPromptId()));
            prompt.setParentPrompt(parentPrompt);
        }
        
        return evaluationSubjectivePromptRepository.save(prompt);
    }

    @Override
    public Optional<EvaluationSubjectivePrompt> getEvaluationSubjectivePromptById(Long id) {
        return evaluationSubjectivePromptRepository.findById(id);
    }

    @Override
    public List<EvaluationSubjectivePrompt> getActiveEvaluationSubjectivePrompts() {
        return evaluationSubjectivePromptRepository.findByIsActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public List<EvaluationSubjectivePrompt> getAllEvaluationSubjectivePrompts() {
        return evaluationSubjectivePromptRepository.findByDeletedAtIsNull();
    }

    @Override
    @Transactional
    public void deleteEvaluationSubjectivePrompt(Long id, Long userId) {
        logger.info("删除评测主观题提示词: {}", id);
        
        EvaluationSubjectivePrompt prompt = evaluationSubjectivePromptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("评测主观题提示词不存在: " + id));
        
        prompt.setDeletedAt(LocalDateTime.now());
        evaluationSubjectivePromptRepository.save(prompt);
    }
} 