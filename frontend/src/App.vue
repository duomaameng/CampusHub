<script setup lang="ts">
import {
  Bell,
  Bookmark,
  ClipboardList,
  GraduationCap,
  Home,
  LogIn,
  LogOut,
  Megaphone,
  MessageSquareText,
  PlusCircle,
  ShieldCheck,
  Undo2,
  UserPlus,
  UserRound
} from '@lucide/vue'
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useConfirmDialog } from '@/composables/useConfirmDialog'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const isLanding = computed(() => route.name === 'landing')
const logoutDialog = useConfirmDialog()

onMounted(async () => {
  document.documentElement.setAttribute('data-theme', 'light')
  localStorage.setItem('campus-hub-theme', 'light')
  if (auth.token) {
    try {
      await auth.loadMe()
      await auth.refreshUnread()
    } catch {
      await auth.logout()
    }
  }
})

function handleLogout() {
  logoutDialog.request({
    title: '退出当前账号？',
    description: '退出后需要重新登录才能查看订单、消息和个人资料。',
    confirmText: '确认退出'
  }, async () => {
    await auth.logout()
    await router.push('/login')
  })
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
        <RouterLink to="/">
          <Undo2 class="nav-icon" aria-hidden="true" />
          <span>返回首页</span>
        </RouterLink>
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
        <RouterLink v-if="auth.isAuthenticated" to="/tasks/favorites">
          <Bookmark class="nav-icon" aria-hidden="true" />
          <span>我的收藏</span>
        </RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/orders">
          <ClipboardList class="nav-icon" aria-hidden="true" />
          <span>我的订单</span>
        </RouterLink>
        <RouterLink
          v-if="auth.isAuthenticated"
          to="/chats"
          :class="{ 'router-link-active': route.name === 'order-chat' || route.name === 'chats' }"
        >
          <MessageSquareText class="nav-icon" aria-hidden="true" />
          <span>消息</span>
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

    <section class="workspace-shell">
      <main class="page-container">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </section>
  </div>
  <ConfirmDialog v-bind="logoutDialog.state" @confirm="logoutDialog.confirm" @cancel="logoutDialog.cancel" />
</template>

<style scoped>
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
