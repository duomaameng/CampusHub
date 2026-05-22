<script setup lang="ts">
import { MailCheck, RotateCcw, ShieldCheck } from '@lucide/vue'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

const email = ref((route.query.email as string) || auth.user?.email || '')
const code = ref('')
const loading = ref(false)
const resending = ref(false)
const error = ref('')
const success = ref('')

async function verify() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await auth.verifyEmail(email.value, code.value)
    success.value = '邮箱验证成功，请重新登录后继续使用核心功能。'
    window.setTimeout(() => router.push('/login'), 800)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '邮箱验证失败'
  } finally {
    loading.value = false
  }
}

async function resend() {
  error.value = ''
  success.value = ''
  resending.value = true
  try {
    await auth.sendVerificationCode(email.value, 'REGISTER')
    success.value = useMock ? '验证码已重新发送。mock 环境验证码为 123456。' : '验证码已重新发送，请查看学校邮箱。'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '验证码发送失败'
  } finally {
    resending.value = false
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
        <strong>邮箱验证码认证</strong>
        <span>完成学校邮箱验证后，才能发布需求、申请接单和提交评价。</span>
      </div>
    </div>

    <div class="page-title">
      <div>
        <h1>验证学校邮箱</h1>
        <p>请输入邮箱中收到的 6 位验证码。</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="verify">
      <div class="field">
        <label for="email">学校邮箱</label>
        <input id="email" v-model.trim="email" type="email" required />
      </div>

      <div class="field">
        <label for="code">验证码</label>
        <input id="code" v-model.trim="code" inputmode="numeric" maxlength="6" placeholder="123456" required />
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>
      <p v-if="success" class="success-message">{{ success }}</p>

      <div class="actions">
        <button class="button primary" type="submit" :disabled="loading">
          <MailCheck class="button-icon" aria-hidden="true" />
          <span>{{ loading ? '验证中' : '完成验证' }}</span>
        </button>
        <button class="button ghost" type="button" :disabled="resending || !email" @click="resend">
          <RotateCcw class="button-icon" aria-hidden="true" />
          <span>{{ resending ? '发送中' : '重新发送' }}</span>
        </button>
      </div>
    </form>
  </section>
</template>
