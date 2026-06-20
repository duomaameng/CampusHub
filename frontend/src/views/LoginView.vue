<script setup lang="ts">
import { ArrowRight, LockKeyhole, LogIn, Mail, Sparkles, UserPlus } from '@lucide/vue'
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import PasswordInput from '@/components/PasswordInput.vue'
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
    error.value = err instanceof Error ? err.message : '登录失败，请检查邮箱和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-page">
    <div class="login-shell">
      <aside class="login-story" aria-label="CampusHub 登录说明">
        <div class="story-label">
          <Sparkles class="story-label-icon" aria-hidden="true" />
          <span>CampusHub</span>
        </div>
        <h1>
          校园互助，
          <mark>从可信登录开始</mark>
        </h1>
      </aside>

      <div class="login-card">
        <div class="login-card-heading">
          <span class="section-tag">登录</span>
          <div>
            <h2>欢迎回到 CampusHub</h2>
            <p>使用学校邮箱进入你的校园互助工作台。</p>
          </div>
        </div>

        <form class="login-form" @submit.prevent="submit">
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
            <PasswordInput id="password" v-model="password" autocomplete="current-password" required />
          </div>

          <p class="hint">演示账号：student.demo1@smail.nju.edu.cn / CampusHub123!</p>
          <p v-if="error" class="error-message">{{ error }}</p>

          <div class="actions">
            <button class="button primary login-submit" type="submit" :disabled="loading">
              <LogIn class="button-icon" aria-hidden="true" />
              <span>{{ loading ? '登录中' : '登录' }}</span>
              <ArrowRight class="button-icon" aria-hidden="true" />
            </button>
            <RouterLink class="button ghost" to="/forgot-password">忘记密码</RouterLink>
          </div>
        </form>

        <div class="signup-strip">
          <span>还没有账号？</span>
          <RouterLink to="/register">
            <UserPlus class="button-icon" aria-hidden="true" />
            <span>注册 CampusHub</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-page {
  --positivus-green: #ffb454;
  --positivus-dark: #191a23;
  --positivus-grey: #f3f3f3;
  --positivus-line: #000000;
  width: 100%;
  min-height: calc(100vh - 48px);
  display: grid;
  place-items: center;
  padding: clamp(20px, 4vw, 54px);
  color: var(--positivus-dark);
}

.login-shell {
  width: min(1060px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(360px, 1fr);
  gap: clamp(24px, 4vw, 54px);
  align-items: stretch;
}

.login-story,
.login-card {
  border: 2px solid var(--positivus-line);
  border-radius: 28px;
  box-shadow: none;
}

.login-story {
  position: relative;
  overflow: hidden;
  min-height: 560px;
  padding: clamp(30px, 4vw, 52px);
  background:
    radial-gradient(circle at 88% 16%, rgba(255, 180, 84, 0.78) 0 74px, transparent 76px),
    linear-gradient(145deg, #ffffff 0%, var(--positivus-grey) 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.login-story::before,
.login-story::after {
  content: '';
  position: absolute;
  z-index: 0;
  border: 2px solid var(--positivus-line);
  pointer-events: none;
}

.login-story::before {
  right: -68px;
  bottom: 92px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: var(--positivus-green);
}

.login-story::after {
  right: 92px;
  bottom: 52px;
  width: 74px;
  height: 74px;
  border-radius: 18px;
  background: #ffffff;
  transform: rotate(12deg);
}

.story-label,
.section-tag {
  position: relative;
  z-index: 1;
  width: max-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 7px;
  background: var(--positivus-green);
  color: #000000;
  font-weight: 800;
  line-height: 1;
}

.story-label-icon {
  width: 16px;
  height: 16px;
}

.login-story h1 {
  position: relative;
  z-index: 2;
  max-width: 460px;
  margin: 36px 0 clamp(120px, 18vh, 180px);
  font-family: var(--font-display);
  font-size: clamp(40px, 5vw, 60px);
  font-weight: 800;
  line-height: 1.24;
  letter-spacing: 0;
  color: #000000;
}

.login-story mark {
  display: inline;
  padding: 0 7px 4px;
  border-radius: 7px;
  background: transparent;
  color: #000000;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
}

.login-card {
  padding: clamp(30px, 4vw, 48px);
  background: #ffffff;
}

.login-card-heading {
  display: grid;
  gap: 20px;
  margin-bottom: 34px;
}

.login-card h2 {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: clamp(32px, 4vw, 43px);
  line-height: 1.08;
  font-weight: 800;
  letter-spacing: 0;
  color: #000000;
}

.login-card-heading p {
  margin: 0;
  color: #4a4d57;
  font-size: 17px;
  line-height: 1.55;
}

.login-form {
  display: grid;
  gap: 20px;
}

.field {
  gap: 9px;
  margin-bottom: 0;
}

.field label {
  color: #000000;
  font-size: 15px;
  font-weight: 800;
}

.label-icon {
  width: 16px;
  height: 16px;
  color: var(--positivus-dark);
}

.field input,
:deep(.password-input-wrapper input) {
  min-height: 58px;
  padding: 16px 18px;
  border: 2px solid var(--positivus-line);
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-size: 16px;
  box-shadow: none;
}

.field input:focus,
:deep(.password-input-wrapper input:focus) {
  border-color: var(--positivus-line);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(255, 180, 84, 0.55);
}

:deep(.password-visibility-button) {
  right: 10px;
  color: var(--positivus-dark);
}

:deep(.password-visibility-button:hover) {
  background: var(--positivus-green);
  color: #000000;
}

.hint {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--positivus-grey);
  color: #383b45;
  font-size: 13px;
  line-height: 1.5;
}

.error-message {
  margin: 0;
  border: 2px solid #ef4444;
  border-radius: 14px;
  background: #fff5f5;
  color: #b91c1c;
}

.actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  margin-top: 4px;
}

.button {
  min-height: 58px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
}

.button.primary {
  background: var(--positivus-dark);
  color: #ffffff;
  border: 2px solid var(--positivus-dark);
  box-shadow: none;
}

.button.primary:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 0 0 4px rgba(255, 180, 84, 0.65);
  transform: translateY(-2px);
}

.button.primary:disabled {
  opacity: 0.62;
}

.button.ghost {
  padding-inline: 20px;
  border: 2px solid var(--positivus-line);
  color: #000000;
  background: #ffffff;
}

.button.ghost:hover {
  background: var(--positivus-green);
  color: #000000;
}

.login-submit .button-icon:last-child {
  transition: transform var(--transition-fast);
}

.login-submit:hover .button-icon:last-child {
  transform: translateX(3px);
}

.signup-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 2px solid var(--positivus-line);
  color: #4a4d57;
  font-size: 15px;
}

.signup-strip a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #000000;
  font-weight: 800;
  text-decoration: none;
}

.signup-strip a:hover {
  color: #b45309;
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-story {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-story,
  .login-card {
    border-radius: 20px;
    box-shadow: none;
  }

  .login-story {
    padding: 26px;
  }

  .login-story h1 {
    margin-top: 28px;
    font-size: 39px;
  }

  .login-story p {
    font-size: 16px;
  }

  .login-card {
    padding: 26px;
  }

  .actions,
  .signup-strip {
    grid-template-columns: 1fr;
  }

  .actions {
    display: grid;
  }

  .signup-strip {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>




