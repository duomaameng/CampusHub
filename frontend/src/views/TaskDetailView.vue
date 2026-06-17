<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { fileApi, orderApi, reportApi, taskApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusText } from '@/types'
import type { ApplicationItem, OrderItem, RewardType, TaskItem, TaskUpdatePayload, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const task = ref<TaskItem>()
const relatedOrder = ref<OrderItem | null>(null)
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
const editMode = ref(false)
const savingTask = ref(false)
const deletingTask = ref(false)
const deleteConfirming = ref(false)
const favoriteLoading = ref(false)
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

const taskId = computed(() => Number(route.params.id))
const isPublisher = computed(() => Boolean(task.value && auth.user?.id === task.value.publisherId))
const canApply = computed(() => Boolean(auth.isAuthenticated && auth.user?.verified && applyMessage.value))
const canEditTask = computed(() => Boolean(isPublisher.value && task.value?.status === 'OPEN' && task.value.applicationCount === 0))
const canReportTask = computed(() => Boolean(
  auth.isAuthenticated &&
  task.value &&
  ['IN_PROGRESS', 'COMPLETED'].includes(task.value.status) &&
  relatedOrder.value &&
  (
    relatedOrder.value.publisherId === auth.user?.id ||
    relatedOrder.value.serviceProviderId === auth.user?.id
  )
))
const isFavorited = computed(() => Boolean(task.value?.isFavorited || (task.value as (TaskItem & { favorited?: boolean }) | undefined)?.favorited))

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
  CASH: '现金',
  NEGOTIABLE: '面议',
  CREDIT_INTENT: '积分意向'
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

async function load() {
  error.value = ''
  loading.value = true
  try {
    task.value = await taskApi.get(taskId.value)
    relatedOrder.value = null
    if (auth.isAuthenticated && ['IN_PROGRESS', 'COMPLETED'].includes(task.value.status)) {
      const orders = await orderApi.list({ page: 1, size: 100 })
      relatedOrder.value = orders.records.find((item) => item.taskId === task.value?.id) || null
    }
    if (isPublisher.value) {
      applications.value = await taskApi.applications(taskId.value)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '需求加载失败'
  } finally {
    loading.value = false
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
  <section class="task-detail-view">
    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载需求</div>

    <div v-else-if="task" class="detail-layout">
      <article class="panel grid">
        <div class="page-title">
          <div>
            <h1>{{ task.title }}</h1>
            <p><RouterLink :to="{ name: 'user-public-profile', params: { id: task.publisherId } }">{{ task.publisherNickname }}</RouterLink> · {{ task.campus }} · {{ new Date(task.createdAt).toLocaleString() }}</p>
          </div>
          <div class="task-actions">
            <span class="tag">{{ categoryText[task.category] }}</span>
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

        <p>{{ task.description }}</p>

        <div v-if="task.imageUrls.length" class="upload-grid">
          <article v-for="url in task.imageUrls" :key="url" class="upload-card">
            <img :src="resolveAssetUrl(url)" alt="任务配图" />
          </article>
        </div>

        <div class="grid two">
          <div class="panel">
            <strong>报酬类型</strong>
            <p>{{ rewardText[task.rewardType] }}</p>
          </div>
          <div class="panel">
            <strong>截止时间</strong>
            <p>{{ new Date(task.deadline).toLocaleString() }}</p>
          </div>
        </div>

        <div v-if="task.categoryFields && Object.keys(task.categoryFields).length" class="panel">
          <h2>订单相关信息</h2>
          <div class="meta-line">
            <span v-for="(value, key) in task.categoryFields" :key="key" class="tag">
              {{ formatCategoryFieldKey(String(key)) }}: {{ formatCategoryFieldValue(value) }}
            </span>
          </div>
        </div>
      </article>

      <aside class="grid">
        <section v-if="isPublisher" class="panel grid">
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

        <section v-if="canReportTask" class="panel grid">
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
              <h3><RouterLink :to="{ name: 'user-public-profile', params: { id: application.applicantId } }">{{ application.applicantNickname }}</RouterLink></h3>
              <span class="tag">{{ applicationStatusText[application.status] }}</span>
            </div>
            <p>{{ application.message }}</p>
            <p class="hint">信用分 {{ application.applicantCreditScore }} · {{ new Date(application.createdAt).toLocaleString() }}</p>
            <button
              v-if="task.status === 'OPEN' && application.status === 'PENDING'"
              class="button secondary"
              type="button"
              :disabled="actionLoadingApplicationId === application.id"
              @click="confirmApplication(application.id)"
            >
              确认接单
            </button>
            <button
              v-if="task.status === 'OPEN' && application.status === 'PENDING'"
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
  </section>
</template>

<style scoped>
.task-detail-view {
  --task-green: #7dbe8e;
  --task-dark: #191a23;
  --task-grey: #f3f3f3;
}

.detail-layout {
  align-items: start;
  gap: 26px;
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
  justify-content: flex-end;
  gap: var(--space-2);
}

.favorite-button {
  min-width: 86px;
  border: 2px solid #000000;
  background: #ffffff;
  color: #000000;
  box-shadow: 0 4px 0 #000000;
}

.favorite-button:hover:not(:disabled) {
  border-color: #000000;
  background: var(--task-green);
  color: #000000;
  box-shadow: 0 5px 0 #000000;
}

.favorite-button.active {
  border-color: #000000;
  background: var(--task-green);
  color: #000000;
  box-shadow: 0 4px 0 #000000;
}

.panel {
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
    radial-gradient(circle at 94% 10%, var(--task-green) 0 72px, transparent 73px),
    #ffffff;
}

.panel h1 {
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: var(--task-green);
  color: #000000;
  font-size: 32px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
  -webkit-text-fill-color: #000000;
  box-shadow: 0 4px 0 #000000;
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
  color: #2d5a3d;
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
  box-shadow: 0 4px 0 #000000;
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

.upload-card {
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 0 #000000;
  transition: all var(--transition-base);
}

.upload-card:hover {
  border-color: #000000;
  box-shadow: 0 5px 0 #000000;
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
  box-shadow: 0 6px 0 #000000;
}

aside .panel h2 {
  font-size: 22px;
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: 0 4px 0 #000000;
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
  box-shadow: 0 5px 0 #000000;
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
  box-shadow: 0 0 0 3px rgba(125, 190, 142, 0.48);
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
  box-shadow: 0 4px 0 #000000;
}

.item-card:hover {
  border-color: #000000;
  transform: translateY(-2px);
  box-shadow: 0 5px 0 #000000;
}

.item-card h3 a {
  color: #000000;
  font-weight: 900;
}

.item-card h3 a:hover {
  color: #2d5a3d;
}

.empty-state {
  padding: var(--space-8) var(--space-4);
  font-size: 13px;
  font-weight: 800;
  background: #ffffff;
  border: 2px dashed #000000;
  border-radius: 22px;
  box-shadow: 0 4px 0 #000000;
}

.error-message,
.success-message {
  margin-top: var(--space-2);
  padding: 10px 14px;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
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
