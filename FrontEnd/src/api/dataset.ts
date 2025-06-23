import api from './index'
import { apiUrls } from '@/config'
import type {
  CreateDatasetVersionRequest,
  UpdateDatasetVersionRequest,
  DeleteDatasetVersionRequest,
  CloneDatasetVersionRequest,
  GetAllDatasetVersionsParams,
  DeleteOperationResponse,
  DatasetVersionResponse,
  DatasetVersionPageResponse,
  DatasetQuestionPageResponse,
  GetStandardQuestionsByDatasetParams,
  StandardQuestionsByDatasetResponse
} from '@/types/dataset'

/**
 * 创建数据集版本
 * @param data 数据集版本数据
 * @returns 创建的数据集版本信息
 */
export const createDatasetVersion = (data: CreateDatasetVersionRequest) => {
  return api.post<DatasetVersionResponse>(
    apiUrls.dataset.createVersion,
    data
  )
}


/**
 * 更新数据集版本
 * @param versionId 版本ID
 * @param data 更新数据（包含名称、描述、要添加和移除的问题ID）
 * @returns 更新后的数据集版本信息
 */
export const updateDatasetVersion = (
  versionId: number | string,
  data: UpdateDatasetVersionRequest
) => {
  return api.put<DatasetVersionResponse>(
    `${apiUrls.dataset.updateVersion}/${versionId}`,
    data
  )
}

/**
 * 删除数据集版本
 * @param versionId 版本ID
 * @param data 删除请求数据（包含userId）
 * @returns 删除操作响应
 */
export const deleteDatasetVersion = (
  versionId: number | string,
  data: DeleteDatasetVersionRequest
) => {
  return api.delete<DeleteOperationResponse>(
    `${apiUrls.dataset.deleteVersion}/${versionId}`,
    { data }
  )
}

/**
 * 克隆数据集版本
 * @param versionId 要克隆的版本ID
 * @param data 克隆请求数据（包含新版本号、名称、描述和用户ID）
 * @returns 克隆操作响应
 */
export const cloneDatasetVersion = (
  versionId: number | string,
  data: CloneDatasetVersionRequest
) => {
  return api.post<DatasetVersionResponse>(
    `${apiUrls.dataset.cloneVersion}/${versionId}/clone`,
    data
  )
}

/**
 * 获取所有数据集版本
 * @param params 查询参数
 * @returns 数据集版本列表
 */
export const getAllDatasetVersions = (params?: GetAllDatasetVersionsParams) => {
  return api.get<DatasetVersionResponse[]>(
    apiUrls.dataset.getVersions,
    { params }
  )
}

/**
 * 获取数据集问题列表（分页）
 * @param versionId 数据集版本ID
 * @param params 分页参数
 * @returns 分页的数据集问题列表
 */
export const getDatasetVersionQuestions = (
  versionId: number | string,
  params?: {
    page?: string
    size?: string
    sort?: string
  }
) => {
  return api.get<DatasetQuestionPageResponse>(
    `${apiUrls.dataset.getVersionQuestions}/${versionId}/questions/pageable`,
    { params }
  )
}

/**
 * 获取基于数据集的标准问题
 * @param datasetId 数据集ID
 * @param params 查询参数
 * @returns 标准问题列表
 */
export const getStandardQuestionsByDataset = (
  datasetId: number | string,
  params?: GetStandardQuestionsByDatasetParams
) => {
  return api.get<StandardQuestionsByDatasetResponse>(
    `${apiUrls.dataset.getStandardQuestionsByDataset}/${datasetId}`,
    { params }
  )
}

/**
 * 获取数据集内的标准问题
 * @param datasetId 数据集ID
 * @param params 查询参数
 * @returns 数据集内的标准问题列表
 */
export const getQuestionsInDataset = (
  datasetId: number | string,
  params?: Omit<GetStandardQuestionsByDatasetParams, 'inOrOut'>
) => {
  return getStandardQuestionsByDataset(datasetId, {
    ...params,
    inOrOut: 'in'
  })
}

/**
 * 获取数据集外的标准问题
 * @param datasetId 数据集ID
 * @param params 查询参数
 * @returns 数据集外的标准问题列表
 */
export const getQuestionsOutsideDataset = (
  datasetId: number | string,
  params?: Omit<GetStandardQuestionsByDatasetParams, 'inOrOut'>
) => {
  // 查询数据集外问题时，默认只获取最新的问题（叶子节点）
  const onlyLatest = params?.onlyLatest !== undefined ? params.onlyLatest : true

  return getStandardQuestionsByDataset(datasetId, {
    ...params,
    inOrOut: 'out',
    onlyLatest: onlyLatest.toString()
  })
}

/**
 * 获取有标准答案的标准问题（数据集内或数据集外）
 * @param datasetId 数据集ID
 * @param inOrOut 'in' 表示数据集内，'out' 表示数据集外
 * @param params 其他查询参数
 * @returns 有标准答案的标准问题列表
 */
export const getQuestionsWithStandardAnswers = (
  datasetId: number | string,
  inOrOut: 'in' | 'out',
  params?: Omit<GetStandardQuestionsByDatasetParams, 'inOrOut' | 'onlyWithStandardAnswers'>
) => {
  return getStandardQuestionsByDataset(datasetId, {
    ...params,
    inOrOut,
    onlyWithStandardAnswers: 'true'
  })
}

/**
 * 获取数据集内有标准答案的问题
 * @param datasetId 数据集ID
 * @param params 查询参数
 * @returns 数据集内有标准答案的问题列表
 */
export const getQuestionsInDatasetWithStandardAnswers = (
  datasetId: number | string,
  params?: Omit<GetStandardQuestionsByDatasetParams, 'inOrOut' | 'onlyWithStandardAnswers'>
) => {
  return getQuestionsWithStandardAnswers(datasetId, 'in', params)
}

/**
 * 获取数据集外有标准答案的问题
 * @param datasetId 数据集ID
 * @param params 查询参数
 * @returns 数据集外有标准答案的问题列表
 */
export const getQuestionsOutsideDatasetWithStandardAnswers = (
  datasetId: number | string,
  params?: Omit<GetStandardQuestionsByDatasetParams, 'inOrOut' | 'onlyWithStandardAnswers'>
) => {
  // 查询数据集外问题时，默认只获取最新的问题（叶子节点）
  const onlyLatest = params?.onlyLatest !== undefined ? params.onlyLatest : true

  return getQuestionsWithStandardAnswers(datasetId, 'out', {
    ...params,
    onlyLatest: onlyLatest.toString()
  })
}
