import api from './index'
import { apiUrls } from '@/config'

/**
 * 用户信息接口
 */
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

/**
 * 创建评测主观题提示词请求接口
 */
export interface CreateEvaluationSubjectivePromptRequest {
  name: string                       // 提示词名称
  promptTemplate: string             // 提示词模板内容
  description: string                // 描述信息
  evaluationCriteriaFocus: object    // 评估标准焦点
  scoringInstruction: string         // 评分指导
  outputFormatInstruction: string    // 输出格式要求
  isActive: boolean                  // 是否激活
  version: string                    // 版本号
  parentPromptId: null               // 父提示词ID
  userId: number                     // 用户ID
}

/**
 * 评测主观题提示词信息接口
 */
export interface EvaluationSubjectivePromptInfo {
  id: number                         // 提示词ID
  name: string                       // 提示词名称
  promptTemplate: string             // 提示词模板内容
  description: string                // 描述信息
  evaluationCriteriaFocus: object    // 评估标准焦点
  scoringInstruction: string         // 评分指导
  outputFormatInstruction: string    // 输出格式要求
  isActive: boolean                  // 是否激活
  version: string                    // 版本号
  createdAt: string                  // 创建时间
  updatedAt: string                  // 更新时间
  createdByUser: UserInfo            // 创建者信息
}

/**
 * 创建评测主观题提示词
 * @param data 评测主观题提示词数据
 * @returns 创建的评测主观题提示词信息
 *
 * 接口路径: /api/prompts/evaluation/subjective
 * 请求方法: POST
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "提示词名称",
 *   "promptTemplate": "提示词模板内容",
 *   "description": "描述信息",
 *   "evaluationCriteriaFocus": {},
 *   "scoringInstruction": "评分指导",
 *   "outputFormatInstruction": "输出格式要求",
 *   "isActive": true,
 *   "version": "版本号",
 *   "parentPromptId": null,
 *   "userId": 1
 * }
 * ```
 */
export const createEvaluationSubjectivePrompt = (data: CreateEvaluationSubjectivePromptRequest) => {
  return api.post<unknown, EvaluationSubjectivePromptInfo>(
    apiUrls.prompts.evaluationSubjective,
    data
  )
}

/**
 * 更新评测主观题提示词请求接口（与创建请求相同）
 */
export type UpdateEvaluationSubjectivePromptRequest = CreateEvaluationSubjectivePromptRequest;

/**
 * 更新评测主观题提示词
 * @param id 评测主观题提示词ID
 * @param data 更新的评测主观题提示词数据
 * @returns 更新后的评测主观题提示词信息
 *
 * 接口路径: /api/prompts/evaluation/subjective/{id}
 * 请求方法: PUT
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "提示词名称",
 *   "promptTemplate": "提示词模板内容",
 *   "description": "描述信息",
 *   "evaluationCriteriaFocus": {},
 *   "scoringInstruction": "评分指导",
 *   "outputFormatInstruction": "输出格式要求",
 *   "isActive": true,
 *   "version": "版本号",
 *   "parentPromptId": null,
 *   "userId": 1
 * }
 * ```
 */
export const updateEvaluationSubjectivePrompt = (
  id: number | string,
  data: UpdateEvaluationSubjectivePromptRequest
) => {
  return api.put<unknown, EvaluationSubjectivePromptInfo>(
    `${apiUrls.prompts.updateEvaluationSubjective}/${id}`,
    data
  )
}

/**
 * 获取评测主观题提示词详情
 * @param id 评测主观题提示词ID
 * @returns 评测主观题提示词详细信息
 *
 * 接口路径: /api/prompts/evaluation/subjective/{id}
 * 请求方法: GET
 * 返回: 指定ID的评测主观题提示词详细信息
 */
export const getEvaluationSubjectivePromptById = (id: number | string) => {
  return api.get<unknown, EvaluationSubjectivePromptInfo>(
    `${apiUrls.prompts.getEvaluationSubjectiveDetail}/${id}`
  )
}

/**
 * 获取所有评测主观题提示词
 * @returns 所有评测主观题提示词列表
 *
 * 接口路径: /api/prompts/evaluation/subjective
 * 请求方法: GET
 * 返回: 所有评测主观题提示词信息列表
 */
export const getAllEvaluationSubjectivePrompts = () => {
  return api.get<unknown, EvaluationSubjectivePromptInfo[]>(
    apiUrls.prompts.getAllEvaluationSubjective
  )
}

/**
 * 获取所有激活状态的评测主观题提示词
 * @returns 所有激活状态的评测主观题提示词列表
 *
 * 接口路径: /prompts/evaluation/subjective/active
 * 请求方法: GET
 * 返回: 所有激活状态的评测主观题提示词信息列表
 */
export const getActiveEvaluationSubjectivePrompts = () => {
  return api.get<unknown, EvaluationSubjectivePromptInfo[]>(
    apiUrls.prompts.getActiveEvaluationSubjective
  )
}

/**
 * 删除评测主观题提示词请求接口
 */
export interface DeleteEvaluationSubjectivePromptRequest {
  userId: number
}

/**
 * 删除评测主观题提示词（软删除）
 * @param id 评测主观题提示词ID
 * @param data 包含用户ID的请求数据
 * @returns 空对象
 *
 * 接口路径: /prompts/evaluation/subjective/{id}
 * 请求方法: DELETE
 *
 * 示例请求:
 * ```json
 * {
 *   "userId": 1
 * }
 * ```
 */
export const deleteEvaluationSubjectivePrompt = (
  id: number | string,
  data: DeleteEvaluationSubjectivePromptRequest
) => {
  return api.delete<unknown, Record<string, never>>(
    `${apiUrls.prompts.deleteEvaluationSubjective}/${id}`,
    { data }
  )
}
