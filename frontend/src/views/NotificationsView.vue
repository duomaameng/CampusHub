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
  <section class="notifications-view">
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
.notifications-view {
  --notice-green: #ffb454;
  --notice-dark: #191a23;
  --notice-grey: #f3f3f3;
  --notice-line: #000000;
}

.notifications-view :deep(.page-title) {
  padding: 28px 32px;
  border: 2px solid var(--notice-line);
  border-radius: 28px;
  background:
    radial-gradient(circle at 94% 10%, rgba(255, 180, 84, 0.82) 0 56px, transparent 58px),
    #ffffff;
  box-shadow: none;
}

.notifications-view :deep(.page-title h1) {
  width: max-content;
  margin-bottom: 10px;
  padding: 5px 10px;
  border-radius: 24px;
  border: 2px solid #000000;
  background: transparent;
  background-clip: border-box;
  -webkit-background-clip: border-box;
  color: #000000;
  -webkit-text-fill-color: #000000;
  font-size: 34px;
  line-height: 1.12;
  letter-spacing: 0;
  box-shadow: none;
}

.notifications-view :deep(.page-title p) {
  color: #2b2d35;
  font-size: 16px;
  font-weight: 700;
}

.page-title .button.secondary {
  padding: 12px 18px;
  border: 2px solid var(--notice-line);
  border-radius: 14px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
  font-size: 14px;
  background: #ffffff;
  color: #000000;
  box-shadow: none;
}

.page-title .button.secondary:hover {
  background: var(--notice-green);
  box-shadow: none;
  transform: translateY(-1px);
}

.page-actions {
  display: flex;
  gap: var(--space-3);
}

.notification-item {
  gap: 8px;
  padding: 12px 16px;
  border: 2px solid var(--notice-line);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: none;
  position: relative;
  transition: transform var(--transition-base), box-shadow var(--transition-base), background var(--transition-base);
}

.notification-item::before,
.notification-item::after {
  display: none;
}

.notification-item.unread {
  background: linear-gradient(135deg, #ffffff 0%, #fff3df 100%);
  box-shadow: none;
}

.notification-item:hover {
  border-color: var(--notice-line);
  box-shadow: none;
  transform: translateY(-1px);
}

.notification-item h2 {
  max-width: 100%;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.25;
}

.notification-item p {
  margin: 2px 0 8px;
  font-size: 13.5px;
  line-height: 1.42;
  color: #343743;
}

.notification-item .tag {
  display: none;
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
  min-height: 38px;
  padding: 7px 14px;
  border: 2px solid var(--notice-line);
  border-radius: 14px;
  background: var(--notice-dark);
  color: #ffffff;
  font-weight: 900;
}

.notification-item .button.ghost:hover {
  color: #ffffff;
  background: #000000;
}

.notification-item .button.secondary {
  min-height: 38px;
  padding: 7px 14px;
  border: 2px solid var(--notice-line);
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
}

.notification-item .button.secondary:hover:not(:disabled) {
  border-color: var(--notice-line);
  background: var(--notice-green);
  color: #000000;
}

.notification-item .button.danger-outline {
  min-height: 38px;
  padding: 7px 14px;
  border: 2px solid #b91c1c;
  border-radius: 14px;
  background: transparent;
  color: #b91c1c;
  font-weight: 900;
}

.notification-item .button.danger-outline:hover {
  background: #fee2e2;
  border-color: #b91c1c;
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 900;
  border: 2px dashed var(--notice-line);
  border-radius: 24px;
  background: #ffffff;
}

.error-message {
  margin-bottom: var(--space-4);
  padding: 12px 16px;
  font-weight: 900;
  border: 2px solid #b91c1c;
  border-radius: 14px;
}

.grid {
  gap: var(--space-4);
}

@media (max-width: 768px) {
  .notification-item {
    padding: 16px;
  }
}
</style>




