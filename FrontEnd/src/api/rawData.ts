import api from './index'
import { apiUrls } from '@/config'
import type { RawQuestionPageResponse } from '@/types/rawData'

// 分页排序接口
export interface Pageable {
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  pageSize: number
  pageNumber: number
  offset: number
  paged: boolean
  unpaged: boolean
}

// 分页响应接口
export interface PageResponse<T> {
  content: T[]
  pageable: Pageable
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  size: number
  number: number
  numberOfElements: number
  empty: boolean
  sort?: {
    empty: boolean
    sorted: boolean
    unsorted: boolean
  }
}

// 搜索原始问题的响应数据项接口
export interface RawQuestionSearchItem {
  id: number
  sourceUrl: string
  sourceSite: string
  title: string
  content: string
  crawlTime: string
  tags: string[]
  standardized: boolean
  standardQuestionId: number | null
}

// 原始问题请求数据接口
export interface RawQuestionDto {
  sourceUrl: string
  sourceSite: string
  title: string
  content: string
  tags: string[]
  otherMetadata: string
}

// 原始回答请求数据接口
export interface RawAnswerDto {
  rawQuestionId: number
  authorInfo: string
  content: string
  publishTime: string
  upvotes: number
  isAccepted: boolean
  otherMetadata: string
}

// 问题及其多个回答的请求数据接口
export interface QuestionWithAnswersDto {
  question: {
    sourceUrl: string
    sourceSite: string
    title: string
    content: string
    otherMetadata: string
  }
  answers: Array<{
    authorInfo: string
    content: string
    publishTime: string
    upvotes: number
    isAccepted: boolean
    otherMetadata: string
  }>
}

// Tag接口
interface Tag {
  id: number
  tagName: string
  tagType?: string
  description?: string
  createdAt: string
  createdByUser?: {
    id: number
    username: string
    name: string
    contactInfo: string
    role: string
    createdAt: string
    updatedAt: string
  }
}

// QuestionTag接口
interface QuestionTag {
  id: number
  tag: Tag
  createdAt: string
}

// 原始问题响应数据接口
export interface RawQuestionResponse {
  id: number
  sourceUrl: string
  sourceSite: string
  title: string
  content: string
  crawlTime: string
  tags: string
  otherMetadata: string
  questionTags: QuestionTag[]
}

// 原始回答响应数据接口
export interface RawAnswerResponse {
  id: number
  rawQuestion?: {
    id: number
    sourceUrl: string
    sourceSite: string
    title: string
    content: string
    crawlTime: string
    otherMetadata: string
  }
  rawQuestionId: number
  authorInfo: string
  content: string
  publishTime: string
  upvotes: number
  isAccepted: boolean
  otherMetadata: string
}

/**
 * 录入单个原始问题
 * @param data 原始问题数据
 * @returns 录入后的问题信息
 */
export const createRawQuestion = (data: RawQuestionDto) => {
  return api.post<unknown, RawQuestionResponse>(apiUrls.rawData.createQuestion, data)
}

/**
 * 录入单个原始回答
 * @param data 原始回答数据
 * @returns 录入后的回答信息
 */
export const createRawAnswer = (data: RawAnswerDto) => {
  return api.post<unknown, RawAnswerResponse>(apiUrls.rawData.createAnswer, data)
}

/**
 * 录入一个问题及其多个回答
 * @param data 包含问题和回答列表的数据
 * @returns 操作结果
 */
export const createQuestionWithAnswers = (data: QuestionWithAnswersDto) => {
  return api.post<unknown, Record<string, never>>(apiUrls.rawData.createQuestionWithAnswers, data)
}

/**
 * 搜索原始问题
 * @param params 搜索参数
 * @returns 分页的问题列表
 */
export const searchRawQuestions = (params: {
  keyword?: string[]
  page?: number | string
  size?: number | string
  sort?: string
  tags?: string[]
  unStandardized?: boolean
}) => {
  return api.get<unknown, PageResponse<RawQuestionSearchItem>>(
    apiUrls.rawData.searchQuestions,
    { params }
  )
}

/**
 * 根据标准化状态查询原始问题
 * @param params 查询参数
 * @returns 分页的问题列表
 */
export const getRawQuestionsByStatus = (params: {
  standardized?: boolean
  page?: number
  size?: number
  sort?: string
}) => {
  return api.get<unknown, PageResponse<RawQuestionSearchItem>>(
    apiUrls.rawData.getQuestionsByStatus,
    { params }
  )
}

/**
 * 删除原始问题
 * @param questionId 问题ID
 * @returns 空对象
 */
export const deleteRawQuestion = (questionId: number | string) => {
  return api.delete<unknown, Record<string, never>>(
    `${apiUrls.rawData.deleteQuestion}/${questionId}`
  )
}

/**
 * 删除原始回答
 * @param answerId 回答ID
 * @returns 空对象
 */
export const deleteRawAnswer = (answerId: number | string) => {
  return api.delete<unknown, Record<string, never>>(
    `${apiUrls.rawData.deleteAnswer}/${answerId}`
  )
}

/**
 * 获取所有原始问题（分页）
 * @param params 分页参数
 * @returns 分页的原始问题列表
 */
export const getRawQuestions = (params?: {
  page?: string | number
  size?: string | number
  sort?: string
}) => {
  return api.get<RawQuestionPageResponse>(
    apiUrls.rawData.getQuestions,
    { params }
  )
}

/**
 * 获取原始问题的所有回答（分页）
 * @param questionId 问题ID
 * @param params 分页参数
 * @returns 分页的原始回答列表
 */
export const getQuestionAnswers = (
  questionId: number | string,
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<unknown, PageResponse<RawAnswerResponse>>(
    `${apiUrls.rawData.getQuestions}/${questionId}/answers`,
    { params }
  )
}
