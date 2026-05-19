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
          CampusHub Live Board
        </span>
        <strong>把校园里的零散互助请求收拢到一条清晰流程里。</strong>
        <span>先筛选合适任务，再进入详情页申请接单；发布者确认后即可进入订单协作。</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon">
          <Tag aria-hidden="true" />
        </span>
        <strong>{{ page?.total ?? 0 }}</strong>
        <span>当前需求</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon success">
          <CheckCircle2 aria-hidden="true" />
        </span>
        <strong>{{ openTaskCount }}</strong>
        <span>可申请任务</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon amber">
          <Users aria-hidden="true" />
        </span>
        <strong>{{ totalApplications }}</strong>
        <span>接单申请</span>
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
      <RouterLink v-for="task in page.records" :key="task.id" class="item-card" :to="`/tasks/${task.id}`">
        <div class="item-title">
          <h2>{{ task.title }}</h2>
          <span class="tag">{{ categoryText[task.category] }}</span>
        </div>
        <p>{{ task.description }}</p>
        <div class="meta-line">
          <span><MapPin class="meta-icon" aria-hidden="true" />{{ task.campus }}</span>
          <span><Tag class="meta-icon" aria-hidden="true" />{{ rewardText[task.rewardType] }}</span>
          <span>{{ task.publisherNickname }}</span>
          <span><Clock class="meta-icon" aria-hidden="true" />{{ new Date(task.deadline).toLocaleString() }} 截止</span>
        </div>
        <div class="meta-line">
          <span><Users class="meta-icon" aria-hidden="true" />申请 {{ task.applicationCount }}</span>
          <span><Bookmark class="meta-icon" aria-hidden="true" />收藏 {{ task.favoriteCount }}</span>
          <span :class="['tag', task.status === 'OPEN' ? 'success' : 'warning']">{{ task.status }}</span>
        </div>
        <span class="card-action">
          查看详情
          <ArrowRight class="meta-icon" aria-hidden="true" />
        </span>
      </RouterLink>
    </div>
  </section>
</template>
