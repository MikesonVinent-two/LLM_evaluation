import api from './index'
import { apiUrls } from '@/config'
import type {
  StandardQuestionPageResponse,
  StandardQuestionBase,
  TagOperationRequest,
  TagOperationResponse,
  DeleteOperationResponse,
  SearchQuestionResponse,
  OriginalDataResponse,
  QuestionsWithoutAnswersResponse,
  DeleteQuestionResponse
} from '@/types/standardQuestion'
import { TagOperationType } from '@/types/standardQuestion'

/**
 * 问题类型枚举
 */
export enum QuestionType {
  SINGLE_CHOICE = 'SINGLE_CHOICE',   // 单选题
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE', // 多选题
  SIMPLE_FACT = 'SIMPLE_FACT',       // 简单事实题
  SUBJECTIVE = 'SUBJECTIVE',         // 主观题
}

/**
 * 难度级别枚举
 */
export enum DifficultyLevel {
  EASY = 'EASY',     // 简单
  MEDIUM = 'MEDIUM', // 中等
  HARD = 'HARD',     // 困难
}

/**
 * 变更类型枚举
 */
export enum ChangeType {
  ADD = 'ADD',         // 新增
  MODIFY = 'MODIFY',   // 修改
  DELETE = 'DELETE',   // 删除
}

/**
 * 用户角色枚举
 */
export enum UserRole {
  ADMIN = 'ADMIN',     // 管理员
  EXPERT = 'EXPERT',   // 专家
  USER = 'USER',       // 普通用户
}

/**
 * 用户信息接口
 */
export interface UserInfo {
  id: number           // 用户ID
  username: string     // 用户名
  name: string        // 姓名
  role: UserRole      // 角色
  contactInfo: string // 联系信息
}

/**
 * 版本变更记录接口
 */
export interface VersionChange {
  field: string       // 变更字段
  oldValue: string    // 旧值
  newValue: string    // 新值
}

/**
 * 问题版本节点接口
 */
export interface QuestionVersionNode {
  id: number                    // 版本ID
  questionText: string          // 问题文本
  creationTime: string         // 创建时间
  createdByUser: UserInfo      // 创建用户信息
  commitMessage: string        // 提交说明
  tags: string[]               // 标签列表
  changes?: VersionChange[]    // 变更记录
  children?: QuestionVersionNode[] // 子版本
}

/**
 * 变更记录接口
 */
export interface ChangeRecord {
  attributeName: string     // 属性名称
  oldValue: string         // 旧值
  newValue: string         // 新值
  changeType: ChangeType   // 变更类型
}

/**
 * 问题历史版本接口
 */
export interface QuestionHistoryVersion {
  id: number                      // 版本ID
  questionText: string            // 问题文本
  questionType: QuestionType      // 问题类型
  difficulty: DifficultyLevel     // 难度级别
  creationTime: string           // 创建时间
  createdByUserId: number        // 创建用户ID
  createdByUsername: string      // 创建用户名
  parentQuestionId?: number      // 父问题ID
  tags: string[]                 // 标签列表
  commitMessage: string          // 提交说明
  changes: ChangeRecord[]        // 变更记录
  changeLogId: number           // 变更日志ID
}

/**
 * 选项接口
 */
export interface Option {
  id: number | string
  text: string
}

/**
 * 标准答案基础接口
 */
interface StandardAnswerBase {
  standardQuestionId: number    // 必填，标准问题ID
  userId: number               // 必填，用户ID
  questionType: QuestionType   // 必填，问题类型
  answerText: string          // 必填，答案文本/解析
  commitMessage?: string      // 选填，提交说明
}

/**
 * 选择题答案接口（单选和多选）
 */
export interface ChoiceAnswerDto extends StandardAnswerBase {
  questionType: QuestionType.SINGLE_CHOICE | QuestionType.MULTIPLE_CHOICE
  options: Option[]           // 必填，选项列表
  correctIds: (number | string)[]  // 必填，正确答案ID列表
}

/**
 * 简单事实题答案接口
 */
export interface SimpleFactAnswerDto extends StandardAnswerBase {
  questionType: QuestionType.SIMPLE_FACT
  alternativeAnswers?: string[]  // 选填，可接受的同义词或变体答案列表
}

/**
 * 主观题答案接口
 */
export interface SubjectiveAnswerDto extends StandardAnswerBase {
  questionType: QuestionType.SUBJECTIVE
  scoringGuidance: string    // 必填，评分指导
}

/**
 * 创建标准问题的请求数据接口
 */
export interface StandardQuestionDto extends StandardQuestionBase {
  parentStandardQuestionId?: number   // 选填，父问题ID
}

/**
 * 修改标准问题的请求数据接口
 */
export interface UpdateStandardQuestionDto extends StandardQuestionBase {
  // 添加一个可选字段以避免空接口警告
  updatedAt?: string
}

/**
 * 标准问题响应数据接口
 */
export interface StandardQuestionResponse {
  id: number                  // 问题ID
  difficulty: DifficultyLevel // 难度级别
  createdByUserId: number    // 创建用户ID
  message: string            // 响应消息
  questionType: QuestionType // 问题类型
  questionText: string       // 问题文本
  tags: string[]            // 标签列表
}

/**
 * 创建标准问题
 * @param data 标准问题数据
 * @returns 创建的标准问题信息
 */
export const createStandardQuestion = (data: StandardQuestionDto) => {
  return api.post<StandardQuestionResponse>(
    apiUrls.standardData.createQuestion,
    data
  )
}

/**
 * 修改标准问题
 * @param questionId 问题ID
 * @param data 更新的问题数据
 * @returns 操作结果
 */
export const updateStandardQuestion = (
  questionId: number | string,
  data: UpdateStandardQuestionDto
) => {
  return api.put<Record<string, never>>(
    `${apiUrls.standardData.updateQuestion}/${questionId}`,
    data
  )
}

/**
 * 获取标准问题的历史版本
 * @param questionId 问题ID
 * @returns 历史版本列表
 */
export const getQuestionHistory = (questionId: number | string) => {
  return api.get<QuestionHistoryVersion[]>(
    `${apiUrls.standardData.getQuestionHistory}/${questionId}/history`
  )
}

/**
 * 回退标准问题到指定版本
 * @param versionId 要回退到的版本ID
 * @param data 回退操作参数
 * @returns 回退后的问题信息
 */
export const rollbackQuestionVersion = (
  versionId: number | string,
  data: {
    userId: number
    commitMessage: string
  }
) => {
  return api.post<{
    id: number
    questionText: string
    questionType: QuestionType
    difficulty: DifficultyLevel
    userId: number
    tags: string[]
  }>(
    `${apiUrls.standardData.rollbackQuestion}/${versionId}/rollback`,
    data
  )
}

/**
 * 获取标准问题的版本树结构
 * @param questionId 问题ID
 * @returns 版本树结构
 */
export const getQuestionVersionTree = (questionId: number | string) => {
  return api.get<QuestionVersionNode>(
    `${apiUrls.standardData.getQuestionVersionTree}/${questionId}/version-tree`
  )
}

/**
 * 创建标准答案
 * @param data 标准答案数据
 * @returns 创建结果
 */
export const createStandardAnswer = (
  data: ChoiceAnswerDto | SimpleFactAnswerDto | SubjectiveAnswerDto
) => {
  // 根据题型处理数据
  const requestData = {
    ...data,
    // 处理需要转换为JSON字符串的字段
    options: data.questionType === QuestionType.SINGLE_CHOICE ||
            data.questionType === QuestionType.MULTIPLE_CHOICE
      ? JSON.stringify((data as ChoiceAnswerDto).options)
      : undefined,
    correctIds: data.questionType === QuestionType.SINGLE_CHOICE ||
               data.questionType === QuestionType.MULTIPLE_CHOICE
      ? JSON.stringify((data as ChoiceAnswerDto).correctIds)
      : undefined,
    alternativeAnswers: data.questionType === QuestionType.SIMPLE_FACT
      ? JSON.stringify((data as SimpleFactAnswerDto).alternativeAnswers || [])
      : undefined,
  }

  return api.post<Record<string, never>>(
    apiUrls.standardData.createAnswer,
    requestData
  )
}

/**
 * 修改标准答案
 * @param standardQuestionId 标准问题ID
 * @param data 标准答案数据
 * @returns 修改结果
 */
export const updateStandardAnswer = (
  standardQuestionId: number | string,
  data: ChoiceAnswerDto | SimpleFactAnswerDto | SubjectiveAnswerDto
) => {
  // 根据题型处理数据
  const requestData = {
    ...data,
    // 处理需要转换为JSON字符串的字段
    options: data.questionType === QuestionType.SINGLE_CHOICE ||
            data.questionType === QuestionType.MULTIPLE_CHOICE
      ? JSON.stringify((data as ChoiceAnswerDto).options)
      : undefined,
    correctIds: data.questionType === QuestionType.SINGLE_CHOICE ||
               data.questionType === QuestionType.MULTIPLE_CHOICE
      ? JSON.stringify((data as ChoiceAnswerDto).correctIds)
      : undefined,
    alternativeAnswers: data.questionType === QuestionType.SIMPLE_FACT
      ? JSON.stringify((data as SimpleFactAnswerDto).alternativeAnswers || [])
      : undefined,
  }

  return api.put<Record<string, never>>(
    `${apiUrls.standardData.updateAnswer}/${standardQuestionId}`,
    requestData
  )
}

/**
 * 获取标准问题列表
 * @param params 查询参数
 * @returns 分页的标准问题列表
 */
export const getStandardQuestions = (params: {
  userId?: string
  page?: string
  size?: string
  sort?: string
}) => {
  return api.get<StandardQuestionPageResponse>(
    apiUrls.standardData.getQuestions,
    { params }
  )
}

/**
 * 获取最新版本的标准问题列表
 * @param params 查询参数
 * @returns 分页的最新版本标准问题列表
 */
export const getLatestStandardQuestions = (params: {
  page?: string
  size?: string
  sort?: string[]
}) => {
  return api.get<StandardQuestionPageResponse>(
    apiUrls.standardData.getLatestQuestions,
    {
      params: {
        ...params,
        sort: params.sort?.join(',')
      }
    }
  )
}

/**
 * 添加标准问题标签
 * @param data 标签操作请求数据
 * @returns 操作响应
 */
export const addQuestionTags = (data: Omit<TagOperationRequest, 'operationType'>) => {
  return api.post<unknown, TagOperationResponse>(
    apiUrls.standardData.addTags,
    {
      ...data,
      operationType: TagOperationType.ADD
    }
  )
}

/**
 * 移除标准问题标签
 * @param data 标签操作请求数据
 * @returns 操作响应
 */
export const removeQuestionTags = (data: Omit<TagOperationRequest, 'operationType'>) => {
  return api.post<unknown, TagOperationResponse>(
    apiUrls.standardData.removeTags,
    {
      ...data,
      operationType: TagOperationType.REMOVE
    }
  )
}

/**
 * 替换标准问题标签
 * @param data 标签操作请求数据
 * @returns 操作响应
 */
export const replaceQuestionTags = (data: Omit<TagOperationRequest, 'operationType'>) => {
  return api.post<unknown, TagOperationResponse>(
    apiUrls.standardData.replaceTags,
    {
      ...data,
      operationType: TagOperationType.REPLACE
    }
  )
}

/**
 * 统一的标签操作方法
 * @param data 标签操作请求数据
 * @returns 操作响应
 */
export const updateQuestionTags = (data: TagOperationRequest) => {
  return api.post<unknown, TagOperationResponse>(
    apiUrls.standardData.updateTags,
    data
  )
}

/**
 * 删除标准答案
 * @param id 标准答案ID
 * @returns 删除操作响应
 */
export const deleteStandardAnswer = (id: string | number) => {
  return api.delete<DeleteOperationResponse>(
    `${apiUrls.standardData.deleteAnswer}/${id}`
  )
}

/**
 * 搜索标准问题
 * @param params 搜索参数
 * @returns 搜索结果
 */
export const searchStandardQuestions = (params: {
  tags?: string
  keyword?: string
  userId?: string
  page?: string
  size?: string
  onlyLatest?: boolean | string // 是否只返回叶子节点的标准问题
  onlyWithStandardAnswers?: boolean | string // 是否只返回有标准答案的问题
}) => {
  return api.get<SearchQuestionResponse>(
    apiUrls.standardData.searchQuestions,
    { params }
  )
}

/**
 * 获取标准问题对应的原始问题和原始回答列表
 * @param questionId 标准问题ID
 * @param params 分页参数
 * @returns 原始数据响应
 */
export const getQuestionOriginalData = (
  questionId: number | string,
  params?: {
    page?: string
    size?: string
  }
) => {
  return api.get<OriginalDataResponse>(
    `${apiUrls.standardData.getOriginalData}/${questionId}/original-data`,
    { params }
  )
}

/**
 * 获取所有没有标准答案的标准问题
 * @param params 分页参数
 * @returns 没有标准回答的标准问题列表
 */
export const getQuestionsWithoutAnswers = (params?: {
  page?: string
  size?: string
  onlyLatest?: boolean | string // 是否只返回叶子节点的标准问题
}) => {
  return api.get<QuestionsWithoutAnswersResponse>(
    apiUrls.standardData.getQuestionsWithoutAnswers,
    { params }
  )
}

/**
 * 获取所有没有标准答案的标准问题
 * @param params 查询参数
 * @returns 没有标准回答的标准问题列表
 */
export const getQuestionsWithoutStandardAnswers = (params?: {
  pageNumber?: string | number  // 页码，从0开始
  pageSize?: string | number    // 每页大小
  onlyLatest?: boolean | string  // 是否只返回叶子节点的标准问题
  onlyLatestVersion?: boolean | string // 是否只返回最新版本的标准问题
}) => {
  return api.get<QuestionsWithoutAnswersResponse>(
    apiUrls.standardData.getQuestionsWithoutAnswers,
    { params }
  )
}

/**
 * 删除标准问题
 * @param questionId 问题ID
 * @param params 删除操作参数
 * @returns 删除操作响应
 */
export const deleteStandardQuestion = (
  questionId: number | string,
  params?: {
    userId?: number | string  // 执行删除操作的用户ID
    permanent?: boolean | string  // 是否永久删除，默认为false（软删除）
  }
) => {
  return api.delete<DeleteQuestionResponse>(
    `${apiUrls.standardData.deleteQuestion}/${questionId}`,
    { params }
  )
}

// 原始问题相关接口
export const getRawQuestions = (params: {
  page?: string
  size?: string
  sort?: string
  status?: string
}) => {
  return api.get(apiUrls.rawData.getQuestions, { params })
}

export const getRawQuestionsByStatus = (params: {
  page?: string
  size?: string
  sort?: string
  status?: string
}) => {
  return api.get(apiUrls.rawData.getQuestionsByStatus, { params })
}
