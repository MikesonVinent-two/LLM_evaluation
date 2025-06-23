<template>
  <div class="navbar-container">
    <el-menu
      :default-active="activeIndex"
      class="navbar"
      mode="horizontal"
      :ellipsis="false"
      @select="handleSelect"
    >
      <!-- 左侧 Logo 和标题 -->
      <div class="navbar-left">
        <div class="logo-container" @click="router.push('/')" role="button">
          <img src="@/assets/llm-logo.svg" alt="LLM评测系统" class="logo" />
          <span class="title">LLM评测系统</span>
        </div>
      </div>

      <!-- 右侧功能区 -->
      <div class="navbar-right">
        <template v-if="isLoggedIn && currentUser">
          <!-- 对话按钮 -->
          <el-menu-item index="chat" class="nav-item">
            <el-tooltip content="开始对话" placement="bottom" :show-after="500">
              <div class="nav-button">
                <span class="nav-text">对话</span>
              </div>
            </el-tooltip>
          </el-menu-item>

          <!-- 模型配置按钮 -->
          <el-menu-item index="model-settings" class="nav-item" @click="handleModelConfig">
            <el-tooltip content="配置AI模型" placement="bottom" :show-after="500">
              <div class="nav-button">
                <span class="nav-text">配置</span>
              </div>
            </el-tooltip>
          </el-menu-item>

          <!-- 用户头像 -->
          <el-dropdown trigger="click" class="user-dropdown">
            <div class="user-avatar">
              <el-avatar :size="32" :src="currentUser.avatar">
                {{ currentUser.username?.charAt(0).toUpperCase() }}
              </el-avatar>
            </div>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <div class="user-info-header">
                    <el-avatar :size="48" :src="currentUser.avatar">
                      {{ currentUser.username?.charAt(0).toUpperCase() }}
                    </el-avatar>
                    <div class="user-info-main">
                      <span class="user-info-name">{{ currentUser.username }}</span>
                      <span class="user-info-role">{{ getRoleName(currentUser.role) }}</span>
                    </div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <div class="user-info-details">
                    <div class="info-item">
                      <span class="info-label">用户名：</span>
                      <span class="info-value">{{ currentUser.username }}</span>
                    </div>
                    <div class="info-item" v-if="currentUser.name">
                      <span class="info-label">姓名：</span>
                      <span class="info-value">{{ currentUser.name }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">联系方式：</span>
                      <span class="info-value">{{ currentUser.contactInfo }}</span>
                    </div>
                    <div class="info-item" v-if="currentUser.isEvaluator">
                      <span class="info-label">评测员ID：</span>
                      <span class="info-value">{{ currentUser.evaluatorId || '-' }}</span>
                    </div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided @click="router.push('/profile')">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-menu-item index="login" class="auth-item">
            <el-icon><User /></el-icon>
            登录
          </el-menu-item>
          <el-menu-item index="register" class="auth-item">
            <el-icon><Plus /></el-icon>
            注册
          </el-menu-item>
        </template>
      </div>
    </el-menu>

    <!-- 模型选择器组件 - 放在导航栏容器内，但设置为隐藏 -->
    <div class="model-selector-container">
      <ModelSelector ref="modelSelectorRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { UserRole } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'
import {
  User,
  UserFilled,
  Setting,
  SwitchButton,
  ChatDotRound,
  List,
  Plus,
  Message,
  Calendar,
  ArrowDown,
} from '@element-plus/icons-vue'
import ModelSelector from './ModelSelector.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 添加本地状态跟踪登录状态，确保立即响应
const localIsLoggedIn = ref(!!localStorage.getItem('user'))

// 添加用户信息加载状态
const isLoadingUserInfo = ref(false)

// 添加组件刷新触发器
const refreshTrigger = ref(0)

const activeIndex = computed(() => route.name as string)

// 从内存中获取用户信息
const currentUser = computed(() => {
  // 利用刷新触发器强制重新计算
  // eslint-disable-next-line no-unused-vars
  const _ = refreshTrigger.value

  // 检查localStorage是否有效，如果无效直接返回null
  const storedUser = localStorage.getItem('user')
  if (!storedUser || storedUser === '{}') {
    return null
  }

  // 优先从内存中获取
  if (window.userInfo) {
    return window.userInfo
  }

  // 尝试从localStorage获取
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo && storedUserInfo !== '{}') {
    try {
      const parsedUser = JSON.parse(storedUserInfo)
      // 验证用户信息完整性
      if (parsedUser && parsedUser.username && parsedUser.role) {
        return parsedUser
      }
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }

  // 最后从store获取
  return userStore.currentUser
})

// 使用本地状态而不是直接依赖store
const isLoggedIn = computed(() => {
  // 利用刷新触发器强制重新计算
  // eslint-disable-next-line no-unused-vars
  const _ = refreshTrigger.value

  // 检查localStorage的user键是否存在有效数据
  const storedUser = localStorage.getItem('user')
  const hasValidStorage = storedUser && storedUser !== '{}'

  // 同时检查currentUser是否存在
  return hasValidStorage && !!currentUser.value
})

const modelSelectorRef = ref<InstanceType<typeof ModelSelector> | null>(null)

const handleSelect = (key: string) => {
  if (key !== 'model-settings' && key !== 'logout' && key !== 'user') {
    router.push({ name: key })
  }
}

// 处理角色名称显示
const getRoleName = (role: UserRole | undefined) => {
  if (!role) return '未知角色'
  switch (role) {
    case UserRole.ADMIN:
      return '管理员'
    case UserRole.CURATOR:
      return '策展人'
    case UserRole.EXPERT:
      return '专家'
    case UserRole.ANNOTATOR:
      return '标注员'
    case UserRole.REFEREE:
      return '审核员'
    case UserRole.CROWDSOURCE_USER:
      return '众包用户'
    default:
      return role || '未知角色' // 返回原始角色名称，避免显示未知角色
  }
}

const formatDate = (date: string | undefined) => {
  if (!date) return '未知'
  try {
    return new Date(date).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    console.error('日期格式化失败:', e)
    return '格式错误'
  }
}

// 处理用户登录事件
const handleUserLogin = async (event: Event) => {
  // 将事件转换为CustomEvent类型
  const customEvent = event as CustomEvent<UserInfo>
  const userData = customEvent.detail

  console.log('导航栏组件: 检测到用户登录事件')

  isLoadingUserInfo.value = true
  try {
    // 立即更新本地登录状态
    localIsLoggedIn.value = true

    // 更新window.userInfo (全局内存)
    if (userData) {
      window.userInfo = userData
      console.log('导航栏组件: 用户数据已更新', userData.username)
    } else if (!window.userInfo) {
      // 如果事件没有提供数据，尝试从localStorage获取
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        try {
          window.userInfo = JSON.parse(storedUser)
          console.log('导航栏组件: 从localStorage加载用户数据')
        } catch (e) {
          console.error('解析存储的用户数据失败', e)
        }
      }
    }

    // 强制组件重新渲染
    await nextTick()
  } catch (error) {
    console.error('更新用户信息失败:', error)
  } finally {
    isLoadingUserInfo.value = false
  }
}

// 监听存储变化事件处理
const handleStorageChange = (event: StorageEvent) => {
  if (event.key === 'user' || event.key === 'userInfo') {
    console.log('导航栏组件: 检测到存储变化事件', event.key)

    // 强制刷新计算属性
    refreshTrigger.value++

    // 如果用户数据被清除，清理内存中的用户信息
    if (!event.newValue || event.newValue === '{}') {
      window.userInfo = undefined
      localIsLoggedIn.value = false
    } else {
      // 如果有新的用户数据，更新本地状态
      localIsLoggedIn.value = true
      try {
        if (event.newValue) {
          window.userInfo = JSON.parse(event.newValue)
        }
      } catch (e) {
        console.error('解析存储事件数据失败:', e)
      }
    }

    // 确保视图更新
    nextTick()
  }
}

// 生命周期钩子
onMounted(() => {
  console.log('Debug: TheNavbar mounted')
  console.log('Debug: modelSelectorRef:', modelSelectorRef.value)

  // 添加用户登录事件监听
  window.addEventListener('user-login', handleUserLogin as EventListener)

  // 添加存储变化事件监听
  window.addEventListener('storage', handleStorageChange)

  // 初始化时检查登录状态
  const storedUser = localStorage.getItem('user')
  if (storedUser) {
    console.log('导航栏组件: 初始化检测到用户已登录')
    // 手动触发一次登录事件处理
    handleUserLogin(new CustomEvent('user-login'))
  }
})

onUnmounted(() => {
  // 移除用户登录事件监听
  window.removeEventListener('user-login', handleUserLogin as EventListener)

  // 移除存储变化事件监听
  window.removeEventListener('storage', handleStorageChange)
})

// 监听userStore中currentUser的变化
watch(
  () => userStore.currentUser,
  (newValue) => {
    console.log('导航栏组件: 检测到userStore.currentUser变化', newValue?.username)
    refreshTrigger.value++
    nextTick()
  },
  { deep: true }
)

const handleLogout = async () => {
  try {
    // 立即更新本地状态
    localIsLoggedIn.value = false

    // 清理所有存储
    window.userInfo = undefined
    localStorage.removeItem('userInfo')
    localStorage.removeItem('user')  // 主要清理这个即可
    sessionStorage.removeItem('userInfo')
    sessionStorage.removeItem('user')

    // 强制刷新计算属性
    refreshTrigger.value++

    // 执行登出操作
    await userStore.logout()

    // 确保视图更新
    await nextTick()

    // 额外清理，防止因数据不同步导致的问题
    if (userStore.currentUser) {
      userStore.$reset && userStore.$reset()
    }

    // 触发一个存储事件，通知其他组件用户已登出
    window.dispatchEvent(new StorageEvent('storage', {
      key: 'user',
      newValue: null
    }))

    // 强制重定向到登录页面，确保导航栏刷新
    router.push('/login')

    ElMessage.success('已退出登录')
  } catch (error) {
    console.error('退出登录失败:', error)
    ElMessage.error('退出登录失败，请刷新页面重试')
  }
}

// 处理模型配置
const handleModelConfig = () => {
  if (modelSelectorRef.value) {
    modelSelectorRef.value.toggleConfig()
  }
}
</script>

<style scoped>
.navbar-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--navbar-height);
  z-index: 2000;
  background-color: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.95);
  transition: all 0.3s ease;
}

.navbar {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
  background-color: transparent;
  border-bottom: 1px solid rgba(230, 230, 230, 0.7);
  padding: 0;
  margin: 0;
}

:deep(.el-menu) {
  border: none !important;
  width: 100%;
}

:deep(.el-menu--horizontal) {
  height: var(--navbar-height);
  display: flex;
  align-items: center;
}

:deep(.el-menu--horizontal > .el-menu-item) {
  height: var(--navbar-height);
  line-height: var(--navbar-height);
  border-bottom: none;
  padding: 0 12px;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: none;
}

.navbar-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.navbar-right {
  display: flex;
  align-items: center;
  margin-left: auto;
  padding-right: 16px;
  height: 100%;
}

.logo-container {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 24px;
  height: 100%;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.logo-container::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #4F46E5, #3B82F6, #0EA5E9);
  transform: translateY(3px);
  transition: transform 0.3s ease;
}

.logo-container:hover::after {
  transform: translateY(0);
}

.logo-container:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.logo {
  height: 36px;
  margin-right: 14px;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.15));
  transition: all 0.3s ease;
}

.logo-container:hover .logo {
  transform: scale(1.05);
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.2));
}

.title {
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #4F46E5, #3B82F6, #0EA5E9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  letter-spacing: 0.5px;
}

.nav-item {
  padding: 0 14px;
  position: relative;
  overflow: hidden;
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #4F46E5, #3B82F6, #0EA5E9);
  transform: translateY(3px);
  transition: transform 0.3s ease;
}

.nav-item:hover::after {
  transform: translateY(0);
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: none;
  color: #4F46E5;
}

:deep(.el-menu--horizontal > .el-menu-item.is-active)::after {
  transform: translateY(0);
}

.nav-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  transition: all 0.3s ease;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.nav-item:hover .nav-text {
  color: #4F46E5;
}

.auth-item {
  margin-left: 10px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.auth-item:hover {
  background-color: rgba(79, 70, 229, 0.1);
}

.user-dropdown {
  margin-left: 14px;
  margin-right: 0;
  cursor: pointer;
  position: relative;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
}

:deep(.el-avatar) {
  border: 2px solid transparent;
  box-sizing: content-box;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

:deep(.el-dropdown:hover .el-avatar) {
  border-color: rgba(79, 70, 229, 0.3);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.user-info-header {
  display: flex;
  padding: 10px;
  align-items: center;
  min-width: 200px;
}

.user-info-main {
  margin-left: 10px;
  display: flex;
  flex-direction: column;
}

.user-info-name {
  font-weight: bold;
  font-size: 14px;
}

.user-info-role {
  color: #999;
  font-size: 12px;
}

.user-info-details {
  padding: 8px;
  min-width: 240px;
}

.info-item {
  margin: 6px 0;
  display: flex;
  font-size: 13px;
}

.info-label {
  color: #606266;
  width: 80px;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

.model-selector-container {
  position: absolute;
  visibility: hidden;
  height: 0;
  width: 0;
  overflow: hidden;
}
</style>

