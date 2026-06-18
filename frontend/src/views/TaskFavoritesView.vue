<script setup lang="ts">
import { Bookmark, Clock, MapPin, Tag, Users } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, TaskItem } from '@/types'

const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')
const filters = reactive({
  status: '',
  keyword: '',
  sort: 'latest'
})

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
      <RouterLink v-for="(task, index) in filteredFavorites" :key="task.id" class="item-card" :class="{ 'has-image': task.imageUrls && task.imageUrls.length }" :to="`/tasks/${task.id}`" :style="{ '--i': index }">
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
        <img v-if="task.imageUrls && task.imageUrls.length" :src="task.imageUrls[0]" alt="" class="card-image" />
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.task-favorites-view {
  --favorite-green: #7dbe8e;
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
  background: #e8f5ec;
}

.toolbar .field select:focus,
.toolbar .field input:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(125, 190, 142, 0.48);
}

.toolbar .field label {
  color: #4a4e5b;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.item-card {
  padding: 22px 26px;
  border: 2px solid #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: none;
  position: relative;
  overflow: hidden;
}

.item-card::before {
  display: none;
}

.item-card::after {
  content: '';
  position: absolute;
  right: 28px;
  bottom: 24px;
  width: 124px;
  height: 72px;
  border: 2px solid #000000;
  border-radius: 22px;
  background:
    radial-gradient(circle at 74% 28%, var(--favorite-green) 0 22px, transparent 23px),
    #ffffff;
  filter: none;
  opacity: 1;
  transform: rotate(-6deg);
  transition: transform var(--transition-fast);
}

.item-card.has-image::after {
  display: none;
}

.card-image {
  position: absolute;
  right: 28px;
  bottom: 24px;
  width: 120px;
  height: 90px;
  object-fit: cover;
  border: 2px solid #000000;
  border-radius: 22px;
  transform: rotate(-4deg);
  transition: transform var(--transition-fast);
  z-index: 0;
}

.item-card:hover .card-image {
  transform: rotate(-2deg);
}

.item-card:hover::after {
  opacity: 1;
  transform: rotate(-3deg);
}

.item-title,
.item-card p,
.meta-line {
  position: relative;
  z-index: 1;
}

.item-title {
  position: static;
  align-items: flex-start;
  gap: 16px;
}

.item-title h2 {
  position: relative;
  z-index: 1;
}

.item-title > h2 {
  margin-right: 0;
}

.item-card h2 {
  max-width: calc(100% - 120px);
  font-size: 21px;
  font-weight: 900;
  line-height: 1.18;
}

.item-card p {
  max-width: 480px;
  color: #343743;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-line {
  max-width: 520px;
  font-size: 11.5px;
  gap: 10px;
}

.meta-line span {
  color: #6f7485;
  font-weight: 800;
}

.item-card .tag {
  position: absolute;
  right: 30px;
  bottom: 28px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: max-content;
  min-width: max-content;
  max-width: none;
  white-space: nowrap;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
  color: #000000;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  padding: 5px 12px;
  box-shadow: none;
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

  .item-card::after {
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
}
</style>
