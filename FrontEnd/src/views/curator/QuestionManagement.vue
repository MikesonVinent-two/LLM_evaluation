<template>
  <div class="question-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>原始问题管理</h2>
          <div class="header-actions">
            <el-upload
              class="batch-upload"
              :show-file-list="false"
              accept=".json, .csv"
              :auto-upload="false"
              :on-change="handleFileChange"
            >
              <el-button type="primary" :loading="isUploading">
                <el-icon><Upload /></el-icon>批量导入
              </el-button>
            </el-upload>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>新增问题
            </el-button>
            <el-button type="primary" @click="fetchQuestions">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索过滤区域 -->
      <div class="search-area">
        <el-form :model="searchForm" inline>
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="标题或内容关键词"
              clearable
              prefix-icon="Search"
            />
          </el-form-item>
          <el-form-item label="来源网站">
            <el-input
              v-model="searchForm.sourceSite"
              placeholder="来源网站"
              clearable
              prefix-icon="House"
            />
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="searchForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入标签（回车确认）"
              clearable
              style="min-width: 220px;"
              @keyup.enter="handleSearchTagEnter"
              popper-class="tag-select-dropdown"
            >
              <el-option
                v-for="tag in allTags"
                :key="tag"
                :label="tag"
                :value="tag"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标准化状态">
            <el-select v-model="searchForm.standardized" placeholder="全部" clearable>
              <el-option label="已标准化" :value="true" />
              <el-option label="未标准化" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchQuestions" :icon="Search">搜索</el-button>
            <el-button @click="resetSearch" :icon="RefreshRight">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 问题列表 -->
      <el-table
        v-loading="loading"
        :data="questions"
        style="width: 100%"
        border
        stripe
        row-key="id"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sourceSite" label="来源" width="120" show-overflow-tooltip />
        <el-table-column prop="crawlTime" label="收集时间" width="180" sortable />
        <el-table-column label="标签" width="200" show-overflow-tooltip>
          <template #default="scope">
            <div class="tag-container">
              <el-tag
                v-for="tag in scope.row.tags"
                :key="tag"
                size="small"
                class="tag-item"
                effect="light"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!scope.row.tags || scope.row.tags.length === 0" class="no-tags">无标签</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标准化状态" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.standardized ? 'success' : 'info'"
              effect="plain"
            >
              {{ scope.row.standardized ? '已标准化' : '未标准化' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="openEditDialog(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click.stop="confirmDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
    </el-card>

    <!-- 新增/编辑问题对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑原始问题' : '新增原始问题'"
      width="70%"
      destroy-on-close
    >
      <el-form
        ref="questionFormRef"
        :model="questionForm"
        :rules="questionRules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="questionForm.title" placeholder="请输入问题标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            placeholder="请输入问题详细内容"
            :rows="6"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="来源网站" prop="sourceSite">
          <el-input v-model="questionForm.sourceSite" placeholder="请输入来源网站" />
        </el-form-item>
        <el-form-item label="来源URL" prop="sourceUrl">
          <el-input v-model="questionForm.sourceUrl" placeholder="请输入来源URL" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <div class="tag-input-container">
            <el-select
              v-model="questionForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入标签（回车确认）"
              style="width: 100%"
              @keyup.enter="handleTagEnter"
            >
              <el-option
                v-for="tag in allTags"
                :key="tag"
                :label="tag"
                :value="tag"
              />
            </el-select>
            <el-button
              type="primary"
              @click="getTagSuggestions"
              :disabled="!questionForm.content"
              size="default"
              style="margin-left: 10px;"
            >
              获取推荐标签
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="其他元数据" prop="otherMetadata">
          <el-input
            v-model="questionForm.otherMetadata"
            type="textarea"
            placeholder="可选：JSON格式的其他元数据"
            :rows="3"
          />
        </el-form-item>

        <!-- 原始回答部分 -->
        <el-divider content-position="center">添加原始回答（可选）</el-divider>

        <div class="answers-input-section">
          <div class="answers-header">
            <h4>为该问题添加原始回答</h4>
            <el-button type="primary" size="small" @click="addAnswerInput">添加一个回答</el-button>
          </div>

          <div v-if="questionAnswerInputs.length > 0">
            <el-collapse v-model="activeAnswers">
              <el-collapse-item
                v-for="(answer, index) in questionAnswerInputs"
                :key="index"
                :title="`回答 #${index + 1}${answer.authorInfo ? ' - ' + answer.authorInfo : ''}`"
                :name="index"
              >
                <div class="answer-input-container">
                  <el-form :model="answer">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="作者信息" :prop="`authorInfo`" :rules="answerRules.authorInfo">
                          <el-input v-model="answer.authorInfo" placeholder="请输入作者信息" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="发布时间" :prop="`publishTime`" :rules="answerRules.publishTime">
                          <el-date-picker
                            v-model="answer.publishTime"
                            type="datetime"
                            placeholder="请选择发布时间"
                            format="YYYY-MM-DDTHH:mm:ss"
                            value-format="YYYY-MM-DDTHH:mm:ss"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>

                    <el-form-item label="回答内容" :prop="`content`" :rules="answerRules.content">
                      <el-input
                        v-model="answer.content"
                        type="textarea"
                        placeholder="请输入回答内容"
                        :rows="4"
                        maxlength="5000"
                        show-word-limit
                      />
                    </el-form-item>

                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-form-item label="点赞数" :prop="`upvotes`" :rules="answerRules.upvotes">
                          <el-input-number v-model="answer.upvotes" :min="0" :max="999999" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="是否被采纳" :prop="`isAccepted`">
                          <el-switch v-model="answer.isAccepted" />
                        </el-form-item>
                      </el-col>
                    </el-row>

                    <el-form-item label="其他元数据" :prop="`otherMetadata`">
                      <el-input
                        v-model="answer.otherMetadata"
                        type="textarea"
                        placeholder="可选：JSON格式的其他元数据"
                        :rows="2"
                      />
                    </el-form-item>

                    <el-form-item>
                      <el-button type="danger" @click="removeAnswerInput(index)">删除此回答</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <div v-else class="no-answers-input">
            <el-empty description="暂未添加回答" />
          </div>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestion" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 问题详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="问题详情"
      width="70%"
      destroy-on-close
    >
      <div v-if="currentQuestion" class="question-detail">
        <h3>{{ currentQuestion.title }}</h3>
        <div class="detail-item">
          <div class="detail-label">ID：</div>
          <div class="detail-value">{{ currentQuestion.id }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">来源：</div>
          <div class="detail-value">{{ currentQuestion.sourceSite }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">来源URL：</div>
          <div class="detail-value">
            <a :href="currentQuestion.sourceUrl" target="_blank">{{ currentQuestion.sourceUrl }}</a>
          </div>
        </div>
        <div class="detail-item">
          <div class="detail-label">收集时间：</div>
          <div class="detail-value">{{ currentQuestion.crawlTime }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">标签：</div>
          <div class="detail-value">
            <div class="tag-container">
              <el-tag
                v-for="tag in currentQuestion.tags"
                :key="tag"
                size="small"
                class="tag-item"
                effect="light"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!currentQuestion.tags || currentQuestion.tags.length === 0" class="no-tags">无标签</span>
            </div>
          </div>
        </div>
        <div class="detail-item">
          <div class="detail-label">标准化状态：</div>
          <div class="detail-value">
            <el-tag
              :type="currentQuestion.standardized ? 'success' : 'info'"
              effect="plain"
            >
              {{ currentQuestion.standardized ? '已标准化' : '未标准化' }}
            </el-tag>
          </div>
        </div>
        <div class="detail-item content-item">
          <div class="detail-label">内容：</div>
          <div class="detail-value content-value">{{ currentQuestion.content }}</div>
        </div>
        <div v-if="currentQuestion.otherMetadata" class="detail-item content-item">
          <div class="detail-label">其他元数据：</div>
          <div class="detail-value content-value">
            <pre>{{ formatJson(currentQuestion.otherMetadata) }}</pre>
          </div>
        </div>

        <!-- 原始回答部分 -->
        <el-divider content-position="center">原始回答</el-divider>

        <div class="answers-section">
          <div class="answers-header">
            <h4>该问题的原始回答列表</h4>
            <el-button type="primary" size="small" @click="openAddAnswerDialog">添加回答</el-button>
          </div>

          <el-table
            :data="questionAnswers"
            style="width: 100%"
            border
            v-loading="loadingAnswers"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="authorInfo" label="作者" width="120" show-overflow-tooltip />
            <el-table-column prop="content" label="回答内容" min-width="250" show-overflow-tooltip />
            <el-table-column prop="publishTime" label="发布时间" width="180" />
            <el-table-column prop="upvotes" label="点赞数" width="80" align="center" />
            <el-table-column label="被采纳" width="80" align="center">
              <template #default="scope">
                <el-tag
                  :type="scope.row.isAccepted ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ scope.row.isAccepted ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  type="danger"
                  link
                  size="small"
                  @click="confirmDeleteAnswer(scope.row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="questionAnswers.length === 0 && !loadingAnswers" class="no-answers">
            <el-empty description="暂无原始回答" />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 添加原始回答对话框 -->
    <el-dialog
      v-model="answerDialogVisible"
      title="添加原始回答"
      width="60%"
      destroy-on-close
    >
      <el-form
        ref="answerFormRef"
        :model="answerForm"
        :rules="answerRules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="作者信息" prop="authorInfo">
          <el-input v-model="answerForm.authorInfo" placeholder="请输入作者信息" />
        </el-form-item>
        <el-form-item label="回答内容" prop="content">
          <el-input
            v-model="answerForm.content"
            type="textarea"
            placeholder="请输入回答内容"
            :rows="6"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="发布时间" prop="publishTime">
          <el-date-picker
            v-model="answerForm.publishTime"
            type="datetime"
            placeholder="请选择发布时间"
            format="YYYY-MM-DDTHH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="点赞数" prop="upvotes">
          <el-input-number v-model="answerForm.upvotes" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="是否被采纳" prop="isAccepted">
          <el-switch v-model="answerForm.isAccepted" />
        </el-form-item>
        <el-form-item label="其他元数据" prop="otherMetadata">
          <el-input
            v-model="answerForm.otherMetadata"
            type="textarea"
            placeholder="可选：JSON格式的其他元数据"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="answerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAnswer" :loading="submittingAnswer">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量导入原始问题"
      width="70%"
      destroy-on-close
    >
      <div class="batch-upload-container">
        <div class="upload-instructions">
          <h3>导入说明</h3>
          <p>支持以下格式导入：</p>
          <ol>
            <li>
              <strong>标准JSON格式</strong>：数组格式，每项包含title、content、sourceSite、sourceUrl、tags字段
            </li>
            <li>
              <strong>StackExchange格式</strong>：数组格式，每项包含question_title、question_body、answer_body、source、source_url、source_question_id等字段
            </li>
            <li>
              <strong>CSV格式</strong>：第一行为表头，必须包含title、content字段，可选包含sourceSite、sourceUrl、tags字段
            </li>
          </ol>
          <p>最大支持导入500条记录</p>
        </div>

        <div class="file-preview" v-if="batchData.length > 0">
          <h3>预览数据（显示前5条）</h3>

          <!-- 标准格式数据预览 -->
          <div v-if="batchDataFormat === 'standard'">
            <el-table :data="batchData.slice(0, 5)" border style="width: 100%">
              <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
              <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="sourceSite" label="来源网站" width="120" show-overflow-tooltip />
              <el-table-column prop="sourceUrl" label="来源URL" width="150" show-overflow-tooltip />
              <el-table-column label="标签" width="150" show-overflow-tooltip>
                <template #default="scope">
                  <div class="tag-container">
                    <el-tag
                      v-for="tag in scope.row.tags"
                      :key="tag"
                      size="small"
                      class="tag-item"
                      effect="light"
                    >
                      {{ tag }}
                    </el-tag>
                    <span v-if="!scope.row.tags || scope.row.tags.length === 0" class="no-tags">无标签</span>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- StackExchange格式数据预览 -->
          <div v-else>
            <el-table :data="batchData.slice(0, 5)" border style="width: 100%">
              <el-table-column label="问题标题" min-width="150" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.question.title }}
                </template>
              </el-table-column>
              <el-table-column label="问题内容" min-width="150" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.question.content }}
                </template>
              </el-table-column>
              <el-table-column label="来源" width="120" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.question.sourceSite }}
                </template>
              </el-table-column>
              <el-table-column label="回答数量" width="80" align="center">
                <template #default="scope">
                  {{ scope.row.answers.length }}
                </template>
              </el-table-column>
              <el-table-column label="回答预览" min-width="200" show-overflow-tooltip>
                <template #default="scope">
                  <div v-if="scope.row.answers.length > 0">
                    {{ scope.row.answers[0].content.substring(0, 100) }}{{ scope.row.answers[0].content.length > 100 ? '...' : '' }}
                  </div>
                  <span v-else>无回答</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="batch-summary">
            <p>共 {{ batchData.length }} 条数据</p>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBatchImport" :loading="isUploading">
            开始导入
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type UploadFile } from 'element-plus'
import { Plus, Refresh, Upload, Search, RefreshRight } from '@element-plus/icons-vue'
import { createRawQuestion, getRawQuestions, deleteRawQuestion, searchRawQuestions, getRawQuestionsByStatus, createRawAnswer, deleteRawAnswer, createQuestionWithAnswers, getQuestionAnswers } from '@/api/rawData'
import type { RawQuestionDto, RawQuestionSearchItem, RawAnswerDto, RawAnswerResponse, QuestionWithAnswersDto, PageResponse } from '@/api/rawData'
import { getTagRecommendations } from '@/utils/tagUtils'
import { getAllTags } from '@/api/tags'
import Papa from 'papaparse'

// 搜索原始问题的响应数据项接口（扩展现有类型以添加otherMetadata）
interface ExtendedRawQuestionSearchItem extends RawQuestionSearchItem {
  otherMetadata?: string;
}

// 扩展的RawQuestionPageResponse接口
interface ExtendedRawQuestionPageResponse {
  content: ExtendedRawQuestionSearchItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
  sort?: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
}

// 加载状态
const loading = ref(false)
const submitting = ref(false)
const isUploading = ref(false)
const loadingAnswers = ref(false)
const submittingAnswer = ref(false)

// 对话框控制
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const answerDialogVisible = ref(false)
const isEdit = ref(false)

// 表单引用
const questionFormRef = ref<FormInstance>()
const answerFormRef = ref<FormInstance>()

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  keyword: '',
  sourceSite: '',
  standardized: null as boolean | null,
  tags: [] as string[]
})

// 问题表单
const questionForm = reactive<RawQuestionDto>({
  title: '',
  content: '',
  sourceSite: '',
  sourceUrl: '',
  tags: [],
  otherMetadata: ''
})

// 回答表单
const answerForm = reactive<RawAnswerDto>({
  rawQuestionId: 0,
  authorInfo: '',
  content: '',
  publishTime: new Date().toISOString(),
  upvotes: 0,
  isAccepted: false,
  otherMetadata: ''
})

// 回答表单验证规则
const answerRules = {
  authorInfo: [
    { required: true, message: '请输入作者信息', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入回答内容', trigger: 'blur' },
    { min: 5, max: 5000, message: '内容长度应在5-5000个字符之间', trigger: 'blur' }
  ],
  publishTime: [
    { required: true, message: '请选择发布时间', trigger: 'change' }
  ],
  upvotes: [
    { required: true, message: '请输入点赞数', trigger: 'change' }
  ]
}

// 问题表单校验规则
const questionRules = {
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度应在2-200个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入问题内容', trigger: 'blur' },
    { min: 5, max: 2000, message: '内容长度应在5-2000个字符之间', trigger: 'blur' }
  ],
  sourceSite: [
    { required: true, message: '请输入来源网站', trigger: 'blur' }
  ],
  sourceUrl: [
    { pattern: /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([\/\w .-]*)*\/?$/, message: '请输入有效的URL', trigger: 'blur' }
  ]
}

// 当前问题的回答列表
const questionAnswers = ref<RawAnswerResponse[]>([])

// 新增问题时的回答输入列表
const questionAnswerInputs = ref<Omit<RawAnswerDto, 'rawQuestionId'>[]>([])
const activeAnswers = ref<number[]>([0]) // 展开的回答面板

// 批量导入数据
const batchData = ref<any[]>([])
// 批量数据格式标识: 'standard' 或 'stackexchange'
const batchDataFormat = ref('standard')

// 问题列表
const questions = ref<ExtendedRawQuestionSearchItem[]>([])

// 当前查看的问题
const currentQuestion = ref<ExtendedRawQuestionSearchItem | null>(null)

// 所有可用标签
const allTags = ref<string[]>([])

// 格式化JSON显示
const formatJson = (jsonStr: string) => {
  try {
    const json = JSON.parse(jsonStr)
    return JSON.stringify(json, null, 2)
  } catch (error) {
    return jsonStr
  }
}

// 获取所有标签
const loadAllTags = async () => {
  try {
    const response = await getAllTags()
    if (response) {
      // 提取标签名称(tagName)
      allTags.value = response.map((tag: any) => tag.tagName || tag)
    }
  } catch (error) {
    console.error('获取标签失败:', error)
    ElMessage.error('获取标签列表失败')
  }
}

// 获取标签推荐
const getTagSuggestions = async () => {
  if (!questionForm.content) {
    ElMessage.warning('请先填写问题内容以获取标签推荐')
    return
  }

  try {
    const recommendedTags = await getTagRecommendations(
      questionForm.content,
      questionForm.tags
    )

    if (recommendedTags.length > 0) {
      // 合并推荐标签和已有标签，去重
      const uniqueTags = [...new Set([...questionForm.tags, ...recommendedTags])]
      questionForm.tags = uniqueTags
      ElMessage.success(`已添加${recommendedTags.length}个推荐标签`)
    } else {
      ElMessage.info('未找到合适的推荐标签')
    }
  } catch (error) {
    console.error('获取标签推荐失败:', error)
    ElMessage.error('获取标签推荐失败，请重试')
  }
}

// 初始化加载数据
onMounted(() => {
  fetchQuestions()
  loadAllTags() // 获取所有可用标签
})

// 获取问题列表
const fetchQuestions = async () => {
  loading.value = true
  try {
    const response = await getRawQuestions({
      page: pagination.currentPage - 1,
      size: pagination.pageSize,
      sort: 'id,desc'
    })

    // 安全地访问响应数据
    const responseData = response as unknown as ExtendedRawQuestionPageResponse;
    if (responseData && responseData.content) {
      questions.value = responseData.content || []
      pagination.total = responseData.totalElements || 0
    } else {
      questions.value = []
      pagination.total = 0
    }

    ElMessage.success('数据加载成功')
  } catch (error) {
    console.error('获取问题列表失败:', error)
    ElMessage.error('获取问题列表失败，请重试')
  } finally {
    loading.value = false
  }
}

// 搜索问题
const searchQuestions = async () => {
  loading.value = true
  try {
    let response;

    // 根据标准化状态搜索
    if (searchForm.standardized !== null) {
      // 使用新的API接口
      const params: {
        page?: number | string;
        size?: number | string;
        sort?: string;
        keyword?: string[];
        tags?: string[];
        unStandardized?: boolean;
      } = {
        page: pagination.currentPage - 1,
        size: pagination.pageSize,
        sort: 'id,desc'
      }

      // 设置关键词
      if (searchForm.keyword) {
        params.keyword = [searchForm.keyword]
      }

      // 设置来源网站
      if (searchForm.sourceSite) {
        if (!params.keyword) {
          params.keyword = []
        }
        params.keyword.push(searchForm.sourceSite)
      }

      // 设置标签
      if (searchForm.tags.length > 0) {
        params.tags = searchForm.tags
      }

      // 设置标准化状态
      params.unStandardized = !searchForm.standardized

      response = await searchRawQuestions(params)
    } else {
      // 关键词搜索
      const params: {
        page?: number | string;
        size?: number | string;
        sort?: string;
        keyword?: string[];
        tags?: string[];
        unStandardized?: boolean;
      } = {
        page: pagination.currentPage - 1,
        size: pagination.pageSize,
        sort: 'id,desc'
      }

      if (searchForm.keyword) {
        // 将关键词转换为数组格式
        params.keyword = [searchForm.keyword]
      }

      if (searchForm.sourceSite) {
        // 保留sourceSite作为搜索条件，但不作为API参数
        // 可以考虑将其添加到keyword中
        if (!params.keyword) {
          params.keyword = []
        }
        params.keyword.push(searchForm.sourceSite)
      }

      if (searchForm.tags.length > 0) {
        params.tags = searchForm.tags
      }

      response = await searchRawQuestions(params)
    }

    // 检查并安全地获取数据
    const responseData = response as unknown as PageResponse<ExtendedRawQuestionSearchItem>;
    if (responseData && responseData.content) {
      questions.value = responseData.content || []
      pagination.total = responseData.totalElements || 0
    } else {
      questions.value = []
      pagination.total = 0
    }

    ElMessage.success('搜索完成')
  } catch (error) {
    console.error('搜索问题失败:', error)
    ElMessage.error('搜索问题失败，请重试')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.sourceSite = ''
  searchForm.standardized = null
  searchForm.tags = []
  pagination.currentPage = 1
  fetchQuestions()
}

// 处理分页大小变化
const handleSizeChange = (val: number) => {
  pagination.pageSize = val
  fetchQuestions()
}

// 处理当前页变化
const handleCurrentChange = (val: number) => {
  pagination.currentPage = val
  fetchQuestions()
}

// 打开新增问题对话框
const openCreateDialog = () => {
  isEdit.value = false
  // 重置表单
  questionForm.title = ''
  questionForm.content = ''
  questionForm.sourceSite = ''
  questionForm.sourceUrl = ''
  questionForm.tags = []
  questionForm.otherMetadata = ''
  // 清空回答输入
  questionAnswerInputs.value = []
  activeAnswers.value = []
  dialogVisible.value = true
}

// 打开编辑问题对话框
const openEditDialog = (row: ExtendedRawQuestionSearchItem) => {
  isEdit.value = true
  // 填充表单
  questionForm.title = row.title
  questionForm.content = row.content
  questionForm.sourceSite = row.sourceSite
  questionForm.sourceUrl = row.sourceUrl
  questionForm.tags = [...row.tags]
  questionForm.otherMetadata = row.otherMetadata || ''
  dialogVisible.value = true
}

// 提交问题表单
const submitQuestion = async () => {
  if (!questionFormRef.value) return

  await questionFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const formData = { ...questionForm }

        // 确保otherMetadata为字符串
        if (formData.otherMetadata && typeof formData.otherMetadata !== 'string') {
          formData.otherMetadata = JSON.stringify(formData.otherMetadata)
        }

        // 检查是否有回答需要一起创建
        if (questionAnswerInputs.value.length > 0) {
          // 创建问题和回答
          const questionWithAnswers: QuestionWithAnswersDto = {
            question: {
              sourceUrl: formData.sourceUrl,
              sourceSite: formData.sourceSite,
              title: formData.title,
              content: formData.content,
              otherMetadata: formData.otherMetadata || '{}'
            },
            answers: questionAnswerInputs.value.map(answer => ({
              authorInfo: answer.authorInfo,
              content: answer.content,
              publishTime: answer.publishTime,
              upvotes: answer.upvotes,
              isAccepted: answer.isAccepted,
              otherMetadata: answer.otherMetadata || '{}'
            }))
          }

          await createQuestionWithAnswers(questionWithAnswers)
        } else {
          // 只创建问题
          await createRawQuestion(formData)
        }

        ElMessage.success(`${isEdit.value ? '更新' : '创建'}问题成功`)
        dialogVisible.value = false
        fetchQuestions()
      } catch (error) {
        console.error(`${isEdit.value ? '更新' : '创建'}问题失败:`, error)
        ElMessage.error(`${isEdit.value ? '更新' : '创建'}问题失败，请重试`)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 确认删除问题
const confirmDelete = (row: ExtendedRawQuestionSearchItem) => {
  ElMessageBox.confirm(
    `确定要删除问题 "${row.title}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteRawQuestion(row.id)
      ElMessage.success('删除成功')
      fetchQuestions()
    } catch (error) {
      console.error('删除问题失败:', error)
      ElMessage.error('删除问题失败，请重试')
    }
  }).catch(() => {
    // 用户取消删除，不做处理
  })
}

// 处理行点击
const handleRowClick = (row: ExtendedRawQuestionSearchItem) => {
  currentQuestion.value = row
  detailDialogVisible.value = true
  // 加载问题的回答
  fetchQuestionAnswers(row.id)
}

// 获取问题的回答列表
const fetchQuestionAnswers = async (questionId: number) => {
  loadingAnswers.value = true
  try {
    const response = await getQuestionAnswers(questionId, {
      page: 0,
      size: 50,
      sort: 'publishTime,desc'
    });

    questionAnswers.value = response.content || [];
    loadingAnswers.value = false;
  } catch (error) {
    console.error('获取问题回答失败:', error);
    ElMessage.error('获取问题回答失败');
    loadingAnswers.value = false;
  }
}

// 打开添加回答对话框
const openAddAnswerDialog = () => {
  if (!currentQuestion.value) return

  // 重置回答表单
  answerForm.rawQuestionId = currentQuestion.value.id
  answerForm.authorInfo = ''
  answerForm.content = ''
  answerForm.publishTime = new Date().toISOString()
  answerForm.upvotes = 0
  answerForm.isAccepted = false
  answerForm.otherMetadata = ''

  answerDialogVisible.value = true
}

// 提交回答
const submitAnswer = async () => {
  if (!answerFormRef.value) return

  await answerFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingAnswer.value = true
      try {
        const response = await createRawAnswer(answerForm)

        // 添加到当前回答列表
        questionAnswers.value.push(response)

        ElMessage.success('添加回答成功')
        answerDialogVisible.value = false
      } catch (error) {
        console.error('添加回答失败:', error)
        ElMessage.error('添加回答失败，请重试')
      } finally {
        submittingAnswer.value = false
      }
    }
  })
}

// 确认删除回答
const confirmDeleteAnswer = (answer: RawAnswerResponse) => {
  ElMessageBox.confirm(
    `确定要删除此回答吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteRawAnswer(answer.id)
      ElMessage.success('删除回答成功')
      // 从回答列表中移除
      questionAnswers.value = questionAnswers.value.filter(a => a.id !== answer.id)
    } catch (error) {
      console.error('删除回答失败:', error)
      ElMessage.error('删除回答失败，请重试')
    }
  }).catch(() => {
    // 用户取消删除，不做处理
  })
}

// 添加回答输入
const addAnswerInput = () => {
  questionAnswerInputs.value.push({
    authorInfo: '',
    content: '',
    publishTime: new Date().toISOString(),
    upvotes: 0,
    isAccepted: false,
    otherMetadata: ''
  })

  // 默认展开新添加的回答
  activeAnswers.value = [questionAnswerInputs.value.length - 1]
}

// 移除回答输入
const removeAnswerInput = (index: number) => {
  questionAnswerInputs.value.splice(index, 1)

  // 如果移除后没有回答了，重置activeAnswers
  if (questionAnswerInputs.value.length === 0) {
    activeAnswers.value = []
  }
}

// 处理文件变更（批量导入）
const handleFileChange = (file: UploadFile) => {
  const rawFile = file.raw
  if (!rawFile) {
    ElMessage.error('文件获取失败')
    return
  }

  const fileExt = rawFile.name.split('.').pop()?.toLowerCase()

  if (fileExt === 'json') {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const result = e.target?.result as string
        const data = JSON.parse(result)

        if (!Array.isArray(data)) {
          ElMessage.error('JSON文件格式错误，应为数组格式')
          return
        }

        // 验证数据格式并处理StackExchange格式
        const validData = data.filter((item: any) => {
          // 支持标准格式
          if (item.title && item.content) {
            return true
          }

          // 支持StackExchange格式
          if (item.question_title && (item.question_body || item.answer_body)) {
            return true
          }

          return false
        })

        if (validData.length === 0) {
          ElMessage.error('没有找到有效的问题数据')
          return
        }

        // 识别数据是否为StackExchange格式
        const isStackExchangeFormat = validData.some((item: any) =>
          item.question_title && (item.question_body || item.answer_body)
        )

        // 保存原始数据格式信息，用于提交时的处理
        batchDataFormat.value = isStackExchangeFormat ? 'stackexchange' : 'standard'

        // 格式化数据
        if (isStackExchangeFormat) {
          // 处理StackExchange格式，按问题分组
          const questionMap = new Map()

          validData.forEach((item: any) => {
            const questionId = item.source_question_id

            // 清理HTML内容
            const cleanHtml = (html: string) => {
              if (!html) return ''
              return html.replace(/<\/?[^>]+(>|$)/g, ' ').trim()
            }

            if (!questionMap.has(questionId)) {
              // 创建新的问题记录
              questionMap.set(questionId, {
                question: {
                  title: item.question_title || '',
                  content: item.question_body ? cleanHtml(item.question_body) : '',
                  sourceSite: item.source || 'StackExchange',
                  sourceUrl: item.source_url || '',
                  tags: Array.isArray(item.tags) ? item.tags : [],
                  otherMetadata: JSON.stringify({
                    source_question_id: item.source_question_id,
                    source_id: item.source_id
                  })
                },
                answers: []
              })
            }

            // 如果有答案内容，添加到该问题的答案列表
            if (item.answer_body) {
              questionMap.get(questionId).answers.push({
                authorInfo: item.author_info || '未知作者',
                content: cleanHtml(item.answer_body),
                publishTime: item.publish_time || new Date().toISOString(),
                upvotes: item.upvotes || 0,
                isAccepted: item.is_accepted || false,
                otherMetadata: JSON.stringify({
                  source_answer_id: item.source_answer_id
                })
              })
            }
          })

          // 转换为数组形式
          batchData.value = Array.from(questionMap.values())
        } else {
          // 处理标准格式
          batchData.value = validData.map((item: any) => ({
            title: item.title || '',
            content: item.content || '',
            sourceSite: item.sourceSite || '',
            sourceUrl: item.sourceUrl || '',
            tags: Array.isArray(item.tags) ? item.tags : [],
            otherMetadata: item.otherMetadata || ''
          }))
        }

        batchDialogVisible.value = true
      } catch (error) {
        console.error('解析JSON文件失败:', error)
        ElMessage.error('解析JSON文件失败，请检查文件格式')
      }
    }
    reader.readAsText(rawFile)
  } else if (fileExt === 'csv') {
    Papa.parse(rawFile, {
      header: true,
      complete: (results) => {
        handleCsvData(results)
        batchDataFormat.value = 'standard'
      }
    })
  } else {
    ElMessage.error('不支持的文件格式，请上传JSON或CSV文件')
  }
}

// 处理CSV文件中的数据
const handleCsvData = (results: { data: any[], errors: any[] }) => {
  if (results.errors.length > 0) {
    console.error('解析CSV文件错误:', results.errors)
    ElMessage.error('解析CSV文件失败，请检查文件格式')
    return
  }

  // 验证数据格式，使用unknown类型处理未知结构
  const resultData = results.data as unknown[];
  const validData = resultData.filter((item) => {
    const typedItem = item as Record<string, any>;
    return typedItem.title && typedItem.content;
  });

  if (validData.length === 0) {
    ElMessage.error('没有找到有效的问题数据')
    return
  }

  // 格式化数据，使用unknown类型处理未知结构
  batchData.value = validData.map((item) => {
    const typedItem = item as Record<string, any>;
    // 处理tags字段，可能是逗号分隔的字符串
    let tags: string[] = []
    if (typedItem.tags) {
      if (typeof typedItem.tags === 'string') {
        tags = typedItem.tags.split(',').map((tag: string) => tag.trim()).filter(Boolean)
      } else if (Array.isArray(typedItem.tags)) {
        tags = typedItem.tags
      }
    }

    return {
      title: typedItem.title || '',
      content: typedItem.content || '',
      sourceSite: typedItem.sourceSite || '',
      sourceUrl: typedItem.sourceUrl || '',
      tags,
      otherMetadata: typedItem.otherMetadata || ''
    }
  })

  batchDialogVisible.value = true
}

// 提交批量导入
const submitBatchImport = async () => {
  if (batchData.value.length === 0) {
    ElMessage.warning('没有可导入的数据')
    return
  }

  if (batchData.value.length > 500) {
    ElMessage.warning('导入数据过多，最多支持500条记录')
    return
  }

  isUploading.value = true

  try {
    if (batchDataFormat.value === 'stackexchange') {
      // 使用问题与答案批量导入接口
      const promises = batchData.value.map(item =>
        createQuestionWithAnswers({
          question: item.question,
          answers: item.answers
        })
      )
      await Promise.all(promises)
    } else {
      // 使用标准问题导入接口
      const promises = batchData.value.map(item => createRawQuestion(item))
      await Promise.all(promises)
    }

    ElMessage.success(`成功导入${batchData.value.length}条问题数据`)
    batchDialogVisible.value = false
    batchData.value = []
    fetchQuestions()
  } catch (error) {
    console.error('批量导入失败:', error)
    ElMessage.error('批量导入失败，请重试')
  } finally {
    isUploading.value = false
  }
}

// 处理标签回车输入
const handleTagEnter = (event: KeyboardEvent) => {
  // 获取el-select的输入框
  const input = event.target as HTMLInputElement
  const value = input.value.trim()

  if (value && !questionForm.tags.includes(value)) {
    // 添加新标签
    questionForm.tags = [...questionForm.tags, value]

    // 尝试清空输入框，但这可能不会直接生效
    // 因为Element Plus的Select组件管理自己的输入状态
    setTimeout(() => {
      input.value = ''
    }, 0)
  }
}

// 处理搜索标签回车输入
const handleSearchTagEnter = (event: KeyboardEvent) => {
  // 获取el-select的输入框
  const input = event.target as HTMLInputElement
  const value = input.value.trim()

  if (value && !searchForm.tags.includes(value)) {
    // 添加新标签
    searchForm.tags = [...searchForm.tags, value]

    // 尝试清空输入框
    setTimeout(() => {
      input.value = ''
    }, 0)
  }
}
</script>

<style scoped>
.question-management {
  padding: 20px;
}

.management-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.management-card:hover {
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.08);
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

.search-area {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.tag-item {
  margin-right: 5px;
  margin-bottom: 5px;
  transition: all 0.3s;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tag-input-container {
  display: flex;
  align-items: center;
}

.answers-input-section {
  margin-top: 20px;
}

.answers-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.answer-input-container {
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.no-answers-input {
  padding: 20px;
  text-align: center;
}

.question-detail {
  padding: 10px;
}

.detail-item {
  display: flex;
  margin-bottom: 10px;
}

.detail-label {
  font-weight: bold;
  min-width: 100px;
}

.detail-value {
  flex: 1;
}

.content-item {
  flex-direction: column;
}

.content-value {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
}

.answers-section {
  margin-top: 20px;
}

.no-answers {
  padding: 20px;
  text-align: center;
}

.batch-preview {
  margin-top: 20px;
}

.batch-summary {
  margin-top: 10px;
  text-align: right;
  font-weight: bold;
}

/* 标签样式优化 */
:deep(.el-select__tags) {
  flex-wrap: wrap;
  max-height: none;
}

:deep(.el-tag) {
  margin: 2px;
  border-radius: 4px;
  background-color: #ecf5ff;
  border-color: #d9ecff;
  color: #409eff;
}

:deep(.el-tag--info) {
  background-color: #f4f4f5;
  border-color: #e9e9eb;
  color: #909399;
}

:deep(.el-tag--success) {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
  color: #67c23a;
}

:deep(.el-select-dropdown__item) {
  padding: 0 10px;
  height: 34px;
  line-height: 34px;
}

/* 标签选择下拉菜单样式 */
:deep(.tag-select-dropdown) {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

:deep(.tag-select-dropdown .el-select-dropdown__item) {
  padding: 8px 15px;
  border-radius: 4px;
  margin: 4px;
  transition: all 0.2s;
}

:deep(.tag-select-dropdown .el-select-dropdown__item:hover) {
  background-color: #ecf5ff;
}

:deep(.tag-select-dropdown .el-select-dropdown__item.selected) {
  background-color: #409eff;
  color: white;
}

/* 卡片和表格样式优化 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafafa;
}

:deep(.el-table__row:hover > td) {
  background-color: #ecf5ff !important;
}

/* 按钮样式优化 */
:deep(.el-button) {
  border-radius: 6px;
  transition: all 0.3s;
}

:deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #409eff, #3a8ee6);
}

:deep(.el-pagination) {
  padding: 15px 0;
  justify-content: center;
}
</style>
