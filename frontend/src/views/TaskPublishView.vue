<script setup lang="ts">
import { CalendarClock, MapPin, Send, Tags, Text, Type } from '@lucide/vue'
import { computed, reactive, ref, watch } from 'vue'
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
const uploadedImages = ref<UploadedFileItem[]>([])

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
      ['foundTime', '时间'],
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

async function submit() {
  error.value = ''
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

function removeUploadedImage(imageId: number) {
  uploadedImages.value = uploadedImages.value.filter((item) => item.id !== imageId)
  form.imageIds = form.imageIds.filter((item) => item !== imageId)
}
</script>

<template>
  <section class="form-panel">
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
              <button class="button ghost" type="button" @click="removeUploadedImage(item.id)">移除</button>
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
          <input id="deadline" v-model="form.deadline" type="datetime-local" required />
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
            required
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
