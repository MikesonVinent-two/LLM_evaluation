import api from './index'
import { apiUrls } from '@/config'

// 回答生成批次状态枚举
export enum BatchStatus {
  PENDING = 'PENDING',
  GENERATING_ANSWERS = 'GENERATING_ANSWERS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  PAUSED = 'PAUSED',
  RESUMING = 'RESUMING'
}

// 全局参数结构
export interface GlobalParameters {
  param1: string
  param2: string
}

// 模型特定参数结构
export interface ModelSpecificParameters {
  [modelId: string]: {
    temperature?: number
    max_tokens?: number
    // 其他可能的模型特定参数
  }
}

// 回答生成批次创建数据结构
export interface AnswerGenerationBatchCreateData {
  name: string                         // 必填，批次名称
  description: string                  // 可选，批次描述
  datasetVersionId: number             // 必填，数据集版本ID
  answerAssemblyConfigId: number       // 必填，回答组装配置ID
  llmModelIds: number[]                // 必填，LLM模型ID列表
  globalParameters?: GlobalParameters   // 可选，全局参数
  modelSpecificParameters?: ModelSpecificParameters // 可选，模型特定参数
  answerRepeatCount?: number            // 可选，每个问题生成回答的重复次数
  userId: number                       // 必填，创建批次的用户ID
  singleChoicePromptId?: number        // 可选，单选题prompt ID
  multipleChoicePromptId?: number      // 可选，多选题prompt ID
  simpleFactPromptId?: number          // 可选，简单事实题prompt ID
  subjectivePromptId?: number          // 可选，主观题prompt ID
}

// 回答生成批次返回数据结构
export interface AnswerGenerationBatch {
  id: number
  name: string
  description: string
  datasetVersionId: number
  datasetVersionName: string
  status: BatchStatus
  creationTime: string
  answerAssemblyConfigId: number
  globalParameters: GlobalParameters
  createdByUserId: number
  createdByUsername: string
  completedAt?: string
  progressPercentage: number
  lastActivityTime: string
  resumeCount: number
  pauseTime?: string
  pauseReason?: string
  answerRepeatCount: number
  totalRuns: number
  pendingRuns: number
  completedRuns: number
  failedRuns: number
  lastProcessedRunId?: number
  singleChoicePromptId?: number
  singleChoicePromptName?: string
  multipleChoicePromptId?: number
  multipleChoicePromptName?: string
  simpleFactPromptId?: number
  simpleFactPromptName?: string
  subjectivePromptId?: number
  subjectivePromptName?: string
}

// 批次状态信息结构
export interface BatchStatusInfo {
  id: number
  name: string
  description: string
  datasetVersionId: number
  datasetVersionName: string
  status: BatchStatus
  creationTime: string
  answerAssemblyConfigId: number
  globalParameters: GlobalParameters
  createdByUserId: number
  progressPercentage: number
  lastActivityTime: string
  resumeCount: number
  answerRepeatCount: number
  completedAt?: string
  pauseTime?: string
  pauseReason?: string
  totalRuns?: number
  pendingRuns?: number
  completedRuns?: number
  failedRuns?: number
  lastProcessedRunId?: number
  singleChoicePromptId?: number
  singleChoicePromptName?: string
  multipleChoicePromptId?: number
  multipleChoicePromptName?: string
  simpleFactPromptId?: number
  simpleFactPromptName?: string
  subjectivePromptId?: number
  subjectivePromptName?: string
}

// 运行状态接口
export interface RunStatus {
  runId: number
  status: string
}

// 模型连通性测试结果接口
export interface ModelConnectivityResult {
  connected: boolean
  modelName: string
  apiEndpoint: string
  modelId: number
  provider: string
  responseTime: number
  success?: boolean
  timestamp?: number
  runs?: RunStatus[]
}

// 连通性测试结果接口
export interface ConnectivityTestResult {
  batchName: string
  totalModels: number
  modelResults: ModelConnectivityResult[]
  failedModels: number
  success: boolean
  passedModels: number
  batchId: number
  batchStatus: string
  testDuration: number
  timestamp: number
}

/**
 * 创建回答生成批次
 * @param data 回答生成批次数据
 * @returns 创建的回答生成批次信息
 */
export const createAnswerGenerationBatch = (data: AnswerGenerationBatchCreateData) => {
  return api.post<unknown, AnswerGenerationBatch>(apiUrls.answerGeneration.batches, data)
}

/**
 * 启动回答生成批次
 * @param batchId 批次ID
 * @returns 启动结果
 */
export const startAnswerGenerationBatch = (batchId: number | string) => {
  return api.post<unknown, Record<string, never>>(`${apiUrls.answerGeneration.startBatch}/${batchId}/start`)
}

/**
 * 暂停回答生成批次
 * @param batchId 批次ID
 * @param reason 暂停原因（可选）
 * @returns 暂停结果
 */
export const pauseAnswerGenerationBatch = (batchId: number | string, reason?: string) => {
  const params = reason ? { reason } : undefined
  return api.post<unknown, Record<string, never>>(
    `${apiUrls.answerGeneration.pauseBatch}/${batchId}/pause`,
    null,
    { params }
  )
}

/**
 * 恢复回答生成批次
 * @param batchId 批次ID
 * @returns 恢复结果
 */
export const resumeAnswerGenerationBatch = (batchId: number | string) => {
  return api.post<unknown, Record<string, never>>(`${apiUrls.answerGeneration.resumeBatch}/${batchId}/resume`)
}

/**
 * 获取回答生成批次状态
 * @param batchId 批次ID
 * @returns 批次状态信息
 */
export const getAnswerGenerationBatchStatus = (batchId: number | string) => {
  return api.get<unknown, BatchStatusInfo>(`${apiUrls.answerGeneration.getBatchStatus}/${batchId}`)
}

/**
 * 测试批次模型连通性
 * @param batchId 批次ID
 * @returns 连通性测试结果
 */
export const testBatchModelConnectivity = (batchId: number | string) => {
  return api.get<unknown, ConnectivityTestResult>(`${apiUrls.answerGeneration.testConnectivity}/${batchId}/test-connectivity`)
}

/**
 * 获取用户的回答生成批次列表
 * @param userId 用户ID
 * @param options 可选参数
 * @returns 批次列表
 */
export const getUserAnswerGenerationBatches = (
  userId: number | string,
  options?: {
    page?: number
    size?: number
    status?: BatchStatus
  }
) => {
  const params = options || {}
  return api.get<unknown, { total: number; batches: AnswerGenerationBatch[] }>(
    `${apiUrls.answerGeneration.getUserBatches}/${userId}`,
    { params }
  )
}

/**
 * 获取所有回答生成批次
 * @returns 所有回答生成批次列表
 */
export const getAllAnswerGenerationBatches = () => {
  return api.get<unknown, AnswerGenerationBatch[]>(apiUrls.answerGeneration.batches)
}

/**
 * 测试单个模型连通性
 * @param modelId 模型ID
 * @returns 模型连通性测试结果
 */
export const testModelConnectivity = (modelId: number | string) => {
  return api.get<unknown, ModelConnectivityResult>(
    `${apiUrls.answerGeneration.testModelConnectivity}/${modelId}/test-connectivity`
  )
}
