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
  --positivus-green: #7dbe8e;
  --positivus-dark: #191a23;
  --positivus-grey: #f3f3f3;
  --positivus-line: #000000;
  position: fixed;
  inset: 0;
  z-index: 100;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: clamp(18px, 4vw, 48px);
  background:
    linear-gradient(90deg, rgba(25, 26, 35, 0.045) 1px, transparent 1px),
    linear-gradient(0deg, rgba(25, 26, 35, 0.045) 1px, transparent 1px),
    #ffffff;
  background-size: 52px 52px;
  color: var(--positivus-dark);
}

.landing-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border: 2px solid var(--positivus-line);
  opacity: 1;
}

.orb-1 {
  width: clamp(220px, 28vw, 430px);
  height: clamp(220px, 28vw, 430px);
  right: -120px;
  top: -92px;
  border-radius: 50%;
  background: var(--positivus-green);
}

.orb-2 {
  width: clamp(180px, 24vw, 330px);
  height: clamp(180px, 24vw, 330px);
  left: -110px;
  bottom: -120px;
  border-radius: 50%;
  background: var(--positivus-dark);
}

.orb-3 {
  width: 94px;
  height: 94px;
  right: 14%;
  bottom: 14%;
  border-radius: 22px;
  background: #ffffff;
  transform: rotate(12deg);
}

.orb-4 {
  width: 58px;
  height: 58px;
  left: 15%;
  top: 18%;
  border-radius: 50%;
  background: var(--positivus-green);
}

.mesh-grid {
  position: absolute;
  left: 7%;
  top: 9%;
  width: 120px;
  height: 120px;
  background:
    radial-gradient(circle, var(--positivus-dark) 2px, transparent 2px);
  background-size: 18px 18px;
  opacity: 0.42;
}

.noise-overlay {
  position: absolute;
  right: 8%;
  bottom: 12%;
  width: 210px;
  height: 210px;
  border: 2px solid var(--positivus-line);
  border-radius: 50%;
  opacity: 0.12;
}

.particles {
  display: none;
}

.landing-main {
  position: relative;
  z-index: 2;
  width: min(860px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: clamp(34px, 6vw, 70px);
  border: 2px solid var(--positivus-line);
  border-radius: 34px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 9px 0 var(--positivus-line);
}

.landing-main::before {
  content: '';
  position: absolute;
  right: clamp(22px, 5vw, 56px);
  top: clamp(22px, 5vw, 48px);
  width: 88px;
  height: 88px;
  border: 2px solid var(--positivus-line);
  border-radius: 20px;
  background: var(--positivus-green);
  transform: rotate(-9deg);
  pointer-events: none;
}

.landing-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 38px;
  padding: 8px 14px;
  border-radius: 7px;
  background: var(--positivus-green);
  color: #000000;
  opacity: 0;
  transform: translateY(18px);
  transition: all 0.55s ease;
}

.landing-brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--positivus-dark);
  color: var(--positivus-green);
  display: grid;
  place-items: center;
}

.landing-brand-mark .brand-icon {
  width: 18px;
  height: 18px;
}

.brand-word {
  color: #000000;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
}

.landing-headline {
  position: relative;
  z-index: 1;
  margin: 0 0 clamp(56px, 8vh, 88px);
  color: #000000;
  font-family: var(--font-display);
  font-size: clamp(48px, 8vw, 86px);
  font-weight: 900;
  line-height: 1.24;
  letter-spacing: 0;
}

.line-1,
.line-2 {
  display: block;
  opacity: 0;
  transform: translateY(28px);
  transition: all 0.65s ease;
}

.line-2 {
  transition-delay: 0.08s;
}

.gradient-text {
  display: inline;
  padding: 0 10px 6px;
  border-radius: 7px;
  background: var(--positivus-green);
  color: #000000;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
}

.landing-subtitle {
  position: relative;
  z-index: 1;
  max-width: 560px;
  margin: 0 0 38px;
  color: #2b2d35;
  font-size: clamp(16px, 2vw, 20px);
  line-height: 1.7;
  opacity: 0;
  transform: translateY(22px);
  transition: all 0.65s ease 0.14s;
}

.landing-actions {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
  opacity: 0;
  transform: translateY(22px);
  transition: all 0.65s ease 0.2s;
}

.landing-main.is-mounted .landing-brand,
.landing-main.is-mounted .line-1,
.landing-main.is-mounted .line-2,
.landing-main.is-mounted .landing-subtitle,
.landing-main.is-mounted .landing-actions {
  opacity: 1;
  transform: translateY(0);
}

.landing-btn {
  min-height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 28px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
  text-decoration: none;
  border: 2px solid var(--positivus-line);
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
}

.landing-btn-primary {
  background: var(--positivus-dark);
  color: #ffffff;
  box-shadow: none;
}

.landing-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 0 4px rgba(125, 190, 142, 0.65);
}

.landing-btn-secondary {
  background: #ffffff;
  color: #000000;
}

.landing-btn-secondary:hover,
.landing-btn-ghost:hover {
  background: var(--positivus-green);
  color: #000000;
  transform: translateY(-2px);
}

.landing-btn-ghost {
  background: var(--positivus-grey);
  color: #000000;
}

.btn-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.btn-icon-sm {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}

.landing-btn:hover .btn-icon-sm {
  transform: translateX(3px);
}

.scroll-hint {
  display: none;
}

.landing-footer {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 24px;
  text-align: center;
  color: rgba(25, 26, 35, 0.55);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

@media (max-width: 640px) {
  .landing-root {
    padding: 16px;
  }

  .landing-main {
    padding: 32px 22px;
    border-radius: 26px;
  }

  .landing-main::before {
    width: 58px;
    height: 58px;
    right: 18px;
    top: 18px;
  }

  .landing-brand {
    margin-bottom: 30px;
  }

  .landing-headline {
    font-size: 42px;
  }

  .landing-subtitle {
    font-size: 15px;
  }

  .landing-actions {
    width: 100%;
    flex-direction: column;
  }

  .landing-btn {
    width: 100%;
  }

  .landing-footer {
    display: none;
  }
}
</style>
