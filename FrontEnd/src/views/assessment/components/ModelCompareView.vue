<template>
  <div class="model-compare-view">
    <!-- 对比概览 -->
    <div class="compare-overview">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>模型对比概览</span>
            <el-tag type="info" size="small">{{ models.length }}个模型</el-tag>
          </div>
        </template>

        <div class="models-grid">
          <div
            v-for="(model, index) in models"
            :key="model.modelInfo.id"
            class="model-card"
            :class="getModelCardClass(index)"
          >
            <div class="model-header">
              <div class="model-name">{{ model.modelInfo.name }}</div>
              <div class="model-rank">排名 #{{ model.rank }}</div>
            </div>
            <div class="model-provider">{{ model.modelInfo.provider }}</div>
            <div class="model-score">
              <div class="score-value">{{ safeNumber(model.overallScore).toFixed(1) }}</div>
              <div class="score-label">综合评分</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 对比图表 -->
    <div class="compare-charts">
      <el-row :gutter="24">
        <!-- 雷达图对比 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><PieChart /></el-icon>
                <span>多维度对比</span>
              </div>
            </template>
            <div class="chart-container">
              <div ref="radarChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>

        <!-- 柱状图对比 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Histogram /></el-icon>
                <span>分类评分对比</span>
              </div>
            </template>
            <div class="chart-container">
              <div ref="barChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 详细对比表格 -->
    <div class="compare-table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><List /></el-icon>
            <span>详细对比数据</span>
          </div>
        </template>

        <el-table :data="comparisonData" class="compare-table" border>
          <el-table-column prop="metric" label="评测指标" width="150" fixed="left">
            <template #default="{ row }">
              <div class="metric-cell">
                <span>{{ row.metric }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            v-for="(model, index) in models"
            :key="model.modelInfo.id"
            :label="model.modelInfo.name"
            :width="120"
            align="center"
          >
            <template #header>
              <div class="model-header-cell">
                <div class="model-name">{{ model.modelInfo.name }}</div>
                <el-tag :type="getModelTagType(index)" size="small">
                  {{ model.modelInfo.provider }}
                </el-tag>
              </div>
            </template>

            <template #default="{ row }">
              <div class="score-cell">
                <div
                  class="score-value"
                  :class="getScoreClass(row.values[index])"
                >
                  {{ formatValue(row.values[index], row.type) }}
                </div>
                <div v-if="row.type === 'score'" class="score-bar">
                  <el-progress
                    :percentage="Math.round(safeNumber(row.values[index]))"
                    :color="getScoreColor(row.values[index])"
                    :show-text="false"
                    :stroke-width="4"
                  />
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 操作按钮 -->
    <div class="action-section">
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" @click="exportCompareReport">导出对比报告</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  DataAnalysis,
  PieChart,
  Histogram,
  List
} from '@element-plus/icons-vue'
import type { ModelScore } from '@/api/evaluations'

// Props
interface Props {
  batchId: number | null
  models: ModelScore[]
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  close: []
}>()

// 响应式数据
const radarChart = ref<HTMLElement>()
const barChart = ref<HTMLElement>()
let radarChartInstance: echarts.ECharts | null = null
let barChartInstance: echarts.ECharts | null = null

// 工具函数
const safeNumber = (value: any): number => {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return 0
  }
  return Number(value)
}

const getScoreColor = (score: number): string => {
  const safeScore = safeNumber(score)
  if (safeScore >= 90) return '#67C23A'
  if (safeScore >= 80) return '#E6A23C'
  if (safeScore >= 70) return '#F56C6C'
  return '#909399'
}

const getScoreClass = (score: number): string => {
  const safeScore = safeNumber(score)
  if (safeScore >= 90) return 'score-excellent'
  if (safeScore >= 80) return 'score-good'
  if (safeScore >= 70) return 'score-average'
  return 'score-poor'
}

const getModelCardClass = (index: number): string => {
  const classes = ['model-primary', 'model-success', 'model-warning', 'model-danger', 'model-info']
  return classes[index % classes.length]
}

const getModelTagType = (index: number): string => {
  const types = ['primary', 'success', 'warning', 'danger', 'info']
  return types[index % types.length]
}

const formatValue = (value: any, type: string): string => {
  if (type === 'score') {
    return safeNumber(value).toFixed(1)
  } else if (type === 'count') {
    return safeNumber(value).toString()
  }
  return value?.toString() || 'N/A'
}

// 计算属性
const comparisonData = computed(() => {
  return [
    {
      metric: '综合评分',
      type: 'score',
      values: props.models.map(m => m.overallScore)
    },
    {
      metric: '客观题平均分',
      type: 'score',
      values: props.models.map(m => m.objectiveScores?.average_score)
    },
    {
      metric: '客观题数量',
      type: 'count',
      values: props.models.map(m => m.objectiveScores?.total_answers)
    },
    {
      metric: '主观题AI平均分',
      type: 'score',
      values: props.models.map(m => m.subjectiveAiScores?.average_score)
    },
    {
      metric: '主观题AI数量',
      type: 'count',
      values: props.models.map(m => m.subjectiveAiScores?.total_answers)
    },
    {
      metric: '主观题人工平均分',
      type: 'score',
      values: props.models.map(m => m.subjectiveHumanScores?.average_score)
    },
    {
      metric: '主观题人工数量',
      type: 'count',
      values: props.models.map(m => m.subjectiveHumanScores?.total_answers)
    },
    {
      metric: '单选题平均分',
      type: 'score',
      values: props.models.map(m => m.objectiveScores?.single_choice_avg)
    },
    {
      metric: '多选题平均分',
      type: 'score',
      values: props.models.map(m => m.objectiveScores?.multiple_choice_avg)
    },
    {
      metric: '简单事实题平均分',
      type: 'score',
      values: props.models.map(m => m.objectiveScores?.simple_fact_avg)
    },
    {
      metric: '总评测数',
      type: 'count',
      values: props.models.map(m => m.detailStats?.total_evaluations)
    },
    {
      metric: '成功评测数',
      type: 'count',
      values: props.models.map(m => m.detailStats?.success_count)
    }
  ]
})

// 渲染雷达图
const renderRadarChart = () => {
  if (!radarChart.value) return

  if (radarChartInstance) {
    radarChartInstance.dispose()
  }

  radarChartInstance = echarts.init(radarChart.value)

  const indicators = [
    { name: '综合评分', max: 100 },
    { name: '客观题', max: 100 },
    { name: '主观题(AI)', max: 100 },
    { name: '主观题(人工)', max: 100 },
    { name: '单选题', max: 100 },
    { name: '多选题', max: 100 },
    { name: '简单事实题', max: 100 }
  ]

  const series = props.models.map((model, index) => ({
    name: model.modelInfo.name,
    type: 'radar',
    data: [{
      value: [
        safeNumber(model.overallScore),
        safeNumber(model.objectiveScores?.average_score),
        safeNumber(model.subjectiveAiScores?.average_score),
        safeNumber(model.subjectiveHumanScores?.average_score),
        safeNumber(model.objectiveScores?.single_choice_avg),
        safeNumber(model.objectiveScores?.multiple_choice_avg),
        safeNumber(model.objectiveScores?.simple_fact_avg)
      ],
      name: model.modelInfo.name
    }],
    itemStyle: {
      color: getModelColor(index)
    },
    lineStyle: {
      color: getModelColor(index)
    },
    areaStyle: {
      color: getModelColor(index),
      opacity: 0.1
    }
  }))

  const option = {
    title: {
      text: '模型多维度对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: props.models.map(m => m.modelInfo.name)
    },
    radar: {
      indicator: indicators,
      center: ['60%', '50%'],
      radius: '70%'
    },
    series: series
  }

  radarChartInstance.setOption(option)
}

// 渲染柱状图
const renderBarChart = () => {
  if (!barChart.value) return

  if (barChartInstance) {
    barChartInstance.dispose()
  }

  barChartInstance = echarts.init(barChart.value)

  const categories = ['综合评分', '客观题', '主观题(AI)', '主观题(人工)']
  const series = props.models.map((model, index) => ({
    name: model.modelInfo.name,
    type: 'bar',
    data: [
      safeNumber(model.overallScore),
      safeNumber(model.objectiveScores?.average_score),
      safeNumber(model.subjectiveAiScores?.average_score),
      safeNumber(model.subjectiveHumanScores?.average_score)
    ],
    itemStyle: {
      color: getModelColor(index)
    }
  }))

  const option = {
    title: {
      text: '分类评分对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: props.models.map(m => m.modelInfo.name),
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: categories
    },
    yAxis: {
      type: 'value',
      name: '分数',
      max: 100
    },
    series: series
  }

  barChartInstance.setOption(option)
}

const getModelColor = (index: number): string => {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452']
  return colors[index % colors.length]
}

const exportCompareReport = () => {
  ElMessage.info('导出功能开发中...')
}

// 生命周期
onMounted(async () => {
  await nextTick()
  renderRadarChart()
  renderBarChart()
})
</script>

<style scoped lang="scss">
.model-compare-view {
  padding: 16px;
}

.compare-overview {
  margin-bottom: 24px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    > div:first-child {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      color: #303133;
    }
  }

  .models-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;

    .model-card {
      padding: 20px;
      border-radius: 12px;
      text-align: center;
      transition: transform 0.3s ease;

      &:hover {
        transform: translateY(-4px);
      }

      .model-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .model-name {
          font-weight: bold;
          color: white;
          font-size: 16px;
        }

        .model-rank {
          background: rgba(255, 255, 255, 0.2);
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 12px;
          color: white;
        }
      }

      .model-provider {
        color: rgba(255, 255, 255, 0.8);
        font-size: 14px;
        margin-bottom: 12px;
      }

      .model-score {
        .score-value {
          font-size: 32px;
          font-weight: bold;
          color: white;
          line-height: 1;
          margin-bottom: 4px;
        }

        .score-label {
          color: rgba(255, 255, 255, 0.8);
          font-size: 12px;
        }
      }

      &.model-primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.model-success {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
      }

      &.model-warning {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }

      &.model-danger {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }

      &.model-info {
        background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);

        .model-name,
        .score-value {
          color: #333;
        }

        .model-provider,
        .score-label {
          color: rgba(51, 51, 51, 0.7);
        }

        .model-rank {
          background: rgba(51, 51, 51, 0.1);
          color: #333;
        }
      }
    }
  }
}

.compare-charts {
  margin-bottom: 24px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    color: #303133;
  }

  .chart-container {
    height: 400px;

    .chart {
      width: 100%;
      height: 100%;
    }
  }
}

.compare-table-section {
  margin-bottom: 24px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    color: #303133;
  }

  .compare-table {
    .metric-cell {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }

    .model-header-cell {
      text-align: center;

      .model-name {
        font-weight: 500;
        margin-bottom: 4px;
      }
    }

    .score-cell {
      .score-value {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 4px;

        &.score-excellent {
          color: #67c23a;
        }

        &.score-good {
          color: #e6a23c;
        }

        &.score-average {
          color: #f56c6c;
        }

        &.score-poor {
          color: #909399;
        }
      }

      .score-bar {
        margin-top: 4px;
      }
    }
  }
}

.action-section {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

// 响应式设计
@media (max-width: 768px) {
  .models-grid {
    grid-template-columns: 1fr;
  }

  .compare-charts .el-col {
    margin-bottom: 24px;
  }
}
</style>

