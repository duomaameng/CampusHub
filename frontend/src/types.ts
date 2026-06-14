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
export type NotificationType = 'APPLICATION' | 'ORDER_STATUS' | 'ORDER_MESSAGE' | 'REVIEW_REQUEST' | 'REPORT_RESULT'
export type MessageType = 'TEXT' | 'IMAGE'
export type UploadBusinessType = 'AVATAR' | 'TASK_IMAGE' | 'CHAT_IMAGE' | 'REPORT_EVIDENCE' | 'ORDER_PROOF'
export type AnnouncementPriority = 'NORMAL' | 'IMPORTANT'
export type ReportTargetType = 'TASK' | 'ORDER_MESSAGE' | 'REVIEW' | 'USER'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'

export const taskStatusText: Record<TaskStatus, string> = {
  OPEN: '待接单',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
}

export const applicationStatusText: Record<ApplicationStatus, string> = {
  PENDING: '待处理',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已取消'
}

export const orderStatusText: Record<OrderStatus, string> = {
  PENDING_CONFIRM: '待接单',
  IN_PROGRESS: '进行中',
  PENDING_COMPLETION: '待确认完成',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  DISPUTE: '争议处理中',
  REVIEWED: '已评价'
}

export const userStatusText: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '已禁用',
  ANONYMIZED: '已注销'
}

export const announcementPriorityText: Record<AnnouncementPriority, string> = {
  NORMAL: '普通',
  IMPORTANT: '重要'
}

export const reportStatusText: Record<ReportStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已处理',
  REJECTED: '已驳回'
}

export const reportTargetTypeText: Record<ReportTargetType, string> = {
  TASK: '任务',
  ORDER_MESSAGE: '订单消息',
  REVIEW: '评价',
  USER: '用户'
}

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

export type TaskUpdatePayload = Partial<TaskForm>

export interface FavoriteToggleResult {
  favorited: boolean
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
  serviceProviderId?: number | null
  serviceProviderNickname?: string
  status: OrderStatus
  cancelReason?: string
  createdAt: string
}

export interface OrderDetail extends OrderItem {
  taskDescription: string
  campus: string
  rewardType: RewardType
  proofImageUrl?: string
  completionNote?: string
  statusLogs: OrderStatusLog[]
  messages: OrderMessage[]
}

export interface OrderStatusLog {
  id: number
  fromStatus?: OrderStatus
  toStatus: OrderStatus
  operatorId?: number
  operatorNickname: string
  reason: string
  createdAt: string
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

export interface UploadedFileItem {
  id: number
  businessType: UploadBusinessType
  fileName: string
  contentType: string
  size: number
  url: string
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

export interface UserReviewItem {
  reviewId: number
  orderId: number
  reviewerId: number
  reviewerNickname?: string
  rating: number
  content: string
  createdAt: string
}

export interface PublicProfile {
  userId: number
  nickname: string
  avatarUrl?: string
  gender: string
  college: string
  campus: string
  verified: boolean
  contact?: string
  contactVisible: boolean
  creditScore: number
  completedOrders: number
  praiseRate: number
  memberSince: string
}

export interface CreditChangeItem {
  changeAmount: number
  scoreBefore: number
  scoreAfter: number
  reason: string
  relatedOrderId?: number
  createdAt: string
}

export interface CreditInfo {
  userId: number
  score: number
  completedOrders: number
  praiseRate: number
  recentChanges: CreditChangeItem[]
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

export interface AdminReportItem {
  id: number
  reporterId: number
  targetType: ReportTargetType
  targetId: number
  reason: string
  status: ReportStatus
  createdAt: string
}

export interface AnnouncementItem {
  id: number
  title: string
  content: string
  priority: AnnouncementPriority
  isActive: boolean
  publisherId: number
  createdAt: string
  updatedAt: string
}

export interface AnnouncementForm {
  title: string
  content: string
  priority: AnnouncementPriority
  isActive?: boolean
}

export interface AnnouncementPublishResult {
  announcementId: number
  status: 'PUBLISHED' | 'ACTIVE' | 'INACTIVE' | string
}

export interface ReportSubmission {
  reportId: number
  taskId: number
  reason: string
  evidenceImageIds: number[]
  createdAt: string
}

export interface ReportItem {
  reportId: number
  targetType: ReportTargetType
  targetId: number
  reason: string
  status: ReportStatus
  result?: string | null
  createdAt: string
  processedAt?: string | null
}

export interface ReportDetail extends ReportItem {
  reporterId: number
  processedBy?: number | null
  evidenceImageIds: number[]
}
