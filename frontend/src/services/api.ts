import { mockApi } from './mock'
import { request } from './http'

import type {
  AdminUserItem,
  AnnouncementForm,
  AnnouncementItem,
  AnnouncementPublishResult,
  ApplicationItem,
  CreditInfo,
  LoginResult,
  NotificationItem,
  OrderDetail,
  OrderItem,
  OrderStatus,
  PageData,
  PublicProfile,
  ReportSubmission,
  ReviewItem,
  TaskForm,
  TaskItem,
  UploadedFileItem,
  UploadBusinessType,
  UserProfile,
  UserStatus
} from '@/types'

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

export const authApi = {
  login(email: string, password: string): Promise<LoginResult> {
    if (useMock) return mockApi.login(email, password)
    return request<LoginResult>({ method: 'POST', url: '/auth/login', data: { email, password } })
  },
  register(email: string, password: string, confirmPassword: string, code: string) {
    if (useMock) return mockApi.register(email, password, confirmPassword, code)
    return request<{ userId: number; email: string }>({
      method: 'POST',
      url: '/auth/register',
      data: { email, password, confirmPassword, code }
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
  },
  getPublicProfile(userId: number): Promise<PublicProfile> {
    return request<PublicProfile>({ method: 'GET', url: `/users/${userId}/profile` })
  },
  getUserCredit(userId: number): Promise<CreditInfo> {
    return request<CreditInfo>({ method: 'GET', url: `/users/${userId}/credit` })
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
  sendImage(orderId: number, imageId: number) {
    if (useMock) return mockApi.sendImage(orderId, imageId)
    return request({
      method: 'POST',
      url: `/orders/${orderId}/messages`,
      data: { messageType: 'IMAGE', imageId }
    })
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

export const announcementApi = {
  list(params: { page?: number; size?: number } = {}): Promise<PageData<AnnouncementItem>> {
    if (useMock) return mockApi.listAnnouncements(params)
    return request<PageData<AnnouncementItem>>({ method: 'GET', url: '/announcements', params })
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
  },
  announcements(params: { page?: number; size?: number } = {}): Promise<PageData<AnnouncementItem>> {
    if (useMock) return mockApi.adminAnnouncements(params)
    return request<PageData<AnnouncementItem>>({ method: 'GET', url: '/admin/announcements', params })
  },
  createAnnouncement(payload: AnnouncementForm): Promise<AnnouncementPublishResult> {
    if (useMock) return mockApi.createAnnouncement(payload)
    return request<AnnouncementPublishResult>({
      method: 'POST',
      url: '/admin/announcements',
      data: {
        title: payload.title,
        content: payload.content,
        priority: payload.priority
      }
    })
  },
  updateAnnouncement(announcementId: number, payload: Partial<AnnouncementForm>): Promise<AnnouncementPublishResult> {
    if (useMock) return mockApi.updateAnnouncement(announcementId, payload)
    return request<AnnouncementPublishResult>({
      method: 'PATCH',
      url: `/admin/announcements/${announcementId}`,
      data: payload
    })
  },
  deleteAnnouncement(announcementId: number) {
    if (useMock) return mockApi.deleteAnnouncement(announcementId)
    return request<null>({ method: 'DELETE', url: `/admin/announcements/${announcementId}` })
  }
}

export const fileApi = {
  upload(file: File, businessType: UploadBusinessType): Promise<UploadedFileItem> {
    if (useMock) return mockApi.uploadFile(file, businessType)

    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', businessType)

    return request<UploadedFileItem>({
      method: 'POST',
      url: '/files/upload',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export const reportApi = {
  submit(taskId: number, reason: string, evidenceImageIds: number[]): Promise<ReportSubmission> {
    if (useMock) return mockApi.submitReport(taskId, reason, evidenceImageIds)
    return request<ReportSubmission>({
      method: 'POST',
      url: `/tasks/${taskId}/reports`,
      data: { reason, evidenceImageIds }
    })
  }
}
