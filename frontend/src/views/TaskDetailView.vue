<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ApplicationItem, TaskItem } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const task = ref<TaskItem>()
const applications = ref<ApplicationItem[]>([])
const applyMessage = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

const taskId = computed(() => Number(route.params.id))
const isPublisher = computed(() => Boolean(task.value && auth.user?.id === task.value.publisherId))
const canApply = computed(() => Boolean(auth.isAuthenticated && auth.user?.verified && applyMessage.value))

const categoryText: Record<string, string> = {
  EXPRESS: '快递代取',
  ERRAND: '跑腿代办',
  TUTORING: '学习辅导',
  SECOND_HAND: '二手交易',
  LOST_FOUND: '失物招领',
  CONSULTATION: '咨询问答',
  TEAM_UP: '组队搭子',
  OTHER: '其他'
}

async function load() {
  error.value = ''
  loading.value = true
  try {
    task.value = await taskApi.get(taskId.value)
    if (isPublisher.value) {
      applications.value = await taskApi.applications(taskId.value)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '需求加载失败'
  } finally {
    loading.value = false
  }
}

async function applyTask() {
  error.value = ''
  success.value = ''
  try {
    await taskApi.apply(taskId.value, applyMessage.value)
    success.value = '接单申请已提交'
    applyMessage.value = ''
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '接单申请失败'
  }
}

async function confirmApplication(applicationId: number) {
  error.value = ''
  try {
    const result = await taskApi.confirmApplication(applicationId)
    router.push(`/orders/${result.orderId}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '确认接单失败'
  }
}

onMounted(load)
</script>

<template>
  <section>
    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载需求</div>

    <div v-else-if="task" class="detail-layout">
      <article class="panel grid">
        <div class="page-title">
          <div>
            <h1>{{ task.title }}</h1>
            <p>{{ task.publisherNickname }} · {{ task.campus }} · {{ new Date(task.createdAt).toLocaleString() }}</p>
          </div>
          <span class="tag">{{ categoryText[task.category] }}</span>
        </div>

        <p>{{ task.description }}</p>

        <div class="grid two">
          <div class="panel">
            <strong>报酬类型</strong>
            <p>{{ task.rewardType }}</p>
          </div>
          <div class="panel">
            <strong>截止时间</strong>
            <p>{{ new Date(task.deadline).toLocaleString() }}</p>
          </div>
        </div>

        <div v-if="task.categoryFields && Object.keys(task.categoryFields).length" class="panel">
          <h2>分类字段</h2>
          <div class="meta-line">
            <span v-for="(value, key) in task.categoryFields" :key="key" class="tag">{{ key }}: {{ value }}</span>
          </div>
        </div>
      </article>

      <aside class="grid">
        <section v-if="!isPublisher" class="panel grid">
          <h2>申请接单</h2>
          <div class="field">
            <textarea v-model.trim="applyMessage" placeholder="说明你的时间、位置或服务能力" />
          </div>
          <button class="button primary" type="button" :disabled="!canApply" @click="applyTask">提交申请</button>
          <p v-if="!auth.isAuthenticated" class="hint">登录后可申请接单。</p>
          <p v-else-if="!auth.user?.verified" class="hint">完成邮箱验证后才能申请接单。</p>
          <p v-if="success" class="success-message">{{ success }}</p>
        </section>

        <section v-else class="panel grid">
          <h2>接单申请</h2>
          <div v-if="!applications.length" class="empty-state">暂无申请</div>
          <div v-for="application in applications" :key="application.id" class="item-card">
            <div class="item-title">
              <h3>{{ application.applicantNickname }}</h3>
              <span class="tag">{{ application.status }}</span>
            </div>
            <p>{{ application.message }}</p>
            <p class="hint">信用分 {{ application.applicantCreditScore }} · {{ new Date(application.createdAt).toLocaleString() }}</p>
            <button
              class="button secondary"
              type="button"
              :disabled="application.status !== 'PENDING'"
              @click="confirmApplication(application.id)"
            >
              确认接单
            </button>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>
