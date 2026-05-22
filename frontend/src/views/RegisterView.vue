<script setup lang="ts">
import { LockKeyhole, Mail, RotateCcw, ShieldCheck, UserPlus } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import PasswordInput from '@/components/PasswordInput.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

const email = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')

async function sendCode() {
  error.value = ''
  success.value = ''
  sending.value = true
  try {
    await auth.sendVerificationCode(email.value, 'REGISTER')
    success.value = useMock ? '验证码已发送。mock 环境验证码为 123456。' : '验证码已发送，请查看学校邮箱。'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function submit() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await auth.register(email.value, password.value, confirmPassword.value, code.value)
    success.value = '注册成功，请使用同一验证码继续完成邮箱验证。'
    window.setTimeout(() => router.push({ name: 'verify-email', query: { email: email.value } }), 650)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
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
        <strong>注册前完成验证码校验</strong>
        <span>先向学校邮箱发送验证码，再提交账号和密码信息。</span>
      </div>
    </div>

    <div class="page-title">
      <div>
        <h1>注册账号</h1>
        <p>账号需要使用南京大学学校邮箱，并输入收到的 6 位验证码。</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="submit">
      <div class="field">
        <label for="email">
          <Mail class="label-icon" aria-hidden="true" />
          学校邮箱
        </label>
        <input id="email" v-model.trim="email" type="email" placeholder="student@smail.nju.edu.cn" required />
      </div>

      <div class="actions">
        <button class="button ghost" type="button" :disabled="sending || loading || !email" @click="sendCode">
          <RotateCcw class="button-icon" aria-hidden="true" />
          <span>{{ sending ? '发送中' : '发送验证码' }}</span>
        </button>
      </div>

      <div class="field">
        <label for="code">
          <ShieldCheck class="label-icon" aria-hidden="true" />
          验证码
        </label>
        <input id="code" v-model.trim="code" inputmode="numeric" maxlength="6" autocomplete="one-time-code" placeholder="123456" required />
      </div>

      <div class="grid two">
        <div class="field">
          <label for="password">
            <LockKeyhole class="label-icon" aria-hidden="true" />
            密码
          </label>
          <PasswordInput id="password" v-model="password" minlength="8" required />
        </div>
        <div class="field">
          <label for="confirm">确认密码</label>
          <PasswordInput id="confirm" v-model="confirmPassword" minlength="8" required />
        </div>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>
      <p v-if="success" class="success-message">{{ success }}</p>

      <div class="actions">
        <button class="button primary" type="submit" :disabled="loading">
          <UserPlus class="button-icon" aria-hidden="true" />
          <span>{{ loading ? '提交中' : '注册' }}</span>
        </button>
        <RouterLink class="button ghost" to="/login">返回登录</RouterLink>
      </div>
    </form>
  </section>
</template>
