/**
 * 创建数据集版本的请求接口
 */
export interface CreateDatasetVersionRequest {
  versionNumber: string
  name: string
  description: string
  standardQuestionIds: number[]
  userId: number
}

/**
 * 数据集版本响应接口
 */
export interface DatasetVersionResponse {
  id: number
  versionNumber: string
  name: string
  description: string
  creationTime: string
  createdByUserId: number
  createdByUserName: string
  questionCount: number
  hasStandardAnswer?: boolean
  hasExpertAnswer?: boolean
  hasCrowdsourcedAnswer?: boolean
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
 * 数据集版本分页响应接口
 */
export interface DatasetVersionPageResponse {
  content: DatasetVersionResponse[]
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
 * 更新数据集版本的请求接口
 */
export interface UpdateDatasetVersionRequest {
  name: string
  description: string
  standardQuestionsToAdd: number[]
  standardQuestionsToRemove: number[]
  userId: number
}

/**
 * 删除数据集版本的请求接口
 */
export interface DeleteDatasetVersionRequest {
  userId: number
}

/**
 * 删除操作响应接口
 */
export interface DeleteOperationResponse {
  success: boolean
  message: string
}

/**
 * 克隆数据集版本的请求接口
 */
export interface CloneDatasetVersionRequest {
  newVersionNumber: string
  newName: string
  description: string
  userId: number
}

/**
 * 获取所有数据集版本的查询参数
 */
export interface GetAllDatasetVersionsParams {
  name?: string
}

/**
 * 数据集问题项接口
 */
export interface DatasetQuestionItem {
  id: number
  datasetVersionId: number
  datasetVersionName: string
  standardQuestionId: number
  standardQuestionText: string
  orderInDataset: number
}

/**
 * 数据集问题分页响应接口
 */
export interface DatasetQuestionPageResponse {
  content: DatasetQuestionItem[]
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
 * 基于数据集查询标准问题的参数接口
 */
export interface GetStandardQuestionsByDatasetParams {
  inOrOut?: string          // 'in' 或 'out'，表示在数据集内或数据集外的问题
  onlyLatest?: string | boolean  // 是否只返回叶子节点的标准问题
  onlyLatestVersion?: string | boolean  // 是否只返回最新版本的标准问题
  tags?: string             // 标签，多个用逗号分隔
  keyword?: string          // 搜索关键词
  pageNumber?: string | number  // 页码，从0开始
  pageSize?: string | number    // 每页大小
  onlyWithStandardAnswers?: string | boolean  // 是否只返回有标准答案的问题
}

/**
 * 基于数据集查询标准问题的响应接口
 */
export interface StandardQuestionsByDatasetResponse {
  success: boolean
  questions: {
    id: number
    questionText: string
    questionType: string
    difficulty: string
    creationTime: string
    createdByUserId: number
    tags: string[]
    parentQuestionId?: number
    hasStandardAnswer: boolean  // 是否有标准答案
  }[]
  total: number
  page: number
  size: number
  totalPages: number
  datasetId: number
  isInDataset: boolean
  onlyLatest: boolean
  onlyLatestVersion: boolean
  onlyWithStandardAnswers: boolean  // 是否只返回有标准答案的问题
}
