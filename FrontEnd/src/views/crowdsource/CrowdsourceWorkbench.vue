<template>
  <div class="crowdsource-workbench">
    <el-container>
      <el-aside width="300px">
        <!-- 任务列表 -->
        <div class="task-list">
          <h3>众包任务列表</h3>
          <el-input
            v-model="searchQuery"
            placeholder="搜索任务"
            prefix-icon="el-icon-search"
          />
          <div class="task-filters">
            <el-select v-model="taskType" placeholder="题型筛选">
              <el-option
                v-for="type in questionTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              />
            </el-select>
            <el-select v-model="taskStatus" placeholder="状态筛选">
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
                v-for="task in filteredTasks"
                :key="task.id"
                @click="selectTask(task)"
                :class="{ 'completed': task.status === 'completed' }"
              >
                <div class="task-item">
                  <div class="task-info">
                    <span class="task-title">{{ task.title }}</span>
                    <el-tag size="small" :type="getTaskTypeTag(task.type)">
                      {{ task.type }}
                    </el-tag>
                  </div>
                  <div class="task-meta">
                    <span>奖励：{{ task.reward }}积分</span>
                    <span>截止：{{ formatDeadline(task.deadline) }}</span>
                  </div>
                </div>
              </el-list-item>
            </el-list>
          </el-scrollbar>
        </div>
      </el-aside>

      <el-container>
        <el-header height="60px">
          <div class="header-actions" v-if="selectedTask">
            <div class="task-header">
              <h3>{{ selectedTask.title }}</h3>
              <el-tag>剩余时间：{{ remainingTime }}</el-tag>
            </div>
            <div class="action-buttons">
              <el-button
                type="primary"
                @click="submitAnswer"
                :disabled="!canSubmit"
              >
                提交答案
              </el-button>
              <el-button @click="saveAsDraft">
                保存草稿
              </el-button>
            </div>
          </div>
        </el-header>

        <el-main>
          <template v-if="selectedTask">
            <!-- 任务详情 -->
            <div class="task-detail">
              <div class="question-content">
                <h4>问题描述</h4>
                <div class="content-box">{{ selectedTask.content }}</div>
              </div>

              <div class="answer-requirements">
                <h4>答案要求</h4>
                <div class="content-box">{{ selectedTask.requirements }}</div>
              </div>

              <div class="reference-materials" v-if="selectedTask.references">
                <h4>参考资料</h4>
                <div class="content-box">
                  <div
                    v-for="(ref, index) in selectedTask.references"
                    :key="index"
                    class="reference-item"
                  >
                    <el-link :href="ref.url" target="_blank">
                      {{ ref.title }}
                    </el-link>
                  </div>
                </div>
              </div>
            </div>

            <!-- 答案编辑器 -->
            <div class="answer-editor">
              <h4>我的答案</h4>
              <div class="editor-toolbar">
                <el-select v-model="answerTemplate" placeholder="选择答案模板">
                  <el-option
                    v-for="template in answerTemplates"
                    :key="template.value"
                    :label="template.label"
                    :value="template.value"
                  />
                </el-select>
              </div>
              <el-input
                v-model="answerContent"
                type="textarea"
                :rows="12"
                placeholder="请输入你的答案"
              />
            </div>
          </template>

          <el-empty v-else description="请选择一个任务" />
        </el-main>
      </el-container>

      <!-- 提交历史抽屉 -->
      <el-drawer
        v-model="showHistory"
        title="提交历史"
        direction="rtl"
        size="50%"
      >
        <el-timeline>
          <el-timeline-item
            v-for="record in submissionHistory"
            :key="record.id"
            :timestamp="record.submitTime"
            :type="getSubmissionTypeIcon(record.status)"
          >
            <div class="submission-item">
              <div class="submission-content">{{ record.content }}</div>
              <div class="submission-meta">
                <span>状态：{{ record.status }}</span>
                <span>评分：{{ record.score || '待评分' }}</span>
              </div>
              <el-button
                v-if="record.status === 'draft'"
                size="small"
                @click="restoreFromHistory(record)"
              >
                继续编辑
              </el-button>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-drawer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCrowdsourceTasks,
  getTaskDetail,
  submitCrowdsourceAnswer,
  saveDraft,
  getSubmissionHistory
} from '@/api/crowdsourcedAnswer'

const loading = ref(false)
const searchQuery = ref('')
const taskType = ref('')
const taskStatus = ref('')
const selectedTask = ref(null)
const answerContent = ref('')
const showHistory = ref(false)
const submissionHistory = ref([])
const answerTemplate = ref('')

const questionTypes = [
  { value: 'single', label: '单选题' },
  { value: 'multiple', label: '多选题' },
  { value: 'subjective', label: '主观题' },
  { value: 'programming', label: '编程题' }
]

const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待完成' },
  { value: 'completed', label: '已完成' },
  { value: 'expired', label: '已过期' }
]

const answerTemplates = [
  { value: 'single', label: '单选题答案模板' },
  { value: 'multiple', label: '多选题答案模板' },
  { value: 'subjective', label: '主观题答案模板' },
  { value: 'programming', label: '编程题答案模板' }
]

const filteredTasks = computed(() => {
  let tasks = tasks.value
  if (searchQuery.value) {
    tasks = tasks.filter(task =>
      task.title.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }
  if (taskType.value) {
    tasks = tasks.filter(task => task.type === taskType.value)
  }
  if (taskStatus.value && taskStatus.value !== 'all') {
    tasks = tasks.filter(task => task.status === taskStatus.value)
  }
  return tasks
})

const remainingTime = computed(() => {
  if (!selectedTask.value?.deadline) return ''
  // 实现剩余时间计算逻辑
  return '2小时30分'
})

const canSubmit = computed(() => {
  return selectedTask.value && answerContent.value.trim().length > 0
})

onMounted(async () => {
  loading.value = true
  try {
    tasks.value = await getCrowdsourceTasks()
  } catch (error) {
    ElMessage.error('获取任务列表失败')
  }
  loading.value = false
})

const selectTask = async (task) => {
  try {
    const detail = await getTaskDetail(task.id)
    selectedTask.value = detail
    answerTemplate.value = task.type
    // 获取历史提交记录
    submissionHistory.value = await getSubmissionHistory(task.id)
  } catch (error) {
    ElMessage.error('获取任务详情失败')
  }
}

const submitAnswer = async () => {
  try {
    await submitCrowdsourceAnswer({
      taskId: selectedTask.value.id,
      content: answerContent.value
    })
    ElMessage.success('提交成功')
    // 刷新任务状态
    await selectTask(selectedTask.value)
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const saveAsDraft = async () => {
  try {
    await saveDraft({
      taskId: selectedTask.value.id,
      content: answerContent.value
    })
    ElMessage.success('保存草稿成功')
  } catch (error) {
    ElMessage.error('保存草稿失败')
  }
}

const restoreFromHistory = (record) => {
  answerContent.value = record.content
  showHistory.value = false
}

const getTaskTypeTag = (type) => {
  const typeMap = {
    single: '',
    multiple: 'success',
    subjective: 'warning',
    programming: 'danger'
  }
  return typeMap[type] || ''
}

const getSubmissionTypeIcon = (status) => {
  const statusMap = {
    submitted: 'primary',
    draft: 'info',
    accepted: 'success',
    rejected: 'danger'
  }
  return statusMap[status] || 'info'
}

const formatDeadline = (deadline) => {
  // 实现日期格式化逻辑
  return new Date(deadline).toLocaleDateString()
}
</script>

<style scoped>
.crowdsource-workbench {
  height: 100vh;
}

.task-list {
  padding: 20px;
  border-right: 1px solid #dcdfe6;
}

.task-filters {
  margin: 15px 0;
  display: flex;
  gap: 10px;
}

.task-item {
  width: 100%;
}

.task-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.completed {
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

.task-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.task-detail {
  margin-bottom: 20px;
}

.content-box {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-top: 10px;
}

.answer-editor {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.editor-toolbar {
  margin-bottom: 20px;
}

.reference-item {
  margin-bottom: 10px;
}

.submission-item {
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
}

.submission-content {
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.submission-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}
</style>
