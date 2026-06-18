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
  <section class="form-panel auth-panel register-view">
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

<style scoped>
.register-view {
  --register-green: #7dbe8e;
  --register-dark: #191a23;
  --register-grey: #f3f3f3;
  position: relative;
  max-width: 760px;
  padding: 34px;
  border: 2px solid #000000;
  border-radius: 30px;
  background:
    radial-gradient(circle at 96% 6%, var(--register-green) 0 72px, transparent 73px),
    #ffffff;
  box-shadow: none;
  overflow: hidden;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.register-view::before,
.register-view::after {
  display: none;
}

.auth-visual {
  position: relative;
  padding: 22px 160px 22px 24px;
  border: 2px solid #000000;
  border-radius: 24px;
  background: var(--register-grey);
  box-shadow: none;
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
    radial-gradient(circle at 72% 28%, var(--register-green) 0 18px, transparent 19px),
    #ffffff;
  transform: rotate(-7deg);
}

.auth-icon {
  width: 52px;
  height: 52px;
  border: 2px solid #000000;
  border-radius: 14px;
  background: var(--register-green);
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
  padding: 5px 14px;
  border-radius: 18px;
  background: transparent;
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

.field input,
:deep(.password-input) {
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

.field input:focus,
:deep(.password-input:focus) {
  border-color: #000000;
  box-shadow: 0 0 0 3px rgba(125, 190, 142, 0.48);
}

:deep(.password-visibility-button) {
  color: #000000;
}

.button {
  border: 2px solid #000000;
  border-radius: 14px;
  font-weight: 900;
  box-shadow: none;
}

.button.primary {
  color: #ffffff;
  background: var(--register-dark);
}

.button.primary:hover:not(:disabled) {
  color: #000000;
  background: var(--register-green);
  box-shadow: none;
}

.button.ghost {
  color: #000000;
  background: #ffffff;
}

.button.ghost:hover:not(:disabled) {
  color: #000000;
  background: var(--register-green);
  box-shadow: none;
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
  box-shadow: none;
  font-weight: 800;
}

@media (max-width: 768px) {
  .register-view {
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
