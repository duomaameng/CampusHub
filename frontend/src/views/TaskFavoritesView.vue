<script setup lang="ts">
import { Bookmark, Clock, MapPin, Tag, Users } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, TaskItem, TaskStatus } from '@/types'

const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')
const filters = reactive({
  status: '',
  keyword: '',
  sort: 'latest'
})

const statusTagClass: Record<TaskStatus, string> = {
  OPEN: 'success',
  IN_PROGRESS: 'info',
  COMPLETED: 'warning',
  CANCELLED: 'danger',
  EXPIRED: 'danger'
}

const filteredFavorites = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  const records = [...(page.value?.records || [])].filter((task) => {
    const matchesStatus = !filters.status || task.status === filters.status
    const matchesKeyword = !keyword || `${task.title} ${task.description} ${task.campus}`.toLowerCase().includes(keyword)
    return matchesStatus && matchesKeyword
  })

  return records.sort((a, b) => {
    if (filters.sort === 'deadline') {
      return new Date(a.deadline).getTime() - new Date(b.deadline).getTime()
    }
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

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
  <section class="task-favorites-view">
    <div class="page-title">
      <div>
        <h1>我的收藏</h1>
      </div>
      <RouterLink class="button ghost" to="/tasks">返回任务大厅</RouterLink>
    </div>

    <form class="toolbar" @submit.prevent>
      <div class="field">
        <label for="favorite-status">状态</label>
        <select id="favorite-status" v-model="filters.status">
          <option value="">全部状态</option>
          <option value="OPEN">待接单</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
          <option value="EXPIRED">已过期</option>
        </select>
      </div>
      <div class="field">
        <label for="favorite-keyword">关键词</label>
        <input id="favorite-keyword" v-model.trim="filters.keyword" type="search" placeholder="标题、描述或校区" />
      </div>
      <div class="field">
        <label for="favorite-sort">排序</label>
        <select id="favorite-sort" v-model="filters.sort">
          <option value="latest">最新收藏</option>
          <option value="deadline">截止时间</option>
        </select>
      </div>
    </form>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载收藏列表</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无收藏的需求</div>

    <div v-else-if="!filteredFavorites.length" class="empty-state">暂无符合筛选条件的收藏</div>

    <div v-else class="cards-grid">
      <RouterLink v-for="(task, index) in filteredFavorites" :key="task.id" class="item-card" :to="`/tasks/${task.id}`" :style="{ '--i': index }">
        <div class="item-title">
          <div>
            <span class="relation-pill">收藏任务</span>
            <h2>{{ task.title }}</h2>
          </div>
          <span :class="['tag', 'status-tag', statusTagClass[task.status]]">{{ taskStatusText[task.status] }}</span>
        </div>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ task.rewardType }}</span>
        </div>
        <div class="meta-line">
          <span><Users class="meta-icon" aria-hidden="true" />申请 {{ task.applicationCount }}</span>
          <span><Bookmark class="meta-icon" aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
        </div>
        <p class="hint"><Clock class="meta-icon" aria-hidden="true" />截止时间 · {{ new Date(task.deadline).toLocaleString() }}</p>
        <img v-if="task.imageUrls && task.imageUrls.length" :src="task.imageUrls[0]" alt="" class="card-image" />
        <span v-else class="order-card-visual" aria-hidden="true">
          <span class="visual-dot" />
        </span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.task-favorites-view {
  --favorite-green: #ffb454;
  --favorite-dark: #191a23;
  --favorite-grey: #f3f3f3;
}

.page-title {
  align-items: flex-start;
  margin-bottom: 28px;
}

.page-title h1 {
  width: max-content;
  padding: 5px 14px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: transparent;
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
  box-shadow: none;
}

.page-title .button.ghost {
  padding: 12px 22px;
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
  box-shadow: none;
}

.page-title .button.ghost:hover {
  background: var(--favorite-green);
  box-shadow: none;
  transform: translateY(-2px);
}

.toolbar {
  position: relative;
  grid-template-columns: minmax(180px, 0.8fr) minmax(240px, 1fr) minmax(180px, 0.8fr);
  gap: 22px;
  margin-bottom: 28px;
  padding: 26px 28px;
  border: 2px solid #000000;
  border-radius: 26px;
  background: #ffffff;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  overflow: visible;
}

.toolbar::before {
  display: none;
}

.toolbar .field select,
.toolbar .field input {
  min-height: 50px;
  padding: 10px 16px;
  color: #000000;
  font-weight: 800;
  background: #ffffff;
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: none;
  transition: all var(--transition-fast);
}

.toolbar .field select:hover,
.toolbar .field input:hover {
  background: #fff1df;
}

.toolbar .field select:focus,
.toolbar .field input:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(255, 180, 84, 0.48);
}

.toolbar .field label {
  color: #4a4e5b;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.item-card {
  padding: 22px 130px 22px 26px;
  border: 2px solid #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: none;
  position: relative;
  overflow: hidden;
}

.item-card::after {
  display: none;
}

.order-card-visual {
  position: absolute;
  right: 30px;
  bottom: 24px;
  z-index: 0;
  width: 128px;
  height: 74px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: #ffffff;
  transform: rotate(-6deg);
  transition: transform var(--transition-fast);
  pointer-events: none;
}

.order-card-visual .visual-dot {
  position: absolute;
  right: 18px;
  top: 14px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--favorite-green);
}

.card-image {
  position: absolute;
  right: 24px;
  top: 54px;
  z-index: 0;
  width: 120px;
  height: 90px;
  object-fit: cover;
  border: 2px solid #000000;
  border-radius: 22px;
  transform: rotate(-4deg);
  transition: transform var(--transition-fast);
}

.item-card:hover .card-image {
  transform: rotate(-2deg);
}

.item-card:hover .order-card-visual {
  transform: rotate(-3deg);
}

.item-title {
  position: relative;
  z-index: 1;
  align-items: flex-start;
  gap: 16px;
}

.item-title > div {
  width: 100%;
}

.item-card h2 {
  max-width: 100%;
  margin-top: 8px;
  font-size: 21px;
  font-weight: 900;
  line-height: 1.18;
  letter-spacing: 0;
}

.relation-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 11px;
  border: 2px solid #000000;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  color: #000000;
  background: var(--favorite-green);
}

.item-card .meta-line,
.item-card > .hint {
  position: relative;
  z-index: 1;
}

.item-card .meta-line {
  max-width: 620px;
  font-size: 12.5px;
  gap: 12px;
}

.item-card .tag {
  position: absolute;
  top: 0;
  left: 88px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: max-content;
  min-width: max-content;
  max-width: none;
  margin-left: 0;
  flex-shrink: 0;
  z-index: 2;
  white-space: nowrap;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
  color: #000000;
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: 0;
  padding: 4px 12px;
  box-shadow: none;
}

.item-card .hint {
  max-width: 620px;
  color: #6f7485;
  font-weight: 800;
}

.item-card .status-tag.success {
  background: linear-gradient(135deg, var(--success-bg), rgba(245, 158, 11, 0.08));
  color: #9a3412;
  border-color: rgba(245, 158, 11, 0.28);
}

.item-card .status-tag.info {
  background: linear-gradient(135deg, var(--info-bg), rgba(59, 130, 246, 0.08));
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.28);
}

.item-card .status-tag.warning {
  background: linear-gradient(135deg, var(--warning-bg), rgba(245, 158, 11, 0.08));
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.28);
}

.item-card .status-tag.danger {
  background: linear-gradient(135deg, var(--danger-bg), rgba(239, 68, 68, 0.08));
  color: #b91c1c;
  border-color: rgba(239, 68, 68, 0.28);
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  border: 2px dashed #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: none;
  color: #4a4e5b;
  font-size: 14px;
  font-weight: 800;
}

.error-message {
  margin-bottom: var(--space-4);
  padding: 12px 16px;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: none;
  font-weight: 800;
}

.task-favorites-view .cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  grid-auto-rows: auto;
  gap: 22px;
}

@media (max-width: 768px) {
  .task-favorites-view .cards-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .item-card {
    min-height: auto;
    padding: var(--space-5);
  }

  .order-card-visual {
    display: none;
  }

  .card-image {
    position: static;
    transform: none;
    width: 100%;
    height: 160px;
    margin-bottom: 12px;
    border-radius: 16px;
  }

  .item-card:hover .card-image {
    transform: none;
  }
}
</style>





