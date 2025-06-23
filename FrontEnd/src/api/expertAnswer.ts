import api from './index'
import { apiUrls } from '@/config'
import type {
  ExpertCandidateAnswerDTO,
  UpdateExpertAnswerRequest,
  ExpertAnswerResponse,
  ExpertAnswerPageResponse,
  DeleteExpertAnswerResponse
} from '@/types/expertAnswer'

/**
 * 创建专家候选回答
 * POST /api/expert-candidate-answers
 * @param data 专家候选回答数据
 * @returns 创建的专家回答信息
 */
export const createExpertAnswer = (data: ExpertCandidateAnswerDTO) => {
  return api.post<ExpertAnswerResponse>(
    apiUrls.expert.create,
    data
  )
}

/**
 * 获取专家回答列表
 * @param params 查询参数
 * @returns 专家回答列表
 */
export const getExpertAnswers = (params?: {
  page?: string
  size?: string
  sort?: string
}) => {
  return api.get<ExpertAnswerPageResponse>(
    apiUrls.expert.getList,
    { params }
  )
}

/**
 * 更新专家回答
 * @param answerId 回答ID
 * @param data 更新数据（必须包含userId和answerText）
 * @returns 更新后的专家回答信息
 * @throws 如果userId不是回答的创建者，将抛出错误
 */
export const updateExpertAnswer = (
  answerId: number | string,
  data: UpdateExpertAnswerRequest
) => {
  return api.put<ExpertAnswerResponse>(
    `${apiUrls.expert.update}/${answerId}`,
    data
  )
}

/**
 * 删除专家回答
 * @param answerId 回答ID
 * @param userId 用户ID（可选，如果提供则验证是否为回答创建者）
 * @returns 删除操作响应
 */
export const deleteExpertAnswer = (
  answerId: number | string,
  userId?: number | string
) => {
  const params = userId ? { userId } : undefined

  return api.delete<DeleteExpertAnswerResponse>(
    `${apiUrls.expert.delete}/${answerId}`,
    { params }
  )
}

/**
 * 根据问题ID查询专家回答
 * @param questionId 标准问题ID
 * @param params 查询参数
 * @returns 分页的专家回答列表
 */
export const getExpertAnswersByQuestion = (
  questionId: number | string,
  params?: {
    page?: string
    size?: string
    sort?: string
  }
) => {
  return api.get<ExpertAnswerPageResponse>(
    `${apiUrls.expert.byQuestion}/${questionId}`,
    { params }
  )
}

/**
 * 根据用户ID查询专家回答
 * @param userId 用户ID
 * @param params 查询参数
 * @returns 分页的专家回答列表
 */
export const getExpertAnswersByUser = (
  userId: number | string,
  params?: {
    page?: string
    size?: string
    sort?: string
  }
) => {
  return api.get<ExpertAnswerPageResponse>(
    `${apiUrls.expert.byUser}/${userId}`,
    { params }
  )
}

/**
 * 获取所有未评分的专家回答
 * @param params 查询参数
 * @returns 分页的未评分专家回答列表
 */
export const getUnratedExpertAnswers = (params?: {
  page?: string
  size?: string
  sort?: string
}) => {
  return api.get<ExpertAnswerPageResponse>(
    apiUrls.expert.unrated,
    { params }
  )
}

/**
 * 给专家回答评分并评论
 * @param answerId 回答ID
 * @param params 评分参数
 * @param params.qualityScore 质量评分（0-100）
 * @param params.feedback 评价反馈（可选）
 * @returns 更新后的专家回答信息
 */
export const rateExpertAnswer = (
  answerId: number | string,
  params: {
    qualityScore: number | string
    feedback?: string
  }
) => {
  return api.put<ExpertAnswerResponse>(
    `${apiUrls.expert.quality}/${answerId}/quality`,
    null,
    { params }
  )
}
