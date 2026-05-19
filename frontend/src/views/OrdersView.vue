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
      <RouterLink v-for="order in page.records" :key="order.id" class="item-card" :to="`/orders/${order.id}`">
        <div class="item-title">
          <h2>{{ order.taskTitle }}</h2>
          <span :class="['tag', statusClass[order.status]]">{{ orderStatusText[order.status] }}</span>
        </div>
        <div class="meta-line">
          <span><UserRound class="meta-icon" aria-hidden="true" />发布者 {{ order.publisherNickname }}</span>
          <span><ClipboardList class="meta-icon" aria-hidden="true" />服务方 {{ order.serviceProviderNickname }}</span>
        </div>
        <p class="hint">
          <CalendarClock class="meta-icon" aria-hidden="true" />
          订单号 {{ order.id }} · {{ new Date(order.createdAt).toLocaleString() }}
        </p>
      </RouterLink>
    </div>
  </section>
</template>
