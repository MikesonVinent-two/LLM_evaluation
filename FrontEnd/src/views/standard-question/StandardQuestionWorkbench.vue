<template>
  <div class="standard-question-workbench">
    <el-container>
      <el-aside width="300px">
        <!-- 原始问题列表 -->
        <div class="raw-questions">
          <h3>原始问题列表</h3>
          <el-input
            v-model="searchQuery"
            placeholder="搜索原始问题"
            prefix-icon="el-icon-search"
          />
          <el-scrollbar height="calc(100vh - 200px)">
            <el-list v-loading="loading">
              <el-list-item
                v-for="question in rawQuestions"
                :key="question.id"
                @click="selectRawQuestion(question)"
              >
                {{ question.content }}
              </el-list-item>
            </el-list>
          </el-scrollbar>
        </div>
      </el-aside>

      <el-main>
        <!-- 标准问题编辑区 -->
        <div class="standard-question-editor">
          <div class="editor-header">
            <h3>标准问题编辑</h3>
            <div class="header-actions">
              <el-button @click="showHistory">
                历史版本
              </el-button>
              <el-button type="primary" @click="saveStandardQuestions">
                保存标准问题
              </el-button>
            </div>
          </div>

          <div class="selected-raw-question" v-if="selectedRawQuestion">
            <h4>原始问题：</h4>
            <p>{{ selectedRawQuestion.content }}</p>
            <div class="raw-tags">
              <el-tag
                v-for="tag in selectedRawQuestion.tags"
                :key="tag"
                class="mx-1"
                closable
              >
                {{ tag }}
              </el-tag>
              <el-button class="button-new-tag" size="small" @click="showTagSelector">
                + 添加标签
              </el-button>
              <el-button class="button-new-tag" size="small" @click="applyTagsToAll">
                应用标签到所有标准问题
              </el-button>
            </div>
          </div>

          <!-- 标准问题列表 -->
          <div class="standard-questions-list">
            <div class="batch-actions" v-if="standardQuestions.length">
              <el-button-group>
                <el-button size="small" @click="batchSetType">
                  批量设置题型
                </el-button>
                <el-button size="small" @click="batchSetDifficulty">
                  批量设置难度
                </el-button>
                <el-button size="small" @click="batchAddTags">
                  批量添加标签
                </el-button>
              </el-button-group>
            </div>

            <div
              v-for="(question, index) in standardQuestions"
              :key="index"
              class="standard-question-item"
            >
              <div class="question-header">
                <span class="question-number">问题 {{ index + 1 }}</span>
                <el-button-group>
                  <el-button
                    size="small"
                    icon="el-icon-top"
                    @click="moveQuestion(index, 'up')"
                    :disabled="index === 0"
                  />
                  <el-button
                    size="small"
                    icon="el-icon-bottom"
                    @click="moveQuestion(index, 'down')"
                    :disabled="index === standardQuestions.length - 1"
                  />
                  <el-button
                    size="small"
                    icon="el-icon-delete"
                    type="danger"
                    @click="removeQuestion(index)"
                  />
                </el-button-group>
              </div>

              <el-input
                v-model="question.content"
                type="textarea"
                :rows="3"
                placeholder="输入标准问题"
              />

              <div class="question-properties">
                <el-select v-model="question.type" placeholder="选择题型">
                  <el-option
                    v-for="type in questionTypes"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                  />
                </el-select>

                <el-select v-model="question.difficulty" placeholder="选择难度">
                  <el-option
                    v-for="level in difficultyLevels"
                    :key="level.value"
                    :label="level.label"
                    :value="level.value"
                  />
                </el-select>
              </div>

              <div class="question-tags">
                <el-tag
                  v-for="tag in question.tags"
                  :key="tag"
                  closable
                  @close="removeTag(index, tag)"
                >
                  {{ tag }}
                </el-tag>
                <el-button class="button-new-tag" size="small" @click="showTagSelector(index)">
                  + 添加标签
                </el-button>
                <el-button
                  class="button-new-tag"
                  size="small"
                  @click="applyOriginalTags(index)"
                  v-if="selectedRawQuestion?.tags?.length"
                >
                  引用原始标签
                </el-button>
              </div>

              <div class="recommended-tags" v-if="question.recommendedTags?.length">
                <h5>推荐标签：</h5>
                <el-tag
                  v-for="tag in question.recommendedTags"
                  :key="tag"
                  class="mx-1"
                  @click="addRecommendedTag(index, tag)"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>

            <el-button type="dashed" @click="addNewStandardQuestion">
              + 添加新标准问题
            </el-button>
          </div>
        </div>
      </el-main>

      <!-- 标签选择器抽屉 -->
      <el-drawer
        v-model="showTagSelectorDrawer"
        title="选择标签"
        direction="rtl"
        size="30%"
      >
        <div class="tag-selector">
          <el-input
            v-model="tagSearchQuery"
            placeholder="搜索标签"
            prefix-icon="el-icon-search"
          />
          <div class="tag-categories">
            <div v-for="category in tagCategories" :key="category.name">
              <h4>{{ category.name }}</h4>
              <div class="tag-list">
                <el-tag
                  v-for="tag in filterTags(category.tags)"
                  :key="tag"
                  @click="selectTag(tag)"
                  :class="{ 'selected': isTagSelected(tag) }"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-drawer>

      <!-- 历史版本抽屉 -->
      <el-drawer
        v-model="showVersionHistory"
        title="版本历史"
        direction="rtl"
        size="50%"
      >
        <el-tree
          :data="versionHistory"
          :props="defaultProps"
          @node-click="handleVersionSelect"
        >
          <template #default="{ node, data }">
            <div class="version-node">
              <span>{{ data.label }}</span>
              <span class="version-time">{{ formatTime(data.createTime) }}</span>
              <el-button
                size="small"
                type="primary"
                @click.stop="restoreVersion(data)"
              >
                恢复此版本
              </el-button>
            </div>
          </template>
        </el-tree>
      </el-drawer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRawQuestions,
  getStandardQuestions,
  saveStandardQuestion,
  getQuestionVersionHistory,
  getRecommendedTags
} from '@/api/standardData'

const loading = ref(false)
const searchQuery = ref('')
const rawQuestions = ref([])
const selectedRawQuestion = ref(null)
const standardQuestions = ref([])
const showVersionHistory = ref(false)
const versionHistory = ref([])
const showTagSelectorDrawer = ref(false)
const tagSearchQuery = ref('')

const questionTypes = [
  { value: 'single', label: '单选题' },
  { value: 'multiple', label: '多选题' },
  { value: 'subjective', label: '主观题' },
  { value: 'programming', label: '编程题' }
]

const difficultyLevels = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' }
]

const defaultProps = {
  children: 'children',
  label: 'label'
}

onMounted(async () => {
  loading.value = true
  try {
    rawQuestions.value = await getRawQuestions()
  } catch (error) {
    ElMessage.error('获取原始问题失败')
  }
  loading.value = false
})

const selectRawQuestion = async (question) => {
  selectedRawQuestion.value = question
  try {
    const standards = await getStandardQuestions(question.id)
    standardQuestions.value = standards
    // 获取推荐标签
    for (let sq of standardQuestions.value) {
      sq.recommendedTags = await getRecommendedTags(sq.content)
    }
  } catch (error) {
    ElMessage.error('获取标准问题失败')
  }
}

const saveStandardQuestions = async () => {
  try {
    await Promise.all(
      standardQuestions.value.map(q =>
        saveStandardQuestion({
          ...q,
          rawQuestionId: selectedRawQuestion.value.id
        })
      )
    )
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const addNewStandardQuestion = () => {
  standardQuestions.value.push({
    content: '',
    type: '',
    difficulty: 1,
    tags: []
  })
}

const showTagSelector = (index) => {
  showTagSelectorDrawer.value = true
}

const removeTag = (questionIndex, tag) => {
  standardQuestions.value[questionIndex].tags =
    standardQuestions.value[questionIndex].tags.filter(t => t !== tag)
}

const addRecommendedTag = (questionIndex, tag) => {
  if (!standardQuestions.value[questionIndex].tags.includes(tag)) {
    standardQuestions.value[questionIndex].tags.push(tag)
  }
}

const handleVersionSelect = (data) => {
  // 实现版本选择逻辑
}

const applyTagsToAll = () => {
  if (!selectedRawQuestion.value?.tags?.length) return
  standardQuestions.value.forEach(question => {
    question.tags = [...new Set([...question.tags, ...selectedRawQuestion.value.tags])]
  })
}

const applyOriginalTags = (index) => {
  if (!selectedRawQuestion.value?.tags?.length) return
  standardQuestions.value[index].tags = [
    ...new Set([...standardQuestions.value[index].tags, ...selectedRawQuestion.value.tags])
  ]
}

const moveQuestion = (index, direction) => {
  const questions = [...standardQuestions.value]
  if (direction === 'up' && index > 0) {
    [questions[index], questions[index - 1]] = [questions[index - 1], questions[index]]
  } else if (direction === 'down' && index < questions.length - 1) {
    [questions[index], questions[index + 1]] = [questions[index + 1], questions[index]]
  }
  standardQuestions.value = questions
}

const removeQuestion = (index) => {
  ElMessageBox.confirm('确定要删除这个标准问题吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    standardQuestions.value.splice(index, 1)
    ElMessage.success('删除成功')
  })
}

const batchSetType = () => {
  // 实现批量设置题型的逻辑
}

const batchSetDifficulty = () => {
  // 实现批量设置难度的逻辑
}

const batchAddTags = () => {
  // 实现批量添加标签的逻辑
}

const formatTime = (time) => {
  return new Date(time).toLocaleString()
}

const restoreVersion = (version) => {
  ElMessageBox.confirm('恢复此版本将覆盖当前编辑的内容，是否继续？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 实现版本恢复逻辑
    showVersionHistory.value = false
  })
}
</script>

<style scoped>
.standard-question-workbench {
  height: 100vh;
}

.raw-questions {
  padding: 20px;
  border-right: 1px solid #dcdfe6;
}

.standard-question-editor {
  padding: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.selected-raw-question {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.standard-question-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.question-properties {
  display: flex;
  gap: 10px;
  margin: 10px 0;
}

.question-tags {
  margin-top: 10px;
}

.recommended-tags {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.batch-actions {
  margin-bottom: 20px;
}

.tag-selector {
  padding: 20px;
}

.tag-categories {
  margin-top: 20px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.version-node {
  display: flex;
  align-items: center;
  gap: 20px;
}

.version-time {
  color: #909399;
  font-size: 12px;
}
</style>
