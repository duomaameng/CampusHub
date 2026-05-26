<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fileApi, reportApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusText } from '@/types'
import type { ApplicationItem, TaskItem, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const task = ref<TaskItem>()
const applications = ref<ApplicationItem[]>([])
const applyMessage = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')
const reportReason = ref('')
const reportUploadError = ref('')
const reportSubmitting = ref(false)
const reportEvidenceUploading = ref(false)
const reportEvidenceFiles = ref<UploadedFileItem[]>([])

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

async function handleReportEvidenceChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  reportUploadError.value = ''
  reportEvidenceUploading.value = true
  try {
    for (const file of files) {
      const uploaded = await fileApi.upload(file, 'REPORT_EVIDENCE')
      reportEvidenceFiles.value.push(uploaded)
    }
  } catch (err) {
    reportUploadError.value = err instanceof Error ? err.message : '举报证据上传失败'
  } finally {
    reportEvidenceUploading.value = false
    input.value = ''
  }
}

function removeReportEvidence(fileId: number) {
  reportEvidenceFiles.value = reportEvidenceFiles.value.filter((item) => item.id !== fileId)
}

async function submitReport() {
  if (!task.value || !reportReason.value.trim()) return

  error.value = ''
  success.value = ''
  reportSubmitting.value = true
  try {
    await reportApi.submit(
      task.value.id,
      reportReason.value.trim(),
      reportEvidenceFiles.value.map((item) => item.id)
    )
    reportReason.value = ''
    reportEvidenceFiles.value = []
    success.value = '举报已提交'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '举报提交失败'
  } finally {
    reportSubmitting.value = false
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

        <div v-if="task.imageUrls.length" class="upload-grid">
          <article v-for="url in task.imageUrls" :key="url" class="upload-card">
            <img :src="resolveAssetUrl(url)" alt="任务配图" />
          </article>
        </div>

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

        <section v-if="auth.isAuthenticated && !isPublisher" class="panel grid">
          <h2>举报任务</h2>
          <div class="field">
            <textarea v-model.trim="reportReason" placeholder="填写举报原因或补充说明" maxlength="300" />
          </div>
          <label class="button ghost upload-trigger">
            <input multiple type="file" accept="image/png,image/jpeg,image/webp" @change="handleReportEvidenceChange" />
            <span>{{ reportEvidenceUploading ? '上传中...' : '上传举报证据' }}</span>
          </label>
          <p class="hint">支持截图或照片证据，每张不超过 5MB。</p>
          <p v-if="reportUploadError" class="error-message">{{ reportUploadError }}</p>
          <div v-if="reportEvidenceFiles.length" class="upload-grid">
            <article v-for="item in reportEvidenceFiles" :key="item.id" class="upload-card">
              <img :src="resolveAssetUrl(item.url)" :alt="item.fileName" />
              <div class="upload-card-meta">
                <strong>{{ item.fileName }}</strong>
                <button class="button ghost" type="button" @click="removeReportEvidence(item.id)">移除</button>
              </div>
            </article>
          </div>
          <button class="button danger" type="button" :disabled="reportSubmitting || !reportReason" @click="submitReport">
            {{ reportSubmitting ? '提交中...' : '提交举报' }}
          </button>
        </section>

        <section v-if="isPublisher" class="panel grid">
          <h2>接单申请</h2>
          <div v-if="!applications.length" class="empty-state">暂无申请</div>
          <div v-for="application in applications" :key="application.id" class="item-card">
            <div class="item-title">
              <h3>{{ application.applicantNickname }}</h3>
              <span class="tag">{{ applicationStatusText[application.status] }}</span>
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
