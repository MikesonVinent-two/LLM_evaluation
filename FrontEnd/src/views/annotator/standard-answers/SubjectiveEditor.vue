<template>
  <div class="subjective-editor">
    <h4>主观题标准答案编辑</h4>

    <div class="answer-section">
      <h5>标准参考答案</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        请提供主观题的标准参考答案，尽量全面详细
      </el-alert>

      <el-input
        v-model="localAnswerText"
        type="textarea"
        :rows="8"
        placeholder="请输入标准参考答案"
        @input="updateAnswerText"
      />
    </div>

    <div class="scoring-section">
      <h5>评分指导</h5>
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        请提供评分指导，包括评分标准、评分点和评分权重等
      </el-alert>

      <el-input
        v-model="localScoringGuidance"
        type="textarea"
        :rows="8"
        placeholder="请输入评分指导，例如：
1. 正确提及XX知识点（30分）
2. 分析思路清晰，有理有据（30分）
3. 结论正确（20分）
4. 表述流畅，语言规范（20分）"
        @input="updateScoringGuidance"
      />
    </div>

    <div class="example-section">
      <h5>示例回答（可选）</h5>

      <el-collapse>
        <el-collapse-item title="优秀回答示例" name="excellent">
          <el-input
            v-model="excellentExample"
            type="textarea"
            :rows="5"
            placeholder="请输入优秀回答示例"
            @input="updateExamples"
          />
        </el-collapse-item>

        <el-collapse-item title="良好回答示例" name="good">
          <el-input
            v-model="goodExample"
            type="textarea"
            :rows="5"
            placeholder="请输入良好回答示例"
            @input="updateExamples"
          />
        </el-collapse-item>

        <el-collapse-item title="一般回答示例" name="average">
          <el-input
            v-model="averageExample"
            type="textarea"
            :rows="5"
            placeholder="请输入一般回答示例"
            @input="updateExamples"
          />
        </el-collapse-item>

        <el-collapse-item title="不合格回答示例" name="poor">
          <el-input
            v-model="poorExample"
            type="textarea"
            :rows="5"
            placeholder="请输入不合格回答示例"
            @input="updateExamples"
          />
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  answerText: string
  scoringGuidance: string
}>()

const emit = defineEmits<{
  (e: 'update:answerText', answerText: string): void
  (e: 'update:scoringGuidance', scoringGuidance: string): void
}>()

// 本地状态
const localAnswerText = ref(props.answerText)
const localScoringGuidance = ref(props.scoringGuidance)

// 示例回答（这些会被添加到scoringGuidance中）
const excellentExample = ref('')
const goodExample = ref('')
const averageExample = ref('')
const poorExample = ref('')

// 监听属性变化
watch(() => props.answerText, (newAnswerText) => {
  localAnswerText.value = newAnswerText
})

watch(() => props.scoringGuidance, (newGuidance) => {
  // 尝试从评分指导中分离出示例部分
  const regex = /示例回答：\n\n优秀回答：([\s\S]*?)\n\n良好回答：([\s\S]*?)\n\n一般回答：([\s\S]*?)\n\n不合格回答：([\s\S]*)/
  const match = newGuidance.match(regex)

  if (match) {
    localScoringGuidance.value = newGuidance.split('示例回答：')[0].trim()
    excellentExample.value = match[1].trim()
    goodExample.value = match[2].trim()
    averageExample.value = match[3].trim()
    poorExample.value = match[4].trim()
  } else {
    localScoringGuidance.value = newGuidance
  }
})

// 方法
const updateAnswerText = () => {
  emit('update:answerText', localAnswerText.value)
}

const updateScoringGuidance = () => {
  const fullGuidance = buildFullGuidance()
  emit('update:scoringGuidance', fullGuidance)
}

const updateExamples = () => {
  updateScoringGuidance()
}

const buildFullGuidance = () => {
  let fullGuidance = localScoringGuidance.value

  // 如果有任何示例，添加示例部分
  if (excellentExample.value || goodExample.value || averageExample.value || poorExample.value) {
    fullGuidance += '\n\n示例回答：\n\n优秀回答：' + (excellentExample.value || '无') +
      '\n\n良好回答：' + (goodExample.value || '无') +
      '\n\n一般回答：' + (averageExample.value || '无') +
      '\n\n不合格回答：' + (poorExample.value || '无')
  }

  return fullGuidance
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
.subjective-editor {
  margin-bottom: 20px;
}

.answer-section,
.scoring-section,
.example-section {
  margin-bottom: 30px;
}
</style>
