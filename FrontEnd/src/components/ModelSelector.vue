<!-- 模型选择器组件 -->
<template>
  <div class="model-selector">
    <!-- 模型选择器占位符 -->
    <div class="model-select-placeholder"></div>

    <!-- API配置触发区域 (保持按钮等可见) -->
    <div class="config-trigger">
            <el-tooltip
              content="配置模型API"
              placement="top"
              :show-after="500"
            >
              <el-button
                class="settings-button"
                :icon="Setting"
                circle
                @click="toggleConfig"
                :type="showConfig ? 'primary' : 'default'"
              />
            </el-tooltip>
          </div>

    <!-- API配置表单 Popover -->
    <el-popover
      v-model:visible="showConfig"
      :width="400"
      trigger="manual"
      placement="bottom-end"
      popper-class="model-config-popover"
      :show-arrow="true"
      :offset="12"
      :teleported="true"
      append-to-body
    >
      <template #reference>
        <div style="display: none"></div>
      </template>

      <template #default>
        <div class="model-config">
          <div class="config-header">
            <span class="title">模型配置</span>
            <el-button
              type="text"
              :icon="Close"
              @click="closeConfig"
              class="close-button"
            />
          </div>

          <el-form
            ref="configFormRef"
            :model="apiConfig"
            :rules="configRules"
            label-position="top"
            class="api-config-form"
            @submit.prevent="handleSave"
          >
            <el-form-item
              label="API 基础 URL"
              prop="apiUrl"
              :rules="[
                { required: true, message: '请输入API基础URL', trigger: 'blur' },
                { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
              ]"
            >
              <el-input
                v-model="apiConfig.apiUrl"
                placeholder="请输入API URL"
                :disabled="isLoadingModels"
              >
                <template #suffix>
                  <el-tooltip content="点击使用默认URL" placement="top">
                    <el-button
                      link
                      type="primary"
                      @click="useDefaultUrl"
                      :disabled="isLoadingModels"
                    >
                      默认
                    </el-button>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item
              label="API 类型"
              prop="apiType"
              :rules="[
                { required: true, message: '请选择API类型', trigger: 'change' }
              ]"
            >
              <el-select
                v-model="apiConfig.apiType"
                placeholder="请选择API类型"
                :disabled="isLoadingModels"
                style="width: 100%"
              >
                <el-option
                  v-for="option in apiTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

          <el-form-item
            label="API 密钥"
            prop="apiKey"
            :rules="[
              { required: true, message: '请输入API密钥', trigger: 'blur' },
              { min: 32, message: 'API密钥长度不正确', trigger: 'blur' }
            ]"
          >
            <el-input
              v-model="apiConfig.apiKey"
              type="password"
              placeholder="请输入API Key"
              show-password
              :disabled="isLoadingModels"
            />
          </el-form-item>

          <!-- 模型选择器 - 把按钮和选择器分开条件渲染 -->
          <el-form-item label="选择模型">
            <div class="selector-with-button">
              <!-- 只有模型选择器受条件控制 -->
              <el-select
                v-if="availableModels.length > 0 || isLoadingModels"
                v-model="modelSelection.selectedModel"
                placeholder="请选择或加载模型"
                class="model-select-input flex-grow"
                :loading="isLoadingModels"
                loading-text="正在加载模型..."
                :disabled="isLoadingModels"
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
              <!-- 当没有模型时，显示占位提示 -->
              <div
                v-else
                class="select-placeholder flex-grow"
              >
                请点击右侧按钮加载
              </div>
              <!-- 加载按钮始终显示 -->
              <el-button
                type="info"
                @click="loadModels"
                :loading="isLoadingModels"
                :disabled="!isFormValid || isLoadingModels"
                class="load-button"
              >
                加载模型
              </el-button>
            </div>
          </el-form-item>

          <!-- 提供给测评按钮 - 只在成功加载模型后显示 -->
          <el-form-item v-if="availableModels.length > 0 && !isLoadingModels">
            <div class="evaluation-section" :class="{ 'register-success': registerSuccess }">
              <el-button
                type="success"
                @click="registerForEvaluation"
                :loading="isRegistering"
                :disabled="!modelSelection.selectedModel"
                class="register-button"
              >
                <el-icon><DataAnalysis /></el-icon>
                {{ registerSuccess ? '已提供' : '提供给测评' }}
              </el-button>
              <div class="register-hint">
                {{ registerSuccess ? '感谢您的贡献，您的API已成功提供给测评系统' : '提供您的API给我们进行模型测评，将有助于改进系统评测和比较' }}
              </div>
            </div>
          </el-form-item>

          <!-- 表单按钮 -->
            <el-form-item>
              <div class="form-buttons">
                <el-button @click="closeConfig">取消</el-button>
                <el-button
                  type="primary"
                  native-type="submit"
                  :loading="isLoadingModels"
                  :disabled="!isFormValid"
                >
                  保存
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </template>
    </el-popover>

    <!-- 选中模型的详细信息 (保持在外部) -->
      <div v-if="selectedModelInfo" class="model-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模型名称">
            {{ selectedModelInfo.name }}
          </el-descriptions-item>
          <el-descriptions-item label="提供商">
            {{ selectedModelInfo.provider }}
          </el-descriptions-item>
          <el-descriptions-item label="最大Token">
            {{ selectedModelInfo.maxTokens || '未指定' }}
          </el-descriptions-item>
          <el-descriptions-item label="描述">
            {{ selectedModelInfo.description || '暂无描述' }}
          </el-descriptions-item>
        </el-descriptions>
    </div>

    <!-- 错误提示 -->
    <el-alert v-if="error" :title="error" type="error" show-icon closable class="error-alert" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useLLMStore } from '@/stores/llm'
import { Setting, Close, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { defaultApiConfig } from '@/config'
import { ApiType, registerLlmModels } from '@/api/llmModel'
import { useUserStore } from '@/stores/user'

const store = useLLMStore()
const userStore = useUserStore()

// 控制注册模型到评测系统的加载状态
const isRegistering = ref(false)
const registerSuccess = ref(false)

// 表单引用
const configFormRef = ref<FormInstance>()

// 控制配置面板显示
const showConfig = ref(false)

// API配置
const apiConfig = ref({
  apiUrl: defaultApiConfig.apiUrl,
  apiKey: defaultApiConfig.apiKey,
  apiType: 'openai_compatible', // 默认为OpenAI兼容API
})

// API类型选项列表
const apiTypeOptions = [
  { label: 'OpenAI兼容API', value: ApiType.OPENAI_COMPATIBLE },
  { label: 'Anthropic API', value: ApiType.ANTHROPIC },
  { label: 'Google AI API', value: ApiType.GOOGLE_AI },
  { label: '百度文心一言API', value: ApiType.BAIDU },
  { label: 'Azure OpenAI API', value: ApiType.AZURE_OPENAI },
]

// 配置验证规则
const configRules = {
  apiKey: [
    { required: true, message: '请输入API密钥', trigger: 'blur' },
    { min: 32, message: 'API密钥长度不正确', trigger: 'blur' },
  ],
  apiUrl: [
    { required: true, message: '请输入API基础URL', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' },
  ],
}

// 模型选择
const modelSelection = ref({
  selectedModel: '',
})

// 切换配置面板显示状态的方法
const toggleConfig = () => {
  showConfig.value = !showConfig.value
}

const closeConfig = () => {
  showConfig.value = false
}

// 使用默认URL
const useDefaultUrl = () => {
  apiConfig.value.apiUrl = defaultApiConfig.apiUrl
}

// 保存配置到localStorage
const saveConfig = () => {
  // 保存API配置
  localStorage.setItem('apiConfig', JSON.stringify(apiConfig.value))
  window.apiConfig = { ...apiConfig.value }

  // 保存选中的模型
  if (modelSelection.value.selectedModel) {
    localStorage.setItem('selectedModel', modelSelection.value.selectedModel)
    window.selectedModel = modelSelection.value.selectedModel
  }
}

// 从localStorage中恢复配置
const restoreConfig = () => {
  // 恢复API配置
  const savedConfig = localStorage.getItem('apiConfig')
  if (savedConfig) {
    try {
      const config = JSON.parse(savedConfig)
      apiConfig.value = config
      window.apiConfig = config
    } catch (e) {
      console.error('解析保存的配置失败:', e)
    }
  } else {
    apiConfig.value = { ...defaultApiConfig }
  }

  // 恢复选中的模型
  const savedModel = localStorage.getItem('selectedModel')
  if (savedModel) {
    modelSelection.value.selectedModel = savedModel
    window.selectedModel = savedModel
  }
}

// 计算属性
const isFormValid = computed(() => {
  return apiConfig.value.apiUrl.trim() !== '' && apiConfig.value.apiKey.trim() !== ''
})

const selectedModelInfo = computed(() => {
  return store.availableModels.find((model) => model.id === modelSelection.value.selectedModel)
})

// 从store获取状态
const availableModels = computed(() => store.availableModels)
const isLoadingModels = computed(() => store.isLoadingModels)
const error = computed(() => store.error)

// 方法
const handleSave = async () => {
  if (!configFormRef.value) return

  try {
    // 重置注册状态
    resetRegistrationStatus()

    // 验证表单
    await configFormRef.value.validate()

    // 保存配置到localStorage和内存
    saveConfig()

    // 加载模型列表
    await store.fetchModels(apiConfig.value.apiUrl, apiConfig.value.apiKey, apiConfig.value.apiType)

    // 成功后关闭面板
    closeConfig()
    ElMessage.success('配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('配置保存失败，请检查输入是否正确')
  }
}

// 重置注册状态
const resetRegistrationStatus = () => {
  registerSuccess.value = false
  localStorage.removeItem('modelRegisteredForEvaluation')
  console.log('注册状态已重置')
}

const loadModels = async () => {
  if (!configFormRef.value) return

  try {
    // 重置注册状态
    resetRegistrationStatus()

    // 1. Validate the form first
    await configFormRef.value.validateField(['apiUrl', 'apiKey', 'apiType'])

    // 2. If validation passes, try fetching models
    try {
      ElMessage.info('正在加载模型列表...')
      // Save config before fetching
      saveConfig()
      // Fetch models
      await store.fetchModels(apiConfig.value.apiUrl, apiConfig.value.apiKey, apiConfig.value.apiType)

      // Check for errors reported by the store
      if (store.error) {
        ElMessage.error(`加载模型失败: ${store.error}`)
      } else {
        // ElMessage.success('模型加载成功') // 移除成功提示
      }
    } catch (fetchError) { // Catch errors specifically from fetchModels or saveConfig
      console.error('加载模型时发生错误:', fetchError)
      // Display error message, preferring the one from the store if available
      if (!store.error) {
        ElMessage.error('加载模型失败，请检查网络连接或API配置')
      } else {
        // If store.error was set by fetchModels, it might have already been shown,
        // but showing it again here ensures it's displayed if the initial check missed it.
        ElMessage.error(`加载模型失败: ${store.error}`)
      }
    }

  } catch (validationError) { // Catch validation errors
    // Validation errors are usually handled automatically by Element Plus messages.
    // Logging them might still be useful for debugging.
    console.log('API 配置验证失败:', validationError)
    // Optionally provide a generic message if Element Plus doesn't
    // ElMessage.warning('请先正确填写 API 配置')
  }
}

const registerForEvaluation = async () => {
  if (!modelSelection.value.selectedModel || !userStore.currentUser) {
    ElMessage.warning('请先选择模型并确保已登录')
    return
  }

  try {
    isRegistering.value = true

    // 构造注册请求数据
    const registrationData = {
      userId: userStore.currentUser.id,
      apiUrl: apiConfig.value.apiUrl,
      apiKey: apiConfig.value.apiKey,
      apiType: apiConfig.value.apiType
    }

    // 调用注册API
    const result = await registerLlmModels(registrationData)

    if (result.success) {
      ElMessage.success({
        message: '模型已成功注册到评测系统，感谢您的贡献！',
        duration: 5000
      })
      registerSuccess.value = true
      localStorage.setItem('modelRegisteredForEvaluation', 'true')
      isRegistering.value = false
    } else {
      // 处理特定错误情况
      if (result.message === '没有新模型被注册') {
        ElMessage.info({
          message: '您的模型API已经在评测系统中，无需重复提供',
          duration: 5000
        })
        registerSuccess.value = true
        localStorage.setItem('modelRegisteredForEvaluation', 'true')
        isRegistering.value = false
        return
      } else {
        ElMessage.warning({
          message: `注册未成功: ${result.message || '未知错误'}`,
          duration: 5000
        })
      }
    }
  } catch (error) {
    console.error('注册模型到评测系统失败:', error)

    // 尝试从错误对象中提取更详细的信息
    let errorMessage = '注册失败，请稍后重试'
    let isAlreadyRegistered = false

    if (error && typeof error === 'object') {
      // 检查是否有response.data.message字段 (axios错误格式)
      if (error.response && error.response.data) {
        const responseData = error.response.data
        console.log('错误响应数据:', responseData)

        if (responseData.message && (responseData.message.includes('没有新模型被注册') || responseData.message.includes('已经注册'))) {
          isAlreadyRegistered = true
        } else if (responseData.message) {
          errorMessage = `注册失败: ${responseData.message}`
        }

        // 检查状态码
        if (error.response.status === 400) {
          // 当收到400错误时，很可能是已注册情况
          isAlreadyRegistered = true
        }
      }
      // 检查是否有data.message字段
      else if (error.data && error.data.message) {
        if (error.data.message.includes('没有新模型被注册') || error.data.message.includes('已经注册')) {
          isAlreadyRegistered = true
        } else {
          errorMessage = `注册失败: ${error.data.message}`
        }
      }
      // 检查错误消息
      else if (error.message) {
        if (error.message.includes('400')) {
          // 当收到400错误但没有明确消息时，假设是已注册情况
          isAlreadyRegistered = true
        } else {
          errorMessage = `注册失败: ${error.message}`
        }
      }
    }

    // 处理已注册情况
    if (isAlreadyRegistered) {
      ElMessage.info({
        message: '您的模型API已经在评测系统中，无需重复提供',
        duration: 5000
      })
      registerSuccess.value = true
      localStorage.setItem('modelRegisteredForEvaluation', 'true')
      isRegistering.value = false
      return
    }

    // 显示错误消息
    ElMessage.error({
      message: errorMessage,
      duration: 5000
    })
  } finally {
    isRegistering.value = false
  }
}

// 监听API配置变化
watch(
  () => [apiConfig.value.apiUrl, apiConfig.value.apiKey, apiConfig.value.apiType],
  (newValues, oldValues) => {
    // 检查是否有变化
    if (
      newValues[0] !== oldValues[0] || // apiUrl变化
      newValues[1] !== oldValues[1] || // apiKey变化
      newValues[2] !== oldValues[2]    // apiType变化
    ) {
      console.log('API配置已变化，重置注册状态')
      resetRegistrationStatus()
    }
  }
)

// 监听模型选择变化
watch(
  () => modelSelection.value.selectedModel,
  (newModel, oldModel) => {
    if (newModel !== oldModel) {
      console.log('选择的模型已变化，重置注册状态')
      resetRegistrationStatus()
    }
  }
)

// 生命周期钩子
onMounted(() => {
  restoreConfig()
  // 添加事件监听
  window.addEventListener('toggle-model-config', toggleConfig)

  // 检查是否已经注册过模型
  const registeredStatus = localStorage.getItem('modelRegisteredForEvaluation')
  if (registeredStatus === 'true') {
    registerSuccess.value = true
  }
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('toggle-model-config', toggleConfig)
})

// 对外暴露的属性和方法
defineExpose({
  selectedModel: computed(() => modelSelection.value.selectedModel),
  apiConfig,
  toggleConfig,
  showConfig,
})
</script>

<style scoped>
.model-selector {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  overflow: visible;
}

.model-select-placeholder {
  height: 0; /* 或根据需要调整 */
}

.config-trigger {
  display: none; /* 隐藏独立的触发器，因为我们使用导航栏中的按钮 */
}

.popover-model-selector {
  margin-top: 16px;
}

.selector-with-button {
  display: flex;
  align-items: center;
  gap: 8px;
  /* 确保与输入框宽度保持一致 */
  width: 100%;
}

.flex-grow {
  flex-grow: 1;
}

.load-button {
  flex-shrink: 0;
  /* 设置固定宽度，你可以根据需要调整 */
  width: 90px;
}

/* 修正 API 配置表单的右侧内边距，确保与容器宽度一致 */
.api-config-form {
  margin-top: 16px;
  padding-right: 0;
}

/* 确保所有表单项都撑满容器宽度 */
:deep(.el-form-item) {
  width: 100%;
  margin-bottom: 18px;
}

/* 确保输入框容器撑满其父容器 */
:deep(.el-input), :deep(.el-select) {
  width: 100%;
}

.model-config {
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 8px;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.close-button {
  padding: 4px;
}

.form-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

.model-info {
  margin-top: 24px;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

.error-alert {
  margin-top: 20px;
}

/* 添加过渡动画 */
.el-collapse-transition {
  transition: all 0.3s ease-in-out;
}

/* 确保弹出框正确显示 */
:deep(.model-config-popover) {
  padding: 0;
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
  z-index: 3000;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.trigger-wrapper {
  display: contents;
}

/* 模型选项样式 */
.model-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

:deep(.el-select-dropdown__item) {
  padding: 8px 12px;
}

:deep(.el-tag) {
  margin-left: 8px;
}

.full-width {
  width: 100%; /* 确保选择器在 Popover 中占满宽度 */
}

.select-placeholder {
  height: 40px; /* 与 el-select 高度一致 */
  padding: 0 12px;
  display: flex;
  align-items: center;
  background-color: var(--el-fill-color-blank);
  border-radius: 4px;
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.register-button {
  margin-top: 0;
  margin-left: 0;
  width: 140px;
}

.register-hint {
  margin-top: 0;
  margin-left: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.evaluation-section {
  display: flex;
  align-items: center;
  margin-top: 10px;
  padding: 10px;
  border-radius: 6px;
  background-color: rgba(0, 200, 0, 0.05);
  border: 1px dashed var(--el-color-success);
}

.register-success {
  background-color: rgba(0, 200, 0, 0.1);
  border: 1px solid var(--el-color-success);
  animation: success-pulse 2s ease-in-out;
}

@keyframes success-pulse {
  0% {
    background-color: rgba(0, 200, 0, 0.05);
    border: 1px dashed var(--el-color-success);
  }
  50% {
    background-color: rgba(0, 200, 0, 0.2);
    border: 1px solid var(--el-color-success);
  }
  100% {
    background-color: rgba(0, 200, 0, 0.1);
    border: 1px solid var(--el-color-success);
  }
}
</style>
