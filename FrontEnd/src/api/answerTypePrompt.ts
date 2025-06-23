import api from './index'
import { apiUrls } from '@/config'

// 问题类型枚举
export enum QuestionType {
  SINGLE_CHOICE = 'SINGLE_CHOICE', // 单选题
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE', // 多选题
  SIMPLE_FACT = 'SIMPLE_FACT', // 简单事实题
  SUBJECTIVE = 'SUBJECTIVE' // 主观题
}

// 答题类型提示词创建数据结构
export interface AnswerTypePromptCreateData {
  userId: number                      // 必填，操作用户ID
  name: string                        // 必填，提示词名称
  questionType: QuestionType          // 必填，题型
  promptTemplate: string              // 必填，提示词模板内容
  description: string                 // 可选，描述
  isActive?: boolean                  // 可选，默认为true
  responseFormatInstruction?: string  // 可选，回答格式要求
  responseExample?: string            // 可选，回答示例
  version?: string                    // 可选，版本号
  parentPromptId?: number | null      // 可选，父提示词ID
}

// 答题类型提示词更新数据结构
export interface AnswerTypePromptUpdateData {
  userId: number                      // 必填，操作用户ID
  name: string                        // 必填，提示词名称
  questionType: QuestionType          // 必填，题型
  promptTemplate: string              // 必填，提示词模板内容
  description: string                 // 可选，描述
  isActive?: boolean                  // 可选，默认为true
  responseFormatInstruction?: string  // 可选，回答格式要求
  responseExample?: string            // 可选，回答示例
  version?: string                    // 可选，版本号
  parentPromptId?: number | null      // 可选，父提示词ID
}

// 用户信息
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

// 答题类型提示词返回数据结构
export interface AnswerTypePrompt {
  id: number
  name: string
  questionType: QuestionType
  promptTemplate: string
  description: string
  isActive: boolean
  responseFormatInstruction: string
  responseExample: string
  version: string
  createdAt: string
  updatedAt: string
  createdByUser: UserInfo
}

// 删除答题类型提示词请求数据结构
export interface DeleteAnswerTypePromptData {
  userId: number  // 必填，操作用户ID
}

/**
 * 创建答题类型提示词
 * @param data 答题类型提示词数据
 * @returns 创建的答题类型提示词信息
 */
export const createAnswerTypePrompt = (data: AnswerTypePromptCreateData) => {
  return api.post<unknown, AnswerTypePrompt>(apiUrls.prompts.questionTypes, data)
}

/**
 * 更新答题类型提示词
 * @param id 答题类型提示词ID
 * @param data 更新的答题类型提示词数据
 * @returns 更新后的答题类型提示词信息
 */
export const updateAnswerTypePrompt = (id: number | string, data: AnswerTypePromptUpdateData) => {
  return api.put<unknown, AnswerTypePrompt>(`${apiUrls.prompts.updateQuestionType}/${id}`, data)
}

/**
 * 获取答题类型提示词详情
 * @param id 答题类型提示词ID
 * @returns 答题类型提示词详情信息
 */
export const getAnswerTypePromptDetail = (id: number | string) => {
  return api.get<unknown, AnswerTypePrompt>(`${apiUrls.prompts.getQuestionTypeDetail}/${id}`)
}

/**
 * 获取所有答题类型提示词
 * @returns 所有答题类型提示词列表
 */
export const getAllAnswerTypePrompts = () => {
  return api.get<unknown, AnswerTypePrompt[]>(apiUrls.prompts.getAllQuestionTypes)
}

/**
 * 获取特定题型的激活状态提示词
 * @param type 题型类型（SINGLE_CHOICE, MULTIPLE_CHOICE, SIMPLE_FACT, SUBJECTIVE）
 * @returns 特定题型的激活状态提示词列表
 */
export const getActiveAnswerTypePromptsByType = (type: QuestionType | string) => {
  return api.get<unknown, AnswerTypePrompt[]>(`${apiUrls.prompts.getActiveByQuestionType}/${type}`)
}

/**
 * 删除答题类型提示词
 * @param id 答题类型提示词ID
 * @param data 删除操作数据
 * @returns 删除结果
 */
export const deleteAnswerTypePrompt = (id: number | string, data: DeleteAnswerTypePromptData) => {
  return api.delete<unknown, Record<string, never>>(`${apiUrls.prompts.deleteQuestionType}/${id}`, { data })
}

/**
 * 获取支持的题型枚举值
 * @returns 支持的题型枚举值
 */
export const getSupportedQuestionTypes = () => {
  return api.get<unknown, Record<string, string>>(apiUrls.prompts.getSupportedQuestionTypes)
}
