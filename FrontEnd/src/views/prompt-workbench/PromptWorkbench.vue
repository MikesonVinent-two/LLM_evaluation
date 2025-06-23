<template>
  <div class="prompt-workbench">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="标签回答Prompt" name="tag-answer">
        <prompt-editor
          :value="tagAnswerPrompt"
          @update:value="updateTagAnswerPrompt"
          :api="tagPromptApi"
        />
      </el-tab-pane>
      <el-tab-pane label="评测Prompt" name="evaluation">
        <prompt-editor
          :value="evaluationPrompt"
          @update:value="updateEvaluationPrompt"
          :api="evaluationPromptApi"
        />
      </el-tab-pane>
      <el-tab-pane label="组装Config" name="assembly">
        <config-editor
          :value="assemblyConfig"
          @update:value="updateAssemblyConfig"
          :api="assemblyConfigApi"
        />
      </el-tab-pane>
      <el-tab-pane label="回答题型Prompt" name="answer-type">
        <prompt-editor
          :value="answerTypePrompt"
          @update:value="updateAnswerTypePrompt"
          :api="answerTypePromptApi"
        />
      </el-tab-pane>
      <el-tab-pane label="评测主观题Prompt" name="subjective">
        <prompt-editor
          :value="subjectivePrompt"
          @update:value="updateSubjectivePrompt"
          :api="subjectivePromptApi"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PromptEditor from '@/components/prompt/PromptEditor.vue'
import ConfigEditor from '@/components/prompt/ConfigEditor.vue'
import {
  updateTagAnswerPrompt,
  updateEvaluationPrompt,
  updateAssemblyConfig,
  updateAnswerTypePrompt,
  updateSubjectivePrompt
} from '@/api/promptApis'

const activeTab = ref('tag-answer')
const tagAnswerPrompt = ref('')
const evaluationPrompt = ref('')
const assemblyConfig = ref({})
const answerTypePrompt = ref('')
const subjectivePrompt = ref('')

// API引用
const tagPromptApi = {
  save: updateTagAnswerPrompt
}
const evaluationPromptApi = {
  save: updateEvaluationPrompt
}
const assemblyConfigApi = {
  save: updateAssemblyConfig
}
const answerTypePromptApi = {
  save: updateAnswerTypePrompt
}
const subjectivePromptApi = {
  save: updateSubjectivePrompt
}
</script>

<style scoped>
.prompt-workbench {
  padding: 20px;
}
</style>
