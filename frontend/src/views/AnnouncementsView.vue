<script setup lang="ts">
import { AlertCircle, CalendarClock, Megaphone, RefreshCcw } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'

import { announcementApi } from '@/services/api'
import { announcementPriorityText } from '@/types'
import type { AnnouncementItem, PageData } from '@/types'

const pageNumber = ref(1)
const page = ref<PageData<AnnouncementItem>>()
const loading = ref(false)
const error = ref('')

const importantCount = computed(() => page.value?.records.filter((item) => item.priority === 'IMPORTANT').length ?? 0)

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await announcementApi.list({ page: pageNumber.value, size: 10 })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '公告加载失败'
  } finally {
    loading.value = false
  }
}

async function goPage(nextPage: number) {
  if (nextPage < 1 || (page.value && nextPage > page.value.pages)) return
  pageNumber.value = nextPage
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>系统公告</h1>
        <p>查看平台维护、功能更新和重要运营通知。</p>
      </div>
      <button class="button secondary" type="button" @click="load">
        <RefreshCcw class="button-icon" aria-hidden="true" />
        <span>刷新</span>
      </button>
    </div>

    <div class="hero-strip announcement-hero">
      <div class="hero-copy">
        <span class="eyebrow">
          <Megaphone class="eyebrow-icon" aria-hidden="true" />
          Notice Board
        </span>
        <strong>平台公告集中展示，便于游客和用户快速了解当前系统动态。</strong>
        <span>后台仅发布处于上线状态的公告到这里，列表按发布时间倒序展示。</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon">
          <Megaphone aria-hidden="true" />
        </span>
        <strong>{{ page?.total ?? 0 }}</strong>
        <span>公告总数</span>
      </div>
      <div class="metric-card">
        <span class="metric-icon amber">
          <AlertCircle aria-hidden="true" />
        </span>
        <strong>{{ importantCount }}</strong>
        <span>重要公告</span>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载公告</div>
    <div v-else-if="!page?.records.length" class="empty-state">暂无公告</div>

    <div v-else class="grid">
      <article
        v-for="(item, index) in page.records"
        :key="item.id"
        class="item-card announcement-card"
        :style="{ '--i': index }"
      >
        <div class="item-title">
          <h2>{{ item.title }}</h2>
          <span :class="['tag', item.priority === 'IMPORTANT' ? 'warning' : 'info']">
            {{ announcementPriorityText[item.priority] }}
          </span>
        </div>
        <p class="announcement-content">{{ item.content }}</p>
        <div class="meta-line">
          <span><CalendarClock class="meta-icon" aria-hidden="true" />{{ formatDate(item.createdAt) }}</span>
        </div>
      </article>
    </div>

    <div v-if="page && page.pages > 1" class="pagination">
      <button class="button ghost" type="button" :disabled="page.page <= 1" @click="goPage(page.page - 1)">上一页</button>
      <span class="hint">第 {{ page.page }} / {{ page.pages }} 页</span>
      <button class="button ghost" type="button" :disabled="page.page >= page.pages" @click="goPage(page.page + 1)">下一页</button>
    </div>
  </section>
</template>

<style scoped>
.announcement-hero {
  grid-template-columns: 2fr 1fr 1fr;
}

.announcement-card {
  gap: var(--space-4);
}

.announcement-content {
  white-space: pre-line;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

@media (max-width: 1024px) {
  .announcement-hero {
    grid-template-columns: 1fr;
  }
}
</style>
