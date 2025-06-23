<template>
  <div class="single-choice-editor">
    <h4>单选题标准答案编辑</h4>

    <div class="options-section">
      <h5>选项设置</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        添加选项并选择一个正确答案
      </el-alert>

      <div v-for="(option, index) in options" :key="index" class="option-item">
        <div class="option-header">
          <h5>选项 {{ String.fromCharCode(65 + index) }}</h5>
          <el-radio
            v-model="localCorrectId"
            :label="option.id"
            @change="updateCorrectIds"
          >
            正确答案
          </el-radio>
        </div>

        <el-input
          v-model="option.text"
          type="textarea"
          :rows="2"
          placeholder="请输入选项内容"
          @input="emitUpdate"
        />

        <div class="option-actions">
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="small"
            @click="removeOption(index)"
            :disabled="options.length <= 2"
          >
            删除
          </el-button>
        </div>
      </div>

      <el-button
        type="primary"
        icon="el-icon-plus"
        @click="addOption"
        class="add-option-btn"
      >
        添加选项
      </el-button>
    </div>

    <div class="answer-explanation">
      <h5>答案解析</h5>
      <el-input
        v-model="localAnswerText"
        type="textarea"
        :rows="5"
        placeholder="请输入答案解析，说明为什么选项是正确的，以及其他选项为什么不正确"
        @input="updateAnswerText"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  options: { id: number | string, text: string }[]
  correctIds: (number | string)[]
  answerText: string
}>()

const emit = defineEmits<{
  (e: 'update:options', options: { id: number | string, text: string }[]): void
  (e: 'update:correctIds', correctIds: (number | string)[]): void
  (e: 'update:answerText', answerText: string): void
}>()

// 本地状态
const localCorrectId = ref<number | string>(
  props.correctIds.length > 0 ? props.correctIds[0] : props.options.length > 0 ? props.options[0].id : 0
)
const localAnswerText = ref(props.answerText)

// 监听属性变化
watch(() => props.options, (newOptions) => {
  // 如果选项发生变化，检查当前选中的正确答案是否还存在
  const correctIdExists = newOptions.some(option => option.id === localCorrectId.value)
  if (!correctIdExists && newOptions.length > 0) {
    localCorrectId.value = newOptions[0].id
    updateCorrectIds()
  }
}, { deep: true })

watch(() => props.correctIds, (newCorrectIds) => {
  if (newCorrectIds.length > 0 && newCorrectIds[0] !== localCorrectId.value) {
    localCorrectId.value = newCorrectIds[0]
  }
}, { deep: true })

watch(() => props.answerText, (newAnswerText) => {
  localAnswerText.value = newAnswerText
})

// 方法
const addOption = () => {
  // 找到最大的ID并加1
  const maxId = props.options.reduce((max, option) =>
    typeof option.id === 'number' && option.id > max ? option.id : max, 0)

  const newOption = {
    id: maxId + 1,
    text: ''
  }

  emit('update:options', [...props.options, newOption])
}

const removeOption = (index: number) => {
  const newOptions = [...props.options]
  const removedOption = newOptions.splice(index, 1)[0]

  // 如果删除的是正确答案，则选择第一个选项作为正确答案
  if (removedOption.id === localCorrectId.value && newOptions.length > 0) {
    localCorrectId.value = newOptions[0].id
    updateCorrectIds()
  }

  emit('update:options', newOptions)
}

const updateCorrectIds = () => {
  emit('update:correctIds', [localCorrectId.value])
}

const updateAnswerText = () => {
  emit('update:answerText', localAnswerText.value)
}

const emitUpdate = () => {
  emit('update:options', [...props.options])
}

// 向父组件提供方法
defineExpose({
  setAnswerText: (text: string) => {
    localAnswerText.value = text
    updateAnswerText()
  }
})
</script>

<style scoped>
.single-choice-editor {
  margin-bottom: 20px;
}

.options-section {
  margin-bottom: 20px;
}

.option-item {
  margin-bottom: 15px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.option-actions {
  margin-top: 10px;
  text-align: right;
}

.add-option-btn {
  margin-top: 10px;
}

.answer-explanation {
  margin-top: 30px;
}
</style>
