<script setup lang="ts">
import { CheckCheck, MessageSquareText, Send, Star, XCircle } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { fileApi, orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { orderStatusText } from '@/types'
import type { OrderDetail, OrderStatusLog, ReviewItem, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const auth = useAuthStore()

const order = ref<OrderDetail>()
const statusLogs = ref<OrderStatusLog[]>([])
const reviews = ref<ReviewItem[]>([])
const error = ref('')
const success = ref('')
const loading = ref(false)
const message = ref('')
const cancelReason = ref('')
const reviewRating = ref(5)
const reviewContent = ref('')
const chatImageUploading = ref(false)
const chatImageError = ref('')
const uploadedChatImage = ref<UploadedFileItem | null>(null)

const orderId = computed(() => Number(route.params.id))
const isPublisher = computed(() => order.value?.publisherId === auth.user?.id)
const isProvider = computed(() => order.value?.serviceProviderId === auth.user?.id)
const canSubmitCompletion = computed(() => isProvider.value && order.value?.status === 'IN_PROGRESS' && !order.value?.cancelReason)
const canConfirmCompletion = computed(() => isPublisher.value && order.value?.status === 'PENDING_COMPLETION')
const isOrderTerminal = computed(() => Boolean(order.value && ['COMPLETED', 'REVIEWED', 'CANCELLED', 'PENDING_CONFIRM'].includes(order.value.status)))
const latestStatusLog = computed(() => statusLogs.value[statusLogs.value.length - 1])
const hasPendingCancelRequest = computed(() => Boolean(
  isPublisher.value &&
  order.value?.status === 'IN_PROGRESS' &&
  latestStatusLog.value?.toStatus === 'IN_PROGRESS' &&
  (
    latestStatusLog.value?.operatorId === order.value?.serviceProviderId ||
    latestStatusLog.value?.reason.includes('申请取消')
  )
))

async function load() {
  error.value = ''
  loading.value = true
  try {
    order.value = await orderApi.get(orderId.value)
    statusLogs.value = await orderApi.statusLogs(orderId.value)
    reviews.value = await orderApi.reviews(orderId.value)
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

onMounted(load)
</script>

<template>
  <section>
    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="success" class="success-message">{{ success }}</p>
    <div v-if="loading" class="empty-state">正在加载订单</div>

    <div v-else-if="order" class="detail-layout">
      <article class="panel grid">
        <div class="page-title">
          <div>
            <h1>{{ order.taskTitle }}</h1>
            <p>订单号 {{ order.id }} · {{ order.campus }} · {{ new Date(order.createdAt).toLocaleString() }}</p>
          </div>
          <span class="tag success">{{ orderStatusText[order.status] }}</span>
        </div>

        <p>{{ order.taskDescription }}</p>

        <div class="grid two">
          <div class="panel">
            <strong>发布者</strong>
            <p><RouterLink :to="{ name: 'user-public-profile', params: { id: order.publisherId } }">{{ order.publisherNickname }}</RouterLink></p>
          </div>
          <div class="panel">
            <strong>服务方</strong>
            <p><RouterLink :to="{ name: 'user-public-profile', params: { id: order.serviceProviderId } }">{{ order.serviceProviderNickname }}</RouterLink></p>
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
          <form class="actions" @submit.prevent="sendMessage">
            <div class="field" style="flex:1;margin-bottom:0">
              <input v-model.trim="message" placeholder="输入订单留言" />
            </div>
            <button class="button secondary" type="submit">
              <Send class="button-icon" aria-hidden="true" />
              <span>发送</span>
            </button>
          </form>
          <div class="grid">
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
        <section class="panel grid">
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
          <div v-if="!isProvider || !order?.cancelReason" class="field">
            <input v-model.trim="cancelReason" placeholder="取消原因" />
          </div>
          <button
            v-if="!isProvider || !order?.cancelReason"
            class="button danger"
            type="button"
            :class="{ 'is-soft-disabled': !cancelReason && !isOrderTerminal }"
            :disabled="isOrderTerminal"
            @click="cancelOrder"
          >
            <XCircle class="button-icon" aria-hidden="true" />
            <span>取消订单</span>
          </button>
          <div v-if="isProvider && order?.cancelReason" class="hint">取消申请已提交，等待发布方处理</div>
        </section>

        <section class="panel grid">
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
          <button class="button primary" type="button" :disabled="order.status !== 'COMPLETED'" @click="submitReview">
            <Star class="button-icon" aria-hidden="true" />
            <span>提交评价</span>
          </button>
        </section>

        <section class="panel grid">
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
.button.is-soft-disabled {
  opacity: 0.48;
  filter: grayscale(0.18);
  box-shadow: none;
}

.button.is-soft-disabled:hover {
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
</style>
