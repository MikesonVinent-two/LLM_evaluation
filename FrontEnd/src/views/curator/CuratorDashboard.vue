<template>
  <div class="curator-dashboard">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <h2>数据管理员工作台</h2>
          <p>欢迎使用数据管理系统，您可以在这里进行原始问题和回答的管理。</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="8">
        <el-card class="function-card" @click="navigateTo('/data/original-questions')">
          <div class="card-content">
            <el-icon class="icon"><Document /></el-icon>
            <div class="info">
              <h3>原始问题管理</h3>
              <p>录入、编辑和管理原始问题数据</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="function-card" @click="navigateTo('/data/batch-import')">
          <div class="card-content">
            <el-icon class="icon"><Upload /></el-icon>
            <div class="info">
              <h3>批量数据导入</h3>
              <p>批量导入问题及其对应的回答</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="function-card" @click="navigateTo('/data/datasets')">
          <div class="card-content">
            <el-icon class="icon"><Collection /></el-icon>
            <div class="info">
              <h3>数据集管理</h3>
              <p>管理系统中的数据集</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <h3>原始问题统计</h3>
            </div>
          </template>
          <div class="stat-content">
            <div class="stat-item">
              <div class="label">总问题数量：</div>
              <div class="value">{{ stats.totalQuestions }}</div>
            </div>
            <div class="stat-item">
              <div class="label">已标准化问题：</div>
              <div class="value">{{ stats.standardizedQuestions }}</div>
            </div>
            <div class="stat-item">
              <div class="label">未标准化问题：</div>
              <div class="value">{{ stats.totalQuestions - stats.standardizedQuestions }}</div>
            </div>
            <div class="stat-item">
              <div class="label">标准化比例：</div>
              <div class="value">{{ stats.standardizationRate }}%</div>
            </div>
            <el-progress :percentage="stats.standardizationRate" :format="format" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card class="recent-card">
          <template #header>
            <div class="card-header">
              <h3>最近活动</h3>
              <el-button text @click="refreshRecentActivity">刷新</el-button>
            </div>
          </template>
          <div class="recent-content">
            <el-timeline>
              <el-timeline-item
                v-for="(activity, index) in recentActivities"
                :key="index"
                :timestamp="activity.time"
                :type="activity.type"
              >
                {{ activity.content }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Collection, Upload } from '@element-plus/icons-vue'

const router = useRouter()

// 统计数据
const stats = reactive({
  totalQuestions: 0,
  standardizedQuestions: 0,
  standardizationRate: 0
})

// 最近活动
const recentActivities = ref([
  {
    content: '录入了10个新的原始问题',
    time: '2025-06-01 10:30:00',
    type: 'primary'
  },
  {
    content: '批量导入了5个问题及其回答',
    time: '2025-06-01 09:15:00',
    type: 'success'
  },
  {
    content: '更新了问题ID为15的原始问题',
    time: '2025-05-31 16:45:00',
    type: 'info'
  },
  {
    content: '将3个问题添加到数据集',
    time: '2025-05-31 14:20:00',
    type: 'warning'
  },
  {
    content: '创建了新的医学问答数据集',
    time: '2025-05-30 11:05:00',
    type: 'success'
  }
])

// 导航到指定路径
const navigateTo = (path: string) => {
  router.push(path)
}

// 进度条格式化
const format = (percentage: number) => {
  return `${percentage}%`
}

// 刷新最近活动
const refreshRecentActivity = () => {
  // 实际项目中，这里应该调用API获取最新活动
  // 此处仅为示例
}

// 获取统计数据
const fetchStats = () => {
  // 实际项目中，这里应该调用API获取真实统计数据
  // 此处使用模拟数据

  // 模拟问题统计
  stats.totalQuestions = 256
  stats.standardizedQuestions = 182
  stats.standardizationRate = Math.round((stats.standardizedQuestions / stats.totalQuestions) * 100)
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.curator-dashboard {
  padding: 20px;
}

.mt-20 {
  margin-top: 20px;
}

.welcome-card {
  background-color: #f0f9ff;
  border-radius: 8px;
  padding: 10px;
}

.welcome-card h2 {
  margin-top: 0;
  color: #1989fa;
}

.function-card {
  height: 150px;
  cursor: pointer;
  transition: all 0.3s;
}

.function-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.icon {
  font-size: 40px;
  margin-bottom: 15px;
  color: #409eff;
}

.info h3 {
  margin-top: 0;
  margin-bottom: 10px;
}

.info p {
  margin: 0;
  color: #606266;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-content {
  padding: 10px 0;
}

.stat-item {
  display: flex;
  margin-bottom: 10px;
}

.stat-item .label {
  width: 120px;
  font-weight: bold;
}

.stat-item .value {
  font-size: 16px;
  color: #409eff;
}

.recent-content {
  max-height: 300px;
  overflow-y: auto;
}
</style>
