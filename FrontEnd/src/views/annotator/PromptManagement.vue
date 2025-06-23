<template>
  <div class="prompt-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>Prompt工作台</h2>
          <el-input
            v-model="searchQuery"
            placeholder="搜索提示词..."
            prefix-icon="el-icon-search"
            clearable
            style="width: 250px"
          />
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- 回答阶段 - 题型提示词 -->
        <el-tab-pane label="回答阶段-题型提示词" name="answerTypePrompt">
          <question-type-prompt-manager :search-query="searchQuery" ref="questionTypePromptRef" />
        </el-tab-pane>

        <!-- 回答阶段 - 标签提示词 -->
        <el-tab-pane label="回答阶段-标签提示词" name="answerTagPrompt">
          <tag-prompt-manager :search-query="searchQuery" ref="tagPromptRef" />
        </el-tab-pane>

        <!-- 回答阶段 - 组装提示词 -->
        <el-tab-pane label="回答阶段-组装提示词" name="answerAssemblyPrompt">
          <prompt-assembly-config ref="promptAssemblyRef" />
        </el-tab-pane>

        <!-- 评测阶段 - 简答题提示词 -->
        <el-tab-pane label="评测阶段-简答题提示词" name="evaluationSubjectivePrompt">
          <evaluation-prompt-manager :search-query="searchQuery" ref="evaluationPromptRef" />
        </el-tab-pane>

        <!-- 评测阶段 - 标签提示词 -->
        <el-tab-pane label="评测阶段-标签提示词" name="evaluationTagPrompt">
          <evaluation-tag-prompt-manager :search-query="searchQuery" ref="evalTagPromptRef" />
        </el-tab-pane>

        <!-- 评测阶段 - 组装提示词 -->
        <el-tab-pane label="评测阶段-组装提示词" name="evaluationAssemblyPrompt">
          <evaluation-assembly-config ref="evalAssemblyRef" />
        </el-tab-pane>
      </el-tabs>

      <el-button
        type="primary"
        icon="el-icon-plus"
        class="create-button"
        @click="handleCreatePrompt"
      >
        创建{{ getActiveTabLabel() }}
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import TagPromptManager from './prompts/TagPromptManager.vue'
import QuestionTypePromptManager from './prompts/QuestionTypePromptManager.vue'
import EvaluationPromptManager from './prompts/EvaluationPromptManager.vue'
import PromptAssemblyConfig from './prompts/PromptAssemblyConfig.vue'
import EvaluationTagPromptManager from './prompts/EvaluationTagPromptManager.vue'
import EvaluationAssemblyConfig from './prompts/EvaluationAssemblyConfig.vue'

const searchQuery = ref('')
const activeTab = ref('answerTypePrompt')

// 引用组件实例
const questionTypePromptRef = ref(null)
const tagPromptRef = ref(null)
const promptAssemblyRef = ref(null)
const evaluationPromptRef = ref(null)
const evalTagPromptRef = ref(null)
const evalAssemblyRef = ref(null)

// 获取当前活动的标签页标题
const getActiveTabLabel = () => {
  switch (activeTab.value) {
    case 'answerTypePrompt':
      return '回答阶段-题型提示词'
    case 'answerTagPrompt':
      return '回答阶段-标签提示词'
    case 'answerAssemblyPrompt':
      return '回答阶段-组装提示词'
    case 'evaluationSubjectivePrompt':
      return '评测阶段-简答题提示词'
    case 'evaluationTagPrompt':
      return '评测阶段-标签提示词'
    case 'evaluationAssemblyPrompt':
      return '评测阶段-组装提示词'
    default:
      return '提示词'
  }
}

const handleTabClick = () => {
  // 可以在这里添加标签切换时的处理逻辑
}

// 处理创建提示词按钮点击
const handleCreatePrompt = () => {
  // 根据当前活动的标签页打开相应的创建对话框
  switch (activeTab.value) {
    case 'answerTypePrompt':
      if (questionTypePromptRef.value) {
        questionTypePromptRef.value.openCreateDialog()
      }
      break
    case 'answerTagPrompt':
      if (tagPromptRef.value) {
        tagPromptRef.value.openCreateDialog()
      }
      break
    case 'answerAssemblyPrompt':
      if (promptAssemblyRef.value) {
        promptAssemblyRef.value.openAnswerConfigCreateDialog()
      }
      break
    case 'evaluationSubjectivePrompt':
      if (evaluationPromptRef.value) {
        evaluationPromptRef.value.openSubjectivePromptCreateDialog()
      }
      break
    case 'evaluationTagPrompt':
      if (evalTagPromptRef.value) {
        evalTagPromptRef.value.openTagPromptCreateDialog()
      }
      break
    case 'evaluationAssemblyPrompt':
      if (evalAssemblyRef.value) {
        evalAssemblyRef.value.openEvaluationConfigCreateDialog()
      }
      break
  }
}
</script>

<style scoped>
.prompt-management {
  padding: 20px;
}

.management-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-button {
  margin-top: 20px;
}
</style>
