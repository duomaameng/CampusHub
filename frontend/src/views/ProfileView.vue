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
  <section class="profile-view">
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
              <option value="苏州校区">苏州校区</option>
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
.profile-view {
  --profile-green: #b9ff66;
  --profile-dark: #191a23;
  --profile-grey: #f3f3f3;
}

.page-title {
  margin-bottom: 26px;
}

.page-title h1 {
  width: max-content;
  padding: 5px 14px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: var(--profile-green);
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
  color: #3f4350;
  font-size: 16px;
  font-weight: 700;
}

.detail-layout {
  align-items: start;
  gap: 26px;
}

.form-panel {
  position: relative;
  padding: 32px;
  border: 2px solid #000000;
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 7px 0 #000000;
  overflow: hidden;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.form-panel::before,
.form-panel::after,
.panel::before,
.panel::after,
.empty-state::before {
  display: none;
}

.form-panel > .panel {
  position: relative;
  padding: 24px;
  border: 2px solid #000000;
  border-radius: 24px;
  background:
    radial-gradient(circle at 96% 0%, var(--profile-green) 0 58px, transparent 59px),
    var(--profile-grey);
  box-shadow: 0 5px 0 #000000;
  overflow: hidden;
}

.form-panel h2 {
  width: max-content;
  max-width: 100%;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
}

.form-panel h2::before {
  display: none;
}

.form-panel .field input,
.form-panel .field select,
.form-panel .field textarea {
  min-height: 50px;
  padding: 11px 15px;
  color: #000000;
  font-size: 14px;
  font-weight: 800;
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: none;
  transition: all var(--transition-fast);
}

.form-panel .field input:focus,
.form-panel .field select:focus,
.form-panel .field textarea:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(185, 255, 102, 0.48);
  background: #ffffff;
}

.form-panel .field label {
  color: #4a4e5b;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.form-panel .button.primary {
  padding: 12px 28px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
  background: var(--profile-dark);
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: 0 4px 0 #000000;
  align-self: flex-start;
}

.form-panel .button.primary:hover:not(:disabled) {
  color: #000000;
  background: var(--profile-green);
  transform: translateY(-2px);
  box-shadow: 0 5px 0 #000000;
}

.upload-trigger {
  padding: 10px 20px;
  color: #ffffff;
  font-weight: 900;
  letter-spacing: 0;
  background: var(--profile-dark);
  border: 2px solid #000000;
  border-radius: 14px;
  box-shadow: 0 4px 0 #000000;
}

.upload-trigger:hover {
  border-color: #000000;
  background: var(--profile-green);
  color: #000000;
}

.avatar-preview {
  border: 2px solid #000000;
  background: #ffffff;
  box-shadow: 0 5px 0 #000000;
}

.avatar-preview::after {
  display: none;
}

aside.panel {
  position: relative;
  padding: 28px;
  border: 2px solid #000000;
  border-radius: 28px;
  background:
    radial-gradient(circle at 96% 8%, var(--profile-green) 0 62px, transparent 63px),
    var(--profile-dark);
  color: #ffffff;
  box-shadow: 0 7px 0 #000000;
  overflow: hidden;
}

aside.panel h2 {
  width: max-content;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
}

aside.panel h2::before {
  display: none;
}

aside.panel .grid.two > div {
  padding: 18px;
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 0 #000000;
}

aside.panel strong {
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
  background: none;
  -webkit-text-fill-color: #000000;
  display: block;
}

aside.panel p {
  font-size: 13.5px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.82);
  font-weight: 800;
}

aside.panel .grid.two .hint {
  color: #6f7485;
}

.error-message,
.success-message {
  padding: 12px 16px;
  font-weight: 800;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
}

.error-message {
  border-color: rgba(239, 68, 68, 0.2);
}

.success-message {
  border-color: rgba(16, 185, 129, 0.2);
}

.hint {
  font-size: 12.5px;
  font-weight: 700;
}

.checkbox-label {
  font-size: 13.5px;
  font-weight: 800;
  color: #343743;
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
  background: var(--profile-green);
  border-color: #000000;
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

.empty-state {
  padding: var(--space-10) var(--space-6);
  font-size: 14px;
  font-weight: 800;
  border: 2px dashed #000000;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 5px 0 #000000;
}

@media (max-width: 1024px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
