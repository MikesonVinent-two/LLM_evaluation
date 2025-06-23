<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import UserProfileCard from '@/components/UserProfileCard.vue'

const route = useRoute()
const userId = ref<string | undefined>(route.params.id as string | undefined)
</script>

<template>
  <div class="user-profile-view">
    <div class="container">
      <h1 class="page-title">用户详情</h1>
      <UserProfileCard :userId="userId" />
    </div>
  </div>
</template>

<style scoped>
.user-profile-view {
  padding: var(--navbar-with-margin) 0 40px;
  min-height: calc(100vh - var(--navbar-height));
}

.container {
  max-width: var(--container-width);
  margin: 0 auto;
  padding: 0 var(--container-padding);
}

.page-title {
  text-align: center;
  margin-bottom: 40px;
  color: var(--primary-color);
  font-size: 2rem;
}

@media (max-width: 768px) {
  .page-title {
    font-size: 1.5rem;
    margin-bottom: 30px;
  }
}
</style>
