<template>
  <div class="batch-import">
    <el-card class="import-card">
      <template #header>
        <div class="card-header">
          <h2>批量数据导入</h2>
        </div>
      </template>

      <div class="import-content">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="问题回答关联导入" name="related">
            <div class="import-instructions">
              <h3>导入说明</h3>
              <p>本功能用于导入问题及其对应的多个回答，支持以下格式：</p>
              <ul>
                <li><strong>JSON格式</strong>：包含问题信息和回答数组，符合API要求的格式</li>
              </ul>
              <div class="format-example">
                <pre>{
  "question": {
    "sourceUrl": "https://example.com/q/12345",
    "sourceSite": "医疗问答网站",
    "title": "高血压的主要症状有哪些？",
    "content": "最近感觉头痛，想了解高血压的主要症状。",
    "otherMetadata": "{\"tags\":[\"高血压\",\"症状\"]}"
  },
  "answers": [
    {
      "authorInfo": "心血管专家",
      "content": "高血压的常见症状包括头痛、头晕、恶心等...",
      "publishTime": "2025-05-01 14:30:00",
      "upvotes": 25,
      "isAccepted": true,
      "otherMetadata": ""
    },
    {
      "authorInfo": "内科医生",
      "content": "高血压早期可能没有明显症状，但常见的有...",
      "publishTime": "2025-05-01 15:20:00",
      "upvotes": 12,
      "isAccepted": false,
      "otherMetadata": ""
    }
  ]
}</pre>
              </div>
            </div>

            <div class="upload-area">
              <el-upload
                class="upload-component"
                drag
                :show-file-list="false"
                accept=".json"
                :auto-upload="false"
                :multiple="true"
                :on-change="handleFileChange"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  将文件拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持JSON格式文件，每个文件包含一个问题及其多个回答
                  </div>
                </template>
              </el-upload>
            </div>

            <div v-if="parsedFiles.length > 0" class="files-preview">
              <h3>已上传文件 ({{ parsedFiles.length }}个)</h3>
              <el-table :data="parsedFiles" style="width: 100%" border>
                <el-table-column prop="filename" label="文件名" width="220" />
                <el-table-column label="问题标题" min-width="220" show-overflow-tooltip>
                  <template #default="scope">
                    {{ scope.row.data.question.title }}
                  </template>
                </el-table-column>
                <el-table-column label="来源网站" width="150" show-overflow-tooltip>
                  <template #default="scope">
                    {{ scope.row.data.question.sourceSite }}
                  </template>
                </el-table-column>
                <el-table-column label="回答数量" width="100" align="center">
                  <template #default="scope">
                    {{ scope.row.data.answers.length }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100" align="center">
                  <template #default="scope">
                    <el-tag
                      :type="scope.row.status === 'pending' ? 'info' : scope.row.status === 'success' ? 'success' : 'danger'"
                      effect="plain"
                    >
                      {{
                        scope.row.status === 'pending' ? '等待导入' :
                        scope.row.status === 'success' ? '导入成功' : '导入失败'
                      }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button
                      type="danger"
                      link
                      size="small"
                      @click="removeFile(scope.$index)"
                    >
                      移除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="import-actions">
                <el-button type="danger" @click="clearFiles">清空文件</el-button>
                <el-button type="primary" @click="importFiles" :loading="importing">
                  开始导入
                </el-button>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="历史导入记录" name="history">
            <div class="history-content">
              <el-table
                :data="importHistory"
                style="width: 100%"
                border
                v-loading="loadingHistory"
              >
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="importTime" label="导入时间" width="180" sortable />
                <el-table-column prop="fileCount" label="文件数" width="100" align="center" />
                <el-table-column prop="questionCount" label="问题数" width="100" align="center" />
                <el-table-column prop="answerCount" label="回答数" width="100" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center">
                  <template #default="scope">
                    <el-tag
                      :type="scope.row.status === 'completed' ? 'success' : scope.row.status === 'in_progress' ? 'warning' : 'danger'"
                      effect="plain"
                    >
                      {{
                        scope.row.status === 'completed' ? '已完成' :
                        scope.row.status === 'in_progress' ? '进行中' : '失败'
                      }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="importedBy" label="导入用户" width="120" />
                <el-table-column prop="successRate" label="成功率" width="120" align="center">
                  <template #default="scope">
                    {{ scope.row.successRate }}%
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                  <template #default="scope">
                    <el-button
                      type="primary"
                      link
                      size="small"
                      @click="viewImportDetail(scope.row)"
                    >
                      查看详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="pagination.currentPage"
                  v-model:page-size="pagination.pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="pagination.total"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>

    <!-- 导入详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="导入详情"
      width="70%"
    >
      <div v-if="currentImportDetail" class="import-detail">
        <div class="detail-header">
          <div class="detail-item">
            <span class="label">导入ID：</span>
            <span class="value">{{ currentImportDetail.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">导入时间：</span>
            <span class="value">{{ currentImportDetail.importTime }}</span>
          </div>
          <div class="detail-item">
            <span class="label">状态：</span>
            <span class="value">
              <el-tag
                :type="currentImportDetail.status === 'completed' ? 'success' : currentImportDetail.status === 'in_progress' ? 'warning' : 'danger'"
                effect="plain"
              >
                {{
                  currentImportDetail.status === 'completed' ? '已完成' :
                  currentImportDetail.status === 'in_progress' ? '进行中' : '失败'
                }}
              </el-tag>
            </span>
          </div>
          <div class="detail-item">
            <span class="label">导入用户：</span>
            <span class="value">{{ currentImportDetail.importedBy }}</span>
          </div>
        </div>

        <el-divider />

        <div class="detail-summary">
          <div class="summary-item">
            <div class="label">问题数量</div>
            <div class="value">{{ currentImportDetail.questionCount }}</div>
          </div>
          <div class="summary-item">
            <div class="label">回答数量</div>
            <div class="value">{{ currentImportDetail.answerCount }}</div>
          </div>
          <div class="summary-item">
            <div class="label">成功数量</div>
            <div class="value">{{ currentImportDetail.successCount }}</div>
          </div>
          <div class="summary-item">
            <div class="label">失败数量</div>
            <div class="value">{{ currentImportDetail.failureCount }}</div>
          </div>
          <div class="summary-item">
            <div class="label">成功率</div>
            <div class="value">{{ currentImportDetail.successRate }}%</div>
          </div>
        </div>

        <el-divider />

        <div class="detail-files">
          <h3>导入文件列表</h3>
          <el-table :data="currentImportDetail.files" style="width: 100%" border>
            <el-table-column prop="filename" label="文件名" min-width="200" />
            <el-table-column prop="questionTitle" label="问题标题" min-width="250" show-overflow-tooltip />
            <el-table-column prop="answerCount" label="回答数量" width="100" align="center" />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag
                  :type="scope.row.status === 'success' ? 'success' : 'danger'"
                  effect="plain"
                >
                  {{ scope.row.status === 'success' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { createQuestionWithAnswers } from '@/api/rawData'
import type { QuestionWithAnswersDto } from '@/api/rawData'

// 标签页控制
const activeTab = ref('related')

// 对话框控制
const detailDialogVisible = ref(false)

// 加载状态
const importing = ref(false)
const loadingHistory = ref(false)

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 已解析的文件列表
const parsedFiles = ref<Array<{
  filename: string,
  data: QuestionWithAnswersDto,
  status: 'pending' | 'success' | 'failure',
  errorMessage?: string
}>>([])

// 导入历史记录
const importHistory = ref<Array<{
  id: number,
  importTime: string,
  fileCount: number,
  questionCount: number,
  answerCount: number,
  status: 'completed' | 'in_progress' | 'failed',
  importedBy: string,
  successRate: number
}>>([])

// 当前查看的导入详情
const currentImportDetail = ref<{
  id: number,
  importTime: string,
  status: string,
  importedBy: string,
  questionCount: number,
  answerCount: number,
  successCount: number,
  failureCount: number,
  successRate: number,
  files: Array<{
    filename: string,
    questionTitle: string,
    answerCount: number,
    status: string,
    errorMessage?: string
  }>
} | null>(null)

// 处理文件上传
const handleFileChange = (file: any) => {
  const rawFile = file.raw
  if (!rawFile) {
    ElMessage.error('文件获取失败')
    return
  }

  if (!rawFile.name.toLowerCase().endsWith('.json')) {
    ElMessage.error('只支持JSON格式文件')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const result = e.target?.result as string
      const data = JSON.parse(result)

      // 验证数据格式
      if (!data.question || !data.answers || !Array.isArray(data.answers)) {
        ElMessage.error(`文件 ${rawFile.name} 格式不正确，缺少问题或回答数组`)
        return
      }

      if (!data.question.title || !data.question.content) {
        ElMessage.error(`文件 ${rawFile.name} 中的问题缺少必要字段`)
        return
      }

      // 检查是否已经添加过相同文件
      const existingFile = parsedFiles.value.find(f => f.filename === rawFile.name)
      if (existingFile) {
        ElMessage.warning(`文件 ${rawFile.name} 已添加，请勿重复上传`)
        return
      }

      parsedFiles.value.push({
        filename: rawFile.name,
        data: data,
        status: 'pending'
      })

      ElMessage.success(`文件 ${rawFile.name} 已添加`)
    } catch (error) {
      console.error('解析JSON文件失败:', error)
      ElMessage.error(`解析文件 ${rawFile.name} 失败，请检查文件格式`)
    }
  }

  reader.readAsText(rawFile)
}

// 移除文件
const removeFile = (index: number) => {
  parsedFiles.value.splice(index, 1)
}

// 清空文件列表
const clearFiles = () => {
  if (parsedFiles.value.length === 0) return

  ElMessageBox.confirm(
    '确定要清空所有已上传的文件吗？',
    '清空确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    parsedFiles.value = []
    ElMessage.success('文件列表已清空')
  }).catch(() => {
    // 用户取消操作
  })
}

// 导入文件
const importFiles = async () => {
  if (parsedFiles.value.length === 0) {
    ElMessage.warning('请先上传文件')
    return
  }

  // 确认导入
  ElMessageBox.confirm(
    `确定要导入${parsedFiles.value.length}个文件中的问题和回答吗？`,
    '导入确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    importing.value = true

    try {
      // 逐个导入文件
      for (let i = 0; i < parsedFiles.value.length; i++) {
        const file = parsedFiles.value[i]

        // 跳过已经成功的文件
        if (file.status === 'success') continue

        try {
          await createQuestionWithAnswers(file.data)
          file.status = 'success'
        } catch (error: any) {
          file.status = 'failure'
          file.errorMessage = error.message || '导入失败'
          console.error(`导入文件 ${file.filename} 失败:`, error)
        }
      }

      // 统计导入结果
      const successCount = parsedFiles.value.filter(f => f.status === 'success').length
      const failureCount = parsedFiles.value.filter(f => f.status === 'failure').length

      ElMessage.success(`导入完成：成功${successCount}个，失败${failureCount}个`)

      // 如果全部成功，刷新导入历史
      if (successCount === parsedFiles.value.length) {
        fetchImportHistory()
      }
    } catch (error) {
      console.error('导入过程中发生错误:', error)
      ElMessage.error('导入过程中发生错误，请查看控制台获取详细信息')
    } finally {
      importing.value = false
    }
  }).catch(() => {
    // 用户取消导入
  })
}

// 获取导入历史
const fetchImportHistory = async () => {
  loadingHistory.value = true

  try {
    // 模拟数据，实际项目中应调用API获取历史记录
    setTimeout(() => {
      importHistory.value = generateMockImportHistory()
      pagination.total = importHistory.value.length
      loadingHistory.value = false
    }, 500)
  } catch (error) {
    console.error('获取导入历史失败:', error)
    ElMessage.error('获取导入历史失败')
    loadingHistory.value = false
  }
}

// 查看导入详情
const viewImportDetail = (row: any) => {
  // 模拟获取详情数据，实际项目中应调用API
  setTimeout(() => {
    currentImportDetail.value = {
      id: row.id,
      importTime: row.importTime,
      status: row.status,
      importedBy: row.importedBy,
      questionCount: row.questionCount,
      answerCount: row.answerCount,
      successCount: Math.round(row.questionCount * (row.successRate / 100)),
      failureCount: row.questionCount - Math.round(row.questionCount * (row.successRate / 100)),
      successRate: row.successRate,
      files: generateMockImportFiles(row.fileCount, row.successRate)
    }
    detailDialogVisible.value = true
  }, 300)
}

// 分页处理
const handleSizeChange = (val: number) => {
  pagination.pageSize = val
  fetchImportHistory()
}

const handleCurrentChange = (val: number) => {
  pagination.currentPage = val
  fetchImportHistory()
}

// 生成模拟导入历史数据
const generateMockImportHistory = () => {
  const history = []
  for (let i = 1; i <= 15; i++) {
    const fileCount = Math.floor(Math.random() * 20) + 1
    const questionCount = fileCount
    const answerCount = questionCount * (Math.floor(Math.random() * 3) + 1)
    const successRate = Math.round(Math.random() * 30) + 70

    history.push({
      id: i,
      importTime: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString().replace('T', ' ').slice(0, 19),
      fileCount,
      questionCount,
      answerCount,
      status: Math.random() > 0.2 ? 'completed' : (Math.random() > 0.5 ? 'in_progress' : 'failed'),
      importedBy: '管理员',
      successRate
    })
  }
  return history
}

// 生成模拟导入文件数据
const generateMockImportFiles = (count: number, successRate: number) => {
  const files = []
  const titles = [
    '高血压的主要症状有哪些？',
    '糖尿病的饮食控制方法',
    '婴儿发烧如何处理',
    '经常头痛是什么原因',
    '腰椎间盘突出的治疗方法',
    '肩周炎的自我康复训练',
    '长期失眠应该怎么调理',
    '胃痛的常见原因及治疗',
    '过敏性鼻炎怎么根治',
    '高血脂的危害及预防'
  ]

  for (let i = 1; i <= count; i++) {
    const answerCount = Math.floor(Math.random() * 4) + 1
    const isSuccess = Math.random() * 100 < successRate

    files.push({
      filename: `medical_qa_${i}.json`,
      questionTitle: titles[Math.floor(Math.random() * titles.length)],
      answerCount,
      status: isSuccess ? 'success' : 'failure',
      errorMessage: isSuccess ? '' : '数据格式错误或服务器错误'
    })
  }

  return files
}

onMounted(() => {
  fetchImportHistory()
})
</script>

<style scoped>
.batch-import {
  padding: 20px;
}

.import-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.import-content {
  padding: 20px 0;
}

.import-instructions {
  margin-bottom: 20px;
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
}

.import-instructions h3 {
  margin-top: 0;
  margin-bottom: 10px;
}

.format-example {
  margin-top: 10px;
  background-color: #f0f0f0;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
}

.format-example pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: monospace;
}

.upload-area {
  margin: 20px 0;
}

.upload-component {
  width: 100%;
}

.files-preview {
  margin-top: 20px;
}

.import-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.import-detail {
  padding: 10px;
}

.detail-header {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item .label {
  font-weight: bold;
  margin-right: 5px;
}

.detail-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  margin: 20px 0;
}

.summary-item {
  flex: 1;
  min-width: 120px;
  text-align: center;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  margin: 5px;
}

.summary-item .label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.summary-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.detail-files {
  margin-top: 20px;
}

.detail-files h3 {
  margin-bottom: 15px;
}
</style>
