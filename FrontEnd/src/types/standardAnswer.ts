import { QuestionType } from '@/api/standardData'

/**
 * 标准回答版本节点接口
 */
export interface AnswerVersionNode {
  id: number
  commitMessage: string
  commitTime: string
  userId: number
  userName: string
  children: AnswerVersionNode[]
}

/**
 * 标准回答历史版本接口
 */
export interface AnswerHistoryVersion {
  id: number
  commitMessage: string
  commitTime: string
  userId: number
  userName: string
  details: AnswerChangeDetail[]
}

/**
 * 答案变更详情接口
 */
export interface AnswerChangeDetail {
  field: string
  oldValue: string | null
  newValue: string
}

/**
 * 版本信息接口
 */
export interface VersionInfo {
  id: number
  commitMessage: string
  commitTime: string
  userId: number
  userName: string
}

/**
 * 字段差异接口
 */
export interface FieldDiff {
  field: string
  baseValue: string
  compareValue: string
  changed: boolean
}

/**
 * 标准回答比较结果接口
 */
export interface AnswerCompareResult {
  baseVersion: VersionInfo
  compareVersion: VersionInfo
  fieldDiffs: FieldDiff[]
}

/**
 * 标准回答版本树响应接口
 */
export type AnswerVersionTreeResponse = AnswerVersionNode

/**
 * 标准回答回滚请求接口
 */
export interface AnswerRollbackRequest {
  userId: number
  commitMessage?: string
}

/**
 * 用户基本信息
 */
export interface UserBasicInfo {
  id: number
  username: string
}

/**
 * 标准回答回滚通用字段
 */
interface AnswerRollbackCommon {
  id: number
  determinedTime: string
  determinedByUser: UserBasicInfo
}

/**
 * 客观题回答回滚响应接口
 */
export interface ObjectiveAnswerRollbackResponse extends AnswerRollbackCommon {
  options: string
  correctIds: string
}

/**
 * 简答题回答回滚响应接口
 */
export interface ShortAnswerRollbackResponse extends AnswerRollbackCommon {
  answerText: string
  alternativeAnswers: string
}

/**
 * 主观题回答回滚响应接口
 */
export interface SubjectiveAnswerRollbackResponse extends AnswerRollbackCommon {
  answerText: string
  scoringGuidance: string
}

/**
 * 标准回答回滚响应接口（联合类型）
 */
export type AnswerRollbackResponse =
  | ObjectiveAnswerRollbackResponse
  | ShortAnswerRollbackResponse
  | SubjectiveAnswerRollbackResponse
