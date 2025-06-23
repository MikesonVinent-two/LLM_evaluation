import type { UserInfo } from '@/api/user'

declare global {
  interface Window {
    userInfo?: UserInfo
    apiConfig?: {
      apiUrl: string
      apiKey: string
      apiType: string
    }
    selectedModel?: string
  }
}

export {}
