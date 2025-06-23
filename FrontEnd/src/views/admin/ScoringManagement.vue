<template>
  <div class="scoring-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <h2>算分管理</h2>
        </div>
      </template>
      <div class="management-content">
        <p>正在施工中...</p>
        <p>此页面将用于管理评测结果的算分，包括执行算分、查看分数统计等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加算分管理相关逻辑
</script>

<style scoped>
.scoring-management {
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

.management-content {
  padding: 20px 0;
}
</style>
