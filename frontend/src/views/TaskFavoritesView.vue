<script setup lang="ts">
import { Bookmark, Clock, MapPin, Tag, Users } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, RewardType, TaskItem, TaskStatus } from '@/types'

const filters = reactive<{
  status: '' | TaskStatus
  keyword: string
}>({
  status: '',
  keyword: ''
})
const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')

const rewardText: Record<RewardType, string> = {
  CASH: '现金',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分意向'
}

const filteredRecords = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return (page.value?.records || []).filter((task) => {
    const matchesStatus = !filters.status || task.status === filters.status
    const matchesKeyword =
      !keyword ||
      task.title.toLowerCase().includes(keyword) ||
      task.description.toLowerCase().includes(keyword)
    return matchesStatus && matchesKeyword
  })
})

async function loadFavorites() {
  error.value = ''
  loading.value = true
  try {
    page.value = await taskApi.favorites({ page: 1, size: 100 })
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

    <form class="toolbar" @submit.prevent="loadFavorites">
      <div class="field">
        <label for="favorite-status">状态</label>
        <select id="favorite-status" v-model="filters.status">
          <option value="">全部状态</option>
          <option value="OPEN">开放中</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="COMPLETED">已完成</option>
          <option value="EXPIRED">已过期</option>
          <option value="CANCELLED">已取消</option>
        </select>
      </div>
      <div class="field">
        <label for="favorite-keyword">关键词</label>
        <input id="favorite-keyword" v-model.trim="filters.keyword" type="search" placeholder="搜索标题或描述" />
      </div>
      <div class="field">
        <label>&nbsp;</label>
        <button class="button secondary" type="submit">筛选</button>
      </div>
    </form>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载收藏列表</div>
    <div v-else-if="!filteredRecords.length" class="empty-state">暂无符合条件的收藏需求</div>

    <div v-else class="cards-grid">
      <RouterLink v-for="task in filteredRecords" :key="task.id" class="item-card" :to="`/tasks/${task.id}`">
        <div class="item-title">
          <h2>{{ task.title }}</h2>
          <span :class="['tag', task.status === 'OPEN' ? 'success' : 'warning']">{{ taskStatusText[task.status] }}</span>
        </div>
        <p>{{ task.description }}</p>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ rewardText[task.rewardType] }}</span>
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
.toolbar {
  position: relative;
}

.toolbar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.15), transparent);
}

.toolbar .field select,
.toolbar .field input {
  padding: 10px 14px;
  font-weight: 500;
  background: var(--bg-surface);
  border: 1.5px solid var(--border-light);
  transition: all var(--transition-fast);
}

.toolbar .field select:focus,
.toolbar .field input:focus {
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1), var(--shadow-sm);
}

.toolbar .field label {
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.toolbar .button.secondary {
  padding: 10px 24px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  font-size: 11px;
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  color: white;
  border: none;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.toolbar .button.secondary:hover {
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
  transform: translateY(-1px);
}

.item-card {
  padding: var(--space-5);
  border: 1.5px solid var(--border-light);
  position: relative;
}

.item-card::after {
  content: '';
  position: absolute;
  inset: auto;
  top: 0;
  left: 0;
  right: 0;
  bottom: auto;
  width: auto;
  min-width: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-400), var(--secondary-400));
  filter: none;
  opacity: 0;
  transform: none;
  transition: opacity var(--transition-base);
}

.item-card:hover::after {
  opacity: 1;
}

.item-card p {
  color: var(--text-secondary);
  line-height: 1.65;
}

.meta-line {
  font-size: 12px;
}

@media (max-width: 768px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
