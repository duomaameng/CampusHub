<script setup lang="ts">
import {
  ArrowRight,
  Bookmark,
  CheckCircle2,
  Clock,
  Handshake,
  MapPin,
  PlusCircle,
  Search,
  Tag,
  Users
} from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { taskApi } from '@/services/api'
import { taskStatusText } from '@/types'
import type { PageData, TaskItem } from '@/types'

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

const categoryText: Record<string, string> = Object.fromEntries(categories.filter((item) => item.value).map((item) => [item.value, item.label]))
const rewardText: Record<string, string> = {
  CASH: '现金',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分意向'
}

const filters = reactive({
  category: '',
  campus: '',
  keyword: '',
  sort: 'latest'
})
const page = ref<PageData<TaskItem>>()
const loading = ref(false)
const error = ref('')

const openTaskCount = computed(() => page.value?.records.filter((task) => task.status === 'OPEN').length ?? 0)
const totalApplications = computed(() => page.value?.records.reduce((sum, task) => sum + task.applicationCount, 0) ?? 0)

async function loadTasks() {
  error.value = ''
  loading.value = true
  try {
    page.value = await taskApi.list({ ...filters, page: 1, size: 20 })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '任务加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>任务大厅</h1>
        <p>浏览公开需求，按分类、校区、关键词和截止时间筛选。</p>
      </div>
      <RouterLink class="button primary" to="/tasks/new">
        <PlusCircle class="button-icon" aria-hidden="true" />
        <span>发布需求</span>
      </RouterLink>
    </div>

    <div class="hero-strip">
      <div class="hero-copy">
        <span class="eyebrow">
          <Handshake class="eyebrow-icon" aria-hidden="true" />
          Live Board
        </span>
        <strong>浏览校园互助任务，找到适合你的机会。</strong>
        <span>筛选分类与校区，进入详情页查看完整信息并提交接单申请；发布者确认后即可开始协作。</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon">
          <Tag aria-hidden="true" />
        </span>
        <strong>{{ page?.total ?? 0 }}</strong>
        <span>全部任务</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon success">
          <CheckCircle2 aria-hidden="true" />
        </span>
        <strong>{{ openTaskCount }}</strong>
        <span>待接任务</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon amber">
          <Users aria-hidden="true" />
        </span>
        <strong>{{ totalApplications }}</strong>
        <span>申请总数</span>
      </div>
    </div>

    <form class="toolbar" @submit.prevent="loadTasks">
      <div class="field">
        <label for="category">分类</label>
        <select id="category" v-model="filters.category">
          <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </div>
      <div class="field">
        <label for="campus">校区</label>
        <select id="campus" v-model="filters.campus">
          <option value="">全部校区</option>
          <option value="仙林校区">仙林校区</option>
          <option value="鼓楼校区">鼓楼校区</option>
          <option value="浦口校区">浦口校区</option>
          <option value="苏州校区">苏州校区</option>
        </select>
      </div>
      <div class="field">
        <label for="keyword">
          <Search class="label-icon" aria-hidden="true" />
          关键词
        </label>
        <input id="keyword" v-model.trim="filters.keyword" type="search" placeholder="标题或描述" />
      </div>
      <div class="field">
        <label for="sort">排序</label>
        <select id="sort" v-model="filters.sort">
          <option value="latest">最新发布</option>
          <option value="deadline">截止时间</option>
        </select>
      </div>
      <div class="field">
        <label>&nbsp;</label>
        <button class="button secondary" type="submit">筛选</button>
      </div>
    </form>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载任务</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无符合条件的任务</div>

    <div v-else class="cards-grid">
      <RouterLink
        v-for="(task, index) in page.records"
        :key="task.id"
        class="item-card"
        :to="`/tasks/${task.id}`"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <h2>{{ task.title }}</h2>
          <span class="tag">{{ categoryText[task.category] }}</span>
        </div>
        <p>{{ task.description }}</p>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ rewardText[task.rewardType] }}</span>
          <span><RouterLink :to="{ name: 'user-public-profile', params: { id: task.publisherId } }">{{ task.publisherNickname }}</RouterLink></span>
          <span><Clock class="meta-icon" aria-hidden="true" />{{ new Date(task.deadline).toLocaleString() }} 截止</span>
        </div>
        <div class="meta-line">
          <span><Users class="meta-icon" aria-hidden="true" />申请 {{ task.applicationCount }}</span>
          <span><Bookmark class="meta-icon" aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
          <span :class="['tag', task.status === 'OPEN' ? 'success' : 'warning']">{{ taskStatusText[task.status] }}</span>
        </div>
        <span class="card-action">
          查看详情
          <ArrowRight class="meta-icon" aria-hidden="true" />
        </span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
section {
  --card-hover-lift: -6px;
}

.hero-strip {
  position: relative;
}

.hero-copy::before {
  animation: float 8s ease-in-out infinite;
}

.metric-card {
  padding: var(--space-5) var(--space-4);
}

.metric-card strong {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--text-primary), var(--text-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.metric-card span:last-child {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.toolbar {
  position: relative;
}

.toolbar .field select,
.toolbar .field input {
  padding: 10px 14px;
  font-weight: 500;
  background: var(--bg-surface);
  border: 1.5px solid var(--border-light);
  transition: all var(--transition-fast);
}

.toolbar .field select:hover,
.toolbar .field input:hover {
  border-color: var(--primary-300);
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
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
}

.cards-grid {
  position: relative;
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
  background: linear-gradient(90deg, var(--primary-400), var(--secondary-400), var(--accent-400));
  filter: none;
  opacity: 0;
  transform: none;
  transition: opacity var(--transition-base);
  background-size: 200% 100%;
}

.item-card:hover::after {
  opacity: 1;
  animation: shimmerCard 2s linear infinite;
}

.item-card .item-title {
  margin-bottom: 2px;
}

.item-card h2 {
  font-size: 15.5px;
  line-height: 1.35;
  color: var(--text-primary);
}

.item-card p {
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-card .tag {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 4px 12px;
  background: linear-gradient(135deg, var(--primary-50), var(--secondary-50));
  color: var(--primary-600);
  border: 1px solid var(--primary-100);
}

.item-card .meta-line {
  font-size: 12px;
  gap: var(--space-3);
}

.item-card .meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.item-card .meta-line a {
  color: var(--primary-600);
  font-weight: 600;
  transition: color var(--transition-fast);
}

.item-card .meta-line a:hover {
  color: var(--primary-700);
}

.item-card .card-action {
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.02em;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-light);
  color: var(--primary-600);
  transition: all var(--transition-fast);
}

.item-card:hover .card-action {
  color: var(--primary-700);
  gap: var(--space-3);
}

.card-action .meta-icon {
  transition: transform var(--transition-fast);
}

.item-card:hover .card-action .meta-icon {
  transform: translateX(4px);
}

.page-title .button.primary {
  padding: 11px 22px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.page-title .button.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.35);
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-tertiary);
  background:
    radial-gradient(circle at 50% 50%, rgba(99, 102, 241, 0.03), transparent 60%),
    var(--bg-surface);
  border: 2px dashed var(--border-medium);
  position: relative;
}

.error-message {
  margin-bottom: var(--space-4);
  padding: 12px 16px;
  font-weight: 600;
  border: 1.5px solid rgba(239, 68, 68, 0.2);
}

@media (max-width: 1024px) {
  .hero-strip {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .hero-strip {
    grid-template-columns: 1fr;
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
