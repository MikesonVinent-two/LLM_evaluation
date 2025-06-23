import api from './index'
import { apiUrls } from '@/config'

// 答案标签提示词数据结构
export interface AnswerTagPromptCreateData {
  userId: number            // 必填，操作用户ID
  tagId: number             // 必填，关联的标签ID
  name: string              // 必填，提示词名称
  promptTemplate: string    // 必填，提示词模板内容
  description: string       // 可选，描述
  isActive?: boolean        // 可选，默认为true
  promptPriority?: number   // 可选，默认为50，范围1-100
  version?: string          // 可选，版本号
  parentPromptId?: number | null // 可选，父提示词ID
}

// 答案标签提示词更新数据结构
export interface AnswerTagPromptUpdateData {
  userId: number            // 必填，操作用户ID
  tagId: number             // 必填，关联的标签ID
  name: string              // 必填，提示词名称
  promptTemplate: string    // 必填，提示词模板内容
  description: string       // 可选，描述
  isActive?: boolean        // 可选，默认为true
  promptPriority?: number   // 可选，默认为50，范围1-100
  version?: string          // 可选，版本号
  parentPromptId?: number | null // 可选，父提示词ID
}

// 标签信息
export interface Tag {
  id: number
  tagName: string
  tagType: string
  description: string
  createdAt: string
  createdByUser: UserInfo
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

// 答案标签提示词返回数据结构
export interface AnswerTagPrompt {
  id: number
  tag: Tag
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

// 删除答案标签提示词请求数据结构
export interface DeleteAnswerTagPromptData {
  userId: number  // 必填，操作用户ID
}

/**
 * 创建答案标签提示词
 * @param data 答案标签提示词数据
 * @returns 创建的答案标签提示词信息
 */
export const createAnswerTagPrompt = (data: AnswerTagPromptCreateData) => {
  return api.post<unknown, AnswerTagPrompt>(apiUrls.prompts.tags, data)
}

/**
 * 更新答案标签提示词
 * @param id 答案标签提示词ID
 * @param data 更新的答案标签提示词数据
 * @returns 更新后的答案标签提示词信息
 */
export const updateAnswerTagPrompt = (id: number | string, data: AnswerTagPromptUpdateData) => {
  return api.put<unknown, AnswerTagPrompt>(`${apiUrls.prompts.updateTag}/${id}`, data)
}

/**
 * 获取答案标签提示词详情
 * @param id 答案标签提示词ID
 * @returns 答案标签提示词详情信息
 */
export const getAnswerTagPromptDetail = (id: number | string) => {
  return api.get<unknown, AnswerTagPrompt>(`${apiUrls.prompts.getTagDetail}/${id}`)
}

/**
 * 获取所有答案标签提示词
 * @returns 所有答案标签提示词列表
 */
export const getAllAnswerTagPrompts = () => {
  return api.get<unknown, AnswerTagPrompt[]>(apiUrls.prompts.getAllTags)
}

/**
 * 获取特定标签的激活状态答案标签提示词
 * @param tagId 标签ID
 * @returns 特定标签的激活状态答案标签提示词列表
 */
export const getActiveAnswerTagPromptsByTagId = (tagId: number | string) => {
  return api.get<unknown, AnswerTagPrompt[]>(`${apiUrls.prompts.getActiveByTagId}/${tagId}`)
}

/**
 * 删除答案标签提示词
 * @param id 答案标签提示词ID
 * @param data 删除操作数据
 * @returns 删除结果
 */
export const deleteAnswerTagPrompt = (id: number | string, data: DeleteAnswerTagPromptData) => {
  return api.delete<unknown, Record<string, never>>(`${apiUrls.prompts.deleteTag}/${id}`, { data })
}
