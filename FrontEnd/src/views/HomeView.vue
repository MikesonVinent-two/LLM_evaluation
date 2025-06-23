<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Setting,
  Files,
  VideoPlay,
  DataAnalysis,
  Collection,
  QuestionFilled,
  ChatDotRound,
  Star,
  StarFilled,
  EditPen,
  Document,
  TopRight,
  Check,
  View,
  Connection,
  List,
  Histogram,
  User,
  Cpu,
  UserFilled,
  FolderAdd,
  Monitor,
  PieChart,
  Timer,
  Plus
} from '@element-plus/icons-vue'
import { websocketService } from '@/services/websocket'
import { WebSocketConnectionStatus } from '@/types/websocketTypes'
import { getAccessibleWorkspaces, workspaceTypeNames } from '@/config/workspaceRoles'

const router = useRouter()
const userStore = useUserStore()
let removeConnectionListener: (() => void) | null = null

// 用户角色和显示名称
const userRole = computed(() => userStore.currentUser?.role || '')
const userRoleDisplay = computed(() => {
  const roleMap: Record<string, string> = {
    'ADMIN': '管理员',
    'CURATOR': '数据管理员',
    'EXPERT': '专家',
    'ANNOTATOR': '标注员',
    'REFEREE': '评审员',
    'CROWDSOURCE_USER': '众包用户'
  }
  return roleMap[userRole.value] || '未知角色'
})

// 评测员信息
const isEvaluator = computed(() => userStore.currentUser?.isEvaluator || false)
const evaluatorId = computed(() => userStore.currentUser?.evaluatorId || '-')

// 获取当前用户可访问的工作台
const accessibleWorkspaces = computed(() => {
  return getAccessibleWorkspaces(userRole.value, isEvaluator.value)
})

// 按工作台类型分组
const workspacesByType = computed(() => {
  const result: Record<string, Array<{
    id: string;
    name: string;
    type: string;
    path: string;
    roles: string[];
    description: string;
    icon: string;
    requiresEvaluator?: boolean;
    parentId?: string;
  }>> = {}

  accessibleWorkspaces.value.forEach(workspace => {
    if (!result[workspace.type]) {
      result[workspace.type] = []
    }
    result[workspace.type].push(workspace)
  })

  return result
})

// 获取工作台类型名称
const getWorkspaceTypeName = (type: string) => {
  // 确保workspaceTypeNames存在并且是对象
  if (workspaceTypeNames && typeof workspaceTypeNames === 'object') {
    // 使用类型断言来避免TypeScript错误
    return (workspaceTypeNames as Record<string, string>)[type] || type
  }
  return type
}

// 获取图标组件
const getIconComponent = (iconName: string) => {
  const iconMap: Record<string, unknown> = {
    Setting, Files, VideoPlay, DataAnalysis, Collection,
    QuestionFilled, ChatDotRound, Star, StarFilled, EditPen,
    Document, TopRight, Check, View, Connection, List, Histogram,
    User, Cpu, UserFilled, FolderAdd, Monitor, PieChart, Timer, Plus
  }
  return iconMap[iconName] || Setting
}

// WebSocket状态处理
const wsStatus = computed(() => websocketService.status.value)
const isConnected = computed(() => wsStatus.value === WebSocketConnectionStatus.CONNECTED)
const isConnecting = computed(() => wsStatus.value === WebSocketConnectionStatus.CONNECTING)

const wsStatusText = computed(() => {
  switch (wsStatus.value) {
    case WebSocketConnectionStatus.CONNECTED:
      return '已连接'
    case WebSocketConnectionStatus.CONNECTING:
      return '正在连接...'
    case WebSocketConnectionStatus.DISCONNECTED:
      return '已断开'
    case WebSocketConnectionStatus.ERROR:
      return '连接错误'
    default:
      return '未知状态'
  }
})

const wsStatusClass = computed(() => {
  switch (wsStatus.value) {
    case WebSocketConnectionStatus.CONNECTED:
      return 'status-connected'
    case WebSocketConnectionStatus.CONNECTING:
      return 'status-connecting'
    case WebSocketConnectionStatus.DISCONNECTED:
      return 'status-disconnected'
    case WebSocketConnectionStatus.ERROR:
      return 'status-error'
    default:
      return ''
  }
})

// 页面导航
const navigateTo = (path: string) => {
  router.push(path)
}

// 连接WebSocket
const connectWebSocket = async () => {
  await websocketService.connect()
}

// 断开WebSocket连接
const disconnectWebSocket = async () => {
  await websocketService.disconnect()
}

// 导航到WebSocket演示页面
const navigateToDemo = () => {
  router.push('/websocket-demo')
}

onMounted(() => {
  // 添加WebSocket连接状态监听器
  removeConnectionListener = websocketService.addConnectionListener((status) => {
    console.log('WebSocket连接状态已更改:', status)
  })
})

onBeforeUnmount(() => {
  // 移除WebSocket连接状态监听器
  if (removeConnectionListener) {
    removeConnectionListener()
  }
})
</script>

<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <template #header>
            <div class="card-header">
              <h2>欢迎使用LLM评测系统</h2>
            </div>
          </template>
          <div class="welcome-content">
            <p>您当前的角色：<strong>{{ userRoleDisplay }}</strong></p>
            <p v-if="isEvaluator" class="evaluator-badge">
              <el-tag type="success">评测员</el-tag>
              <span>评测员ID: {{ evaluatorId }}</span>
            </p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 通用功能卡片 - 所有用户可见 -->
    <el-row :gutter="20" class="function-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="function-card" @click="navigateTo('/chat')">
          <el-icon class="card-icon"><ChatDotRound /></el-icon>
          <h3>AI对话</h3>
          <p>与AI助手进行对话交流</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 根据工作台类型分组显示卡片 -->
    <template v-for="(workspaces, type) in workspacesByType" :key="type">
      <h2 class="workspace-type-title">{{ getWorkspaceTypeName(type) }}</h2>
    <el-row :gutter="20" class="function-row">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="workspace in workspaces" :key="workspace.id">
          <el-card class="function-card" @click="navigateTo(workspace.path)">
            <el-icon class="card-icon">
              <component :is="getIconComponent(workspace.icon)" />
            </el-icon>
            <h3>{{ workspace.name }}</h3>
            <p>{{ workspace.description }}</p>
          </el-card>
        </el-col>
      </el-row>
      </template>

    <!-- 添加WebSocket连接状态指示器 -->
    <div class="websocket-status-widget">
      <h3>WebSocket连接状态</h3>
      <div class="status-display">
        <div class="status-indicator" :class="wsStatusClass"></div>
        <span>{{ wsStatusText }}</span>
      </div>
      <div class="status-actions">
        <el-button
          type="primary"
          size="small"
          @click="connectWebSocket"
          :disabled="isConnected || isConnecting"
        >
          连接
        </el-button>
        <el-button
          type="danger"
          size="small"
          @click="disconnectWebSocket"
          :disabled="!isConnected"
        >
          断开
        </el-button>
        <el-button
          type="success"
          size="small"
          @click="navigateToDemo"
        >
          查看演示页面
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 强制覆盖App.vue中的样式 */
:deep(.main-content) {
  margin-left: 0 !important;
}

.home {
  padding: 20px;
}

.welcome-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-content {
  padding: 20px 0;
}

.evaluator-badge {
  margin-top: 10px;
  display: flex;
  align-items: center;
}

.evaluator-badge span {
  margin-left: 10px;
}

.workspace-type-title {
  margin: 30px 0 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eaeaea;
  color: #333;
  font-size: 18px;
}

.function-row {
  margin-top: 20px;
}

.function-card {
  height: 180px;
  margin-bottom: 20px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  transition: all 0.3s;
}

.function-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.card-icon {
  font-size: 36px;
  margin-bottom: 10px;
  color: var(--primary-color);
}

.function-card h3 {
  margin: 10px 0;
  font-size: 18px;
}

.function-card p {
  color: var(--light-text);
  font-size: 14px;
}

/* WebSocket状态组件样式 */
.websocket-status-widget {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  background-color: #f9f9f9;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  max-width: 400px;
}

.websocket-status-widget h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #333;
}

.status-display {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
}

.status-connected {
  background-color: #4caf50;
  box-shadow: 0 0 8px #4caf50;
}

.status-connecting {
  background-color: #2196f3;
  animation: blink 1s infinite;
}

.status-disconnected {
  background-color: #9e9e9e;
}

.status-error {
  background-color: #f44336;
  box-shadow: 0 0 8px #f44336;
}

.status-actions {
  display: flex;
  gap: 10px;
}

@keyframes blink {
  0% { opacity: 0.4; }
  50% { opacity: 1; }
  100% { opacity: 0.4; }
}
</style>
