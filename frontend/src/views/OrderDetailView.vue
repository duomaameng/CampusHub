<script setup lang="ts">
import { CheckCheck, MessageSquareText, Send, Star, XCircle } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { fileApi, orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { orderStatusText } from '@/types'
import type { OrderDetail, ReviewItem, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const auth = useAuthStore()

const order = ref<OrderDetail>()
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

async function load() {
  error.value = ''
  loading.value = true
  try {
    order.value = await orderApi.get(orderId.value)
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

async function confirmOrderCompletion() {
  if (!order.value) return
  await runAction(() => orderApi.confirmCompletion(order.value!.id), '已确认完成')
}

async function cancelOrder() {
  if (!order.value) return
  await runAction(() => orderApi.cancel(order.value!.id, cancelReason.value), '订单已取消')
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
            <li v-for="log in order.statusLogs" :key="log.id">
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
            class="button secondary"
            type="button"
            :disabled="!isProvider || order.status !== 'IN_PROGRESS'"
            @click="completeOrder"
          >
            <MessageSquareText class="button-icon" aria-hidden="true" />
            <span>提交完成</span>
          </button>
          <button
            class="button primary"
            type="button"
            :disabled="!isPublisher || order.status !== 'PENDING_COMPLETION'"
            @click="confirmOrderCompletion"
          >
            <CheckCheck class="button-icon" aria-hidden="true" />
            <span>确认完成</span>
          </button>
          <div class="field">
            <input v-model.trim="cancelReason" placeholder="取消原因" />
          </div>
          <button
            class="button danger"
            type="button"
            :disabled="!cancelReason || ['COMPLETED', 'REVIEWED', 'CANCELLED'].includes(order.status)"
            @click="cancelOrder"
          >
            <XCircle class="button-icon" aria-hidden="true" />
            <span>取消订单</span>
          </button>
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
