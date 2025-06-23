import api from './index'
import { apiUrls } from '@/config'
import type {
  AnswerHistoryVersion,
  AnswerVersionTreeResponse,
  AnswerCompareResult,
  AnswerRollbackRequest,
  AnswerRollbackResponse,
  ObjectiveAnswerRollbackResponse,
  ShortAnswerRollbackResponse,
  SubjectiveAnswerRollbackResponse,
  FieldDiff
} from '@/types/standardAnswer'

// 导出类型以便在其他组件中使用
export type {
  AnswerHistoryVersion,
  AnswerVersionTreeResponse,
  AnswerCompareResult,
  AnswerRollbackRequest,
  AnswerRollbackResponse,
  ObjectiveAnswerRollbackResponse,
  ShortAnswerRollbackResponse,
  SubjectiveAnswerRollbackResponse,
  FieldDiff
}

/**
 * 获取标准回答的历史版本列表
 * @param answerId 标准回答ID
 * @returns Promise<AnswerHistoryVersion[]> 历史版本列表
 */
export const getAnswerHistory = async (answerId: number | string): Promise<AnswerHistoryVersion[]> => {
  const response = await api.get<AnswerHistoryVersion[]>(
    `${apiUrls.standardData.getAnswerHistory}/${answerId}/history`
  )
  return response.data
}

/**
 * 获取标准回答的版本树
 * @param answerId 标准回答ID
 * @returns Promise<AnswerVersionTreeResponse> 版本树结构
 */
export const getAnswerVersionTree = async (answerId: number | string): Promise<AnswerVersionTreeResponse> => {
  const response = await api.get<AnswerVersionTreeResponse>(
    `${apiUrls.standardData.getAnswerVersionTree}/${answerId}/version-tree`
  )
  return response.data
}

/**
 * 比较两个版本的标准回答
 * @param baseVersionId 基准版本ID
 * @param compareVersionId 比较版本ID
 * @returns Promise<AnswerCompareResult> 比较结果
 */
export const compareAnswerVersions = async (
  baseVersionId: number | string,
  compareVersionId: number | string
): Promise<AnswerCompareResult> => {
  const response = await api.get<AnswerCompareResult>(
    `${apiUrls.standardData.compareAnswerVersions}/${baseVersionId}/compare/${compareVersionId}`
  )
  return response.data
}

/**
 * 回滚标准回答到指定版本
 * @param versionId 目标版本ID
 * @param data 回滚请求数据
 * @returns Promise<AnswerRollbackResponse> 回滚操作响应（不同题型返回不同数据结构）
 */
export const rollbackAnswer = async (
  versionId: number | string,
  data: AnswerRollbackRequest
): Promise<AnswerRollbackResponse> => {
  const response = await api.post<AnswerRollbackResponse>(
    `${apiUrls.standardData.rollbackAnswer}/${versionId}/rollback`,
    data
  )
  return response.data
}

/**
 * 判断回滚响应是否为客观题
 * @param response 回滚响应
 * @returns 是否为客观题
 */
export const isObjectiveAnswerResponse = (
  response: AnswerRollbackResponse
): response is ObjectiveAnswerRollbackResponse => {
  return 'options' in response && 'correctIds' in response
}

/**
 * 判断回滚响应是否为简答题
 * @param response 回滚响应
 * @returns 是否为简答题
 */
export const isShortAnswerResponse = (
  response: AnswerRollbackResponse
): response is ShortAnswerRollbackResponse => {
  return 'answerText' in response && 'alternativeAnswers' in response
}

/**
 * 判断回滚响应是否为主观题
 * @param response 回滚响应
 * @returns 是否为主观题
 */
export const isSubjectiveAnswerResponse = (
  response: AnswerRollbackResponse
): response is SubjectiveAnswerRollbackResponse => {
  return 'answerText' in response && 'scoringGuidance' in response
}
