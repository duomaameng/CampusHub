<script setup lang="ts">
import { CalendarClock, MapPin, Send, Tags, Text, Type } from '@lucide/vue'
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { fileApi, taskApi } from '@/services/api'
import type { TaskCategory, TaskForm, UploadedFileItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const router = useRouter()

const form = reactive<TaskForm>({
  category: 'EXPRESS',
  title: '',
  description: '',
  campus: '仙林校区',
  rewardType: 'NEGOTIABLE',
  deadline: '',
  anonymous: false,
  imageIds: [],
  categoryFields: {
    expressCompany: '',
    pickupLocation: '',
    pickupCode: '',
    deliveryLocation: ''
  }
})

const loading = ref(false)
const error = ref('')
const uploadError = ref('')
const imageUploading = ref(false)
const removingImageIds = ref<number[]>([])
const uploadedImages = ref<UploadedFileItem[]>([])
const minDeadline = ref(createMinDeadline())
let minDeadlineTimer: number | undefined

function createMinDeadline() {
  const date = new Date(Date.now() + 60 * 1000)
  date.setSeconds(0, 0)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000).toISOString().slice(0, 16)
}

function refreshMinDeadline() {
  minDeadline.value = createMinDeadline()
}

const categoryLabel: Record<TaskCategory, string> = {
  EXPRESS: '快递代取',
  ERRAND: '跑腿代办',
  TUTORING: '学习辅导',
  SECOND_HAND: '二手交易',
  LOST_FOUND: '失物招领',
  CONSULTATION: '咨询问答',
  TEAM_UP: '组队搭子',
  OTHER: '其他'
}

const categoryFields = computed(() => {
  if (form.category === 'EXPRESS') {
    return [
      ['expressCompany', '快递公司'],
      ['pickupLocation', '取件地点'],
      ['pickupCode', '取件码'],
      ['deliveryLocation', '送达地点']
    ]
  }
  if (form.category === 'SECOND_HAND') {
    return [
      ['goodsCategory', '商品分类'],
      ['condition', '新旧程度'],
      ['price', '售价']
    ]
  }
  if (form.category === 'LOST_FOUND') {
    return [
      ['itemName', '物品名称'],
      ['location', '地点'],
      ['foundTime', '丢失/捡到时间'],
      ['itemDescription', '物品描述'],
      ['contactInfo', '联系方式']
    ]
  }
  if (form.category === 'TEAM_UP') {
    return [
      ['activityType', '活动类型'],
      ['requiredCount', '人数需求'],
      ['activityTime', '活动时间']
    ]
  }
  return []
})

watch(
  () => form.category,
  () => {
    form.categoryFields = {}
  }
)

onMounted(() => {
  refreshMinDeadline()
  minDeadlineTimer = window.setInterval(refreshMinDeadline, 30000)
})

onUnmounted(() => {
  if (minDeadlineTimer) window.clearInterval(minDeadlineTimer)
})

async function submit() {
  error.value = ''
  if (!validateDeadline()) {
    return
  }
  for (const [key] of categoryFields.value) {
    if (!validateCategoryDateTimeField(key)) return
  }
  loading.value = true
  try {
    const result = await taskApi.create(form)
    router.push(`/tasks/${result.id}`)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发布失败'
  } finally {
    loading.value = false
  }
}

function validateDeadline() {
  refreshMinDeadline()
  if (!form.deadline || new Date(form.deadline).getTime() <= Date.now()) {
    form.deadline = minDeadline.value
    return false
  }
  return true
}

function isDateTimeField(key: string) {
  return key.includes('Time')
}

function requiresFutureDateTime(key: string) {
  return key === 'activityTime'
}

function validateCategoryDateTimeField(key: string) {
  if (!requiresFutureDateTime(key)) return true
  refreshMinDeadline()
  const value = form.categoryFields[key]
  if (typeof value !== 'string' || !value || new Date(value).getTime() <= Date.now()) {
    form.categoryFields[key] = minDeadline.value
    return false
  }
  return true
}

async function handleTaskImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  uploadError.value = ''
  imageUploading.value = true
  try {
    for (const file of files) {
      const uploaded = await fileApi.upload(file, 'TASK_IMAGE')
      uploadedImages.value.push(uploaded)
      form.imageIds.push(uploaded.id)
    }
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : '任务配图上传失败'
  } finally {
    imageUploading.value = false
    input.value = ''
  }
}

async function removeUploadedImage(imageId: number) {
  if (removingImageIds.value.includes(imageId)) return

  uploadError.value = ''
  removingImageIds.value = [...removingImageIds.value, imageId]
  try {
    await fileApi.remove(imageId)
    uploadedImages.value = uploadedImages.value.filter((item) => item.id !== imageId)
    form.imageIds = form.imageIds.filter((item) => item !== imageId)
  } catch (err) {
    const message = err instanceof Error ? err.message : ''
    uploadError.value = message.includes('status code 404')
      ? '移除失败：后端删除接口未生效，请重启后端服务后重试'
      : message || '任务配图移除失败'
  } finally {
    removingImageIds.value = removingImageIds.value.filter((item) => item !== imageId)
  }
}
</script>

<template>
  <section class="form-panel task-publish-view">
    <div class="page-title">
      <div>
        <h1>发布需求</h1>
        <p>填写任务信息后会进入任务大厅，等待其他同学申请接单。</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="submit">
      <section class="panel grid">
        <h2>任务配图</h2>
        <label class="button secondary upload-trigger">
          <input multiple type="file" accept="image/png,image/jpeg,image/webp" @change="handleTaskImageChange" />
          <span>{{ imageUploading ? '上传中...' : '上传任务配图' }}</span>
        </label>
        <p class="hint">支持多张图片，每张不超过 5MB。</p>
        <p v-if="uploadError" class="error-message">{{ uploadError }}</p>
        <div v-if="uploadedImages.length" class="upload-grid">
          <article v-for="item in uploadedImages" :key="item.id" class="upload-card">
            <img :src="resolveAssetUrl(item.url)" :alt="item.fileName" />
            <div class="upload-card-meta">
              <strong>{{ item.fileName }}</strong>
              <button
                class="button ghost"
                type="button"
                :disabled="removingImageIds.includes(item.id)"
                @click="removeUploadedImage(item.id)"
              >
                {{ removingImageIds.includes(item.id) ? '移除中...' : '移除' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <div class="grid two">
        <div class="field">
          <label for="category">
            <Tags class="label-icon" aria-hidden="true" />
            需求分类
          </label>
          <select id="category" v-model="form.category" required>
            <option v-for="(label, value) in categoryLabel" :key="value" :value="value">{{ label }}</option>
          </select>
        </div>

        <div class="field">
          <label for="campus">
            <MapPin class="label-icon" aria-hidden="true" />
            校区
          </label>
          <select id="campus" v-model="form.campus" required>
            <option value="仙林校区">仙林校区</option>
            <option value="鼓楼校区">鼓楼校区</option>
            <option value="浦口校区">浦口校区</option>
            <option value="苏州校区">苏州校区</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label for="title">
          <Type class="label-icon" aria-hidden="true" />
          标题
        </label>
        <input id="title" v-model.trim="form.title" minlength="2" maxlength="100" required />
      </div>

      <div class="field">
        <label for="description">
          <Text class="label-icon" aria-hidden="true" />
          描述
        </label>
        <textarea id="description" v-model.trim="form.description" minlength="10" maxlength="2000" required />
      </div>

      <div class="grid two">
        <div class="field">
          <label for="reward">报酬类型</label>
          <select id="reward" v-model="form.rewardType" required>
            <option value="CASH">现金</option>
            <option value="NEGOTIABLE">面议</option>
            <option value="CREDIT_INTENT">积分意向</option>
          </select>
        </div>
        <div class="field">
          <label for="deadline">
            <CalendarClock class="label-icon" aria-hidden="true" />
            截止时间
          </label>
          <input
            id="deadline"
            v-model="form.deadline"
            type="datetime-local"
            :min="minDeadline"
            required
            @input="validateDeadline"
            @change="validateDeadline"
            @blur="validateDeadline"
          />
        </div>
      </div>

      <div v-if="categoryFields.length" class="grid two">
        <div v-for="[key, label] in categoryFields" :key="key" class="field">
          <label :for="key">{{ label }}</label>
          <select v-if="key === 'condition'" :id="key" v-model="form.categoryFields[key]" required>
            <option value="NEW">全新</option>
            <option value="LIKE_NEW">几乎全新</option>
            <option value="USED">有使用痕迹</option>
          </select>
          <input
            v-else
            :id="key"
            v-model="form.categoryFields[key]"
            :type="key.includes('Time') ? 'datetime-local' : key === 'price' || key === 'requiredCount' ? 'number' : 'text'"
            :min="requiresFutureDateTime(key) ? minDeadline : undefined"
            required
            @input="validateCategoryDateTimeField(key)"
            @change="validateCategoryDateTimeField(key)"
            @blur="validateCategoryDateTimeField(key)"
          />
        </div>
      </div>

      <div class="field">
        <label class="checkbox-label">
          <input v-model="form.anonymous" type="checkbox" />
          <span>匿名发布</span>
        </label>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>
      <div class="actions">
        <button class="button primary" type="submit" :disabled="loading">
          <Send class="button-icon" aria-hidden="true" />
          <span>{{ loading ? '发布中' : '发布需求' }}</span>
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.task-publish-view {
  --publish-green: #b9ff66;
  --publish-dark: #191a23;
  --publish-grey: #f3f3f3;
  position: relative;
  padding: 34px;
  border: 2px solid #000000;
  border-radius: 30px;
  background:
    radial-gradient(circle at 96% 4%, var(--publish-green) 0 78px, transparent 79px),
    #ffffff;
  box-shadow: 0 8px 0 #000000;
  overflow: hidden;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.task-publish-view::before,
.task-publish-view::after,
.panel::before,
.panel::after {
  display: none;
}

.page-title {
  margin-bottom: 28px;
}

.page-title h1 {
  width: max-content;
  max-width: 100%;
  padding: 5px 14px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: var(--publish-green);
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
  box-shadow: 0 4px 0 #000000;
}

.page-title p {
  margin-top: 12px;
  max-width: 680px;
  color: #3f4350;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.6;
}

.panel {
  position: relative;
  padding: 24px;
  border: 2px solid #000000;
  border-radius: 24px;
  background:
    radial-gradient(circle at 96% 0%, var(--publish-green) 0 58px, transparent 59px),
    var(--publish-grey);
  box-shadow: 0 5px 0 #000000;
  overflow: hidden;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.panel h2 {
  width: max-content;
  max-width: 100%;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
}

.field label {
  color: #4a4e5b;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.field input,
.field select,
.field textarea {
  min-height: 50px;
  padding: 11px 15px;
  color: #000000;
  font-size: 14px;
  font-weight: 800;
  border: 2px solid #000000;
  border-radius: 14px;
  background-color: #ffffff;
  box-shadow: none;
  transition: all var(--transition-fast);
}

.field textarea {
  min-height: 132px;
  line-height: 1.6;
}

.field input:hover,
.field select:hover,
.field textarea:hover {
  background-color: #f8ffe8;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: #000000;
  background-color: #ffffff;
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
  color: #ffffff;
  background: var(--publish-dark);
}

.button.primary:hover:not(:disabled),
.button.secondary:hover:not(:disabled) {
  color: #000000;
  background: var(--publish-green);
  box-shadow: 0 5px 0 #000000;
  transform: translateY(-2px);
}

.button.ghost {
  color: #000000;
  background: #ffffff;
}

.button.ghost:hover:not(:disabled) {
  background: var(--publish-green);
  box-shadow: 0 5px 0 #000000;
}

.button:disabled {
  opacity: 0.5;
  box-shadow: none;
}

.upload-trigger {
  width: max-content;
  max-width: 100%;
  padding: 11px 20px;
}

.upload-grid {
  gap: var(--space-3);
}

.upload-card {
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 0 #000000;
  transition: all var(--transition-fast);
}

.upload-card:hover {
  border-color: #000000;
  box-shadow: 0 5px 0 #000000;
  transform: translateY(-2px);
}

.upload-card-meta strong {
  color: #000000;
  font-weight: 900;
}

.hint {
  color: #6f7485;
  font-size: 12.5px;
  font-weight: 700;
}

.checkbox-label {
  color: #343743;
  font-size: 13.5px;
  font-weight: 800;
}

.checkbox-label input[type='checkbox'] {
  width: 22px;
  height: 22px;
  min-height: 0;
  padding: 0;
  border: 2px solid #000000;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 2px 0 #000000;
  appearance: none;
  cursor: pointer;
}

.checkbox-label input[type='checkbox']:checked {
  background: var(--publish-green);
}

.checkbox-label input[type='checkbox']:checked::after {
  content: '';
  position: absolute;
  left: 7px;
  top: 3px;
  width: 5px;
  height: 10px;
  border: solid #000000;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-label input[type='checkbox']:focus-visible {
  box-shadow: 0 0 0 3px rgba(185, 255, 102, 0.48), 0 2px 0 #000000;
}

.error-message {
  padding: 12px 16px;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
  font-weight: 800;
}

.actions .button.primary {
  min-width: 160px;
}

@media (max-width: 768px) {
  .task-publish-view {
    padding: 24px;
  }
}
</style>
