<script setup lang="ts">
import { KeyRound, Mail, RotateCcw } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import PasswordInput from '@/components/PasswordInput.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmNewPassword = ref('')
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')

async function sendCode() {
  error.value = ''
  success.value = ''
  sending.value = true
  try {
    await auth.sendVerificationCode(email.value, 'RESET_PASSWORD')
    success.value = '重置验证码已发送。mock 环境验证码为 123456。'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function resetPassword() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await auth.resetPassword(email.value, code.value, newPassword.value, confirmNewPassword.value)
    success.value = '密码重置成功，请使用新密码登录。'
    window.setTimeout(() => router.push('/login'), 800)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码重置失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="form-panel auth-panel">
    <div class="page-title">
      <div>
        <h1>重置密码</h1>
        <p>通过学校邮箱验证码重置密码。mock 环境验证码固定为 123456。</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="resetPassword">
      <div class="field">
        <label for="email">
          <Mail class="label-icon" aria-hidden="true" />
          学校邮箱
        </label>
        <input id="email" v-model.trim="email" type="email" required />
      </div>

      <div class="actions">
        <button class="button ghost" type="button" :disabled="sending || !email" @click="sendCode">
          <RotateCcw class="button-icon" aria-hidden="true" />
          <span>{{ sending ? '发送中' : '发送验证码' }}</span>
        </button>
      </div>

      <div class="field">
        <label for="code">验证码</label>
        <input id="code" v-model.trim="code" inputmode="numeric" maxlength="6" required />
      </div>

      <div class="grid two">
        <div class="field">
          <label for="password">
            <KeyRound class="label-icon" aria-hidden="true" />
            新密码
          </label>
          <PasswordInput id="password" v-model="newPassword" minlength="8" required />
        </div>
        <div class="field">
          <label for="confirm">确认新密码</label>
          <PasswordInput id="confirm" v-model="confirmNewPassword" minlength="8" required />
        </div>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>
      <p v-if="success" class="success-message">{{ success }}</p>

      <div class="actions">
        <button class="button primary" type="submit" :disabled="loading">
          <KeyRound class="button-icon" aria-hidden="true" />
          <span>{{ loading ? '重置中' : '重置密码' }}</span>
        </button>
        <RouterLink class="button ghost" to="/login">返回登录</RouterLink>
      </div>
    </form>
  </section>
</template>
