/**
 * 创建专家回答的请求接口
 */
export interface CreateExpertAnswerRequest {
  standardQuestionId: string
  expertId: string
  answerText: string
  explanation?: string
  references?: string
}

/**
 * 专家候选回答数据传输对象
 * 用于POST /api/expert-candidate-answers接口
 */
export interface ExpertCandidateAnswerDTO {
  standardQuestionId: string | number
  userId: string | number
  candidateAnswerText: string
}

/**
 * 专家回答响应接口
 */
export interface ExpertAnswerResponse {
  id: number
  standardQuestionId: number
  userId: number
  userUsername: string
  candidateAnswerText: string
  submissionTime: string
  qualityScore: number
  feedback: string
  standardQuestionCategory?: string
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
 * 专家回答分页响应接口
 */
export interface ExpertAnswerPageResponse {
  content: ExpertAnswerResponse[]
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
 * 更新专家回答的请求接口
 */
export interface UpdateExpertAnswerRequest {
  userId: number
  answerText: string
}

/**
 * 删除专家回答的响应接口
 */
export interface DeleteExpertAnswerResponse {
  status: string
  message: string
}
