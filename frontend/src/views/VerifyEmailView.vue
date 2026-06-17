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
  <section class="form-panel auth-panel verify-email-view">
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

<style scoped>
.verify-email-view {
  --verify-green: #7dbe8e;
  --verify-dark: #191a23;
  --verify-grey: #f3f3f3;
  position: relative;
  max-width: 720px;
  padding: 34px;
  border: 2px solid #000000;
  border-radius: 30px;
  background:
    radial-gradient(circle at 96% 6%, var(--verify-green) 0 72px, transparent 73px),
    #ffffff;
  box-shadow: 0 8px 0 #000000;
  overflow: hidden;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.verify-email-view::before,
.verify-email-view::after {
  display: none;
}

.auth-visual {
  position: relative;
  padding: 22px 160px 22px 24px;
  border: 2px solid #000000;
  border-radius: 24px;
  background: var(--verify-grey);
  box-shadow: 0 5px 0 #000000;
  overflow: hidden;
}

.auth-visual::before {
  display: none;
}

.auth-visual::after {
  content: '';
  position: absolute;
  right: 28px;
  bottom: 20px;
  width: 100px;
  height: 58px;
  border: 2px solid #000000;
  border-radius: 20px;
  background:
    radial-gradient(circle at 72% 28%, var(--verify-green) 0 18px, transparent 19px),
    #ffffff;
  transform: rotate(-7deg);
}

.auth-icon {
  width: 52px;
  height: 52px;
  border: 2px solid #000000;
  border-radius: 14px;
  background: var(--verify-green);
  color: #000000;
  box-shadow: none;
}

.auth-visual strong {
  color: #000000;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.24;
}

.auth-visual span {
  color: #4a4e5b;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.6;
}

.page-title {
  margin: 26px 0 22px;
}

.page-title h1 {
  width: max-content;
  max-width: 100%;
  padding: 5px 14px;
  border-radius: 18px;
  background: var(--verify-green);
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
}

.page-title p {
  margin-top: 12px;
  max-width: 560px;
  color: #3f4350;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.65;
}

.field label {
  color: #4a4e5b;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: none;
}

.field input {
  min-height: 50px;
  padding: 11px 15px;
  color: #000000;
  font-size: 14px;
  font-weight: 800;
  border: 2px solid #000000;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: none;
}

.field input:focus {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(125, 190, 142, 0.48);
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: 0 4px 0 #000000;
}

.button.primary {
  color: #ffffff;
  background: var(--verify-dark);
}

.button.primary:hover:not(:disabled) {
  color: #000000;
  background: var(--verify-green);
  box-shadow: 0 5px 0 #000000;
}

.button.ghost {
  color: #000000;
  background: #ffffff;
}

.button.ghost:hover:not(:disabled) {
  color: #000000;
  background: var(--verify-green);
  box-shadow: 0 5px 0 #000000;
}

.button:disabled {
  opacity: 0.5;
  box-shadow: none;
}

.error-message,
.success-message {
  padding: 12px 16px;
  border: 2px solid #000000;
  border-radius: 18px;
  box-shadow: 0 3px 0 #000000;
  font-weight: 800;
}

@media (max-width: 768px) {
  .verify-email-view {
    padding: 24px;
  }

  .auth-visual {
    padding: 20px;
  }

  .auth-visual::after {
    display: none;
  }
}
</style>
