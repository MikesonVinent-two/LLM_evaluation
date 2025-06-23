package com.example.demo.entity.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评测标准实体类
 */
public class EvaluationCriterion {
    
    // 表名
    public static final String TABLE_NAME = "evaluation_criteria";

    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_QUESTION_TYPE = "question_type";
    public static final String COLUMN_DATA_TYPE = "data_type";
    public static final String COLUMN_SCORE_RANGE = "score_range";
    public static final String COLUMN_APPLICABLE_QUESTION_TYPES = "applicable_question_types";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_IS_REQUIRED = "is_required";
    public static final String COLUMN_ORDER_INDEX = "order_index";
    public static final String COLUMN_OPTIONS = "options";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_PARENT_CRITERION_ID = "parent_criterion_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    /**
     * 评分数据类型
     */
    public enum DataType {
        SCORE,      // 分数类型
        BOOLEAN,    // 布尔类型
        TEXT,       // 文本类型
        CATEGORICAL // 分类类型
    }
    
    private Long id;
    private String name;
    private String version;
    private String description;
    private QuestionType questionType;
    private DataType dataType;
    private String scoreRange;
    private List<String> applicableQuestionTypes;
    private BigDecimal weight;
    private Boolean isRequired = true;
    private Integer orderIndex;
    private Map<String, Object> options;
    private LocalDateTime createdAt = LocalDateTime.now();
    private User createdByUser;
    private EvaluationCriterion parentCriterion;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getScoreRange() {
        return scoreRange;
    }

    public void setScoreRange(String scoreRange) {
        this.scoreRange = scoreRange;
    }
    
    /**
     * 获取评分标准的最大分数
     * @return 最大分数
     */
    public BigDecimal getMaxScore() {
        if (scoreRange == null || scoreRange.isEmpty()) {
            return BigDecimal.ONE; // 默认最大分数为1
        }
        
        try {
            // 假设scoreRange格式为"0-5"或"1-10"这样的范围格式
            String[] parts = scoreRange.split("-");
            if (parts.length == 2) {
                return new BigDecimal(parts[1].trim());
            }
            return BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }
    
    public List<String> getApplicableQuestionTypes() {
        return applicableQuestionTypes;
    }
    
    public void setApplicableQuestionTypes(List<String> applicableQuestionTypes) {
        this.applicableQuestionTypes = applicableQuestionTypes;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public EvaluationCriterion getParentCriterion() {
        return parentCriterion;
    }
    
    public void setParentCriterion(EvaluationCriterion parentCriterion) {
        this.parentCriterion = parentCriterion;
    }
    
    public ChangeLog getCreatedChangeLog() {
        return createdChangeLog;
    }
    
    public void setCreatedChangeLog(ChangeLog createdChangeLog) {
        this.createdChangeLog = createdChangeLog;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
} 