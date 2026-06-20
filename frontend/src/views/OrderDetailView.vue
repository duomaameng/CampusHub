<script setup lang="ts">
import { AlertTriangle, CheckCheck, Eye, MessageSquareText, Star, X, XCircle } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { fileApi, orderApi, reportApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusText, orderStatusText } from '@/types'
import type { ApplicationItem, OrderDetail, OrderStatusLog, ReviewItem, TaskItem, TaskUpdatePayload, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useConfirmDialog } from '@/composables/useConfirmDialog'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref<OrderDetail>()
const task = ref<TaskItem>()
const applications = ref<ApplicationItem[]>([])
const statusLogs = ref<OrderStatusLog[]>([])
const reviews = ref<ReviewItem[]>([])
const error = ref('')
const success = ref('')
const loading = ref(false)
const cancelReason = ref('')
const cancelDialogOpen = ref(false)
const cancelSubmitting = ref(false)
const reviewRating = ref(1)
const reviewContent = ref('')
const reviewDialogOpen = ref(false)
const reviewRecordsDialogOpen = ref(false)
const reviewSubmitting = ref(false)
const completionProofUploading = ref(false)
const completionProofError = ref('')
const uploadedCompletionProof = ref<UploadedFileItem | null>(null)
const completionDialogOpen = ref(false)
const completionSubmitting = ref(false)
const completionProofPreviewOpen = ref(false)
const reportReason = ref('')
const reportUploadError = ref('')
const reportSubmitting = ref(false)
const reportEvidenceUploading = ref(false)
const reportEvidenceFiles = ref<UploadedFileItem[]>([])
const reportDialogOpen = ref(false)
const editMode = ref(false)
const savingTask = ref(false)
const deletingTask = ref(false)
const actionLoadingApplicationId = ref<number | null>(null)
const dangerDialog = useConfirmDialog()
const editForm = reactive<TaskUpdatePayload>({
  category: 'EXPRESS',
  title: '',
  description: '',
  campus: '',
  rewardType: 'NEGOTIABLE',
  deadline: '',
  anonymous: false,
  imageIds: [],
  categoryFields: {}
})

const orderId = computed(() => Number(route.params.id))
const isPublisher = computed(() => order.value?.publisherId === auth.user?.id)
const isProvider = computed(() => order.value?.serviceProviderId === auth.user?.id)
const reportTargetUser = computed(() => {
  if (!order.value) return null
  if (isPublisher.value && order.value.serviceProviderId) {
    return {
      id: order.value.serviceProviderId,
      nickname: order.value.serviceProviderNickname || '服务方'
    }
  }
  if (isProvider.value) {
    return {
      id: order.value.publisherId,
      nickname: order.value.publisherNickname
    }
  }
  return null
})
const isAwaitingNewProvider = computed(() => order.value?.status === 'PENDING_CONFIRM')
const canSubmitCompletion = computed(() => (
  isProvider.value &&
  order.value?.status === 'IN_PROGRESS' &&
  (!order.value?.cancelReason || hasRejectedCancelRequest.value)
))
const canConfirmCompletion = computed(() => isPublisher.value && order.value?.status === 'PENDING_COMPLETION')
const canReviewOrder = computed(() => order.value?.status === 'COMPLETED')
const hasCurrentUserReviewed = computed(() => reviews.value.some((review) => review.reviewerId === auth.user?.id))
const isOrderTerminal = computed(() => Boolean(order.value && ['COMPLETED', 'REVIEWED', 'CANCELLED', 'TIMEOUT', 'PENDING_CONFIRM'].includes(order.value.status)))
const canEditTask = computed(() => Boolean(task.value && task.value.status === 'OPEN' && task.value.applicationCount === 0))

const statusClass: Record<string, string> = {
  IN_PROGRESS: 'info',
  PENDING_COMPLETION: 'warning',
  COMPLETED: 'warning',
  CANCELLED: 'danger',
  TIMEOUT: 'danger',
  DISPUTE: 'warning',
  REVIEWED: 'warning',
  PENDING_CONFIRM: 'success'
}
const latestStatusLog = computed(() => statusLogs.value[statusLogs.value.length - 1])
const providerCancelRequestLogs = computed(() => statusLogs.value.filter((log) =>
  log.operatorId === order.value?.serviceProviderId &&
  log.reason.includes('服务方申请取消')
))
const hasRejectedCancelRequest = computed(() => Boolean(
  isProvider.value &&
  latestStatusLog.value?.operatorId === order.value?.publisherId &&
  latestStatusLog.value?.reason.includes('发布方拒绝取消申请')
))
const cancelRequestLimitReached = computed(() => isProvider.value && providerCancelRequestLogs.value.length >= 2)
const hasActiveProviderCancelRequest = computed(() => Boolean(
  isProvider.value &&
  order.value?.cancelReason &&
  !hasRejectedCancelRequest.value
))
const hasPendingCancelRequest = computed(() => Boolean(
  isPublisher.value &&
  order.value?.status === 'IN_PROGRESS' &&
  order.value?.cancelReason
))
const shouldShowOrderSidebar = computed(() => Boolean(
  isAwaitingNewProvider.value && isPublisher.value
))

async function load() {
  error.value = ''
  loading.value = true
  try {
    order.value = await orderApi.get(orderId.value)
    statusLogs.value = await orderApi.statusLogs(orderId.value)
    reviews.value = await orderApi.reviews(orderId.value)
    if (isPublisher.value && isAwaitingNewProvider.value) {
      task.value = await taskApi.get(order.value.taskId)
      applications.value = await taskApi.applications(order.value.taskId)
    } else {
      task.value = undefined
      applications.value = []
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}

async function runAction(action: () => Promise<unknown>, messageText: string) {
  error.value = ''
  success.value = ''
  try {
    await action()
    success.value = messageText
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  }
}

async function handleCompletionProofChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  completionProofError.value = ''
  completionProofUploading.value = true
  try {
    uploadedCompletionProof.value = await fileApi.upload(file, 'ORDER_PROOF')
  } catch (err) {
    completionProofError.value = err instanceof Error ? err.message : '完成凭证上传失败'
  } finally {
    completionProofUploading.value = false
    input.value = ''
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
  if (!reportTargetUser.value) return
  error.value = ''
  reportUploadError.value = ''
  reportDialogOpen.value = true
}

function closeReportDialog() {
  if (reportSubmitting.value) return
  reportDialogOpen.value = false
}

async function submitReport() {
  if (!reportTargetUser.value || !reportReason.value.trim()) return

  error.value = ''
  success.value = ''
  reportSubmitting.value = true
  try {
    const evidenceIds = reportEvidenceFiles.value.map((item) => item.id)
    if (order.value?.status === 'TIMEOUT' && isPublisher.value) {
      await reportApi.submitTimeoutOrder(orderId.value, reportReason.value.trim(), evidenceIds)
    } else {
      await reportApi.submitUser(
        reportTargetUser.value.id,
        reportReason.value.trim(),
        evidenceIds
      )
    }
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

function openReviewDialog() {
  if (!canReviewOrder.value || hasCurrentUserReviewed.value) return
  error.value = ''
  reviewRating.value = 1
  reviewContent.value = ''
  reviewDialogOpen.value = true
}

function closeReviewDialog() {
  if (reviewSubmitting.value) return
  reviewDialogOpen.value = false
}

function openReviewRecordsDialog() {
  if (!canReviewOrder.value) return
  reviewRecordsDialogOpen.value = true
}

function closeReviewRecordsDialog() {
  reviewRecordsDialogOpen.value = false
}

async function submitReview() {
  if (!reviewContent.value.trim()) return
  reviewSubmitting.value = true
  error.value = ''
  success.value = ''
  try {
    await orderApi.submitReview(orderId.value, reviewRating.value, reviewContent.value.trim())
    success.value = '评价已提交'
    reviewDialogOpen.value = false
    reviewContent.value = ''
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '评价提交失败'
  } finally {
    reviewSubmitting.value = false
  }
}

function handleCompleteOrderClick() {
  if (!canSubmitCompletion.value) {
    error.value = '只有订单进行中时，服务方才能提交完成'
    success.value = ''
    return
  }
  error.value = ''
  completionProofError.value = ''
  uploadedCompletionProof.value = null
  completionDialogOpen.value = true
}

function closeCompletionDialog() {
  if (completionSubmitting.value || completionProofUploading.value) return
  completionDialogOpen.value = false
  uploadedCompletionProof.value = null
  completionProofError.value = ''
}

function openCompletionProofPreview() {
  if (!isPublisher.value || !order.value?.proofImageUrl) return
  completionProofPreviewOpen.value = true
}

function closeCompletionProofPreview() {
  completionProofPreviewOpen.value = false
}

async function submitCompletion() {
  if (!order.value || !uploadedCompletionProof.value) return
  completionSubmitting.value = true
  error.value = ''
  success.value = ''
  try {
    await orderApi.complete(order.value.id, uploadedCompletionProof.value.id)
    success.value = '已提交完成'
    completionDialogOpen.value = false
    uploadedCompletionProof.value = null
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交完成失败'
  } finally {
    completionSubmitting.value = false
  }
}

async function confirmOrderCompletion() {
  if (!order.value) return
  await runAction(() => orderApi.confirmCompletion(order.value!.id), '已确认完成')
}

function handleConfirmCompletionClick() {
  if (!canConfirmCompletion.value) {
    error.value = '需要服务方先提交完成后，发布方才能确认完成'
    success.value = ''
    return
  }
  dangerDialog.request({
    title: '确认任务已经完成？',
    description: '请先核对服务方提交的完成凭证。确认后订单将进入已完成状态，并可进行评价。',
    confirmText: '确认完成'
  }, confirmOrderCompletion)
}

function openCancelDialog() {
  if (!order.value) return
  if (isOrderTerminal.value) {
    error.value = '订单已完成、已评价或已取消，不能再取消'
    success.value = ''
    return
  }
  if (cancelRequestLimitReached.value) {
    error.value = '取消申请最多只能提交两次'
    success.value = ''
    return
  }
  error.value = ''
  success.value = ''
  cancelReason.value = ''
  cancelDialogOpen.value = true
}

function closeCancelDialog() {
  if (cancelSubmitting.value) return
  cancelDialogOpen.value = false
}

async function submitCancellation() {
  if (!order.value || !cancelReason.value.trim()) return
  const providerRequest = isProvider.value && !isPublisher.value
  cancelSubmitting.value = true
  error.value = ''
  success.value = ''
  try {
    await orderApi.cancel(order.value.id, cancelReason.value.trim())
    success.value = providerRequest ? '取消申请已提交，等待发布方处理' : '订单已取消'
    cancelDialogOpen.value = false
    cancelReason.value = ''
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '取消订单失败'
  } finally {
    cancelSubmitting.value = false
  }
}

function approveCancelRequest() {
  if (!order.value) return
  const currentOrder = order.value
  dangerDialog.request({
    title: '同意取消订单？',
    description: '同意后当前订单将被取消，原需求会重新开放并等待新的服务方。',
    confirmText: '同意取消'
  }, () => runAction(() => orderApi.approveCancelRequest(currentOrder.id), '已同意取消申请，需求已重新开放'))
}

async function rejectCancelRequest() {
  if (!order.value) return
  await runAction(() => orderApi.rejectCancelRequest(order.value!.id), '已拒绝取消申请')
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
  editForm.deadline = toDatetimeLocal(task.value.deadline)
  editForm.anonymous = task.value.anonymous
  editForm.categoryFields = task.value.categoryFields || {}
  editMode.value = true
}

async function saveTask() {
  if (!task.value || !order.value) return
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
      deadline: editForm.deadline,
      anonymous: editForm.anonymous,
      categoryFields: editForm.categoryFields
    })
    order.value = {
      ...order.value,
      taskTitle: task.value.title,
      taskDescription: task.value.description,
      campus: task.value.campus,
      rewardType: task.value.rewardType
    }
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
    title: '删除这条需求？',
    description: `“${currentTask.title}”删除后无法恢复。`,
    confirmText: '确认删除'
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

onMounted(load)
</script>

<template>
  <section class="order-detail-view">
    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="success" class="success-message">{{ success }}</p>
    <div v-if="loading" class="empty-state">正在加载订单</div>

    <div v-else-if="order" :class="['detail-layout', { 'single-column': !shouldShowOrderSidebar }]">
      <article class="panel grid">
        <div class="page-title">
          <div>
            <h1>{{ order.taskTitle }}</h1>
            <p>
              订单号 {{ order.id }} ·
              <RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">{{ order.publisherNickname }}</RouterLink>
              · {{ order.campus }} · {{ new Date(order.createdAt).toLocaleString() }}
            </p>
          </div>
          <span :class="['tag', statusClass[order.status]]">{{ orderStatusText[order.status] }}</span>
        </div>

        <p>{{ order.taskDescription }}</p>

        <div class="grid two">
          <div class="panel">
            <div class="participant-card-heading">
              <strong>发布者</strong>
              <button
                v-if="auth.isAuthenticated && isProvider && reportTargetUser"
                class="report-icon-button"
                type="button"
                title="举报发布者"
                aria-label="举报发布者"
                @click="openReportDialog"
              >
                <AlertTriangle aria-hidden="true" />
              </button>
            </div>
            <p><RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">{{ order.publisherNickname }}</RouterLink></p>
          </div>
          <div class="panel">
            <div class="participant-card-heading">
              <strong>服务方</strong>
              <button
                v-if="auth.isAuthenticated && isPublisher && reportTargetUser"
                class="report-icon-button"
                type="button"
                title="举报服务方"
                aria-label="举报服务方"
                @click="openReportDialog"
              >
                <AlertTriangle aria-hidden="true" />
              </button>
            </div>
            <p v-if="isAwaitingNewProvider" class="hint">暂无服务方，任务已回到待接单</p>
            <p v-else-if="order.serviceProviderId"><RouterLink :to="{ name: 'user-public-profile', params: { id: order.serviceProviderId } }">{{ order.serviceProviderNickname }}</RouterLink></p>
          </div>
        </div>

        <section class="grid">
          <h2>状态日志</h2>
          <ul class="timeline">
            <li v-for="log in statusLogs" :key="log.id">
              <strong>{{ orderStatusText[log.toStatus] }}</strong>
              <p>{{ log.reason }} · {{ log.operatorNickname }} · {{ new Date(log.createdAt).toLocaleString() }}</p>
            </li>
          </ul>
        </section>

        <RouterLink
          v-if="!isAwaitingNewProvider"
          class="button secondary contact-button"
          :to="{ name: 'order-chat', params: { id: order.id } }"
        >
          <MessageSquareText class="button-icon" aria-hidden="true" />
          <span>联系对方</span>
        </RouterLink>

        <section v-if="!isAwaitingNewProvider" class="main-order-actions grid">
          <div class="main-order-action-buttons">
            <button
              v-if="isPublisher && order.proofImageUrl"
              class="button ghost"
              type="button"
              @click="openCompletionProofPreview"
            >
              <Eye class="button-icon" aria-hidden="true" />
              <span>查看完成凭证</span>
            </button>
            <button
              v-if="isProvider"
              class="button secondary"
              :class="{ 'is-soft-disabled': !canSubmitCompletion }"
              :aria-disabled="!canSubmitCompletion"
              type="button"
              @click="handleCompleteOrderClick"
            >
              <MessageSquareText class="button-icon" aria-hidden="true" />
              <span>提交完成</span>
            </button>
            <button
              v-if="isPublisher"
              class="button primary"
              :class="{ 'is-soft-disabled': !canConfirmCompletion }"
              :aria-disabled="!canConfirmCompletion"
              type="button"
              @click="handleConfirmCompletionClick"
            >
              <CheckCheck class="button-icon" aria-hidden="true" />
              <span>确认完成</span>
            </button>
            <button
              v-if="(!isProvider || !hasActiveProviderCancelRequest) && !cancelRequestLimitReached"
              class="button danger"
              type="button"
              :class="{ 'is-cancel-unavailable': isOrderTerminal }"
              :aria-disabled="isOrderTerminal"
              @click="openCancelDialog"
            >
              <XCircle class="button-icon" aria-hidden="true" />
              <span>{{ isProvider && !isPublisher ? '取消服务' : '取消订单' }}</span>
            </button>
          </div>
          <div v-if="hasPendingCancelRequest" class="cancel-request-actions">
            <p class="hint">服务方申请取消订单，请审核。</p>
            <button class="button primary" type="button" @click="approveCancelRequest">
              <CheckCheck class="button-icon" aria-hidden="true" />
              <span>同意取消申请</span>
            </button>
            <button class="button secondary" type="button" @click="rejectCancelRequest">
              <XCircle class="button-icon" aria-hidden="true" />
              <span>拒绝取消申请</span>
            </button>
          </div>
          <p v-if="hasRejectedCancelRequest" class="hint">申请已被发布方驳回</p>
          <p v-if="cancelRequestLimitReached" class="hint">取消申请次数已达上限，不能再次提交</p>
          <div v-if="hasActiveProviderCancelRequest" class="hint">取消申请已提交，等待发布方处理</div>
          <div v-if="order.status === 'COMPLETED'" class="review-order-actions">
            <div class="main-order-action-buttons">
              <button
                class="button primary"
                type="button"
                :disabled="hasCurrentUserReviewed"
                @click="openReviewDialog"
              >
                <Star class="button-icon" aria-hidden="true" />
                <span>{{ hasCurrentUserReviewed ? '已提交评价' : '提交评价' }}</span>
              </button>
              <button class="button ghost" type="button" @click="openReviewRecordsDialog">
                <Eye class="button-icon" aria-hidden="true" />
                <span>查看评价记录</span>
              </button>
            </div>
          </div>
        </section>
      </article>

      <aside v-if="shouldShowOrderSidebar" class="grid">
        <section v-if="isAwaitingNewProvider && isPublisher && task" class="panel grid">
          <h2>需求管理</h2>
          <p v-if="!canEditTask" class="hint">只有未接单、且没有接单申请的开放需求可以编辑或删除。</p>
          <form v-if="editMode" class="grid" @submit.prevent="saveTask">
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
                <label for="edit-reward">报酬类型</label>
                <select id="edit-reward" v-model="editForm.rewardType" required>
                  <option value="CASH">现金</option>
                  <option value="NEGOTIABLE">面议</option>
                  <option value="CREDIT_INTENT">积分意向</option>
                </select>
              </div>
            </div>
            <div class="field">
              <label for="edit-deadline">截止时间</label>
              <input id="edit-deadline" v-model="editForm.deadline" type="datetime-local" required />
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
          <div v-else class="actions">
            <button
              class="button secondary"
              :class="{ 'is-soft-disabled': !canEditTask }"
              type="button"
              :aria-disabled="!canEditTask"
              @click="startEdit"
            >
              编辑
            </button>
            <button
              class="button danger"
              :class="{ 'is-soft-disabled': !canEditTask }"
              type="button"
              :disabled="deletingTask"
              :aria-disabled="!canEditTask"
              @click="deleteTask"
            >
              {{ deletingTask ? '删除中...' : '删除' }}
            </button>
          </div>
        </section>

        <section v-if="isAwaitingNewProvider && isPublisher" class="panel grid">
          <h2>接单申请</h2>
          <div v-if="!applications.length" class="empty-state">暂无申请</div>
          <div v-for="application in applications" :key="application.id" class="item-card">
            <div class="item-title">
              <h3><RouterLink :to="{ name: 'user-public-profile', params: { id: application.applicantId } }">{{ application.applicantNickname }}</RouterLink></h3>
              <span class="tag">{{ applicationStatusText[application.status] }}</span>
            </div>
            <p>{{ application.message }}</p>
            <p class="hint">信用分 {{ application.applicantCreditScore }} · {{ new Date(application.createdAt).toLocaleString() }}</p>
            <button
              v-if="task?.status === 'OPEN' && application.status === 'PENDING'"
              class="button secondary"
              type="button"
              :disabled="actionLoadingApplicationId === application.id"
              @click="confirmApplication(application.id)"
            >
              确认接单
            </button>
            <button
              v-if="task?.status === 'OPEN' && application.status === 'PENDING'"
              class="button danger"
              type="button"
              :disabled="actionLoadingApplicationId === application.id"
              @click="rejectApplication(application.id)"
            >
              拒绝申请
            </button>
          </div>
        </section>

      </aside>
    </div>
    <Teleport to="body">
      <div v-if="reviewDialogOpen && order" class="report-modal-backdrop" role="presentation" @click.self="closeReviewDialog">
        <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="review-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">订单评价</p>
              <h2 id="review-modal-title">提交评价</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭评价弹窗" :disabled="reviewSubmitting" @click="closeReviewDialog">
              <X aria-hidden="true" />
            </button>
          </header>
          <form class="report-modal-form grid" @submit.prevent="submitReview">
            <div class="field">
              <label>评分</label>
              <div class="stars" aria-label="评分">
                <button
                  v-for="score in 5"
                  :key="score"
                  :class="['star-button', score <= reviewRating ? 'active' : '']"
                  type="button"
                  :aria-label="`${score} 分`"
                  @click="reviewRating = score"
                >
                  {{ score }}
                </button>
              </div>
            </div>
            <div class="field">
              <label for="review-content">评价内容</label>
              <textarea id="review-content" v-model.trim="reviewContent" placeholder="填写对本次协作的评价" maxlength="500" required />
            </div>
            <p v-if="error" class="error-message">{{ error }}</p>
            <div class="report-modal-actions">
              <button class="button ghost" type="button" :disabled="reviewSubmitting" @click="closeReviewDialog">取消</button>
              <button class="button primary" type="submit" :disabled="reviewSubmitting || !reviewContent.trim()">
                {{ reviewSubmitting ? '提交中...' : '提交评价' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="reviewRecordsDialogOpen && order" class="report-modal-backdrop" role="presentation" @click.self="closeReviewRecordsDialog">
        <section class="report-modal review-records-modal" role="dialog" aria-modal="true" aria-labelledby="review-records-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">协作反馈</p>
              <h2 id="review-records-modal-title">评价记录</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭评价记录" @click="closeReviewRecordsDialog">
              <X aria-hidden="true" />
            </button>
          </header>
          <div class="review-records-list">
            <div v-if="!reviews.length" class="empty-state">暂无评价</div>
            <article v-for="review in reviews" :key="review.id" class="item-card">
              <strong>
                <RouterLink :to="{ name: 'user-public-profile', params: { id: review.reviewerId } }">{{ review.reviewerNickname }}</RouterLink>
                →
                <RouterLink :to="{ name: 'user-public-profile', params: { id: review.revieweeId } }">{{ review.revieweeNickname }}</RouterLink>
              </strong>
              <p>{{ review.rating }} 分 · {{ review.content }}</p>
            </article>
          </div>
          <div class="report-modal-actions">
            <button class="button ghost" type="button" @click="closeReviewRecordsDialog">关闭</button>
          </div>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div
        v-if="completionProofPreviewOpen && order?.proofImageUrl"
        class="report-modal-backdrop"
        role="presentation"
        @click.self="closeCompletionProofPreview"
      >
        <section class="report-modal completion-proof-viewer" role="dialog" aria-modal="true" aria-labelledby="completion-proof-viewer-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">服务方提交</p>
              <h2 id="completion-proof-viewer-title">完成凭证</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭完成凭证" @click="closeCompletionProofPreview">
              <X aria-hidden="true" />
            </button>
          </header>
          <img :src="resolveAssetUrl(order.proofImageUrl)" alt="服务方提交的完成凭证" />
          <div class="report-modal-actions">
            <button class="button ghost" type="button" @click="closeCompletionProofPreview">关闭</button>
          </div>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="completionDialogOpen && order" class="report-modal-backdrop" role="presentation" @click.self="closeCompletionDialog">
        <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="completion-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">订单履约</p>
              <h2 id="completion-modal-title">提交完成凭证</h2>
            </div>
            <button
              class="report-modal-close"
              type="button"
              aria-label="关闭完成凭证弹窗"
              :disabled="completionSubmitting || completionProofUploading"
              @click="closeCompletionDialog"
            >
              <X aria-hidden="true" />
            </button>
          </header>
          <form class="report-modal-form grid" @submit.prevent="submitCompletion">
            <label class="button ghost upload-trigger">
              <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleCompletionProofChange" />
              <span>{{ completionProofUploading ? '上传中...' : uploadedCompletionProof ? '重新上传凭证' : '上传完成凭证' }}</span>
            </label>
            <p class="hint">必须上传能够证明任务已经完成的截图或照片，每张不超过 5MB。</p>
            <p v-if="completionProofError" class="error-message">{{ completionProofError }}</p>
            <p v-if="error" class="error-message">{{ error }}</p>
            <div v-if="uploadedCompletionProof" class="upload-card completion-proof-preview">
              <img :src="resolveAssetUrl(uploadedCompletionProof.url)" :alt="uploadedCompletionProof.fileName" />
              <div class="upload-card-meta">
                <strong>{{ uploadedCompletionProof.fileName }}</strong>
                <button class="button ghost" type="button" :disabled="completionSubmitting" @click="uploadedCompletionProof = null">移除</button>
              </div>
            </div>
            <div class="report-modal-actions">
              <button class="button ghost" type="button" :disabled="completionSubmitting || completionProofUploading" @click="closeCompletionDialog">取消</button>
              <button class="button primary" type="submit" :disabled="completionSubmitting || completionProofUploading || !uploadedCompletionProof">
                {{ completionSubmitting ? '提交中...' : '确认提交' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="cancelDialogOpen && order" class="report-modal-backdrop" role="presentation" @click.self="closeCancelDialog">
        <section class="report-modal cancel-order-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-order-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">订单取消</p>
              <h2 id="cancel-order-modal-title">{{ isProvider && !isPublisher ? '申请取消服务' : '取消订单' }}</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭取消弹窗" :disabled="cancelSubmitting" @click="closeCancelDialog">
              <X aria-hidden="true" />
            </button>
          </header>
          <form class="report-modal-form grid" @submit.prevent="submitCancellation">
            <div class="field">
              <label for="cancel-order-reason">取消原因</label>
              <textarea
                id="cancel-order-reason"
                v-model.trim="cancelReason"
                placeholder="请填写取消原因"
                maxlength="500"
                required
              />
            </div>
            <p class="hint">
              {{ isProvider && !isPublisher ? '提交后将等待发布方审核。' : '订单取消后将无法继续履约。' }}
            </p>
            <p v-if="error" class="error-message">{{ error }}</p>
            <div class="report-modal-actions">
              <button class="button ghost" type="button" :disabled="cancelSubmitting" @click="closeCancelDialog">返回</button>
              <button class="button danger" type="submit" :disabled="cancelSubmitting || !cancelReason.trim()">
                {{ cancelSubmitting ? '提交中...' : (isProvider && !isPublisher ? '提交申请' : '确认取消') }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="reportDialogOpen && reportTargetUser" class="report-modal-backdrop" role="presentation" @click.self="closeReportDialog">
        <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="report-modal-title">
          <header class="report-modal-header">
            <div>
              <p class="report-modal-eyebrow">账号举报</p>
              <h2 id="report-modal-title">举报 {{ reportTargetUser.nickname }}</h2>
            </div>
            <button class="report-modal-close" type="button" aria-label="关闭举报弹窗" :disabled="reportSubmitting" @click="closeReportDialog">
              <X aria-hidden="true" />
            </button>
          </header>
          <form class="report-modal-form grid" @submit.prevent="submitReport">
            <div class="field">
              <label for="report-reason">举报原因</label>
              <textarea id="report-reason" v-model.trim="reportReason" placeholder="填写举报原因或补充说明" maxlength="300" required />
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
.order-detail-view {
  --order-green: #ffb454;
  --order-dark: #191a23;
  --order-grey: #f3f3f3;
}

.detail-layout {
  align-items: start;
  gap: 26px;
}

.detail-layout.single-column {
  grid-template-columns: minmax(0, 1fr);
}

.panel,
.item-card,
.empty-state {
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
    radial-gradient(circle at 92% 12%, var(--order-green) 0 74px, transparent 75px),
    #ffffff;
}

.detail-layout > article.panel > p {
  max-width: 760px;
  color: #343743;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.7;
}

.detail-layout > aside {
  gap: 20px;
}

.detail-layout > aside > .panel {
  padding: 24px;
  border-radius: 24px;
}

.page-title {
  align-items: flex-start;
  margin-bottom: 8px;
}

.page-title h1 {
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: transparent;
  color: #000000;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
  font-size: 32px;
  -webkit-text-fill-color: #000000;
  box-shadow: none;
}

.panel h2 {
  width: max-content;
  max-width: 100%;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
  font-size: 22px;
}

.page-title p {
  margin-top: 12px;
  color: #4a4e5b;
  font-size: 14px;
  font-weight: 800;
}

.page-title > .tag {
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
  box-shadow: none;
}

.grid.two > .panel {
  padding: 22px;
  border-radius: 22px;
  background: var(--order-grey);
  box-shadow: none;
}

.grid.two > .panel strong {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 900;
}

.timeline {
  display: grid;
  gap: 12px;
  padding-left: 0;
  list-style: none;
}

.timeline li {
  position: relative;
  padding: 14px 16px 14px 42px;
  border: 2px solid #000000;
  border-radius: 18px;
  background: var(--order-grey);
  box-shadow: none;
}

.timeline li::before {
  content: '';
  position: absolute;
  left: 16px;
  top: 18px;
  width: 12px;
  height: 12px;
  border: 2px solid #000000;
  border-radius: 50%;
  background: var(--order-green);
}

.timeline strong,
.message-bubble strong,
.item-card strong {
  color: #000000;
  font-weight: 900;
}

.timeline p {
  margin-top: 5px;
  color: #565b6b;
  font-weight: 700;
}

.messages {
  gap: 12px;
}

.message-bubble {
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: none;
}

.message-bubble p {
  color: #343743;
  font-weight: 700;
}

.field input,
.field textarea,
.field select {
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-weight: 800;
  box-shadow: none;
}

.field input:focus,
.field textarea:focus,
.field select:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(255, 180, 84, 0.48);
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: none;
}

.participant-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 2px solid #000000;
  border-radius: 50%;
  background: #ffffff;
  color: #c1121f;
  cursor: pointer;
  transition: transform var(--transition-fast), background var(--transition-fast);
}

.report-icon-button svg {
  width: 17px;
  height: 17px;
}

.report-icon-button:hover {
  background: #ffe8e8;
  transform: translateY(-2px);
}

.report-icon-button:focus-visible {
  outline: 3px solid rgba(255, 180, 84, 0.65);
  outline-offset: 2px;
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
  font-size: 26px;
  font-weight: 900;
}

.report-modal-close {
  display: grid;
  place-items: center;
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

.report-modal .star-button.active,
.report-modal .star-button:hover {
  background: #ffb454;
  color: #000000;
}

.review-records-modal {
  width: min(660px, 100%);
}

.review-records-list {
  display: grid;
  gap: 14px;
}

.review-records-list .item-card,
.review-records-list .empty-state {
  padding: 18px;
  border: 2px solid #000000;
  border-radius: 18px;
  background: #f3f3f3;
}

.review-records-list .item-card p {
  margin-top: 8px;
  color: #4a4e5b;
  font-weight: 700;
}

.review-records-list a {
  color: #4f46e5;
  font-weight: 900;
}

.completion-proof-preview {
  overflow: hidden;
}

.completion-proof-preview img {
  width: 100%;
  max-height: 280px;
  object-fit: contain;
  background: #f3f3f3;
}

.completion-proof-viewer {
  width: min(760px, 100%);
}

.completion-proof-viewer > img {
  display: block;
  width: 100%;
  max-height: min(62vh, 620px);
  object-fit: contain;
  border: 2px solid #000000;
  border-radius: 18px;
  background: #f3f3f3;
}

.report-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
  padding-top: 18px;
  border-top: 2px solid #000000;
}

.report-modal-actions .button.primary {
  border-color: #000000;
  background: #191a23;
  color: #ffffff;
}

.report-modal-actions .button.primary:hover:not(:disabled) {
  background: #ffb454;
  color: #000000;
}

.report-modal-actions .button.primary:disabled {
  border-color: #9ca3af;
  background: #e5e7eb;
  color: #6b7280;
  opacity: 1;
}

@media (max-width: 620px) {
  .report-modal-backdrop {
    padding: 14px;
  }

  .report-modal {
    padding: 22px;
    border-radius: 22px;
  }

  .report-modal-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}

.contact-button {
  width: max-content;
  min-width: 150px;
  padding: 12px 22px;
}

.main-order-actions {
  margin-top: 6px;
  padding-top: 24px;
  border-top: 2px solid #000000;
}

.main-order-action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.main-order-action-buttons .button {
  min-width: 150px;
}

.main-order-actions .upload-card {
  margin-top: 12px;
}

.review-order-actions {
  margin-top: 8px;
  padding-top: 24px;
  border-top: 2px solid #000000;
}

.button.primary,
.button.secondary {
  background: var(--order-dark);
  color: #ffffff;
}

.button.primary:hover,
.button.secondary:hover {
  background: var(--order-green);
  color: #000000;
  box-shadow: none;
}

.button.ghost {
  background: #ffffff;
  color: #000000;
}

.button.ghost:hover {
  background: var(--order-green);
  color: #000000;
}

.button.danger {
  border-color: #000000;
  background: #ffffff;
  color: #c1121f;
}

.button.danger:hover {
  background: #ffe8e8;
  color: #9f0f19;
}

.item-card {
  padding: 18px;
  border-radius: 22px;
  background: var(--order-grey);
}

.item-card .tag {
  border: 2px solid #000000;
  background: var(--order-green);
  color: #000000;
  font-weight: 900;
}

.item-title {
  align-items: flex-start;
}

.hint {
  color: #6f7485;
  font-weight: 700;
}

.stars {
  gap: 8px;
}

.star-button {
  border: 2px solid #000000;
  border-radius: 12px;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
  box-shadow: none;
}

.star-button.active,
.star-button:hover {
  background: var(--order-green);
  color: #000000;
}

.upload-card {
  border: 2px solid #000000;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: none;
}

.success-message,
.error-message {
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: none;
  font-weight: 800;
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

.button.is-cancel-unavailable {
  border-color: rgba(148, 163, 184, 0.24);
  background: linear-gradient(135deg, #e5e7eb, #cbd5e1);
  color: #64748b;
  box-shadow: none;
  cursor: not-allowed;
}

.button.is-cancel-unavailable:hover {
  transform: none;
  box-shadow: none;
}

.cancel-request-actions {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--warning-bg), rgba(245, 158, 11, 0.04));
}

.page-title p a,
.panel p a,
.item-card h3 a {
  color: var(--primary-600);
  font-weight: 600;
}

.page-title p a:hover,
.panel p a:hover,
.item-card h3 a:hover {
  color: var(--primary-700);
}
</style>




