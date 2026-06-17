<script setup lang="ts">
import { CalendarClock, ClipboardList, UserRound } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { orderApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { orderStatusText } from '@/types'
import type { OrderItem, OrderStatus, PageData, TaskItem } from '@/types'

type OrderRoleFilter = '' | 'PUBLISHER' | 'PROVIDER'
type OrderListCard = {
  key: string
  route: string
  relationLabel: string
  relationClass: string
  title: string
  status: OrderStatus
  publisherId: number
  publisherNickname: string
  serviceProviderId?: number | null
  serviceProviderNickname?: string
  serviceProviderHint: string
  numberLabel: string
  createdAt: string
}

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
  IN_PROGRESS: 'info',
  PENDING_COMPLETION: 'warning',
  COMPLETED: 'warning',
  TIMEOUT: 'danger',
  DISPUTE: 'warning',
  REVIEWED: 'warning',
  PENDING_CONFIRM: 'success'
}

const statusRank: Record<OrderStatus, number> = {
  IN_PROGRESS: 0,
  PENDING_COMPLETION: 0,
  PENDING_CONFIRM: 1,
  DISPUTE: 1,
  COMPLETED: 2,
  REVIEWED: 2,
  TIMEOUT: 3,
  CANCELLED: 3
}

const orderCards = computed<OrderListCard[]>(() => {
  const orderItems = (page.value?.records || []).map<OrderListCard>((order) => ({
    key: `order-${order.id}`,
    route: `/orders/${order.id}`,
    relationLabel: relationLabel(order),
    relationClass: relationClass(order),
    title: order.taskTitle,
    status: order.status,
    publisherId: order.publisherId,
    publisherNickname: order.publisherNickname,
    serviceProviderId: order.serviceProviderId,
    serviceProviderNickname: order.serviceProviderNickname,
    serviceProviderHint: '暂无服务方',
    numberLabel: `订单号 ${order.id}`,
    createdAt: order.createdAt
  }))

  const taskItems = missingPublishedTasks.value.map<OrderListCard>((task) => ({
    key: `task-${task.id}`,
    route: `/tasks/${task.id}`,
    relationLabel: '我发布',
    relationClass: 'publisher',
    title: task.title,
    status: 'PENDING_CONFIRM',
    publisherId: task.publisherId,
    publisherNickname: task.publisherNickname,
    serviceProviderHint: '尚未接单',
    numberLabel: `任务号 ${task.id}`,
    createdAt: task.createdAt
  }))

  return [...orderItems, ...taskItems].sort(compareOrderCards)
})

async function loadOrders() {
  error.value = ''
  loading.value = true
  try {
    const result = await orderApi.list({
      page: 1,
      size: 20,
      role: filters.role || undefined,
      status: (filters.status || undefined) as OrderStatus | undefined,
      keyword: filters.keyword || undefined
    })
    const records = result.records.filter(shouldShowOrder).sort(compareOrders)
    page.value = { ...result, total: records.length, records }
    missingPublishedTasks.value = await loadMissingPublishedTasks(page.value.records)
  } catch (err) {
    missingPublishedTasks.value = []
    error.value = err instanceof Error ? err.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

function shouldShowOrder(order: OrderItem) {
  if (order.status === 'CANCELLED') return false
  if (order.status !== 'PENDING_CONFIRM') return true
  return order.publisherId === auth.user?.id
}

function compareOrders(a: OrderItem, b: OrderItem) {
  return compareByStatusAndTime(a.status, a.createdAt, b.status, b.createdAt)
}

function compareOrderCards(a: OrderListCard, b: OrderListCard) {
  return compareByStatusAndTime(a.status, a.createdAt, b.status, b.createdAt)
}

function compareByStatusAndTime(aStatus: OrderStatus, aCreatedAt: string, bStatus: OrderStatus, bCreatedAt: string) {
  const rankDiff = statusRank[aStatus] - statusRank[bStatus]
  if (rankDiff !== 0) return rankDiff
  return new Date(bCreatedAt).getTime() - new Date(aCreatedAt).getTime()
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
  if (order.status !== 'PENDING_CONFIRM' && order.serviceProviderId === auth.user?.id) return '我接单'
  return '相关订单'
}

function relationClass(order: OrderItem) {
  if (order.publisherId === auth.user?.id) return 'publisher'
  if (order.status !== 'PENDING_CONFIRM' && order.serviceProviderId === auth.user?.id) return 'provider'
  return ''
}

onMounted(loadOrders)
</script>

<template>
  <section class="orders-view">
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
          <option value="TIMEOUT">已超时</option>
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
    <div v-else-if="!orderCards.length" class="empty-state">暂无符合条件的订单</div>

    <div v-else class="cards-grid">
      <RouterLink
        v-for="(card, index) in orderCards"
        :key="card.key"
        class="item-card"
        :to="card.route"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <div>
            <span :class="['relation-pill', card.relationClass]">{{ card.relationLabel }}</span>
            <h2>{{ card.title }}</h2>
          </div>
          <span :class="['tag', statusClass[card.status]]">{{ orderStatusText[card.status] }}</span>
        </div>
        <div class="meta-line">
          <span>
            <UserRound class="meta-icon" aria-hidden="true" />
            发布者
            <RouterLink :to="{ name: 'user-public-profile', params: { id: card.publisherId } }">
              {{ card.publisherNickname }}
            </RouterLink>
          </span>
          <span>
            <ClipboardList class="meta-icon" aria-hidden="true" />
            服务方
            <span v-if="card.status === 'PENDING_CONFIRM' || !card.serviceProviderId" class="hint">{{ card.serviceProviderHint }}</span>
            <RouterLink v-else :to="{ name: 'user-public-profile', params: { id: card.serviceProviderId } }">
              {{ card.serviceProviderNickname }}
            </RouterLink>
          </span>
        </div>
        <p class="hint">
          <CalendarClock class="meta-icon" aria-hidden="true" />
          {{ card.numberLabel }} · {{ new Date(card.createdAt).toLocaleString() }}
        </p>
        <span class="order-card-visual" aria-hidden="true">
          <span class="visual-dot" />
        </span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.orders-view {
  --order-green: #b9ff66;
  --order-dark: #191a23;
  --order-grey: #f3f3f3;
}

.page-title {
  margin-bottom: 22px;
}

.page-title h1 {
  width: max-content;
  padding: 5px 14px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: var(--order-green);
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
  box-shadow: 0 4px 0 #000000;
}

.orders-view .cards-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  grid-auto-rows: auto;
  gap: 22px;
}

.orders-view .cards-grid .item-card:nth-child(3n + 1) {
  grid-row: span 2;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

.role-tab {
  display: flex;
  align-items: center;
  min-height: 72px;
  padding: 16px 20px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: #ffffff;
  color: #000000;
  text-align: left;
  box-shadow: 0 5px 0 #000000;
  transition: all var(--transition-fast);
}

.role-tab strong {
  font-size: 17px;
  color: #000000;
  font-weight: 900;
}

.role-tab:hover {
  background: #f8ffe8;
  transform: translateY(-2px);
}

.role-tab.active {
  border-color: #000000;
  background: var(--order-green);
  box-shadow: 0 5px 0 #000000;
}

.role-tab.active strong {
  color: #000000;
}

.toolbar {
  position: relative;
  grid-template-columns: minmax(180px, 0.8fr) minmax(240px, 1fr) minmax(150px, 0.55fr);
  gap: 22px;
  margin-bottom: 28px;
  padding: 26px 28px;
  border: 2px solid #000000;
  border-radius: 26px;
  background: #ffffff;
  box-shadow: 0 6px 0 #000000;
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
  background: #f8ffe8;
}

.toolbar .field select:focus,
.toolbar .field input:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(185, 255, 102, 0.48);
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
  padding: 10px 24px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
  background: var(--order-dark);
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: 0 4px 0 #000000;
}

.toolbar .button.secondary:hover {
  color: #000000;
  background: var(--order-green);
  box-shadow: 0 5px 0 #000000;
  transform: translateY(-2px);
}

.item-card {
  padding: 22px 130px 22px 26px;
  border: 2px solid #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 5px 0 #000000;
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
  background: var(--order-green);
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

.item-card .meta-line,
.item-card > .hint {
  position: relative;
  z-index: 1;
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
  background: #ffffff;
}

.relation-pill.publisher {
  color: #000000;
  background: var(--order-green);
  border-color: #000000;
}

.relation-pill.provider {
  color: #000000;
  background: #ffffff;
  border-color: #000000;
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
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  padding: 5px 12px;
  box-shadow: none;
}

.item-card .meta-line {
  max-width: 620px;
  font-size: 12.5px;
  gap: 12px;
}

.item-card .meta-line a {
  color: #000000;
  font-weight: 900;
}

.item-card .meta-line a:hover {
  color: #365600;
}

.item-card .hint {
  max-width: 620px;
  color: #6f7485;
  font-weight: 800;
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 800;
  border: 2px dashed #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 5px 0 #000000;
}

.error-message {
  margin-bottom: var(--space-4);
  padding: 12px 16px;
  font-weight: 800;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
}

@media (max-width: 768px) {
  .orders-view .cards-grid {
    grid-template-columns: 1fr;
  }

  .orders-view .cards-grid .item-card:nth-child(3n + 1) {
    grid-row: span 1;
  }

  .role-tabs,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }

  .item-card {
    min-height: auto;
    padding: var(--space-5);
  }

  .item-card::after {
    display: none;
  }

  .order-card-visual {
    display: none;
  }
}
</style>
