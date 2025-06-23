import api from './index'
import { apiUrls } from '@/config'

// API类型枚举
export enum ApiType {
  OPENAI_COMPATIBLE = 'openai_compatible',
  ANTHROPIC = 'anthropic',
  GOOGLE_AI = 'google_ai',
  BAIDU = 'baidu',
  AZURE_OPENAI = 'azure_openai'
}

// 注册模型请求数据结构
export interface RegisterModelRequest {
  userId: number     // 必填，用户ID
  apiUrl: string     // 必填，API URL
  apiKey: string     // 必填，API Key
  apiType: ApiType | string  // 必填，API类型
}

// 注册的模型结构
export interface RegisteredModel {
  id: number
  name: string
  provider: string
  apiType: string
}

// 注册模型响应数据结构
export interface RegisterModelResponse {
  success: boolean
  message: string
  registeredModels: RegisteredModel[]
}

// 模型信息结构
export interface ModelInfo {
  id: number
  name: string
  provider: string
  version?: string
  description?: string
  apiType: string
}

// 获取模型列表响应数据结构
export interface GetModelsResponse {
  models: ModelInfo[]
  total: number
  success: boolean
}

// 删除模型响应数据结构
export interface DeleteModelResponse {
  success: boolean
  message: string
}

// 模型连通性测试结果接口
export interface ModelConnectivityTestResult {
  connected: boolean
  modelName: string
  apiEndpoint: string
  modelId: number
  provider: string
  responseTime: number
  error?: string
}

// 系统级模型连通性测试结果接口
export interface SystemConnectivityTestResult {
  totalModels: number
  modelResults: ModelConnectivityTestResult[]
  failedModels: number
  success: boolean
  passedModels: number
  testDuration: number
  timestamp: number
}

// 单个模型连通性测试结果接口
export interface SingleModelConnectivityTestResult {
  connected: boolean
  modelName: string
  apiEndpoint: string
  modelId: number
  provider: string
  responseTime: number
  success: boolean
  timestamp: number
  error?: string
}



/**
 * 注册LLM模型
 * @param data 注册模型数据
 * @returns 注册结果
 */
export const registerLlmModels = (data: RegisterModelRequest) => {
  return api.post<unknown, RegisterModelResponse>(apiUrls.llmModels.register, data)
}

/**
 * 获取已注册的LLM模型列表
 * @returns 模型列表
 */
export const getRegisteredLlmModels = () => {
  return api.get<unknown, GetModelsResponse>(apiUrls.llmModels.getModels)
}

/**
 * 删除LLM模型
 * @param modelId 模型ID
 * @returns 删除结果
 */
export const deleteLlmModel = (modelId: number | string) => {
  return api.delete<unknown, DeleteModelResponse>(`${apiUrls.llmModels.base}/${modelId}`)
}

/**
 * 系统级测试所有模型连通性
 * @returns 系统级连通性测试结果
 */
export const testSystemModelConnectivity = () => {
  return api.get<unknown, SystemConnectivityTestResult>(apiUrls.answerGeneration.testSystemConnectivity)
}

/**
 * 测试单个模型连通性
 * @param modelId 模型ID
 * @returns 单个模型连通性测试结果
 */
export const testSingleModelConnectivity = (modelId: number | string) => {
  return api.get<unknown, SingleModelConnectivityTestResult>(
    `${apiUrls.answerGeneration.testModelConnectivity}/${modelId}/test-connectivity`
  )}
