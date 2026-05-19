<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { userApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types'

const auth = useAuthStore()
const profile = ref<UserProfile>()
const form = reactive({
  nickname: '',
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

function fillForm(data: UserProfile) {
  form.nickname = data.profile.nickname
  form.gender = data.profile.gender
  form.grade = data.profile.grade
  form.college = data.profile.college
  form.bio = data.profile.bio
  form.campus = data.profile.campus
  form.contact = data.profile.contact
  form.contactVisible = data.profile.contactVisible
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
