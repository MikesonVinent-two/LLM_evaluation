<template>
  <div class="assessment-workbench">
    <el-container class="workbench-container">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="workbench-aside">
        <div class="logo-container">
          <h3>评测工作台</h3>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="workbench-menu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/assessment/evaluations">
            <el-icon><Histogram /></el-icon>
            <span>评测管理</span>
          </el-menu-item>
          <el-menu-item index="/assessment/scoring">
            <el-icon><Document /></el-icon>
            <span>评分管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主要内容区域 -->
      <el-container class="workbench-content">
        <el-header height="60px" class="workbench-header">
          <div class="header-left">
            <h2>{{ pageTitle }}</h2>
          </div>
          <div class="header-right">
            <el-dropdown>
              <span class="user-dropdown-link">
                {{ userName }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="workbench-main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Histogram, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 用户信息
const userName = ref('评测员')

// 当前激活的菜单项
const activeMenu = computed(() => route.path)

// 当前页面标题
const pageTitle = computed(() => {
  const routePath = route.path
  if (routePath.includes('evaluations')) {
    return '评测管理'
  } else if (routePath.includes('scoring')) {
    return '评分管理'
  }
  return '评测工作台'
})

// 退出登录
const logout = () => {
  // TODO: 实现退出登录逻辑
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.assessment-workbench {
  height: 100vh;
  width: 100%;
}

.workbench-container {
  height: 100%;
}

.workbench-aside {
  background-color: #304156;
  color: #bfcbd9;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  line-height: 60px;
  text-align: center;
  background-color: #263445;
  color: #fff;
}

.workbench-menu {
  border-right: none;
}

.workbench-content {
  flex-direction: column;
}

.workbench-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
}

.user-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  display: flex;
  align-items: center;
}

.workbench-main {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
