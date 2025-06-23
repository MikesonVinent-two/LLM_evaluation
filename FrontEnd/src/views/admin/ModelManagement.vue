<template>
  <div class="model-management-container">
    <el-card class="model-management-card">
      <template #header>
        <div class="card-header">
          <h2>模型管理</h2>
          <div class="header-actions">
            <el-button
              type="primary"
              :icon="Plus"
              @click="showRegisterModelDialog">
              注册模型
            </el-button>
            <el-button
              type="info"
              :icon="Connection"
              @click="startSequentialTest"
              :loading="isTestingAll"
              :disabled="models.length === 0">
              {{ sequentialTestInProgress ? '暂停测试' : '测试所有模型' }}
            </el-button>
            <el-tooltip content="刷新模型列表" placement="top">
              <el-button
                :icon="Refresh"
                circle
                @click="fetchModels"
                :loading="isLoading">
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 模型列表 -->
      <div v-if="isLoading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="models.length === 0" class="empty-container">
        <el-empty description="暂无注册模型" />
        <el-button type="primary" @click="showRegisterModelDialog">注册模型</el-button>
      </div>
      <div v-else>
        <el-table
          :data="models"
          border
          stripe
          style="width: 100%"
          :row-class-name="getRowClassName">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="模型名称" min-width="150" />
          <el-table-column prop="provider" label="提供商" min-width="120" />
          <el-table-column prop="apiType" label="API类型" min-width="150">
            <template #default="scope">
              <el-tag :type="getApiTypeTagType(scope.row.apiType)">
                {{ formatApiType(scope.row.apiType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="scope">
              {{ scope.row.description || '暂无描述' }}
            </template>
          </el-table-column>
          <el-table-column label="连通状态" width="150">
            <template #default="scope">
              <div v-if="isModelTesting(scope.row.id)" class="status-testing">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>测试中...</span>
              </div>
              <div v-else-if="getModelStatus(scope.row.id)" class="status-info">
                <el-tag
                  :type="getModelStatus(scope.row.id).connected ? 'success' : 'danger'"
                  size="small">
                  {{ getModelStatus(scope.row.id).connected ? '已连接' : '未连接' }}
                </el-tag>
                <span v-if="getModelStatus(scope.row.id).connected" class="response-time">
                  {{ getModelStatus(scope.row.id).responseTime }}ms
                </span>
              </div>
              <div v-else class="status-unknown">
                <el-tag type="info" size="small">未测试</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="primary"
                  :icon="Connection"
                  @click="testSingleModel(scope.row.id)"
                  :loading="isModelTesting(scope.row.id)">
                  测试
                </el-button>
                <el-popconfirm
                  :title="`确认删除模型 ${scope.row.name}?`"
                  width="220"
                  confirm-button-text="确认"
                  cancel-button-text="取消"
                  @confirm="deleteModel(scope.row.id)">
                  <template #reference>
                    <el-button
                      size="small"
                      type="danger"
                      :icon="Delete"
                      :loading="isDeletingModel === scope.row.id">
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 系统测试结果 -->
      <div v-if="systemTestResult || sequentialTestInProgress" class="system-test-result">
        <el-divider>测试进度</el-divider>

        <div v-if="sequentialTestInProgress" class="sequential-test-progress">
          <el-progress
            :percentage="testProgress"
            :status="sequentialTestPaused ? 'warning' : ''"
            :stroke-width="15"
            :format="percentageFormat" />

          <div class="test-controls">
            <el-button
              type="primary"
              :icon="sequentialTestPaused ? VideoPlay : VideoPause"
              @click="toggleSequentialTest"
              size="small">
              {{ sequentialTestPaused ? '继续测试' : '暂停测试' }}
            </el-button>
            <el-button
              type="danger"
              :icon="CircleClose"
              @click="stopSequentialTest"
              size="small">
              停止测试
            </el-button>
          </div>

          <div class="test-info">
            <p>正在测试: {{ currentTestingModelName || '准备中...' }}</p>
            <p>已测试: {{ testedCount }} / {{ models.length }}</p>
            <p>成功: <span class="success-count">{{ passedCount }}</span></p>
            <p>失败: <span class="failed-count">{{ failedCount }}</span></p>
          </div>
        </div>

        <div v-if="systemTestResult" class="system-test-summary">
          <el-descriptions
            :column="4"
            border
            size="small"
            class="result-descriptions">
            <el-descriptions-item label="总模型数">
              {{ systemTestResult.totalModels }}
            </el-descriptions-item>
            <el-descriptions-item label="连接成功">
              <span class="success-count">{{ systemTestResult.passedModels }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="连接失败">
              <span class="failed-count">{{ systemTestResult.failedModels }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="测试用时">
              {{ systemTestResult.testDuration }} ms
            </el-descriptions-item>
          </el-descriptions>

          <div class="test-timestamp">
            <el-text type="info">
              测试时间: {{ formatTimestamp(systemTestResult.timestamp) }}
            </el-text>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 注册模型对话框 -->
    <el-dialog
      v-model="registerModelDialogVisible"
      title="注册新模型"
      width="500px"
      :close-on-click-modal="false">
      <ModelSelector ref="modelSelectorRef" />

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="registerModelDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="registerModel" :loading="isRegistering">
            注册
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Connection, Delete, Loading,
  VideoPlay, VideoPause, CircleClose
} from '@element-plus/icons-vue'
import {
  getRegisteredLlmModels,
  testSystemModelConnectivity,
  testSingleModelConnectivity,
  deleteLlmModel,
  type ModelInfo,
  type ModelConnectivityTestResult,
  type SystemConnectivityTestResult,
  type SingleModelConnectivityTestResult
} from '@/api/llmModel'
import { useUserStore } from '@/stores/user'
import ModelSelector from '@/components/ModelSelector.vue'

// 存储模型列表
const models = ref<ModelInfo[]>([])
// 加载状态
const isLoading = ref(false)
// 测试中的模型ID
const testingModels = ref<number[]>([])
// 存储模型状态
const modelStatus = ref<Map<number, ModelConnectivityTestResult>>(new Map())
// 系统测试结果
const systemTestResult = ref<SystemConnectivityTestResult | null>(null)
// 是否正在测试所有模型
const isTestingAll = ref(false)
// 是否正在删除模型
const isDeletingModel = ref<number | null>(null)

// 注册模型相关
const registerModelDialogVisible = ref(false)
const modelSelectorRef = ref<InstanceType<typeof ModelSelector> | null>(null)
const isRegistering = ref(false)

// 用户store
const userStore = useUserStore()

// 顺序测试相关状态
const sequentialTestInProgress = ref(false)
const sequentialTestPaused = ref(false)
const currentTestIndex = ref(0)
const currentTestingModelId = ref<number | null>(null)
const currentTestingModelName = ref<string | null>(null)
const testQueue = ref<number[]>([])
const testedModels = ref<Set<number>>(new Set())
const passedCount = ref(0)
const failedCount = ref(0)
const testTimeout = ref(300000) // 默认测试超时时间为30秒
const currentTestTimeoutId = ref<number | null>(null)

// 计算已测试模型数量
const testedCount = computed(() => testedModels.value.size)

// 计算测试进度百分比
const testProgress = computed(() => {
  if (models.value.length === 0) return 0
  return Math.floor((testedCount.value / models.value.length) * 100)
})

// 格式化进度百分比显示
const percentageFormat = (percentage: number) => {
  return sequentialTestPaused.value ? '已暂停' : `${percentage}%`
}

// 获取模型列表
const fetchModels = async () => {
  try {
    isLoading.value = true
    const response = await getRegisteredLlmModels()
    if (response.success) {
      models.value = response.models
    } else {
      ElMessage.error('获取模型列表失败')
    }
  } catch (error) {
    console.error('获取模型列表出错:', error)
    ElMessage.error('获取模型列表失败')
  } finally {
    isLoading.value = false
  }
}

// 测试单个模型
const testSingleModel = async (modelId: number) => {
  if (testingModels.value.includes(modelId)) return

  try {
    testingModels.value.push(modelId)
    const result = await testSingleModelConnectivity(modelId)

    if (result.success) {
      // 更新模型状态
      modelStatus.value.set(modelId, {
        connected: result.connected,
        modelName: result.modelName,
        apiEndpoint: result.apiEndpoint,
        modelId: result.modelId,
        provider: result.provider,
        responseTime: result.responseTime,
        error: result.error
      })

      ElMessage({
        type: result.connected ? 'success' : 'error',
        message: result.connected
          ? `模型 ${result.modelName} 连接成功，响应时间: ${result.responseTime}ms`
          : `模型 ${result.modelName} 连接失败: ${result.error || '未知错误'}`
      })
    } else {
      ElMessage.error(`测试模型连接失败`)
    }
  } catch (error) {
    console.error('测试模型连接出错:', error)
    ElMessage.error(`测试模型连接出错`)
  } finally {
    testingModels.value = testingModels.value.filter(id => id !== modelId)
  }
}

// 测试所有模型
const testAllModels = async () => {
  if (isTestingAll.value) return

  try {
    isTestingAll.value = true
    const result = await testSystemModelConnectivity()

    if (result.success) {
      systemTestResult.value = result

      // 更新单个模型状态
      result.modelResults.forEach(modelResult => {
        modelStatus.value.set(modelResult.modelId, modelResult)
      })

      ElMessage.success(`模型连通性测试完成: ${result.passedModels}/${result.totalModels} 成功`)
    } else {
      ElMessage.error('测试所有模型连接失败')
    }
  } catch (error) {
    console.error('测试所有模型连接出错:', error)
    ElMessage.error('测试所有模型连接出错')
  } finally {
    isTestingAll.value = false
  }
}

// 删除模型
const deleteModel = async (modelId: number) => {
  isDeletingModel.value = modelId

  try {
    const result = await deleteLlmModel(modelId)

    if (result.success) {
      ElMessage.success(result.message || '模型删除成功')
      // 更新模型列表
      models.value = models.value.filter(model => model.id !== modelId)
      // 清除该模型的状态
      modelStatus.value.delete(modelId)
    } else {
      ElMessage.error(result.message || '模型删除失败')
    }
  } catch (error) {
    console.error('删除模型出错:', error)
    ElMessage.error('删除模型失败')
  } finally {
    isDeletingModel.value = null
  }
}

// 获取行类名
const getRowClassName = ({ row }: { row: ModelInfo }) => {
  const status = modelStatus.value.get(row.id)
  if (!status) return ''
  return status.connected ? 'connected-row' : 'disconnected-row'
}

// 获取模型状态
const getModelStatus = (modelId: number) => {
  return modelStatus.value.get(modelId)
}

// 判断模型是否正在测试
const isModelTesting = (modelId: number) => {
  return testingModels.value.includes(modelId)
}

// 格式化API类型
const formatApiType = (apiType: string) => {
  switch (apiType) {
    case 'openai_compatible':
      return 'OpenAI 兼容'
    case 'anthropic':
      return 'Anthropic'
    case 'google_ai':
      return 'Google AI'
    case 'baidu':
      return '百度文心一言'
    case 'azure_openai':
      return 'Azure OpenAI'
    default:
      return apiType
  }
}

// 获取API类型标签样式
const getApiTypeTagType = (apiType: string) => {
  switch (apiType) {
    case 'openai_compatible':
      return 'success'
    case 'anthropic':
      return 'warning'
    case 'google_ai':
      return 'primary'
    case 'baidu':
      return 'danger'
    case 'azure_openai':
      return 'info'
    default:
      return ''
  }
}

// 格式化时间戳
const formatTimestamp = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

// 显示注册模型对话框
const showRegisterModelDialog = () => {
  registerModelDialogVisible.value = true
}

// 注册模型
const registerModel = async () => {
  if (!modelSelectorRef.value) return

  try {
    isRegistering.value = true
    // 调用ModelSelector组件中的registerForEvaluation方法
    await modelSelectorRef.value.registerForEvaluation()
    // 成功后关闭对话框并刷新模型列表
    registerModelDialogVisible.value = false
    fetchModels()
  } catch (error) {
    console.error('注册模型出错:', error)
  } finally {
    isRegistering.value = false
  }
}

// 开始顺序测试所有模型
const startSequentialTest = () => {
  if (sequentialTestInProgress.value) {
    // 如果已经在测试中，则切换暂停状态
    toggleSequentialTest()
    return
  }

  // 检查是否有模型可测试
  if (models.value.length === 0) {
    ElMessage.warning('没有可测试的模型')
    return
  }

  // 初始化测试状态
  currentTestIndex.value = 0
  passedCount.value = 0
  failedCount.value = 0
  sequentialTestInProgress.value = true
  sequentialTestPaused.value = false

  // 构建测试队列，仅包含未测试的模型
  testQueue.value = models.value
    .filter(model => !modelStatus.value.has(model.id))
    .map(model => model.id)

  if (testQueue.value.length === 0) {
    // 所有模型已测试
    ElMessage.info('所有模型已经测试过，将重新测试所有模型')
    testQueue.value = models.value.map(model => model.id)
    testedModels.value.clear()
  }

  // 开始测试第一个模型
  continueSequentialTest()
}

// 继续顺序测试
const continueSequentialTest = async () => {
  if (!sequentialTestInProgress.value || sequentialTestPaused.value) return

  if (currentTestIndex.value >= testQueue.value.length) {
    // 所有模型测试完成
    finishSequentialTest()
    return
  }

  const modelId = testQueue.value[currentTestIndex.value]
  currentTestingModelId.value = modelId
  currentTestingModelName.value = models.value.find(m => m.id === modelId)?.name || `模型 ${modelId}`

  try {
    // 如果模型已经测试过，跳过
    if (testedModels.value.has(modelId)) {
      currentTestIndex.value++
      continueSequentialTest()
      return
    }

    // 设置测试超时
    const timeoutId = window.setTimeout(() => {
      // 如果超时，标记为失败并继续下一个
      if (currentTestingModelId.value === modelId) {
        console.warn(`测试模型 ${modelId} 超时`)

        // 更新模型状态为连接失败
        modelStatus.value.set(modelId, {
          connected: false,
          modelName: currentTestingModelName.value || `模型 ${modelId}`,
          apiEndpoint: '未知',
          modelId: modelId,
          provider: '未知',
          responseTime: testTimeout.value,
          error: '测试请求超时'
        })

        failedCount.value++
        testedModels.value.add(modelId)

        // 继续测试下一个
        currentTestIndex.value++
        continueSequentialTest()
      }
    }, testTimeout.value)

    currentTestTimeoutId.value = timeoutId as unknown as number

    // 测试当前模型
    const result = await testSingleModelConnectivity(modelId)

    // 清除超时计时器
    if (currentTestTimeoutId.value) {
      clearTimeout(currentTestTimeoutId.value)
      currentTestTimeoutId.value = null
    }

    if (result.success) {
      // 更新模型状态
      modelStatus.value.set(modelId, {
        connected: result.connected,
        modelName: result.modelName,
        apiEndpoint: result.apiEndpoint,
        modelId: result.modelId,
        provider: result.provider,
        responseTime: result.responseTime,
        error: result.error
      })

      // 更新测试计数
      if (result.connected) {
        passedCount.value++
      } else {
        failedCount.value++
      }

      // 标记为已测试
      testedModels.value.add(modelId)
    }
  } catch (error) {
    // 清除超时计时器
    if (currentTestTimeoutId.value) {
      clearTimeout(currentTestTimeoutId.value)
      currentTestTimeoutId.value = null
    }

    console.error(`测试模型 ${modelId} 出错:`, error)
    failedCount.value++

    // 更新模型状态为连接失败
    modelStatus.value.set(modelId, {
      connected: false,
      modelName: currentTestingModelName.value || `模型 ${modelId}`,
      apiEndpoint: '未知',
      modelId: modelId,
      provider: '未知',
      responseTime: 0,
      error: error instanceof Error ? error.message : '未知错误'
    })

    testedModels.value.add(modelId) // 仍标记为已测试
  } finally {
    // 继续测试下一个模型
    currentTestIndex.value++

    // 使用setTimeout避免阻塞UI
    setTimeout(() => {
      if (sequentialTestInProgress.value && !sequentialTestPaused.value) {
        continueSequentialTest()
      }
    }, 500) // 增加500ms延迟，避免API请求过快
  }
}

// 切换测试暂停/继续状态
const toggleSequentialTest = () => {
  sequentialTestPaused.value = !sequentialTestPaused.value

  if (!sequentialTestPaused.value) {
    // 如果从暂停状态恢复，继续测试
    continueSequentialTest()
  }

  ElMessage.info(sequentialTestPaused.value ? '测试已暂停' : '测试已继续')
}

// 停止测试
const stopSequentialTest = () => {
  sequentialTestInProgress.value = false
  sequentialTestPaused.value = false
  currentTestingModelId.value = null
  currentTestingModelName.value = null

  // 清除超时计时器
  if (currentTestTimeoutId.value) {
    clearTimeout(currentTestTimeoutId.value)
    currentTestTimeoutId.value = null
  }

  ElMessage.info('测试已停止')

  // 生成测试报告
  generateTestReport()
}

// 完成测试
const finishSequentialTest = () => {
  sequentialTestInProgress.value = false
  sequentialTestPaused.value = false
  currentTestingModelId.value = null
  currentTestingModelName.value = null
  ElMessage.success('所有模型测试完成')

  // 生成测试报告
  generateTestReport()
}

// 生成测试报告
const generateTestReport = () => {
  // 创建模拟的系统测试结果
  systemTestResult.value = {
    totalModels: models.value.length,
    modelResults: Array.from(modelStatus.value.values()),
    failedModels: failedCount.value,
    success: true,
    passedModels: passedCount.value,
    testDuration: 0, // 这个值可以计算测试持续时间
    timestamp: Date.now()
  }
}

// 生命周期钩子
onMounted(() => {
  fetchModels()
})

// 在组件卸载前停止所有测试
onBeforeUnmount(() => {
  if (sequentialTestInProgress.value) {
    stopSequentialTest()
  }

  // 确保清除所有计时器
  if (currentTestTimeoutId.value) {
    clearTimeout(currentTestTimeoutId.value)
    currentTestTimeoutId.value = null
  }
})
</script>

<style scoped>
.model-management-container {
  padding: 20px;
}

.model-management-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.empty-container .el-button {
  margin-top: 20px;
}

.loading-container {
  padding: 20px 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.system-test-result {
  margin-top: 24px;
}

.result-descriptions {
  margin-top: 16px;
}

.test-timestamp {
  margin-top: 12px;
  text-align: right;
}

.success-count {
  color: #67c23a;
  font-weight: bold;
}

.failed-count {
  color: #f56c6c;
  font-weight: bold;
}

.status-testing {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.response-time {
  font-size: 12px;
  color: #67c23a;
}

:deep(.connected-row) {
  background-color: rgba(103, 194, 58, 0.1);
}

:deep(.disconnected-row) {
  background-color: rgba(245, 108, 108, 0.1);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.sequential-test-progress {
  margin-top: 16px;
  padding: 16px;
  border-radius: 8px;
  background-color: #f8f9fa;
  border: 1px solid #e4e7ed;
}

.test-controls {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.test-info {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.test-info p {
  margin: 0;
  font-size: 14px;
}

.system-test-summary {
  margin-top: 16px;
}
</style>
