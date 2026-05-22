<script setup lang="ts">
import {
  Bell,
  ClipboardList,
  Home,
  LogIn,
  LogOut,
  PanelLeft,
  PlusCircle,
  ShieldCheck,
  Sparkles,
  UserPlus,
  UserRound,
  Zap
} from '@lucide/vue'
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const isLanding = computed(() => route.name === 'landing')
const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const workspaceStatus = computed(() => (useMock ? 'Vue 3 + Mock API' : 'Vue 3 + Backend API'))

onMounted(async () => {
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
  <!-- Landing page: no shell at all -->
  <template v-if="isLanding">
    <RouterView />
  </template>

  <!-- Normal app shell for all other pages -->
  <div v-else class="app-shell">
    <!-- Sidebar -->
    <aside class="sidebar-shell">
      <RouterLink to="/tasks" class="brand">
        <span class="brand-mark">
          <Zap class="brand-icon" aria-hidden="true" />
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

    <!-- Main Workspace -->
    <section class="workspace-shell">
      <header class="workspace-top">
        <div>
          <span class="eyebrow">
            <Sparkles class="eyebrow-icon" aria-hidden="true" />
            Phase 4 前端工作区
          </span>
          <strong>校园互助主流程演示版</strong>
        </div>
        <span class="workspace-status">
          <PanelLeft class="meta-icon" aria-hidden="true" />
          {{ workspaceStatus }}
        </span>
      </header>

      <main class="page-container">
        <RouterView />
      </main>
    </section>
  </div>
</template>
