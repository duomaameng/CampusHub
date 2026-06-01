<script setup lang="ts">
import { CalendarClock, ClipboardList, UserRound } from '@lucide/vue'
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { orderApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { orderStatusText } from '@/types'
import type { OrderItem, OrderStatus, PageData, TaskItem } from '@/types'

type OrderRoleFilter = '' | 'PUBLISHER' | 'PROVIDER'

const auth = useAuthStore()

const roleOptions: Array<{ value: OrderRoleFilter; label: string }> = [
  { value: '', label: '全部订单' },
  { value: 'PUBLISHER', label: '我的发布' },
  { value: 'PROVIDER', label: '我的接单' }
]

const filters = reactive<{
  role: OrderRoleFilter
  status: ''
  keyword: string
}>({
  role: '',
  status: '',
  keyword: ''
})

const page = ref<PageData<OrderItem>>()
const missingPublishedTasks = ref<TaskItem[]>([])
const loading = ref(false)
const error = ref('')

const statusClass: Record<string, string> = {
  IN_PROGRESS: 'success',
  PENDING_COMPLETION: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  DISPUTE: 'warning',
  REVIEWED: 'success',
  PENDING_CONFIRM: 'info'
}

async function loadOrders() {
  error.value = ''
  loading.value = true
  try {
    page.value = await orderApi.list({
      page: 1,
      size: 20,
      role: filters.role || undefined,
      status: (filters.status || undefined) as OrderStatus | undefined,
      keyword: filters.keyword || undefined
    })
    missingPublishedTasks.value = await loadMissingPublishedTasks(page.value.records)
  } catch (err) {
    missingPublishedTasks.value = []
    error.value = err instanceof Error ? err.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMissingPublishedTasks(orders: OrderItem[]) {
  if (!auth.user || filters.role === 'PROVIDER' || filters.status) return []

  const linkedTaskIds = new Set(orders.map((order) => order.taskId))
  const result = await taskApi.list({
    page: 1,
    size: 100,
    keyword: filters.keyword || undefined,
    sort: 'newest'
  })

  return result.records.filter((task) => task.publisherId === auth.user?.id && !linkedTaskIds.has(task.id))
}

function setRoleFilter(role: OrderRoleFilter) {
  if (filters.role === role) return
  filters.role = role
  void loadOrders()
}

function relationLabel(order: OrderItem) {
  if (order.publisherId === auth.user?.id) return '我发布'
  if (order.serviceProviderId === auth.user?.id) return '我接单'
  return '相关订单'
}

function relationClass(order: OrderItem) {
  if (order.publisherId === auth.user?.id) return 'publisher'
  if (order.serviceProviderId === auth.user?.id) return 'provider'
  return ''
}

onMounted(loadOrders)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>我的订单</h1>
      </div>
    </div>

    <div class="role-tabs" aria-label="订单角色筛选">
      <button
        v-for="option in roleOptions"
        :key="option.value || 'ALL'"
        type="button"
        :class="['role-tab', { active: filters.role === option.value }]"
        @click="setRoleFilter(option.value)"
      >
        <strong>{{ option.label }}</strong>
      </button>
    </div>

    <form class="toolbar" @submit.prevent="loadOrders">
      <div class="field">
        <label for="status">状态</label>
        <select id="status" v-model="filters.status">
          <option value="">全部状态</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="PENDING_COMPLETION">待确认完成</option>
          <option value="COMPLETED">已完成</option>
          <option value="REVIEWED">已评价</option>
          <option value="CANCELLED">已取消</option>
          <option value="DISPUTE">争议处理中</option>
          <option value="PENDING_CONFIRM">待接单</option>
        </select>
      </div>
      <div class="field">
        <label for="keyword">关键词</label>
        <input id="keyword" v-model.trim="filters.keyword" type="search" placeholder="搜索任务标题" />
      </div>
      <div class="field">
        <label>&nbsp;</label>
        <button class="button secondary" type="submit">筛选</button>
      </div>
    </form>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载订单</div>
    <div v-else-if="!page?.records.length && !missingPublishedTasks.length" class="empty-state">暂无符合条件的订单</div>

    <div v-else class="cards-grid">
      <RouterLink
        v-for="(order, index) in page?.records || []"
        :key="order.id"
        class="item-card"
        :to="`/orders/${order.id}`"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <div>
            <span :class="['relation-pill', relationClass(order)]">{{ relationLabel(order) }}</span>
            <h2>{{ order.taskTitle }}</h2>
          </div>
          <span :class="['tag', statusClass[order.status]]">{{ orderStatusText[order.status] }}</span>
        </div>
        <div class="meta-line">
          <span>
            <UserRound class="meta-icon" aria-hidden="true" />
            发布者
            <RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">
              {{ order.publisherNickname }}
            </RouterLink>
          </span>
          <span>
            <ClipboardList class="meta-icon" aria-hidden="true" />
            服务方
            <RouterLink :to="{ name: 'user-public-profile', params: { id: order.serviceProviderId } }">
              {{ order.serviceProviderNickname }}
            </RouterLink>
          </span>
        </div>
        <p class="hint">
          <CalendarClock class="meta-icon" aria-hidden="true" />
          订单号 {{ order.id }} · {{ new Date(order.createdAt).toLocaleString() }}
        </p>
      </RouterLink>
      <RouterLink
        v-for="(task, index) in missingPublishedTasks"
        :key="`task-${task.id}`"
        class="item-card"
        :to="`/tasks/${task.id}`"
        :style="{ '--i': (page?.records.length || 0) + index }"
      >
        <div class="item-title">
          <div>
            <span class="relation-pill publisher">我发布</span>
            <h2>{{ task.title }}</h2>
          </div>
          <span class="tag warning">待接单</span>
        </div>
        <div class="meta-line">
          <span>
            <UserRound class="meta-icon" aria-hidden="true" />
            发布者
            <RouterLink :to="{ name: 'user-public-profile', params: { id: task.publisherId } }">
              {{ task.publisherNickname }}
            </RouterLink>
          </span>
          <span>
            <ClipboardList class="meta-icon" aria-hidden="true" />
            尚未接单
          </span>
        </div>
        <p class="hint">
          <CalendarClock class="meta-icon" aria-hidden="true" />
          任务号 {{ task.id }} · {{ new Date(task.createdAt).toLocaleString() }}
        </p>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.role-tab {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border: 1.5px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--glass-panel-bg);
  color: var(--text-secondary);
  text-align: left;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);
}

.role-tab strong {
  font-size: 13px;
  color: var(--text-primary);
}

.role-tab:hover {
  border-color: rgba(99, 102, 241, 0.24);
  transform: translateY(-1px);
}

.role-tab.active {
  border-color: rgba(99, 102, 241, 0.42);
  background:
    radial-gradient(90% 120% at 100% 0%, rgba(99, 102, 241, 0.1), transparent 58%),
    var(--glass-panel-bg);
  box-shadow: 0 10px 24px rgba(70, 82, 140, 0.11), inset 0 0 0 1px rgba(99, 102, 241, 0.08);
}

.role-tab.active strong {
  color: var(--primary-600);
}

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

.item-title {
  align-items: flex-start;
}

.item-card h2 {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.relation-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: var(--radius-full);
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-tertiary);
  background: rgba(138, 143, 168, 0.08);
  border: 1px solid rgba(138, 143, 168, 0.14);
}

.relation-pill.publisher {
  color: var(--primary-600);
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.16);
}

.relation-pill.provider {
  color: var(--success);
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.16);
}

.item-card .tag {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 4px 12px;
}

.item-card .meta-line {
  font-size: 12.5px;
  gap: var(--space-3);
}

.item-card .meta-line a {
  color: var(--primary-600);
  font-weight: 600;
}

.item-card .meta-line a:hover {
  color: var(--primary-700);
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 600;
  border: 2px dashed var(--border-medium);
}

.error-message {
  margin-bottom: var(--space-4);
  padding: 12px 16px;
  font-weight: 600;
  border: 1.5px solid rgba(239, 68, 68, 0.2);
}

@media (max-width: 768px) {
  .role-tabs,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
