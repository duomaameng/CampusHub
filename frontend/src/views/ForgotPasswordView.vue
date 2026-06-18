<script setup lang="ts">
import { KeyRound, Mail, RotateCcw } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import PasswordInput from '@/components/PasswordInput.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

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
    success.value = useMock ? '重置验证码已发送。mock 环境验证码为 123456。' : '重置验证码已发送，请查看学校邮箱。'
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
  <section class="form-panel auth-panel forgot-password-view">
    <div class="page-title">
      <div>
        <h1>重置密码</h1>
        <p>通过学校邮箱验证码重置密码。</p>
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

<style scoped>
.forgot-password-view {
  --reset-green: #7dbe8e;
  --reset-dark: #191a23;
  --reset-grey: #f3f3f3;
  --reset-line: #000000;
  max-width: 760px;
  border: 2px solid var(--reset-line);
  border-radius: 28px;
  background:
    radial-gradient(circle at 92% 8%, rgba(125, 190, 142, 0.82) 0 58px, transparent 60px),
    #ffffff;
  box-shadow: none;
}

.forgot-password-view::before,
.forgot-password-view::after {
  display: none;
}

.forgot-password-view :deep(.page-title) {
  margin-bottom: var(--space-6);
}

.forgot-password-view :deep(.page-title h1) {
  width: max-content;
  margin-bottom: 10px;
  padding: 5px 10px;
  border-radius: 24px;
  background: transparent;
  background-clip: border-box;
  -webkit-background-clip: border-box;
  color: #000000;
  -webkit-text-fill-color: #000000;
  font-size: 34px;
  line-height: 1.12;
  letter-spacing: 0;
}

.forgot-password-view :deep(.page-title p) {
  color: #2b2d35;
  font-size: 16px;
  font-weight: 700;
}

.forgot-password-view .grid {
  gap: 20px;
}

.forgot-password-view .field {
  gap: 9px;
  margin-bottom: 0;
}

.forgot-password-view .field label {
  color: #000000;
  font-size: 15px;
  font-weight: 900;
}

.forgot-password-view .label-icon {
  width: 16px;
  height: 16px;
  color: var(--reset-dark);
}

.forgot-password-view .field input,
.forgot-password-view :deep(.password-input-wrapper input) {
  min-height: 58px;
  padding: 16px 18px;
  border: 2px solid var(--reset-line);
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-size: 16px;
  box-shadow: none;
}

.forgot-password-view .field input:focus,
.forgot-password-view :deep(.password-input-wrapper input:focus) {
  border-color: var(--reset-line);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(125, 190, 142, 0.55);
}

.forgot-password-view :deep(.password-visibility-button) {
  right: 10px;
  color: var(--reset-dark);
}

.forgot-password-view :deep(.password-visibility-button:hover) {
  background: var(--reset-green);
  color: #000000;
}

.forgot-password-view .button {
  min-height: 52px;
  border: 2px solid var(--reset-line);
  border-radius: 14px;
  font-size: 15px;
  font-weight: 900;
  box-shadow: none;
}

.forgot-password-view .button.primary {
  background: var(--reset-dark);
  color: #ffffff;
}

.forgot-password-view .button.primary:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 0 0 4px rgba(125, 190, 142, 0.55);
  transform: translateY(-1px);
}

.forgot-password-view .button.ghost {
  background: #ffffff;
  color: #000000;
}

.forgot-password-view .button.ghost:hover:not(:disabled) {
  background: var(--reset-green);
  color: #000000;
  transform: translateY(-1px);
}

.forgot-password-view .button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.forgot-password-view .error-message,
.forgot-password-view .success-message {
  border-width: 2px;
  border-radius: 14px;
  font-weight: 900;
}

@media (max-width: 768px) {
  .forgot-password-view {
    margin: var(--space-4);
    padding: 26px;
    border-radius: 22px;
  }
}
</style>
