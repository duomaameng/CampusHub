<script setup lang="ts">
import { CheckCheck, MessageSquareText, Send, Star, XCircle } from '@lucide/vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { fileApi, orderApi, reportApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusText, orderStatusText } from '@/types'
import type { ApplicationItem, OrderDetail, OrderStatusLog, ReviewItem, TaskItem, TaskUpdatePayload, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

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
const message = ref('')
const cancelReason = ref('')
const reviewRating = ref(1)
const reviewContent = ref('')
const chatImageUploading = ref(false)
const chatImageError = ref('')
const uploadedChatImage = ref<UploadedFileItem | null>(null)
const reportReason = ref('')
const reportUploadError = ref('')
const reportSubmitting = ref(false)
const reportEvidenceUploading = ref(false)
const reportEvidenceFiles = ref<UploadedFileItem[]>([])
const editMode = ref(false)
const savingTask = ref(false)
const deletingTask = ref(false)
const deleteConfirming = ref(false)
const actionLoadingApplicationId = ref<number | null>(null)
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
const shouldShowReviews = computed(() => Boolean(order.value && ['COMPLETED', 'REVIEWED'].includes(order.value.status)))
const isOrderTerminal = computed(() => Boolean(order.value && ['COMPLETED', 'REVIEWED', 'CANCELLED', 'PENDING_CONFIRM'].includes(order.value.status)))
const canEditTask = computed(() => Boolean(task.value && task.value.status === 'OPEN' && task.value.applicationCount === 0))

const statusClass: Record<string, string> = {
  IN_PROGRESS: 'info',
  PENDING_COMPLETION: 'warning',
  COMPLETED: 'warning',
  CANCELLED: 'danger',
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

async function sendMessage() {
  if (!message.value.trim()) return
  await runAction(() => orderApi.sendMessage(orderId.value, message.value.trim()), '消息已发送')
  message.value = ''
}

async function handleChatImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  chatImageError.value = ''
  chatImageUploading.value = true
  try {
    uploadedChatImage.value = await fileApi.upload(file, 'CHAT_IMAGE')
  } catch (err) {
    chatImageError.value = err instanceof Error ? err.message : '聊天图片上传失败'
  } finally {
    chatImageUploading.value = false
    input.value = ''
  }
}

async function sendImageMessage() {
  if (!uploadedChatImage.value) return
  await runAction(() => orderApi.sendImage(orderId.value, uploadedChatImage.value!.id), '图片消息已发送')
  uploadedChatImage.value = null
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
  if (!reportTargetUser.value || !reportReason.value.trim()) return

  error.value = ''
  success.value = ''
  reportSubmitting.value = true
  try {
    await reportApi.submitUser(
      reportTargetUser.value.id,
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

async function submitReview() {
  await runAction(() => orderApi.submitReview(orderId.value, reviewRating.value, reviewContent.value), '评价已提交')
  reviewContent.value = ''
}

async function completeOrder() {
  if (!order.value) return
  await runAction(() => orderApi.complete(order.value!.id), '已提交完成')
}

async function handleCompleteOrderClick() {
  if (!canSubmitCompletion.value) {
    error.value = '只有订单进行中时，服务方才能提交完成'
    success.value = ''
    return
  }
  await completeOrder()
}

async function confirmOrderCompletion() {
  if (!order.value) return
  await runAction(() => orderApi.confirmCompletion(order.value!.id), '已确认完成')
}

async function handleConfirmCompletionClick() {
  if (!canConfirmCompletion.value) {
    error.value = '需要服务方先提交完成后，发布方才能确认完成'
    success.value = ''
    return
  }
  await confirmOrderCompletion()
}

async function cancelOrder() {
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
  if (!cancelReason.value.trim()) {
    error.value = '请先填写取消原因'
    success.value = ''
    return
  }
  const successText = isProvider.value && !isPublisher.value ? 'Cancel request submitted, waiting for publisher approval' : 'Order cancelled'
  await runAction(() => orderApi.cancel(order.value!.id, cancelReason.value), successText)
  cancelReason.value = ''
}

async function approveCancelRequest() {
  if (!order.value) return
  await runAction(() => orderApi.approveCancelRequest(order.value!.id), 'Cancel request approved, task reopened')
}

async function rejectCancelRequest() {
  if (!order.value) return
  await runAction(() => orderApi.rejectCancelRequest(order.value!.id), 'Cancel request rejected')
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
  deleteConfirming.value = false
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
  if (!deleteConfirming.value) {
    deleteConfirming.value = true
    return
  }
  error.value = ''
  deletingTask.value = true
  try {
    await taskApi.remove(task.value.id)
    router.push('/tasks')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '需求删除失败'
  } finally {
    deletingTask.value = false
    deleteConfirming.value = false
  }
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

    <div v-else-if="order" class="detail-layout">
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
            <strong>发布者</strong>
            <p><RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">{{ order.publisherNickname }}</RouterLink></p>
          </div>
          <div class="panel">
            <strong>服务方</strong>
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

        <section class="grid">
          <h2>订单留言</h2>
          <div class="messages">
            <div v-if="!order.messages.length" class="hint">暂无留言</div>
            <div v-for="item in order.messages" :key="item.id" class="message-bubble">
              <strong><RouterLink :to="{ name: 'user-public-profile', params: { id: item.senderId } }">{{ item.senderNickname }}</RouterLink></strong>
              <p v-if="item.content">{{ item.content }}</p>
              <img v-if="item.imageUrl" class="message-image" :src="resolveAssetUrl(item.imageUrl)" alt="聊天图片" />
              <span class="hint">{{ new Date(item.createdAt).toLocaleString() }}</span>
            </div>
          </div>
          <form v-if="!isAwaitingNewProvider" class="actions" @submit.prevent="sendMessage">
            <div class="field" style="flex:1;margin-bottom:0">
              <input v-model.trim="message" placeholder="输入订单留言" />
            </div>
            <button class="button secondary" type="submit">
              <Send class="button-icon" aria-hidden="true" />
              <span>发送</span>
            </button>
          </form>
          <div v-if="!isAwaitingNewProvider" class="grid">
            <label class="button ghost upload-trigger">
              <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleChatImageChange" />
              <span>{{ chatImageUploading ? '上传中...' : '上传聊天图片' }}</span>
            </label>
            <p v-if="chatImageError" class="error-message">{{ chatImageError }}</p>
            <div v-if="uploadedChatImage" class="upload-card inline">
              <img :src="resolveAssetUrl(uploadedChatImage.url)" :alt="uploadedChatImage.fileName" />
              <div class="upload-card-meta">
                <strong>{{ uploadedChatImage.fileName }}</strong>
                <div class="actions">
                  <button class="button secondary" type="button" @click="sendImageMessage">发送图片</button>
                  <button class="button ghost" type="button" @click="uploadedChatImage = null">取消</button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </article>

      <aside class="grid">
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
              {{ deletingTask ? '删除中...' : deleteConfirming ? '再次点击确认删除' : '删除' }}
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

        <section v-if="!isAwaitingNewProvider" class="panel grid">
          <h2>订单操作</h2>
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
          <div v-if="(!isProvider || !hasActiveProviderCancelRequest) && !cancelRequestLimitReached" class="field">
            <input v-model.trim="cancelReason" placeholder="取消原因" />
          </div>
          <button
            v-if="(!isProvider || !hasActiveProviderCancelRequest) && !cancelRequestLimitReached"
            class="button danger"
            type="button"
            :class="{
              'is-soft-disabled': !cancelReason && !isOrderTerminal,
              'is-cancel-unavailable': isOrderTerminal
            }"
            :aria-disabled="isOrderTerminal"
            @click="cancelOrder"
          >
            <XCircle class="button-icon" aria-hidden="true" />
            <span>{{ isProvider && !isPublisher ? '取消服务' : '取消订单' }}</span>
          </button>
          <div v-if="hasActiveProviderCancelRequest" class="hint">取消申请已提交，等待发布方处理</div>
        </section>

        <section v-if="auth.isAuthenticated && reportTargetUser" class="panel grid">
          <h2>举报账号</h2>
          <p class="hint">
            举报对象：
            <RouterLink :to="{ name: 'user-public-profile', params: { id: reportTargetUser.id } }">
              {{ reportTargetUser.nickname }}
            </RouterLink>
          </p>
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

        <section v-if="canReviewOrder" class="panel grid">
          <h2>提交评价</h2>
          <div class="stars" aria-label="评分">
            <button
              v-for="score in 5"
              :key="score"
              :class="['star-button', score <= reviewRating ? 'active' : '']"
              type="button"
              @click="reviewRating = score"
            >
              {{ score }}
            </button>
          </div>
          <div class="field">
            <textarea v-model.trim="reviewContent" placeholder="评价内容" maxlength="500" />
          </div>
          <button class="button primary" type="button" @click="submitReview">
            <Star class="button-icon" aria-hidden="true" />
            <span>提交评价</span>
          </button>
        </section>

        <section v-if="shouldShowReviews" class="panel grid">
          <h2>评价记录</h2>
          <div v-if="!reviews.length" class="hint">暂无评价</div>
          <div v-for="review in reviews" :key="review.id" class="item-card">
            <strong><RouterLink :to="{ name: 'user-public-profile', params: { id: review.reviewerId } }">{{ review.reviewerNickname }}</RouterLink> -> <RouterLink :to="{ name: 'user-public-profile', params: { id: review.revieweeId } }">{{ review.revieweeNickname }}</RouterLink></strong>
            <p>{{ review.rating }} 分 · {{ review.content }}</p>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.order-detail-view {
  --order-green: #b9ff66;
  --order-dark: #191a23;
  --order-grey: #f3f3f3;
}

.detail-layout {
  align-items: start;
  gap: 26px;
}

.panel,
.item-card,
.empty-state {
  border: 2px solid #000000;
  border-radius: 26px;
  background: #ffffff;
  box-shadow: 0 6px 0 #000000;
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

.page-title h1,
.panel h2 {
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  background: var(--order-green);
  color: #000000;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
  -webkit-text-fill-color: #000000;
}

.page-title h1 {
  font-size: 32px;
}

.panel h2 {
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
  box-shadow: 0 3px 0 #000000;
}

.grid.two > .panel {
  padding: 22px;
  border-radius: 22px;
  background: var(--order-grey);
  box-shadow: 0 4px 0 #000000;
}

.grid.two > .panel strong {
  display: inline-block;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--order-green);
  color: #000000;
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
  box-shadow: 0 3px 0 #000000;
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
  box-shadow: 0 3px 0 #000000;
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
  box-shadow: 0 0 0 3px rgba(185, 255, 102, 0.48);
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: 0 4px 0 #000000;
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
  box-shadow: 0 5px 0 #000000;
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
  box-shadow: 0 3px 0 #000000;
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
  box-shadow: 0 3px 0 #000000;
}

.success-message,
.error-message {
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
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
