<template>
  <div class="scoring">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <h2>评分管理</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 功能开发中提示 -->
    <el-card>
      <div class="development-notice">
        <el-empty description="评分管理功能正在开发中...">
          <template #image>
            <el-icon :size="64" color="#909399"><Tools /></el-icon>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'

// 状态
const loading = ref(false)

// 刷新数据
const refreshData = () => {
  ElMessage.info('该功能正在开发中')
}
</script>

<style scoped>
.scoring {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.development-notice {
  padding: 40px;
  text-align: center;
}
</style>
