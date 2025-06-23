<script setup lang="ts">
import { RouterView } from 'vue-router'
import TheNavbar from '@/components/TheNavbar.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import { useUserStore } from '@/stores/user'
import { onMounted, onUnmounted, ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { websocketService } from '@/services/websocket'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const sidebarCollapsed = ref(false)

// 判断用户是否已登录
const isLoggedIn = computed(() => {
  // 首先检查localStorage中的user是否存在并有效
  const storedUser = localStorage.getItem('user')
  const hasValidLocalStorage = storedUser && storedUser !== '{}'
  // 同时检查用户存储中的状态
  return hasValidLocalStorage && !!userStore.currentUser
})


// 计算侧边栏宽度
const sidebarWidth = computed(() => {
  const storedUser = localStorage.getItem('user')
  const hasValidUser = storedUser && storedUser !== '{}'

  if (!hasValidUser) return '0px'
  return sidebarCollapsed.value ? '64px' : '200px'
})

// 监听侧边栏折叠状态变化
const handleSidebarCollapse = (collapsed: boolean) => {
  sidebarCollapsed.value = collapsed
  // 保存侧边栏状态以便其他组件使用
  localStorage.setItem('sidebarState', JSON.stringify({ collapsed }))
}

// 监听storage事件，当localStorage发生变化时（比如在其他标签页登出）更新状态
const handleStorageChange = (event: StorageEvent) => {
  if (event.key === 'user') {
    if (!event.newValue || event.newValue === '{}') {
      // 用户被清除，立即清除本地状态
      userStore.currentUser = null
      console.log('用户登出: 检测到本地存储变化，已清除用户状态')

      // 强制重新渲染导航栏和侧边栏
      nextTick(() => {
        // 如果当前不在登录页，则重定向到登录页
        if (route.name !== 'login') {
          router.push('/login')
        }
      })
    } else if (event.newValue !== event.oldValue) {
      // 用户信息变化，重新加载
      userStore.fetchUserInfo()
      console.log('用户信息变化: 检测到本地存储变化，已更新用户状态')

      // 手动触发一次更新
      nextTick(() => {
        // 触发一个自定义事件，通知需要更新的组件
        window.dispatchEvent(new CustomEvent('auth-state-change'))
      })
    }
  }
}

// 监听用户登录事件
const handleUserLogin = () => {
  console.log('App组件: 检测到用户登录事件，刷新状态')

  // 强制刷新用户信息，确保状态正确
  userStore.fetchUserInfo()

  // 登录成功后，确保刷新界面
  nextTick(() => {
    console.log('App组件: 登录状态已更新，isLoggedIn =', isLoggedIn.value)
  })
}

// 在组件挂载时获取用户信息并添加事件监听器
onMounted(async () => {
  console.log('应用挂载: 开始获取用户信息')
  // 检查本地存储中是否有用户信息
  await userStore.fetchUserInfo()
  console.log('应用挂载: 用户信息获取完成', !!userStore.currentUser, 'isLoggedIn =', isLoggedIn.value)

  // 添加事件监听器
  window.addEventListener('storage', handleStorageChange)

  // 添加登录事件监听（修复类型问题）
  window.addEventListener('user-login', handleUserLogin as EventListener)

  // 在挂载时立即检查登录状态并触发更新
  if (localStorage.getItem('user')) {
    console.log('应用挂载: 检测到已有登录信息，触发状态更新')
    // 手动触发一次登录事件，确保所有组件都能正确响应
    window.dispatchEvent(new CustomEvent('user-login'))
  }

  // 初始化WebSocket连接
  websocketService.connect()
})

// 在组件卸载时移除事件监听器
onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange)
  window.removeEventListener('user-login', handleUserLogin as EventListener)

  // 关闭WebSocket连接
  websocketService.disconnect()
})
</script>

<template>
  <div class="app">
    <TheNavbar />
    <div class="app-content">
      <TheSidebar @collapse-change="handleSidebarCollapse" />
      <main class="main-content" :style="{ marginLeft: sidebarWidth }">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style>
/* 全局样式 */
:root {
  --primary-color: #4c84ff;
  --secondary-color: #3a70e3;
  --text-color: #333;
  --light-text: #666;
  --border-color: #eee;
  --background-color: #f5f7fa;
  --white: #fff;
  --container-width: 1600px;
  --container-padding: 40px;
  --navbar-height: 60px;
  --sidebar-width: 200px;
  --sidebar-collapsed-width: 64px;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}

body {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  color: var(--text-color);
  line-height: 1.6;
  background-color: var(--background-color);
  min-width: 320px;
}

a {
  text-decoration: none;
  color: var(--primary-color);
}

#app {
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow-x: hidden;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  position: relative;
}

.app-content {
  display: flex;
  flex: 1;
  position: relative;
  margin-top: var(--navbar-height);
  width: 100%;
  min-height: calc(100vh - var(--navbar-height));
  overflow-x: hidden;
}

.main-content {
  position: fixed;
  top: var(--navbar-height);
  left: 0;
  right: 0;
  bottom: 0;
  overflow-y: auto;
  transition: margin-left 0.3s;
  background-color: var(--background-color);
  z-index: 1;
  box-sizing: border-box;
}

/* 响应式布局调整 */
@media (max-width: 1700px) {
  :root {
    --container-width: 1400px;
    --container-padding: 30px;
  }
}

@media (max-width: 1500px) {
  :root {
    --container-width: 1200px;
    --container-padding: 25px;
  }
}

@media (max-width: 1300px) {
  :root {
    --container-width: 1000px;
    --container-padding: 20px;
  }
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0 !important;
  }
}
</style>
