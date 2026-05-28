<script setup lang="ts">
import { CalendarClock, ClipboardList, UserRound } from '@lucide/vue'
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { orderApi } from '@/services/api'
import { orderStatusText } from '@/types'
import type { OrderItem, OrderStatus, PageData } from '@/types'

const filters = reactive({
  role: '',
  status: '',
  keyword: ''
})
const page = ref<PageData<OrderItem>>()
const loading = ref(false)
const error = ref('')

const statusClass: Record<string, string> = {
  IN_PROGRESS: 'success',
  PENDING_COMPLETION: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  DISPUTE: 'warning',
  REVIEWED: 'success'
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
  } catch (err) {
    error.value = err instanceof Error ? err.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>我的订单</h1>
        <p>查看我发布的订单、我接单的订单和订单状态。</p>
      </div>
    </div>

    <form class="toolbar" @submit.prevent="loadOrders">
      <div class="field">
        <label for="role">角色</label>
        <select id="role" v-model="filters.role">
          <option value="">全部</option>
          <option value="PUBLISHER">我发布的</option>
          <option value="PROVIDER">我接单的</option>
        </select>
      </div>
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
        </select>
      </div>
      <div class="field">
        <label for="keyword">关键词</label>
        <input id="keyword" v-model.trim="filters.keyword" type="search" />
      </div>
      <div class="field">
        <label>&nbsp;</label>
        <button class="button secondary" type="submit">筛选</button>
      </div>
    </form>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载订单</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无订单</div>

    <div v-else class="cards-grid">
      <RouterLink
        v-for="(order, index) in page.records"
        :key="order.id"
        class="item-card"
        :to="`/orders/${order.id}`"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <h2>{{ order.taskTitle }}</h2>
          <span :class="['tag', statusClass[order.status]]">{{ orderStatusText[order.status] }}</span>
        </div>
        <div class="meta-line">
          <span><UserRound class="meta-icon" aria-hidden="true" />发布者 <RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">{{ order.publisherNickname }}</RouterLink></span>
          <span><ClipboardList class="meta-icon" aria-hidden="true" />服务方 <RouterLink :to="{ name: 'user-public-profile', params: { id: order.serviceProviderId } }">{{ order.serviceProviderNickname }}</RouterLink></span>
        </div>
        <p class="hint">
          <CalendarClock class="meta-icon" aria-hidden="true" />
          订单号 {{ order.id }} · {{ new Date(order.createdAt).toLocaleString() }}
        </p>
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
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-400), var(--secondary-400));
  opacity: 0;
  transition: opacity var(--transition-base);
}

.item-card:hover::after {
  opacity: 1;
}

.item-card h2 {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
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
  .toolbar {
    grid-template-columns: 1fr;
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
