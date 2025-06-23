import api from './index'
import { apiUrls } from '@/config'

// 用户角色枚举
export enum UserRole {
  ADMIN = 'ADMIN',
  CURATOR = 'CURATOR',
  EXPERT = 'EXPERT',
  ANNOTATOR = 'ANNOTATOR',
  REFEREE = 'REFEREE',
  CROWDSOURCE_USER = 'CROWDSOURCE_USER'
}

export interface LoginData {
  username: string
  password: string
}

export interface RegisterData {
  username: string // 必填，4-50个字符
  password: string // 必填，6-100个字符
  contactInfo: string // 必填，需要符合联系方式格式
  role?: UserRole // 可选，默认为"CROWDSOURCE_USER"
}

export interface UserInfo {
  id: number
  username: string
  name: string | null
  role: UserRole
  contactInfo: string
  createdAt: string
  updatedAt: string
  email?: string
  isEvaluator?: boolean    // 新增：是否是评测员
  evaluatorId?: number     // 新增：评测员ID（仅当isEvaluator为true时存在）
}

// 登录响应结构
export interface LoginResponse {
  id: number
  username: string
  name: string | null
  role: UserRole
  contactInfo: string
  isEvaluator: boolean
  evaluatorId?: number // 可选，仅当isEvaluator为true时存在
}

export interface LogoutData {
  id: number
  username: string
}

export interface LogoutResponse {
  message: string
}

/**
 * 用户更新数据接口
 */
export interface UpdateUserData {
  username: string
  password: string
  name: string
  contactInfo: string
  role: UserRole
}

/**
 * 用户搜索参数接口
 */
export interface UserSearchParams {
  page?: number | string
  size?: number | string
  sort?: string
  keyword?: string
}

/**
 * 分页信息接口
 */
export interface PageInfo {
  currentPage: number
  totalPages: number
  totalElements: number
  size: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

/**
 * 用户搜索响应接口
 */
export interface UserSearchResponse {
  success: boolean
  users: UserInfo[]
  pageInfo: PageInfo
  keyword: string
}

/**
 * 用户登录
 * @param data 登录数据
 * @returns 登录响应，包含用户基本信息和评测员相关信息
 *
 * 当用户是评测员时，返回中会包含evaluatorId字段
 * 当用户不是评测员时，返回中不包含evaluatorId字段，isEvaluator为false
 */
export const login = async (data: LoginData) => {
  // 登录并获取用户信息
  return api.post<unknown, LoginResponse>(apiUrls.auth.login, data)
}

/**
 * 用户注册
 */
export const register = (data: RegisterData) => {
  return api.post<unknown, UserInfo>(apiUrls.auth.register, data)
}

/**
 * 获取当前用户信息
 */
export const getUserInfo = async () => {
  const userJson = localStorage.getItem('user')
  if (!userJson) {
    throw new Error('未登录')
  }

  try {
    const user = JSON.parse(userJson) as LoginResponse
    return user
  } catch (e) {
    console.error('获取用户信息失败', e)
    throw new Error('获取用户信息失败')
  }
}

/**
 * 根据ID获取用户信息
 * @param userId 用户ID
 * @returns 用户详细信息
 */
export const getUserById = (userId: string | number) => {
  if (!userId) {
    throw new Error('用户ID不能为空')
  }
  // 使用profile接口获取用户信息
  return api.get<unknown, UserInfo>(`${apiUrls.user.profile}/${userId}`)
}

/**
 * 搜索用户
 * @param params 搜索参数，包括页码、每页大小、排序方式和关键词
 * @returns 用户搜索结果，包含用户列表和分页信息
 */
export const searchUsers = (params: UserSearchParams) => {
  return api.get<unknown, UserSearchResponse>(apiUrls.user.search, { params })
}

/**
 * 删除用户
 * @param userId 要删除的用户ID
 * @returns 空对象
 */
export const deleteUser = (userId: string | number) => {
  if (!userId) {
    throw new Error('用户ID不能为空')
  }
  return api.delete<unknown, Record<string, never>>(`${apiUrls.user.delete}/${userId}`)
}

/**
 * 修改用户
 * @param userId 要修改的用户ID
 * @param data 用户更新数据
 * @returns 空对象
 */
export const updateUser = (userId: string | number, data: UpdateUserData) => {
  if (!userId) {
    throw new Error('用户ID不能为空')
  }
  return api.put<unknown, Record<string, never>>(`${apiUrls.user.update}/${userId}`, data)
}

/**
 * 注销账户
 * @param userId 要注销的用户ID
 * @returns 空对象
 */
export const deactivateUser = (userId: string | number) => {
  if (!userId) {
    throw new Error('用户ID不能为空')
  }
  return api.post<unknown, Record<string, never>>(`${apiUrls.user.deactivate}/${userId}`)
}

/**
 * 更新用户信息
 * @param userId 用户ID
 * @param data 要更新的用户信息
 * @returns 更新后的用户信息
 */
export const updateUserInfo = (userId: string | number, data: Partial<UserInfo>) => {
  if (!userId) {
    throw new Error('用户ID不能为空')
  }
  return api.put<unknown, UserInfo>(`${apiUrls.user.profile}/${userId}`, data)
}

/**
 * 登出
 */
export const logout = (data: LogoutData) => {
  return api.post<unknown, LogoutResponse>(apiUrls.auth.logout, data)
}
