import { QuestionType, DifficultyLevel } from '@/api/standardData'

/**
 * 标准问题基础接口
 */
export interface StandardQuestionBase {
  userId: number                      // 必填，用户ID
  questionText: string                // 必填，问题文本
  questionType: QuestionType          // 必填，问题类型
  difficulty: DifficultyLevel        // 必填，难度级别
  tags: string[]                     // 必填，标签列表
  commitMessage: string              // 必填，提交信息
  originalRawQuestionId?: number     // 选填，原始问题ID
}

/**
 * 标准问题创建接口
 */
export interface StandardQuestionDto extends StandardQuestionBase {
  parentStandardQuestionId?: number   // 选填，父问题ID
}

/**
 * 标准问题列表项接口
 */
export interface StandardQuestionItem {
  id: number
  userId: number
  questionText: string
  questionType: QuestionType
  difficulty: string
  tags: string[]
  parentStandardQuestionId?: number
}

/**
 * 分页信息接口
 */
export interface PageInfo {
  pageNumber: number
  pageSize: number
  sort: {
    empty: boolean
    sorted: boolean
    unsorted: boolean
  }
  offset: number
  paged: boolean
  unpaged: boolean
}

/**
 * 排序信息接口
 */
export interface SortInfo {
  empty: boolean
  sorted: boolean
  unsorted: boolean
}

/**
 * 标准问题分页响应接口
 */
export interface StandardQuestionPageResponse {
  content: StandardQuestionItem[]
  pageable: PageInfo
  totalPages: number
  last: boolean
  totalElements: number
  size: number
  number: number
  sort: SortInfo
  first: boolean
  numberOfElements: number
  empty: boolean
}

/**
 * 标签操作类型枚举
 */
export enum TagOperationType {
  ADD = 'ADD',         // 添加标签
  REMOVE = 'REMOVE',   // 移除标签
  REPLACE = 'REPLACE'  // 替换标签
}

/**
 * 标签操作请求接口
 */
export interface TagOperationRequest {
  questionId: number
  userId: number
  operationType: TagOperationType
  tags: string[]
  commitMessage: string
}

/**
 * 标签操作响应接口
 */
export interface TagOperationResponse {
  question: StandardQuestionItem
  message: string
  status: 'success' | 'error'
}

/**
 * 删除操作响应接口
 */
export interface DeleteOperationResponse {
  success: boolean
  message: string
}

/**
 * 删除标准问题响应接口
 */
export interface DeleteQuestionResponse {
  success: boolean     // 操作是否成功
  message: string      // 操作结果消息
  questionId: number   // 被删除的问题ID
  changeLogId: number  // 相关的变更日志ID
}

/**
 * 标准问题搜索结果项接口
 */
export interface SearchQuestionItem {
  id: number
  questionText: string
  questionType: string
  difficulty: string
  creationTime: string
  tags: string[]
  hasStandardAnswer: boolean
  hasCrowdsourcedAnswer: boolean
  hasExpertAnswer: boolean
}

/**
 * 标准问题搜索响应接口
 */
export interface SearchQuestionResponse {
  success: boolean
  questions: SearchQuestionItem[]
  total: number
  page: number
  size: number
  totalPages: number
}

/**
 * 原始数据响应接口
 */
export interface OriginalDataResponse {
  standardQuestion: StandardQuestionItem
  originalQuestions: {
    id: number
    content: string
    source: string
    creationTime: string
  }[]
  originalAnswers: {
    id: number
    content: string
    originalQuestionId: number
    source: string
    creationTime: string
  }[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * 无标准回答的问题响应接口
 */
export interface QuestionsWithoutAnswersResponse {
  success: boolean               // 请求是否成功
  questions: {
    id: number                   // 问题ID
    questionText: string         // 问题文本
    questionType: string         // 问题类型
    difficulty: string           // 难度级别
    creationTime: string         // 创建时间
    createdByUserId: number      // 创建用户ID
    tags: string[]               // 标签列表
    parentQuestionId?: number    // 父问题ID
  }[]
  total: number                  // 总记录数
  page: number                   // 当前页码
  size: number                   // 页大小
  totalPages: number             // 总页数
  onlyLatest: boolean            // 是否只返回叶子节点
  onlyLatestVersion: boolean     // 是否只返回最新版本
}
