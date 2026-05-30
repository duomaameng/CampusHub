<script setup lang="ts">
import {
  ArrowRight,
  Bookmark,
  CalendarClock,
  CheckCircle2,
  Clock,
  HandHeart,
  MapPin,
  PlusCircle,
  Search,
  SlidersHorizontal,
  Tag,
  Users
} from '@lucide/vue'
import { gsap } from 'gsap'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, TaskItem, TaskStatus } from '@/types'

const categories = [
  { value: '', label: '全部分类' },
  { value: 'EXPRESS', label: '快递代取' },
  { value: 'ERRAND', label: '跑腿代办' },
  { value: 'TUTORING', label: '学习辅导' },
  { value: 'SECOND_HAND', label: '二手交易' },
  { value: 'LOST_FOUND', label: '失物招领' },
  { value: 'CONSULTATION', label: '咨询问答' },
  { value: 'TEAM_UP', label: '组队搭子' },
  { value: 'OTHER', label: '其他' }
]

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'OPEN', label: '待接单' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'EXPIRED', label: '已过期' }
]

const categoryText: Record<string, string> = Object.fromEntries(
  categories.filter((item) => item.value).map((item) => [item.value, item.label])
)

const rewardText: Record<string, string> = {
  CASH: '现金',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分意向'
}

const filters = reactive({
  category: '',
  campus: '',
  keyword: '',
  sort: 'latest',
  status: ''
})

const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')
const taskHallRoot = ref<HTMLElement | null>(null)
let animationContext: gsap.Context | undefined

const filteredRecords = computed(() => {
  const records = page.value?.records ?? []
  return filters.status ? records.filter((task) => task.status === filters.status) : records
})

const openTaskCount = computed(() => filteredRecords.value.filter((task) => task.status === 'OPEN').length)
const totalApplications = computed(() => filteredRecords.value.reduce((sum, task) => sum + task.applicationCount, 0))

async function loadTasks() {
  error.value = ''
  loading.value = true
  try {
    page.value = await taskApi.list({
      category: filters.category,
      campus: filters.campus,
      keyword: filters.keyword,
      sort: filters.sort,
      page: 1,
      size: 100
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '任务加载失败'
  } finally {
    loading.value = false
    await nextTick()
    animateCards()
  }
}

async function applyFilters() {
  await loadTasks()
}

function animateCards() {
  if (!animationContext || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animationContext.add(() => {
    gsap.fromTo(
      '.task-card',
      { autoAlpha: 0, y: 18 },
      { autoAlpha: 1, y: 0, duration: 0.44, ease: 'power2.out', stagger: 0.045, overwrite: true }
    )
  })
}

function statusClass(status: TaskStatus) {
  if (status === 'OPEN') return 'success'
  if (status === 'IN_PROGRESS') return 'info'
  if (status === 'COMPLETED') return 'done'
  return 'muted'
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  await loadTasks()
  if (!taskHallRoot.value) return

  animationContext = gsap.context(() => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduceMotion) {
      gsap.set(['.hall-title', '.hall-search', '.hall-filter-panel', '.hall-stats', '.task-card'], {
        autoAlpha: 1,
        clearProps: 'transform,visibility,opacity'
      })
      return
    }

    gsap.timeline({ defaults: { duration: 0.55, ease: 'power3.out' } })
      .from('.hall-title', { autoAlpha: 0, y: 16 })
      .from('.hall-search', { autoAlpha: 0, y: 16 }, '-=0.3')
      .from('.hall-filter-panel', { autoAlpha: 0, y: 16 }, '-=0.26')
      .from('.hall-stats article', { autoAlpha: 0, y: 12, stagger: 0.06 }, '-=0.22')
      .from('.task-card', { autoAlpha: 0, y: 18, stagger: 0.045 }, '-=0.18')
  }, taskHallRoot.value)
})

onUnmounted(() => {
  animationContext?.revert()
})
</script>

<template>
  <section ref="taskHallRoot" class="task-hall-page">
    <div class="hall-title">
      <div>
        <span class="hall-eyebrow">
          <HandHeart aria-hidden="true" />
          CampusHub Task Board
        </span>
        <h1>任务大厅</h1>
        <p>浏览公开需求，按分类、校区、关键词和截止时间筛选。</p>
      </div>
      <RouterLink class="publish-button" to="/tasks/new">
        <PlusCircle aria-hidden="true" />
        发布任务
      </RouterLink>
    </div>

    <form class="hall-search" @submit.prevent="applyFilters">
      <Search class="search-icon" aria-hidden="true" />
      <input v-model.trim="filters.keyword" type="search" placeholder="搜索任务关键词，例如：快递、打印、组队" />
      <button type="submit">搜索</button>
    </form>

    <form class="hall-filter-panel" @submit.prevent="applyFilters">
      <div class="filter-title">
        <SlidersHorizontal aria-hidden="true" />
        <span>筛选任务</span>
      </div>
      <label>
        <span>任务分类</span>
        <select v-model="filters.category">
          <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>排序方式</span>
        <select v-model="filters.sort">
          <option value="latest">最新发布</option>
          <option value="deadline">截止时间最近</option>
        </select>
      </label>
      <label>
        <span>任务状态</span>
        <select v-model="filters.status" @change="animateCards">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label>
        <span>校区</span>
        <select v-model="filters.campus">
          <option value="">全部校区</option>
          <option value="仙林校区">仙林校区</option>
          <option value="鼓楼校区">鼓楼校区</option>
          <option value="浦口校区">浦口校区</option>
          <option value="苏州校区">苏州校区</option>
        </select>
      </label>
      <button class="filter-button" type="submit">应用筛选</button>
    </form>

    <section class="hall-stats" aria-label="任务概览">
      <article>
        <Tag aria-hidden="true" />
        <strong>{{ filteredRecords.length }}</strong>
        <span>当前结果</span>
      </article>
      <article>
        <CheckCircle2 aria-hidden="true" />
        <strong>{{ openTaskCount }}</strong>
        <span>待接任务</span>
      </article>
      <article>
        <Users aria-hidden="true" />
        <strong>{{ totalApplications }}</strong>
        <span>申请总数</span>
      </article>
    </section>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载任务</div>
    <div v-else-if="!filteredRecords.length" class="empty-state">暂无符合条件的任务</div>

    <div v-else class="cards-grid">
      <article v-for="task in filteredRecords" :key="task.id" class="task-card">
        <div class="task-card-head">
          <span class="category-pill">{{ categoryText[task.category] }}</span>
          <span :class="['status-pill', statusClass(task.status)]">{{ taskStatusText[task.status] }}</span>
        </div>

        <h2>{{ task.title }}</h2>
        <p>{{ task.description }}</p>

        <div class="task-meta">
          <span><Tag aria-hidden="true" />{{ rewardText[task.rewardType] }}</span>
          <span><MapPin aria-hidden="true" />{{ task.campus }}</span>
          <span><CalendarClock aria-hidden="true" />发布 {{ formatDate(task.createdAt) }}</span>
          <span><Clock aria-hidden="true" />截止 {{ formatDate(task.deadline) }}</span>
          <span><Users aria-hidden="true" />{{ task.publisherNickname }}</span>
          <span><Bookmark aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
        </div>

        <div class="task-card-actions">
          <RouterLink class="detail-link" :to="`/tasks/${task.id}`">
            查看详情
            <ArrowRight aria-hidden="true" />
          </RouterLink>
          <RouterLink class="accept-button" :to="`/tasks/${task.id}`">
            {{ task.status === 'OPEN' ? '接取任务' : '查看任务' }}
          </RouterLink>
        </div>
      </article>
    </div>

    <nav class="pagination" aria-label="任务分页">
      <span>共 {{ filteredRecords.length }} 条任务</span>
      <span>当前显示全部结果</span>
    </nav>
  </section>
</template>

<style scoped>
.task-hall-page {
  --campus-ink: #112031;
  --campus-ink-soft: #526071;
  --campus-paper: #fbf7ef;
  --campus-line: rgba(17, 32, 49, 0.12);
  --campus-leaf: #1e7d5f;
  --campus-leaf-deep: #125440;
  --campus-sun: #e8a84f;
  --campus-sky: #7ca7cc;
  --campus-shadow: 0 18px 44px rgba(26, 32, 44, 0.1);
  display: grid;
  gap: 18px;
  color: var(--campus-ink);
  font-family: 'ManropeLocal', 'SourceHanSansSC', 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

.hall-title,
.hall-search,
.hall-filter-panel,
.hall-stats article,
.task-card,
.pagination,
.empty-state,
.error-message {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(251, 247, 239, 0.84);
  box-shadow: var(--campus-shadow);
  backdrop-filter: blur(18px);
}

.hall-title {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 22px 26px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 12% 18%, rgba(205, 231, 219, 0.68), transparent 34%),
    radial-gradient(circle at 90% 82%, rgba(248, 223, 164, 0.24), transparent 28%),
    rgba(251, 247, 239, 0.88);
}

.hall-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--campus-leaf-deep);
  font-size: 0.88rem;
  font-weight: 850;
}

.hall-eyebrow svg,
.publish-button svg,
.detail-link svg {
  width: 1em;
  height: 1em;
}

.hall-title h1 {
  margin: 8px 0 0;
  color: var(--campus-ink);
  font-family: 'STZhongsong', 'Songti SC', 'Noto Serif SC', 'SourceHanSansSC', serif;
  font-size: clamp(2rem, 3.2vw, 3rem);
  line-height: 1.1;
  letter-spacing: 0;
}

.hall-title p {
  margin: 10px 0 0;
  color: var(--campus-ink-soft);
  line-height: 1.7;
}

.publish-button,
.hall-search button,
.filter-button,
.accept-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 20px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: var(--campus-leaf);
  font-weight: 850;
  text-decoration: none;
  box-shadow: 0 16px 30px rgba(30, 125, 95, 0.18);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    background-color 180ms ease;
}

.hall-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 24px;
}

.search-icon {
  width: 22px;
  height: 22px;
  color: var(--campus-leaf);
}

.hall-search input {
  min-height: 44px;
  border: 0;
  background: transparent;
  color: var(--campus-ink);
  font: inherit;
  font-size: 1rem;
}

.hall-search input:focus {
  outline: none;
}

.hall-filter-panel {
  display: grid;
  grid-template-columns: auto repeat(4, minmax(140px, 1fr)) auto;
  gap: 12px;
  align-items: end;
  padding: 18px;
  border-radius: 26px;
}

.filter-title,
.hall-filter-panel label {
  display: grid;
  gap: 8px;
}

.filter-title {
  align-self: center;
  min-width: 110px;
  color: var(--campus-leaf-deep);
  font-weight: 850;
}

.filter-title svg {
  width: 20px;
  height: 20px;
}

.hall-filter-panel label span {
  color: var(--campus-ink-soft);
  font-size: 0.8rem;
  font-weight: 800;
}

.hall-filter-panel select {
  min-height: 44px;
  width: 100%;
  padding: 0 12px;
  border: 1px solid var(--campus-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--campus-ink);
  font: inherit;
}

.hall-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hall-stats article {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 22px;
}

.hall-stats svg {
  grid-row: span 2;
  width: 34px;
  height: 34px;
  padding: 8px;
  border-radius: 14px;
  color: var(--campus-leaf);
  background: rgba(30, 125, 95, 0.1);
}

.hall-stats strong {
  color: var(--campus-ink);
  font-size: 1.35rem;
  line-height: 1;
}

.hall-stats span {
  color: var(--campus-ink-soft);
  font-weight: 800;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.task-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 332px;
  padding: 22px;
  border-radius: 26px;
  overflow: hidden;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.task-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 12%, rgba(205, 231, 219, 0.62), transparent 34%),
    radial-gradient(circle at 90% 84%, rgba(248, 223, 164, 0.18), transparent 26%);
  pointer-events: none;
}

.task-card > * {
  position: relative;
  z-index: 1;
}

.task-card-head,
.task-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.category-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 850;
}

.category-pill {
  color: var(--campus-leaf-deep);
  background: rgba(30, 125, 95, 0.1);
  border: 1px solid rgba(30, 125, 95, 0.16);
}

.status-pill {
  color: var(--campus-ink-soft);
  background: rgba(17, 32, 49, 0.06);
}

.status-pill.success {
  color: #166348;
  background: rgba(30, 125, 95, 0.12);
}

.status-pill.info {
  color: #315f83;
  background: rgba(124, 167, 204, 0.18);
}

.status-pill.done {
  color: #8a5b15;
  background: rgba(232, 168, 79, 0.16);
}

.task-card h2 {
  margin: 18px 0 0;
  color: var(--campus-ink);
  font-size: 1.24rem;
  line-height: 1.3;
  letter-spacing: 0;
}

.task-card p {
  margin: 12px 0 0;
  color: var(--campus-ink-soft);
  line-height: 1.72;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-meta {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.task-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--campus-ink-soft);
  font-size: 0.9rem;
  font-weight: 650;
}

.task-meta svg {
  width: 1em;
  height: 1em;
  color: rgba(82, 96, 113, 0.72);
}

.task-card-actions {
  margin-top: auto;
  padding-top: 18px;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--campus-leaf-deep);
  font-weight: 850;
  text-decoration: none;
}

.accept-button {
  min-height: 40px;
  padding-inline: 16px;
  box-shadow: none;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 18px 24px;
  border-radius: 24px;
  color: var(--campus-ink-soft);
  font-weight: 800;
}

.empty-state,
.error-message {
  padding: 28px;
  border-radius: 24px;
  color: var(--campus-ink-soft);
}

.error-message {
  color: #9f3a38;
  border-color: rgba(217, 127, 127, 0.28);
}

@media (hover: hover) and (pointer: fine) {
  .publish-button:hover,
  .hall-search button:hover,
  .filter-button:hover,
  .accept-button:hover {
    transform: translateY(-2px);
    background: var(--campus-leaf-deep);
    box-shadow: 0 18px 32px rgba(30, 125, 95, 0.22);
  }

  .task-card:hover {
    transform: translateY(-5px);
    border-color: rgba(30, 125, 95, 0.2);
    box-shadow: 0 24px 52px rgba(17, 32, 49, 0.13);
  }

  .task-card:hover .detail-link svg {
    transform: translateX(3px);
  }

  .detail-link svg {
    transition: transform 180ms ease;
  }
}

@media (prefers-reduced-motion: reduce) {
  .task-hall-page *,
  .task-hall-page *::before,
  .task-hall-page *::after {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}

@media (max-width: 1180px) {
  .hall-filter-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-title,
  .filter-button {
    grid-column: 1 / -1;
  }

  .cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hall-title,
  .hall-search,
  .hall-filter-panel,
  .hall-stats,
  .cards-grid {
    grid-template-columns: 1fr;
  }

  .hall-title,
  .hall-filter-panel,
  .task-card {
    border-radius: 22px;
    padding: 20px;
  }

  .hall-search {
    grid-template-columns: auto 1fr;
  }

  .hall-search button,
  .publish-button {
    width: 100%;
  }

  .hall-search button {
    grid-column: 1 / -1;
  }

  .pagination {
    flex-wrap: wrap;
  }
}
</style>
