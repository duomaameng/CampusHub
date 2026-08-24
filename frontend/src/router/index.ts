import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: () => import('@/views/LandingView.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue') },
    { path: '/verify-email', name: 'verify-email', component: () => import('@/views/VerifyEmailView.vue') },
    { path: '/forgot-password', name: 'forgot-password', component: () => import('@/views/ForgotPasswordView.vue') },
    { path: '/tasks', name: 'tasks', component: () => import('@/views/TaskHallView.vue') },
    { path: '/tasks/favorites', name: 'task-favorites', component: () => import('@/views/TaskFavoritesView.vue'), meta: { requiresAuth: true } },
    { path: '/announcements', name: 'announcements', component: () => import('@/views/AnnouncementsView.vue') },
    {
      path: '/tasks/new',
      name: 'task-new',
      component: () => import('@/views/TaskPublishView.vue'),
      meta: { requiresAuth: true, requiresVerified: true }
    },
    { path: '/tasks/:id', name: 'task-detail', component: () => import('@/views/TaskDetailView.vue') },
    { path: '/orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { requiresAuth: true } },
    {
      path: '/orders/:id',
      name: 'order-detail',
      component: () => import('@/views/OrderDetailView.vue'),
      meta: { requiresAuth: true }
    },
    { path: '/profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/users/:id', name: 'user-public-profile', component: () => import('@/views/UserPublicProfileView.vue') },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/NotificationsView.vue'),
      meta: { requiresAuth: true }
    },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresVerified && !auth.user?.verified) {
    return { name: 'verify-email', query: { redirect: to.fullPath, email: auth.user?.email || '' } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'tasks' }
  }
  return true
})

export default router
