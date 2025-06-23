package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.LlmAnswerQueryDTO;
import com.example.demo.dto.LlmAnswerResponseDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.repository.jdbc.EvaluationRepository;
import com.example.demo.repository.jdbc.LlmAnswerRepository;
import com.example.demo.service.LlmAnswerQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * LLM回答查询服务实现类
 */
@Service
public class LlmAnswerQueryServiceImpl implements LlmAnswerQueryService {

    private static final Logger logger = LoggerFactory.getLogger(LlmAnswerQueryServiceImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final LlmAnswerRepository llmAnswerRepository;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    public LlmAnswerQueryServiceImpl(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            LlmAnswerRepository llmAnswerRepository, 
            EvaluationRepository evaluationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.llmAnswerRepository = llmAnswerRepository;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<LlmAnswerResponseDTO> queryLlmAnswers(LlmAnswerQueryDTO queryDTO) {
        try {
            // 使用命名参数构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            // 基础查询，使用最简单的语法
            sqlBuilder.append("SELECT la.ID, la.MODEL_ANSWER_RUN_ID, la.ANSWER_TEXT, la.GENERATION_TIME, la.REPEAT_INDEX, " +
                    "lm.NAME AS model_name, sq.ID AS question_id, sq.QUESTION_TEXT, sq.QUESTION_TYPE " +
                    "FROM LLM_ANSWERS la " +
                    "JOIN MODEL_ANSWER_RUNS mar ON la.MODEL_ANSWER_RUN_ID = mar.ID " +
                    "JOIN LLM_MODELS lm ON mar.LLM_MODEL_ID = lm.ID " +
                    "JOIN DATASET_QUESTION_MAPPING dqm ON la.DATASET_QUESTION_MAPPING_ID = dqm.ID " +
                    "JOIN STANDARD_QUESTIONS sq ON dqm.STANDARD_QUESTION_ID = sq.ID " +
                    "WHERE 1=1 ");
    
            // 添加条件语句
            // 关键词搜索条件
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                sqlBuilder.append("AND (la.ANSWER_TEXT LIKE :keyword OR sq.QUESTION_TEXT LIKE :keyword) ");
                String searchPattern = "%" + queryDTO.getKeyword().trim() + "%";
                params.addValue("keyword", searchPattern);
            }
            
            // 标签搜索条件
            if (queryDTO.getTag() != null && !queryDTO.getTag().trim().isEmpty()) {
                sqlBuilder.append("AND sq.ID IN (SELECT sqt.STANDARD_QUESTION_ID FROM STANDARD_QUESTION_TAGS sqt " +
                        "JOIN TAGS t ON sqt.TAG_ID = t.ID WHERE t.NAME = :tag) ");
                params.addValue("tag", queryDTO.getTag().trim());
            }
            
            // 只返回特定评测员未评测过的回答
            if (Boolean.TRUE.equals(queryDTO.getOnlyUnevaluated()) && queryDTO.getEvaluatorId() != null) {
                sqlBuilder.append("AND NOT EXISTS (SELECT 1 FROM EVALUATIONS e " +
                        "WHERE e.LLM_ANSWER_ID = la.ID AND e.EVALUATOR_ID = :evaluatorId) ");
                params.addValue("evaluatorId", queryDTO.getEvaluatorId());
            }
            
            // 根据批次ID过滤
            if (queryDTO.getBatchId() != null) {
                sqlBuilder.append("AND mar.ANSWER_GENERATION_BATCH_ID = :batchId ");
                params.addValue("batchId", queryDTO.getBatchId());
            }
            
            // 根据问题类型过滤
            if (queryDTO.getQuestionType() != null && !queryDTO.getQuestionType().trim().isEmpty()) {
                sqlBuilder.append("AND sq.QUESTION_TYPE = :questionType ");
                params.addValue("questionType", queryDTO.getQuestionType().trim());
            }
            
            // 添加排序
            sqlBuilder.append("ORDER BY la.GENERATION_TIME DESC, la.ID DESC ");
            
            // 计算总记录数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_query";
            
            logger.debug("查询总数SQL: {}", countSql);
            logger.debug("查询参数: {}", params.getValues());
            
            Long totalCount = 0L;
            try {
                totalCount = namedParameterJdbcTemplate.queryForObject(countSql, params, Long.class);
                if (totalCount == null) {
                    totalCount = 0L;
                }
            } catch (Exception e) {
                logger.error("计算总记录数时出错: {}", e.getMessage());
                return new PageResponseDTO<>(Collections.emptyList(), 0, queryDTO.getPage(), queryDTO.getSize());
            }
            
            if (totalCount == 0L) {
                return new PageResponseDTO<>(Collections.emptyList(), 0, queryDTO.getPage(), queryDTO.getSize());
            }
            
            // 执行查询
            logger.debug("执行SQL: {}", sqlBuilder.toString());
            logger.debug("查询参数: {}", params.getValues());
            
            List<Map<String, Object>> allRows;
            try {
                allRows = namedParameterJdbcTemplate.queryForList(sqlBuilder.toString(), params);
            } catch (Exception e) {
                logger.error("执行查询时出错: {}", e.getMessage(), e);
                return new PageResponseDTO<>(Collections.emptyList(), 0, queryDTO.getPage(), queryDTO.getSize());
            }
            
            // 手动在内存中处理分页
            int pageSize = queryDTO.getSize();
            int pageNumber = queryDTO.getPage();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allRows.size());
            
            if (fromIndex >= allRows.size()) {
                return new PageResponseDTO<>(Collections.emptyList(), totalCount, pageNumber, pageSize);
            }
            
            List<Map<String, Object>> pagedRows = allRows.subList(fromIndex, toIndex);
            
            // 提取问题ID列表
            Set<Long> questionIds = pagedRows.stream()
                    .map(row -> ((Number) row.get("question_id")).longValue())
                    .collect(Collectors.toSet());
            
            // 提取回答ID列表
            List<Long> answerIds = pagedRows.stream()
                    .map(row -> ((Number) row.get("ID")).longValue())
                    .collect(Collectors.toList());
            
            // 查询每个LLM回答的标签
            Map<Long, List<String>> questionTagsMap = getQuestionTags(questionIds);
            
            // 批量查询标准答案
            Map<Long, String> standardAnswersMap = getStandardAnswers(questionIds);
            
            // 查询哪些回答已被指定评测员评测过
            Set<Long> evaluatedAnswerIds = Collections.emptySet();
            if (queryDTO.getEvaluatorId() != null) {
                evaluatedAnswerIds = getEvaluatedAnswerIds(queryDTO.getEvaluatorId(), answerIds);
            }
            
            // 转换为DTO
            List<LlmAnswerResponseDTO> result = new ArrayList<>();
            for (Map<String, Object> row : pagedRows) {
                LlmAnswerResponseDTO dto = new LlmAnswerResponseDTO();
                
                Long answerId = ((Number) row.get("ID")).longValue();
                Long questionId = ((Number) row.get("question_id")).longValue();
                String questionType = (String) row.get("QUESTION_TYPE");
                
                dto.setId(answerId);
                dto.setModelAnswerRunId(((Number) row.get("MODEL_ANSWER_RUN_ID")).longValue());
                dto.setModelName((String) row.get("model_name"));
                dto.setAnswerText((String) row.get("ANSWER_TEXT"));
                
                if (row.get("GENERATION_TIME") != null) {
                    Object timeValue = row.get("GENERATION_TIME");
                    if (timeValue instanceof java.sql.Timestamp) {
                        dto.setGenerationTime(((java.sql.Timestamp) timeValue).toLocalDateTime());
                    } else if (timeValue instanceof java.time.LocalDateTime) {
                        dto.setGenerationTime((java.time.LocalDateTime) timeValue);
                    } else {
                        logger.warn("意外的日期时间类型: {}", timeValue.getClass().getName());
                    }
                }
                
                dto.setRepeatIndex((Integer) row.get("REPEAT_INDEX"));
                dto.setQuestionId(questionId);
                dto.setQuestionText((String) row.get("QUESTION_TEXT"));
                dto.setQuestionType(questionType);
                dto.setStandardAnswer(standardAnswersMap.get(questionId));
                dto.setTags(questionTagsMap.getOrDefault(questionId, Collections.emptyList()));
                dto.setEvaluated(evaluatedAnswerIds.contains(answerId));
                
                result.add(dto);
            }
            
            // 返回分页结果
            return new PageResponseDTO<>(result, totalCount, queryDTO.getPage(), queryDTO.getSize());
        } catch (Exception e) {
            logger.error("查询LLM回答时发生错误: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 查询哪些回答已被指定评测员评测过
     * 
     * @param evaluatorId 评测员ID
     * @param answerIds 回答ID列表
     * @return 已评测过的回答ID集合
     */
    private Set<Long> getEvaluatedAnswerIds(Long evaluatorId, List<Long> answerIds) {
        if (answerIds.isEmpty()) {
            return Collections.emptySet();
        }
        
        try {
            Set<Long> result = new HashSet<>();
            
            for (Long answerId : answerIds) {
                String sql = "SELECT COUNT(*) FROM EVALUATIONS WHERE EVALUATOR_ID = ? AND LLM_ANSWER_ID = ?";
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, evaluatorId, answerId);
                if (count != null && count > 0) {
                    result.add(answerId);
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("查询已评测回答时出错: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
    
    /**
     * 获取问题标签（添加缓存）
     */
    @Cacheable(value = "question_tags", key = "#questionIds")
    private Map<Long, List<String>> getQuestionTags(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            Map<Long, List<String>> result = new HashMap<>();
            
            for (Long questionId : questionIds) {
                String sql = "SELECT t.NAME AS tag_name FROM STANDARD_QUESTION_TAGS sqt " +
                        "JOIN TAGS t ON sqt.TAG_ID = t.ID WHERE sqt.STANDARD_QUESTION_ID = ?";
                
                List<String> tags = jdbcTemplate.queryForList(sql, String.class, questionId);
                if (!tags.isEmpty()) {
                    result.put(questionId, tags);
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("查询问题标签时出错: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * 批量获取标准答案（添加缓存）
     */
    @Cacheable(value = "standard_answers", key = "#questionIds")
    private Map<Long, String> getStandardAnswers(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> result = new HashMap<>();
        
        // 查询问题类型
        Map<Long, String> questionTypes = new HashMap<>();
        for (Long questionId : questionIds) {
            try {
                String typeSql = "SELECT QUESTION_TYPE FROM STANDARD_QUESTIONS WHERE ID = ?";
                String questionType = jdbcTemplate.queryForObject(typeSql, String.class, questionId);
                if (questionType != null) {
                    questionTypes.put(questionId, questionType);
                }
            } catch (Exception e) {
                logger.error("查询问题{}类型时出错: {}", questionId, e.getMessage());
            }
        }
        
        // 对每个问题查询标准答案
        questionTypes.forEach((questionId, questionType) -> {
            String tableName;
            
            switch (questionType) {
                case "SINGLE_CHOICE":
                case "MULTIPLE_CHOICE":
                    // 单选题和多选题都存储在STANDARD_OBJECTIVE_ANSWERS表中
                    tableName = "STANDARD_OBJECTIVE_ANSWERS";
                    // 对于客观题，我们需要特殊处理，因为答案存储在OPTIONS和CORRECT_IDS字段中
                    try {
                        String optionsSql = "SELECT OPTIONS, CORRECT_IDS FROM " + tableName + 
                                " WHERE STANDARD_QUESTION_ID = ?";
                        
                        Map<String, Object> optionsResult = jdbcTemplate.queryForMap(optionsSql, questionId);
                        if (optionsResult != null) {
                            String options = (String) optionsResult.get("OPTIONS");
                            String correctIds = (String) optionsResult.get("CORRECT_IDS");
                            
                            // 构建格式化的答案文本
                            StringBuilder formattedAnswer = new StringBuilder("选项：\n");
                            
                            // 解析JSON数据
                            try {
                                List<Map<String, String>> optionsList = objectMapper.readValue(options, List.class);
                                List<String> correctIdsList = objectMapper.readValue(correctIds, List.class);
                                
                                for (Map<String, String> option : optionsList) {
                                    String id = option.get("id");
                                    String text = option.get("text");
                                    boolean isCorrect = correctIdsList.contains(id);
                                    
                                    formattedAnswer.append(id)
                                            .append(". ")
                                            .append(text);
                                    
                                    if (isCorrect) {
                                        formattedAnswer.append(" (✓)");
                                    }
                                    
                                    formattedAnswer.append("\n");
                                }
                                
                                result.put(questionId, formattedAnswer.toString());
                            } catch (Exception e) {
                                logger.error("解析客观题答案JSON时出错: {}", e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("查询客观题问题{}的标准答案时出错: {}", questionId, e.getMessage());
                    }
                    break;
                case "SIMPLE_FACT":
                    tableName = "STANDARD_SIMPLE_ANSWERS";
                    try {
                        String answerSql = "SELECT ANSWER_TEXT FROM " + tableName + 
                                " WHERE STANDARD_QUESTION_ID = ?";
                        
                        String answer = jdbcTemplate.queryForObject(answerSql, String.class, questionId);
                        if (answer != null) {
                            result.put(questionId, answer);
                        }
                    } catch (Exception e) {
                        logger.error("查询简单事实题问题{}的标准答案时出错: {}", questionId, e.getMessage());
                    }
                    break;
                case "SUBJECTIVE":
                    tableName = "STANDARD_SUBJECTIVE_ANSWERS";
                    try {
                        String answerSql = "SELECT ANSWER_TEXT FROM " + tableName + 
                                " WHERE STANDARD_QUESTION_ID = ?";
                        
                        String answer = jdbcTemplate.queryForObject(answerSql, String.class, questionId);
                        if (answer != null) {
                            result.put(questionId, answer);
                        }
                    } catch (Exception e) {
                        logger.error("查询主观题问题{}的标准答案时出错: {}", questionId, e.getMessage());
                    }
                    break;
                default:
                    logger.warn("未知题型: {}", questionType);
                    break;
            }
        });
        
        return result;
    }
} 