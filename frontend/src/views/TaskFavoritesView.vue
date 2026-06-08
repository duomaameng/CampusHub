<script setup lang="ts">
import { Bookmark, Clock, MapPin, Tag, Users } from '@lucide/vue'
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, TaskItem } from '@/types'

const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')

async function loadFavorites() {
  error.value = ''
  loading.value = true
  try {
    page.value = await taskApi.favorites({ page: 1, size: 20 })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '收藏列表加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadFavorites)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>我的收藏</h1>
        <p>集中查看已经收藏的需求，继续跟进或取消收藏可进入详情页操作。</p>
      </div>
      <RouterLink class="button ghost" to="/tasks">返回任务大厅</RouterLink>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载收藏列表</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无收藏的需求</div>

    <div v-else class="cards-grid">
      <RouterLink v-for="task in page.records" :key="task.id" class="item-card" :to="`/tasks/${task.id}`">
        <div class="item-title">
          <h2>{{ task.title }}</h2>
          <span :class="['tag', task.status === 'OPEN' ? 'success' : 'warning']">{{ taskStatusText[task.status] }}</span>
        </div>
        <p>{{ task.description }}</p>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ task.rewardType }}</span>
          <span><Clock class="meta-icon" aria-hidden="true" />{{ new Date(task.deadline).toLocaleString() }}</span>
        </div>
        <div class="meta-line">
          <span><Users class="meta-icon" aria-hidden="true" />申请 {{ task.applicationCount }}</span>
          <span><Bookmark class="meta-icon" aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.item-card {
  padding: var(--space-5);
  border: 1.5px solid var(--border-light);
}

.item-card p {
  color: var(--text-secondary);
  line-height: 1.65;
}

.meta-line {
  font-size: 12px;
}
</style>
