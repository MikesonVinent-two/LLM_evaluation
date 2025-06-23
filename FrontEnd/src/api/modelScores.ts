import api from './index'
import { apiUrls } from '@/config'

/**
 * 评分统计信息接口
 */
export interface ScoreStatistics {
  average_score: number  // 平均分
  scored_answers: number // 已评分答案数量
  total_answers: number  // 总答案数量
  max_score?: number     // 最高分（仅OVERALL包含）
  min_score?: number     // 最低分（仅OVERALL包含）
}

/**
 * 题型评分统计信息接口
 */
export interface QuestionTypeScoreStatistics {
  average_score: number  // 平均分
  total_answers: number  // 总答案数量
}

/**
 * 模型批次评分计算结果接口
 */
export interface ModelBatchScoreCalculationResult {
  modelName: string      // 模型名称
  modelId: number        // 模型ID
  provider: string       // 提供商
  scores: {              // 评分统计
    OVERALL: ScoreStatistics  // 总体评分统计
    [key: string]: ScoreStatistics  // 其他类型评分统计
  }
  success: boolean       // 是否成功
  byQuestionType: {      // 按题型统计
    [key: string]: QuestionTypeScoreStatistics  // 各题型评分统计
  }
  batchId: number        // 批次ID
  message: string        // 消息
  calculatedAt: string   // 计算时间
}

/**
 * 计算模型在批次中的评分
 * @param batchId 批次ID
 * @param modelId 模型ID
 * @returns 模型批次评分计算结果
 *
 * 接口路径: /api/model-batch-scores/batches/{batchId}/models/{modelId}/calculate
 * 请求方法: POST
 *
 * 示例用法:
 * ```typescript
 * import { calculateModelBatchScore } from '@/api/modelScores'
 *
 * // 计算模型在批次中的评分
 * calculateModelBatchScore(5, 3).then(result => {
 *   console.log(result.scores.OVERALL.average_score)  // 92
 * })
 * ```
 */
export const calculateModelBatchScore = (batchId: number | string, modelId: number | string) => {
  return api.post<unknown, ModelBatchScoreCalculationResult>(
    `${apiUrls.modelScores.calculateBatchModelScore}/${batchId}/models/${modelId}/calculate`
  )
}

/**
 * 批次中所有模型评分接口
 */
export interface ModelScoresSummary {
  modelName: string      // 模型名称
  modelId: number        // 模型ID
  provider: string       // 提供商
  scores: {              // 各类型评分
    OVERALL: number      // 总体评分
    [key: string]: number // 其他类型评分
  }
}

/**
 * 批次中所有模型评分统计结果接口
 */
export interface BatchAllModelsScoreResult {
  batchName: string                 // 批次名称
  totalModels: number               // 模型总数
  success: boolean                  // 是否成功
  batchId: number                   // 批次ID
  message: string                   // 消息
  batchDescription: string          // 批次描述
  modelScores: ModelScoresSummary[] // 各模型评分汇总
}

/**
 * 计算批次中所有模型的评分统计
 * @param batchId 批次ID
 * @returns 批次中所有模型的评分统计结果
 *
 * 接口路径: /api/model-batch-scores/batches/{batchId}/calculate
 * 请求方法: POST
 *
 * 示例用法:
 * ```typescript
 * import { calculateBatchAllModelsScore } from '@/api/modelScores'
 *
 * // 计算批次中所有模型的评分
 * calculateBatchAllModelsScore(4).then(result => {
 *   // 获取所有模型评分
 *   result.modelScores.forEach(model => {
 *     console.log(`${model.modelName}: ${model.scores.OVERALL}`)
 *   })
 * })
 * ```
 */
export const calculateBatchAllModelsScore = (batchId: number | string) => {
  return api.post<unknown, BatchAllModelsScoreResult>(
    `${apiUrls.modelScores.calculateAllModelsInBatch}/${batchId}/calculate`
  )
}

/**
 * 标签接口
 */
export interface Tag {
  id: number         // 标签ID
  tag_name: string   // 标签名称
  tag_type: string   // 标签类型
}

/**
 * 标准答案接口
 */
export interface DetailedStandardAnswer {
  options: string                // 选项JSON字符串
  correct_ids: string           // 正确答案ID JSON字符串
  answer_type: string           // 答案类型标识
  answer_text: string           // 标准答案文本
  alternative_answers: string   // 替代答案JSON字符串
  scoring_guidance: string      // 评分指导
}

/**
 * 评分详情接口
 */
export interface ScoreDetail {
  id: number                    // 评分ID
  normalized_score: number      // 归一化分数
  score_type: string            // 评分类型
  scoring_method: string        // 评分方法
  comments: string              // 评语
  evaluator_name: string        // 评测者名称
  evaluator_type: string        // 评测者类型
  scored_by_username: string    // 评分者用户名
}

/**
 * 回答详情接口
 */
export interface AnswerDetail {
  id: number                   // 回答ID
  answer_text: string          // 回答内容
  repeat_index: number         // 重复索引
  generation_time: string      // 生成时间
  run_id: number               // 运行ID
  run_name: string             // 运行名称
  scores: ScoreDetail[]        // 评分详情
  averageScore: number         // 平均分
  hasScores: boolean           // 是否有评分
}

/**
 * 重复索引统计接口
 */
export interface RepeatIndexStat {
  count: number                // 回答数量
  scoreCount: number           // 评分数量
  averageScore: number         // 平均分
}

/**
 * 重复索引统计集合接口
 */
export interface RepeatIndexStatistics {
  [key: string]: RepeatIndexStat // 各重复索引的统计
}

/**
 * 详细评分结果接口
 */
export interface DetailedScoresResponse {
  success: boolean                // 请求是否成功
  batchId: number                 // 批次ID
  batchName: string               // 批次名称
  batchCreatedAt: string          // 批次创建时间
  modelId: number                 // 模型ID
  modelName: string               // 模型名称
  provider: string                // 模型提供商
  version: string                 // 模型版本
  questionId: number              // 问题ID
  standardQuestionId: number      // 标准问题ID
  questionText: string            // 问题文本
  questionType: string            // 问题类型
  difficulty: string              // 问题难度
  standardAnswer: DetailedStandardAnswer // 标准答案
  tags: Tag[]                     // 标签列表
  totalAnswers: number            // 总回答数量
  overallAverageScore: number     // 总体平均分
  answers: AnswerDetail[]         // 回答详情列表
  repeatIndexStatistics: RepeatIndexStatistics // 重复索引统计
  hasAnswers: boolean             // 是否有回答数据
}

/**
 * 获取模型在批次中针对特定问题的详细评分
 * @param batchId 批次ID
 * @param modelId 模型ID
 * @param questionId 问题ID
 * @returns 详细评分结果
 *
 * 接口路径: /api/llm-models/batch/{batchId}/model/{modelId}/questions/{questionId}/detailed-scores
 * 请求方法: GET
 *
 * 示例用法:
 * ```typescript
 * import { getDetailedScores } from '@/api/modelScores'
 *
 * // 获取详细评分
 * getDetailedScores(123, 456, 789).then(result => {
 *   if (result.success && result.hasAnswers) {
 *     console.log(`问题: ${result.questionText}`)
 *     console.log(`总体平均分: ${result.overallAverageScore}`)
 *
 *     // 显示各次回答的评分
 *     result.answers.forEach(answer => {
 *       console.log(`第${answer.repeat_index+1}次回答: ${answer.averageScore}分`)
 *     })
 *   }
 * })
 * ```
 */
export const getDetailedScores = (
  batchId: number | string,
  modelId: number | string,
  questionId: number | string
) => {
  return api.get<unknown, DetailedScoresResponse>(
    `${apiUrls.modelScores.detailedScores}/${batchId}/model/${modelId}/questions/${questionId}/detailed-scores`
  )
}

/**
 * 模型评分类型统计接口
 */
export interface ScoreTypeStatistics {
  score_type: string    // 评分类型
  avg_score: number     // 平均分数
  count: number         // 评分数量
  max_score: number     // 最高分
  min_score: number     // 最低分
  std_dev: number       // 标准差
}

/**
 * 批次中模型排名信息接口
 */
export interface ModelRankingInfo {
  id: number                      // 模型ID
  name: string                    // 模型名称
  provider: string                // 提供商
  version: string                 // 版本
  totalAnswers: number            // 总回答数量
  totalScores: number             // 总评分数量
  overallAverageScore: number     // 总体平均分
  scoresByType: ScoreTypeStatistics[] // 按评分类型统计
}

/**
 * 批次内模型排名响应接口
 */
export interface BatchModelRankingsResponse {
  success: boolean                // 请求是否成功
  batchId: number                 // 批次ID
  batchName: string               // 批次名称
  batchCreatedAt: string          // 批次创建时间
  totalModels: number             // 模型总数
  models: ModelRankingInfo[]      // 模型排名信息列表
  hasModels: boolean              // 是否有模型数据
}

/**
 * 获取批次内模型排名
 * @param batchId 批次ID
 * @returns 批次内模型排名结果
 *
 * 接口路径: /api/llm-models/batch/{batchId}/rankings
 * 请求方法: GET
 *
 * 示例用法:
 * ```typescript
 * import { getBatchModelRankings } from '@/api/modelScores'
 *
 * // 获取批次内模型排名
 * getBatchModelRankings(123).then(result => {
 *   if (result.success && result.hasModels) {
 *     console.log(`批次: ${result.batchName}`)
 *     console.log(`共有${result.totalModels}个模型`)
 *
 *     // 按总体平均分排序
 *     const sortedModels = [...result.models].sort(
 *       (a, b) => b.overallAverageScore - a.overallAverageScore
 *     )
 *
 *     // 显示排名
 *     sortedModels.forEach((model, index) => {
 *       console.log(`${index + 1}. ${model.name}: ${model.overallAverageScore}分`)
 *     })
 *   }
 * })
 * ```
 */
export const getBatchModelRankings = (batchId: number | string) => {
  return api.get<unknown, BatchModelRankingsResponse>(
    `${apiUrls.modelScores.modelRankings}/${batchId}/rankings`
  )
}

/**
 * 按评分类型的简化统计接口
 */
export interface SimpleScoreTypeStatistics {
  score_type: string    // 评分类型
  avg_score: number     // 平均分数
  count: number         // 评分数量
  max_score: number     // 最高分
  min_score: number     // 最低分
}

/**
 * 问题类型表现详情接口
 */
export interface QuestionTypePerformance {
  questionType: string                    // 问题类型
  totalAnswers: number                    // 总回答数量
  totalScores: number                     // 总评分数量
  overallAverageScore: number             // 总体平均分
  scoresByType: SimpleScoreTypeStatistics[] // 按评分类型统计
}

/**
 * 问题类型维度的模型表现响应接口
 */
export interface ModelPerformanceByQuestionTypeResponse {
  success: boolean                        // 请求是否成功
  batchId: number                         // 批次ID
  batchName: string                       // 批次名称
  batchCreatedAt: string                  // 批次创建时间
  modelId: number                         // 模型ID
  modelName: string                       // 模型名称
  provider: string                        // 提供商
  version: string                         // 版本
  totalQuestionTypes: number              // 问题类型总数
  performanceByQuestionType: {            // 按问题类型的表现
    [questionType: string]: QuestionTypePerformance
  }
  hasData: boolean                        // 是否有数据
}

/**
 * 获取问题类型维度的模型表现
 * @param batchId 批次ID
 * @param modelId 模型ID
 * @returns 问题类型维度的模型表现结果
 *
 * 接口路径: /api/llm-models/batch/{batchId}/model/{modelId}/performance-by-question-type
 * 请求方法: GET
 *
 * 示例用法:
 * ```typescript
 * import { getModelPerformanceByQuestionType } from '@/api/modelScores'
 *
 * // 获取问题类型维度的模型表现
 * getModelPerformanceByQuestionType(123, 1).then(result => {
 *   if (result.success && result.hasData) {
 *     console.log(`模型: ${result.modelName}`)
 *     console.log(`共有${result.totalQuestionTypes}种问题类型`)
 *
 *     // 显示各题型表现
 *     Object.entries(result.performanceByQuestionType).forEach(([type, performance]) => {
 *       console.log(`\n${type}题型:`)
 *       console.log(`  平均分: ${performance.overallAverageScore.toFixed(2)}`)
 *       console.log(`  回答数: ${performance.totalAnswers}`)
 *     })
 *   }
 * })
 * ```
 */
export const getModelPerformanceByQuestionType = (batchId: number | string, modelId: number | string) => {
  return api.get<unknown, ModelPerformanceByQuestionTypeResponse>(
    `${apiUrls.modelScores.performanceByQuestionType}/${batchId}/model/${modelId}/performance-by-question-type`
  )
}

/**
 * 模型排名信息接口
 */
export interface ModelRankingItem {
  rank: number
  model_id: number
  model_name: string
  provider: string
  version: string
  average_score: number
  total_answers: number
  scored_answers: number
  max_score: number
  min_score: number
  calculated_at: string
}

/**
 * 模型排名响应接口
 */
export interface ModelRankingsResponse {
  success: boolean
  batchId: number
  scoreType: string
  rankings: ModelRankingItem[]
  totalModels: number
}

/**
 * 获取批次中模型的排名信息
 * @param batchId 批次ID
 * @param scoreType 排名依据的评分类型
 * @returns 模型排名信息
 *
 * 接口路径: /api/model-detailed-scores/batch/{batchId}/rankings
 * 请求方法: GET
 *
 * 示例用法:
 * ```typescript
 * import { getModelRankings } from '@/api/modelScores'
 *
 * // 获取总体评分排名
 * getModelRankings(123, 'OVERALL').then(result => {
 *   result.rankings.forEach(model => {
 *     console.log(`排名${model.rank}: ${model.model_name} - ${model.average_score}分`)
 *   })
 * })
 * ```
 */
export const getModelRankings = (
  batchId: number | string,
  scoreType: string = 'OVERALL'
) => {
  return api.get<ModelRankingsResponse>(
    `${apiUrls.modelScores.batchRankings}/${batchId}/rankings`,
    { params: { scoreType } }
  )
}
