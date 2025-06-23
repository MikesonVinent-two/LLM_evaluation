<template>
  <div class="standard-answer-workbench">
    <el-container>
      <el-aside width="300px">
        <!-- 标准问题列表 -->
        <div class="standard-questions">
          <h3>待处理标准问题</h3>
          <div class="filter-section">
            <el-input
              v-model="searchQuery"
              placeholder="搜索标准问题"
              prefix-icon="el-icon-search"
            />
            <el-select v-model="typeFilter" placeholder="题型筛选" clearable>
              <el-option
                v-for="type in questionTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              />
            </el-select>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable>
              <el-option
                v-for="status in statusOptions"
                :key="status.value"
                :label="status.label"
                :value="status.value"
              />
            </el-select>
          </div>
          <el-scrollbar height="calc(100vh - 250px)">
            <el-list v-loading="loading">
              <el-list-item
                v-for="question in filteredQuestions"
                :key="question.id"
                @click="selectStandardQuestion(question)"
                :class="{ 'has-answer': question.hasStandardAnswer }"
              >
                <div class="question-item">
                  <span>{{ question.content }}</span>
                  <div class="question-meta">
                    <el-tag size="small" :type="getQuestionTypeTag(question.type)">
                      {{ question.type }}
                    </el-tag>
                    <el-tag size="small" :type="getAnswerStatusTag(question.status)">
                      {{ question.status }}
                    </el-tag>
                  </div>
                </div>
              </el-list-item>
            </el-list>
          </el-scrollbar>
        </div>
      </el-aside>

      <el-container>
        <el-header height="60px">
          <div class="header-actions" v-if="selectedQuestion">
            <div class="question-info">
              <h3>{{ selectedQuestion.content }}</h3>
              <el-tag>{{ selectedQuestion.type }}</el-tag>
            </div>
            <div class="action-buttons">
              <el-button @click="showHistory">
                历史版本
              </el-button>
              <el-button type="primary" @click="saveStandardAnswer">
                保存标准答案
              </el-button>
            </div>
          </div>
        </el-header>

        <el-main>
          <el-row :gutter="20" v-if="selectedQuestion">
            <!-- 原始回答 -->
            <el-col :span="8">
              <div class="answer-panel">
                <div class="panel-header">
                  <h4>原始回答</h4>
                  <el-button-group>
                    <el-button size="small" @click="sortAnswers('score', 'original')">
                      按分数排序
                    </el-button>
                    <el-button size="small" @click="sortAnswers('time', 'original')">
                      按时间排序
                    </el-button>
                  </el-button-group>
                </div>
                <el-scrollbar height="calc(100vh - 300px)">
                  <div
                    v-for="answer in originalAnswers"
                    :key="answer.id"
                    class="answer-item"
                  >
                    <div class="answer-content">{{ answer.content }}</div>
                    <div class="answer-meta">
                      <div class="meta-info">
                        <span>得分：{{ answer.score }}</span>
                        <span>时间：{{ formatTime(answer.createTime) }}</span>
                      </div>
                      <div class="meta-actions">
                        <el-button
                          type="text"
                          @click="referenceAnswer(answer.content)"
                        >
                          引用
                        </el-button>
                        <el-button
                          type="text"
                          @click="previewAnswer(answer)"
                        >
                          预览
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </el-col>

            <!-- 众包回答 -->
            <el-col :span="8">
              <div class="answer-panel">
                <div class="panel-header">
                  <h4>众包回答</h4>
                  <el-button-group>
                    <el-button size="small" @click="sortAnswers('rating', 'crowdsourced')">
                      按评分排序
                    </el-button>
                    <el-button size="small" @click="sortAnswers('time', 'crowdsourced')">
                      按时间排序
                    </el-button>
                  </el-button-group>
                </div>
                <el-scrollbar height="calc(100vh - 300px)">
                  <div
                    v-for="answer in crowdsourcedAnswers"
                    :key="answer.id"
                    class="answer-item"
                  >
                    <div class="answer-content">{{ answer.content }}</div>
                    <div class="answer-meta">
                      <div class="meta-info">
                        <span>评分：{{ answer.rating }}</span>
                        <span>时间：{{ formatTime(answer.createTime) }}</span>
                      </div>
                      <div class="meta-actions">
                        <el-button
                          type="text"
                          @click="referenceAnswer(answer.content)"
                        >
                          引用
                        </el-button>
                        <el-button
                          type="text"
                          @click="previewAnswer(answer)"
                        >
                          预览
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </el-col>

            <!-- 专家回答 -->
            <el-col :span="8">
              <div class="answer-panel">
                <div class="panel-header">
                  <h4>专家回答</h4>
                  <el-button-group>
                    <el-button size="small" @click="sortAnswers('time', 'expert')">
                      按时间排序
                    </el-button>
                  </el-button-group>
                </div>
                <el-scrollbar height="calc(100vh - 300px)">
                  <div
                    v-for="answer in expertAnswers"
                    :key="answer.id"
                    class="answer-item"
                  >
                    <div class="answer-content">{{ answer.content }}</div>
                    <div class="answer-meta">
                      <div class="meta-info">
                        <span>专家：{{ answer.expert }}</span>
                        <span>时间：{{ formatTime(answer.createTime) }}</span>
                      </div>
                      <div class="meta-actions">
                        <el-button
                          type="text"
                          @click="referenceAnswer(answer.content)"
                        >
                          引用
                        </el-button>
                        <el-button
                          type="text"
                          @click="previewAnswer(answer)"
                        >
                          预览
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </el-col>
          </el-row>

          <!-- 标准答案编辑器 -->
          <div class="standard-answer-editor" v-if="selectedQuestion">
            <div class="editor-header">
              <h4>标准答案</h4>
              <div class="editor-actions">
                <el-select
                  v-model="answerTemplate"
                  placeholder="选择答案模板"
                  @change="applyTemplate"
                >
                  <el-option
                    v-for="template in getTemplatesByType(selectedQuestion.type)"
                    :key="template.value"
                    :label="template.label"
                    :value="template.value"
                  />
                </el-select>
                <el-button @click="generateAIAnswer" :loading="generating">
                  AI智能生成
                </el-button>
              </div>
            </div>
            <el-input
              v-model="standardAnswer"
              type="textarea"
              :rows="12"
              placeholder="请输入标准答案"
            />
            <div class="answer-preview" v-if="previewMode">
              <div class="preview-header">
                <h4>预览效果</h4>
                <el-button @click="previewMode = false">关闭预览</el-button>
              </div>
              <div class="preview-content" v-html="renderedAnswer"></div>
            </div>
          </div>
        </el-main>
      </el-container>

      <!-- 历史版本抽屉 -->
      <el-drawer
        v-model="showVersionHistory"
        title="历史版本"
        direction="rtl"
        size="50%"
      >
        <el-timeline>
          <el-timeline-item
            v-for="version in answerVersions"
            :key="version.id"
            :timestamp="version.createTime"
            :type="getVersionTypeIcon(version.type)"
          >
            <div class="version-item">
              <div class="version-header">
                <span class="version-type">{{ version.type }}</span>
                <span class="version-author">{{ version.author }}</span>
              </div>
              <div class="version-content">{{ version.content }}</div>
              <div class="version-actions">
                <el-button
                  size="small"
                  type="primary"
                  @click="restoreVersion(version)"
                >
                  恢复此版本
                </el-button>
                <el-button
                  size="small"
                  @click="previewVersion(version)"
                >
                  预览
                </el-button>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-drawer>

      <!-- 答案预览对话框 -->
      <el-dialog
        v-model="showPreview"
        title="答案预览"
        width="50%"
      >
        <div class="preview-content" v-html="previewContent"></div>
      </el-dialog>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getStandardQuestions,
  getOriginalAnswers,
  getCrowdsourcedAnswers,
  getExpertAnswers,
  saveStandardAnswer,
  getAnswerVersionHistory
} from '@/api/standardData'

const loading = ref(false)
const searchQuery = ref('')
const standardQuestions = ref([])
const selectedQuestion = ref(null)
const originalAnswers = ref([])
const crowdsourcedAnswers = ref([])
const expertAnswers = ref([])
const standardAnswer = ref('')
const showVersionHistory = ref(false)
const answerVersions = ref([])
const answerTemplate = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const generating = ref(false)
const previewMode = ref(false)
const showPreview = ref(false)
const previewContent = ref('')

const questionTypes = [
  { value: 'single', label: '单选题' },
  { value: 'multiple', label: '多选题' },
  { value: 'subjective', label: '主观题' },
  { value: 'programming', label: '编程题' }
]

const statusOptions = [
  { value: 'pending', label: '待处理' },
  { value: 'completed', label: '已完成' },
  { value: 'reviewing', label: '审核中' }
]

const filteredQuestions = computed(() => {
  let result = standardQuestions.value
  if (searchQuery.value) {
    result = result.filter(q =>
      q.content.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }
  if (typeFilter.value) {
    result = result.filter(q => q.type === typeFilter.value)
  }
  if (statusFilter.value) {
    result = result.filter(q => q.status === statusFilter.value)
  }
  return result
})

onMounted(async () => {
  loading.value = true
  try {
    standardQuestions.value = await getStandardQuestions()
  } catch (error) {
    ElMessage.error('获取标准问题失败')
  }
  loading.value = false
})

const selectStandardQuestion = async (question) => {
  selectedQuestion.value = question
  answerTemplate.value = question.type
  try {
    const [original, crowdsourced, expert] = await Promise.all([
      getOriginalAnswers(question.id),
      getCrowdsourcedAnswers(question.id),
      getExpertAnswers(question.id)
    ])
    originalAnswers.value = original
    crowdsourcedAnswers.value = crowdsourced
    expertAnswers.value = expert
  } catch (error) {
    ElMessage.error('获取答案数据失败')
  }
}

const referenceAnswer = (content) => {
  standardAnswer.value = standardAnswer.value
    ? standardAnswer.value + '\n\n' + content
    : content
}

const saveStandardAnswer = async () => {
  try {
    await saveStandardAnswer({
      questionId: selectedQuestion.value.id,
      content: standardAnswer.value,
      type: selectedQuestion.value.type
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const showHistory = async () => {
  try {
    answerVersions.value = await getAnswerVersionHistory(selectedQuestion.value.id)
    showVersionHistory.value = true
  } catch (error) {
    ElMessage.error('获取历史版本失败')
  }
}

const restoreVersion = (version) => {
  standardAnswer.value = version.content
  showVersionHistory.value = false
}

const getQuestionTypeTag = (type) => {
  const typeMap = {
    single: '',
    multiple: 'success',
    subjective: 'warning',
    programming: 'danger'
  }
  return typeMap[type] || ''
}

const getTemplatesByType = (type) => {
  const templates = {
    single: [
      { value: 'basic', label: '基础模板' },
      { value: 'detailed', label: '详细模板' }
    ],
    multiple: [
      { value: 'basic', label: '基础模板' },
      { value: 'detailed', label: '详细模板' }
    ],
    subjective: [
      { value: 'outline', label: '大纲模板' },
      { value: 'detailed', label: '详细模板' },
      { value: 'stepByStep', label: '步骤模板' }
    ],
    programming: [
      { value: 'algorithm', label: '算法模板' },
      { value: 'implementation', label: '实现模板' },
      { value: 'debug', label: '调试模板' }
    ]
  }
  return templates[type] || []
}

const applyTemplate = async (templateValue) => {
  try {
    const template = await getAnswerTemplate(selectedQuestion.value.type, templateValue)
    standardAnswer.value = template
  } catch (error) {
    ElMessage.error('获取模板失败')
  }
}

const generateAIAnswer = async () => {
  generating.value = true
  try {
    const aiAnswer = await generateAnswer({
      question: selectedQuestion.value,
      originalAnswers: originalAnswers.value,
      crowdsourcedAnswers: crowdsourcedAnswers.value,
      expertAnswers: expertAnswers.value
    })
    standardAnswer.value = aiAnswer
  } catch (error) {
    ElMessage.error('生成答案失败')
  }
  generating.value = false
}

const sortAnswers = (criterion, type) => {
  const sorter = (a, b) => {
    if (criterion === 'time') {
      return new Date(b.createTime) - new Date(a.createTime)
    }
    return b[criterion] - a[criterion]
  }

  if (type === 'original') {
    originalAnswers.value.sort(sorter)
  } else if (type === 'crowdsourced') {
    crowdsourcedAnswers.value.sort(sorter)
  } else {
    expertAnswers.value.sort(sorter)
  }
}

const previewAnswer = (answer) => {
  previewContent.value = renderAnswer(answer.content)
  showPreview.value = true
}

const previewVersion = (version) => {
  previewContent.value = renderAnswer(version.content)
  showPreview.value = true
}

const renderAnswer = (content) => {
  // 实现答案渲染逻辑，可能需要处理Markdown、代码高亮等
  return content
}

const getAnswerStatusTag = (status) => {
  const statusMap = {
    pending: 'info',
    completed: 'success',
    reviewing: 'warning'
  }
  return statusMap[status] || ''
}

const getVersionTypeIcon = (type) => {
  const typeMap = {
    create: 'primary',
    update: 'warning',
    restore: 'success'
  }
  return typeMap[type] || 'info'
}

const formatTime = (time) => {
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
.standard-answer-workbench {
  height: 100vh;
}

.standard-questions {
  padding: 20px;
  border-right: 1px solid #dcdfe6;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.has-answer {
  color: #67c23a;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 100%;
  border-bottom: 1px solid #dcdfe6;
}

.answer-panel {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  height: 100%;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.answer-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.answer-content {
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.answer-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 14px;
}

.standard-answer-editor {
  margin-top: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.editor-toolbar {
  margin-bottom: 20px;
}

.version-item {
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
}

.version-content {
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.filter-section {
  margin: 15px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-meta {
  display: flex;
  gap: 8px;
}

.meta-info {
  display: flex;
  gap: 15px;
}

.meta-actions {
  display: flex;
  gap: 8px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.editor-actions {
  display: flex;
  gap: 10px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.version-type {
  font-weight: bold;
}

.version-author {
  color: #909399;
}

.version-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.preview-content {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 200px;
}
</style>
