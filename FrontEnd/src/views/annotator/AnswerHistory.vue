<template>
  <div class="answer-history">
    <el-container>
      <el-aside width="300px">
        <!-- 问题列表 -->
        <div class="question-list">
          <h3>问题列表</h3>
          <div class="filter-section">
            <el-input
              v-model="searchQuery"
              placeholder="搜索问题"
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
          </div>
          <el-scrollbar height="calc(100vh - 200px)">
            <el-list v-loading="loading">
              <el-list-item
                v-for="question in filteredQuestions"
                :key="question.id"
                @click="selectQuestion(question)"
                :class="{ 'active': selectedQuestion?.id === question.id }"
              >
                <div class="question-item">
                  <span class="question-content">{{ question.content }}</span>
                  <el-tag size="small" :type="getQuestionTypeTag(question.type)">
                    {{ question.type }}
                  </el-tag>
                </div>
              </el-list-item>
            </el-list>
          </el-scrollbar>
        </div>
      </el-aside>

      <el-container>
        <el-header height="60px">
          <div class="header-actions" v-if="selectedQuestion">
            <h3>{{ selectedQuestion.content }}</h3>
            <div class="version-filter">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="filterVersions"
              />
              <el-select v-model="authorFilter" placeholder="作者筛选" clearable>
                <el-option
                  v-for="author in authors"
                  :key="author"
                  :label="author"
                  :value="author"
                />
              </el-select>
            </div>
          </div>
        </el-header>

        <el-main>
          <template v-if="selectedQuestion">
            <!-- 版本时间线 -->
            <div class="version-timeline">
              <el-timeline>
                <el-timeline-item
                  v-for="version in filteredVersions"
                  :key="version.id"
                  :timestamp="formatTime(version.updateTime)"
                  :type="getVersionTypeIcon(version.type)"
                >
                  <div class="version-card">
                    <div class="version-header">
                      <div class="version-info">
                        <span class="version-type">{{ version.type }}</span>
                        <span class="version-author">作者：{{ version.author }}</span>
                        <span class="version-number">版本：{{ version.versionNumber }}</span>
                      </div>
                      <div class="version-actions">
                        <el-button-group>
                          <el-button
                            size="small"
                            @click="previewVersion(version)"
                          >
                            预览
                          </el-button>
                          <el-button
                            size="small"
                            type="primary"
                            @click="restoreVersion(version)"
                          >
                            恢复此版本
                          </el-button>
                          <el-button
                            size="small"
                            @click="compareVersion(version)"
                          >
                            对比
                          </el-button>
                        </el-button-group>
                      </div>
                    </div>
                    <div class="version-content">{{ version.content }}</div>
                    <div class="version-meta">
                      <span>修改原因：{{ version.reason || '无' }}</span>
                      <span>修改时间：{{ formatTime(version.updateTime) }}</span>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </template>

          <el-empty v-else description="请选择一个问题" />
        </el-main>
      </el-container>

      <!-- 版本对比抽屉 -->
      <el-drawer
        v-model="showComparison"
        title="版本对比"
        direction="rtl"
        size="60%"
      >
        <div class="comparison-container">
          <div class="comparison-header">
            <div class="version-selector">
              <el-select v-model="compareVersionA" placeholder="选择版本A">
                <el-option
                  v-for="version in versionHistory"
                  :key="version.id"
                  :label="formatVersionLabel(version)"
                  :value="version.id"
                />
              </el-select>
              <span class="comparison-divider">VS</span>
              <el-select v-model="compareVersionB" placeholder="选择版本B">
                <el-option
                  v-for="version in versionHistory"
                  :key="version.id"
                  :label="formatVersionLabel(version)"
                  :value="version.id"
                />
              </el-select>
            </div>
          </div>
          <div class="comparison-content">
            <div class="version-diff" v-html="diffResult"></div>
          </div>
        </div>
      </el-drawer>

      <!-- 预览对话框 -->
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRawQuestions } from '@/api/standardData'
import {
  getAnswerHistory,
  rollbackAnswer,
  compareAnswerVersions
} from '@/api/standardAnswer'
import type {
  AnswerCompareResult,
  FieldDiff
} from '@/api/standardAnswer'

interface Question {
  id: number;
  content: string;
  type: string;
}

interface Version {
  id: number;
  content: string;
  author: string;
  reason?: string;
  updateTime: string;
  type: 'create' | 'update' | 'restore';
  versionNumber: number;
}

const loading = ref(false)
const searchQuery = ref('')
const typeFilter = ref('')
const authorFilter = ref('')
const dateRange = ref<[Date | null, Date | null]>([null, null])
const questions = ref<Question[]>([])
const selectedQuestion = ref<Question | null>(null)
const versionHistory = ref<Version[]>([])
const showComparison = ref(false)
const showPreview = ref(false)
const compareVersionA = ref<string | null>(null)
const compareVersionB = ref<string | null>(null)
const previewContent = ref('')
const diffResult = ref('')

const questionTypes = [
  { value: 'single', label: '单选题' },
  { value: 'multiple', label: '多选题' },
  { value: 'subjective', label: '主观题' },
  { value: 'programming', label: '编程题' }
]

const filteredQuestions = computed(() => {
  let result = questions.value
  if (searchQuery.value) {
    result = result.filter(q =>
      q.content.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }
  if (typeFilter.value) {
    result = result.filter(q => q.type === typeFilter.value)
  }
  return result
})

const authors = computed(() => {
  const authorSet = new Set(versionHistory.value.map(v => v.author))
  return Array.from(authorSet)
})

const filteredVersions = computed(() => {
  let result = versionHistory.value
  if (authorFilter.value) {
    result = result.filter(v => v.author === authorFilter.value)
  }
  if (dateRange.value?.[0] && dateRange.value?.[1]) {
    const [start, end] = dateRange.value
    result = result.filter(v => {
      const date = new Date(v.updateTime)
      return date >= start && date <= end
    })
  }
  return result
})

onMounted(async () => {
  loading.value = true
  try {
    const response = await getRawQuestions({
      page: '1',
      size: '100'
    })
    questions.value = response.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('获取问题列表失败')
  }
  loading.value = false
})

const selectQuestion = async (question: Question) => {
  selectedQuestion.value = question
  try {
    const historyVersions = await getAnswerHistory(question.id)
    // 将API返回的历史版本转换为组件需要的版本格式
    versionHistory.value = historyVersions.map((version, index) => ({
      id: version.id,
      content: version.details.find(d => d.field === 'answerText')?.newValue || '',
      author: version.userName,
      reason: version.commitMessage,
      updateTime: version.commitTime,
      type: index === 0 ? 'create' : 'update',
      versionNumber: historyVersions.length - index
    }))
  } catch (error) {
    console.error(error)
    ElMessage.error('获取版本历史失败')
  }
}

const filterVersions = () => {
  // 版本过滤逻辑已通过计算属性实现
}

const previewVersion = (version: Version) => {
  previewContent.value = renderAnswer(version.content)
  showPreview.value = true
}

const restoreVersion = (version: Version) => {
  ElMessageBox.confirm('确定要恢复到此版本吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await rollbackAnswer(version.id, {
        userId: 101, // 当前用户ID，需要从用户状态中获取
        commitMessage: `恢复到版本 ${version.versionNumber}`
      })
      ElMessage.success('恢复成功')
      // 刷新版本历史
      if (selectedQuestion.value) {
        const historyVersions = await getAnswerHistory(selectedQuestion.value.id)
        versionHistory.value = historyVersions.map((version, index) => ({
          id: version.id,
          content: version.details.find(d => d.field === 'answerText')?.newValue || '',
          author: version.userName,
          reason: version.commitMessage,
          updateTime: version.commitTime,
          type: index === 0 ? 'create' : 'update',
          versionNumber: historyVersions.length - index
        }))
      }
    } catch (error) {
      console.error(error)
      ElMessage.error('恢复失败')
    }
  })
}

const compareVersion = async (version: Version) => {
  compareVersionA.value = version.id.toString()
  compareVersionB.value = null
  showComparison.value = true
}

const formatVersionLabel = (version: Version) => {
  return `${formatTime(version.updateTime)} - ${version.author}`
}

const renderAnswer = (content: string) => {
  // 实现答案渲染逻辑，可能需要处理Markdown、代码高亮等
  return content
}

const getQuestionTypeTag = (type: string) => {
  const typeMap: Record<string, string> = {
    single: '',
    multiple: 'success',
    subjective: 'warning',
    programming: 'danger'
  }
  return typeMap[type] || ''
}

const getVersionTypeIcon = (type: Version['type']) => {
  const typeMap: Record<Version['type'], string> = {
    create: 'primary',
    update: 'warning',
    restore: 'success'
  }
  return typeMap[type] || 'info'
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 监听版本选择变化
watch([compareVersionA, compareVersionB], async ([newA, newB]) => {
  if (newA && newB) {
    try {
      const compareResult = await compareAnswerVersions(newA, newB)
      diffResult.value = formatDiffResult(compareResult)
    } catch (error) {
      console.error(error)
      ElMessage.error('版本对比失败')
    }
  }
})

// 格式化差异结果为HTML
const formatDiffResult = (diff: AnswerCompareResult) => {
  // 实现差异格式化逻辑
  let html = '<div class="diff-result">'

  if (diff && diff.fieldDiffs) {
    html += `<h4>比较 ${diff.baseVersion.id} 和 ${diff.compareVersion.id}</h4>`

    diff.fieldDiffs.forEach((fieldDiff: FieldDiff) => {
      html += `<div class="diff-item ${fieldDiff.changed ? 'changed' : ''}">
        <div class="diff-field">${fieldDiff.field}</div>
        <div class="diff-values">
          <div class="diff-old">${fieldDiff.baseValue}</div>
          <div class="diff-new">${fieldDiff.compareValue}</div>
        </div>
      </div>`
    })
  }

  html += '</div>'
  return html
}
</script>

<style scoped>
.answer-history {
  height: 100vh;
}

.question-list {
  padding: 20px;
  border-right: 1px solid #dcdfe6;
}

.filter-section {
  margin: 15px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.question-content {
  flex: 1;
  margin-right: 10px;
}

.active {
  background-color: #f5f7fa;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 100%;
  border-bottom: 1px solid #dcdfe6;
}

.version-filter {
  display: flex;
  gap: 15px;
}

.version-card {
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.version-info {
  display: flex;
  gap: 15px;
  align-items: center;
}

.version-type {
  font-weight: bold;
}

.version-author {
  color: #909399;
}

.version-number {
  color: #409eff;
}

.version-content {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  margin: 10px 0;
  white-space: pre-wrap;
}

.version-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
}

.comparison-container {
  padding: 20px;
}

.comparison-header {
  margin-bottom: 20px;
}

.version-selector {
  display: flex;
  align-items: center;
  gap: 15px;
}

.comparison-divider {
  font-weight: bold;
  color: #909399;
}

.version-diff {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 200px;
}

.diff-result h4 {
  margin-bottom: 15px;
  color: #606266;
}

.diff-item {
  margin-bottom: 15px;
  padding: 10px;
  border-radius: 4px;
  background-color: #fff;
}

.diff-item.changed {
  background-color: #fcf8e3;
  border-left: 3px solid #e6a23c;
}

.diff-field {
  font-weight: bold;
  margin-bottom: 5px;
  color: #303133;
}

.diff-values {
  display: flex;
  gap: 20px;
}

.diff-old {
  flex: 1;
  padding: 8px;
  background-color: #fef0f0;
  border-radius: 4px;
  text-decoration: line-through;
  color: #f56c6c;
}

.diff-new {
  flex: 1;
  padding: 8px;
  background-color: #f0f9eb;
  border-radius: 4px;
  color: #67c23a;
}

.preview-content {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  min-height: 200px;
}
</style>
