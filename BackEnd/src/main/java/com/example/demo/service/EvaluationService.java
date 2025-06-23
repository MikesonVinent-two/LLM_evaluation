package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.example.demo.entity.jdbc.Evaluation;
import com.example.demo.entity.jdbc.EvaluationCriterion;
import com.example.demo.entity.jdbc.EvaluationDetail;
import com.example.demo.entity.jdbc.EvaluationRun;
import com.example.demo.entity.jdbc.LlmAnswer;
import com.example.demo.entity.jdbc.QuestionType;

/**
 * 评测服务接口
 */
public interface EvaluationService {
    
    /**
     * 评测单个LLM回答
     * 
     * @param llmAnswer LLM回答
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果
     */
    Evaluation evaluateAnswer(LlmAnswer llmAnswer, Long evaluatorId, Long userId);
    
    /**
     * 批量评测LLM回答
     * 
     * @param llmAnswers LLM回答列表
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果列表
     */
    List<Evaluation> evaluateAnswers(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId);
    
    /**
     * 评测单选题回答
     * 
     * @param answerText 回答文本
     * @param correctIds 正确选项ID列表
     * @param options 选项列表
     * @return 评测结果，包含得分和评语
     */
    Map<String, Object> evaluateSingleChoice(String answerText, String correctIds, String options);
    
    /**
     * 评测多选题回答
     * 
     * @param answerText 回答文本
     * @param correctIds 正确选项ID列表
     * @param options 选项列表
     * @return 评测结果，包含得分和评语
     */
    Map<String, Object> evaluateMultipleChoice(String answerText, String correctIds, String options);
    
    /**
     * 评测简单事实题回答
     * 
     * @param answerText 回答文本
     * @param standardAnswer 标准答案文本
     * @param alternativeAnswers 备选答案文本列表
     * @return 评测结果，包含得分、评语和相似度指标
     */
    Map<String, Object> evaluateSimpleFact(String answerText, String standardAnswer, String alternativeAnswers);
    
    /**
     * 使用AI评测主观题回答
     * 
     * @param answerText 回答文本
     * @param questionText 问题文本
     * @param referenceAnswer 参考答案
     * @param criteria 评测标准列表
     * @param evaluatorId AI评测者ID
     * @return 评测结果，包含总分、各维度得分和评语
     */
    Map<String, Object> evaluateSubjectiveWithAI(String answerText, String questionText, 
                                               String referenceAnswer, List<EvaluationCriterion> criteria,
                                               Long evaluatorId);
    
    /**
     * 创建人工评测记录（用于主观题人工评分）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID（人类）
     * @param userId 用户ID
     * @return 创建的评测记录
     */
    Evaluation createHumanEvaluation(Long llmAnswerId, Long evaluatorId, Long userId);
    
    /**
     * 提交人工评测结果
     * 
     * @param evaluationId 评测ID
     * @param overallScore 总分
     * @param comments 评语
     * @param detailScores 各维度得分和评语
     * @param userId 用户ID
     * @return 更新后的评测记录
     */
    Evaluation submitHumanEvaluation(Long evaluationId, BigDecimal overallScore, String comments, 
                                    List<Map<String, Object>> detailScores, Long userId);
    
    /**
     * 创建评测运行记录
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param runName 运行名称
     * @param runDescription 运行描述
     * @param parameters 运行参数
     * @param userId 用户ID
     * @return 创建的评测运行记录
     */
    EvaluationRun createEvaluationRun(Long modelAnswerRunId, Long evaluatorId, String runName,
                                     String runDescription, Map<String, Object> parameters, Long userId);
    
    /**
     * 启动评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 异步任务
     */
    CompletableFuture<Void> startEvaluationRun(Long evaluationRunId);
    
    /**
     * 暂停评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 是否成功暂停
     */
    boolean pauseEvaluationRun(Long evaluationRunId);
    
    /**
     * 恢复评测运行
     * 
     * @param evaluationRunId 评测运行ID
     * @return 异步任务
     */
    CompletableFuture<Void> resumeEvaluationRun(Long evaluationRunId);
    
    /**
     * 获取评测运行进度
     * 
     * @param evaluationRunId 评测运行ID
     * @return 进度信息
     */
    Map<String, Object> getEvaluationRunProgress(Long evaluationRunId);
    
    /**
     * 获取评测运行列表
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param evaluatorId 评测者ID
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 评测运行列表
     */
    List<EvaluationRun> getEvaluationRuns(Long modelAnswerRunId, Long evaluatorId, String status, int page, int size);
    
    /**
     * 获取评测运行详情
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测运行详情
     */
    EvaluationRun getEvaluationRun(Long evaluationRunId);
    
    /**
     * 获取评测运行结果
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测运行结果
     */
    Map<String, Object> getEvaluationRunResults(Long evaluationRunId);
    
    /**
     * 获取评测详情
     * 
     * @param evaluationId 评测ID
     * @return 评测详情列表
     */
    List<EvaluationDetail> getEvaluationDetails(Long evaluationId);
    
    /**
     * 获取特定问题类型的评测标准
     * 
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType);
    
    /**
     * 获取特定问题类型的评测标准（分页，排除已删除）
     * 
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 评测标准列表
     */
    List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType, int page, int size);
    
    /**
     * 保存评测标准（新增或更新）
     * 
     * @param criterion 评测标准对象
     * @return 保存后的评测标准对象
     */
    EvaluationCriterion saveEvaluationCriterion(EvaluationCriterion criterion);
    
    /**
     * 根据ID获取评测标准
     * 
     * @param criterionId 评测标准ID
     * @return 评测标准对象
     */
    EvaluationCriterion getEvaluationCriterionById(Long criterionId);
    
    /**
     * 获取所有评测标准（分页）
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 评测标准列表
     */
    List<EvaluationCriterion> getAllEvaluationCriteria(int page, int size);
    
    /**
     * 删除评测标准
     * 
     * @param criterionId 评测标准ID
     * @param userId 操作用户ID
     */
    void deleteEvaluationCriterion(Long criterionId, Long userId);
    
    /**
     * 计算文本相似度
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度（0-1之间）
     */
    BigDecimal calculateTextSimilarity(String text1, String text2);
    
    /**
     * 使用BERT模型计算文本相似度
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度（0-1之间）
     */
    BigDecimal calculateBertSimilarity(String text1, String text2);
    
    /**
     * 计算ROUGE分数
     * 
     * @param candidateText 候选文本
     * @param referenceText 参考文本
     * @return ROUGE分数
     */
    BigDecimal calculateRougeScore(String candidateText, String referenceText);
    
    /**
     * 计算BLEU分数
     * 
     * @param candidateText 候选文本
     * @param referenceText 参考文本
     * @return BLEU分数
     */
    BigDecimal calculateBleuScore(String candidateText, String referenceText);
    
    /**
     * 评测一个批次中的所有客观题（单选题、多选题和简单事实题）
     * 
     * @param batchId 回答生成批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果统计信息
     */
    Map<String, Object> evaluateBatchObjectiveQuestions(Long batchId, Long evaluatorId, Long userId);

    /**
     * 评测批次的主观题
     * @param batchId 批次ID
     * @param evaluatorId 评测者ID（必须是AI模型类型）
     * @param userId 用户ID
     * @return 评测结果统计
     */
    Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId);
    
    /**
     * 评测批次的主观题（使用指定的评测提示词）
     * @param batchId 批次ID
     * @param evaluatorId 评测者ID（必须是AI模型类型）
     * @param userId 用户ID
     * @param subjectivePromptId 主观题评测提示词ID（可选）
     * @return 评测结果统计
     */
    Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId, Long subjectivePromptId);
    
    /**
     * 批量评测主观题，支持指定评测标准ID列表
     * 
     * @param batchId 批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @param subjectivePromptId 主观题评测提示词ID
     * @param evaluationAssemblyConfigId 评测组装配置ID
     * @param criteriaIds 评测标准ID列表
     * @return 评测结果
     */
    Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId, 
            Long subjectivePromptId, Long evaluationAssemblyConfigId, List<Long> criteriaIds);
    
    /**
     * 重新评测单个主观题回答（强制覆盖已有评测）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测分数
     */
    BigDecimal reEvaluateSubjectiveAnswer(Long llmAnswerId, Long evaluatorId, Long userId);
    
    /**
     * 批量重新评测一个批次中的所有主观题（强制覆盖已有评测）
     * 
     * @param batchId 回答生成批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @return 评测结果统计
     */
    Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId);
    
    /**
     * 批量重新评测主观题（强制覆盖已有评测）- 支持指定评测提示词和评测标准
     * @param batchId 批次ID
     * @param evaluatorId 评测者ID
     * @param userId 用户ID
     * @param subjectivePromptId 主观题评测提示词ID
     * @param evaluationAssemblyConfigId 评测组装配置ID
     * @param criteriaIds 评测标准ID列表
     * @return 评测结果
     */
    Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId,
            Long subjectivePromptId, Long evaluationAssemblyConfigId, List<Long> criteriaIds);
    
    /**
     * 获取客观题机器评测详细结果
     * 
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param page 页码
     * @param size 每页大小
     * @return 客观题评测详细结果
     */
    Map<String, Object> getObjectiveDetailedResults(Long batchId, List<Long> modelIds, int page, int size);
    
    /**
     * 获取主观题大模型评测详细结果
     * 
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param evaluatorId 评测者ID（大模型）
     * @param page 页码
     * @param size 每页大小
     * @return 主观题评测详细结果
     */
    Map<String, Object> getSubjectiveDetailedResults(Long batchId, List<Long> modelIds, Long evaluatorId, int page, int size);
    
    /**
     * 获取主观题所有评测员的评测详细结果
     * 
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param page 页码
     * @param size 每页大小
     * @return 包含所有评测员评测结果的主观题评测详细结果
     */
    Map<String, Object> getSubjectiveDetailedResultsWithAllEvaluators(Long batchId, List<Long> modelIds, int page, int size);
    
    /**
     * 获取待人工评测的回答列表
     * 
     * @param userId 用户ID
     * @param evaluatorId 评测者ID
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 待评测回答列表
     */
    Map<String, Object> getPendingHumanEvaluations(Long userId, Long evaluatorId, Long batchId, List<Long> modelIds, String questionType, int page, int size);
    
    /**
     * 获取用户已评测的回答列表
     * 
     * @param userId 用户ID
     * @param evaluatorId 评测者ID
     * @param batchId 批次ID
     * @param modelIds 模型ID列表
     * @param questionType 问题类型
     * @param page 页码
     * @param size 每页大小
     * @return 已评测回答列表
     */
    Map<String, Object> getCompletedHumanEvaluations(Long userId, Long evaluatorId, Long batchId, List<Long> modelIds, String questionType, int page, int size);
    
    /**
     * 创建并提交人工评测（一步式操作）
     * 
     * @param llmAnswerId LLM回答ID
     * @param evaluatorId 评测者ID（人类）
     * @param overallScore 总分
     * @param comments 评语
     * @param detailScores 各维度得分和评语
     * @param userId 用户ID
     * @return 评测记录
     */
    Evaluation createAndSubmitHumanEvaluation(Long llmAnswerId, Long evaluatorId, 
                                           BigDecimal overallScore, String comments, 
                                           List<Map<String, Object>> detailScores, Long userId);
    
    /**
     * 检查回答是否已被指定评测员评测
     *
     * @param answerId 回答ID
     * @param evaluatorId 评测员ID
     * @return 是否已评测
     */
    boolean isAnswerEvaluatedByEvaluator(Long answerId, Long evaluatorId);
    
    /**
     * 获取标准问题对应的标准答案
     *
     * @param questionId 标准问题ID
     * @return 包含标准答案信息的Map
     */
    Map<String, Object> getStandardAnswerForQuestion(Long questionId);
    
    /**
     * 获取或创建评测运行记录
     *
     * @param batchId 批次ID
     * @param evaluatorId 评测员ID
     * @param userId 用户ID
     * @return 评测运行记录
     */
    EvaluationRun getOrCreateEvaluationRun(Long batchId, Long evaluatorId, Long userId);
    
    /**
     * 一步式人工评测（创建并提交评测）
     *
     * @param llmAnswerId LLM回答ID
     * @param userId 用户ID
     * @return 评测记录
     */
    Evaluation oneStepHumanEvaluation(Long llmAnswerId, Long userId);
    
    /**
     * 获取批次综合评分展示数据
     * 包含客观题、主观题的详细评分统计和模型排名
     *
     * @param batchId 批次ID
     * @param modelIds 模型ID列表（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 批次综合评分数据
     */
    Map<String, Object> getBatchComprehensiveScores(Long batchId, List<Long> modelIds, int page, int size);

    /**
     * 获取回答的所有评分详情（分页）
     * 支持查看每道题的所有评测员评分，包括各个评测标准的详细得分
     * 
     * @param answerId 回答ID（可选）
     * @param batchId 批次ID（可选）
     * @param questionId 问题ID（可选）
     * @param modelIds 模型ID列表（可选）
     * @param evaluatorIds 评测员ID列表（可选）
     * @param questionType 问题类型（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 评分详情分页结果
     */
    Map<String, Object> getAnswerEvaluationDetails(Long answerId, Long batchId, Long questionId, 
            List<Long> modelIds, List<Long> evaluatorIds, String questionType, int page, int size);
} 