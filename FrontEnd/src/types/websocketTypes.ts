/**
 * WebSocket 消息类型枚举
 */
export enum WebSocketMessageType {
  // 系统消息类型
  CONNECTION_ESTABLISHED = 'CONNECTION_ESTABLISHED',
  CONNECTION_CLOSED = 'CONNECTION_CLOSED',
  ERROR = 'ERROR',

  // 业务消息类型
  PROGRESS_UPDATE = 'PROGRESS_UPDATE',
  STATUS_CHANGE = 'STATUS_CHANGE',
  TASK_STARTED = 'TASK_STARTED',
  TASK_COMPLETED = 'TASK_COMPLETED',
  TASK_PAUSED = 'TASK_PAUSED',
  TASK_RESUMED = 'TASK_RESUMED',
  TASK_FAILED = 'TASK_FAILED',
  QUESTION_STARTED = 'QUESTION_STARTED',
  QUESTION_COMPLETED = 'QUESTION_COMPLETED',
  QUESTION_FAILED = 'QUESTION_FAILED',
  NOTIFICATION = 'NOTIFICATION',
  BATCHES_STATUS_UPDATE = 'BATCHES_STATUS_UPDATE'
}

/**
 * WebSocket 消息接口
 */
export interface WebSocketMessage {
  type: WebSocketMessageType;
  payload: {
    message?: string;
    source?: string;
    subscribed?: boolean;
    batchId?: number;
    status?: BatchStatus;
    progressPercentage?: number;
    completedRuns?: number;
    pendingRuns?: number;
    failedRuns?: number;
    lastActivityTime?: string;
    oldStatus?: string;
    newStatus?: string;
    entityId?: number;
    entityType?: string;
    questionId?: number;
    completedCount?: number;
  };
  timestamp: string;
}

/**
 * WebSocket 连接状态枚举
 */
export enum WebSocketConnectionStatus {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  ERROR = 'ERROR'
}

/**
 * WebSocket 配置接口
 */
export interface WebSocketConfig {
  url?: string
  reconnectAttempts?: number
  reconnectInterval?: number
  heartbeatInterval?: number
  debug?: boolean
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

/**
 * 批次状态枚举
 */
export enum BatchStatus {
  PENDING = 'PENDING',
  GENERATING_ANSWERS = 'GENERATING_ANSWERS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  PAUSED = 'PAUSED'
}

/**
 * 批次信息接口
 */
export interface BatchInfo {
  id: number
  name: string
  status: BatchStatus
  datasetName: string
  runsCount: number
  completedRuns: number
  failedRuns: number
  pendingRuns: number
  lastActivityTime?: string
  progress?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 批次状态更新消息接口
 */
export interface BatchesStatusMessage {
  batches: BatchInfo[]
  timestamp: number
  subscribed: boolean
  message?: string
}

/**
 * 订阅主题枚举
 */
export enum WebSocketTopic {
  // 批次相关
  BATCH = '/topic/batch',  // 使用时需要拼接批次ID: ${WebSocketTopic.BATCH}/${batchId}
  BATCHES_ALL = '/topic/batches/all',

  // 运行相关
  RUN = '/topic/run',  // 使用时需要拼接运行ID: ${WebSocketTopic.RUN}/${runId}
  RUN_PROGRESS = '/topic/progress/run',  // 使用时需要拼接运行ID: ${WebSocketTopic.RUN_PROGRESS}/${runId}

  // 全局消息
  GLOBAL = '/topic/global',
  ERRORS = '/topic/errors',

  // 状态变更
  STATUS = '/topic/status',  // 使用时需要拼接实体ID: ${WebSocketTopic.STATUS}/${entityId}

  // 用户消息 - 使用时需要拼接用户ID: /user/${userId}/queue/messages
  USER_MESSAGES = '/queue/messages'  // 注意：这个会被前缀为 /user/{userId}
}

/**
 * 消息发送目的地枚举
 */
export enum WebSocketDestination {
  // 批次订阅
  BATCH_SUBSCRIBE = '/app/batch',  // 使用时需要拼接批次ID和/subscribe: ${WebSocketDestination.BATCH_SUBSCRIBE}/${batchId}/subscribe

  // 运行订阅
  RUN_SUBSCRIBE = '/app/run',  // 使用时需要拼接运行ID和/subscribe: ${WebSocketDestination.RUN_SUBSCRIBE}/${runId}/subscribe

  // 全局订阅
  GLOBAL_SUBSCRIBE = '/app/global/subscribe',

  // 所有批次订阅
  BATCHES_ALL_SUBSCRIBE = '/app/batches/all/subscribe'
}

/**
 * 进度更新消息接口
 */
export interface ProgressUpdateMessage {
  runId: number
  progress: number
  total: number
  message?: string
}

/**
 * 状态变更消息接口
 */
export interface StatusChangeMessage {
  entityId: number
  entityType: string
  oldStatus: string
  newStatus: string
  timestamp: number
}

/**
 * 任务相关消息接口
 */
export interface TaskMessage {
  taskId: number
  type: string
  status: string
  message?: string
  timestamp: number
}

/**
 * 问题相关消息接口
 */
export interface QuestionMessage {
  questionId: number
  content: string
  status: string
  result?: any
  error?: string
  timestamp: number
}

/**
 * 错误消息接口
 */
export interface ErrorMessage {
  code: string
  message: string
  details?: any
  timestamp: number
}

/**
 * 通知消息接口
 */
export interface NotificationMessage {
  title: string
  message: string
  type: 'info' | 'success' | 'warning' | 'error'
  timestamp: number
}
