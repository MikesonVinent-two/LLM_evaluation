<template>
  <div class="simple-fact-editor">
    <h4>简单事实题标准答案编辑</h4>

    <div class="answer-section">
      <h5>标准答案</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        请输入简单事实题的标准答案，尽量简洁明了
      </el-alert>

      <el-input
        v-model="localAnswerText"
        type="textarea"
        :rows="3"
        placeholder="请输入标准答案"
        @input="updateAnswerText"
      />
    </div>

    <div class="alternative-section">
      <h5>同义答案</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        添加可接受的同义词或变体答案，可根据需要添加多个
      </el-alert>

      <div v-for="(alternative, index) in localAlternativeAnswers" :key="index" class="alternative-item">
        <div class="alternative-header">
          <h5>同义答案 {{ index + 1 }}</h5>
        </div>

        <div class="alternative-content">
          <el-input
            v-model="localAlternativeAnswers[index]"
            placeholder="请输入同义答案"
            @input="updateAlternativeAnswers"
          >
            <template #append>
              <el-button
                type="danger"
                icon="el-icon-delete"
                @click="removeAlternative(index)"
              />
            </template>
          </el-input>
        </div>
      </div>

      <el-button
        type="primary"
        icon="el-icon-plus"
        @click="addAlternative"
        class="add-alternative-btn"
      >
        添加同义答案
      </el-button>
    </div>

    <div class="explanation-section">
      <h5>答案解析</h5>
      <el-input
        v-model="localExplanation"
        type="textarea"
        :rows="5"
        placeholder="请输入答案解析，可以包含更详细的信息、来源或背景"
        @input="updateExplanation"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  answerText: string
  alternativeAnswers: string[]
}>()

const emit = defineEmits<{
  (e: 'update:answerText', answerText: string): void
  (e: 'update:alternativeAnswers', alternativeAnswers: string[]): void
}>()

// 本地状态
const localAnswerText = ref(props.answerText)
const localAlternativeAnswers = ref<string[]>(props.alternativeAnswers || [])
const localExplanation = ref('') // 这个不会发送到服务器，但会附加到answerText中

// 监听属性变化
watch(() => props.answerText, (newAnswerText) => {
  // 尝试从answerText中分离出解析部分
  const parts = newAnswerText.split('\n\n解析：')
  if (parts.length > 1) {
    localAnswerText.value = parts[0]
    localExplanation.value = parts[1]
  } else {
    localAnswerText.value = newAnswerText
  }
})

watch(() => props.alternativeAnswers, (newAlternativeAnswers) => {
  localAlternativeAnswers.value = [...newAlternativeAnswers]
}, { deep: true })

// 方法
const addAlternative = () => {
  localAlternativeAnswers.value.push('')
  updateAlternativeAnswers()
}

const removeAlternative = (index: number) => {
  localAlternativeAnswers.value.splice(index, 1)
  updateAlternativeAnswers()
}

const updateAnswerText = () => {
  const fullAnswerText = localExplanation.value
    ? `${localAnswerText.value}\n\n解析：${localExplanation.value}`
    : localAnswerText.value

  emit('update:answerText', fullAnswerText)
}

const updateExplanation = () => {
  updateAnswerText()
}

const updateAlternativeAnswers = () => {
  // 过滤掉空的同义答案
  const filteredAlternatives = localAlternativeAnswers.value.filter(alt => alt.trim() !== '')
  emit('update:alternativeAnswers', filteredAlternatives)
}

// 向父组件提供方法
defineExpose({
  setAnswerText: (text: string) => {
    // 尝试从文本中分离出解析部分
    const parts = text.split('\n\n解析：')
    if (parts.length > 1) {
      localAnswerText.value = parts[0]
      localExplanation.value = parts[1]
    } else {
      localAnswerText.value = text
    }

    updateAnswerText()
  }
})
</script>

<style scoped>
.simple-fact-editor {
  margin-bottom: 20px;
}

.answer-section,
.alternative-section,
.explanation-section {
  margin-bottom: 30px;
}

.alternative-item {
  margin-bottom: 15px;
}

.alternative-header {
  margin-bottom: 10px;
}

.alternative-content {
  display: flex;
  align-items: center;
}

.add-alternative-btn {
  margin-top: 10px;
}
</style>
