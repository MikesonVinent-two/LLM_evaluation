<template>
  <div class="question-history">
    <el-row :gutter="20">
      <el-col :span="8">
        <!-- 左侧标准问题列表 -->
        <el-card class="left-panel">
          <template #header>
            <div class="panel-header">
              <h3>标准问题列表</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索问题..."
                prefix-icon="el-icon-search"
                clearable
                style="width: 220px; margin-left: 10px;"
              />
            </div>
          </template>

          <div class="filter-bar">
            <el-select v-model="filterQuestionType" placeholder="题型筛选" style="width: 150px; margin-right: 10px;">
              <el-option label="全部" value="" />
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="简单事实题" value="SIMPLE_FACT" />
              <el-option label="主观题" value="SUBJECTIVE" />
            </el-select>

            <el-switch
              v-model="onlyLatest"
              active-text="仅显示最新版本"
              inactive-text="显示所有版本"
              style="margin-right: 10px;"
              @change="handleOnlyLatestChange"
            />

            <el-select v-model="sortOrder" placeholder="排序方式" style="width: 150px;">
              <el-option label="创建时间 (新→旧)" value="creationTime,desc" />
              <el-option label="创建时间 (旧→新)" value="creationTime,asc" />
              <el-option label="问题文本 (A→Z)" value="questionText,asc" />
              <el-option label="问题文本 (Z→A)" value="questionText,desc" />
            </el-select>
          </div>

          <el-table
            :data="filteredQuestions"
            style="width: 100%"
            @row-click="handleQuestionClick"
            border
            highlight-current-row
            v-loading="loading.questions"
          >
            <el-table-column label="问题文本" prop="questionText" show-overflow-tooltip />
            <el-table-column label="题型" width="90">
              <template #default="{ row }">
                {{ getQuestionTypeText(row.questionType) }}
              </template>
            </el-table-column>
            <el-table-column label="版本" width="70">
              <template #default="{ row }">
                <el-tag>版本 {{ row.changeLogId || '未知' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              layout="prev, pager, next"
              :total="totalQuestions"
              :page-size="pageSize"
              :current-page="currentPage"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <!-- 右侧内容区域 -->
        <div v-if="!selectedQuestion" class="empty-state">
          <el-empty description="请从左侧选择一个标准问题" />
        </div>

        <template v-else>
          <!-- 标准问题详情 -->
          <el-card class="question-card">
            <template #header>
              <div class="panel-header">
                <h3>标准问题详情</h3>
                <div>
                  <el-tag type="success" style="margin-right: 10px;">版本 {{ selectedQuestion.changeLogId || '未知' }}</el-tag>
                  <el-button type="primary" size="small" @click="showQuestionEditor">编辑问题</el-button>
                  <el-button type="danger" size="small" @click="confirmDeleteQuestion">删除问题</el-button>
                </div>
              </div>
            </template>

            <div class="question-details">
              <p><strong>问题：</strong> {{ selectedQuestion.questionText }}</p>
              <p><strong>题型：</strong> {{ getQuestionTypeText(selectedQuestion.questionType) }}</p>
              <p><strong>难度：</strong> {{ getDifficultyText(selectedQuestion.difficulty) }}</p>
              <p><strong>创建时间：</strong> {{ formatDate(selectedQuestion.creationTime) }}</p>
              <p><strong>创建者：</strong> {{ selectedQuestion.createdByUser?.name || '未知' }}</p>
              <p>
                <strong>标签：</strong>
                <el-tag
                  v-for="tag in selectedQuestion.tags"
                  :key="tag"
                  style="margin-right: 5px;"
                  size="small"
                >
                  {{ tag }}
                </el-tag>
              </p>
            </div>
          </el-card>

          <!-- 版本历史列表 -->
          <el-card class="history-card">
            <template #header>
              <div class="panel-header">
                <h3>版本历史 <span class="version-count" v-if="versionHistory.length > 0">(共{{ versionHistory.length }}条)</span></h3>
                <el-radio-group v-model="historyViewMode" size="small">
                  <el-radio-button label="list">列表视图</el-radio-button>
                  <el-radio-button label="tree">树形视图</el-radio-button>
                </el-radio-group>
              </div>
            </template>

            <div v-loading="loading.history">
              <!-- 列表视图 -->
              <div v-if="historyViewMode === 'list'" class="list-view">
                <div v-if="versionHistory.length === 0" class="empty-history">
                  <el-empty description="暂无版本历史数据" />
                </div>
                <el-timeline v-else>
                  <el-timeline-item
                    v-for="version in paginatedVersionHistory"
                    :key="version.id"
                    :timestamp="formatDate(version.creationTime)"
                    :type="version.id === selectedQuestion.id ? 'primary' : ''"
                  >
                    <div class="version-item">
                      <div class="version-header">
                        <h4>
                          <el-tag type="success" size="medium" class="version-tag">版本 {{ version.changeLogId || '未知' }}</el-tag>
                        </h4>
                        <div>
                          <el-button
                            type="text"
                            @click="viewVersionDetail(version)"
                            :disabled="version.id === selectedQuestion.id"
                          >
                            查看
                          </el-button>
                          <el-button
                            type="text"
                            @click="restoreVersion(version)"
                            :disabled="version.id === selectedQuestion.id"
                          >
                            恢复此版本
                          </el-button>
                        </div>
                      </div>

                      <p class="version-message">{{ version.commitMessage || '无提交说明' }}</p>
                      <p class="version-meta">
                        修改者: {{ version.createdByUser?.name || '未知' }}
                        <span v-if="version.changeLogId" class="change-log-id">变更ID: {{ version.changeLogId }}</span>
                      </p>
                    </div>
                  </el-timeline-item>
                </el-timeline>

                <!-- 版本历史分页 -->
                <div class="pagination" v-if="versionHistory.length > historyPageSize">
                  <el-pagination
                    layout="prev, pager, next"
                    :total="versionHistory.length"
                    :page-size="historyPageSize"
                    :current-page="historyCurrentPage"
                    @current-change="historyCurrentPage = $event"
                  />
                </div>
              </div>

              <!-- 树形视图 -->
              <div v-else class="tree-view">
                <div v-if="versionTree.length === 0" class="empty-tree">
                  <el-empty description="暂无版本树数据" />
                </div>
                <el-tree
                  v-else
                  :data="versionTree"
                  node-key="id"
                  default-expand-all
                  :expand-on-click-node="false"
                  :highlight-current="true"
                  :current-node-key="selectedQuestion.id"
                >
                  <template #default="{ node, data }">
                    <div class="tree-node">
                      <span class="tree-label">
                        版本 {{ data.changeLogId || '未知' }} - {{ formatDate(data.creationTime) }}
                      </span>
                      <span class="tree-message">{{ data.commitMessage || '无提交说明' }}</span>
                      <div class="tree-actions">
                        <el-button
                          type="text"
                          @click="viewVersionDetail(data)"
                          :disabled="data.id === selectedQuestion.id"
                        >
                          查看
                        </el-button>
                        <el-button
                          type="text"
                          @click="restoreVersion(data)"
                          :disabled="data.id === selectedQuestion.id"
                        >
                          恢复
                        </el-button>
                      </div>
                    </div>
                  </template>
                </el-tree>
              </div>
            </div>
          </el-card>
        </template>
      </el-col>
    </el-row>

    <!-- 编辑对话框 -->
    <el-dialog
      title="编辑标准问题"
      v-model="editorDialogVisible"
      width="70%"
    >
      <el-form :model="editorForm" label-width="120px" ref="editorFormRef">
        <el-form-item label="问题文本" prop="questionText" :rules="[{ required: true, message: '请输入标准问题文本', trigger: 'blur' }]">
          <el-input
            v-model="editorForm.questionText"
            type="textarea"
            :rows="3"
            placeholder="请输入标准问题文本"
          />
        </el-form-item>

        <el-form-item label="题型" prop="questionType" :rules="[{ required: true, message: '请选择题型', trigger: 'change' }]">
          <el-select v-model="editorForm.questionType" placeholder="请选择题型">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="简单事实题" value="SIMPLE_FACT" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度" prop="difficulty" :rules="[{ required: true, message: '请选择难度', trigger: 'change' }]">
          <el-select v-model="editorForm.difficulty" placeholder="请选择难度">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签" prop="tags" :rules="[{ required: true, message: '请至少添加一个标签', trigger: 'change' }]">
          <el-tag
            v-for="tag in editorForm.tags"
            :key="tag"
            closable
            @close="handleRemoveTag(tag)"
            style="margin-right: 5px; margin-bottom: 5px;"
          >
            {{ tag }}
          </el-tag>

          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            class="tag-input"
            size="mini"
            @keyup.enter="handleAddTag"
            @blur="handleAddTag"
          />

          <el-button v-else class="button-new-tag" size="small" @click="showTagInput">
            + 新标签
          </el-button>
        </el-form-item>

        <el-form-item label="提交说明" prop="commitMessage" :rules="[{ required: true, message: '请输入提交说明', trigger: 'blur' }]">
          <el-input
            v-model="editorForm.commitMessage"
            placeholder="请简要描述此次修改的原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editorDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestionEdit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 版本对比对话框 -->
    <el-dialog
      title="版本对比"
      v-model="compareDialogVisible"
      width="80%"
      destroy-on-close
    >
      <div class="compare-view" v-if="compareVersion">
        <el-tabs type="border-card">
          <el-tab-pane label="问题对比">
            <div class="compare-grid">
              <div class="compare-row compare-header-row">
                <div class="compare-cell">属性</div>
                <div class="compare-cell">当前版本</div>
                <div class="compare-cell">历史版本</div>
              </div>

              <!-- 问题文本对比 -->
              <div class="compare-row">
                <div class="compare-cell compare-label">问题文本</div>
                <div class="compare-cell">{{ selectedQuestion.questionText }}</div>
                <div class="compare-cell">{{ compareVersion.questionText }}</div>
              </div>

              <!-- 题型对比 -->
              <div class="compare-row">
                <div class="compare-cell compare-label">题型</div>
                <div class="compare-cell">{{ getQuestionTypeText(selectedQuestion.questionType) }}</div>
                <div class="compare-cell">{{ getQuestionTypeText(compareVersion.questionType) }}</div>
              </div>

              <!-- 难度对比 -->
              <div class="compare-row">
                <div class="compare-cell compare-label">难度</div>
                <div class="compare-cell">{{ getDifficultyText(selectedQuestion.difficulty) }}</div>
                <div class="compare-cell">{{ getDifficultyText(compareVersion.difficulty) }}</div>
              </div>

              <!-- 标签对比 -->
              <div class="compare-row">
                <div class="compare-cell compare-label">标签</div>
                <div class="compare-cell">
                  <el-tag
                    v-for="tag in selectedQuestion.tags"
                    :key="tag"
                    style="margin-right: 5px; margin-bottom: 5px;"
                    size="small"
                  >
                    {{ tag }}
                  </el-tag>
                  <span v-if="!selectedQuestion.tags || selectedQuestion.tags.length === 0">无标签</span>
                </div>
                <div class="compare-cell">
                  <el-tag
                    v-for="tag in compareVersion.tags"
                    :key="tag"
                    style="margin-right: 5px; margin-bottom: 5px;"
                    size="small"
                  >
                    {{ tag }}
                  </el-tag>
                  <span v-if="!compareVersion.tags || compareVersion.tags.length === 0">无标签</span>
                </div>
              </div>

              <!-- 变更记录 -->
              <div class="compare-row" v-if="compareVersion.changes && compareVersion.changes.length > 0">
                <div class="compare-cell compare-label">变更记录</div>
                <div class="compare-cell changes-cell" style="grid-column: span 2;">
                  <div class="changes-list">
                    <div v-for="(change, index) in compareVersion.changes" :key="index" class="change-item">
                      <div>
                        <strong>{{ change.field || change.attributeName }}：</strong>
                        <span>{{ change.oldValue }}</span>
                        <el-icon><ArrowRight /></el-icon>
                        <span>{{ change.newValue }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="元数据">
            <div class="meta-grid">
              <div class="meta-row meta-header-row">
                <div class="meta-cell">属性</div>
                <div class="meta-cell">当前版本</div>
                <div class="meta-cell">历史版本</div>
              </div>

              <!-- 版本号 -->
              <div class="meta-row">
                <div class="meta-cell meta-label">版本号</div>
                <div class="meta-cell">{{ selectedQuestion.changeLogId || '未知' }}</div>
                <div class="meta-cell">{{ compareVersion.changeLogId || '未知' }}</div>
              </div>

              <!-- 创建时间 -->
              <div class="meta-row">
                <div class="meta-cell meta-label">创建时间</div>
                <div class="meta-cell">{{ formatDate(selectedQuestion.creationTime) }}</div>
                <div class="meta-cell">{{ formatDate(compareVersion.creationTime) }}</div>
              </div>

              <!-- 创建者 -->
              <div class="meta-row">
                <div class="meta-cell meta-label">创建者</div>
                <div class="meta-cell">{{ selectedQuestion.createdByUser?.name || '未知' }}</div>
                <div class="meta-cell">{{ compareVersion.createdByUser?.name || '未知' }}</div>
              </div>

              <!-- 提交说明 -->
              <div class="meta-row">
                <div class="meta-cell meta-label">提交说明</div>
                <div class="meta-cell">{{ selectedQuestion.commitMessage || '无' }}</div>
                <div class="meta-cell">{{ compareVersion.commitMessage || '无' }}</div>
              </div>

              <!-- 变更ID -->
              <div class="meta-row">
                <div class="meta-cell meta-label">变更ID</div>
                <div class="meta-cell">{{ selectedQuestion.changeLogId || '无' }}</div>
                <div class="meta-cell">{{ compareVersion.changeLogId || '无' }}</div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

// 导入相关API
import {
  searchStandardQuestions,
  getQuestionHistory,
  getQuestionVersionTree,
  updateStandardQuestion,
  rollbackQuestionVersion,
  deleteStandardQuestion,
  type QuestionType,
  type DifficultyLevel,
  type QuestionHistoryVersion,
  type QuestionVersionNode
} from '@/api/standardData'

// 标准问题接口
interface StandardQuestion {
  id: number
  questionText: string
  questionType: QuestionType
  difficulty: DifficultyLevel
  creationTime: string
  createdByUser?: {
    id: number
    name: string
    username: string
    role: string
    contactInfo: string
  }
  tags: string[]
  commitMessage?: string
  version?: string
  changeLogId?: number
}

// 版本变更接口
interface VersionChange {
  field?: string
  attributeName?: string
  oldValue: string
  newValue: string
}

// 版本历史接口
interface QuestionVersion extends StandardQuestion {
  changes?: VersionChange[]
  parentQuestionId?: number
  changeLogId?: number
}

// 状态定义
const loading = reactive({
  questions: false,
  history: false,
  updating: false
})
const searchQuery = ref('')
const filterQuestionType = ref('')
const sortOrder = ref('creationTime,desc')
const questions = ref<StandardQuestion[]>([])
const selectedQuestion = ref<StandardQuestion | null>(null)
const versionHistory = ref<QuestionVersion[]>([])
const versionTree = ref<QuestionVersionNode[]>([])
const totalQuestions = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const historyCurrentPage = ref(1) // 版本历史分页当前页
const historyPageSize = ref(10) // 版本历史分页每页条数
const historyViewMode = ref('list')
const editorDialogVisible = ref(false)
const compareDialogVisible = ref(false)
const compareVersion = ref<QuestionVersion | null>(null)
const editorFormRef = ref<FormInstance>()
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref<HTMLInputElement | null>(null)
const onlyLatest = ref(true)

// 编辑表单数据
const editorForm = reactive<{
  id: number;
  questionText: string;
  questionType: QuestionType;
  difficulty: DifficultyLevel;
  tags: string[];
  commitMessage: string;
  userId: number;
}>({
  id: 0,
  questionText: '',
  questionType: 'SINGLE_CHOICE' as QuestionType,
  difficulty: 'MEDIUM' as DifficultyLevel,
  tags: [] as string[],
  commitMessage: '',
  userId: 1 // 应该从用户状态获取
})

// 过滤标准问题
const filteredQuestions = computed(() => {
  if (!searchQuery.value && !filterQuestionType.value) return questions.value

  return questions.value.filter(question => {
    const matchesSearch = !searchQuery.value ||
      question.questionText.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesType = !filterQuestionType.value ||
      question.questionType === filterQuestionType.value

    return matchesSearch && matchesType
  })
})

// 计算当前页的版本历史
const paginatedVersionHistory = computed(() => {
  const start = (historyCurrentPage.value - 1) * historyPageSize.value
  const end = start + historyPageSize.value
  return versionHistory.value.slice(start, end)
})

// 生命周期钩子
onMounted(() => {
  fetchStandardQuestions()
})

// 获取标准问题列表
const fetchStandardQuestions = async () => {
  loading.questions = true
  try {
    const params = {
      page: (currentPage.value - 1).toString(),
      size: pageSize.value.toString(),
      sort: sortOrder.value,
      onlyLatest: onlyLatest.value
    }

    const response = await searchStandardQuestions(params)

    // 正确处理响应数据
    if (response && response.questions) {
      questions.value = response.questions || []
      totalQuestions.value = response.total || 0
    } else {
      console.warn('标准问题数据格式不符合预期:', response)
      questions.value = []
      totalQuestions.value = 0
      ElMessage.warning('获取标准问题数据格式异常')
    }
  } catch (error) {
    console.error('获取标准问题失败:', error)
    questions.value = []
    totalQuestions.value = 0
    ElMessage.error('获取标准问题失败')
  } finally {
    loading.questions = false
  }
}

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 获取题型文本
const getQuestionTypeText = (type: string) => {
  switch (type) {
    case 'SINGLE_CHOICE':
      return '单选题'
    case 'MULTIPLE_CHOICE':
      return '多选题'
    case 'SIMPLE_FACT':
      return '简单事实题'
    case 'SUBJECTIVE':
      return '主观题'
    default:
      return '未知'
  }
}

// 获取难度文本
const getDifficultyText = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY':
      return '简单'
    case 'MEDIUM':
      return '中等'
    case 'HARD':
      return '困难'
    default:
      return '未知'
  }
}

// 处理标准问题点击
const handleQuestionClick = async (row: StandardQuestion) => {
  selectedQuestion.value = row
  await fetchQuestionHistory(row.id)
}

// 获取问题历史
const fetchQuestionHistory = async (questionId: number) => {
  loading.history = true
  historyCurrentPage.value = 1 // 重置历史分页为第一页
  versionHistory.value = [] // 先清空历史记录，避免显示旧数据

  try {
    // 获取版本历史
    const historyResponse = await getQuestionHistory(questionId)
    console.log('版本历史响应数据:', historyResponse)

    if (Array.isArray(historyResponse)) {
      versionHistory.value = historyResponse
    } else if (historyResponse.data && Array.isArray(historyResponse.data)) {
      versionHistory.value = historyResponse.data
    } else {
      versionHistory.value = []
      console.warn('获取版本历史数据格式异常:', historyResponse)
    }

    // 显示返回的版本历史记录数量
    console.log(`获取到 ${versionHistory.value.length} 条版本历史记录`)

    // 如果版本历史不为空，按创建时间排序（从新到旧）
    if (versionHistory.value.length > 0) {
      versionHistory.value.sort((a, b) => {
        return new Date(b.creationTime).getTime() - new Date(a.creationTime).getTime()
      })
    }

    // 获取版本树
    const treeResponse = await getQuestionVersionTree(questionId)
    console.log('版本树响应数据:', treeResponse)

    // 正确处理版本树数据
    if (treeResponse) {
      if (Array.isArray(treeResponse)) {
        // 如果响应直接是数组
        versionTree.value = treeResponse
      } else if (Array.isArray(treeResponse.data)) {
        // 如果响应包含data数组属性
        versionTree.value = treeResponse.data
      } else if (treeResponse.data) {
        // 如果响应包含单个data对象
        versionTree.value = [treeResponse.data]
      } else {
        versionTree.value = []
        console.warn('获取版本树数据格式异常:', treeResponse)
      }
    } else {
      versionTree.value = []
      console.warn('获取版本树响应为空')
    }
  } catch (error) {
    console.error('获取问题历史失败:', error)
    versionHistory.value = []
    versionTree.value = []
    ElMessage.error('获取问题历史失败')
  } finally {
    loading.history = false
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchStandardQuestions()
}

// 显示问题编辑器
const showQuestionEditor = () => {
  if (!selectedQuestion.value) return

  editorForm.id = selectedQuestion.value.id
  editorForm.questionText = selectedQuestion.value.questionText
  editorForm.questionType = selectedQuestion.value.questionType
  editorForm.difficulty = selectedQuestion.value.difficulty
  editorForm.tags = [...selectedQuestion.value.tags]
  editorForm.commitMessage = ''

  editorDialogVisible.value = true
}

// 标签相关方法
const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
}

const handleAddTag = () => {
  if (tagInputValue.value) {
    if (!editorForm.tags.includes(tagInputValue.value)) {
      editorForm.tags.push(tagInputValue.value)
    }
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

const handleRemoveTag = (tag: string) => {
  editorForm.tags = editorForm.tags.filter(t => t !== tag)
}

// 提交问题编辑
const submitQuestionEdit = async () => {
  if (!editorFormRef.value) return

  await editorFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.updating = true
      try {
        // 构造更新请求数据
        const updateData = {
          questionText: editorForm.questionText,
          questionType: editorForm.questionType,
          difficulty: editorForm.difficulty,
          tags: editorForm.tags,
          commitMessage: editorForm.commitMessage,
          userId: editorForm.userId
        }

        await updateStandardQuestion(editorForm.id, updateData)
        ElMessage.success('标准问题更新成功')

        // 刷新数据
        await fetchStandardQuestions()

        // 更新选中的问题
        const updatedQuestion = questions.value.find(q => q.id === editorForm.id)
        if (updatedQuestion) {
          selectedQuestion.value = updatedQuestion
          await fetchQuestionHistory(updatedQuestion.id)
        }

        editorDialogVisible.value = false
      } catch (error) {
        console.error('更新标准问题失败:', error)
        ElMessage.error('更新标准问题失败')
      } finally {
        loading.updating = false
      }
    }
  })
}

// 查看版本详情
const viewVersionDetail = (version: QuestionVersion) => {
  compareVersion.value = version
  compareDialogVisible.value = true
}

// 恢复版本
const restoreVersion = (version: QuestionVersion) => {
  ElMessageBox.confirm(
    `确定要将问题恢复到版本 ${version.changeLogId || '未知'} 吗？`,
    '恢复版本',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.updating = true
    try {
      // 使用版本回退API，传递changeLogId作为版本ID
      const commitMessage = `回退到版本 ${version.changeLogId || '未知'}`
      const versionId = version.changeLogId || version.id
      const response = await rollbackQuestionVersion(versionId, {
        userId: editorForm.userId,
        commitMessage
      })

      ElMessage.success('版本回退成功')

      // 刷新数据
      await fetchStandardQuestions()

      // 更新选中的问题
      if (response && selectedQuestion.value) {
        const updatedQuestionId = selectedQuestion.value.id
        const updatedQuestion = questions.value.find(q => q.id === updatedQuestionId)
        if (updatedQuestion) {
          selectedQuestion.value = updatedQuestion
          await fetchQuestionHistory(updatedQuestionId)
        }
      }
    } catch (error) {
      console.error('版本回退失败:', error)
      ElMessage.error('版本回退失败')
    } finally {
      loading.updating = false
    }
  }).catch(() => {
    // 取消操作
  })
}

// 处理仅显示最新版本的切换
const handleOnlyLatestChange = () => {
  fetchStandardQuestions()
}

// 确认删除问题
const confirmDeleteQuestion = () => {
  if (!selectedQuestion.value) return

  ElMessageBox.confirm(
    '确定要删除该标准问题吗？默认为软删除，可以在后台恢复。',
    '删除标准问题',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await deleteStandardQuestion(selectedQuestion.value!.id, {
        userId: editorForm.userId,
        permanent: false // 默认软删除
      })

      if (response && response.success) {
        ElMessage.success('标准问题已删除')
        // 刷新问题列表
        await fetchStandardQuestions()
        // 清空当前选中的问题
        selectedQuestion.value = null
        versionHistory.value = []
        versionTree.value = []
      } else {
        ElMessage.error('删除失败: ' + (response?.message || '未知错误'))
      }
    } catch (error) {
      console.error('删除标准问题失败:', error)
      ElMessage.error('删除标准问题失败')
    }
  }).catch(() => {
    // 用户取消删除操作
  })
}
</script>

<style scoped>
.question-history {
  padding: 20px;
}

.left-panel {
  height: calc(100vh - 140px);
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 15px;
  text-align: center;
}

.empty-state {
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.question-card, .history-card {
  margin-bottom: 20px;
}

.question-details {
  padding: 10px 0;
}

.version-item {
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  margin-bottom: 10px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.version-message {
  color: #606266;
  margin: 10px 0;
}

.version-meta {
  color: #909399;
  font-size: 0.9em;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.change-log-id {
  background-color: #f0f9eb;
  padding: 2px 6px;
  border-radius: 4px;
  color: #67c23a;
  font-weight: 500;
}

.version-count {
  font-size: 0.9em;
  color: #909399;
  font-weight: normal;
  margin-left: 5px;
}

.empty-history {
  padding: 30px 0;
  text-align: center;
}

.version-tag {
  font-weight: normal;
  margin-right: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tree-label {
  font-weight: bold;
  margin-right: 10px;
}

.tree-message {
  color: #606266;
  flex-grow: 1;
  margin: 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-actions {
  margin-left: auto;
}

.tag-input {
  width: 100px;
  margin-right: 5px;
  vertical-align: bottom;
}

.compare-item {
  margin-bottom: 20px;
}

.compare-header {
  margin-bottom: 10px;
}

.compare-content {
  display: flex;
}

.compare-old, .compare-new {
  flex: 1;
  padding: 10px;
}

.compare-old {
  background-color: #f0f9eb;
  margin-right: 5px;
}

.compare-new {
  background-color: #f0f0f5;
  margin-left: 5px;
}

.compare-title {
  font-weight: bold;
  margin-bottom: 10px;
  border-bottom: 1px solid #dcdfe6;
  padding-bottom: 5px;
}

.compare-text {
  min-height: 40px;
}

.compare-meta {
  display: flex;
}

.meta-column {
  flex: 1;
  padding: 20px;
}

.meta-column:first-child {
  border-right: 1px solid #dcdfe6;
}

.empty-tree {
  padding: 20px;
  text-align: center;
}

.compare-grid {
  width: 100%;
  border-collapse: collapse;
  display: table;
  margin-bottom: 20px;
}

.compare-row {
  display: table-row;
}

.compare-header-row {
  background-color: #f5f7fa;
  font-weight: bold;
}

.compare-cell {
  display: table-cell;
  padding: 12px;
  border: 1px solid #ebeef5;
  vertical-align: top;
}

.compare-label {
  background-color: #f5f7fa;
  font-weight: bold;
  width: 120px;
}

.meta-grid {
  width: 100%;
  border-collapse: collapse;
  display: table;
}

.meta-row {
  display: table-row;
}

.meta-header-row {
  background-color: #f5f7fa;
  font-weight: bold;
}

.meta-cell {
  display: table-cell;
  padding: 12px;
  border: 1px solid #ebeef5;
}

.meta-label {
  background-color: #f5f7fa;
  font-weight: bold;
  width: 120px;
}

.changes-list {
  max-height: 200px;
  overflow-y: auto;
}

.change-item {
  padding: 5px 0;
  border-bottom: 1px solid #ebeef5;
}

.change-item:last-child {
  border-bottom: none;
}
</style>
