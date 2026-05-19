import { mockApi } from './mock'
import { request } from './http'

import type {
  AdminUserItem,
  ApplicationItem,
  LoginResult,
  NotificationItem,
  OrderDetail,
  OrderItem,
  OrderStatus,
  PageData,
  ReviewItem,
  TaskForm,
  TaskItem,
  UserProfile,
  UserStatus
} from '@/types'

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

export const authApi = {
  login(email: string, password: string): Promise<LoginResult> {
    if (useMock) return mockApi.login(email, password)
    return request<LoginResult>({ method: 'POST', url: '/auth/login', data: { email, password } })
  },
  register(email: string, password: string, confirmPassword: string) {
    if (useMock) return mockApi.register(email, password, confirmPassword)
    return request<{ userId: number; email: string }>({
      method: 'POST',
      url: '/auth/register',
      data: { email, password, confirmPassword }
    })
  },
  sendVerificationCode(email: string, purpose: 'REGISTER' | 'RESET_PASSWORD') {
    if (useMock) return mockApi.sendVerificationCode(email, purpose)
    return request<null>({
      method: 'POST',
      url: '/auth/send-verification-code',
      data: { email, purpose }
    })
  },
  verifyEmail(email: string, code: string) {
    if (useMock) return mockApi.verifyEmail(email, code)
    return request<{ verified: boolean }>({
      method: 'POST',
      url: '/auth/verify-email',
      data: { email, code }
    })
  },
  resetPassword(email: string, code: string, newPassword: string, confirmNewPassword: string) {
    if (useMock) return mockApi.resetPassword(email, code, newPassword, confirmNewPassword)
    return request<null>({
      method: 'POST',
      url: '/auth/reset-password',
      data: { email, code, newPassword, confirmNewPassword }
    })
  },
  async logout() {
    if (useMock) return mockApi.logout()
    return request<null>({ method: 'POST', url: '/auth/logout' })
  }
}

export const userApi = {
  me(): Promise<UserProfile> {
    if (useMock) return mockApi.me()
    return request<UserProfile>({ method: 'GET', url: '/users/me' })
  },
  updateMe(payload: Partial<UserProfile['profile']>): Promise<UserProfile> {
    if (useMock) return mockApi.updateMe(payload)
    return request<UserProfile>({ method: 'PATCH', url: '/users/me', data: payload })
  }
}

export const taskApi = {
  list(params: {
    page?: number
    size?: number
    category?: string
    campus?: string
    keyword?: string
    sort?: string
  }): Promise<PageData<TaskItem>> {
    if (useMock) return mockApi.listTasks(params)
    return request<PageData<TaskItem>>({ method: 'GET', url: '/tasks', params })
  },
  get(taskId: number): Promise<TaskItem> {
    if (useMock) return mockApi.getTask(taskId)
    return request<TaskItem>({ method: 'GET', url: `/tasks/${taskId}` })
  },
  create(payload: TaskForm) {
    if (useMock) return mockApi.createTask(payload)
    return request<{ id: number; status: string; createdAt: string }>({ method: 'POST', url: '/tasks', data: payload })
  },
  apply(taskId: number, message: string) {
    if (useMock) return mockApi.applyTask(taskId, message)
    return request<{ applicationId: number; taskId: number; status: string; createdAt: string }>({
      method: 'POST',
      url: `/tasks/${taskId}/applications`,
      data: { message }
    })
  },
  applications(taskId: number): Promise<ApplicationItem[]> {
    if (useMock) return mockApi.listApplications(taskId)
    return request<ApplicationItem[]>({ method: 'GET', url: `/tasks/${taskId}/applications` })
  },
  confirmApplication(applicationId: number): Promise<{ orderId: number; taskId: number; status: string; createdAt: string }> {
    if (useMock) return mockApi.confirmApplication(applicationId)
    return request<{ orderId: number; taskId: number; status: string; createdAt: string }>({
      method: 'POST',
      url: `/applications/${applicationId}/confirm`
    })
  }
}

export const orderApi = {
  list(params: { page?: number; size?: number; role?: string; status?: OrderStatus; keyword?: string }): Promise<PageData<OrderItem>> {
    if (useMock) return mockApi.listOrders(params)
    return request<PageData<OrderItem>>({ method: 'GET', url: '/orders', params })
  },
  get(orderId: number): Promise<OrderDetail> {
    if (useMock) return mockApi.getOrder(orderId)
    return request<OrderDetail>({ method: 'GET', url: `/orders/${orderId}` })
  },
  complete(orderId: number) {
    if (useMock) return mockApi.updateOrderStatus(orderId, 'PENDING_COMPLETION', '服务方提交完成凭证')
    return request({ method: 'POST', url: `/orders/${orderId}/complete`, data: { proofImageId: 1, note: '任务已完成' } })
  },
  confirmCompletion(orderId: number) {
    if (useMock) return mockApi.updateOrderStatus(orderId, 'COMPLETED', '发布者确认完成')
    return request({ method: 'POST', url: `/orders/${orderId}/confirm-completion` })
  },
  cancel(orderId: number, reason: string) {
    if (useMock) return mockApi.updateOrderStatus(orderId, 'CANCELLED', reason)
    return request({ method: 'POST', url: `/orders/${orderId}/cancel`, data: { reason } })
  },
  sendMessage(orderId: number, content: string) {
    if (useMock) return mockApi.sendMessage(orderId, content)
    return request({ method: 'POST', url: `/orders/${orderId}/messages`, data: { messageType: 'TEXT', content } })
  },
  submitReview(orderId: number, rating: number, content: string) {
    if (useMock) return mockApi.submitReview(orderId, rating, content)
    return request({ method: 'POST', url: `/orders/${orderId}/reviews`, data: { rating, content } })
  },
  reviews(orderId: number): Promise<ReviewItem[]> {
    if (useMock) return mockApi.getOrderReviews(orderId)
    return request<ReviewItem[]>({ method: 'GET', url: `/orders/${orderId}/reviews` })
  }
}

export const notificationApi = {
  list(params: { page?: number; size?: number; read?: boolean }): Promise<PageData<NotificationItem>> {
    if (useMock) return mockApi.listNotifications(params)
    return request<PageData<NotificationItem>>({ method: 'GET', url: '/notifications', params })
  },
  unreadCount(): Promise<{ count: number }> {
    if (useMock) return mockApi.unreadCount()
    return request<{ count: number }>({ method: 'GET', url: '/notifications/unread-count' })
  },
  markRead(notificationId: number) {
    if (useMock) return mockApi.markNotificationRead(notificationId)
    return request({ method: 'PATCH', url: `/notifications/${notificationId}/read` })
  },
  readAll() {
    if (useMock) return mockApi.readAllNotifications()
    return request({ method: 'PATCH', url: '/notifications/read-all' })
  }
}

export const adminApi = {
  users(params: { page?: number; size?: number; keyword?: string; status?: UserStatus }): Promise<PageData<AdminUserItem>> {
    if (useMock) return mockApi.adminUsers(params)
    return request<PageData<AdminUserItem>>({ method: 'GET', url: '/admin/users', params })
  },
  updateUserStatus(userId: number, status: UserStatus) {
    if (useMock) return mockApi.updateUserStatus(userId, status)
    return request<{ userId: number; status: UserStatus }>({
      method: 'PATCH',
      url: `/admin/users/${userId}/status`,
      data: { status }
    })
  }
}
