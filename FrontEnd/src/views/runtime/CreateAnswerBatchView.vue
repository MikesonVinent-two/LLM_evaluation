<template>
  <div class="create-answer-batch">
    <el-page-header @back="goBack" title="返回" content="创建回答生成批次" />

    <div class="page-container">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <h2>创建回答生成批次</h2>
                <el-tooltip content="创建新的回答生成批次并立即启动">
                  <el-icon><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
            </template>
            <div class="card-content">
              <p>在此页面创建和启动新的回答生成批次。创建批次需要选择数据集版本、回答组装配置、LLM模型以及题型提示词。完成配置后，可以选择保存批次或立即启动批次。</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="content-row">
        <el-col :span="24">
          <el-card>
            <template #header>
              <div class="card-header">
                <h2>批次配置</h2>
                <el-tag type="info">所有带*的字段为必填项</el-tag>
              </div>
            </template>

            <el-form :model="batchForm" label-width="140px" :rules="rules" ref="batchFormRef" class="batch-form">
              <el-divider content-position="left">基本信息</el-divider>

              <el-form-item label="批次名称" prop="name">
                <el-input v-model="batchForm.name" placeholder="请输入批次名称" style="width: 500px" />
              </el-form-item>

              <el-form-item label="批次描述" prop="description">
                <el-input v-model="batchForm.description" type="textarea" placeholder="请输入批次描述" style="width: 500px" />
              </el-form-item>

              <el-divider content-position="left">数据来源</el-divider>

              <el-form-item label="数据集版本" prop="datasetVersionId">
                <div class="selection-container">
                  <el-select
                    v-model="batchForm.datasetVersionId"
                    placeholder="请选择数据集版本"
                    filterable
                    @change="handleDatasetVersionChange"
                    :loading="loading.datasetVersions"
                    style="width: 500px"
                  >
                    <el-option
                      v-for="item in datasetVersions"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button type="primary" link @click="refreshDatasetVersions">
                    <el-icon class="action-icon"><Refresh /></el-icon>
                    刷新列表
                  </el-button>
                </div>
              </el-form-item>

              <el-divider content-position="left">配置与模型</el-divider>

              <el-form-item label="回答组装配置" prop="answerAssemblyConfigId">
                <div class="selection-container">
                  <el-select
                    v-model="batchForm.answerAssemblyConfigId"
                    placeholder="请选择回答组装配置"
                    filterable
                    :loading="loading.assemblyConfigs"
                    style="width: 500px"
                  >
                    <el-option
                      v-for="item in answerAssemblyConfigs"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    >
                      <span style="float: left">{{ item.name }}</span>
                      <span style="float: right; color: #8492a6; font-size: 13px">
                        {{ item.createdByUsername }}
                      </span>
                    </el-option>
                  </el-select>
                  <el-button type="primary" link @click="refreshAssemblyConfigs">
                    <el-icon class="action-icon"><Refresh /></el-icon>
                    刷新列表
                  </el-button>
                </div>
              </el-form-item>

              <el-form-item label="LLM模型" prop="llmModelIds">
                <div class="model-selection-container">
                  <el-select
                    v-model="batchForm.llmModelIds"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    :loading="loading.llmModels"
                    placeholder="请选择LLM模型"
                    class="model-select"
                    style="width: 500px"
                  >
                    <el-option
                      v-for="item in llmModels"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    >
                      <div class="model-option">
                        <div class="model-info">
                          <span class="model-name">{{ item.name }}</span>
                          <el-tag size="small" type="info" class="model-provider">{{ item.provider }}</el-tag>
                        </div>
                      </div>
                    </el-option>
                  </el-select>
                </div>
                <div class="model-actions">
                  <el-button type="primary" link @click="refreshLLMModels">
                    <el-icon class="action-icon"><Refresh /></el-icon>
                    刷新列表
                  </el-button>
                  <el-button type="success" link @click="testModelConnectivity">
                    <el-icon class="action-icon"><Connection /></el-icon>
                    测试连通性
                  </el-button>
                </div>
                <div class="help-text">选择要用于生成回答的LLM模型，可以选择多个模型</div>
              </el-form-item>

              <el-form-item label="回答重复次数" prop="answerRepeatCount">
                <el-input-number v-model="batchForm.answerRepeatCount" :min="1" :max="10" />
                <div class="help-text">每个问题使用每个模型生成的回答次数</div>
              </el-form-item>

              <el-divider content-position="left">题型提示词配置</el-divider>

              <el-form-item label="单选题提示词" prop="singleChoicePromptId">
                <el-select
                  v-model="batchForm.singleChoicePromptId"
                  placeholder="请选择单选题提示词"
                  filterable
                  :loading="loading.singleChoicePrompts"
                >
                  <el-option
                    v-for="item in questionTypePrompts[QuestionType.SINGLE_CHOICE]"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" link @click="refreshQuestionTypePrompts(QuestionType.SINGLE_CHOICE)">刷新</el-button>
              </el-form-item>

              <el-form-item label="多选题提示词" prop="multipleChoicePromptId">
                <el-select
                  v-model="batchForm.multipleChoicePromptId"
                  placeholder="请选择多选题提示词"
                  filterable
                  :loading="loading.multipleChoicePrompts"
                >
                  <el-option
                    v-for="item in questionTypePrompts[QuestionType.MULTIPLE_CHOICE]"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" link @click="refreshQuestionTypePrompts(QuestionType.MULTIPLE_CHOICE)">刷新</el-button>
              </el-form-item>

              <el-form-item label="简单事实题提示词" prop="simpleFactPromptId">
                <el-select
                  v-model="batchForm.simpleFactPromptId"
                  placeholder="请选择简单事实题提示词"
                  filterable
                  :loading="loading.simpleFactPrompts"
                >
                  <el-option
                    v-for="item in questionTypePrompts[QuestionType.SIMPLE_FACT]"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" link @click="refreshQuestionTypePrompts(QuestionType.SIMPLE_FACT)">刷新</el-button>
              </el-form-item>

              <el-form-item label="主观题提示词" prop="subjectivePromptId">
                <el-select
                  v-model="batchForm.subjectivePromptId"
                  placeholder="请选择主观题提示词"
                  filterable
                  :loading="loading.subjectivePrompts"
                >
                  <el-option
                    v-for="item in questionTypePrompts[QuestionType.SUBJECTIVE]"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
                <el-button type="primary" link @click="refreshQuestionTypePrompts(QuestionType.SUBJECTIVE)">刷新</el-button>
              </el-form-item>

              <el-divider content-position="left">高级参数</el-divider>

              <el-form-item label="全局参数">
                <el-button type="primary" size="small" @click="showGlobalParamsDialog">配置全局参数</el-button>
                <div class="param-preview" v-if="hasGlobalParams">
                  <span v-for="(value, key) in batchForm.globalParameters" :key="key">
                    {{ key }}: {{ value }}&nbsp;&nbsp;
                  </span>
                </div>
                <div class="help-text" v-else>未配置全局参数</div>
              </el-form-item>

              <el-form-item label="模型特定参数" v-if="batchForm.llmModelIds && batchForm.llmModelIds.length > 0">
                <el-button type="primary" size="small" @click="showModelParamsDialog">配置模型参数</el-button>
                <div v-if="hasModelParams">
                  <div class="param-preview" v-for="(params, modelId) in batchForm.modelSpecificParameters" :key="modelId">
                    {{ getModelNameById(Number(modelId)) }}:
                    <span v-for="(value, key) in params" :key="key">
                      {{ key }}: {{ value }}&nbsp;&nbsp;
                    </span>
                  </div>
                </div>
                <div class="help-text" v-else>未配置模型特定参数</div>
              </el-form-item>
            </el-form>

            <div class="form-actions">
              <el-button @click="goBack">取消</el-button>
              <el-button type="primary" @click="testBatchConfig" :loading="loading.test">测试配置</el-button>
              <el-button type="success" @click="createAndStartBatch" :loading="loading.start">创建并启动</el-button>
              <el-button type="info" @click="createBatch" :loading="loading.create">仅创建批次</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 全局参数对话框 -->
    <el-dialog v-model="dialogs.globalParams" title="配置全局参数" width="50%">
      <el-form :model="globalParamsForm">
        <el-form-item label="参数名" v-for="(param, index) in globalParamsArray" :key="index">
          <el-row :gutter="10">
            <el-col :span="10">
              <el-input v-model="param.key" placeholder="参数名称" />
            </el-col>
            <el-col :span="10">
              <el-input v-model="param.value" placeholder="参数值" />
            </el-col>
            <el-col :span="4">
              <el-button type="danger" @click="removeGlobalParam(index)">删除</el-button>
            </el-col>
          </el-row>
        </el-form-item>
        <el-button type="primary" @click="addGlobalParam">添加参数</el-button>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogs.globalParams = false">取消</el-button>
          <el-button type="primary" @click="saveGlobalParams">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 模型特定参数对话框 -->
    <el-dialog v-model="dialogs.modelParams" title="配置模型特定参数" width="60%">
      <el-tabs v-model="currentModelTab">
        <el-tab-pane
          v-for="modelId in batchForm.llmModelIds"
          :key="modelId"
          :label="getModelNameById(modelId)"
          :name="modelId.toString()"
        >
          <el-form>
            <el-form-item label="temperature">
              <el-slider
                v-model="modelParamsForms[modelId].temperature"
                :min="0"
                :max="1"
                :step="0.01"
                show-input
              />
            </el-form-item>
            <el-form-item label="max_tokens">
              <el-slider
                v-model="modelParamsForms[modelId].max_tokens"
                :min="100"
                :max="4000"
                :step="100"
                show-input
              />
            </el-form-item>
            <!-- 可以根据需要添加更多模型参数 -->
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogs.modelParams = false">取消</el-button>
          <el-button type="primary" @click="saveModelParams">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 连通性测试结果对话框 -->
    <el-dialog v-model="dialogs.connectivityTest" title="模型连通性测试结果" width="70%">
      <el-result
        :icon="connectivityTestResult.success ? 'success' : 'error'"
        :title="connectivityTestResult.success ? '连接测试成功' : '连接测试失败'"
        :sub-title="`测试了 ${connectivityTestResult.totalModels} 个模型，成功 ${connectivityTestResult.passedModels} 个，失败 ${connectivityTestResult.failedModels} 个。测试用时: ${connectivityTestResult.testDuration}ms`"
      >
      </el-result>

      <el-table :data="connectivityTestResult.modelResults" style="width: 100%">
        <el-table-column prop="modelName" label="模型名称" />
        <el-table-column prop="provider" label="提供商" />
        <el-table-column prop="apiEndpoint" label="API终端" width="250" show-overflow-tooltip />
        <el-table-column prop="responseTime" label="响应时间(ms)" />
        <el-table-column prop="connected" label="连接状态">
          <template #default="scope">
            <el-tag :type="scope.row.connected ? 'success' : 'danger'">
              {{ scope.row.connected ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.error || '无' }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="dialogs.connectivityTest = false">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Refresh, Connection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  createAnswerGenerationBatch,
  startAnswerGenerationBatch,
  type AnswerGenerationBatchCreateData,
  type GlobalParameters,
  type ModelSpecificParameters,
} from '@/api/answerGenerationApis'
import { testSingleModelConnectivity, type SingleModelConnectivityTestResult } from '@/api/llmModel'
import { getActiveAnswerPromptAssemblyConfigs } from '@/api/answerPromptAssemblyConfig'
import { getRegisteredLlmModels } from '@/api/llmModel'
import { getAllDatasetVersions } from '@/api/dataset'
import type { DatasetVersionResponse } from '@/types/dataset'
import { getActiveAnswerTypePromptsByType, QuestionType, type AnswerTypePrompt } from '@/api/answerTypePrompt'

const router = useRouter()
const userStore = useUserStore()
const batchFormRef = ref<FormInstance>()

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  datasetVersionId: [{ required: true, message: '请选择数据集版本', trigger: 'change' }],
  answerAssemblyConfigId: [{ required: true, message: '请选择回答组装配置', trigger: 'change' }],
  llmModelIds: [{ required: true, message: '请选择至少一个LLM模型', trigger: 'change' }],
}

// 批次表单数据
const batchForm = reactive<Partial<AnswerGenerationBatchCreateData>>({
  name: '',
  description: '',
  datasetVersionId: undefined,
  answerAssemblyConfigId: undefined,
  llmModelIds: [],
  answerRepeatCount: 1,
  singleChoicePromptId: undefined,
  multipleChoicePromptId: undefined,
  simpleFactPromptId: undefined,
  subjectivePromptId: undefined,
  globalParameters: { param1: '', param2: '' } as GlobalParameters,
  modelSpecificParameters: {} as ModelSpecificParameters
})

// 加载状态
const loading = reactive({
  datasetVersions: false,
  assemblyConfigs: false,
  llmModels: false,
  singleChoicePrompts: false,
  multipleChoicePrompts: false,
  simpleFactPrompts: false,
  subjectivePrompts: false,
  create: false,
  start: false,
  test: false
})

// 对话框状态
const dialogs = reactive({
  globalParams: false,
  modelParams: false,
  connectivityTest: false
})

// 数据源
const datasetVersions = ref<{ id: number; name: string }[]>([])
const answerAssemblyConfigs = ref<{ id: number; name: string; createdByUsername: string }[]>([])
const llmModels = ref<{ id: number; name: string; provider: string }[]>([])
const questionTypePrompts = reactive<Record<QuestionType, AnswerTypePrompt[]>>({
  [QuestionType.SINGLE_CHOICE]: [],
  [QuestionType.MULTIPLE_CHOICE]: [],
  [QuestionType.SIMPLE_FACT]: [],
  [QuestionType.SUBJECTIVE]: []
})

// 全局参数表单
const globalParamsForm = reactive({})
const globalParamsArray = ref<{ key: string; value: string }[]>([])

// 模型特定参数表单
const modelParamsForms = reactive<Record<number, { temperature?: number; max_tokens?: number }>>({})
const currentModelTab = ref('')

// 连通性测试结果
const connectivityTestResult = reactive({
  success: false,
  totalModels: 0,
  passedModels: 0,
  failedModels: 0,
  testDuration: 0,
  modelResults: [] as SingleModelConnectivityTestResult[]
})

// 计算是否有全局参数
const hasGlobalParams = computed(() => {
  return batchForm.globalParameters && Object.keys(batchForm.globalParameters).length > 0
})

// 计算是否有模型特定参数
const hasModelParams = computed(() => {
  return batchForm.modelSpecificParameters && Object.keys(batchForm.modelSpecificParameters).length > 0
})

// 获取模型名称
const getModelNameById = (modelId: number) => {
  const model = llmModels.value.find(m => m.id === modelId)
  return model ? model.name : `模型 #${modelId}`
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 加载数据集版本
const loadDatasetVersions = async () => {
  loading.datasetVersions = true
  try {
    const response = await getAllDatasetVersions()
    datasetVersions.value = response.map((version: DatasetVersionResponse) => ({
      id: version.id,
      name: version.name
    }))
  } catch (error) {
    ElMessage.error('加载数据集版本失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.datasetVersions = false
  }
}

// 刷新数据集版本列表
const refreshDatasetVersions = () => {
  loadDatasetVersions()
}

// 加载回答组装配置
const loadAnswerAssemblyConfigs = async () => {
  loading.assemblyConfigs = true
  try {
    const response = await getActiveAnswerPromptAssemblyConfigs({ page: 0, size: 100 })
    answerAssemblyConfigs.value = response.configs.map(config => ({
      id: config.id,
      name: config.name,
      createdByUsername: config.createdByUsername
    }))
  } catch (error) {
    ElMessage.error('加载回答组装配置失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.assemblyConfigs = false
  }
}

// 刷新回答组装配置列表
const refreshAssemblyConfigs = () => {
  loadAnswerAssemblyConfigs()
}

// 加载LLM模型
const loadLLMModels = async () => {
  loading.llmModels = true
  try {
    const response = await getRegisteredLlmModels()
    llmModels.value = response.models.map(model => ({
      id: model.id,
      name: model.name,
      provider: model.provider
    }))
  } catch (error) {
    ElMessage.error('加载LLM模型失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.llmModels = false
  }
}

// 刷新LLM模型列表
const refreshLLMModels = () => {
  loadLLMModels()
}

// 加载题型提示词
const loadQuestionTypePrompts = async (type: QuestionType) => {
  const loadingKey = `${type.toLowerCase()}Prompts` as keyof typeof loading
  loading[loadingKey] = true

  try {
    const response = await getActiveAnswerTypePromptsByType(type)
    questionTypePrompts[type] = response
  } catch (error) {
    ElMessage.error(`加载${type}提示词失败: ` + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading[loadingKey] = false
  }
}

// 刷新题型提示词列表
const refreshQuestionTypePrompts = (type: QuestionType) => {
  loadQuestionTypePrompts(type)
}

// 数据集版本变化处理
const handleDatasetVersionChange = (/* value: number */) => {
  // 这里可以根据选择的数据集版本加载相关信息
  // 例如统计数据集中有多少问题等
}

// 显示全局参数对话框
const showGlobalParamsDialog = () => {
  // 转换对象为数组形式便于编辑
  globalParamsArray.value = []
  if (batchForm.globalParameters) {
    for (const [key, value] of Object.entries(batchForm.globalParameters)) {
      globalParamsArray.value.push({ key, value: value as string })
    }
  }
  dialogs.globalParams = true
}

// 添加全局参数
const addGlobalParam = () => {
  globalParamsArray.value.push({ key: '', value: '' })
}

// 移除全局参数
const removeGlobalParam = (index: number) => {
  globalParamsArray.value.splice(index, 1)
}

// 保存全局参数
const saveGlobalParams = () => {
  const params: GlobalParameters = { param1: '', param2: '' }
  for (const param of globalParamsArray.value) {
    if (param.key === 'param1' || param.key === 'param2') {
      params[param.key] = param.value
    }
  }
  batchForm.globalParameters = params
  dialogs.globalParams = false
}

// 显示模型参数对话框
const showModelParamsDialog = () => {
  // 初始化模型参数表单
  if (batchForm.llmModelIds && batchForm.llmModelIds.length > 0) {
    for (const modelId of batchForm.llmModelIds) {
      if (!modelParamsForms[modelId]) {
        // 从已有配置中获取，或使用默认值
        const existingParams = batchForm.modelSpecificParameters && batchForm.modelSpecificParameters[modelId]
        modelParamsForms[modelId] = {
          temperature: existingParams?.temperature || 0.7,
          max_tokens: existingParams?.max_tokens || 1000
        }
      }
    }
    // 设置默认选项卡
    currentModelTab.value = batchForm.llmModelIds[0].toString()
    dialogs.modelParams = true
  } else {
    ElMessage.warning('请先选择LLM模型')
  }
}

// 保存模型参数
const saveModelParams = () => {
  if (!batchForm.modelSpecificParameters) {
    batchForm.modelSpecificParameters = {}
  }

  for (const modelId of batchForm.llmModelIds || []) {
    if (modelParamsForms[modelId]) {
      batchForm.modelSpecificParameters[modelId] = {
        ...modelParamsForms[modelId]
      }
    }
  }

  dialogs.modelParams = false
}

// 测试模型连通性
const testModelConnectivity = async () => {
  if (!batchForm.llmModelIds || batchForm.llmModelIds.length === 0) {
    ElMessage.warning('请先选择LLM模型')
    return
  }

  loading.test = true
  try {
    // 测试每个选定的模型
    const results: SingleModelConnectivityTestResult[] = []
    let passed = 0
    let failed = 0
    const startTime = Date.now()

    for (const modelId of batchForm.llmModelIds) {
      try {
        const result = await testSingleModelConnectivity(modelId)
        results.push(result)
        if (result.connected) passed++
        else failed++
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '未知错误'
        results.push({
          modelId,
          modelName: getModelNameById(modelId),
          provider: '',
          apiEndpoint: '',
          connected: false,
          responseTime: 0,
          success: false,
          timestamp: Date.now(),
          error: errorMessage
        })
        failed++
      }
    }

    const endTime = Date.now()
    connectivityTestResult.modelResults = results
    connectivityTestResult.totalModels = results.length
    connectivityTestResult.passedModels = passed
    connectivityTestResult.failedModels = failed
    connectivityTestResult.testDuration = endTime - startTime
    connectivityTestResult.success = failed === 0

    dialogs.connectivityTest = true
  } catch (error) {
    ElMessage.error('测试模型连通性失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.test = false
  }
}

// 测试批次配置
const testBatchConfig = async () => {
  // 表单验证
  if (!batchFormRef.value) return

  const valid = await batchFormRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完善必填信息')
    return
  }

  ElMessage.success('配置验证通过')
}

// 创建批次
const createBatch = async () => {
  // 表单验证
  if (!batchFormRef.value) return

  const valid = await batchFormRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完善必填信息')
    return
  }

  const userId = userStore.currentUser?.id
  if (!userId) {
    ElMessage.error('用户未登录')
    return
  }

  loading.create = true
  try {
    // 创建批次
    const result = await createAnswerGenerationBatch({
      ...batchForm as AnswerGenerationBatchCreateData,
      userId
    })

    ElMessage.success(`批次 "${result.name}" 创建成功`)
    router.push('/runtime/answer-generation-batches')
  } catch (error) {
    ElMessage.error('创建批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.create = false
  }
}

// 创建并启动批次
const createAndStartBatch = async () => {
  // 表单验证
  if (!batchFormRef.value) return

  const valid = await batchFormRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完善必填信息')
    return
  }

  const userId = userStore.currentUser?.id
  if (!userId) {
    ElMessage.error('用户未登录')
    return
  }

  const confirmText = '批次创建后将立即启动。是否确认？'

  try {
    await ElMessageBox.confirm(confirmText, '确认启动', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.start = true

    // 创建批次
    const result = await createAnswerGenerationBatch({
      ...batchForm as AnswerGenerationBatchCreateData,
      userId
    })

    // 启动批次
    await startAnswerGenerationBatch(result.id)

    ElMessage.success(`批次 "${result.name}" 创建并启动成功`)
    router.push('/runtime/answer-generation-batches')
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('创建并启动批次失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.start = false
  }
}

// 监听模型变化，重置模型参数表单
watch(() => batchForm.llmModelIds, (newIds) => {
  if (newIds) {
    // 移除不再选中的模型参数
    if (batchForm.modelSpecificParameters) {
      const currentIds = new Set(newIds)
      for (const modelId of Object.keys(batchForm.modelSpecificParameters)) {
        if (!currentIds.has(Number(modelId))) {
          delete batchForm.modelSpecificParameters[Number(modelId)]
        }
      }
    }
  }
}, { deep: true })

// 页面加载时获取数据
onMounted(() => {
  // 加载数据集版本
  loadDatasetVersions()

  // 加载回答组装配置
  loadAnswerAssemblyConfigs()

  // 加载LLM模型
  loadLLMModels()

  // 加载各种题型的提示词
  loadQuestionTypePrompts(QuestionType.SINGLE_CHOICE)
  loadQuestionTypePrompts(QuestionType.MULTIPLE_CHOICE)
  loadQuestionTypePrompts(QuestionType.SIMPLE_FACT)
  loadQuestionTypePrompts(QuestionType.SUBJECTIVE)
})
</script>

<style scoped>
.create-answer-batch {
  padding: 20px;
}

.page-container {
  margin-top: 20px;
}

.content-row {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.card-content {
  color: #606266;
  line-height: 1.6;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.param-preview {
  margin-top: 8px;
  padding: 5px 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.model-selection-container {
  margin-bottom: 12px;
}

.model-select {
  width: 500px;
}

.model-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.action-icon {
  margin-right: 4px;
}

.model-option {
  padding: 8px 0;
}

.model-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.model-name {
  font-weight: 500;
  font-size: 14px;
}

.model-provider {
  margin-left: auto;
  min-width: 80px;
  text-align: center;
}

.help-text {
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
  margin-top: 4px;
}

.batch-form {
  max-width: 900px;
  margin: 0 auto;
}

.selection-container {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
