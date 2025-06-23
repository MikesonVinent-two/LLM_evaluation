import api from './index'
import { apiUrls } from '@/config'

/**
 * 评测提示词组装配置请求接口
 */
export interface CreateEvaluationConfigRequest {
  name: string                    // 配置名称
  description: string             // 配置描述
  baseSystemPrompt: string        // 基础系统提示词
  tagPromptsSectionHeader: string // 标签提示词部分标题
  subjectiveSectionHeader: string // 主观题部分标题
  tagPromptSeparator: string      // 标签提示词分隔符
  sectionSeparator: string        // 各部分分隔符
  finalInstruction: string        // 最终指令
}

/**
 * 评测提示词组装配置信息接口
 */
export interface EvaluationConfigInfo {
  id: number                      // 配置ID
  name: string                    // 配置名称
  description: string             // 配置描述
  baseSystemPrompt: string        // 基础系统提示词
  tagPromptsSectionHeader: string // 标签提示词部分标题
  subjectiveSectionHeader: string // 主观题部分标题
  tagPromptSeparator: string      // 标签提示词分隔符
  sectionSeparator: string        // 各部分分隔符
  finalInstruction: string        // 最终指令
  isActive: boolean               // 是否激活
  createdAt: string               // 创建时间
  updatedAt: string               // 更新时间
  createdByUserId: number         // 创建者ID
  createdByUsername: string       // 创建者用户名
}

/**
 * 创建评测提示词组装配置响应接口
 */
export interface CreateEvaluationConfigResponse {
  success: boolean                // 操作是否成功
  message: string                 // 操作消息
  config: EvaluationConfigInfo    // 创建的配置信息
}

/**
 * 创建评测提示词组装配置
 * @param data 评测提示词组装配置数据
 * @param userId 可选的用户ID，通过查询参数传递
 * @returns 创建的评测提示词组装配置信息
 *
 * 示例请求:
 * ```json
 * {
 *   "name": "配置名称",
 *   "description": "配置描述",
 *   "baseSystemPrompt": "基础系统提示词",
 *   "tagPromptsSectionHeader": "标签提示词部分标题",
 *   "subjectiveSectionHeader": "主观题部分标题",
 *   "tagPromptSeparator": "标签提示词分隔符",
 *   "sectionSeparator": "各部分分隔符",
 *   "finalInstruction": "最终指令"
 * }
 * ```
 */
export const createEvaluationConfig = (
  data: CreateEvaluationConfigRequest,
  userId?: number | string
) => {
  const params = userId ? { userId } : undefined
  return api.post<unknown, CreateEvaluationConfigResponse>(
    apiUrls.promptAssembly.evaluationConfigs,
    data,
    { params }
  )
}

/**
 * 获取单个评测提示词组装配置响应接口
 */
export interface GetEvaluationConfigResponse {
  success: boolean
  config: EvaluationConfigInfo
}

/**
 * 获取单个评测提示词组装配置
 * @param configId 配置ID
 * @returns 评测提示词组装配置信息
 *
 * 接口路径: /api/prompt-assembly/evaluation-configs/{configId}
 * 请求方法: GET
 * 返回: 指定ID的评测提示词组装配置信息
 */
export const getEvaluationConfigById = (configId: number | string) => {
  return api.get<unknown, GetEvaluationConfigResponse>(
    `${apiUrls.promptAssembly.evaluationConfigs}/${configId}`
  )
}

/**
 * 获取所有活跃的评测提示词组装配置响应接口
 */
export interface GetAllActiveEvaluationConfigsResponse {
  success: boolean
  configs: EvaluationConfigInfo[]
  total: number
}

/**
 * 获取所有活跃的评测提示词组装配置
 * @returns 所有活跃的评测提示词组装配置列表
 *
 * 接口路径: /api/prompt-assembly/evaluation-configs
 * 请求方法: GET
 * 返回: 所有活跃的评测提示词组装配置信息列表及总数
 */
export const getAllActiveEvaluationConfigs = () => {
  return api.get<unknown, GetAllActiveEvaluationConfigsResponse>(
    apiUrls.promptAssembly.evaluationConfigs
  )
}

/**
 * 获取用户创建的评测提示词组装配置响应接口
 */
export interface GetUserEvaluationConfigsResponse {
  success: boolean
  configs: EvaluationConfigInfo[]
  total: number
}

/**
 * 获取用户创建的评测提示词组装配置
 * @param userId 用户ID
 * @returns 用户创建的评测提示词组装配置列表
 *
 * 接口路径: /api/prompt-assembly/evaluation-configs/user/{userId}
 * 请求方法: GET
 * 返回: 特定用户创建的评测提示词组装配置信息列表及总数
 */
export const getUserEvaluationConfigs = (userId: number | string) => {
  return api.get<unknown, GetUserEvaluationConfigsResponse>(
    `${apiUrls.promptAssembly.getUserEvaluationConfigs}/${userId}`
  )
}
