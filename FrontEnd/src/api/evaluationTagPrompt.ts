import api from './index'
import { apiUrls } from '@/config'

/**
 * 标签信息接口
 */
export interface TagInfo {
  id: number
  tagName: string
  tagType: string
  description: string
  createdAt: string
  createdByUser: UserInfo
}

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
 * 创建评测标签提示词请求接口
 */
export interface CreateEvaluationTagPromptRequest {
  userId: number
  tagId: number
  name: string
  promptTemplate: string
  description: string
  isActive: boolean
  promptPriority: number
  version: string
}

/**
 * 评测标签提示词信息接口
 */
export interface EvaluationTagPromptInfo {
  id: number
  tag: TagInfo
  name: string
  promptTemplate: string
  description: string
  isActive: boolean
  promptPriority: number
  version: string
  createdAt: string
  updatedAt: string
  createdByUser: UserInfo
}

/**
 * 创建评测标签提示词
 * @param data 评测标签提示词数据
 * @returns 创建的评测标签提示词信息
 *
 * 接口路径: /api/prompts/evaluation/tags
 * 请求方法: POST
 *
 * 示例请求:
 * ```json
 * {
 *   "userId": 1,
 *   "tagId": 2,
 *   "name": "数学题评分标准",
 *   "promptTemplate": "在评估数学题回答时，请特别注意以下几点：\n1. 计算过程的正确性\n2. 解题思路的合理性\n3. 公式使用的准确性\n4. 结果的精确度",
 *   "description": "用于数学题答案的评分标准",
 *   "isActive": true,
 *   "promptPriority": 70,
 *   "version": "1.0"
 * }
 * ```
 */
export const createEvaluationTagPrompt = (data: CreateEvaluationTagPromptRequest) => {
  return api.post<unknown, EvaluationTagPromptInfo>(
    apiUrls.prompts.evaluationTags,
    data
  )
}

/**
 * 获取单个评测标签提示词详情
 * @param id 评测标签提示词ID
 * @returns 评测标签提示词详细信息
 *
 * 接口路径: /api/prompts/evaluation/tags/{id}
 * 请求方法: GET
 * 返回: 指定ID的评测标签提示词详细信息
 */
export const getEvaluationTagPromptById = (id: number | string) => {
  return api.get<unknown, EvaluationTagPromptInfo>(
    `${apiUrls.prompts.evaluationTags}/${id}`
  )
}

/**
 * 获取所有评测标签提示词
 * @returns 所有评测标签提示词列表
 *
 * 接口路径: /api/prompts/evaluation/tags
 * 请求方法: GET
 * 返回: 所有评测标签提示词信息列表
 */
export const getAllEvaluationTagPrompts = () => {
  return api.get<unknown, EvaluationTagPromptInfo[]>(
    apiUrls.prompts.evaluationTags
  )
}

/**
 * 获取指定标签的所有激活状态的评测提示词
 * @param tagId 标签ID
 * @returns 指定标签的所有激活状态的评测提示词列表
 *
 * 接口路径: /api/prompts/evaluation/tags/active/tag/{tagId}
 * 请求方法: GET
 * 返回: 指定标签的所有激活状态的评测提示词列表
 */
export const getActiveEvaluationTagPromptsByTagId = (tagId: number | string) => {
  return api.get<unknown, EvaluationTagPromptInfo[]>(
    `${apiUrls.prompts.getActiveEvaluationTagsByTagId}/${tagId}`
  )
}

/**
 * 删除评测标签提示词请求接口
 */
export interface DeleteEvaluationTagPromptRequest {
  userId: number
}

/**
 * 删除评测标签提示词
 * @param id 评测标签提示词ID
 * @param data 包含用户ID的请求数据
 * @returns 空对象
 *
 * 接口路径: /api/prompts/evaluation/tags/{id}
 * 请求方法: DELETE
 *
 * 示例请求:
 * ```json
 * {
 *   "userId": 1
 * }
 * ```
 */
export const deleteEvaluationTagPrompt = (
  id: number | string,
  data: DeleteEvaluationTagPromptRequest
) => {
  return api.delete<unknown, Record<string, never>>(
    `${apiUrls.prompts.deleteEvaluationTag}/${id}`,
    { data }
  )
}
