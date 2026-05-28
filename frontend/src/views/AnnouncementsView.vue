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

.page-title .button-icon {
  transition: transform 0.5s ease;
}

.page-title .button.secondary:hover .button-icon {
  transform: rotate(180deg);
}

.announcement-card {
  gap: var(--space-4);
  padding: var(--space-5);
  border: 1.5px solid var(--border-light);
  position: relative;
}

.announcement-card::after {
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

.announcement-card:hover::after {
  opacity: 1;
}

.announcement-card:hover {
  border-color: rgba(99, 102, 241, 0.15);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.08);
  transform: translateY(-3px);
}

.announcement-card h2 {
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.35;
}

.announcement-card .tag {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 4px 12px;
}

.announcement-card .tag.warning {
  background: linear-gradient(135deg, var(--warning-bg), rgba(245, 158, 11, 0.1));
  color: var(--warning);
  border-color: rgba(245, 158, 11, 0.25);
}

.announcement-card .tag.info {
  background: linear-gradient(135deg, var(--info-bg), rgba(59, 130, 246, 0.1));
  color: var(--info);
  border-color: rgba(59, 130, 246, 0.25);
}

.announcement-content {
  white-space: pre-line;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.announcement-card .meta-line {
  font-size: 12px;
  color: var(--text-tertiary);
  padding-top: var(--space-2);
  border-top: 1px solid var(--border-light);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: var(--space-4);
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.pagination .button.ghost {
  padding: 9px 18px;
  font-weight: 700;
  letter-spacing: 0.01em;
  border: 1.5px solid var(--border-light);
}

.pagination .button.ghost:hover:not(:disabled) {
  border-color: var(--primary-400);
  background: var(--primary-50);
  color: var(--primary-700);
}

.pagination .hint {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 6px 14px;
  background: var(--bg-body);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
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

@media (max-width: 1024px) {
  .announcement-hero {
    grid-template-columns: 1fr;
  }
}
</style>
