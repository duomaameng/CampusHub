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
      <button class="button secondary" type="button" @click="readAll">全部已读</button>
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
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.notification-item.unread {
  border-left: 2px solid var(--primary-500);
}
</style>
