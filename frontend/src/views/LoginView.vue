<script setup lang="ts">
import { BadgeCheck, BookOpen, GraduationCap, LockKeyhole, LogIn, Mail, UserPlus } from '@lucide/vue'
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

const highlights = [
  '学校邮箱登录与身份认证',
  '发布需求、申请接单、订单推进',
  '评价信用与通知中心联动'
]

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
  <section class="login-page" aria-labelledby="login-title">
    <div class="login-layout">
      <article class="site-card" aria-label="CampusHub website information">
        <span class="site-card__mark">
          <GraduationCap aria-hidden="true" />
        </span>

        <div class="site-card__copy">
          <p class="site-card__eyebrow">CampusHub / 校园互助服务平台</p>
          <h1 id="login-title">把校园里的小需求，变成可信的协作流程。</h1>
        </div>

        <div class="site-card__pattern" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>

        <div class="site-card__highlights">
          <p v-for="item in highlights" :key="item">
            <BadgeCheck aria-hidden="true" />
            <span>{{ item }}</span>
          </p>
        </div>
      </article>

      <form class="login-card" @submit.prevent="submit">
        <div class="login-card__heading">
          <h2>登录 CampusHub</h2>
          <p>使用南京大学邮箱进入任务大厅、订单和通知中心。</p>
        </div>

        <label class="login-field" for="email">
          <span>
            <Mail aria-hidden="true" />
            学校邮箱
          </span>
          <input id="email" v-model.trim="email" type="email" autocomplete="email" required />
        </label>

        <label class="login-field" for="password">
          <span>
            <LockKeyhole aria-hidden="true" />
            密码
          </span>
          <input id="password" v-model="password" type="password" autocomplete="current-password" required />
        </label>

        <p v-if="error" class="error-message">{{ error }}</p>

        <button class="login-submit" type="submit" :disabled="loading">
          <LogIn aria-hidden="true" />
          <span>{{ loading ? '登录中' : '登录' }}</span>
        </button>

        <div class="login-card__links">
          <RouterLink to="/register">
            <UserPlus aria-hidden="true" />
            注册账号
          </RouterLink>
          <RouterLink to="/forgot-password">
            <BookOpen aria-hidden="true" />
            忘记密码
          </RouterLink>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.login-page {
  --login-black: #111111;
  --login-card-text: #171717;
  --login-muted: #595959;
  --login-orange: #ff7629;
  --login-orange-dark: #e85e16;
  --login-white: #fffaf5;
  --login-line: rgba(17, 17, 17, 0.13);

  min-height: 100vh;
  width: calc(100vw - var(--sidebar-width));
  margin: calc(var(--space-8) * -1) 0 calc(var(--space-8) * -1) calc(50% - (100vw - var(--sidebar-width)) / 2);
  padding: clamp(28px, 5vw, 64px);
  display: grid;
  place-items: center;
  overflow: hidden;
  position: relative;
  background:
    linear-gradient(115deg, rgba(255, 118, 41, 0.18), transparent 26%),
    repeating-linear-gradient(138deg, rgba(255, 255, 255, 0.09) 0 1px, transparent 1px 13px),
    repeating-linear-gradient(34deg, rgba(255, 255, 255, 0.045) 0 2px, transparent 2px 34px),
    var(--login-black);
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.login-page::before {
  width: 360px;
  height: 360px;
  right: -130px;
  top: -130px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 118, 41, 0.34), transparent 68%);
}

.login-page::after {
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 42%, rgba(0, 0, 0, 0.22));
}

.login-layout {
  width: min(1120px, 100%);
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(430px, 1.02fr) minmax(360px, 0.86fr);
  gap: clamp(20px, 3vw, 34px);
  align-items: stretch;
}

.site-card,
.login-card {
  min-height: 520px;
  border-radius: 8px;
  color: var(--login-card-text);
  position: relative;
  overflow: hidden;
}

.site-card {
  padding: clamp(28px, 4vw, 44px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 34px;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 118, 41, 0.2), transparent 24%),
    linear-gradient(135deg, #ffffff, #fff8f1);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 28px 52px rgba(0, 0, 0, 0.32);
}

.site-card__mark {
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--login-orange);
  border-radius: 50%;
  box-shadow: 0 16px 28px rgba(255, 118, 41, 0.3);
}

.site-card__mark svg {
  width: 27px;
  height: 27px;
}

.site-card__copy {
  max-width: 610px;
  position: relative;
  z-index: 1;
}

.site-card__eyebrow {
  margin: 0 0 16px;
  color: var(--login-muted);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.site-card h1 {
  margin: 0;
  color: var(--login-card-text);
  font-family: var(--font-display);
  font-size: clamp(34px, 4.2vw, 54px);
  line-height: 1.06;
  letter-spacing: 0;
  font-weight: 900;
}

.site-card__pattern {
  width: min(300px, 52%);
  aspect-ratio: 1;
  position: absolute;
  right: -30px;
  bottom: -34px;
  border-radius: 50%;
  background:
    repeating-linear-gradient(135deg, rgba(17, 17, 17, 0.12) 0 2px, transparent 2px 13px),
    radial-gradient(circle, rgba(255, 118, 41, 0.16), transparent 62%);
  opacity: 0.86;
}

.site-card__pattern span {
  position: absolute;
  border: 2px solid rgba(255, 118, 41, 0.56);
  border-radius: 50%;
}

.site-card__pattern span:nth-child(1) {
  width: 120px;
  height: 120px;
  left: 18%;
  top: 18%;
}

.site-card__pattern span:nth-child(2) {
  width: 58px;
  height: 58px;
  right: 19%;
  top: 34%;
}

.site-card__pattern span:nth-child(3) {
  width: 22px;
  height: 22px;
  left: 45%;
  bottom: 22%;
  background: var(--login-orange);
}

.site-card__highlights {
  max-width: 430px;
  display: grid;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.site-card__highlights p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--login-card-text);
  font-size: 14px;
  font-weight: 900;
}

.site-card__highlights svg {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  color: var(--login-orange);
}

.login-card {
  align-self: stretch;
  padding: clamp(28px, 3.4vw, 42px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #ffffff;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0.12)),
    rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.46);
  box-shadow: 0 28px 52px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(145deg, rgba(255, 255, 255, 0.12) 0 1px, transparent 1px 18px),
    radial-gradient(circle at 100% 0%, rgba(255, 118, 41, 0.14), transparent 36%);
  pointer-events: none;
}

.login-card > * {
  position: relative;
  z-index: 1;
}

.login-card__heading {
  margin-bottom: 26px;
}

.login-card h2 {
  margin: 0;
  color: #ffffff;
  font-size: clamp(30px, 3vw, 38px);
  line-height: 1.12;
  font-weight: 900;
}

.login-card__heading p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  line-height: 1.7;
  font-weight: 800;
}

.login-field {
  display: grid;
  gap: 9px;
  margin-bottom: 16px;
}

.login-field span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
}

.login-field span svg {
  width: 15px;
  height: 15px;
  color: var(--login-orange);
}

.login-field input {
  width: 100%;
  height: 48px;
  padding: 0 15px;
  color: #ffffff;
  background: rgba(17, 17, 17, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
}

.login-field input:focus {
  border-color: var(--login-orange);
  background: rgba(17, 17, 17, 0.32);
  box-shadow: 0 0 0 4px rgba(255, 118, 41, 0.16);
  outline: none;
}

.error-message {
  margin: 0 0 14px;
  padding: 12px 13px;
  color: #b42318;
  background: rgba(255, 240, 237, 0.84);
  border: 1px solid rgba(180, 35, 24, 0.2);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
}

.login-submit {
  width: 100%;
  min-height: 50px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: #fff;
  background: linear-gradient(135deg, var(--login-orange), var(--login-orange-dark));
  border: 0;
  border-radius: 999px;
  box-shadow: 0 16px 28px rgba(255, 118, 41, 0.28);
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), opacity var(--transition-fast);
}

.login-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 22px 34px rgba(255, 118, 41, 0.34);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.login-submit svg {
  width: 17px;
  height: 17px;
}

.login-card__links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
}

.login-card__links a {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
  transition: transform var(--transition-fast), border-color var(--transition-fast), background var(--transition-fast);
}

.login-card__links a:hover {
  background: rgba(255, 255, 255, 0.48);
  border-color: rgba(255, 118, 41, 0.38);
  transform: translateY(-1px);
}

.login-card__links svg {
  width: 15px;
  height: 15px;
  color: var(--login-orange);
}

@media (max-width: 820px) {
  .login-layout {
    grid-template-columns: 1fr;
  }

  .site-card,
  .login-card {
    min-height: auto;
  }

  .site-card {
    gap: 44px;
  }
}

@media (max-width: 640px) {
  .login-page {
    min-height: 100vh;
    width: 100vw;
    margin: calc(var(--space-5) * -1) 0 calc(var(--space-5) * -1) calc(50% - 50vw);
    padding: 20px;
  }

  .site-card,
  .login-card {
    padding: 22px;
  }

  .site-card h1 {
    font-size: 32px;
  }

  .site-card__pattern {
    width: 220px;
    opacity: 0.46;
  }

  .login-card__links {
    grid-template-columns: 1fr;
  }
}
</style>
