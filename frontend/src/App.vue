<script setup lang="ts">
import {
  Bell,
  ClipboardList,
  GraduationCap,
  Home,
  LogIn,
  LogOut,
  Megaphone,
  Moon,
  PlusCircle,
  ShieldCheck,
  Sparkles,
  Sun,
  UserPlus,
  UserRound
} from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const isLanding = computed(() => route.name === 'landing')
const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const workspaceStatus = computed(() => (useMock ? 'Mock API' : 'Backend API'))

const isDark = ref(false)

function initTheme() {
  const stored = localStorage.getItem('campus-hub-theme')
  if (stored === 'dark') {
    isDark.value = true
    document.documentElement.setAttribute('data-theme', 'dark')
  } else if (stored === 'light') {
    isDark.value = false
    document.documentElement.setAttribute('data-theme', 'light')
  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    isDark.value = true
  }
}

function toggleTheme() {
  isDark.value = !isDark.value
  const theme = isDark.value ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('campus-hub-theme', theme)
}

onMounted(async () => {
  initTheme()
  if (auth.token) {
    try {
      await auth.loadMe()
      await auth.refreshUnread()
    } catch {
      await auth.logout()
    }
  }
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <template v-if="isLanding">
    <RouterView />
  </template>

  <div v-else class="app-shell">
    <aside class="sidebar-shell">
      <RouterLink to="/tasks" class="brand">
        <span class="brand-mark">
          <GraduationCap class="brand-icon" aria-hidden="true" />
        </span>
        <span class="brand-copy">
          <strong>CampusHub</strong>
          <small>校园互助平台</small>
        </span>
      </RouterLink>

      <nav class="nav-links" aria-label="主导航">
        <RouterLink to="/tasks">
          <Home class="nav-icon" aria-hidden="true" />
          <span>任务大厅</span>
        </RouterLink>
        <RouterLink to="/announcements">
          <Megaphone class="nav-icon" aria-hidden="true" />
          <span>系统公告</span>
        </RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/tasks/new">
          <PlusCircle class="nav-icon" aria-hidden="true" />
          <span>发布需求</span>
        </RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/orders">
          <ClipboardList class="nav-icon" aria-hidden="true" />
          <span>我的订单</span>
        </RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/notifications">
          <Bell class="nav-icon" aria-hidden="true" />
          <span>通知</span>
          <span v-if="auth.unreadCount" class="nav-badge">{{ auth.unreadCount }}</span>
        </RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/profile">
          <UserRound class="nav-icon" aria-hidden="true" />
          <span>个人资料</span>
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin">
          <ShieldCheck class="nav-icon" aria-hidden="true" />
          <span>后台管理</span>
        </RouterLink>
      </nav>

      <div class="account-area">
        <button class="theme-toggle" type="button" :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'" @click="toggleTheme">
          <Moon v-if="isDark" class="theme-toggle-icon" aria-hidden="true" />
          <Sun v-else class="theme-toggle-icon" aria-hidden="true" />
        </button>
        <template v-if="auth.isAuthenticated">
          <span class="account-name">{{ auth.user?.nickname }}</span>
          <button class="button ghost" type="button" @click="handleLogout">
            <LogOut class="button-icon" aria-hidden="true" />
            <span>退出登录</span>
          </button>
        </template>
        <template v-else>
          <RouterLink class="button ghost" to="/login">
            <LogIn class="button-icon" aria-hidden="true" />
            <span>登录</span>
          </RouterLink>
          <RouterLink class="button primary" to="/register">
            <UserPlus class="button-icon" aria-hidden="true" />
            <span>注册</span>
          </RouterLink>
        </template>
      </div>
    </aside>

    <section class="workspace-shell">
      <header class="workspace-top">
        <div>
          <span class="eyebrow">
            <Sparkles class="eyebrow-icon" aria-hidden="true" />
            CampusHub
          </span>
          <strong>校园互助主流程演示版</strong>
        </div>
        <span class="workspace-status">
          {{ workspaceStatus }}
        </span>
      </header>

      <main class="page-container">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </section>
  </div>
</template>

<style scoped>
.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-on-sidebar);
  transition: all var(--transition-fast);
  margin-bottom: var(--space-2);
  align-self: center;
}

.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-on-sidebar-active);
}

.theme-toggle-icon {
  width: 16px;
  height: 16px;
}

.page-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.page-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 1, 1);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}
</style>
