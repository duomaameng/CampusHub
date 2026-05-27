<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { fileApi, userApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const auth = useAuthStore()
const profile = ref<UserProfile>()
const form = reactive({
  nickname: '',
  avatarUrl: '',
  gender: 'OTHER' as UserProfile['profile']['gender'],
  grade: '',
  college: '',
  bio: '',
  campus: '仙林校区',
  contact: '',
  contactVisible: false
})
const loading = ref(false)
const error = ref('')
const success = ref('')
const avatarUploading = ref(false)
const avatarUploadError = ref('')

function fillForm(data: UserProfile) {
  form.nickname = data.profile.nickname
  form.avatarUrl = data.profile.avatarUrl || ''
  form.gender = data.profile.gender
  form.grade = data.profile.grade
  form.college = data.profile.college
  form.bio = data.profile.bio
  form.campus = data.profile.campus
  form.contact = data.profile.contact
  form.contactVisible = data.profile.contactVisible
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  avatarUploadError.value = ''
  avatarUploading.value = true
  try {
    const uploaded = await fileApi.upload(file, 'AVATAR')
    form.avatarUrl = uploaded.url
    success.value = '头像上传成功，保存资料后生效'
  } catch (err) {
    avatarUploadError.value = err instanceof Error ? err.message : '头像上传失败'
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

async function load() {
  error.value = ''
  loading.value = true
  try {
    profile.value = await userApi.me()
    fillForm(profile.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资料加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  success.value = ''
  try {
    profile.value = await userApi.updateMe({ ...form })
    await auth.loadMe()
    success.value = '资料已更新'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>个人资料</h1>
        <p>维护昵称、学院、常用校区、联系方式和信用展示。</p>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="success" class="success-message">{{ success }}</p>
    <div v-if="loading" class="empty-state">正在加载资料</div>

    <div v-else class="detail-layout">
      <form class="form-panel grid" @submit.prevent="save">
        <section class="panel grid">
          <h2>头像</h2>
          <div class="upload-avatar-row">
            <div class="avatar-preview">
              <img v-if="form.avatarUrl" :src="resolveAssetUrl(form.avatarUrl)" alt="头像预览" />
              <span v-else>{{ form.nickname?.slice(0, 1) || 'U' }}</span>
            </div>
            <div class="grid" style="flex:1">
              <label class="button secondary upload-trigger">
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleAvatarChange" />
                <span>{{ avatarUploading ? '上传中...' : '上传头像' }}</span>
              </label>
              <p class="hint">支持 JPG / PNG / WebP，头像建议小于 2MB。</p>
              <p v-if="avatarUploadError" class="error-message">{{ avatarUploadError }}</p>
            </div>
          </div>
        </section>

        <div class="grid two">
          <div class="field">
            <label for="nickname">昵称</label>
            <input id="nickname" v-model.trim="form.nickname" maxlength="30" required />
          </div>
          <div class="field">
            <label for="gender">性别</label>
            <select id="gender" v-model="form.gender">
              <option value="MALE">男</option>
              <option value="FEMALE">女</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
        </div>

        <div class="grid two">
          <div class="field">
            <label for="grade">年级</label>
            <input id="grade" v-model.trim="form.grade" />
          </div>
          <div class="field">
            <label for="college">学院</label>
            <input id="college" v-model.trim="form.college" />
          </div>
        </div>

        <div class="field">
          <label for="bio">个人简介</label>
          <textarea id="bio" v-model.trim="form.bio" maxlength="200" />
        </div>

        <div class="grid two">
          <div class="field">
            <label for="campus">常用校区</label>
            <select id="campus" v-model="form.campus">
              <option value="仙林校区">仙林校区</option>
              <option value="鼓楼校区">鼓楼校区</option>
              <option value="浦口校区">浦口校区</option>
            </select>
          </div>
          <div class="field">
            <label for="contact">联系方式</label>
            <input id="contact" v-model.trim="form.contact" />
          </div>
        </div>

        <div class="field">
          <label class="checkbox-label">
            <input v-model="form.contactVisible" type="checkbox" />
            <span>向订单对方展示联系方式</span>
          </label>
        </div>

        <button class="button primary" type="submit">保存资料</button>
      </form>

      <aside v-if="profile" class="panel grid">
        <h2>信用信息</h2>
        <div class="grid two">
          <div>
            <strong>{{ profile.credit.score }}</strong>
            <p class="hint">信用分</p>
          </div>
          <div>
            <strong>{{ profile.credit.completedOrders }}</strong>
            <p class="hint">完成订单</p>
          </div>
        </div>
        <p>好评率 {{ Math.round(profile.credit.praiseRate * 100) }}%</p>
        <p class="hint">认证状态：{{ profile.verified ? '已认证' : '未认证' }}</p>
        <p class="hint">账号状态：{{ profile.status }}</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.form-panel {
  border: 1.5px solid var(--border-light);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
}

.form-panel h2 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
  position: relative;
  padding-left: var(--space-3);
}

.form-panel h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 2px;
  bottom: 2px;
  width: 3px;
  background: linear-gradient(180deg, var(--primary-400), var(--secondary-500));
  border-radius: 2px;
}

.form-panel .field input,
.form-panel .field select,
.form-panel .field textarea {
  padding: 11px 14px;
  font-size: 14px;
  border: 1.5px solid var(--border-light);
  background: var(--bg-body);
  transition: all var(--transition-fast);
}

.form-panel .field input:focus,
.form-panel .field select:focus,
.form-panel .field textarea:focus {
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1), var(--shadow-sm);
  background: var(--bg-surface);
}

.form-panel .field label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.form-panel .button.primary {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
  align-self: flex-start;
}

.form-panel .button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
}

.upload-trigger {
  padding: 10px 20px;
  font-weight: 700;
  letter-spacing: 0.01em;
  border: 1.5px solid var(--border-light);
}

.upload-trigger:hover {
  border-color: var(--primary-400);
  background: var(--primary-50);
  color: var(--primary-700);
}

.avatar-preview {
  border: 3px solid var(--bg-surface);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.15), 0 2px 4px rgba(0, 0, 0, 0.05);
}

.avatar-preview::after {
  background: conic-gradient(from 0deg, var(--primary-400), var(--secondary-500), var(--accent-400), var(--primary-400));
}

aside.panel {
  border: 1.5px solid var(--border-light);
  background: linear-gradient(135deg, var(--bg-surface), var(--bg-body));
}

aside.panel h2 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
  padding-left: var(--space-3);
  position: relative;
}

aside.panel h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 2px;
  bottom: 2px;
  width: 3px;
  background: linear-gradient(180deg, var(--success), var(--primary-500));
  border-radius: 2px;
}

aside.panel strong {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
  background: linear-gradient(135deg, var(--primary-600), var(--secondary-600));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: block;
}

aside.panel p {
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.error-message,
.success-message {
  padding: 12px 16px;
  font-weight: 600;
  border: 1.5px solid;
}

.error-message {
  border-color: rgba(239, 68, 68, 0.2);
}

.success-message {
  border-color: rgba(16, 185, 129, 0.2);
}

.hint {
  font-size: 12.5px;
  font-weight: 500;
}

.checkbox-label {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-secondary);
}

.checkbox-label input[type='checkbox'] {
  width: 18px;
  height: 18px;
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 600;
  border: 2px dashed var(--border-medium);
}

@media (max-width: 1024px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
