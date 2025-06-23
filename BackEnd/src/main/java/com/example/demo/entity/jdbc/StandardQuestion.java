package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标准问题实体类
 */
public class StandardQuestion {
    
    // 表名
    public static final String TABLE_NAME = "standard_questions";
    
    // 列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_ORIGINAL_RAW_QUESTION_ID = "original_raw_question_id";
    public static final String COLUMN_QUESTION_TEXT = "question_text";
    public static final String COLUMN_QUESTION_TYPE = "question_type";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_CREATION_TIME = "creation_time";
    public static final String COLUMN_CREATED_BY_USER_ID = "created_by_user_id";
    public static final String COLUMN_PARENT_STANDARD_QUESTION_ID = "parent_standard_question_id";
    public static final String COLUMN_CREATED_CHANGE_LOG_ID = "created_change_log_id";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    
    private Long id;
    private RawQuestion originalRawQuestion;
    private String questionText;
    private QuestionType questionType;
    private DifficultyLevel difficulty;
    private LocalDateTime creationTime = LocalDateTime.now();
    private User createdByUser;
    private StandardQuestion parentStandardQuestion;
    private ChangeLog createdChangeLog;
    private LocalDateTime deletedAt;
    private List<StandardQuestionTag> questionTags = new ArrayList<>();
    private List<DatasetQuestionMapping> datasetMappings = new ArrayList<>();
    private StandardObjectiveAnswer standardObjectiveAnswer;
    private StandardSimpleAnswer standardSimpleAnswer;
    private StandardSubjectiveAnswer standardSubjectiveAnswer;

    // 添加标签关联
    public void addTag(StandardQuestionTag tag) {
        questionTags.add(tag);
        tag.setStandardQuestion(this);
    }
    
    // 移除标签关联
    public void removeTag(StandardQuestionTag tag) {
        questionTags.remove(tag);
        tag.setStandardQuestion(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RawQuestion getOriginalRawQuestion() {
        return originalRawQuestion;
    }

    public void setOriginalRawQuestion(RawQuestion originalRawQuestion) {
        this.originalRawQuestion = originalRawQuestion;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * 获取问题难度级别
     * @return 难度级别
     */
    public DifficultyLevel getDifficultyLevel() {
        return difficulty;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public StandardQuestion getParentStandardQuestion() {
        return parentStandardQuestion;
    }

    public void setParentStandardQuestion(StandardQuestion parentStandardQuestion) {
        this.parentStandardQuestion = parentStandardQuestion;
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
    
    public List<StandardQuestionTag> getQuestionTags() {
        return questionTags;
    }

    public void setQuestionTags(List<StandardQuestionTag> questionTags) {
        this.questionTags = questionTags;
    }
    
    /**
     * 获取问题的标签列表
     * @return 标签列表
     */
    public List<Tag> getTags() {
        if (questionTags == null) {
            return new ArrayList<>();
        }
        return questionTags.stream()
                .map(StandardQuestionTag::getTag)
                .collect(Collectors.toList());
    }
    
    public List<DatasetQuestionMapping> getDatasetMappings() {
        return datasetMappings;
    }

    public void setDatasetMappings(List<DatasetQuestionMapping> datasetMappings) {
        this.datasetMappings = datasetMappings;
    }

    public StandardObjectiveAnswer getStandardObjectiveAnswer() {
        return standardObjectiveAnswer;
    }

    public void setStandardObjectiveAnswer(StandardObjectiveAnswer standardObjectiveAnswer) {
        this.standardObjectiveAnswer = standardObjectiveAnswer;
    }

    public StandardSimpleAnswer getStandardSimpleAnswer() {
        return standardSimpleAnswer;
    }

    public void setStandardSimpleAnswer(StandardSimpleAnswer standardSimpleAnswer) {
        this.standardSimpleAnswer = standardSimpleAnswer;
    }

    public StandardSubjectiveAnswer getStandardSubjectiveAnswer() {
        return standardSubjectiveAnswer;
    }

    public void setStandardSubjectiveAnswer(StandardSubjectiveAnswer standardSubjectiveAnswer) {
        this.standardSubjectiveAnswer = standardSubjectiveAnswer;
    }
} 