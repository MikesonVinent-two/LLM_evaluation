/**
 * WebSocket 消息类型枚举
 */
export enum WebSocketMessageType {
  STATUS_CHANGE = 'STATUS_CHANGE',
  TASK_COMPLETED = 'TASK_COMPLETED',
  TASK_FAILED = 'TASK_FAILED',
  TASK_STARTED = 'TASK_STARTED',
  TASK_PAUSED = 'TASK_PAUSED',
  TASK_RESUMED = 'TASK_RESUMED',
  CONNECTION_ESTABLISHED = 'CONNECTION_ESTABLISHED',
  CONNECTION_CLOSED = 'CONNECTION_CLOSED',
  ERROR = 'ERROR'
}

/**
 * WebSocket 消息接口
 */
export interface WebSocketMessage {
  type: WebSocketMessageType
  data: {
    batchId?: number
    status?: string
    progressPercentage?: number
    completedRuns?: number
    totalRuns?: number
    pendingRuns?: number
    failedRuns?: number
    error?: string
    message?: string
    timestamp?: number
  }
  timestamp: number
}

/**
 * WebSocket 连接状态枚举
 */
export enum WebSocketConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR'
}

/**
 * WebSocket 配置接口
 */
export interface WebSocketConfig {
  url: string
  reconnectAttempts: number
  reconnectInterval: number
  heartbeatInterval: number
}

/**
 * WebSocket 状态接口
 */
export interface WebSocketState {
  isConnected: boolean
  isConnecting: boolean
  messages: WebSocketMessage[]
  lastError: Error | null
  connectionStatus: WebSocketConnectionStatus
  reconnectCount: number
}

/**
 * 批次状态更新数据类型
 */
export interface BatchStatusUpdateData {
  batchId: number
  status: string
  progressPercentage: number
  totalRuns: number
  completedRuns: number
  pendingRuns: number
  failedRuns: number
  lastActivityTime?: string
}

/**
 * 回答生成进度数据类型
 */
export interface AnswerGenerationProgressData {
  batchId: number
  progressPercentage: number
  completedRuns: number
}

// 重新导出所有类型，从新的类型文件中
export * from './websocketTypes'
