<script setup lang="ts">
import { LockKeyhole, Mail, UserPlus } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')

async function submit() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await auth.register(email.value, password.value, confirmPassword.value)
    success.value = '注册成功，验证码已发送到学校邮箱。'
    window.setTimeout(() => router.push({ name: 'verify-email', query: { email: email.value } }), 650)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="form-panel">
    <div class="page-title">
      <div>
        <h1>注册账号</h1>
        <p>账号需要使用南京大学学校邮箱。</p>
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

      <div class="grid two">
        <div class="field">
          <label for="password">
            <LockKeyhole class="label-icon" aria-hidden="true" />
            密码
          </label>
          <input id="password" v-model="password" type="password" minlength="8" required />
        </div>
        <div class="field">
          <label for="confirm">确认密码</label>
          <input id="confirm" v-model="confirmPassword" type="password" minlength="8" required />
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
