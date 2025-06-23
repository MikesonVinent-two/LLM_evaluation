<template>
  <div class="crowdsource-review">
    <el-card class="review-card">
      <template #header>
        <div class="card-header">
          <h2>众包回答审核</h2>
        </div>
      </template>
      <div class="review-content">
        <p>正在施工中...</p>
        <p>此页面将用于审核众包用户的回答，包括审核、通过、拒绝回答等功能。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 后续添加众包回答审核相关逻辑
</script>

<style scoped>
.crowdsource-review {
  padding: 20px;
}

.review-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-content {
  padding: 20px 0;
}
</style>
