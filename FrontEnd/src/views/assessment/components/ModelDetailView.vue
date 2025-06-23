<template>
  <div class="model-detail-view">
    <!-- 模型基本信息 -->
    <div class="model-info-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>模型基本信息</span>
          </div>
        </template>
        <div class="model-basic-info">
          <div class="info-item">
            <label>模型名称：</label>
            <span class="value">{{ modelInfo.modelInfo.name }}</span>
          </div>
          <div class="info-item">
            <label>提供商：</label>
            <el-tag type="info">{{ modelInfo.modelInfo.provider }}</el-tag>
          </div>
          <div class="info-item">
            <label>版本：</label>
            <span class="value">{{ modelInfo.modelInfo.version }}</span>
          </div>
          <div class="info-item">
            <label>排名：</label>
            <div class="rank-badge" :class="getRankClass(modelInfo.rank)">
              #{{ modelInfo.rank }}
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 评分概览 -->
    <div class="score-overview-section">
      <el-row :gutter="24">
        <el-col :span="6">
          <el-card class="score-card overall" shadow="hover">
            <div class="score-content">
              <div class="score-icon">
                <el-icon><Trophy /></el-icon>
              </div>
              <div class="score-info">
                <div class="score-value">{{ safeNumber(modelInfo.overallScore).toFixed(1) }}</div>
                <div class="score-label">综合评分</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="score-card objective" shadow="hover">
            <div class="score-content">
              <div class="score-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="score-info">
                <div class="score-value">{{ safeNumber(modelInfo.objectiveScores?.average_score).toFixed(1) }}</div>
                <div class="score-label">客观题</div>
                <div class="score-detail">{{ modelInfo.objectiveScores?.total_answers || 0 }}题</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="score-card subjective-ai" shadow="hover">
            <div class="score-content">
              <div class="score-icon">
                <el-icon><Setting /></el-icon>
              </div>
              <div class="score-info">
                <div class="score-value">{{ safeNumber(modelInfo.subjectiveAiScores?.average_score).toFixed(1) }}</div>
                <div class="score-label">主观题(AI)</div>
                <div class="score-detail">{{ modelInfo.subjectiveAiScores?.total_answers || 0 }}题</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="score-card subjective-human" shadow="hover">
            <div class="score-content">
              <div class="score-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="score-info">
                <div class="score-value">{{ safeNumber(modelInfo.subjectiveHumanScores?.average_score).toFixed(1) }}</div>
                <div class="score-label">主观题(人工)</div>
                <div class="score-detail">{{ modelInfo.subjectiveHumanScores?.total_answers || 0 }}题</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 详细评分数据 -->
    <div class="detailed-scores-section">
      <el-tabs v-model="activeTab" type="card">
        <!-- 客观题详情 -->
        <el-tab-pane label="客观题详情" name="objective">
          <div class="objective-details">
            <div class="stats-summary">
              <el-row :gutter="16">
                <el-col :span="8">
                  <div class="stat-item">
                    <div class="stat-label">单选题</div>
                    <div class="stat-value">
                      {{ safeNumber(modelInfo.objectiveScores?.single_choice_avg).toFixed(1) }}分
                    </div>
                    <div class="stat-count">{{ modelInfo.objectiveScores?.single_choice_count || 0 }}题</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item">
                    <div class="stat-label">多选题</div>
                    <div class="stat-value">
                      {{ safeNumber(modelInfo.objectiveScores?.multiple_choice_avg).toFixed(1) }}分
                    </div>
                    <div class="stat-count">{{ modelInfo.objectiveScores?.multiple_choice_count || 0 }}题</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item">
                    <div class="stat-label">简单事实题</div>
                    <div class="stat-value">
                      {{ safeNumber(modelInfo.objectiveScores?.simple_fact_avg).toFixed(1) }}分
                    </div>
                    <div class="stat-count">{{ modelInfo.objectiveScores?.simple_fact_count || 0 }}题</div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <div class="objective-chart-container">
              <div ref="objectiveChart" class="chart"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 主观题详情 -->
        <el-tab-pane label="主观题详情" name="subjective">
          <div class="subjective-details">
            <el-row :gutter="24">
              <!-- AI评分详情 -->
              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <el-icon><Setting /></el-icon>
                      <span>AI评分详情</span>
                    </div>
                  </template>
                  <div class="criteria-list">
                    <div
                      v-for="criteria in modelInfo.subjectiveAiScores?.criteriaScores || []"
                      :key="criteria.criterion_name"
                      class="criteria-item"
                    >
                      <div class="criteria-header">
                        <span class="criteria-name">{{ criteria.criterion_name }}</span>
                        <span class="criteria-score">{{ safeNumber(criteria.average_score).toFixed(1) }}分</span>
                      </div>
                      <el-progress
                        :percentage="Math.round(safeNumber(criteria.average_score))"
                        :color="getScoreColor(criteria.average_score)"
                        :show-text="false"
                        :stroke-width="6"
                      />
                      <div class="criteria-count">{{ criteria.count }}次评测</div>
                    </div>
                  </div>
                </el-card>
              </el-col>

              <!-- 人工评分详情 -->
              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <el-icon><User /></el-icon>
                      <span>人工评分详情</span>
                    </div>
                  </template>
                  <div class="criteria-list">
                    <div
                      v-for="criteria in modelInfo.subjectiveHumanScores?.criteriaScores || []"
                      :key="criteria.criterion_name"
                      class="criteria-item"
                    >
                      <div class="criteria-header">
                        <span class="criteria-name">{{ criteria.criterion_name }}</span>
                        <span class="criteria-score">{{ safeNumber(criteria.average_score).toFixed(1) }}分</span>
                      </div>
                      <el-progress
                        :percentage="Math.round(safeNumber(criteria.average_score))"
                        :color="getScoreColor(criteria.average_score)"
                        :show-text="false"
                        :stroke-width="6"
                      />
                      <div class="criteria-count">{{ criteria.count }}次评测</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 统计信息 -->
        <el-tab-pane label="统计信息" name="statistics">
          <div class="statistics-details">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <el-icon><DataAnalysis /></el-icon>
                      <span>评测统计</span>
                    </div>
                  </template>
                  <div class="stats-grid">
                    <div class="stat-box">
                      <div class="stat-number">{{ modelInfo.detailStats?.total_evaluations || 0 }}</div>
                      <div class="stat-text">总评测数</div>
                    </div>
                    <div class="stat-box">
                      <div class="stat-number">{{ modelInfo.detailStats?.total_answers || 0 }}</div>
                      <div class="stat-text">总回答数</div>
                    </div>
                    <div class="stat-box">
                      <div class="stat-number">{{ modelInfo.detailStats?.total_evaluators || 0 }}</div>
                      <div class="stat-text">评测员数</div>
                    </div>
                    <div class="stat-box">
                      <div class="stat-number">{{ modelInfo.detailStats?.success_count || 0 }}</div>
                      <div class="stat-text">成功评测</div>
                    </div>
                  </div>
                </el-card>
              </el-col>

              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <el-icon><PieChart /></el-icon>
                      <span>成功率分析</span>
                    </div>
                  </template>
                  <div class="success-rate-container">
                    <div ref="successRateChart" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 操作按钮 -->
    <div class="action-section">
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" @click="exportModelReport">导出报告</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Monitor,
  Trophy,
  Document,
  Setting,
  User,
  DataAnalysis,
  PieChart
} from '@element-plus/icons-vue'
import type { ModelScore } from '@/api/evaluations'

// Props
interface Props {
  batchId: number | null
  modelInfo: ModelScore
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  close: []
}>()

// 响应式数据
const activeTab = ref('objective')
const objectiveChart = ref<HTMLElement>()
const successRateChart = ref<HTMLElement>()
let objectiveChartInstance: echarts.ECharts | null = null
let successRateChartInstance: echarts.ECharts | null = null

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

const getRankClass = (rank: number): string => {
  if (rank === 1) return 'rank-first'
  if (rank === 2) return 'rank-second'
  if (rank === 3) return 'rank-third'
  return 'rank-normal'
}

// 渲染客观题图表
const renderObjectiveChart = () => {
  if (!objectiveChart.value) return

  if (objectiveChartInstance) {
    objectiveChartInstance.dispose()
  }

  objectiveChartInstance = echarts.init(objectiveChart.value)

  const data = [
    {
      name: '单选题',
      value: safeNumber(props.modelInfo.objectiveScores?.single_choice_avg),
      count: props.modelInfo.objectiveScores?.single_choice_count || 0
    },
    {
      name: '多选题',
      value: safeNumber(props.modelInfo.objectiveScores?.multiple_choice_avg),
      count: props.modelInfo.objectiveScores?.multiple_choice_count || 0
    },
    {
      name: '简单事实题',
      value: safeNumber(props.modelInfo.objectiveScores?.simple_fact_avg),
      count: props.modelInfo.objectiveScores?.simple_fact_count || 0
    }
  ]

  const option = {
    title: {
      text: '客观题各类型得分',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        const dataItem = data[item.dataIndex]
        return `${item.name}<br/>平均分: ${item.value.toFixed(1)}<br/>题目数: ${dataItem.count}`
      }
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.name)
    },
    yAxis: {
      type: 'value',
      name: '分数',
      max: 100
    },
    series: [
      {
        type: 'bar',
        data: data.map(item => ({
          value: item.value,
          itemStyle: {
            color: getScoreColor(item.value)
          }
        })),
        barWidth: '60%'
      }
    ]
  }

  objectiveChartInstance.setOption(option)
}

// 渲染成功率图表
const renderSuccessRateChart = () => {
  if (!successRateChart.value) return

  if (successRateChartInstance) {
    successRateChartInstance.dispose()
  }

  successRateChartInstance = echarts.init(successRateChart.value)

  const successCount = props.modelInfo.detailStats?.success_count || 0
  const failedCount = props.modelInfo.detailStats?.failed_count || 0
  const total = successCount + failedCount

  const option = {
    title: {
      text: '评测成功率',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    series: [
      {
        name: '评测结果',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '60%'],
        data: [
          {
            value: successCount,
            name: '成功',
            itemStyle: { color: '#67C23A' }
          },
          {
            value: failedCount,
            name: '失败',
            itemStyle: { color: '#F56C6C' }
          }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  successRateChartInstance.setOption(option)
}

// 导出模型报告
const exportModelReport = () => {
  ElMessage.info('导出功能开发中...')
}

// 生命周期
onMounted(async () => {
  await nextTick()
  renderObjectiveChart()
  renderSuccessRateChart()
})
</script>

<style scoped lang="scss">
.model-detail-view {
  padding: 16px;
}

.model-info-section {
  margin-bottom: 24px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    color: #303133;
  }

  .model-basic-info {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;

    .info-item {
      display: flex;
      align-items: center;
      gap: 8px;

      label {
        font-weight: 500;
        color: #606266;
        min-width: 80px;
      }

      .value {
        color: #303133;
        font-weight: 500;
      }

      .rank-badge {
        padding: 4px 12px;
        border-radius: 16px;
        font-weight: bold;
        color: white;

        &.rank-first {
          background: linear-gradient(135deg, #ffd700, #ffed4e);
          color: #333;
        }

        &.rank-second {
          background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
          color: #333;
        }

        &.rank-third {
          background: linear-gradient(135deg, #cd7f32, #daa520);
        }

        &.rank-normal {
          background: #909399;
        }
      }
    }
  }
}

.score-overview-section {
  margin-bottom: 24px;

  .score-card {
    height: 120px;

    .score-content {
      display: flex;
      align-items: center;
      gap: 16px;
      height: 100%;

      .score-icon {
        width: 60px;
        height: 60px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        color: white;
      }

      .score-info {
        flex: 1;

        .score-value {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
          line-height: 1;
          margin-bottom: 4px;
        }

        .score-label {
          font-size: 14px;
          color: #606266;
          margin-bottom: 2px;
        }

        .score-detail {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    &.overall .score-icon {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }

    &.objective .score-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.subjective-ai .score-icon {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }

    &.subjective-human .score-icon {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }
  }
}

.detailed-scores-section {
  margin-bottom: 24px;

  .objective-details {
    .stats-summary {
      margin-bottom: 24px;

      .stat-item {
        text-align: center;
        padding: 16px;
        background: #f8f9fa;
        border-radius: 8px;

        .stat-label {
          font-size: 14px;
          color: #606266;
          margin-bottom: 8px;
        }

        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 4px;
        }

        .stat-count {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .objective-chart-container {
      height: 300px;

      .chart {
        width: 100%;
        height: 100%;
      }
    }
  }

  .subjective-details {
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      color: #303133;
    }

    .criteria-list {
      .criteria-item {
        margin-bottom: 20px;

        &:last-child {
          margin-bottom: 0;
        }

        .criteria-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .criteria-name {
            font-weight: 500;
            color: #303133;
          }

          .criteria-score {
            font-weight: bold;
            color: #409eff;
          }
        }

        .criteria-count {
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }
      }
    }
  }

  .statistics-details {
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      color: #303133;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;

      .stat-box {
        text-align: center;
        padding: 20px;
        background: #f8f9fa;
        border-radius: 8px;

        .stat-number {
          font-size: 32px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 8px;
        }

        .stat-text {
          font-size: 14px;
          color: #606266;
        }
      }
    }

    .success-rate-container {
      height: 250px;

      .chart {
        width: 100%;
        height: 100%;
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
  .model-basic-info {
    grid-template-columns: 1fr;
  }

  .score-overview-section .el-col {
    margin-bottom: 16px;
  }

  .subjective-details .el-col {
    margin-bottom: 24px;
  }

  .statistics-details .el-col {
    margin-bottom: 24px;
  }
}
</style>
