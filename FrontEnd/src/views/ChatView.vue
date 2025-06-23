<template>
  <div class="chat-view">
    <div class="chat-header">
      <div class="chat-header-content">
        <div class="chat-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          <h1>AI智能助手</h1>
        </div>
        <div class="header-actions">
          <el-tooltip content="清空对话" placement="bottom">
            <el-button circle size="small" @click="confirmClearChat">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="设置" placement="bottom">
            <el-button circle size="small" @click="showSettings = true">
              <el-icon><Setting /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>对话历史</h3>
          <el-button size="small" type="primary" plain @click="createNewChat">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
        </div>
        <div class="chat-history">
          <div
            v-for="(chat, index) in chatHistory"
            :key="index"
            class="history-item"
            :class="{ 'active': currentChatIndex === index }"
            @click="switchChat(index)"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span class="history-title">{{ chat.title || '新对话' }}</span>
            <span class="history-time">{{ formatTime(chat.time) }}</span>
          </div>
          <div v-if="chatHistory.length === 0" class="empty-history">
            <el-icon><InfoFilled /></el-icon>
            <span>暂无历史对话</span>
          </div>
        </div>
      </div>

      <div class="chat-container">
        <chat-interface ref="chatInterface" />
      </div>
    </div>

    <!-- 设置对话框 -->
    <el-dialog
      v-model="showSettings"
      title="设置"
      width="500px"
      destroy-on-close
    >
      <div class="settings-content">
        <h3>界面设置</h3>
        <div class="setting-item">
          <span>深色模式</span>
          <el-switch v-model="darkMode" @change="toggleDarkMode" />
        </div>
        <div class="setting-item">
          <span>显示侧边栏</span>
          <el-switch v-model="showSidebar" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showSettings = false">取消</el-button>
          <el-button type="primary" @click="saveSettings">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 确认清空对话框 -->
    <el-dialog
      v-model="showClearConfirm"
      title="确认清空"
      width="400px"
    >
      <div class="confirm-content">
        <el-icon class="warning-icon"><Warning /></el-icon>
        <p>确定要清空当前对话吗？此操作不可恢复。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showClearConfirm = false">取消</el-button>
          <el-button type="danger" @click="clearChat">
            确认清空
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import ChatInterface from '@/components/ChatInterface.vue'
import { ChatDotRound, ChatLineRound, Delete, Setting, Plus, InfoFilled, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ChatHistoryItem {
  title: string
  time: number
}

const chatInterface = ref(null)
const showSettings = ref(false)
const darkMode = ref(false)
const showSidebar = ref(true)
const showClearConfirm = ref(false)
const chatHistory = ref<ChatHistoryItem[]>([])
const currentChatIndex = ref(0)

// 格式化时间
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 切换深色模式
const toggleDarkMode = (value: boolean) => {
  if (value) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  localStorage.setItem('darkMode', value.toString())
}

// 保存设置
const saveSettings = () => {
  localStorage.setItem('showSidebar', showSidebar.value.toString())
  showSettings.value = false
  ElMessage.success('设置已保存')
}

// 确认清空对话
const confirmClearChat = () => {
  showClearConfirm.value = true
}

// 清空对话
const clearChat = () => {
  if (chatInterface.value) {
    chatInterface.value.clearMessages()
  }
  showClearConfirm.value = false
  ElMessage.success('对话已清空')
}

// 创建新对话
const createNewChat = () => {
  chatHistory.value.unshift({
    title: '新对话',
    time: Date.now()
  })
  currentChatIndex.value = 0
  if (chatInterface.value) {
    chatInterface.value.clearMessages()
  }
  localStorage.setItem('chatHistory', JSON.stringify(chatHistory.value))
}

// 切换对话
const switchChat = (index: number) => {
  currentChatIndex.value = index
  // 这里应该加载对应的聊天记录，但需要在ChatInterface组件中实现加载方法
  ElMessage.info('切换到对话: ' + chatHistory.value[index].title)
}

// 监听侧边栏显示状态
watch(showSidebar, (newValue) => {
  document.body.classList.toggle('hide-sidebar', !newValue)
})

onMounted(() => {
  // 加载设置
  const savedDarkMode = localStorage.getItem('darkMode')
  if (savedDarkMode) {
    darkMode.value = savedDarkMode === 'true'
    toggleDarkMode(darkMode.value)
  }

  const savedShowSidebar = localStorage.getItem('showSidebar')
  if (savedShowSidebar) {
    showSidebar.value = savedShowSidebar === 'true'
  }

  // 加载历史对话
  const savedHistory = localStorage.getItem('chatHistory')
  if (savedHistory) {
    try {
      chatHistory.value = JSON.parse(savedHistory)
    } catch (e) {
      console.error('Failed to parse chat history:', e)
    }
  }

  // 如果没有历史对话，创建一个新的
  if (chatHistory.value.length === 0) {
    createNewChat()
  }
})
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: #f5f7fa;
  overflow: hidden;
  position: relative;
}

.chat-header {
  height: 64px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  z-index: 10;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.chat-header-content {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.chat-title h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  background-color: #fff;
  border-right: 1px solid #eaedf1;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #eaedf1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-item.active {
  background-color: rgba(64, 158, 255, 0.1);
}

.history-item .el-icon {
  font-size: 18px;
  color: #909399;
  margin-right: 10px;
}

.history-item.active .el-icon {
  color: var(--el-color-primary);
}

.history-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  color: #333;
}

.history-time {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
  color: #909399;
}

.empty-history .el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.chat-container {
  flex: 1;
  height: 100%;
  overflow: hidden;
  position: relative;
}

/* 设置对话框样式 */
.settings-content {
  padding: 10px 0;
}

.settings-content h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eaedf1;
  padding-bottom: 10px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 8px 0;
}

.confirm-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.warning-icon {
  font-size: 24px;
  color: var(--el-color-danger);
}

/* 深色模式 */
:global(.dark) .chat-view {
  background-color: #1a1a1a;
}

:global(.dark) .chat-header,
:global(.dark) .chat-sidebar {
  background-color: #252525;
  border-color: #333;
}

:global(.dark) .chat-title h1,
:global(.dark) .sidebar-header h3,
:global(.dark) .history-title {
  color: #e0e0e0;
}

:global(.dark) .history-item:hover {
  background-color: #2c2c2c;
}

:global(.dark) .history-item.active {
  background-color: rgba(64, 158, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-sidebar {
    position: absolute;
    left: 0;
    top: 64px;
    bottom: 0;
    z-index: 100;
    transform: translateX(0);
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }

  :global(.hide-sidebar) .chat-sidebar {
    transform: translateX(-100%);
  }
}
</style>
