/**
 * 质量审核状态枚举
 */
export enum QualityReviewStatus {
  PENDING = 'PENDING',     // 待审核
  ACCEPTED = 'ACCEPTED',   // 已接受
  REJECTED = 'REJECTED',   // 已拒绝
  FLAGGED = 'FLAGGED'      // 已标记（需要进一步审核）
}

/**
 * 创建众包回答的请求接口
 */
export interface CreateCrowdsourcedAnswerRequest {
  standardQuestionId: string
  userId: string
  answerText: string
  taskBatchId?: string
  submissionTime?: string
}

/**
 * 修改众包回答的请求接口
 */
export interface UpdateCrowdsourcedAnswerRequest {
  userId: number        // 必填，必须是回答的创建者ID
  answerText: string   // 必填，修改后的回答内容
  standardQuestionId?: number  // 可选，但不会被修改
}

/**
 * 众包回答响应接口
 */
export interface CrowdsourcedAnswerResponse {
  id: number
  standardQuestionId: number
  userId: number
  answerText: string
  submissionTime: string
  qualityReviewStatus: QualityReviewStatus
  reviewedByUserId: number | null
  reviewTime: string | null
  reviewFeedback: string | null
  userUsername: string
  reviewerUsername: string | null
  taskBatchId: string | null
}

/**
 * 分页信息接口
 */
export interface PageInfo {
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  pageNumber: number
  pageSize: number
  offset: number
  paged: boolean
  unpaged: boolean
}

/**
 * 排序信息接口
 */
export interface SortInfo {
  sorted: boolean
  unsorted: boolean
  empty: boolean
}

/**
 * 众包回答分页响应接口
 */
export interface CrowdsourcedAnswerPageResponse {
  content: CrowdsourcedAnswerResponse[]
  pageable: PageInfo
  totalPages: number
  totalElements: number
  last: boolean
  first: boolean
  size: number
  number: number
  sort: SortInfo
  numberOfElements: number
  empty: boolean
}

/**
 * 审核众包答案的请求接口
 */
export interface ReviewCrowdsourcedAnswerRequest {
  reviewerUserId: number
  status: QualityReviewStatus | string
  feedback: string
}
