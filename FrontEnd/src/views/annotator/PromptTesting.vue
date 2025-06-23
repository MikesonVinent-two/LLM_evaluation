<template>
  <div class="prompt-testing">
    <el-card class="testing-card">
      <template #header>
        <div class="card-header">
          <h2>提示词测试</h2>
          <div class="header-actions">
            <el-button type="primary" @click="runTest" :loading="loading">运行测试</el-button>
            <el-button type="info" @click="saveResult" :disabled="!hasResults">保存结果</el-button>
          </div>
        </div>
      </template>

      <el-form :model="testForm" label-width="120px">
        <!-- 提示词类型选择 -->
        <el-form-item label="提示词阶段">
          <el-radio-group v-model="testForm.promptPhase">
            <el-radio label="ANSWER">回答阶段</el-radio>
            <el-radio label="EVALUATION">评测阶段</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 回答阶段提示词配置 -->
        <el-form-item v-if="testForm.promptPhase === 'ANSWER'" label="回答提示词配置">
          <el-select v-model="testForm.answerPromptConfigId" placeholder="请选择回答提示词配置" style="width: 100%">
            <el-option
              v-for="config in answerPromptConfigs"
              :key="config.id"
              :label="config.name"
              :value="config.id"
            />
          </el-select>
        </el-form-item>

        <!-- 评测阶段提示词配置 -->
        <el-form-item v-if="testForm.promptPhase === 'EVALUATION'" label="评测提示词配置">
          <el-select v-model="testForm.evaluationPromptConfigId" placeholder="请选择评测提示词配置" style="width: 100%">
            <el-option
              v-for="config in evaluationPromptConfigs"
              :key="config.id"
              :label="config.name"
              :value="config.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="测试类型">
          <el-radio-group v-model="testForm.testType">
            <el-radio label="SINGLE_QUESTION">单个问题测试</el-radio>
            <el-radio label="BATCH_QUESTIONS">批量问题测试</el-radio>
            <el-radio label="CUSTOM_INPUT">自定义输入测试</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="testForm.testType === 'SINGLE_QUESTION'">
          <el-form-item label="选择问题">
            <el-select
              v-model="testForm.questionId"
              filterable
              remote
              :remote-method="searchQuestions"
              placeholder="请输入问题关键词搜索"
              style="width: 100%"
            >
              <el-option
                v-for="question in questions"
                :key="question.id"
                :label="question.content"
                :value="question.id"
              />
            </el-select>
          </el-form-item>

          <!-- 用于评测阶段的参考答案 -->
          <el-form-item v-if="testForm.promptPhase === 'EVALUATION'" label="参考答案">
            <el-select
              v-model="testForm.answerReferenceId"
              placeholder="请选择参考答案"
              style="width: 100%"
            >
              <el-option
                v-for="answer in referenceAnswers"
                :key="answer.id"
                :label="answer.content?.substring(0, 30) + '...'"
                :value="answer.id"
              />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="testForm.testType === 'BATCH_QUESTIONS'">
          <el-form-item label="问题集">
            <el-select v-model="testForm.questionSetId" placeholder="请选择问题集" style="width: 100%">
              <el-option
                v-for="set in questionSets"
                :key="set.id"
                :label="set.name"
                :value="set.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最大测试量">
            <el-input-number v-model="testForm.maxTestCount" :min="1" :max="100" />
          </el-form-item>
        </template>

        <template v-if="testForm.testType === 'CUSTOM_INPUT'">
          <el-form-item label="自定义输入">
            <el-input
              type="textarea"
              v-model="testForm.customInput"
              :rows="6"
              placeholder="请输入自定义测试内容"
            />
          </el-form-item>

          <!-- 用于评测阶段的自定义答案 -->
          <el-form-item v-if="testForm.promptPhase === 'EVALUATION'" label="自定义答案">
            <el-input
              type="textarea"
              v-model="testForm.customAnswer"
              :rows="6"
              placeholder="请输入自定义答案内容"
            />
          </el-form-item>
        </template>

        <el-form-item label="模型选择">
          <el-select v-model="testForm.modelId" placeholder="请选择模型" style="width: 100%">
            <el-option
              v-for="model in models"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="参数设置">
          <div class="parameter-inputs">
            <div class="parameter-item">
              <span>温度</span>
              <el-slider v-model="testForm.parameters.temperature" :min="0" :max="2" :step="0.1" show-input />
            </div>
            <div class="parameter-item">
              <span>最大输出令牌</span>
              <el-input-number v-model="testForm.parameters.maxTokens" :min="1" :max="4096" />
            </div>
            <div class="parameter-item">
              <span>采样Top P</span>
              <el-slider v-model="testForm.parameters.topP" :min="0" :max="1" :step="0.05" show-input />
            </div>
          </div>
        </el-form-item>
      </el-form>

      <el-divider>测试结果</el-divider>

      <div v-if="loading" class="loading-container">
        <el-progress type="circle" :percentage="testProgress" />
        <p>正在测试中，请稍候...</p>
      </div>

      <div v-else-if="testResults.length > 0" class="test-results">
        <el-tabs v-model="activeResultTab" type="card">
          <el-tab-pane label="组装后提示词" name="prompt">
            <div class="result-content">
              <pre>{{ selectedResult.assembledPrompt }}</pre>
            </div>
          </el-tab-pane>
          <el-tab-pane label="模型输出" name="output">
            <div class="result-content">
              <pre>{{ selectedResult.modelOutput }}</pre>
            </div>
          </el-tab-pane>
          <el-tab-pane label="性能指标" name="metrics">
            <div class="metrics-container">
              <el-descriptions title="性能指标" :column="2" border>
                <el-descriptions-item label="响应时间">{{ selectedResult.metrics.responseTime }}ms</el-descriptions-item>
                <el-descriptions-item label="输入令牌数">{{ selectedResult.metrics.inputTokens }}</el-descriptions-item>
                <el-descriptions-item label="输出令牌数">{{ selectedResult.metrics.outputTokens }}</el-descriptions-item>
                <el-descriptions-item label="总令牌数">{{ selectedResult.metrics.inputTokens + selectedResult.metrics.outputTokens }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>
        </el-tabs>

        <div v-if="testForm.testType === 'BATCH_QUESTIONS'" class="batch-navigation">
          <div class="nav-controls">
            <el-pagination
              layout="prev, pager, next"
              :total="testResults.length"
              :page-size="1"
              :current-page="currentResultIndex + 1"
              @current-change="handleResultPageChange"
            />
          </div>
          <div class="result-info">
            <span>当前显示: {{ currentResultIndex + 1 }} / {{ testResults.length }}</span>
          </div>
        </div>
      </div>

      <div v-else class="no-results">
        <el-empty description="暂无测试结果，请运行测试" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 导入API
import { getActiveAnswerPromptAssemblyConfigs } from '@/api/answerPromptAssemblyConfig'
import { getAllActiveEvaluationConfigs } from '@/api/evaluationPromptAssembly'
import { getStandardAnswersByQuestionId } from '@/api/standardAnswer'

// 状态定义
const loading = ref(false)
const testProgress = ref(0)
const activeResultTab = ref('output')
const currentResultIndex = ref(0)
const userStore = useUserStore()

// 表单数据
const testForm = reactive({
  promptPhase: 'ANSWER', // ANSWER 或 EVALUATION
  answerPromptConfigId: '',
  evaluationPromptConfigId: '',
  testType: 'SINGLE_QUESTION',
  questionId: '',
  questionSetId: '',
  maxTestCount: 10,
  customInput: '',
  customAnswer: '', // 评测阶段用于自定义答案
  answerReferenceId: '', // 评测阶段用于参考答案
  modelId: '',
  parameters: {
    temperature: 0.7,
    maxTokens: 2048,
    topP: 0.95
  }
})

// 数据
const answerPromptConfigs = ref([])
const evaluationPromptConfigs = ref([])
const questions = ref([])
const questionSets = ref([
  { id: '1', name: '基础数学问题集' },
  { id: '2', name: '历史知识问题集' }
])
const referenceAnswers = ref([])
const models = ref([
  { id: '1', name: 'GPT-3.5-Turbo' },
  { id: '2', name: 'GPT-4' },
  { id: '3', name: 'Claude-3-Opus' }
])

// 测试结果
const testResults = ref([])

// 计算属性
const hasResults = computed(() => testResults.value.length > 0)
const selectedResult = computed(() => {
  if (testResults.value.length === 0) {
    return {
      assembledPrompt: '',
      modelOutput: '',
      metrics: {
        responseTime: 0,
        inputTokens: 0,
        outputTokens: 0
      }
    }
  }
  return testResults.value[currentResultIndex.value]
})

// 生命周期钩子
onMounted(async () => {
  await fetchConfigs()
})

// 监听器
watch(() => testForm.questionId, async (newVal) => {
  if (newVal && testForm.promptPhase === 'EVALUATION') {
    await fetchReferenceAnswers(newVal)
  }
})

watch(() => testForm.promptPhase, async (newVal) => {
  if (newVal === 'EVALUATION' && testForm.questionId) {
    await fetchReferenceAnswers(testForm.questionId)
  }
})

// 获取配置
const fetchConfigs = async () => {
  try {
    // 获取回答提示词组装配置
    const answerConfigsResponse = await getActiveAnswerPromptAssemblyConfigs()
    if (answerConfigsResponse && answerConfigsResponse.success) {
      answerPromptConfigs.value = answerConfigsResponse.configs || []
    }

    // 获取评测提示词组装配置
    const evaluationConfigsResponse = await getAllActiveEvaluationConfigs()
    if (evaluationConfigsResponse && evaluationConfigsResponse.success) {
      evaluationPromptConfigs.value = evaluationConfigsResponse.configs || []
    }
  } catch (error) {
    console.error('获取提示词配置失败:', error)
    ElMessage.error('获取提示词配置失败')
  }
}

// 获取参考答案
const fetchReferenceAnswers = async (questionId: string | number) => {
  try {
    const response = await getStandardAnswersByQuestionId(questionId)
    if (response) {
      referenceAnswers.value = response || []
    }
  } catch (error) {
    console.error('获取参考答案失败:', error)
    ElMessage.error('获取参考答案失败')
  }
}

// 搜索问题
const searchQuestions = async (query: string) => {
  if (query.trim() === '') {
    questions.value = []
    return
  }

  // 这里应该调用搜索API
  // 暂时使用模拟数据
  questions.value = [
    { id: '1', content: `匹配查询: ${query} - 这是一个测试问题1` },
    { id: '2', content: `匹配查询: ${query} - 这是一个测试问题2` },
    { id: '3', content: `匹配查询: ${query} - 这是一个测试问题3` }
  ]
}

// 运行测试
const runTest = async () => {
  // 验证表单
  if (!validateForm()) {
    return
  }

  loading.value = true
  testProgress.value = 0
  testResults.value = []
  currentResultIndex.value = 0

  try {
    if (testForm.testType === 'SINGLE_QUESTION') {
      await runSingleQuestionTest()
    } else if (testForm.testType === 'BATCH_QUESTIONS') {
      await runBatchQuestionsTest()
    } else {
      await runCustomInputTest()
    }
  } catch (error) {
    console.error('测试失败:', error)
    ElMessage.error('测试失败')
  } finally {
    loading.value = false
    testProgress.value = 100
  }
}

// 验证表单
const validateForm = () => {
  if (testForm.promptPhase === 'ANSWER') {
    if (!testForm.answerPromptConfigId) {
      ElMessage.warning('请选择回答提示词配置')
      return false
    }
  } else {
    if (!testForm.evaluationPromptConfigId) {
      ElMessage.warning('请选择评测提示词配置')
      return false
    }
  }

  if (testForm.testType === 'SINGLE_QUESTION') {
    if (!testForm.questionId) {
      ElMessage.warning('请选择问题')
      return false
    }

    if (testForm.promptPhase === 'EVALUATION' && !testForm.answerReferenceId) {
      ElMessage.warning('评测阶段请选择参考答案')
      return false
    }
  } else if (testForm.testType === 'BATCH_QUESTIONS') {
    if (!testForm.questionSetId) {
      ElMessage.warning('请选择问题集')
      return false
    }
  } else {
    if (!testForm.customInput) {
      ElMessage.warning('请输入自定义测试内容')
      return false
    }

    if (testForm.promptPhase === 'EVALUATION' && !testForm.customAnswer) {
      ElMessage.warning('评测阶段请输入自定义答案内容')
      return false
    }
  }

  if (!testForm.modelId) {
    ElMessage.warning('请选择模型')
    return false
  }

  return true
}

// 运行单个问题测试
const runSingleQuestionTest = async () => {
  const questionContent = questions.value.find(q => q.id === testForm.questionId)?.content || '未找到问题内容'

  // 这里应该调用API来组装提示词并运行测试
  // 暂时使用模拟数据
  await simulateApiCall()

  const startTime = Date.now()
  await new Promise(resolve => setTimeout(resolve, 2000)) // 模拟API调用延迟
  const endTime = Date.now()

  const responseTime = endTime - startTime
  const inputTokens = Math.floor(Math.random() * 500) + 200
  const outputTokens = Math.floor(Math.random() * 300) + 100

  let assembledPrompt, modelOutput

  if (testForm.promptPhase === 'ANSWER') {
    assembledPrompt = `系统提示：你是一个专业的AI助手。\n\n问题：${questionContent}\n\n请详细回答上述问题。`
    modelOutput = `这是对问题 "${questionContent}" 的回答：\n\n这是一个模拟的回答内容，实际应用中会返回真实的模型输出。\n\n这个回答包含了问题的详细解释和相关的知识点。`
  } else {
    const answerContent = referenceAnswers.value.find(a => a.id === testForm.answerReferenceId)?.content || '未找到答案内容'
    assembledPrompt = `系统提示：你是一个评测专家。\n\n问题：${questionContent}\n\n答案：${answerContent}\n\n请评估上述答案的质量、准确性和完整性。`
    modelOutput = `评测结果：\n\n针对问题 "${questionContent}" 的答案评估如下：\n\n准确性：8/10\n完整性：7/10\n清晰度：9/10\n\n总体评价：这个答案基本正确，但有一些细节可以进一步完善。`
  }

  testResults.value.push({
    id: 1,
    question: questionContent,
    assembledPrompt,
    modelOutput,
    metrics: {
      responseTime,
      inputTokens,
      outputTokens
    }
  })
}

// 运行批量问题测试
const runBatchQuestionsTest = async () => {
  const totalTests = testForm.maxTestCount

  // 模拟批量测试
  for (let i = 0; i < totalTests; i++) {
    testProgress.value = Math.floor((i / totalTests) * 100)

    await simulateApiCall()

    const questionContent = `这是问题集中的问题 #${i+1}`
    const startTime = Date.now()
    await new Promise(resolve => setTimeout(resolve, 500)) // 模拟API调用延迟
    const endTime = Date.now()

    const responseTime = endTime - startTime
    const inputTokens = Math.floor(Math.random() * 500) + 200
    const outputTokens = Math.floor(Math.random() * 300) + 100

    let assembledPrompt, modelOutput

    if (testForm.promptPhase === 'ANSWER') {
      assembledPrompt = `系统提示：你是一个专业的AI助手。\n\n问题：${questionContent}\n\n请详细回答上述问题。`
      modelOutput = `这是对问题 "${questionContent}" 的回答：\n\n这是一个模拟的回答内容，实际应用中会返回真实的模型输出。`
    } else {
      const answerContent = `这是问题 #${i+1} 的参考答案内容`
      assembledPrompt = `系统提示：你是一个评测专家。\n\n问题：${questionContent}\n\n答案：${answerContent}\n\n请评估上述答案的质量。`
      modelOutput = `评测结果：\n\n针对问题 "${questionContent}" 的答案评估如下：\n\n总体评分：${Math.floor(Math.random() * 3) + 7}/10`
    }

    testResults.value.push({
      id: i + 1,
      question: questionContent,
      assembledPrompt,
      modelOutput,
      metrics: {
        responseTime,
        inputTokens,
        outputTokens
      }
    })
  }
}

// 运行自定义输入测试
const runCustomInputTest = async () => {
  await simulateApiCall()

  const startTime = Date.now()
  await new Promise(resolve => setTimeout(resolve, 2000)) // 模拟API调用延迟
  const endTime = Date.now()

  const responseTime = endTime - startTime
  const inputTokens = Math.floor(Math.random() * 500) + 200
  const outputTokens = Math.floor(Math.random() * 300) + 100

  let assembledPrompt, modelOutput

  if (testForm.promptPhase === 'ANSWER') {
    assembledPrompt = `系统提示：你是一个专业的AI助手。\n\n${testForm.customInput}\n\n请根据上述内容提供回答。`
    modelOutput = `这是对自定义输入的回答：\n\n这是一个模拟的回答内容，基于您提供的自定义输入。在实际应用中，这里会显示真实的模型输出。`
  } else {
    assembledPrompt = `系统提示：你是一个评测专家。\n\n问题/内容：${testForm.customInput}\n\n答案：${testForm.customAnswer}\n\n请评估上述答案的质量。`
    modelOutput = `评测结果：\n\n针对提供的内容和答案评估如下：\n\n准确性：${Math.floor(Math.random() * 3) + 7}/10\n完整性：${Math.floor(Math.random() * 3) + 7}/10\n\n总体评价：这是一个模拟的评测结果。`
  }

  testResults.value.push({
    id: 1,
    question: testForm.customInput,
    assembledPrompt,
    modelOutput,
    metrics: {
      responseTime,
      inputTokens,
      outputTokens
    }
  })
}

// 模拟API调用
const simulateApiCall = async () => {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve(null)
    }, 1000)
  })
}

// 保存结果
const saveResult = () => {
  ElMessage.success('测试结果已保存')
}

// 处理结果页面变化
const handleResultPageChange = (page: number) => {
  currentResultIndex.value = page - 1
}
</script>

<style scoped>
.prompt-testing {
  padding: 20px;
}

.testing-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.parameter-inputs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.parameter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.test-results {
  margin-top: 20px;
}

.result-content {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  white-space: pre-wrap;
  max-height: 500px;
  overflow-y: auto;
  font-family: 'Courier New', Courier, monospace;
}

.metrics-container {
  padding: 15px;
}

.batch-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.no-results {
  padding: 40px 0;
}
</style>
