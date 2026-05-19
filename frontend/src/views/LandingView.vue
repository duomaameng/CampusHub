<script setup lang="ts">
import { LogIn, UserPlus, ArrowRight, Compass, Zap } from '@lucide/vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const mounted = ref(false)

onMounted(() => {
  // 触发动画的标记
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
    <!-- Animated Background Canvas -->
    <div class="landing-bg">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />
      <div class="orb orb-4" />
      <div class="mesh-grid" />
    </div>

    <!-- Floating Particles -->
    <div class="particles" aria-hidden="true">
      <span v-for="n in 24" :key="n" class="particle" :style="{ '--i': n }" />
    </div>

    <!-- Main Content -->
    <main class="landing-main" :class="{ 'is-mounted': mounted }">
      <!-- Brand -->
      <div class="landing-brand">
        <span class="brand-mark landing-brand-mark">
          <Zap class="brand-icon" aria-hidden="true" />
        </span>
        <span class="brand-word">CampusHub</span>
      </div>

      <!-- Headline -->
      <h1 class="landing-headline">
        <span class="line-1">连接校园</span>
        <span class="line-2">
          <span class="gradient-text">互助未来</span>
        </span>
      </h1>

      <!-- Subtitle -->
      <p class="landing-subtitle">
        一个为大学生打造的互助服务平台
        <br />
        让每一份需求都被看见，让每一次帮助都有价值
      </p>

      <!-- Actions -->
      <div class="landing-actions">
        <RouterLink to="/login" class="landing-btn landing-btn-primary">
          <LogIn class="btn-icon" aria-hidden="true" />
          <span>登录</span>
        </RouterLink>
        <RouterLink to="/register" class="landing-btn landing-btn-secondary">
          <UserPlus class="btn-icon" aria-hidden="true" />
          <span>注册</span>
        </RouterLink>
        <button class="landing-btn landing-btn-ghost" type="button" @click="goExplore">
          <Compass class="btn-icon" aria-hidden="true" />
          <span>暂不登录，先逛逛</span>
          <ArrowRight class="btn-icon-sm" aria-hidden="true" />
        </button>
      </div>

      <!-- Scroll Hint -->
      <div class="scroll-hint">
        <span class="scroll-line" />
      </div>
    </main>

    <!-- Footer Meta -->
    <footer class="landing-footer">
      <span>Vue 3 · Mock API · 校园互助演示</span>
    </footer>
  </div>
</template>

<style scoped>
/* ===== Root ===== */
.landing-root {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: #050505;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

/* ===== Background Orbs ===== */
.landing-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0;
  animation: orbFloat 20s ease-in-out infinite, orbFadeIn 2s ease forwards;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.35), transparent 70%);
  top: -10%;
  left: -5%;
  animation-delay: 0s, 0.2s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.25), transparent 70%);
  bottom: -10%;
  right: -5%;
  animation-delay: -7s, 0.4s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.2), transparent 70%);
  top: 40%;
  left: 60%;
  animation-delay: -14s, 0.6s;
}

.orb-4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.15), transparent 70%);
  top: 60%;
  left: 15%;
  animation-delay: -10s, 0.8s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -20px) scale(1.05); }
  50% { transform: translate(-20px, 30px) scale(0.95); }
  75% { transform: translate(20px, 20px) scale(1.02); }
}

@keyframes orbFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ===== Mesh Grid ===== */
.mesh-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
}

/* ===== Particles ===== */
.particles {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 2px;
  height: 2px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  left: calc(var(--i) * 4%);
  bottom: -10px;
  animation: particleRise 12s linear infinite;
  animation-delay: calc(var(--i) * -0.5s);
  opacity: 0;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) scale(0);
    opacity: 0;
  }
  10% {
    opacity: 0.6;
  }
  90% {
    opacity: 0.3;
  }
  100% {
    transform: translateY(-110vh) scale(1);
    opacity: 0;
  }
}

/* ===== Main Content ===== */
.landing-main {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 720px;
  padding: 0 24px;
}

/* Entrance Animations */
.landing-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

.landing-brand-mark {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 0 30px rgba(99, 102, 241, 0.4);
  animation: markPulse 3s ease-in-out infinite;
}

@keyframes markPulse {
  0%, 100% { box-shadow: 0 0 30px rgba(99, 102, 241, 0.4); }
  50% { box-shadow: 0 0 50px rgba(99, 102, 241, 0.6); }
}

.brand-word {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #fff 30%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.landing-headline {
  font-size: clamp(40px, 8vw, 72px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.04em;
  margin-bottom: 24px;
  color: #fff;
}

.line-1 {
  display: block;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.15s;
}

.line-2 {
  display: block;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.3s;
}

.gradient-text {
  background: linear-gradient(135deg, #818cf8 0%, #c084fc 50%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.landing-subtitle {
  font-size: 16px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.55);
  max-width: 480px;
  margin-bottom: 48px;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.5s;
}

.landing-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: 0.7s;
}

/* Trigger animations when mounted */
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

/* ===== Buttons ===== */
.landing-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  cursor: pointer;
  text-decoration: none;
  border: none;
  position: relative;
  overflow: hidden;
}

.landing-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.1), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.landing-btn:hover::before {
  opacity: 1;
}

.landing-btn-primary {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.35), inset 0 1px 0 rgba(255,255,255,0.1);
}

.landing-btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.5), inset 0 1px 0 rgba(255,255,255,0.15);
}

.landing-btn-secondary {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.landing-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-3px);
}

.landing-btn-ghost {
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  padding: 14px 20px;
}

.landing-btn-ghost:hover {
  color: rgba(255, 255, 255, 0.85);
  transform: translateY(-2px);
}

.btn-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.btn-icon-sm {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  opacity: 0.6;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.landing-btn-ghost:hover .btn-icon-sm {
  transform: translateX(3px);
  opacity: 1;
}

/* ===== Scroll Hint ===== */
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
  animation: scrollHintIn 0.6s ease 1.4s forwards;
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(255,255,255,0.3), transparent);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollHintIn {
  from { opacity: 0; transform: translateX(-50%) translateY(10px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.3; transform: scaleY(1); }
  50% { opacity: 0.8; transform: scaleY(1.2); }
}

/* ===== Footer ===== */
.landing-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  z-index: 2;
  opacity: 0;
  animation: footerIn 0.6s ease 1.6s forwards;
}

@keyframes footerIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .landing-headline {
    font-size: 36px;
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
    margin-bottom: 32px;
  }
}
</style>
