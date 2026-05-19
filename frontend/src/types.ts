export type UserRole = 'STUDENT' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'ANONYMIZED'
export type TaskCategory =
  | 'EXPRESS'
  | 'ERRAND'
  | 'TUTORING'
  | 'SECOND_HAND'
  | 'LOST_FOUND'
  | 'CONSULTATION'
  | 'TEAM_UP'
  | 'OTHER'
export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED'
export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type OrderStatus =
  | 'PENDING_CONFIRM'
  | 'IN_PROGRESS'
  | 'PENDING_COMPLETION'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTE'
  | 'REVIEWED'
export type RewardType = 'CASH' | 'NEGOTIABLE' | 'CREDIT_INTENT'
export type NotificationType = 'APPLICATION' | 'ORDER_STATUS' | 'REVIEW_REQUEST' | 'REPORT_RESULT'
export type MessageType = 'TEXT' | 'IMAGE'

export interface PageData<T> {
  total: number
  page: number
  size: number
  pages: number
  records: T[]
}

export interface LoginUser {
  id: number
  email: string
  role: UserRole
  status: UserStatus
  verified: boolean
  nickname: string
  avatarUrl?: string
}

export interface LoginResult {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  user: LoginUser
}

export interface ProfileDetail {
  nickname: string
  avatarUrl?: string
  gender: 'MALE' | 'FEMALE' | 'OTHER'
  grade: string
  college: string
  bio: string
  campus: string
  contact: string
  contactVisible: boolean
}

export interface UserProfile {
  id: number
  email: string
  role: UserRole
  status: UserStatus
  verified: boolean
  profile: ProfileDetail
  credit: {
    score: number
    completedOrders: number
    praiseRate: number
  }
  createdAt: string
}

export interface TaskItem {
  id: number
  publisherId: number
  publisherNickname: string
  publisherAvatarUrl?: string
  category: TaskCategory
  title: string
  description: string
  campus: string
  rewardType: RewardType
  deadline: string
  status: TaskStatus
  anonymous: boolean
  imageUrls: string[]
  applicationCount: number
  favoriteCount: number
  isFavorited: boolean
  createdAt: string
  updatedAt?: string
  categoryFields?: Record<string, string | number | boolean>
}

export interface TaskForm {
  category: TaskCategory
  title: string
  description: string
  campus: string
  rewardType: RewardType
  deadline: string
  anonymous: boolean
  imageIds: number[]
  categoryFields: Record<string, string | number | boolean>
}

export interface ApplicationItem {
  id: number
  taskId: number
  applicantId: number
  applicantNickname: string
  applicantAvatarUrl?: string
  applicantCreditScore: number
  message: string
  status: ApplicationStatus
  createdAt: string
}

export interface OrderItem {
  id: number
  taskId: number
  taskTitle: string
  publisherId: number
  publisherNickname: string
  serviceProviderId: number
  serviceProviderNickname: string
  status: OrderStatus
  createdAt: string
}

export interface OrderDetail extends OrderItem {
  taskDescription: string
  campus: string
  rewardType: RewardType
  proofImageUrl?: string
  completionNote?: string
  statusLogs: Array<{
    id: number
    fromStatus?: OrderStatus
    toStatus: OrderStatus
    operatorNickname: string
    reason: string
    createdAt: string
  }>
  messages: OrderMessage[]
}

export interface OrderMessage {
  id: number
  orderId: number
  senderId: number
  senderNickname: string
  messageType: MessageType
  content?: string
  imageUrl?: string
  createdAt: string
}

export interface NotificationItem {
  id: number
  type: NotificationType
  title: string
  content: string
  targetType: string
  targetId: number
  read: boolean
  createdAt: string
}

export interface ReviewItem {
  id: number
  orderId: number
  reviewerId: number
  reviewerNickname: string
  revieweeId: number
  revieweeNickname: string
  rating: number
  content: string
  createdAt: string
}

export interface AdminUserItem {
  id: number
  email: string
  nickname: string
  role: UserRole
  status: UserStatus
  verified: boolean
  creditScore: number
  createdAt: string
}
