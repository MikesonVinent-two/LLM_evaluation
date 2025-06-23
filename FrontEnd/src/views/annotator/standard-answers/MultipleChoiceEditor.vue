<template>
  <div class="multiple-choice-editor">
    <h4>多选题标准答案编辑</h4>

    <div class="options-section">
      <h5>选项设置</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        添加选项并选择所有正确答案
      </el-alert>

      <div v-for="(option, index) in options" :key="index" class="option-item">
        <div class="option-header">
          <h5>选项 {{ String.fromCharCode(65 + index) }}</h5>
          <el-checkbox
            v-model="localCorrectIds"
            :label="option.id"
            @change="updateCorrectIds"
          >
            正确答案
          </el-checkbox>
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
        placeholder="请输入答案解析，说明为什么选中的选项是正确的，以及其他选项为什么不正确"
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
const localCorrectIds = ref<(number | string)[]>(props.correctIds || [])
const localAnswerText = ref(props.answerText)

// 监听属性变化
watch(() => props.options, (newOptions) => {
  // 如果选项发生变化，过滤掉不存在的正确答案ID
  const validCorrectIds = localCorrectIds.value.filter(id =>
    newOptions.some(option => option.id === id)
  )

  if (validCorrectIds.length !== localCorrectIds.value.length) {
    localCorrectIds.value = validCorrectIds
    updateCorrectIds()
  }
}, { deep: true })

watch(() => props.correctIds, (newCorrectIds) => {
  if (JSON.stringify(newCorrectIds) !== JSON.stringify(localCorrectIds.value)) {
    localCorrectIds.value = [...newCorrectIds]
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

  // 如果删除的是正确答案之一，则从正确答案列表中移除
  if (localCorrectIds.value.includes(removedOption.id)) {
    localCorrectIds.value = localCorrectIds.value.filter(id => id !== removedOption.id)
    updateCorrectIds()
  }

  emit('update:options', newOptions)
}

const updateCorrectIds = () => {
  emit('update:correctIds', [...localCorrectIds.value])
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
.multiple-choice-editor {
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
