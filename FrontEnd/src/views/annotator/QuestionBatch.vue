<template>
  <div class="question-batch">
    <el-card class="batch-card">
      <template #header>
        <div class="card-header">
          <h2>问题批量处理</h2>
          <div class="header-actions">
            <el-button type="primary" @click="importQuestions">导入问题</el-button>
            <el-button type="success" @click="processQuestions" :disabled="!hasSelectedQuestions">批量处理</el-button>
            <el-button type="info" @click="exportQuestions" :disabled="!hasProcessedQuestions">导出结果</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="批量导入" name="import">
          <div class="tab-content">
            <el-alert
              v-if="importInfo.show"
              :title="importInfo.title"
              :type="importInfo.type"
              :description="importInfo.message"
              show-icon
              closable
              @close="importInfo.show = false"
            />

            <div class="import-methods">
              <el-card class="import-method-card">
                <template #header>
                  <div class="method-header">
                    <h3>文件导入</h3>
                  </div>
                </template>
                <div class="upload-area">
                  <el-upload
                    class="upload-box"
                    drag
                    action="#"
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :limit="1"
                    :file-list="fileList"
                  >
                    <el-icon class="el-icon--upload"><Upload /></el-icon>
                    <div class="el-upload__text">
                      拖拽文件到此处或 <em>点击上传</em>
                    </div>
                    <template #tip>
                      <div class="el-upload__tip">
                        支持 .csv, .xlsx, .txt 格式文件，每行一个问题或按指定格式
                      </div>
                    </template>
                  </el-upload>
                  <div class="upload-actions">
                    <el-button type="primary" @click="handleUpload" :disabled="!fileList.length">上传文件</el-button>
                    <el-button @click="clearFiles">清空</el-button>
                  </div>
                </div>
              </el-card>

              <el-card class="import-method-card">
                <template #header>
                  <div class="method-header">
                    <h3>文本导入</h3>
                  </div>
                </template>
                <div class="text-import-area">
                  <el-input
                    type="textarea"
                    v-model="batchText"
                    :rows="8"
                    placeholder="请输入问题，每行一个"
                  />
                  <div class="text-import-actions">
                    <el-button type="primary" @click="importFromText" :disabled="!batchText">导入文本</el-button>
                    <el-button @click="clearText">清空</el-button>
                  </div>
                </div>
              </el-card>
            </div>

            <el-card class="import-format-card">
              <template #header>
                <h3>导入格式设置</h3>
              </template>
              <el-form :model="importSettings" label-width="120px">
                <el-form-item label="文件包含表头">
                  <el-switch v-model="importSettings.hasHeader" />
                </el-form-item>
                <el-form-item label="分隔符" v-if="fileType === 'csv' || fileType === 'txt'">
                  <el-select v-model="importSettings.delimiter">
                    <el-option label="逗号 (,)" value="," />
                    <el-option label="制表符 (Tab)" value="\t" />
                    <el-option label="分号 (;)" value=";" />
                    <el-option label="空格" value=" " />
                  </el-select>
                </el-form-item>
                <el-form-item label="问题列名/索引">
                  <el-input v-model="importSettings.questionColumn" placeholder="列名或索引号" />
                </el-form-item>
                <el-form-item label="默认题型">
                  <el-select v-model="importSettings.defaultQuestionType">
                    <el-option label="简单事实" value="SIMPLE_FACT" />
                    <el-option label="单选题" value="SINGLE_CHOICE" />
                    <el-option label="多选题" value="MULTIPLE_CHOICE" />
                    <el-option label="主观题" value="SUBJECTIVE" />
                  </el-select>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="批量处理" name="process">
          <div class="tab-content">
            <div class="batch-actions">
              <el-form :inline="true" class="action-form">
                <el-form-item label="操作类型">
                  <el-select v-model="batchAction" placeholder="选择操作">
                    <el-option label="批量标准化" value="standardize" />
                    <el-option label="批量分类" value="classify" />
                    <el-option label="批量打标签" value="tag" />
                    <el-option label="批量筛选" value="filter" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="applyBatchAction">应用操作</el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-table
              :data="questions"
              style="width: 100%"
              border
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="content" label="问题内容" min-width="300">
                <template #default="scope">
                  <div class="question-content">{{ scope.row.content }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="questionType" label="题型" width="100">
                <template #default="scope">
                  <el-tag :type="getQuestionTypeTag(scope.row.questionType)">
                    {{ getQuestionTypeName(scope.row.questionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusTag(scope.row.status)">
                    {{ getStatusName(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button type="text" @click="editQuestion(scope.row)">编辑</el-button>
                  <el-button type="text" @click="standardizeQuestion(scope.row)">标准化</el-button>
                  <el-button type="text" @click="removeQuestion(scope.row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-container">
              <el-pagination
                v-model:currentPage="currentPage"
                :page-sizes="[10, 20, 50, 100]"
                v-model:page-size="pageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalQuestions"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="处理结果" name="results">
          <div class="tab-content">
            <el-alert
              v-if="processResults.show"
              :title="processResults.title"
              :type="processResults.type"
              :description="processResults.message"
              show-icon
              closable
              @close="processResults.show = false"
            />

            <div class="results-summary">
              <el-descriptions title="处理结果统计" :column="3" border>
                <el-descriptions-item label="总问题数">{{ processStats.total }}</el-descriptions-item>
                <el-descriptions-item label="成功处理">{{ processStats.success }}</el-descriptions-item>
                <el-descriptions-item label="处理失败">{{ processStats.failed }}</el-descriptions-item>
                <el-descriptions-item label="已标准化">{{ processStats.standardized }}</el-descriptions-item>
                <el-descriptions-item label="已分类">{{ processStats.classified }}</el-descriptions-item>
                <el-descriptions-item label="已打标签">{{ processStats.tagged }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <el-divider>处理明细</el-divider>

            <el-table
              :data="processedQuestions"
              style="width: 100%"
              border
            >
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="originalContent" label="原始问题" min-width="200" />
              <el-table-column prop="standardContent" label="标准化问题" min-width="200" />
              <el-table-column prop="questionType" label="题型" width="100">
                <template #default="scope">
                  <el-tag :type="getQuestionTypeTag(scope.row.questionType)">
                    {{ getQuestionTypeName(scope.row.questionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="tags" label="标签" min-width="150">
                <template #default="scope">
                  <div class="tag-group">
                    <el-tag
                      v-for="tag in scope.row.tags"
                      :key="tag.id"
                      size="small"
                      effect="plain"
                      class="question-tag"
                    >
                      {{ tag.name }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getProcessStatusTag(scope.row.status)">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-container">
              <el-pagination
                v-model:currentPage="resultCurrentPage"
                :page-sizes="[10, 20, 50, 100]"
                v-model:page-size="resultPageSize"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalProcessedQuestions"
                @size-change="handleResultSizeChange"
                @current-change="handleResultCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 编辑问题对话框 -->
    <el-dialog
      v-model="editDialog.visible"
      title="编辑问题"
      width="600px"
      destroy-on-close
    >
      <el-form :model="editDialog.form" label-width="100px">
        <el-form-item label="问题内容">
          <el-input
            v-model="editDialog.form.content"
            type="textarea"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="editDialog.form.questionType" style="width: 100%">
            <el-option label="简单事实" value="SIMPLE_FACT" />
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="editDialog.form.tags"
            multiple
            filterable
            remote
            :remote-method="searchTags"
            placeholder="请输入标签关键词搜索"
            style="width: 100%"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="saveQuestion">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

// 状态变量
const activeTab = ref('import')
const fileList = ref([])
const fileType = ref('')
const batchText = ref('')
const questions = ref([])
const processedQuestions = ref([])
const selectedQuestions = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalQuestions = ref(0)
const resultCurrentPage = ref(1)
const resultPageSize = ref(20)
const totalProcessedQuestions = ref(0)
const batchAction = ref('standardize')
const availableTags = ref([])

// 导入设置
const importSettings = reactive({
  hasHeader: true,
  delimiter: ',',
  questionColumn: 'content',
  defaultQuestionType: 'SIMPLE_FACT'
})

// 导入信息提示
const importInfo = reactive({
  show: false,
  title: '',
  message: '',
  type: 'info'
})

// 处理结果提示
const processResults = reactive({
  show: false,
  title: '',
  message: '',
  type: 'info'
})

// 处理统计
const processStats = reactive({
  total: 0,
  success: 0,
  failed: 0,
  standardized: 0,
  classified: 0,
  tagged: 0
})

// 编辑对话框状态
const editDialog = reactive({
  visible: false,
  form: {
    id: '',
    content: '',
    questionType: 'SIMPLE_FACT',
    tags: []
  }
})

// 计算属性
const hasSelectedQuestions = computed(() => selectedQuestions.value.length > 0)
const hasProcessedQuestions = computed(() => processedQuestions.value.length > 0)

// 生命周期钩子
onMounted(() => {
  // 加载数据
  loadQuestions()
})

// 方法
const handleFileChange = (file) => {
  const fileName = file.name || ''
  const extension = fileName.split('.').pop()?.toLowerCase()
  fileType.value = extension
}

const clearFiles = () => {
  fileList.value = []
  fileType.value = ''
}

const clearText = () => {
  batchText.value = ''
}

const handleUpload = () => {
  // 实际应用中应调用API上传文件
  // 这里使用模拟数据
  importInfo.show = true
  importInfo.title = '文件上传成功'
  importInfo.message = `成功导入 ${Math.floor(Math.random() * 100) + 10} 个问题`
  importInfo.type = 'success'

  // 加载问题数据
  loadQuestions()
}

const importFromText = () => {
  if (!batchText.value) {
    ElMessage.warning('请输入问题文本')
    return
  }

  // 实际应用中应调用API处理文本
  // 这里使用模拟数据
  const lines = batchText.value.split('\n').filter(line => line.trim())

  importInfo.show = true
  importInfo.title = '文本导入成功'
  importInfo.message = `成功导入 ${lines.length} 个问题`
  importInfo.type = 'success'

  // 加载问题数据
  loadQuestions()
}

const importQuestions = () => {
  // 切换到导入标签页
  activeTab.value = 'import'
}

const loadQuestions = () => {
  // 实际应用中应调用API获取问题列表
  // 这里使用模拟数据
  questions.value = Array.from({ length: 50 }, (_, i) => ({
    id: i + 1,
    content: `这是一个示例问题 ${i + 1}，用于演示批量处理功能`,
    questionType: ['SIMPLE_FACT', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SUBJECTIVE'][i % 4],
    status: ['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'][i % 4],
    tags: []
  }))

  totalQuestions.value = 100
}

const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadQuestions()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadQuestions()
}

const handleResultSizeChange = (size) => {
  resultPageSize.value = size
}

const handleResultCurrentChange = (page) => {
  resultCurrentPage.value = page
}

const getQuestionTypeTag = (type) => {
  const types = {
    'SIMPLE_FACT': 'success',
    'SINGLE_CHOICE': 'primary',
    'MULTIPLE_CHOICE': 'warning',
    'SUBJECTIVE': 'info'
  }
  return types[type] || ''
}

const getQuestionTypeName = (type) => {
  const types = {
    'SIMPLE_FACT': '简单事实',
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'SUBJECTIVE': '主观题'
  }
  return types[type] || type
}

const getStatusTag = (status) => {
  const statuses = {
    'DRAFT': 'info',
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return statuses[status] || ''
}

const getStatusName = (status) => {
  const statuses = {
    'DRAFT': '草稿',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return statuses[status] || status
}

const getProcessStatusTag = (status) => {
  if (status === '成功') return 'success'
  if (status === '失败') return 'danger'
  return 'info'
}

const editQuestion = (question) => {
  editDialog.form = {
    id: question.id,
    content: question.content,
    questionType: question.questionType,
    tags: question.tags.map(tag => tag.id)
  }
  editDialog.visible = true
}

const saveQuestion = () => {
  // 实际应用中应调用API保存问题
  ElMessage.success('问题保存成功')
  editDialog.visible = false
  loadQuestions()
}

const standardizeQuestion = (question) => {
  // 实际应用中应跳转到标准化页面或打开标准化对话框
  ElMessage.info(`准备标准化问题: ${question.content}`)
}

const removeQuestion = (question) => {
  ElMessageBox.confirm('确定要从列表中移除此问题吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 实际应用中应调用API移除问题
    ElMessage.success('问题已移除')
    loadQuestions()
  }).catch(() => {})
}

const searchTags = (query) => {
  if (query) {
    // 实际应用中应调用API搜索标签
    // 这里使用模拟数据
    availableTags.value = [
      { id: 1, name: `数学${query}` },
      { id: 2, name: `历史${query}` },
      { id: 3, name: `科学${query}` },
      { id: 4, name: `文学${query}` }
    ]
  } else {
    availableTags.value = []
  }
}

const processQuestions = () => {
  if (!selectedQuestions.value.length) {
    ElMessage.warning('请选择要处理的问题')
    return
  }

  // 实际应用中应调用API处理问题
  ElMessage.info('正在处理问题，请稍候...')

  // 模拟处理过程
  setTimeout(() => {
    processResults.show = true
    processResults.title = '批量处理完成'
    processResults.message = `成功处理 ${selectedQuestions.value.length} 个问题`
    processResults.type = 'success'

    // 更新处理统计
    processStats.total = selectedQuestions.value.length
    processStats.success = Math.floor(selectedQuestions.value.length * 0.9)
    processStats.failed = selectedQuestions.value.length - processStats.success
    processStats.standardized = Math.floor(processStats.success * 0.7)
    processStats.classified = Math.floor(processStats.success * 0.8)
    processStats.tagged = Math.floor(processStats.success * 0.6)

    // 生成处理结果
    processedQuestions.value = selectedQuestions.value.map((question, index) => ({
      id: question.id,
      originalContent: question.content,
      standardContent: `标准化后的问题 ${question.id}`,
      questionType: question.questionType,
      tags: [
        { id: 1, name: '数学' },
        { id: 2, name: '代数' }
      ],
      status: index < processStats.success ? '成功' : '失败'
    }))

    totalProcessedQuestions.value = processedQuestions.value.length

    // 切换到结果标签页
    activeTab.value = 'results'
  }, 1500)
}

const applyBatchAction = () => {
  if (!selectedQuestions.value.length) {
    ElMessage.warning('请选择要处理的问题')
    return
  }

  let actionName = ''
  switch (batchAction.value) {
    case 'standardize': actionName = '标准化'; break
    case 'classify': actionName = '分类'; break
    case 'tag': actionName = '打标签'; break
    case 'filter': actionName = '筛选'; break
  }

  ElMessageBox.confirm(`确定要对选中的 ${selectedQuestions.value.length} 个问题执行${actionName}操作吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    processQuestions()
  }).catch(() => {})
}

const exportQuestions = () => {
  if (!processedQuestions.value.length) {
    ElMessage.warning('暂无处理结果可导出')
    return
  }

  // 实际应用中应调用API导出结果
  ElMessage.success('处理结果导出成功')
}
</script>

<style scoped>
.question-batch {
  padding: 20px;
}

.batch-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.tab-content {
  padding: 15px 0;
}

.import-methods {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.import-method-card {
  flex: 1;
}

.method-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area,
.text-import-area {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.upload-actions,
.text-import-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.import-format-card {
  margin-top: 20px;
}

.batch-actions {
  margin-bottom: 15px;
}

.question-content {
  max-height: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.results-summary {
  margin-bottom: 20px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.question-tag {
  margin-right: 5px;
}
</style>
