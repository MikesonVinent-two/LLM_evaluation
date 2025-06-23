<!-- 聊天界面组件 -->
<template>
  <div class="chat-container">
    <!-- 模型选择器 -->
    <div class="model-selector-bar">
      <div class="current-model">
        <span class="model-label">当前模型：</span>
        <el-select
          v-model="selectedModel"
          placeholder="请选择模型"
          size="small"
          class="model-select"
          :loading="isLoadingModels"
          :disabled="isLoadingModels || availableModels.length === 0"
        >
          <el-option
            v-for="model in availableModels"
            :key="model.id"
            :label="model.name"
            :value="model.id"
            :disabled="!model.available"
          >
            <div class="model-option">
              <span>{{ model.name }}</span>
              <el-tag size="small" :type="model.available ? 'success' : 'danger'">
                {{ model.available ? '可用' : '不可用' }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </div>
      <div class="model-controls">
        <el-tooltip content="启用后会将之前的对话记录一起发送给模型，保持对话连续性" placement="bottom">
          <div class="history-toggle" :class="{ 'history-active': enableHistory }">
            <span class="toggle-label">启用历史：</span>
            <el-switch v-model="enableHistory" />
            <div class="history-status" v-if="enableHistory"></div>
            <span class="history-count" v-if="enableHistory && historyCount > 0">({{ historyCount }}条)</span>
          </div>
        </el-tooltip>
        <div class="model-info" v-if="currentModelInfo">
          <el-tooltip
            :content="currentModelInfo.description || '暂无描述'"
            placement="bottom"
            effect="light"
            :show-after="500"
          >
            <el-tag type="info" size="small">
              {{ currentModelInfo.provider }}
            </el-tag>
          </el-tooltip>
        </div>
      </div>
    </div>

    <!-- 聊天消息列表 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-for="(message, index) in messages" :key="index" class="message-wrapper">
        <!-- AI消息 -->
        <div v-if="message.role === 'assistant'" class="message ai-message">
          <div class="avatar-container">
            <el-avatar :size="40" :src="aiAvatar" class="avatar">AI</el-avatar>
          </div>
          <div class="message-bubble ai-bubble">
            <div class="message-header">
              <span class="sender-name">AI助手</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-content">
              <!-- 渲染思考过程 (如果有) -->
              <div v-if="extractThinking(message.content)" class="thinking-process">
                <div class="thinking-header" @click="toggleThinking(index)">
                  <el-icon><Notebook /></el-icon>
                  <span>思考过程</span>
                  <el-icon :class="{ 'rotate-icon': expandedThinking.includes(index) }">
                    <ArrowDown />
                  </el-icon>
                </div>
                <div v-show="expandedThinking.includes(index)" class="thinking-content markdown-body"
                     v-html="renderThinking(message.content)"></div>
              </div>

              <!-- 渲染正常回复 -->
              <div v-html="renderContent(message.content)" class="markdown-body response-content"></div>
            </div>
          </div>
        </div>

        <!-- 用户消息 -->
        <div v-else class="message user-message">
          <div class="avatar-container user-avatar-container">
            <el-avatar :size="40" :src="userAvatar" class="avatar">U</el-avatar>
          </div>
          <div class="message-bubble user-bubble">
            <div class="message-header">
              <span class="sender-name">你</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-content">
              {{ message.content }}
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态指示器 -->
      <div v-if="isLoading" class="loading-indicator">
        <div class="typing-indicator">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <span>AI正在思考中...</span>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container">
      <div class="input-wrapper">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="输入你的问题..."
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
          @keydown.enter.shift.exact="newline"
          class="custom-input"
        />
        <el-button
          type="primary"
          :disabled="!userInput.trim() || isLoading"
          @click="handleSend"
          class="send-button"
        >
          <el-icon><Position /></el-icon>
          发送
        </el-button>
      </div>
      <div class="input-hint">
        按 Enter 发送，Shift + Enter 换行
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Position, Notebook, ArrowDown } from '@element-plus/icons-vue'
import { sendChatRequest, createDefaultChatRequest } from '@/api/llm'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useLLMStore } from '@/stores/llm'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // 导入GitHub风格的代码高亮样式

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const userAvatar = ref('/src/assets/user-avatar.svg')
const aiAvatar = ref('/src/assets/ai-avatar.svg')
const llmStore = useLLMStore()
const selectedModel = ref('')
const isLoadingModels = computed(() => llmStore.isLoadingModels)
const availableModels = computed(() => llmStore.availableModels)
const currentModelInfo = computed(() =>
  availableModels.value.find(model => model.id === selectedModel.value)
)
const expandedThinking = ref<number[]>([]) // 存储已展开的思考过程消息索引
const enableHistory = ref(false)

// 监听模型选择变化
watch(selectedModel, (newModel) => {
  if (newModel) {
    localStorage.setItem('selectedModel', newModel)
    window.selectedModel = newModel
  }
})

// 监听历史记录设置变化
watch(enableHistory, (newValue) => {
  localStorage.setItem('enableHistory', newValue.toString())
})

// 初始化时加载已选择的模型和历史记录设置
const initSettings = () => {
  // 加载模型设置
  const savedModel = localStorage.getItem('selectedModel')
  if (savedModel) {
    selectedModel.value = savedModel
    window.selectedModel = savedModel
  }

  // 加载历史记录设置
  const savedHistory = localStorage.getItem('enableHistory')
  if (savedHistory) {
    enableHistory.value = savedHistory === 'true'
  }
}

// 格式化时间
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 切换思考过程的显示状态
const toggleThinking = (index: number) => {
  const position = expandedThinking.value.indexOf(index)
  if (position === -1) {
    expandedThinking.value.push(index)
  } else {
    expandedThinking.value.splice(position, 1)
  }
}

// 提取思考过程
const extractThinking = (content: string): string | null => {
  const match = content.match(/<think>([\s\S]*?)<\/think>/i)
  return match ? match[1].trim() : null
}

// 移除思考过程，只保留响应内容
const removeThinking = (content: string): string => {
  return content.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()
}

// 配置marked以使用highlight.js进行代码高亮
onMounted(() => {
  // 初始欢迎消息
  messages.value.push({
    role: 'assistant',
    content: '你好！我是AI助手，有什么我可以帮你的吗？',
    timestamp: Date.now()
  })

  // 配置marked使用highlight.js
  marked.setOptions({
    highlight: function(code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext';
      return hljs.highlight(code, { language }).value;
    },
    langPrefix: 'hljs language-' // 添加类名前缀
  });

  // 初始化设置
  initSettings()
})

// 渲染思考过程
const renderThinking = (content: string): string => {
  const thinking = extractThinking(content)
  if (!thinking) return ''
  const html = marked.parse(thinking, { async: false }) as string
  // 添加代码块语言标签
  const processedHtml = html.replace(/<pre><code class="hljs language-([a-zA-Z0-9]+)">/g,
    (match, lang) => `<pre data-lang="${lang}"><code class="hljs language-${lang}">`)
  return DOMPurify.sanitize(processedHtml)
}

// 渲染正常内容（排除思考过程）
const renderContent = (content: string): string => {
  const cleanContent = removeThinking(content)
  const html = marked.parse(cleanContent, { async: false }) as string
  // 添加代码块语言标签
  const processedHtml = html.replace(/<pre><code class="hljs language-([a-zA-Z0-9]+)">/g,
    (match, lang) => `<pre data-lang="${lang}"><code class="hljs language-${lang}">`)
  return DOMPurify.sanitize(processedHtml)
}

// 检查是否已配置模型
const checkModelConfig = () => {
  const apiConfig = window.apiConfig
  if (!apiConfig || !apiConfig.apiKey || !apiConfig.apiUrl) {
    ElMessage.warning('请先配置API和选择模型')
    // 触发模型配置弹窗
    window.dispatchEvent(new Event('toggle-model-config'))
    return false
  }

  // 检查是否选择了模型
  if (!selectedModel.value) {
    ElMessage.warning('请先选择一个模型')
    return false
  }

  // 检查选择的模型是否可用
  const model = availableModels.value.find(m => m.id === selectedModel.value)
  if (!model?.available) {
    ElMessage.warning('当前选择的模型不可用，请选择其他模型')
    return false
  }

  return true
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 历史消息计数
const historyCount = computed(() => {
  if (!enableHistory.value || messages.value.length <= 1) {
    return 0
  }
  return Math.min(messages.value.length - 1, 10)
})

// 处理发送消息
const handleSend = async () => {
  const message = userInput.value.trim()
  if (!message || isLoading.value) return

  // 检查模型配置
  if (!checkModelConfig()) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message,
    timestamp: Date.now()
  })

  userInput.value = ''
  isLoading.value = true
  await scrollToBottom()

  try {
    // 获取API配置
    const apiConfig = window.apiConfig
    if (!apiConfig || !apiConfig.apiKey || !apiConfig.apiUrl) {
      throw new Error('API 配置无效')
    }

    // 创建请求配置，使用选定的模型
    const chatRequest = createDefaultChatRequest(message)
    chatRequest.apiUrl = apiConfig.apiUrl
    chatRequest.apiKey = apiConfig.apiKey
    chatRequest.api = apiConfig.apiType || 'openai_compatible' // 使用apiType作为api字段
    chatRequest.model = selectedModel.value // 直接使用选定的模型

    // 如果启用了历史记录，添加最近10条消息到请求中
    if (enableHistory.value && messages.value.length > 1) {
      // 获取最近的消息，最多10条(不包括刚刚添加的当前消息)
      const historyMessages = messages.value
        .slice(0, -1) // 排除刚刚添加的消息
        .slice(-10) // 最多取10条
        .map(msg => ({
          role: msg.role,
          content: msg.content
        }))

      // 添加历史消息到请求中
      chatRequest.chatMessages = historyMessages

      // 确保将当前消息也添加到chatMessages中
      if (chatRequest.chatMessages) {
        chatRequest.chatMessages.push({
          role: 'user',
          content: message
        });
      }

      console.log(`发送了 ${historyMessages.length} 条历史消息:`, JSON.stringify(historyMessages))
    }

    // 发送请求
    const response = await sendChatRequest(chatRequest)

    if (response.success) {
      // 添加AI回复
      messages.value.push({
        role: 'assistant',
        content: response.content,
        timestamp: Date.now()
      })
    } else {
      throw new Error(response.errorMessage || '未知错误')
    }
  } catch (error) {
    ElMessage.error('发送消息失败：' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

// 处理换行
const newline = () => {
  userInput.value += '\n'
}

// 清空消息
const clearMessages = () => {
  messages.value = []
  // 添加初始欢迎消息
  messages.value.push({
    role: 'assistant',
    content: '你好！我是AI助手，有什么我可以帮你的吗？',
    timestamp: Date.now()
  })
}

// 暴露方法给父组件
defineExpose({
  clearMessages
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: #f5f7fa;
  overflow: hidden;
  position: relative;
  border-radius: 0;
  box-shadow: none;
}

.model-selector-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: #fff;
  border-bottom: 1px solid #eaedf1;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.current-model {
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #f9fafc;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #eaedf1;
  transition: all 0.3s ease;
}

.current-model:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.model-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  font-weight: 500;
}

.model-select {
  width: 220px;
}

:deep(.el-select .el-input__wrapper) {
  background-color: transparent;
  box-shadow: none;
  padding: 0;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: none;
}

.model-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-select-dropdown__item) {
  padding: 10px 14px;
  border-radius: 6px;
  margin: 4px 6px;
  transition: all 0.2s ease;
}

:deep(.el-select-dropdown__item.hover) {
  background-color: rgba(64, 158, 255, 0.1);
}

:deep(.el-select-dropdown__item.selected) {
  background-color: rgba(64, 158, 255, 0.15);
  color: var(--el-color-primary);
  font-weight: 600;
}

:deep(.el-tag) {
  margin-left: 8px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.model-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.history-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  background-color: #f9fafc;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #eaedf1;
  transition: all 0.3s ease;
}

.history-toggle:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.toggle-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  font-weight: 500;
}

:deep(.el-switch) {
  --el-switch-on-color: var(--el-color-primary);
  --el-switch-off-color: #dcdfe6;
}

:deep(.el-switch.is-checked .el-switch__core) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.history-status {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: var(--el-color-success);
  display: none;
  box-shadow: 0 0 0 2px #fff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

.history-active .history-status {
  display: block;
}

.history-count {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-left: 5px;
  font-weight: 600;
}

/* 消息容器 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background-color: #f5f7fa;
  background-image: radial-gradient(#e6e6e6 1px, transparent 1px);
  background-size: 20px 20px;
  display: flex;
  flex-direction: column;
  scrollbar-width: thin;
  scrollbar-color: #ccc #f1f1f1;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #aaa;
}

/* 消息样式 */
.message-wrapper {
  margin-bottom: 28px;
  width: 100%;
  display: block;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message {
  display: flex;
  max-width: 85%;
  align-items: flex-start;
}

.user-message {
  margin-left: auto;
  margin-right: 0;
  flex-direction: row;
  justify-content: flex-end;
}

.ai-message {
  margin-right: auto;
  margin-left: 0;
  flex-direction: row;
}

.avatar-container {
  flex-shrink: 0;
  margin: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-container .el-avatar {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
}

.user-avatar-container {
  order: 1;
}

.message-bubble {
  border-radius: 16px;
  padding: 14px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  max-width: calc(100% - 60px);
  transition: all 0.3s ease;
}

.user-bubble {
  background-color: #e1f0ff;
  border: 1px solid #c9e2ff;
  border-top-right-radius: 4px;
  margin-left: 0;
  margin-right: 10px;
  box-shadow: 0 2px 8px rgba(0, 120, 255, 0.08);
}

.ai-bubble {
  background-color: #fff;
  border: 1px solid #eaedf1;
  border-top-left-radius: 4px;
  margin-left: 10px;
  margin-right: 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

/* 悬停效果 */
.message-bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.user-bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 120, 255, 0.12);
}

.ai-bubble:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 6px;
}

.sender-name {
  font-weight: 600;
  color: #333;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-content {
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
}

/* 思考过程样式 */
.thinking-process {
  margin-bottom: 16px;
  border: 1px solid #e1e4e8;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.thinking-process:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.thinking-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #f6f8fa;
  background-image: linear-gradient(to right, #f6f8fa, #eef2f7);
  cursor: pointer;
  font-size: 13px;
  color: #24292e;
  font-weight: 500;
  transition: background-color 0.2s;
  border-bottom: 1px solid transparent;
}

.thinking-header:hover {
  background-color: #eef2f7;
  background-image: linear-gradient(to right, #eef2f7, #e6ebf2);
}

.thinking-header .el-icon {
  margin-right: 8px;
  color: #0366d6;
  transition: transform 0.3s ease;
}

.thinking-header:hover .el-icon {
  transform: scale(1.1);
}

.thinking-content {
  padding: 16px;
  background-color: #fafbfc;
  border-top: 1px solid #e1e4e8;
  font-size: 13px;
  color: #444d56;
  transition: all 0.3s ease;
}

.rotate-icon {
  transform: rotate(180deg);
  transition: transform 0.3s;
}

.response-content {
  padding: 0;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: 16px 0;
  padding: 12px 16px;
  color: #666;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(4px);
  max-width: 200px;
  margin-left: 60px;
}

.typing-indicator {
  display: flex;
  margin-right: 10px;
}

.typing-indicator span {
  height: 8px;
  width: 8px;
  border-radius: 50%;
  background-color: var(--el-color-primary);
  margin: 0 2px;
  animation: typing 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0% {
    transform: translateY(0);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-6px);
    opacity: 1;
  }
  100% {
    transform: translateY(0);
    opacity: 0.6;
  }
}

/* 输入区域 */
.input-container {
  padding: 18px 24px;
  border-top: 1px solid #eaedf1;
  background-color: #fff;
  z-index: 10;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.03);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  background-color: #f9fafc;
  border-radius: 12px;
  padding: 8px;
  border: 1px solid #eaedf1;
  transition: all 0.3s ease;
}

.input-wrapper:focus-within {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.custom-input {
  flex: 1;
}

:deep(.el-textarea__inner) {
  border: none;
  background-color: transparent;
  box-shadow: none;
  padding: 8px 12px;
  resize: none;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.send-button {
  margin-left: 10px;
  height: 42px;
  padding: 0 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}

/* Markdown内容样式 */
.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  color: #24292e;
}

.markdown-body pre {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #e1e4e8;
  position: relative;
}

/* 代码块语言标签 */
.markdown-body pre::before {
  content: attr(data-lang);
  position: absolute;
  top: 0;
  right: 0;
  color: #6a737d;
  font-size: 12px;
  padding: 4px 8px;
  background: #f1f1f1;
  border-bottom-left-radius: 4px;
  border-top-right-radius: 4px;
  text-transform: uppercase;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-body code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  background-color: rgba(27, 31, 35, 0.05);
  padding: 3px 6px;
  border-radius: 3px;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
  font-size: 13px;
  color: #24292e;
  word-break: normal;
  white-space: pre;
  display: block;
  overflow-x: auto;
  padding-top: 8px;
}

.markdown-body p {
  margin: 0 0 16px;
}

.markdown-body ul, .markdown-body ol {
  padding-left: 2em;
  margin: 8px 0 16px;
}

.markdown-body li {
  margin: 4px 0;
}

.markdown-body h1, .markdown-body h2, .markdown-body h3,
.markdown-body h4, .markdown-body h5, .markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body h1 {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-body h2 {
  font-size: 1.3em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-body h3 {
  font-size: 1.1em;
}

.markdown-body h4 {
  font-size: 1em;
}

.markdown-body blockquote {
  margin: 16px 0;
  padding: 0 16px;
  color: #6a737d;
  border-left: 4px solid #dfe2e5;
}

.markdown-body img {
  max-width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
  margin: 12px 0;
}

.markdown-body table {
  display: block;
  width: 100%;
  overflow: auto;
  border-spacing: 0;
  border-collapse: collapse;
  margin: 16px 0;
}

.markdown-body table th {
  font-weight: 600;
  padding: 8px 13px;
  border: 1px solid #dfe2e5;
  background-color: #f6f8fa;
}

.markdown-body table td {
  padding: 8px 13px;
  border: 1px solid #dfe2e5;
}

.markdown-body table tr {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.markdown-body table tr:nth-child(2n) {
  background-color: #f6f8fa;
}

.markdown-body hr {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #e1e4e8;
  border: 0;
}

.markdown-body a {
  color: #0366d6;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

.markdown-body strong {
  font-weight: 600;
}

.markdown-body em {
  font-style: italic;
}

/* 高亮.js相关样式 */
.hljs {
  display: block;
  overflow-x: auto;
  padding: 0;
  color: #24292e;
  background: transparent;
}

.hljs-comment,
.hljs-quote {
  color: #6a737d;
  font-style: italic;
}

.hljs-keyword,
.hljs-selector-tag {
  color: #d73a49;
}

.hljs-literal,
.hljs-number,
.hljs-tag .hljs-attr,
.hljs-template-variable,
.hljs-variable {
  color: #005cc5;
}

.hljs-doctag,
.hljs-string {
  color: #032f62;
}

.hljs-section,
.hljs-selector-id,
.hljs-title {
  color: #6f42c1;
  font-weight: bold;
}

.hljs-subst {
  font-weight: normal;
}

.hljs-type {
  color: #d73a49;
  font-weight: bold;
}

.hljs-class .hljs-title {
  color: #6f42c1;
}

.hljs-tag {
  color: #22863a;
}

.hljs-name {
  color: #22863a;
  font-weight: bold;
}

.hljs-attribute {
  color: #005cc5;
}

.hljs-link,
.hljs-regexp {
  color: #032f62;
}

.hljs-symbol {
  color: #005cc5;
}

.hljs-bullet,
.hljs-built_in,
.hljs-builtin-name,
.hljs-meta,
.hljs-meta .hljs-string,
.hljs-selector-attr,
.hljs-selector-pseudo {
  color: #e36209;
}

.hljs-deletion {
  background: #ffeef0;
  color: #b31d28;
}

.hljs-addition {
  background: #f0fff4;
  color: #22863a;
}
</style>
