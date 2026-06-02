<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { notificationApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { NotificationItem, PageData } from '@/types'

const auth = useAuthStore()
const page = ref<PageData<NotificationItem>>()
const error = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await notificationApi.list({ page: 1, size: 20 })
    await auth.refreshUnread()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '通知加载失败'
  } finally {
    loading.value = false
  }
}

async function markRead(notificationId: number) {
  await notificationApi.markRead(notificationId)
  await load()
}

async function readAll() {
  await notificationApi.readAll()
  await load()
}

async function deleteOne(notificationId: number) {
  await notificationApi.delete(notificationId)
  await load()
}

async function deleteAllRead() {
  await notificationApi.deleteRead()
  await load()
}

function targetLink(item: NotificationItem) {
  return item.targetType === 'ORDER' ? `/orders/${item.targetId}` : `/tasks/${item.targetId}`
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>通知中心</h1>
        <p>接单申请、订单状态、评价邀请和举报结果会集中在这里。</p>
      </div>
      <div class="page-actions">
        <button class="button secondary" type="button" @click="readAll">全部已读</button>
        <button class="button secondary" type="button" @click="deleteAllRead">删除已读通知</button>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载通知</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无通知</div>

    <div v-else class="grid">
      <article
        v-for="(item, index) in page.records"
        :key="item.id"
        class="item-card notification-item"
        :class="{ unread: !item.read }"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <h2>{{ item.title }}</h2>
          <span :class="['tag', item.read ? '' : 'warning']">{{ item.read ? '已读' : '未读' }}</span>
        </div>
        <p>{{ item.content }}</p>
        <div class="actions">
          <RouterLink class="button ghost" :to="targetLink(item)">查看</RouterLink>
          <button class="button secondary" type="button" :disabled="item.read" @click="markRead(item.id)">标记已读</button>
          <button class="button danger-outline" type="button" @click="deleteOne(item.id)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.page-title .button.secondary {
  padding: 10px 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  font-size: 11px;
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  color: white;
  border: none;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.page-title .button.secondary:hover {
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
  transform: translateY(-1px);
}

.page-actions {
  display: flex;
  gap: var(--space-3);
}

.notification-item {
  padding: var(--space-5);
  border: 1.5px solid var(--border-light);
  position: relative;
  transition: all var(--transition-base);
}

.notification-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--primary-400), var(--secondary-500));
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.notification-item.unread {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.02), var(--bg-surface));
  border-left: 3px solid var(--primary-500);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.06);
}

.notification-item.unread::before {
  opacity: 1;
}

.notification-item:hover {
  border-color: rgba(99, 102, 241, 0.15);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
  transform: translateX(2px);
}

.notification-item h2 {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.notification-item p {
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.notification-item .tag {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 4px 12px;
}

.notification-item .tag.warning {
  background: linear-gradient(135deg, var(--warning-bg), rgba(245, 158, 11, 0.1));
  color: var(--warning);
  border-color: rgba(245, 158, 11, 0.25);
  animation: pulseGlow 2s ease-in-out infinite;
}

.notification-item .button.ghost {
  padding: 8px 16px;
  font-weight: 600;
}

.notification-item .button.ghost:hover {
  color: var(--primary-600);
  background: var(--primary-50);
}

.notification-item .button.secondary {
  padding: 8px 16px;
  font-weight: 600;
  border: 1.5px solid var(--border-light);
}

.notification-item .button.secondary:hover:not(:disabled) {
  border-color: var(--primary-400);
  background: var(--primary-50);
  color: var(--primary-700);
}

.notification-item .button.danger-outline {
  padding: 8px 16px;
  font-weight: 600;
  border: 1.5px solid rgba(239, 68, 68, 0.3);
  background: transparent;
  color: var(--danger);
}

.notification-item .button.danger-outline:hover {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.5);
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

.grid {
  gap: var(--space-4);
}

@media (max-width: 768px) {
  .notification-item {
    padding: var(--space-4);
  }
}
</style>
