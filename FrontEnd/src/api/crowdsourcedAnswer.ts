import api from './index'
import { apiUrls } from '@/config'
import type {
  CreateCrowdsourcedAnswerRequest,
  UpdateCrowdsourcedAnswerRequest,
  CrowdsourcedAnswerResponse,
  CrowdsourcedAnswerPageResponse,
  ReviewCrowdsourcedAnswerRequest
} from '@/types/crowdsourcedAnswer'
import { QualityReviewStatus } from '@/types/crowdsourcedAnswer'

/**
 * 创建众包回答
 * @param data 众包回答数据
 * @returns 创建的众包回答信息
 */
export const createCrowdsourcedAnswer = (data: CreateCrowdsourcedAnswerRequest) => {
  return api.post<CrowdsourcedAnswerResponse>(
    apiUrls.crowdsourced.create,
    data
  )
}


/**
 * 更新众包回答
 * @param answerId 回答ID
 * @param data 更新数据（必须包含userId和answerText）
 * @returns 更新后的众包回答信息
 * @throws 如果userId不是回答的创建者，将抛出错误
 */
export const updateCrowdsourcedAnswer = (
  answerId: number | string,
  data: UpdateCrowdsourcedAnswerRequest
) => {
  return api.put<CrowdsourcedAnswerResponse>(
    `${apiUrls.crowdsourced.update}/${answerId}`,
    data
  )
}

/**
 * 删除众包回答
 * @param answerId 回答ID
 * @param userId 用户ID（必须是回答的创建者）
 * @returns 删除操作响应
 * @throws 如果userId不是回答的创建者，将抛出错误
 */
export const deleteCrowdsourcedAnswer = (
  answerId: number | string,
  userId: number | string
) => {
  return api.delete<{ success: boolean; message: string }>(
    `${apiUrls.crowdsourced.delete}/${answerId}`,
    {
      params: { userId }
    }
  )
}

/**
 * 审核众包答案
 * @param answerId 回答ID
 * @param data 审核数据
 * @returns 审核后的众包回答信息
 */
export const reviewCrowdsourcedAnswer = (
  answerId: number | string,
  data: ReviewCrowdsourcedAnswerRequest
) => {
  return api.put<CrowdsourcedAnswerResponse>(
    `${apiUrls.crowdsourced.review}/${answerId}/review`,
    data
  )
}

/**
 * 根据问题ID查询众包回答
 * @param questionId 标准问题ID
 * @param params 查询参数
 * @returns 分页的众包回答列表
 */
export const getCrowdsourcedAnswersByQuestion = (
  questionId: number | string,
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    `${apiUrls.crowdsourced.byQuestion}/${questionId}`,
    { params }
  )
}

/**
 * 根据用户ID查询众包回答
 * @param userId 用户ID
 * @param params 查询参数
 * @returns 分页的众包回答列表
 */
export const getCrowdsourcedAnswersByUser = (
  userId: number | string,
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    `${apiUrls.crowdsourced.byUser}/${userId}`,
    { params }
  )
}

/**
 * 根据审核状态查询众包回答
 * @param status 审核状态（PENDING, ACCEPTED, REJECTED, FLAGGED）
 * @param params 查询参数
 * @returns 分页的众包回答列表
 */
export const getCrowdsourcedAnswersByStatus = (
  status: QualityReviewStatus | string,
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    `${apiUrls.crowdsourced.byStatus}/${status}`,
    { params }
  )
}

/**
 * 获取所有众包回答
 * @param params 查询参数
 * @returns 分页的众包回答列表
 */
export const getAllCrowdsourcedAnswers = (
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    apiUrls.crowdsourced.all,
    { params }
  )
}

/**
 * 获取所有未审核的众包回答
 * @param params 查询参数
 * @returns 分页的未审核众包回答列表
 */
export const getPendingCrowdsourcedAnswers = (
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    apiUrls.crowdsourced.pending,
    { params }
  )
}

/**
 * 获取用户审核过的众包回答
 * @param reviewedByUserId 审核者用户ID
 * @param params 查询参数
 * @returns 分页的众包回答列表
 */
export const getReviewedCrowdsourcedAnswers = (
  reviewedByUserId: number | string,
  params?: {
    page?: string | number
    size?: string | number
    sort?: string
  }
) => {
  return api.get<CrowdsourcedAnswerPageResponse>(
    `${apiUrls.crowdsourced.reviewedBy}/${reviewedByUserId}`,
    { params }
  )
}
