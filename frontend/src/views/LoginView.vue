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
  <section class="form-panel auth-panel auth-view">
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

<style scoped>
.auth-view {
  animation-delay: 0.1s;
  border: 1.5px solid var(--border-light);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0, 0, 0, 0.03);
}

.auth-visual {
  padding: var(--space-5) var(--space-6);
  border: 1.5px solid var(--primary-100);
  background: linear-gradient(135deg, var(--primary-50), var(--secondary-50), var(--bg-body));
  position: relative;
  overflow: hidden;
}

.auth-visual::before {
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.08), transparent 70%);
}

.auth-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary-500), var(--secondary-600));
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.auth-icon svg {
  width: 24px;
  height: 24px;
}

.auth-visual strong {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.auth-visual span {
  font-size: 13px;
  line-height: 1.5;
}

.page-title h1 {
  font-size: 22px;
}

.page-title p {
  font-size: 14px;
  line-height: 1.6;
}

.form-panel .grid {
  gap: var(--space-4);
}

.form-panel .field input {
  padding: 11px 14px;
  font-size: 14px;
  border: 1.5px solid var(--border-light);
  background: var(--bg-body);
  transition: all var(--transition-fast);
}

.form-panel .field input:focus {
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1), var(--shadow-sm);
  background: var(--bg-surface);
}

.form-panel .button.primary {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.form-panel .button.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
}

.form-panel .button.ghost {
  padding: 10px 16px;
  font-weight: 600;
}

.form-panel .button.ghost:hover {
  background: var(--bg-hover);
  color: var(--primary-600);
}

.hint {
  font-size: 12.5px;
  font-weight: 500;
  padding: var(--space-2) var(--space-3);
  background: var(--bg-body);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.error-message {
  padding: 12px 16px;
  font-weight: 600;
  border: 1.5px solid rgba(239, 68, 68, 0.2);
}

.actions {
  gap: var(--space-3);
  margin-top: var(--space-2);
}
</style>
