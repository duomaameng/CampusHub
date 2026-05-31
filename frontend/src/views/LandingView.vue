<script setup lang="ts">
import { LogIn, UserPlus, ArrowRight, Compass, GraduationCap } from '@lucide/vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const mounted = ref(false)

onMounted(() => {
  requestAnimationFrame(() => {
    mounted.value = true
  })
})

function goExplore() {
  router.push('/tasks')
}
</script>

<template>
  <div class="landing-root">
    <div class="landing-bg">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />
      <div class="orb orb-4" />
      <div class="mesh-grid" />
      <div class="noise-overlay" />
    </div>

    <div class="particles" aria-hidden="true">
      <span v-for="n in 30" :key="n" class="particle" :style="{ '--i': n }" />
    </div>

    <main class="landing-main" :class="{ 'is-mounted': mounted }">
      <div class="landing-brand">
        <span class="brand-mark landing-brand-mark">
          <GraduationCap class="brand-icon" aria-hidden="true" />
        </span>
        <span class="brand-word">CampusHub</span>
      </div>

      <h1 class="landing-headline">
        <span class="line-1">连接校园</span>
        <span class="line-2">
          <span class="gradient-text">互助未来</span>
        </span>
      </h1>

      <p class="landing-subtitle">
        一个为大学生打造的互助服务平台
        <br />
        让每一份需求都被看见，让每一次帮助都有价值
      </p>

      <div class="landing-actions">
        <button v-if="auth.isAuthenticated" class="landing-btn landing-btn-primary" type="button" @click="goExplore">
          <Compass class="btn-icon" aria-hidden="true" />
          <span>进入任务大厅</span>
          <ArrowRight class="btn-icon-sm" aria-hidden="true" />
        </button>
        <RouterLink v-if="!auth.isAuthenticated" to="/login" class="landing-btn landing-btn-primary">
          <LogIn class="btn-icon" aria-hidden="true" />
          <span>登录</span>
        </RouterLink>
        <RouterLink v-if="!auth.isAuthenticated" to="/register" class="landing-btn landing-btn-secondary">
          <UserPlus class="btn-icon" aria-hidden="true" />
          <span>注册</span>
        </RouterLink>
        <button v-if="!auth.isAuthenticated" class="landing-btn landing-btn-ghost" type="button" @click="goExplore">
          <Compass class="btn-icon" aria-hidden="true" />
          <span>暂不登录，先逛逛</span>
          <ArrowRight class="btn-icon-sm" aria-hidden="true" />
        </button>
      </div>

      <div class="scroll-hint">
        <span class="scroll-line" />
      </div>
    </main>

    <footer class="landing-footer">
      <span>Vue 3 · Mock API · 校园互助演示</span>
    </footer>
  </div>
</template>

<style scoped>
.landing-root {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: #08090e;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #e8eaf0;
}

.landing-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0;
  animation: orbFloat 25s ease-in-out infinite, orbFadeIn 2.5s ease forwards;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.25), transparent 70%);
  top: -15%;
  left: -10%;
  animation-delay: 0s, 0.2s;
}

.orb-2 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.18), transparent 70%);
  bottom: -15%;
  right: -8%;
  animation-delay: -8s, 0.4s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12), transparent 70%);
  top: 35%;
  left: 55%;
  animation-delay: -16s, 0.6s;
}

.orb-4 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.1), transparent 70%);
  top: 55%;
  left: 10%;
  animation-delay: -12s, 0.8s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -30px) scale(1.06); }
  50% { transform: translate(-30px, 40px) scale(0.94); }
  75% { transform: translate(25px, 25px) scale(1.03); }
}

@keyframes orbFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.mesh-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.015) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse at center, black 20%, transparent 65%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 20%, transparent 65%);
}

.noise-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  background-size: 128px 128px;
  pointer-events: none;
}

.particles {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  --i: 0;
  position: absolute;
  width: 2px;
  height: 2px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  left: calc(var(--i) * 3.2%);
  bottom: -10px;
  animation: particleRise 14s linear infinite;
  animation-delay: calc(var(--i) * -0.45s);
  opacity: 0;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) scale(0);
    opacity: 0;
  }
  8% {
    opacity: 0.5;
  }
  92% {
    opacity: 0.2;
  }
  100% {
    transform: translateY(-110vh) scale(1);
    opacity: 0;
  }
}

.landing-main {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 680px;
  padding: 0 24px;
}

.landing-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 56px;
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}

.landing-brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  display: grid;
  place-items: center;
  position: relative;
}

.landing-brand-mark::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.4), rgba(139, 92, 246, 0.2));
  filter: blur(12px);
  z-index: -1;
  animation: markPulse 4s ease-in-out infinite;
}

@keyframes markPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

.landing-brand-mark .brand-icon {
  width: 20px;
  height: 20px;
}

.brand-word {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #f0f1f5 30%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.landing-headline {
  font-size: clamp(42px, 8vw, 76px);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: -0.045em;
  margin-bottom: 28px;
  color: #f0f1f5;
}

.line-1 {
  display: block;
  opacity: 0;
  transform: translateY(48px);
  transition: all 1s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.15s;
}

.line-2 {
  display: block;
  opacity: 0;
  transform: translateY(48px);
  transition: all 1s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.3s;
}

.gradient-text {
  background: linear-gradient(135deg, #818cf8 0%, #a78bfa 35%, #c084fc 65%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  background-size: 200% auto;
  animation: gradientText 6s ease-in-out infinite;
}

@keyframes gradientText {
  0% { background-position: 0% center; }
  50% { background-position: 100% center; }
  100% { background-position: 0% center; }
}

.landing-subtitle {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(232, 234, 240, 0.45);
  max-width: 440px;
  margin-bottom: 52px;
  opacity: 0;
  transform: translateY(32px);
  transition: all 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.5s;
}

.landing-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
  opacity: 0;
  transform: translateY(32px);
  transition: all 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.7s;
}

.landing-main.is-mounted .landing-brand {
  opacity: 1;
  transform: translateY(0);
}
.landing-main.is-mounted .line-1 {
  opacity: 1;
  transform: translateY(0);
}
.landing-main.is-mounted .line-2 {
  opacity: 1;
  transform: translateY(0);
}
.landing-main.is-mounted .landing-subtitle {
  opacity: 1;
  transform: translateY(0);
}
.landing-main.is-mounted .landing-actions {
  opacity: 1;
  transform: translateY(0);
}

.landing-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 13px 26px;
  border-radius: 12px;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.005em;
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  cursor: pointer;
  text-decoration: none;
  border: none;
  position: relative;
  overflow: hidden;
}

.landing-btn-primary {
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #f0f1f5;
  box-shadow: 0 2px 16px rgba(99, 102, 241, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.landing-btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.landing-btn-secondary {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(232, 234, 240, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
}

.landing-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-3px);
}

.landing-btn-ghost {
  background: transparent;
  color: rgba(232, 234, 240, 0.4);
  padding: 13px 20px;
}

.landing-btn-ghost:hover {
  color: rgba(232, 234, 240, 0.8);
  transform: translateY(-2px);
}

.btn-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.btn-icon-sm {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
  opacity: 0.5;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.landing-btn-ghost:hover .btn-icon-sm {
  transform: translateX(4px);
  opacity: 1;
}

.scroll-hint {
  position: absolute;
  bottom: -80px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0;
  animation: scrollHintIn 0.6s ease 1.5s forwards;
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent);
  animation: scrollPulse 2.5s ease-in-out infinite;
}

@keyframes scrollHintIn {
  from { opacity: 0; transform: translateX(-50%) translateY(10px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.2; transform: scaleY(1); }
  50% { opacity: 0.6; transform: scaleY(1.15); }
}

.landing-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 10px;
  color: rgba(232, 234, 240, 0.15);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  z-index: 2;
  opacity: 0;
  animation: footerIn 0.6s ease 1.8s forwards;
}

@keyframes footerIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 640px) {
  .landing-headline {
    font-size: 38px;
  }
  .landing-subtitle {
    font-size: 14px;
  }
  .landing-actions {
    flex-direction: column;
    width: 100%;
    max-width: 280px;
  }
  .landing-btn {
    width: 100%;
    justify-content: center;
  }
  .landing-brand {
    margin-bottom: 36px;
  }
}
</style>
