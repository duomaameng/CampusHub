<script setup lang="ts">
import { LockKeyhole, LogIn, Mail, ShieldCheck, UserPlus } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('student.demo1@smail.nju.edu.cn')
const password = ref('CampusHub123!')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    router.push((route.query.redirect as string) || '/tasks')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="form-panel auth-panel">
    <div class="auth-visual">
      <span class="auth-icon">
        <ShieldCheck aria-hidden="true" />
      </span>
      <div>
        <strong>校园身份可信流转</strong>
        <span>登录后继续完成发布、接单、订单和评价闭环。</span>
      </div>
    </div>

    <div class="page-title">
      <div>
        <h1>登录 CampusHub</h1>
        <p>使用南京大学邮箱进入任务大厅、订单和通知中心。</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="submit">
      <div class="field">
        <label for="email">
          <Mail class="label-icon" aria-hidden="true" />
          学校邮箱
        </label>
        <input id="email" v-model.trim="email" type="email" autocomplete="email" required />
      </div>

      <div class="field">
        <label for="password">
          <LockKeyhole class="label-icon" aria-hidden="true" />
          密码
        </label>
        <input id="password" v-model="password" type="password" autocomplete="current-password" required />
      </div>

      <p class="hint">演示账号：student.demo1@smail.nju.edu.cn / CampusHub123!</p>
      <p v-if="error" class="error-message">{{ error }}</p>

      <div class="actions">
        <button class="button primary" type="submit" :disabled="loading">
          <LogIn class="button-icon" aria-hidden="true" />
          <span>{{ loading ? '登录中' : '登录' }}</span>
        </button>
        <RouterLink class="button ghost" to="/register">
          <UserPlus class="button-icon" aria-hidden="true" />
          <span>注册账号</span>
        </RouterLink>
        <RouterLink class="button ghost" to="/forgot-password">忘记密码</RouterLink>
      </div>
    </form>
  </section>
</template>
