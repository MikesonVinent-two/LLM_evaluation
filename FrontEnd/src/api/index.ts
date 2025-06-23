import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { appConfig } from '@/config'

// 扩展AxiosRequestConfig类型
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    timestamp?: number
  }
}

// 创建axios实例
const api = axios.create({
  baseURL: appConfig.api.baseUrl, // 使用配置文件中的baseUrl
  timeout: appConfig.api.timeout, // 使用配置文件中的timeout
  headers: {
    'Content-Type': appConfig.api.contentType,
  },
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 添加时间戳用于计算请求持续时间
    config.timestamp = Date.now()

    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 打印请求信息
    console.log('🚀 发送请求:', {
      url: config.url,
      method: config.method?.toUpperCase(),
      headers: config.headers,
      params: config.params,
      data: config.data,
      timestamp: new Date().toLocaleString()
    })

    return config
  },
  (error) => {
    // 对请求错误做些什么
    console.error('❌ 请求配置错误:', error)
    return Promise.reject(error)
  },
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 打印响应信息
    console.log('✅ 收到响应:', {
      url: response.config.url,
      method: response.config.method?.toUpperCase(),
      status: response.status,
      statusText: response.statusText,
      data: response.data,
      timestamp: new Date().toLocaleString(),
      duration: `${Date.now() - (response.config.timestamp || 0)}ms`
    })

    // 对响应数据做点什么
    return response.data
  },
  (error) => {
    // 对响应错误做点什么
    if (error.response) {
      // 请求已发出，服务器返回状态码不在 2xx 范围内
      console.error('❌ 请求失败:', {
        url: error.config.url,
        method: error.config.method?.toUpperCase(),
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
        timestamp: new Date().toLocaleString(),
        duration: `${Date.now() - (error.config.timestamp || 0)}ms`
      })

      // 处理401未授权错误
      if (error.response.status === 401) {
        console.warn('⚠️ 用户未授权，清除token')
        localStorage.removeItem('token')
        // 可以在这里添加重定向到登录页的逻辑
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      console.error('❌ 网络错误:', {
        url: error.config.url,
        method: error.config.method?.toUpperCase(),
        error: '未收到响应',
        timestamp: new Date().toLocaleString()
      })
    } else {
      // 请求配置出错
      console.error('❌ 请求配置错误:', {
        error: error.message,
        timestamp: new Date().toLocaleString()
      })
    }
    return Promise.reject(error)
  },
)

export default api
