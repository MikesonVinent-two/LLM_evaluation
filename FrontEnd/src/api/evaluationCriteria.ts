import api from './index'
import { apiUrls } from '@/config'

/**
 * 评测标准创建者信息
 */
export interface CriterionCreator {
  id: number
  username: string
}

/**
 * 评测标准详情
 */
export interface EvaluationCriterion {
  id: number
  name: string
  version: string
  description: string
  questionType: string
  dataType: string
  scoreRange: string
  maxScore: number
  applicableQuestionTypes: string[]
  weight: number
  isRequired: boolean
  orderIndex: number
  options: Record<string, any>
  createdAt: string
  createdByUser: CriterionCreator
  parentCriterion: null | number
  createdChangeLog: null | any
  deletedAt: null | string
}

/**
 * 创建评测标准请求
 */
export interface CreateEvaluationCriterionRequest {
  name: string
  version: string
  description: string
  questionType: string
  dataType: string
  scoreRange: string
  maxScore: number
  applicableQuestionTypes: string[]
  weight: number
  isRequired: boolean
  orderIndex: number
  options: Record<string, any>
}

/**
 * 更新评测标准请求
 */
export interface UpdateEvaluationCriterionRequest {
  name: string
  version: string
  description: string
  questionType: string
  dataType: string
  scoreRange: string
  maxScore: number
  applicableQuestionTypes: string[]
  weight: number
  isRequired: boolean
  orderIndex: number
  options: Record<string, any>
}

/**
 * 删除评测标准响应
 */
export interface DeleteEvaluationCriterionResponse {
  success: boolean
  message: string
  criterionId: number
}

/**
 * 获取所有评测标准
 * @param params 查询参数
 * @returns 评测标准列表
 *
 * 接口路径: /api/evaluations/criteria/all
 * 请求方法: GET
 *
 * 可选参数:
 * - page: 页码（从0开始）
 * - size: 每页条数
 */
export const getAllEvaluationCriteria = (params?: {
  page?: number | string
  size?: number | string
}) => {
  return api.get<unknown, EvaluationCriterion[]>(
    apiUrls.evaluationCriteria.all,
    { params }
  )
}

/**
 * 获取评测标准详情
 * @param criterionId 评测标准ID
 * @returns 评测标准详情
 *
 * 接口路径: /api/evaluations/criteria/{criterionId}
 * 请求方法: GET
 */
export const getEvaluationCriterionDetail = (criterionId: number | string) => {
  return api.get<unknown, EvaluationCriterion>(
    `${apiUrls.evaluationCriteria.detail}/${criterionId}`
  )
}

/**
 * 创建评测标准
 * @param data 评测标准数据
 * @param userId 用户ID（可选）
 * @returns 创建的评测标准
 *
 * 接口路径: /api/evaluations/criteria
 * 请求方法: POST
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "逻辑性",
 *   "version": "1.0",
 *   "description": "回答的逻辑连贯性",
 *   "questionType": "SUBJECTIVE",
 *   "dataType": "SCORE",
 *   "scoreRange": "0-5",
 *   "applicableQuestionTypes": ["SUBJECTIVE"],
 *   "weight": 0.2,
 *   "isRequired": true,
 *   "orderIndex": 2,
 *   "options": {}
 * }
 * ```
 */
export const createEvaluationCriterion = (
  data: CreateEvaluationCriterionRequest,
  userId?: number | string
) => {
  const params = userId ? { userId } : undefined
  return api.post<unknown, CreateEvaluationCriterionRequest>(
    apiUrls.evaluationCriteria.create,
    data,
    { params }
  )
}

/**
 * 更新评测标准
 * @param criterionId 评测标准ID
 * @param data 更新的评测标准数据
 * @param userId 用户ID（可选）
 * @returns 更新后的评测标准
 *
 * 接口路径: /api/evaluations/criteria/{criterionId}
 * 请求方法: PUT
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "逻辑性",
 *   "version": "1.1",
 *   "description": "评估回答的逻辑连贯性和结构合理性",
 *   "questionType": "SUBJECTIVE",
 *   "dataType": "SCORE",
 *   "scoreRange": "0-5",
 *   "applicableQuestionTypes": ["SUBJECTIVE", "ESSAY"],
 *   "weight": 0.25,
 *   "isRequired": true,
 *   "orderIndex": 3,
 *   "options": {}
 * }
 * ```
 */
export const updateEvaluationCriterion = (
  criterionId: number | string,
  data: UpdateEvaluationCriterionRequest,
  userId?: number | string
) => {
  const params = userId ? { userId } : undefined
  return api.put<unknown, EvaluationCriterion>(
    `${apiUrls.evaluationCriteria.update}/${criterionId}`,
    data,
    { params }
  )
}

/**
 * 删除评测标准
 * @param criterionId 评测标准ID
 * @param userId 用户ID（可选）
 * @returns 删除结果
 *
 * 接口路径: /api/evaluations/criteria/{criterionId}
 * 请求方法: DELETE
 *
 * 示例响应:
 * ```json
 * {
 *   "success": true,
 *   "message": "评测标准已删除",
 *   "criterionId": 3
 * }
 * ```
 */
export const deleteEvaluationCriterion = (
  criterionId: number | string,
  userId?: number | string
) => {
  const params = userId ? { userId } : undefined
  return api.delete<unknown, DeleteEvaluationCriterionResponse>(
    `${apiUrls.evaluationCriteria.delete}/${criterionId}`,
    { params }
  )
}
