<script setup lang="ts">
import { AlertTriangle, Download, FileText, X } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { useRealtimeRefresh } from '@/composables/useRealtimeRefresh'
import { fileApi, reportApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusText } from '@/types'
import type { ApplicationItem, RewardPaymentMethod, RewardType, TaskItem, TaskUpdatePayload, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useConfirmDialog } from '@/composables/useConfirmDialog'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const task = ref<TaskItem>()
const applications = ref<ApplicationItem[]>([])
const applyMessage = ref('')
const applicationDialogOpen = ref(false)
const applicationsLoading = ref(false)
const applying = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const reportReason = ref('')
const reportUploadError = ref('')
const reportSubmitting = ref(false)
const reportEvidenceUploading = ref(false)
const reportEvidenceFiles = ref<UploadedFileItem[]>([])
const reportDialogOpen = ref(false)
const editMode = ref(false)
const savingTask = ref(false)
const deletingTask = ref(false)
const favoriteLoading = ref(false)
const actionLoadingApplicationId = ref<number | null>(null)
const previewImageUrl = ref('')
const dangerDialog = useConfirmDialog()
const editForm = reactive<TaskUpdatePayload & { categoryFields: Record<string, string | number | boolean> }>({
  category: 'EXPRESS',
  title: '',
  description: '',
  campus: '',
  rewardType: 'NEGOTIABLE',
  rewardAmount: undefined,
  paymentMethod: undefined,
  deadline: '',
  anonymous: false,
  imageIds: [],
  categoryFields: {}
})

const taskId = computed(() => Number(route.params.id))
const isPublisher = computed(() => Boolean(task.value && auth.user?.id === task.value.publisherId))
const canApply = computed(() => Boolean(
  task.value?.category !== 'TEAM_UP' &&
  task.value?.status === 'OPEN' &&
  auth.isAuthenticated &&
  auth.user?.verified &&
  applyMessage.value
))
const canEditTask = computed(() => Boolean(
  isPublisher.value &&
  task.value?.status === 'OPEN' &&
  (task.value.category === 'TEAM_UP' || task.value.applicationCount === 0)
))
const canReportTask = computed(() => Boolean(
  auth.isAuthenticated &&
  auth.user?.id &&
  task.value &&
  task.value.publisherId !== auth.user.id
))
const isFavorited = computed(() => Boolean(task.value?.isFavorited || (task.value as (TaskItem & { favorited?: boolean }) | undefined)?.favorited))

function openImagePreview(url: string) {
  previewImageUrl.value = resolveAssetUrl(url)
  document.body.style.overflow = 'hidden'
}

function closeImagePreview() {
  previewImageUrl.value = ''
  document.body.style.overflow = ''
}

async function downloadTaskFile(file: { id: number; fileName: string }) {
  if (!task.value?.fileDownloadAllowed) return
  try {
    const blob = await taskApi.downloadFile(taskId.value, file.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文件下载失败'
  }
}

function handlePreviewKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && previewImageUrl.value) closeImagePreview()
}

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

const rewardText: Record<RewardType, string> = {
  CASH: '定价',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分'
}

const paymentMethodText: Record<RewardPaymentMethod, string> = {
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CASH: '现金'
}

const categoryFieldText: Record<string, string> = {
  expressCompany: '快递公司',
  building: '宿舍楼',
  pickupLocation: '取件地点',
  pickupCode: '取件码',
  pickupAddress: '取件地址',
  deliveryLocation: '送达地点',
  deliveryAddress: '送达地址',
  destination: '目的地',
  subject: '辅导科目',
  level: '难度/年级',
  goodsCategory: '商品分类',
  price: '售价',
  itemName: '物品名称',
  condition: '成色',
  lostOrFound: '失物/招领',
  location: '地点',
  itemLocation: '地点',
  foundTime: '丢失/捡到时间',
  itemDescription: '物品描述',
  contactInfo: '联系方式',
  topic: '咨询主题',
  activityType: '活动类型',
  requiredCount: '人数需求',
  activityTime: '活动时间',
  teamType: '组队类型',
  expectedMembers: '期望人数',
  note: '补充说明'
}

const categoryFieldValueText: Record<string, string> = {
  NEW: '全新',
  LIKE_NEW: '几乎全新',
  USED: '有使用痕迹'
}

function formatCategoryFieldKey(key: string) {
  return categoryFieldText[key] || key
}

function formatCategoryFieldValue(value: string | number | boolean) {
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'string') return categoryFieldValueText[value] || value
  return value
}

async function load(silent = false) {
  if (!silent) {
    error.value = ''
    loading.value = true
  }
  try {
    task.value = await taskApi.get(taskId.value)
    if (isPublisher.value && task.value.category !== 'TEAM_UP') {
      await loadApplications(silent)
    }
  } catch (err) {
    if (!silent) error.value = err instanceof Error ? err.message : '需求加载失败'
  } finally {
    if (!silent) loading.value = false
  }
}

async function loadApplications(silent = false) {
  if (!isPublisher.value || task.value?.category === 'TEAM_UP') return
  if (!silent) applicationsLoading.value = true
  try {
    applications.value = await taskApi.applications(taskId.value)
  } catch (err) {
    if (!silent) error.value = err instanceof Error ? err.message : '接单申请加载失败'
  } finally {
    if (!silent) applicationsLoading.value = false
  }
}

function toDatetimeLocal(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000).toISOString().slice(0, 16)
}

function startEdit() {
  if (!task.value) return
  if (!canEditTask.value) {
    error.value = '已有接单或接单申请，不能编辑该需求'
    success.value = ''
    return
  }
  editForm.category = task.value.category
  editForm.title = task.value.title
  editForm.description = task.value.description
  editForm.campus = task.value.campus
  editForm.rewardType = task.value.rewardType
  editForm.rewardAmount = task.value.rewardAmount
  editForm.paymentMethod = task.value.paymentMethod
  editForm.deadline = toDatetimeLocal(task.value.deadline)
  editForm.anonymous = task.value.anonymous
  editForm.categoryFields = task.value.categoryFields || {}
  editMode.value = true
}

async function saveTask() {
  if (!task.value) return
  error.value = ''
  success.value = ''
  savingTask.value = true
  try {
    task.value = await taskApi.update(task.value.id, {
      category: editForm.category,
      title: editForm.title,
      description: editForm.description,
      campus: editForm.campus,
      rewardType: editForm.rewardType,
      rewardAmount: editForm.rewardType === 'CASH' ? editForm.rewardAmount : undefined,
      paymentMethod: editForm.rewardType === 'CASH' ? editForm.paymentMethod : undefined,
      deadline: editForm.deadline,
      anonymous: editForm.anonymous,
      categoryFields: editForm.categoryFields
    })
    editMode.value = false
    success.value = '需求已更新'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '需求更新失败'
  } finally {
    savingTask.value = false
  }
}

async function deleteTask() {
  if (!task.value) return
  if (!canEditTask.value) {
    error.value = '已有接单或接单申请，不能删除该需求'
    success.value = ''
    return
  }
  const currentTask = task.value
  dangerDialog.request({
    title: currentTask.category === 'TEAM_UP' ? '取消这条组队帖？' : '删除这条需求？',
    description: currentTask.category === 'TEAM_UP'
      ? `“${currentTask.title}”取消后将不再展示，且无法恢复。`
      : `“${currentTask.title}”删除后无法恢复，收藏和相关申请也会受到影响。`,
    confirmText: currentTask.category === 'TEAM_UP' ? '确认取消' : '确认删除'
  }, async () => {
    error.value = ''
    deletingTask.value = true
    try {
      await taskApi.remove(currentTask.id)
      await router.push('/tasks')
    } catch (err) {
      error.value = err instanceof Error ? err.message : '需求删除失败'
      throw err
    } finally {
      deletingTask.value = false
    }
  })
}

async function toggleFavorite() {
  if (!task.value) return
  error.value = ''
  favoriteLoading.value = true
  try {
    const result = await taskApi.toggleFavorite(task.value.id)
    task.value = {
      ...task.value,
      isFavorited: result.favorited,
      favoriteCount: Math.max(0, task.value.favoriteCount + (result.favorited ? 1 : -1))
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '收藏操作失败'
  } finally {
    favoriteLoading.value = false
  }
}

async function applyTask() {
  error.value = ''
  success.value = ''
  applying.value = true
  try {
    await taskApi.apply(taskId.value, applyMessage.value)
    success.value = '接单申请已提交'
    applyMessage.value = ''
    applicationDialogOpen.value = false
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '接单申请失败'
  } finally {
    applying.value = false
  }
}

async function openApplicationDialog() {
  error.value = ''
  success.value = ''
  applicationDialogOpen.value = true
  if (isPublisher.value) {
    await loadApplications()
  }
  if (isPublisher.value && task.value?.hasUnreadApplications) {
    task.value = { ...task.value, hasUnreadApplications: false }
    try {
      await taskApi.markApplicationsViewed(taskId.value)
    } catch (err) {
      if (task.value) task.value = { ...task.value, hasUnreadApplications: true }
      error.value = err instanceof Error ? err.message : '申请查看状态更新失败'
    }
  }
}

function closeApplicationDialog() {
  if (applying.value || actionLoadingApplicationId.value !== null) return
  applicationDialogOpen.value = false
}

async function confirmApplication(applicationId: number) {
  error.value = ''
  actionLoadingApplicationId.value = applicationId
  try {
    const result = await taskApi.confirmApplication(applicationId)
    router.push(`/orders/${result.orderId}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '确认接单失败'
  } finally {
    actionLoadingApplicationId.value = null
  }
}

async function rejectApplication(applicationId: number) {
  error.value = ''
  success.value = ''
  actionLoadingApplicationId.value = applicationId
  try {
    await taskApi.rejectApplication(applicationId)
    success.value = '已拒绝接单申请'
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '拒绝申请失败'
  } finally {
    actionLoadingApplicationId.value = null
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

function openReportDialog() {
  if (!canReportTask.value) return
  error.value = ''
  reportUploadError.value = ''
  reportDialogOpen.value = true
}

function closeReportDialog() {
  if (reportSubmitting.value) return
  reportDialogOpen.value = false
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
    reportDialogOpen.value = false
    success.value = '举报已提交'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '举报提交失败'
  } finally {
    reportSubmitting.value = false
  }
}

useRealtimeRefresh(
  ['TASKS_CHANGED', 'APPLICATIONS_CHANGED'],
  () => load(true)
)

onMounted(() => {
  void load()
  window.addEventListener('keydown', handlePreviewKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handlePreviewKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <section class="task-detail-view">
    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="success" class="success-message">{{ success }}</p>
    <div v-if="task" class="detail-layout single-column">
      <article class="panel grid">
        <div class="page-title">
          <div>
            <div class="task-title-row">
              <h1>{{ task.title }}</h1>
              <button
                v-if="canReportTask"
                class="report-icon-button"
                type="button"
                title="举报任务"
                aria-label="举报任务"
                @click="openReportDialog"
              >
                <AlertTriangle aria-hidden="true" />
              </button>
            </div>
            <p v-if="task.anonymous">匿名用户 · {{ task.campus }} · {{ new Date(task.createdAt).toLocaleString() }}</p>
            <p v-else><RouterLink :to="{ name: 'user-public-profile', params: { id: task.publisherId } }">{{ task.publisherNickname }}</RouterLink> · {{ task.campus }} · {{ new Date(task.createdAt).toLocaleString() }}</p>
          </div>
          <div class="task-actions">
            <span class="tag">{{ categoryText[task.category] }}</span>
            <button
              v-if="isPublisher && !editMode"
              class="button secondary"
              :class="{ 'is-soft-disabled': !canEditTask }"
              type="button"
              :aria-disabled="!canEditTask"
              @click="startEdit"
            >
              编辑
            </button>
            <button
              v-if="isPublisher && !editMode"
              class="button danger"
              :class="{ 'is-soft-disabled': !canEditTask }"
              type="button"
              :disabled="deletingTask"
              :aria-disabled="!canEditTask"
              @click="deleteTask"
            >
              {{ deletingTask ? (task.category === 'TEAM_UP' ? '取消中...' : '删除中...') : (task.category === 'TEAM_UP' ? '取消帖子' : '删除') }}
            </button>
            <button
              v-if="auth.isAuthenticated && !isPublisher"
              :class="['button', 'favorite-button', isFavorited ? 'active' : '']"
              type="button"
              :disabled="favoriteLoading"
              @click="toggleFavorite"
            >
              {{ favoriteLoading ? '处理中...' : isFavorited ? '取消收藏' : '收藏' }}
            </button>
          </div>
        </div>

        <form v-if="editMode" class="main-edit-form grid" @submit.prevent="saveTask">
          <div class="field">
            <label for="edit-title">标题</label>
            <input id="edit-title" v-model.trim="editForm.title" required maxlength="100" />
          </div>
          <div class="field">
            <label for="edit-description">描述</label>
            <textarea id="edit-description" v-model.trim="editForm.description" required maxlength="2000" />
          </div>
          <div class="grid two">
            <div class="field">
              <label for="edit-campus">校区</label>
              <input id="edit-campus" v-model.trim="editForm.campus" required />
            </div>
            <div class="field">
              <label for="edit-reward">{{ editForm.category === 'SECOND_HAND' ? '交易方式' : '报酬类型' }}</label>
              <select id="edit-reward" v-model="editForm.rewardType" required>
                <option value="CASH">定价</option>
                <option value="NEGOTIABLE">面议</option>
                <option value="CREDIT_INTENT">积分</option>
              </select>
            </div>
          </div>
          <div v-if="editForm.rewardType === 'CASH'" class="grid two">
            <div class="field">
              <label for="edit-reward-amount">{{ editForm.category === 'SECOND_HAND' ? '售价（元）' : '酬金金额（元）' }}</label>
              <input id="edit-reward-amount" v-model.number="editForm.rewardAmount" type="number" min="0.01" step="0.01" required />
            </div>
            <div class="field">
              <label for="edit-payment-method">{{ editForm.category === 'SECOND_HAND' ? '收款方式' : '支付方式' }}</label>
              <select id="edit-payment-method" v-model="editForm.paymentMethod" required>
                <option value="WECHAT">微信</option>
                <option value="ALIPAY">支付宝</option>
                <option value="CASH">现金</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label for="edit-deadline">截止时间</label>
            <input id="edit-deadline" v-model="editForm.deadline" type="datetime-local" required />
          </div>
          <div v-if="editForm.category === 'TEAM_UP'" class="grid two">
            <div class="field">
              <label for="edit-activity-type">活动类型</label>
              <input id="edit-activity-type" v-model.trim="editForm.categoryFields.activityType" required />
            </div>
            <div class="field">
              <label for="edit-required-count">人数需求</label>
              <input id="edit-required-count" v-model.number="editForm.categoryFields.requiredCount" type="number" min="1" step="1" required />
            </div>
            <div class="field">
              <label for="edit-activity-time">活动时间</label>
              <input id="edit-activity-time" v-model="editForm.categoryFields.activityTime" type="datetime-local" required />
            </div>
            <div class="field">
              <label for="edit-contact-info">联系方式</label>
              <input id="edit-contact-info" v-model.trim="editForm.categoryFields.contactInfo" required />
            </div>
          </div>
          <label class="checkbox-label">
            <input v-model="editForm.anonymous" type="checkbox" />
            <span>匿名发布</span>
          </label>
          <div class="actions">
            <button class="button primary" type="submit" :disabled="savingTask">{{ savingTask ? '保存中...' : '保存修改' }}</button>
            <button class="button ghost" type="button" @click="editMode = false">取消</button>
          </div>
        </form>

        <template v-else>
          <p>{{ task.description }}</p>

          <div v-if="task.imageUrls.length" class="upload-grid">
          <button
            v-for="url in task.imageUrls"
            :key="url"
            class="upload-card task-image-button"
            type="button"
            aria-label="全屏预览任务配图"
            @click="openImagePreview(url)"
          >
            <img :src="resolveAssetUrl(url)" alt="任务配图" />
          </button>
          </div>

          <section v-if="task.files?.length" class="task-files-section">
            <h2>任务文件</h2>
            <div class="task-files-list">
              <article v-for="file in task.files" :key="file.id" class="task-file-row">
                <FileText aria-hidden="true" />
                <strong>{{ file.fileName }}</strong>
                <button class="button ghost" type="button" :disabled="!task.fileDownloadAllowed" @click="downloadTaskFile(file)">
                  <Download class="button-icon" aria-hidden="true" />
                  <span>{{ task.fileDownloadAllowed ? '下载' : '仅服务方可下载' }}</span>
                </button>
              </article>
            </div>
          </section>

          <div class="grid two">
          <div class="panel">
            <strong>{{ task.category === 'SECOND_HAND' ? '交易方式' : '报酬类型' }}</strong>
            <p>
              {{ rewardText[task.rewardType] }}
              <template v-if="task.rewardType === 'CASH' && task.rewardAmount && task.paymentMethod">
                · ¥{{ Number(task.rewardAmount).toFixed(2) }} · {{ paymentMethodText[task.paymentMethod] }}
              </template>
            </p>
          </div>
          <div class="panel">
            <strong>截止时间</strong>
            <p>{{ new Date(task.deadline).toLocaleString() }}</p>
          </div>
          </div>

          <div class="order-information-section">
            <button
              v-if="task.category !== 'TEAM_UP'"
              class="button primary order-application-button"
              type="button"
              :disabled="!isPublisher && task.status !== 'OPEN'"
              @click="openApplicationDialog"
            >
              {{ isPublisher ? '查看接单申请' : '申请接单' }}
              <span v-if="isPublisher && task.hasUnreadApplications" class="application-unread-dot" aria-label="有新的接单申请" />
            </button>

            <div
              v-if="(task.categoryFields && Object.keys(task.categoryFields).length) || task.privateFieldsHidden"
              class="panel"
            >
              <h2>{{ task.category === 'TEAM_UP' ? '组队帖子信息' : '订单相关信息' }}</h2>
              <div v-if="task.categoryFields && Object.keys(task.categoryFields).length" class="meta-line">
                <span v-for="(value, key) in task.categoryFields" :key="key" class="tag">
                  {{ formatCategoryFieldKey(String(key)) }}: {{ formatCategoryFieldValue(value) }}
                </span>
              </div>
              <p v-if="task.privateFieldsHidden" class="hint">取件码、精确送达地点或联系方式等私密信息，将在发布者确认接单后对服务方显示。</p>
            </div>
          </div>
        </template>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="previewImageUrl" class="image-preview-overlay" role="dialog" aria-modal="true" aria-label="任务配图预览" @click.self="closeImagePreview">
        <button class="image-preview-close" type="button" aria-label="关闭图片预览" @click="closeImagePreview">×</button>
        <img :src="previewImageUrl" alt="任务配图大图预览" @click="closeImagePreview" />
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="applicationDialogOpen && task" class="application-modal-backdrop" role="presentation" @click.self="closeApplicationDialog">
        <section class="application-modal" role="dialog" aria-modal="true" aria-labelledby="task-application-modal-title">
          <header class="application-modal-header">
            <h2 id="task-application-modal-title">{{ isPublisher ? '接单申请' : '申请接单' }}</h2>
            <button class="application-modal-close" type="button" aria-label="关闭弹窗" :disabled="applying || actionLoadingApplicationId !== null" @click="closeApplicationDialog">
              <X aria-hidden="true" />
            </button>
          </header>

          <div v-if="isPublisher" class="application-modal-content grid">
            <p v-if="success" class="success-message">{{ success }}</p>
            <div v-if="applicationsLoading && !applications.length" class="empty-state">正在加载申请</div>
            <div v-else-if="!applications.length" class="empty-state">暂无申请</div>
            <div v-for="application in applications" :key="application.id" class="item-card">
              <div class="item-title">
                <h3><RouterLink :to="{ name: 'user-public-profile', params: { id: application.applicantId } }">{{ application.applicantNickname }}</RouterLink></h3>
                <span class="tag">{{ applicationStatusText[application.status] }}</span>
              </div>
              <p>{{ application.message }}</p>
              <p class="hint">信用分 {{ application.applicantCreditScore }} · {{ new Date(application.createdAt).toLocaleString() }}</p>
              <div v-if="task.status === 'OPEN' && application.status === 'PENDING'" class="application-actions">
                <button class="button secondary" type="button" :disabled="actionLoadingApplicationId === application.id" @click="confirmApplication(application.id)">确认接单</button>
                <button class="button danger" type="button" :disabled="actionLoadingApplicationId === application.id" @click="rejectApplication(application.id)">拒绝申请</button>
              </div>
            </div>
          </div>

          <div v-else class="application-modal-content grid">
            <div class="field">
              <textarea v-model.trim="applyMessage" placeholder="说明你的时间、位置或服务能力" />
            </div>
            <p v-if="!auth.isAuthenticated" class="hint">登录后可申请接单。</p>
            <p v-else-if="!auth.user?.verified" class="hint">完成邮箱验证后才能申请接单。</p>
            <div class="application-modal-actions">
              <button class="button ghost" type="button" :disabled="applying" @click="closeApplicationDialog">取消</button>
              <button class="button primary" type="button" :disabled="!canApply || applying" @click="applyTask">
                {{ applying ? '提交中...' : '确认申请' }}
              </button>
            </div>
          </div>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="reportDialogOpen && task" class="report-modal-backdrop" role="presentation" @click.self="closeReportDialog">
        <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="task-report-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">任务举报</p>
              <h2 id="task-report-modal-title">举报“{{ task.title }}”</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭举报弹窗" :disabled="reportSubmitting" @click="closeReportDialog">
              <X aria-hidden="true" />
            </button>
          </header>
          <form class="report-modal-form grid" @submit.prevent="submitReport">
            <div class="field">
              <label for="task-report-reason">举报原因</label>
              <textarea id="task-report-reason" v-model.trim="reportReason" placeholder="填写举报原因或补充说明" maxlength="300" required />
            </div>
            <label class="button ghost upload-trigger">
              <input multiple type="file" accept="image/png,image/jpeg,image/webp" @change="handleReportEvidenceChange" />
              <span>{{ reportEvidenceUploading ? '上传中...' : '上传举报证据' }}</span>
            </label>
            <p class="hint">支持截图或照片证据，每张不超过 5MB。</p>
            <p v-if="reportUploadError" class="error-message">{{ reportUploadError }}</p>
            <p v-if="error" class="error-message">{{ error }}</p>
            <div v-if="reportEvidenceFiles.length" class="upload-grid">
              <article v-for="item in reportEvidenceFiles" :key="item.id" class="upload-card">
                <img :src="resolveAssetUrl(item.url)" :alt="item.fileName" />
                <div class="upload-card-meta">
                  <strong>{{ item.fileName }}</strong>
                  <button class="button ghost" type="button" @click="removeReportEvidence(item.id)">移除</button>
                </div>
              </article>
            </div>
            <div class="report-modal-actions">
              <button class="button ghost" type="button" :disabled="reportSubmitting" @click="closeReportDialog">取消</button>
              <button class="button danger" type="submit" :disabled="reportSubmitting || !reportReason">
                {{ reportSubmitting ? '提交中...' : '提交举报' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
    <ConfirmDialog v-bind="dangerDialog.state" @confirm="dangerDialog.confirm" @cancel="dangerDialog.cancel" />
  </section>
</template>

<style scoped>
.task-detail-view {
  --task-green: #ffb454;
  --task-dark: #191a23;
  --task-grey: #f3f3f3;
}

.detail-layout {
  align-items: start;
  gap: 26px;
}

.detail-layout.single-column {
  grid-template-columns: minmax(0, 1fr);
}

.order-information-section {
  display: grid;
  gap: 14px;
}

.order-application-button {
  position: relative;
  overflow: visible;
  isolation: isolate;
  justify-self: start;
  min-width: 150px;
}

.application-unread-dot {
  position: absolute;
  top: -6px;
  right: -6px;
  z-index: 2;
  width: 13px;
  height: 13px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #dc2626;
  box-shadow: 0 0 0 2px #000000;
}

.button.is-soft-disabled {
  opacity: 0.48;
  filter: grayscale(0.18);
  box-shadow: none;
}

.button.is-soft-disabled:hover {
  transform: none;
  box-shadow: none;
}

.task-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.task-title-row h1 {
  min-width: 0;
}

.report-icon-button {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 2px solid #000000;
  border-radius: 50%;
  background: #ffffff;
  color: #c1121f;
  cursor: pointer;
  transition: transform var(--transition-fast), background var(--transition-fast);
}

.report-icon-button svg {
  width: 18px;
  height: 18px;
}

.report-icon-button:hover {
  background: #ffe8e8;
  transform: translateY(-2px);
}

.report-icon-button:focus-visible {
  outline: 3px solid rgba(255, 180, 84, 0.65);
  outline-offset: 2px;
}

.main-edit-form {
  margin-top: 4px;
  padding-top: 24px;
  border-top: 2px solid #000000;
}

.main-edit-form textarea {
  min-height: 140px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
}

.favorite-button {
  min-width: 86px;
  border: 2px solid #000000;
  background: #ffffff;
  color: #000000;
  box-shadow: none;
}

.favorite-button:hover:not(:disabled) {
  border-color: #000000;
  background: var(--task-green);
  color: #000000;
  box-shadow: none;
}

.favorite-button.active {
  border-color: #000000;
  background: var(--task-green);
  color: #000000;
  box-shadow: none;
}

.panel {
  border: 2px solid #000000;
  border-radius: 26px;
  background: #ffffff;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  overflow: hidden;
}

.panel::before,
.panel::after,
.item-card::before,
.item-card::after,
.empty-state::before {
  display: none;
}

.detail-layout > article.panel {
  position: relative;
  padding: 34px;
  background:
    radial-gradient(circle at 94% 10%, var(--task-green) 0 72px, transparent 73px),
    #ffffff;
}

.task-image-button {
  padding: 0;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: zoom-in;
}

.task-image-button img {
  transition: transform var(--transition-fast);
}

.task-image-button:hover img {
  transform: scale(1.03);
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  place-items: center;
  padding: 32px;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(6px);
}

.image-preview-overlay img {
  display: block;
  max-width: min(94vw, 1440px);
  max-height: 90vh;
  object-fit: contain;
  border: 2px solid #ffffff;
  border-radius: 18px;
  cursor: zoom-out;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.55);
}

.image-preview-close {
  position: fixed;
  top: 22px;
  right: 26px;
  z-index: 1;
  width: 48px;
  height: 48px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #191a23;
  color: #ffffff;
  font-size: 32px;
  line-height: 1;
  cursor: pointer;
}

.report-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 12000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(25, 26, 35, 0.62);
  backdrop-filter: blur(5px);
}

.report-modal {
  isolation: isolate;
  width: min(560px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 28px;
  border: 3px solid #000000;
  border-radius: 28px;
  background-color: #ffffff;
  background-image: radial-gradient(circle at 100% 0%, #ffb454 0 70px, transparent 71px);
  color: #000000;
  box-shadow: none;
}

.report-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.report-modal-eyebrow {
  margin-bottom: 5px;
  color: #b45309;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.report-modal-header h2 {
  max-width: 430px;
  font-size: 26px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.report-modal-close {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 2px solid #000000;
  border-radius: 50%;
  background: #ffffff;
  color: #000000;
  cursor: pointer;
}

.report-modal-close:hover:not(:disabled) {
  background: #ffe8e8;
  color: #c1121f;
}

.report-modal-close svg {
  width: 20px;
  height: 20px;
}

.report-modal-form textarea {
  min-height: 150px;
  padding: 13px 15px;
  background: #ffffff;
  color: #000000;
  line-height: 1.6;
}

.report-modal-form .field label {
  color: #343743;
  font-weight: 900;
}

.report-modal-form .hint {
  color: #6f7485;
}

.report-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
  padding-top: 18px;
  border-top: 2px solid #000000;
}

.application-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 12000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(25, 26, 35, 0.62);
  backdrop-filter: blur(5px);
}

.application-modal {
  --task-green: #ffb454;
  --task-dark: #191a23;
  width: min(680px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 28px;
  border: 3px solid #000000;
  border-radius: 28px;
  background:
    radial-gradient(circle at 100% 0%, #ffb454 0 70px, transparent 71px),
    #ffffff;
  color: #000000;
}

.application-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.application-modal-header h2 {
  font-size: 26px;
  font-weight: 900;
}

.application-modal-close {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 2px solid #000000;
  border-radius: 50%;
  background: #ffffff;
  color: #000000;
  cursor: pointer;
}

.application-modal-close:hover:not(:disabled) {
  background: #ffe8e8;
  color: #c1121f;
}

.application-modal-close svg {
  width: 20px;
  height: 20px;
}

.application-modal-content textarea {
  min-height: 140px;
  padding: 13px 15px;
  line-height: 1.6;
}

.application-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.application-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
  padding-top: 18px;
  border-top: 2px solid #000000;
}

@media (max-width: 620px) {
  .report-modal-backdrop,
  .application-modal-backdrop {
    padding: 14px;
  }

  .report-modal,
  .application-modal {
    padding: 22px;
    border-radius: 22px;
  }

  .report-modal-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .application-modal-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}

.panel h1 {
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: transparent;
  color: #000000;
  font-size: 32px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
  -webkit-text-fill-color: #000000;
  box-shadow: none;
}

.panel h2 {
  width: max-content;
  max-width: 100%;
  margin-bottom: var(--space-2);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
}

.panel h2::before {
  display: none;
}

.panel .tag {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  padding: 5px 14px;
  background: var(--task-green);
  color: #000000;
  border: 2px solid #000000;
  border-radius: 999px;
}

.panel .page-title p {
  margin-top: 12px;
  font-size: 14px;
  color: #4a4e5b;
  font-weight: 800;
}

.panel .page-title p a {
  color: #000000;
  font-weight: 900;
}

.panel .page-title p a:hover {
  color: #b45309;
}

.panel > p {
  max-width: 760px;
  font-size: 16px;
  line-height: 1.7;
  color: #343743;
  font-weight: 700;
}

.panel .grid.two .panel {
  padding: 20px;
  background: var(--task-grey);
  border: 2px solid #000000;
  border-radius: 22px;
  box-shadow: none;
}

.panel .grid.two .panel strong {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.panel .grid.two .panel p {
  font-size: 14px;
  font-weight: 800;
  color: #000000;
}

.upload-grid {
  gap: var(--space-3);
}

.task-files-section,
.task-files-list {
  display: grid;
  gap: 12px;
}

.task-file-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 2px solid #000000;
  border-radius: 18px;
  background: var(--task-grey);
}

.task-file-row > svg {
  width: 40px;
  height: 40px;
  padding: 8px;
  border: 2px solid #000000;
  border-radius: 12px;
  background: var(--task-green);
}

.task-file-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-card {
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: none;
  transition: all var(--transition-base);
}

.upload-card:hover {
  border-color: #000000;
  box-shadow: none;
  transform: translateY(-2px);
}

.upload-card img {
  transition: transform var(--transition-base);
}

.upload-card:hover img {
  transform: scale(1.03);
}

aside .panel {
  padding: 24px;
  border: 2px solid #000000;
  border-radius: 24px;
  box-shadow: none;
}

aside .panel h2 {
  font-size: 22px;
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: none;
}

.button.primary,
.button.secondary {
  color: #ffffff;
  background: var(--task-dark);
}

.button.primary:hover:not(:disabled),
.button.secondary:hover:not(:disabled) {
  color: #000000;
  background: var(--task-green);
  box-shadow: none;
  transform: translateY(-2px);
}

.button.ghost {
  color: #000000;
  background: #ffffff;
}

.button.ghost:hover:not(:disabled) {
  background: var(--task-green);
}

.button.danger {
  border-color: #000000;
  background: #ffffff;
  color: #c1121f;
}

.button.danger:hover:not(:disabled) {
  background: #ffe8e8;
  color: #9f0f19;
}

.field input,
.field textarea,
.field select,
aside .panel textarea {
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-weight: 800;
  box-shadow: none;
}

aside .panel textarea {
  min-height: 100px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  transition: all var(--transition-fast);
}

.field input:focus,
.field textarea:focus,
.field select:focus,
aside .panel textarea:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(255, 180, 84, 0.48);
  background: #ffffff;
}

aside .panel textarea::placeholder {
  color: #8b90a0;
  font-weight: 700;
}

.item-card {
  padding: 18px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: var(--task-grey);
  box-shadow: none;
}

.item-card:hover {
  border-color: #000000;
  transform: translateY(-2px);
  box-shadow: none;
}

.item-card h3 a {
  color: #000000;
  font-weight: 900;
}

.item-card h3 a:hover {
  color: #b45309;
}

.empty-state {
  padding: var(--space-8) var(--space-4);
  font-size: 13px;
  font-weight: 800;
  background: #ffffff;
  border: 2px dashed #000000;
  border-radius: 22px;
  box-shadow: none;
}

.error-message,
.success-message {
  margin-top: var(--space-2);
  padding: 10px 14px;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: none;
  font-weight: 800;
}

.hint {
  font-size: 12.5px;
  font-weight: 700;
  color: #6f7485;
}

@media (max-width: 1024px) {
  .detail-layout {
    gap: var(--space-6);
  }
}
</style>




