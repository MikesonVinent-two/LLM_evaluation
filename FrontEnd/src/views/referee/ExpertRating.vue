<template>
  <div class="expert-rating">
    <el-card class="rating-card">
      <template #header>
        <div class="card-header">
          <h2>专家回答评分</h2>
        </div>
      </template>
      <div class="rating-content">
        <p>正在施工中...</p>
        <p>此页面将用于为专家回答评分，包括查看专家回答、评分、提交评价等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加专家回答评分相关逻辑
</script>

<style scoped>
.expert-rating {
  padding: 20px;
}

.rating-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rating-content {
  padding: 20px 0;
}
</style>
