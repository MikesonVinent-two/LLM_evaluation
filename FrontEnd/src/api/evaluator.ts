import api from './index'
import { apiUrls } from '@/config'

// 评测者类型枚举
export enum EvaluatorType {
  HUMAN = 'HUMAN',   // 人类评测者
  AI_MODEL = 'AI_MODEL'  // AI模型评测者
}

// 创建评测者请求接口
export interface CreateEvaluatorRequest {
  name: string           // 评测者名称
  evaluatorType: EvaluatorType | string  // 评测者类型
  user?: {               // 如果是人类评测者，需要提供用户ID
    id: number
  }
  llmModel?: {           // 如果是AI评测者，需要提供模型ID
    id: number
  }
  createdByUser: {       // 创建者用户
    id: number
  }
}

/**
 * 创建评测者的示例:
 *
 * 1. 注册人类评测员:
 * ```json
 * {
 *   "evaluatorType": "HUMAN",
 *   "user": {
 *     "id": 123
 *   },
 *   "name": "张三的评测账号",
 *   "createdByUser": {
 *     "id": 123
 *   }
 * }
 * ```
 *
 * 2. 注册模型评测员:
 * ```json
 * {
 *   "evaluatorType": "AI_MODEL",
 *   "llmModel": {
 *     "id": 456
 *   },
 *   "name": "GPT-4评测模型",
 *   "createdByUser": {
 *     "id": 123
 *   }
 * }
 * ```
 */

// 自注册为评测者的请求接口
export interface SelfRegisterEvaluatorRequest {
  name: string  // 评测者名称
}

// 用户信息接口
export interface UserInfo {
  id: number
  username: string
  password: string
  name: string
  contactInfo: string
  role: string
  createdAt: string
  updatedAt: string
}

// 模型信息接口
export interface LlmModelInfo {
  id: number
  createdAt: string
}

// 评测者信息接口
export interface EvaluatorInfo {
  id: number
  evaluatorType: string
  llmModel?: LlmModelInfo
  user?: {
    id: number
    role: string
    createdAt: string
    updatedAt: string
  }
  name: string
  createdAt: string
  createdByUser: {
    id: number
    username: string
    password: string
    name: string
    contactInfo: string
    role: string
    createdAt: string
    updatedAt: string
  }
  aiEvaluator: boolean
  humanEvaluator: boolean
}

// 使用类型别名代替空接口扩展
export type CreateEvaluatorResponse = EvaluatorInfo;

// 获取评测者列表响应接口
export interface GetEvaluatorsResponse {
  evaluators: EvaluatorInfo[]
  total: number
  pageSize: number
  pageNumber: number
}

/**
 * 创建评测者
 * @param data 评测者数据
 * @returns 创建的评测者信息
 */
export const createEvaluator = (data: CreateEvaluatorRequest) => {
  return api.post<unknown, CreateEvaluatorResponse>(apiUrls.evaluators.create, data)
}

/**
 * 当前登录用户注册成为评测者
 * @param data 评测者名称数据
 * @returns 创建的评测者信息
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "我的评测账号"
 * }
 * ```
 */
export const registerSelfAsEvaluator = (data: SelfRegisterEvaluatorRequest) => {
  return api.post<unknown, CreateEvaluatorResponse>(apiUrls.evaluators.register, data)
}

/**
 * 获取评测者列表
 * @returns 评测者列表
 */
export const getEvaluators = () => {
  return api.get<unknown, EvaluatorInfo[]>(apiUrls.evaluators.getAll)
}

/**
 * 分页获取评测者列表
 * @param params 分页参数
 * @returns 评测者分页列表
 */
export const getEvaluatorsWithPagination = (params?: {
  pageNumber?: number
  pageSize?: number
  type?: EvaluatorType
}) => {
  return api.get<unknown, GetEvaluatorsResponse>(apiUrls.evaluators.getAllPage, { params })
}

/**
 * 按类型获取评测者列表
 * @param type 评测者类型
 * @param params 分页参数
 * @returns 评测者列表
 */
export const getEvaluatorsByType = (
  type: EvaluatorType,
  params?: {
    pageNumber?: number
    pageSize?: number
  }
) => {
  return api.get<unknown, GetEvaluatorsResponse>(`${apiUrls.evaluators.getByType}/${type}`, { params })
}

/**
 * 获取评测者详情
 * @param evaluatorId 评测者ID
 * @returns 评测者详情
 */
export const getEvaluatorById = (evaluatorId: number | string) => {
  return api.get<unknown, EvaluatorInfo>(`${apiUrls.evaluators.getById}/${evaluatorId}`)
}

/**
 * 更新评测者
 * @param evaluatorId 评测者ID
 * @param data 更新数据
 * @returns 空对象
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "张医生-资深评测专家"
 * }
 * ```
 */
export const updateEvaluator = (
  evaluatorId: number | string,
  data: { name: string }
) => {
  return api.post<unknown, Record<string, never>>(`${apiUrls.evaluators.update}/${evaluatorId}`, data)
}

/**
 * 删除评测者
 * @param evaluatorId 评测者ID
 * @returns 操作结果
 */
export const deleteEvaluator = (evaluatorId: number | string) => {
  return api.delete<unknown, { success: boolean; message: string }>(
    `${apiUrls.evaluators.delete}/${evaluatorId}`
  )
}

/**
 * 获取所有AI评测者
 * @returns AI评测者列表
 */
export const getAllAiEvaluators = () => {
  return api.get<unknown, EvaluatorInfo[]>(apiUrls.evaluators.getAllAi)
}

/**
 * 获取所有人类评测者
 * @returns 人类评测者列表
 */
export const getAllHumanEvaluators = () => {
  return api.get<unknown, EvaluatorInfo[]>(apiUrls.evaluators.getAllHuman)
}

/**
 * 根据用户ID获取评测者
 * @param userId 用户ID
 * @returns 评测者详情
 *
 * 接口路径: /api/evaluators/user/{userId}
 * 请求方法: GET
 * 返回: 与该用户ID关联的评测者信息
 */
export const getEvaluatorByUserId = (userId: number | string) => {
  return api.get<unknown, EvaluatorInfo>(`${apiUrls.evaluators.getByUserId}/${userId}`)
}

/**
 * AI评测员连通性测试结果接口
 */
export interface AiEvaluatorConnectivityResult {
  modelInfo: {
    provider: string
    name: string
    id: number
    apiType: string
  }
  success: boolean
  responseTime: number
  response: string
  message: string
}

/**
 * 测试AI评测员连通性
 * @param evaluatorId 评测员ID
 * @returns 连通性测试结果
 */
export const testAiEvaluatorConnectivity = (evaluatorId: number | string) => {
  return api.get<unknown, AiEvaluatorConnectivityResult>(
    `${apiUrls.evaluators.testAiConnectivity}/${evaluatorId}`
  )
}

