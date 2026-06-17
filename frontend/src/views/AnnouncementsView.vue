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
  <section class="announcements-view">
    <div class="page-title">
      <div>
        <h1>公告</h1>
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
        <strong>及时了解 CampusHub 的重要通知与服务动态。</strong>
        <span>这里汇总平台维护、功能更新和校园互助相关提醒，帮助你更安心地使用服务。</span>
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
.announcements-view {
  --notice-green: #7dbe8e;
  --notice-dark: #191a23;
  --notice-grey: #f3f3f3;
  --notice-line: #000000;
}

.announcements-view :deep(.page-title) {
  align-items: flex-start;
  padding: 20px 26px;
  border: 2px solid var(--notice-line);
  border-radius: 24px;
  background:
    radial-gradient(circle at 92% 12%, rgba(125, 190, 142, 0.8) 0 58px, transparent 60px),
    #ffffff;
  box-shadow: 0 5px 0 var(--notice-line);
}

.announcements-view :deep(.page-title h1) {
  width: max-content;
  margin: 0;
  padding: 5px 10px;
  border-radius: 24px;
  border: 2px solid #000000;
  background: var(--notice-green);
  background-clip: border-box;
  -webkit-background-clip: border-box;
  color: #000000;
  -webkit-text-fill-color: #000000;
  font-size: 32px;
  line-height: 1.12;
  letter-spacing: 0;
  box-shadow: 0 4px 0 #000000;
}

.announcements-view :deep(.page-title p) {
  color: #2b2d35;
  font-size: 14px;
  font-weight: 700;
}

.announcement-hero {
  grid-template-columns: 2fr 1fr 1fr;
  border: 2px solid var(--notice-line);
  border-radius: 24px;
  background: var(--notice-grey);
  box-shadow: 0 5px 0 var(--notice-line);
  overflow: hidden;
}

.announcement-hero :deep(.hero-copy),
.announcement-hero :deep(.metric-card) {
  padding: 18px 22px;
  min-height: 118px;
}

.announcement-hero :deep(.hero-copy) {
  background: #ffffff;
  border-right: 2px solid var(--notice-line);
}

.announcement-hero :deep(.eyebrow) {
  width: max-content;
  padding: 5px 10px;
  border-radius: 24px;
  background: var(--notice-green);
  color: #000000;
  font-weight: 900;
  font-size: 13px;
}

.announcement-hero :deep(.hero-copy strong) {
  color: #000000;
  font-size: 18px;
  line-height: 1.18;
}

.announcement-hero :deep(.hero-copy span:last-child) {
  color: #343743;
  font-weight: 700;
  font-size: 12px;
}

.announcement-hero :deep(.metric-card) {
  border: 0;
  border-left: 2px solid var(--notice-line);
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.announcement-hero :deep(.metric-card strong) {
  color: #000000;
  font-size: 24px;
  -webkit-text-fill-color: currentColor;
  background: none;
}

.announcement-hero :deep(.metric-icon) {
  width: 38px;
  height: 38px;
  border: 2px solid var(--notice-line);
  background: var(--notice-green);
  color: #000000;
}

.page-title .button.secondary {
  margin-top: 8px;
  padding: 12px 22px;
  border: 2px solid var(--notice-line);
  border-radius: 14px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
  font-size: 14px;
  background: var(--notice-dark);
  color: #ffffff;
  box-shadow: none;
}

.page-title .button.secondary:hover {
  background: #000000;
  box-shadow: 0 0 0 4px rgba(125, 190, 142, 0.55);
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
  padding: var(--space-6);
  border: 2px solid var(--notice-line);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 5px 0 var(--notice-line);
  position: relative;
}

.announcement-card::after {
  display: none;
}

.announcement-card:hover::after {
  opacity: 1;
}

.announcement-card:hover {
  border-color: var(--notice-line);
  box-shadow: 0 7px 0 var(--notice-line);
  transform: translateY(-2px);
}

.announcement-card h2 {
  max-width: 100%;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.25;
}

.announcement-card .tag {
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: 0.03em;
  padding: 4px 12px;
  border: 2px solid currentColor;
  border-radius: 999px;
}

.announcement-card .tag.warning {
  background: #fff7ed;
  color: #b45309;
}

.announcement-card .tag.info {
  background: var(--notice-grey);
  color: var(--notice-dark);
}

.announcement-content {
  white-space: pre-line;
  font-size: 14px;
  line-height: 1.7;
  color: #343743;
}

.announcement-card .meta-line {
  font-size: 12px;
  color: #4b4d55;
  padding-top: var(--space-3);
  border-top: 2px solid var(--notice-line);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: var(--space-4);
  background: #ffffff;
  border: 2px solid var(--notice-line);
  border-radius: 24px;
  box-shadow: 0 5px 0 var(--notice-line);
}

.pagination .button.ghost {
  padding: 9px 18px;
  font-weight: 900;
  letter-spacing: 0;
  border: 2px solid var(--notice-line);
  border-radius: 14px;
  color: #000000;
}

.pagination .button.ghost:hover:not(:disabled) {
  border-color: var(--notice-line);
  background: var(--notice-green);
  color: #000000;
}

.pagination .hint {
  font-size: 13px;
  font-weight: 900;
  color: #000000;
  padding: 6px 14px;
  background: var(--notice-grey);
  border-radius: 14px;
  border: 2px solid var(--notice-line);
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

@media (max-width: 1024px) {
  .announcement-hero {
    grid-template-columns: 1fr;
  }
}
</style>
