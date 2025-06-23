import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getUserInfo, getUserById, login, logout as apiLogout, register as apiRegister } from '@/api/user'
import type { UserInfo, LoginData, LogoutData, RegisterData, LoginResponse } from '@/api/user'
import { useRouter } from 'vue-router'

// 用户信息缓存
let userInfoCache: Record<number, UserInfo> = {}

export const useUserStore = defineStore('user', () => {
  const router = useRouter()
  const currentUser = ref<LoginResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 获取缓存的用户信息
  const getCachedUserInfo = (userId: number): UserInfo | null => {
    return userInfoCache[userId] || null
  }

  // 更新用户信息缓存
  const updateUserInfoCache = (userInfo: UserInfo) => {
    if (userInfo && userInfo.id) {
      userInfoCache[userInfo.id] = userInfo
    }
  }

  // 清除用户信息缓存
  const clearUserInfoCache = () => {
    userInfoCache = {}
  }

  const isLoggedIn = computed(() => {
    const hasLocalStorage = !!localStorage.getItem('user')
    const hasCurrentUser = !!currentUser.value
    return hasLocalStorage && hasCurrentUser
  })

  // 从localStorage加载用户信息
  function loadUserFromStorage() {
    const userJson = localStorage.getItem('user')
    if (userJson) {
      try {
        currentUser.value = JSON.parse(userJson)
      } catch (err) {
        console.error('解析存储的用户数据失败', err)
        localStorage.removeItem('user')
        currentUser.value = null
      }
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    // 首先尝试从本地存储加载
    loadUserFromStorage()

    // 如果本地存储中有用户信息，直接使用
    if (currentUser.value) {
      return
    }

    // 如果没有本地存储，则不自动获取默认用户
    error.value = '未登录'
    currentUser.value = null
    return null
  }

  // 根据ID获取用户信息
  async function getUserInfoById(userId: string | number) {
    const numericId = Number(userId)

    // 先检查缓存
    const cachedInfo = getCachedUserInfo(numericId)
    if (cachedInfo) {
      return cachedInfo
    }

    try {
      loading.value = true
      error.value = null
      const userInfo = await getUserById(userId)

      // 更新缓存
      if (userInfo) {
        updateUserInfoCache(userInfo)
      }

      return userInfo
    } catch (err: Error | unknown) {
      error.value = err instanceof Error ? err.message : `获取用户ID=${userId}的信息失败`
      return null
    } finally {
      loading.value = false
    }
  }

  // 用户登录
  async function loginUser(loginData: LoginData) {
    try {
      loading.value = true
      error.value = null

      // 登录并获取完整用户信息
      const userInfo = await login(loginData)

      // 更新状态和本地存储
      currentUser.value = userInfo
      localStorage.setItem('user', JSON.stringify(userInfo))

      // 不再设置token，仅依赖user数据判断登录状态
      console.log('✅ 用户登录成功:', userInfo.username)

      // 触发登录成功事件
      window.dispatchEvent(new CustomEvent('user-login', {
        detail: userInfo
      }))

      return true
    } catch (err: Error | unknown) {
      error.value = err instanceof Error ? err.message : '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }

  // 用户注册
  async function register(registerData: RegisterData) {
    try {
      loading.value = true
      error.value = null
      await apiRegister(registerData)
      return true
    } catch (err: Error | unknown) {
      error.value = err instanceof Error ? err.message : '注册失败'
      return false
    } finally {
      loading.value = false
    }
  }

  // 用户登出
  async function logout() {
    try {
      loading.value = true
      error.value = null

      let logoutData: LogoutData | null = null
      if (currentUser.value) {
        logoutData = {
          id: currentUser.value.id,
          username: currentUser.value.username,
        }
      }

      // 清除状态和存储
      localStorage.removeItem('user')
      currentUser.value = null
      error.value = null
      clearUserInfoCache()

      if (logoutData) {
        try {
          await apiLogout(logoutData)
        } catch (e) {
          console.error('登出API调用失败，但本地状态已清除', e)
        }
      }

      router.push('/login')
    } catch (err: Error | unknown) {
      error.value = err instanceof Error ? err.message : '登出失败'
      localStorage.removeItem('user')
      currentUser.value = null
      clearUserInfoCache()
    } finally {
      loading.value = false
    }
  }

  // 从localStorage初始化用户信息
  function initializeFromStorage() {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const userInfo = JSON.parse(storedUser)
        currentUser.value = userInfo
      } catch (e) {
        console.error('Failed to parse stored user data:', e)
        localStorage.removeItem('user')
      }
    }
  }

  // 获取当前用户信息
  function getCurrentUser() {
    // 首先检查store中是否有用户信息
    if (currentUser.value) {
      return currentUser.value
    }

    // 尝试从localStorage加载
    const userJson = localStorage.getItem('user')
    if (userJson) {
      try {
        const userData = JSON.parse(userJson)
        // 顺便更新store中的用户信息
        currentUser.value = userData
        return userData
      } catch (err) {
        console.error('解析存储的用户数据失败', err)
        localStorage.removeItem('user')
      }
    }

    return null
  }

  return {
    currentUser,
    loading,
    error,
    isLoggedIn,
    fetchUserInfo,
    getUserInfoById,
    loginUser,
    register,
    logout,
    initializeFromStorage,
    getCachedUserInfo,
    updateUserInfoCache,
    clearUserInfoCache,
    getCurrentUser,
  }
})
