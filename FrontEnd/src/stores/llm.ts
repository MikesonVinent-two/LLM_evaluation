import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  ChatRequest,
  ChatResponse,
  ModelInfo,
  GetModelsRequest,
} from '@/api/llm'
import {
  createDefaultChatRequest,
  createDefaultModelsRequest,
  sendChatRequest,
  getAvailableModels,
} from '@/api/llm'

export const useLLMStore = defineStore('llm', () => {
  const isLoading = ref(false)
  const currentResponse = ref<ChatResponse | null>(null)
  const error = ref<string | null>(null)
  const availableModels = ref<ModelInfo[]>([])
  const isLoadingModels = ref(false)

  // 获取可用模型列表
  const fetchModels = async (apiUrl: string, apiKey: string, apiType: string = 'openai_compatible') => {
    try {
      isLoadingModels.value = true
      error.value = null

      // 直接构造请求配置，使用传入的 apiUrl 和 apiType
      const modelsRequest: GetModelsRequest = {
        apiUrl: apiUrl,
        apiKey: apiKey,
        api: apiType,
      }

      // 发送请求
      const models = await getAvailableModels(modelsRequest)
      availableModels.value = models
      return models
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取模型列表时出错'
      throw e
    } finally {
      isLoadingModels.value = false
    }
  }

  // 发送聊天消息
  const sendMessage = async (message: string, apiKey: string, options?: Partial<ChatRequest>) => {
    try {
      isLoading.value = true
      error.value = null

      // 创建请求配置
      const chatRequest = createDefaultChatRequest(message)
      chatRequest.apiKey = apiKey

      // 合并自定义选项
      if (options) {
        Object.assign(chatRequest, options)
      }

      // 发送请求
      const response = await sendChatRequest(chatRequest)
      currentResponse.value = response
      return response
    } catch (e) {
      error.value = e instanceof Error ? e.message : '发送消息时出错'
      throw e
    } finally {
      isLoading.value = false
    }
  }

  // 清除当前响应
  const clearResponse = () => {
    currentResponse.value = null
    error.value = null
  }

  // 清除模型列表
  const clearModels = () => {
    availableModels.value = []
  }

  return {
    isLoading,
    isLoadingModels,
    currentResponse,
    availableModels,
    error,
    sendMessage,
    fetchModels,
    clearResponse,
    clearModels,
  }
})
