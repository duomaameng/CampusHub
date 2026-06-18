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
  UserRound,
  Users
} from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
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

const categoryText: Record<string, string> = Object.fromEntries(categories.filter((item) => item.value).map((item) => [item.value, item.label]))
const rewardText: Record<string, string> = {
  CASH: '现金',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分意向'
}
const statusTagClass: Record<TaskStatus, string> = {
  OPEN: 'success',
  IN_PROGRESS: 'info',
  COMPLETED: 'warning',
  CANCELLED: 'danger',
  EXPIRED: 'danger'
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
          任务大厅
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
          <span class="tag category-tag">{{ categoryText[task.category] }}</span>
        </div>
        <p>{{ task.description }}</p>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ rewardText[task.rewardType] }}</span>
          <span v-if="task.anonymous"><UserRound class="meta-icon" aria-hidden="true" />匿名用户</span>
          <span v-else><RouterLink :to="{ name: 'user-public-profile', params: { id: task.publisherId } }">{{ task.publisherNickname }}</RouterLink></span>
          <span><Clock class="meta-icon" aria-hidden="true" />{{ new Date(task.deadline).toLocaleString() }} 截止</span>
        </div>
        <div class="meta-line">
          <span><Users class="meta-icon" aria-hidden="true" />申请 {{ task.applicationCount }}</span>
          <span><Bookmark class="meta-icon" aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
          <span :class="['tag', 'status-tag', statusTagClass[task.status]]">{{ taskStatusText[task.status] }}</span>
        </div>
        <span class="card-action">
          查看详情
          <ArrowRight class="meta-icon" aria-hidden="true" />
        </span>
        <img v-if="task.imageUrls && task.imageUrls.length" :src="task.imageUrls[0]" alt="" class="card-image" />
        <span v-else class="task-visual" aria-hidden="true">
          <span class="visual-window" />
          <span class="visual-dot" />
          <span class="visual-line visual-line-1" />
          <span class="visual-line visual-line-2" />
        </span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
section {
  --card-hover-lift: -6px;
  --task-green: #ffb454;
  --task-dark: #191a23;
  --task-grey: #f3f3f3;
}

.page-title {
  align-items: flex-start;
  margin-bottom: 28px;
}

.page-title h1 {
  width: max-content;
  padding: 4px 12px;
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

.hero-strip {
  position: relative;
  align-items: stretch;
  grid-template-columns: minmax(320px, 1.2fr) repeat(3, minmax(100px, 0.5fr));
  gap: 14px;
  margin-bottom: 28px;
  background: transparent;
  border: 0;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  overflow: visible;
}

.hero-copy {
  min-height: 90px;
  padding: 18px 80px 16px 22px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: var(--task-dark);
  box-shadow: none;
  overflow: hidden;
}

.hero-copy .eyebrow {
  width: max-content;
  max-width: 100%;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--task-green);
  color: #000000;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.hero-copy .eyebrow-icon {
  width: 14px;
  height: 14px;
}

.hero-copy strong {
  max-width: 320px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
  line-height: 1.2;
  letter-spacing: 0;
}

.hero-copy span:not(.eyebrow) {
  max-width: 340px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 11px;
  font-weight: 700;
  line-height: 1.65;
}

.hero-copy::before {
  content: '';
  position: absolute;
  right: 18px;
  top: 24px;
  width: 72px;
  height: 72px;
  border: 2px solid #000000;
  border-radius: 50%;
  background: var(--task-green);
  opacity: 1;
  filter: none;
  animation: none;
}

.hero-copy::after {
  content: '';
  position: absolute;
  right: 48px;
  bottom: 16px;
  width: 48px;
  height: 38px;
  border: 2px solid #000000;
  border-radius: 16px;
  background: #ffffff;
  transform: rotate(-11deg);
  opacity: 1;
  filter: none;
}

.metric-card {
  min-height: 90px;
  padding: 14px 12px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: var(--task-grey);
  box-shadow: none;
  gap: 8px;
  overflow: hidden;
}

.metric-card::before {
  display: none;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: none;
}

.metric-card .metric-icon {
  width: 30px;
  height: 30px;
  border: 2px solid #000000;
  border-radius: 10px;
  background: var(--task-green);
  color: #000000;
  box-shadow: none;
}

.metric-card .metric-icon.success,
.metric-card .metric-icon.amber {
  background: var(--task-green);
  color: #000000;
}

.metric-card strong {
  color: #000000;
  font-size: 20px;
  font-weight: 900;
  background: none;
  -webkit-background-clip: border-box;
  -webkit-text-fill-color: #000000;
  background-clip: text;
}

.metric-card span:last-child {
  color: #6f7485;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.toolbar {
  position: relative;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) minmax(150px, 0.85fr);
  gap: 22px;
  padding: 28px 30px;
  border: 2px solid #000000;
  border-radius: 28px;
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
  border-color: #000000;
  background: #e8f5ec;
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

.toolbar .button.secondary {
  width: 100%;
  min-height: 50px;
  padding: 10px 28px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
  background: var(--task-dark);
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: none;
}

.toolbar .button.secondary:hover {
  color: #000000;
  background: var(--task-green);
  box-shadow: none;
  transform: translateY(-2px);
}

.cards-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  grid-auto-rows: auto;
  gap: 22px;
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

.item-card::after {
  display: none;
}

.item-card:hover::after {
  display: none;
}

.item-card .item-title {
  max-width: 100%;
  margin-bottom: 6px;
}

.item-card h2 {
  max-width: calc(100% - 120px);
  font-size: 21px;
  font-weight: 900;
  line-height: 1.18;
}

.item-card p {
  max-width: calc(100% - 170px);
  font-size: 13px;
  line-height: 1.5;
  color: #343743;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-card .tag {
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: 0.03em;
  padding: 4px 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.item-card .category-tag {
  position: absolute;
  right: 30px;
  bottom: 28px;
  z-index: 3;
  background: #ffffff;
  color: #000000;
  border: 2px solid #000000;
  min-width: max-content;
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

.item-card .meta-line {
  font-size: 11.5px;
  gap: 10px;
  max-width: calc(100% - 170px);
}

.item-card .meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-tertiary);
  font-weight: 700;
}

.item-card .meta-line a {
  color: #000000;
  font-weight: 900;
  transition: color var(--transition-fast);
}

.item-card .meta-line a:hover {
  color: #2d5a3d;
}

.item-card .card-action {
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.02em;
  width: max-content;
  margin-top: auto;
  padding-top: 12px;
  border-top: 2px solid #000000;
  color: #000000;
  transition: all var(--transition-fast);
}

.item-card:hover .card-action {
  color: #000000;
  gap: var(--space-3);
}

.item-card:hover .card-action .meta-icon {
  transition: transform var(--transition-fast);
  transform: translateX(4px);
}

.task-visual {
  position: absolute;
  right: 28px;
  top: 50%;
  width: 124px;
  height: 110px;
  transform: translateY(-50%);
  pointer-events: none;
}

.visual-window {
  position: absolute;
  inset: 16px 6px 8px 16px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: #ffffff;
  transform: rotate(-6deg);
}

.visual-window::before {
  content: '';
  position: absolute;
  left: 16px;
  top: 16px;
  width: 40px;
  height: 10px;
  border-radius: 999px;
  background: #ffb454;
}

.visual-dot {
  position: absolute;
  right: 8px;
  top: 0;
  width: 46px;
  height: 46px;
  border: 2px solid #000000;
  border-radius: 50%;
  background: #ffb454;
}

.visual-line {
  position: absolute;
  left: 0;
  height: 10px;
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
}

.visual-line-1 {
  right: 48px;
  bottom: 34px;
}

.visual-line-2 {
  right: 70px;
  bottom: 14px;
}

.card-action .meta-icon {
  transition: transform var(--transition-fast);
}

.item-card:hover .card-action .meta-icon {
  transform: translateX(4px);
}

.page-title .button.primary {
  padding: 12px 24px;
  color: #ffffff;
  font-weight: 900;
  letter-spacing: 0;
  background: var(--task-dark);
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: none;
}

.page-title .button.primary:hover {
  transform: translateY(-2px);
  color: #000000;
  background: var(--task-green);
  box-shadow: none;
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

  .item-card {
    padding: var(--space-5);
  }

.card-image {
  position: absolute;
  right: 28px;
  top: 28px;
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

.task-visual {
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

  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>




