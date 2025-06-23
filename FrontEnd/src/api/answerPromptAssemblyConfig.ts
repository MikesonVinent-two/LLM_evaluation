import api from './index'
import { apiUrls } from '@/config'

// 回答提示词组装配置创建数据结构
export interface AnswerPromptAssemblyConfigCreateData {
  name: string                   // 必填，配置名称
  description: string            // 必填，配置描述
  baseSystemPrompt: string       // 必填，基础系统提示词
  tagPromptsSectionHeader: string // 必填，标签提示词部分标题
  questionTypeSectionHeader: string // 必填，题型部分标题
  tagPromptSeparator: string     // 必填，标签提示词分隔符
  sectionSeparator: string       // 必填，部分分隔符
  finalInstruction: string       // 必填，最终指令
}

// 回答提示词组装配置返回数据结构
export interface AnswerPromptAssemblyConfigResponse {
  success: boolean
  message: string
  config: AnswerPromptAssemblyConfig
}

// 回答提示词组装配置数据结构
export interface AnswerPromptAssemblyConfig {
  id: number
  name: string
  description: string
  baseSystemPrompt: string
  tagPromptsSectionHeader: string
  questionTypeSectionHeader: string
  tagPromptSeparator: string
  sectionSeparator: string
  finalInstruction: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdByUserId: number
  createdByUsername: string
}

// 获取单个回答提示词组装配置返回数据结构
export interface GetAnswerPromptAssemblyConfigResponse {
  success: boolean
  config: AnswerPromptAssemblyConfig
}

// 获取所有活跃回答提示词组装配置的分页参数
export interface GetActiveAnswerConfigsParams {
  page?: number
  size?: number
}

// 获取所有活跃回答提示词组装配置的返回数据结构
export interface GetActiveAnswerConfigsResponse {
  configs: AnswerPromptAssemblyConfig[]
  totalItems: number
  success: boolean
  totalPages: number
  currentPage: number
}

// 获取用户创建的回答提示词组装配置的返回数据结构
export interface GetUserAnswerConfigsResponse {
  success: boolean
  configs: AnswerPromptAssemblyConfig[]
  total: number
}

/**
 * 创建回答提示词组装配置
 * @param data 回答提示词组装配置数据
 * @param userId 用户ID (可选，通过查询参数传递)
 * @returns 创建的回答提示词组装配置信息
 */
export const createAnswerPromptAssemblyConfig = (data: AnswerPromptAssemblyConfigCreateData, userId?: string | number) => {
  const queryParams = userId ? { userId } : undefined
  return api.post<unknown, AnswerPromptAssemblyConfigResponse>(
    apiUrls.promptAssembly.answerConfigs,
    data,
    { params: queryParams }
  )
}

/**
 * 获取单个回答提示词组装配置
 * @param configId 配置ID
 * @returns 回答提示词组装配置详情
 */
export const getAnswerPromptAssemblyConfig = (configId: number | string) => {
  return api.get<unknown, GetAnswerPromptAssemblyConfigResponse>(`${apiUrls.promptAssembly.getAnswerConfig}/${configId}`)
}

/**
 * 获取所有活跃的回答提示词组装配置（分页）
 * @param params 分页参数，包括页码和每页大小
 * @returns 活跃的回答提示词组装配置列表及分页信息
 */
export const getActiveAnswerPromptAssemblyConfigs = (params?: GetActiveAnswerConfigsParams) => {
  return api.get<unknown, GetActiveAnswerConfigsResponse>(
    apiUrls.promptAssembly.getActiveAnswerConfigs,
    { params }
  )
}

/**
 * 获取用户创建的回答提示词组装配置
 * @param userId 用户ID
 * @returns 用户创建的回答提示词组装配置列表
 */
export const getUserAnswerPromptAssemblyConfigs = (userId: number | string) => {
  return api.get<unknown, GetUserAnswerConfigsResponse>(`${apiUrls.promptAssembly.getUserAnswerConfigs}/${userId}`)
}
