<script setup lang="ts">
import { ArrowLeft, ImagePlus, MessageSquareText, Send, X } from '@lucide/vue'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { fileApi, orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { orderStatusText } from '@/types'
import type { OrderDetail, OrderMessage, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

type ConversationItem = {
  orderId: number
  taskTitle: string
  participantId?: number | null
  participantNickname: string
  participantAvatarUrl?: string
  participantRole: string
  status: OrderDetail['status']
  lastMessageText: string
  lastMessageAt: string
  messageCount: number
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref<OrderDetail>()
const conversations = ref<ConversationItem[]>([])
const loading = ref(false)
const sending = ref(false)
const imageUploading = ref(false)
const error = ref('')
const uploadError = ref('')
const message = ref('')
const uploadedImage = ref<UploadedFileItem | null>(null)
const messagesPanel = ref<HTMLElement | null>(null)
let loadRequestId = 0

const orderId = computed(() => Number(route.params.id))
const hasOrderId = computed(() => Number.isFinite(orderId.value))
const isPublisher = computed(() => order.value?.publisherId === auth.user?.id)
const isAwaitingNewProvider = computed(() => order.value?.status === 'PENDING_CONFIRM')
const chatTarget = computed(() => {
  if (!order.value) return null
  if (isPublisher.value) {
    return {
      id: order.value.serviceProviderId,
      nickname: order.value.serviceProviderNickname || '服务方',
      avatarUrl: order.value.serviceProviderAvatarUrl,
      role: '服务方'
    }
  }
  return {
    id: order.value.publisherId,
    nickname: order.value.publisherNickname,
    avatarUrl: order.value.publisherAvatarUrl,
    role: '发布者'
  }
})
const sortedMessages = computed(() => [...(order.value?.messages || [])].sort((a, b) =>
  new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
))

async function load() {
  const requestId = ++loadRequestId
  const requestedOrderId = orderId.value
  error.value = ''
  loading.value = true
  try {
    if (!hasOrderId.value) {
      await loadConversations()
      if (conversations.value.length) {
        await router.replace({ name: 'order-chat', params: { id: conversations.value[0].orderId } })
      }
      return
    }
    const nextOrder = await orderApi.get(requestedOrderId)
    if (requestId !== loadRequestId) return

    order.value = nextOrder
    if (!conversations.value.length) await loadConversations()
    await scrollToBottom()
  } catch (err) {
    if (requestId !== loadRequestId) return
    error.value = err instanceof Error ? err.message : '聊天加载失败'
  } finally {
    if (requestId === loadRequestId) loading.value = false
  }
}

async function loadConversations() {
  const result = await orderApi.list({ page: 1, size: 50 })
  const details = await Promise.all(
    result.records.map(async (item) => {
      try {
        return await orderApi.get(item.id)
      } catch {
        return null
      }
    })
  )

  const items = details
    .filter((item): item is OrderDetail => Boolean(item))
    .map(buildConversationItem)
    .filter((item): item is ConversationItem => Boolean(item))
    .filter((item) => item.messageCount > 0)
    .sort((a, b) => new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime())

  conversations.value = items
}

function buildConversationItem(item: OrderDetail): ConversationItem | null {
  const currentUserId = auth.user?.id
  const currentUserIsPublisher = item.publisherId === currentUserId
  const participantId = currentUserIsPublisher ? item.serviceProviderId : item.publisherId
  const participantNickname = currentUserIsPublisher
    ? item.serviceProviderNickname || '服务方'
    : item.publisherNickname
  const participantAvatarUrl = currentUserIsPublisher
    ? item.serviceProviderAvatarUrl
    : item.publisherAvatarUrl
  const participantRole = currentUserIsPublisher ? '服务方' : '发布者'
  const orderedMessages = [...item.messages].sort((a, b) =>
    new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  )
  const latest = orderedMessages[orderedMessages.length - 1]

  if (!participantId) return null

  return {
    orderId: item.id,
    taskTitle: item.taskTitle,
    participantId,
    participantNickname,
    participantAvatarUrl,
    participantRole,
    status: item.status,
    lastMessageText: latest?.content || (latest?.imageUrl ? '[图片]' : '还没有消息'),
    lastMessageAt: latest?.createdAt || item.createdAt,
    messageCount: orderedMessages.length
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesPanel.value) {
    messagesPanel.value.scrollTop = messagesPanel.value.scrollHeight
  }
}

function appendMessage(newMessage: OrderMessage) {
  if (!order.value) return

  order.value = {
    ...order.value,
    messages: [
      ...order.value.messages.filter((item) => item.id !== newMessage.id),
      newMessage
    ]
  }
  void scrollToBottom()
  void loadConversations()
}

function isOrderMessage(value: unknown): value is OrderMessage {
  return Boolean(value && typeof value === 'object' && 'id' in value && 'senderId' in value && 'createdAt' in value)
}

async function refreshCurrentOrderSilently() {
  try {
    order.value = await orderApi.get(orderId.value)
    await scrollToBottom()
    void loadConversations()
  } catch {
    // Keep the current conversation visible if a silent refresh fails.
  }
}

async function sendTextMessage() {
  if (!message.value.trim() || sending.value || loading.value || isAwaitingNewProvider.value) return

  const content = message.value.trim()
  sending.value = true
  error.value = ''
  try {
    const sentMessage = await orderApi.sendMessage(orderId.value, content)
    message.value = ''
    if (isOrderMessage(sentMessage)) {
      appendMessage(sentMessage)
    } else {
      await refreshCurrentOrderSilently()
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '消息发送失败'
  } finally {
    sending.value = false
  }
}

async function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploadError.value = ''
  imageUploading.value = true
  try {
    uploadedImage.value = await fileApi.upload(file, 'CHAT_IMAGE')
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : '聊天图片上传失败'
  } finally {
    imageUploading.value = false
    input.value = ''
  }
}

async function sendImageMessage() {
  if (!uploadedImage.value || sending.value || loading.value || isAwaitingNewProvider.value) return

  sending.value = true
  error.value = ''
  try {
    const sentMessage = await orderApi.sendImage(orderId.value, uploadedImage.value.id)
    uploadedImage.value = null
    if (isOrderMessage(sentMessage)) {
      appendMessage(sentMessage)
    } else {
      await refreshCurrentOrderSilently()
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '图片消息发送失败'
  } finally {
    sending.value = false
  }
}

function handleComposerKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  void sendTextMessage()
}

function initials(name?: string) {
  return (name || '用户').slice(0, 1)
}

watch(orderId, (nextOrderId, previousOrderId) => {
  if (nextOrderId !== previousOrderId) {
    message.value = ''
    uploadedImage.value = null
    uploadError.value = ''
  }
  void load()
})
watch(sortedMessages, scrollToBottom)
onMounted(load)
</script>

<template>
  <section class="order-chat-view">
    <RouterLink v-if="hasOrderId" class="back-link" :to="{ name: 'order-detail', params: { id: orderId } }">
      <ArrowLeft class="button-icon" aria-hidden="true" />
      <span>返回订单详情</span>
    </RouterLink>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading && !order" class="empty-state">正在加载聊天</div>
    <div v-else-if="!order" class="empty-state">暂无聊天记录</div>

    <div v-else-if="order" class="chat-layout">
      <aside class="chat-sidebar">
        <div class="page-title">
          <h1>订单聊天</h1>
          <span class="tag">{{ orderStatusText[order.status] }}</span>
        </div>

        <section class="conversation-list">
          <div v-if="!conversations.length" class="contact-empty">暂无最近聊天</div>
          <RouterLink
            v-for="item in conversations"
            :key="item.orderId"
            :to="{ name: 'order-chat', params: { id: item.orderId } }"
            :class="['contact-item', { active: item.orderId === orderId }]"
          >
            <span class="contact-avatar">
              <img v-if="item.participantAvatarUrl" :src="resolveAssetUrl(item.participantAvatarUrl)" alt="" />
              <span v-else>{{ initials(item.participantNickname) }}</span>
            </span>
            <span class="contact-copy">
              <strong>{{ item.participantNickname }}</strong>
              <span>{{ item.participantRole }} · {{ item.taskTitle }}</span>
              <small>{{ item.lastMessageText }}</small>
            </span>
            <span class="contact-meta">
              <span>{{ item.messageCount }}</span>
              <small>{{ orderStatusText[item.status] }}</small>
            </span>
          </RouterLink>
        </section>
      </aside>

      <article :class="['chat-panel', { 'is-switching': loading }]">
        <div v-if="loading" class="chat-switch-indicator">正在切换会话...</div>
        <header class="chat-header">
          <div class="chat-avatar">
            <img v-if="chatTarget?.avatarUrl" :src="resolveAssetUrl(chatTarget.avatarUrl)" alt="" />
            <span v-else>{{ initials(chatTarget?.nickname) }}</span>
          </div>
          <div>
            <h2>{{ chatTarget?.nickname || '对方' }}</h2>
            <p v-if="isAwaitingNewProvider" class="hint">暂无服务方，暂不能发起聊天</p>
            <p v-else class="hint">围绕订单沟通取送时间、完成凭证和补充信息</p>
          </div>
        </header>

        <div ref="messagesPanel" class="chat-messages">
          <div v-if="!sortedMessages.length" class="chat-empty">
            <MessageSquareText class="empty-icon" aria-hidden="true" />
            <strong>还没有消息</strong>
            <p>可以先向 {{ chatTarget?.nickname || '对方' }} 打个招呼。</p>
          </div>

          <div
            v-for="item in sortedMessages"
            :key="item.id"
            :class="['chat-row', { mine: item.senderId === auth.user?.id }]"
          >
            <div class="sender-avatar">
              <img v-if="item.senderAvatarUrl" :src="resolveAssetUrl(item.senderAvatarUrl)" alt="" />
              <span v-else>{{ initials(item.senderNickname) }}</span>
            </div>
            <div class="bubble-wrap">
              <div class="bubble-meta">
                <strong>{{ item.senderNickname }}</strong>
                <span>{{ new Date(item.createdAt).toLocaleString() }}</span>
              </div>
              <div class="chat-bubble">
                <p v-if="item.content">{{ item.content }}</p>
                <img v-if="item.imageUrl" :src="resolveAssetUrl(item.imageUrl)" alt="聊天图片" />
              </div>
            </div>
          </div>
        </div>

        <div v-if="uploadedImage" class="image-preview">
          <img :src="resolveAssetUrl(uploadedImage.url)" :alt="uploadedImage.fileName" />
          <div>
            <strong>{{ uploadedImage.fileName }}</strong>
            <p class="hint">图片已上传，确认后发送到聊天中。</p>
          </div>
          <button class="button ghost icon-button" type="button" aria-label="取消图片" @click="uploadedImage = null">
            <X class="button-icon" aria-hidden="true" />
          </button>
          <button class="button secondary" type="button" :disabled="sending || loading" @click="sendImageMessage">发送图片</button>
        </div>
        <p v-if="uploadError" class="error-message">{{ uploadError }}</p>

        <form class="composer" @submit.prevent="sendTextMessage">
          <label class="button ghost upload-trigger" :class="{ disabled: imageUploading || loading || isAwaitingNewProvider }">
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              :disabled="imageUploading || loading || isAwaitingNewProvider"
              @change="handleImageChange"
            />
            <ImagePlus class="button-icon" aria-hidden="true" />
            <span>{{ imageUploading ? '上传中...' : '图片' }}</span>
          </label>
          <textarea
            v-model.trim="message"
            :disabled="loading || isAwaitingNewProvider"
            rows="1"
            placeholder="输入消息，Enter 发送"
            @keydown="handleComposerKeydown"
          />
          <button class="button secondary send-button" type="submit" :disabled="sending || loading || !message.trim() || isAwaitingNewProvider">
            <Send class="button-icon" aria-hidden="true" />
            <span>{{ sending ? '发送中...' : '发送' }}</span>
          </button>
        </form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.order-chat-view {
  --chat-green: #ffb454;
  --chat-dark: #191a23;
  --chat-grey: #f3f3f3;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  padding: 9px 14px;
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
}

.back-link:hover {
  background: var(--chat-green);
  color: #000000;
  transform: translateY(-1px);
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.34fr) minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
  min-height: calc(100vh - 160px);
}

.chat-sidebar,
.chat-panel,
.empty-state,
.error-message {
  border: 2px solid #000000;
  background: #ffffff;
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  max-height: calc(100vh - 160px);
  padding: 0 0 24px;
  border-radius: 26px;
  overflow: hidden;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 18px;
}

.page-title h1 {
  width: max-content;
  max-width: 100%;
  padding: 3px 10px;
  border: 2px solid #000000;
  border-radius: 14px;
  background: transparent;
  color: #000000;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
}

.page-title .tag,
.chat-panel .tag {
  width: fit-content;
  border: 2px solid #000000;
  background: #ffffff;
  color: #000000;
  font-weight: 900;
}

.page-title .tag {
  padding: 2px 8px;
  font-size: 10px;
  white-space: nowrap;
}

.side-label {
  color: #4a4e5b;
  font-size: 12px;
  font-weight: 900;
}

.conversation-list {
  display: grid;
  gap: 0;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0;
  margin-top: -8px;
}

.contact-empty {
  padding: 16px 18px;
  color: #6f7485;
  font-weight: 800;
  text-align: center;
}

.contact-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 18px;
  border-bottom: 1.5px solid #000000;
  background: #ffffff;
  color: #000000;
}

.contact-item:first-of-type {
  border-top: 2px solid #000000;
}

.contact-item:hover,
.contact-item.active {
  background: var(--chat-green);
  color: #000000;
  transform: translateY(-1px);
}

.contact-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 2px solid #000000;
  border-radius: 12px;
  background: var(--chat-green);
  color: #000000;
  font-weight: 900;
  overflow: hidden;
}

.contact-item.active .contact-avatar,
.contact-item:hover .contact-avatar {
  background: #ffffff;
}

.contact-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.contact-copy strong,
.contact-copy span,
.contact-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-copy strong {
  font-size: 13px;
  font-weight: 900;
}

.contact-copy span {
  color: #343743;
  font-size: 11px;
  font-weight: 800;
}

.contact-copy small {
  color: #6f7485;
  font-size: 11px;
  font-weight: 800;
}

.contact-meta {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.contact-meta > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 7px;
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
  color: #000000;
  font-size: 11px;
  font-weight: 900;
}

.contact-meta small {
  color: #6f7485;
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
}

.avatar-mark,
.chat-avatar,
.sender-avatar {
  display: grid;
  place-items: center;
  border: 2px solid #000000;
  background: var(--chat-green);
  color: #000000;
  font-weight: 900;
  flex-shrink: 0;
  overflow: hidden;
}

.contact-avatar img,
.chat-avatar img,
.sender-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
}

.chat-panel {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(360px, 1fr) auto auto auto;
  border-radius: 26px;
  overflow: hidden;
}

.chat-panel > * {
  transition: opacity 160ms ease;
}

.chat-panel.is-switching > :not(.chat-switch-indicator) {
  opacity: 0.68;
  pointer-events: none;
}

.chat-switch-indicator {
  position: absolute;
  top: 14px;
  left: 50%;
  z-index: 5;
  width: max-content;
  padding: 7px 14px;
  border: 2px solid #000000;
  border-radius: 999px;
  background: var(--chat-green);
  color: #000000;
  font-size: 12px;
  font-weight: 900;
  transform: translateX(-50%);
  box-shadow: 0 6px 18px rgba(25, 26, 35, 0.16);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 24px;
  border-bottom: 2px solid #000000;
  background:
    radial-gradient(circle at 96% 18%, var(--chat-green) 0 68px, transparent 69px),
    #ffffff;
}

.chat-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
}

.chat-header h2 {
  color: #000000;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 360px;
  max-height: calc(100vh - 360px);
  padding: 24px;
  overflow-y: auto;
  background:
    linear-gradient(90deg, rgba(25, 26, 35, 0.045) 1px, transparent 1px),
    linear-gradient(0deg, rgba(25, 26, 35, 0.045) 1px, transparent 1px),
    var(--chat-grey);
  background-size: 28px 28px;
}

.chat-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 300px;
  color: #4a4e5b;
  text-align: center;
  font-weight: 800;
}

.chat-empty strong {
  color: #000000;
  font-size: 18px;
  font-weight: 900;
}

.empty-icon {
  width: 34px;
  height: 34px;
}

.chat-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  max-width: 76%;
}

.chat-row.mine {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.sender-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #ffffff;
}

.chat-row.mine .sender-avatar {
  background: var(--chat-green);
}

.bubble-wrap {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.bubble-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #6f7485;
  font-size: 11px;
  font-weight: 800;
}

.chat-row.mine .bubble-meta {
  justify-content: flex-end;
}

.bubble-meta strong {
  color: #000000;
  font-weight: 900;
}

.chat-bubble {
  max-width: 100%;
  padding: 12px 14px;
  border: 2px solid #000000;
  border-radius: 18px 18px 18px 6px;
  background: #ffffff;
  color: #22252f;
  font-weight: 700;
  line-height: 1.65;
  word-break: break-word;
}

.chat-row.mine .chat-bubble {
  border-radius: 18px 18px 6px 18px;
  background: var(--chat-green);
  color: #000000;
}

.chat-bubble img {
  display: block;
  width: min(280px, 100%);
  max-height: 260px;
  object-fit: cover;
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
}

.image-preview {
  display: grid;
  grid-template-columns: 72px 1fr auto auto;
  gap: 12px;
  align-items: center;
  padding: 14px 18px;
  border-top: 2px solid #000000;
  background: #ffffff;
}

.image-preview img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border: 2px solid #000000;
  border-radius: 16px;
}

.image-preview strong {
  color: #000000;
  font-weight: 900;
  word-break: break-all;
}

.composer {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border-top: 2px solid #000000;
  background: #ffffff;
}

.composer textarea {
  min-height: 48px;
  max-height: 126px;
  resize: vertical;
  padding: 12px 14px;
  border: 2px solid #000000;
  border-radius: 16px;
  background: #ffffff;
  color: #000000;
  font-weight: 800;
  line-height: 1.5;
}

.composer textarea:focus {
  box-shadow: 0 0 0 3px rgba(255, 180, 84, 0.48);
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
}

.button.secondary {
  background: var(--chat-dark);
  color: #ffffff;
}

.button.secondary:hover {
  background: var(--chat-green);
  color: #000000;
}

.button.secondary:disabled,
.button.ghost.disabled {
  opacity: 0.48;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.button.ghost {
  background: #ffffff;
  color: #000000;
}

.button.ghost:hover {
  background: var(--chat-green);
  color: #000000;
}

.icon-button {
  width: 40px;
  height: 40px;
  padding: 0;
}

.upload-trigger input {
  display: none;
}

.hint {
  color: #6f7485;
  font-weight: 700;
}

.error-message {
  margin-bottom: 16px;
  border-radius: 18px;
  font-weight: 800;
}

@media (max-width: 980px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    order: 2;
  }

  .chat-panel {
    min-height: 70vh;
  }
}

@media (max-width: 768px) {
  .chat-sidebar,
  .chat-panel {
    border-radius: 22px;
  }

  .chat-header,
  .chat-messages,
  .composer {
    padding: 14px;
  }

  .chat-row {
    max-width: 92%;
  }

  .composer {
    grid-template-columns: 1fr auto;
  }

  .composer textarea {
    grid-column: 1 / -1;
    order: -1;
  }

  .image-preview {
    grid-template-columns: 64px 1fr;
  }

  .image-preview .button {
    width: 100%;
  }
}
</style>





