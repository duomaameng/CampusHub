import type {
  AdminUserItem,
  AnnouncementForm,
  AnnouncementItem,
  AnnouncementPriority,
  AnnouncementPublishResult,
  ApplicationItem,
  CreditInfo,
  LoginResult,
  LoginUser,
  NotificationItem,
  OrderDetail,
  OrderItem,
  OrderMessage,
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

interface MockUser {
  id: number
  email: string
  password: string
  role: LoginUser['role']
  status: UserStatus
  verified: boolean
  profile: UserProfile['profile']
  credit: UserProfile['credit']
  createdAt: string
}

interface MockVerificationCode {
  email: string
  purpose: 'REGISTER' | 'RESET_PASSWORD'
  code: string
  expiresAt: string
}

interface MockDatabase {
  currentUserId: number | null
  users: MockUser[]
  verificationCodes: MockVerificationCode[]
  tasks: TaskItem[]
  applications: ApplicationItem[]
  orders: OrderDetail[]
  notifications: NotificationItem[]
  reviews: ReviewItem[]
  uploadedFiles: UploadedFileItem[]
  reports: ReportSubmission[]
  announcements: AnnouncementItem[]
}

const dbKey = 'campus-hub-mock-db'
const mockVerificationCode = '123456'
const wait = () => new Promise((resolve) => window.setTimeout(resolve, 180))
let volatileUploadedFiles: UploadedFileItem[] = []

const seedNow = '2026-05-19T10:00:00.000Z'

const initialDb: MockDatabase = {
  currentUserId: null,
  verificationCodes: [],
  users: [
    {
      id: 10001,
      email: 'student.demo1@smail.nju.edu.cn',
      password: 'CampusHub123!',
      role: 'STUDENT',
      status: 'ACTIVE',
      verified: true,
      profile: {
        nickname: '蔡利扬',
        avatarUrl: '',
        gender: 'MALE',
        grade: '2024',
        college: '软件学院',
        bio: '负责 CampusHub 前端页面、状态管理和接口联调。',
        campus: '仙林校区',
        contact: 'WeChat:student_demo1',
        contactVisible: true
      },
      credit: {
        score: 100,
        completedOrders: 2,
        praiseRate: 0.96
      },
      createdAt: '2026-04-20T10:00:00.000Z'
    },
    {
      id: 10002,
      email: 'student.demo2@smail.nju.edu.cn',
      password: 'CampusHub123!',
      role: 'STUDENT',
      status: 'ACTIVE',
      verified: true,
      profile: {
        nickname: '小红',
        avatarUrl: '',
        gender: 'FEMALE',
        grade: '2023',
        college: '计算机科学与技术系',
        bio: '常在仙林校区帮同学代取快递。',
        campus: '仙林校区',
        contact: 'QQ:12345678',
        contactVisible: true
      },
      credit: {
        score: 95,
        completedOrders: 8,
        praiseRate: 0.98
      },
      createdAt: '2026-04-18T09:30:00.000Z'
    },
    {
      id: 20001,
      email: 'admin.demo@smail.nju.edu.cn',
      password: 'CampusHub123!',
      role: 'ADMIN',
      status: 'ACTIVE',
      verified: true,
      profile: {
        nickname: '管理员',
        avatarUrl: '',
        gender: 'OTHER',
        grade: '2024',
        college: '平台管理组',
        bio: '处理举报和用户状态。',
        campus: '仙林校区',
        contact: '',
        contactVisible: false
      },
      credit: {
        score: 100,
        completedOrders: 0,
        praiseRate: 1
      },
      createdAt: '2026-04-01T09:00:00.000Z'
    }
  ],
  tasks: [
    {
      id: 3001,
      publisherId: 10002,
      publisherNickname: '小红',
      category: 'EXPRESS',
      title: '帮忙取一下韵达快递',
      description: '韵达快递，送到仙林校区 12 栋楼下。',
      campus: '仙林校区',
      rewardType: 'CASH',
      deadline: '2026-06-08T18:00:00.000Z',
      status: 'OPEN',
      anonymous: false,
      imageUrls: [],
      applicationCount: 1,
      favoriteCount: 3,
      isFavorited: false,
      createdAt: '2026-05-17T10:00:00.000Z',
      updatedAt: '2026-05-17T10:30:00.000Z',
      categoryFields: {
        expressCompany: '韵达',
        pickupLocation: '仙林校区快递点',
        pickupCode: 'A-3-2105',
        deliveryLocation: '仙林校区 12 栋'
      }
    },
    {
      id: 3002,
      publisherId: 10001,
      publisherNickname: '蔡利扬',
      category: 'TUTORING',
      title: '帮忙讲解软工项目任务拆解',
      description: '希望找同学一起梳理 P4 编码阶段的任务和联调流程。',
      campus: '仙林校区',
      rewardType: 'NEGOTIABLE',
      deadline: '2026-06-02T20:00:00.000Z',
      status: 'OPEN',
      anonymous: false,
      imageUrls: [],
      applicationCount: 1,
      favoriteCount: 1,
      isFavorited: false,
      createdAt: '2026-05-18T14:20:00.000Z',
      updatedAt: '2026-05-18T14:20:00.000Z',
      categoryFields: {}
    },
    {
      id: 3003,
      publisherId: 10001,
      publisherNickname: '蔡利扬',
      category: 'SECOND_HAND',
      title: '出一台闲置显示器',
      description: '24 寸显示器，支持当面验货。',
      campus: '鼓楼校区',
      rewardType: 'CASH',
      deadline: '2026-06-10T20:00:00.000Z',
      status: 'IN_PROGRESS',
      anonymous: false,
      imageUrls: [],
      applicationCount: 1,
      favoriteCount: 5,
      isFavorited: true,
      createdAt: '2026-05-15T16:00:00.000Z',
      updatedAt: '2026-05-17T12:00:00.000Z',
      categoryFields: {
        goodsCategory: '数码',
        condition: 'LIKE_NEW',
        price: 399
      }
    }
  ],
  applications: [
    {
      id: 6001,
      taskId: 3002,
      applicantId: 10002,
      applicantNickname: '小红',
      applicantCreditScore: 95,
      message: '我可以帮你一起梳理联调流程。',
      status: 'PENDING',
      createdAt: '2026-05-18T15:00:00.000Z'
    }
  ],
  orders: [
    {
      id: 7001,
      taskId: 3003,
      taskTitle: '出一台闲置显示器',
      taskDescription: '24 寸显示器，支持当面验货。',
      campus: '鼓楼校区',
      rewardType: 'CASH',
      publisherId: 10001,
      publisherNickname: '蔡利扬',
      serviceProviderId: 10002,
      serviceProviderNickname: '小红',
      status: 'COMPLETED',
      createdAt: '2026-05-17T12:00:00.000Z',
      statusLogs: [
        {
          id: 1,
          toStatus: 'IN_PROGRESS',
          operatorNickname: '蔡利扬',
          reason: '确认接单并创建订单',
          createdAt: '2026-05-17T12:00:00.000Z'
        },
        {
          id: 2,
          fromStatus: 'IN_PROGRESS',
          toStatus: 'PENDING_COMPLETION',
          operatorNickname: '小红',
          reason: '服务方提交完成凭证',
          createdAt: '2026-05-18T16:00:00.000Z'
        },
        {
          id: 3,
          fromStatus: 'PENDING_COMPLETION',
          toStatus: 'COMPLETED',
          operatorNickname: '蔡利扬',
          reason: '发布者确认完成',
          createdAt: '2026-05-18T17:20:00.000Z'
        }
      ],
      messages: [
        {
          id: 12001,
          orderId: 7001,
          senderId: 10002,
          senderNickname: '小红',
          messageType: 'TEXT',
          content: '我已经到鼓楼校区了，可以当面确认吗？',
          createdAt: '2026-05-18T15:40:00.000Z'
        }
      ]
    },
    {
      id: 7002,
      taskId: 3001,
      taskTitle: '帮忙取一下韵达快递',
      taskDescription: '韵达快递，送到仙林校区 12 栋楼下。',
      campus: '仙林校区',
      rewardType: 'CASH',
      publisherId: 10002,
      publisherNickname: '小红',
      serviceProviderId: 10001,
      serviceProviderNickname: '蔡利扬',
      status: 'IN_PROGRESS',
      createdAt: '2026-05-19T09:00:00.000Z',
      statusLogs: [
        {
          id: 4,
          toStatus: 'IN_PROGRESS',
          operatorNickname: '小红',
          reason: '确认接单并创建订单',
          createdAt: '2026-05-19T09:00:00.000Z'
        }
      ],
      messages: []
    }
  ],
  notifications: [
    {
      id: 9001,
      type: 'APPLICATION',
      title: '新的接单申请',
      content: '小红申请了你的任务：帮忙讲解软工项目任务拆解。',
      targetType: 'TASK',
      targetId: 3002,
      read: false,
      createdAt: '2026-05-18T15:00:00.000Z'
    },
    {
      id: 9002,
      type: 'REVIEW_REQUEST',
      title: '订单已完成',
      content: '显示器交易订单已完成，可以提交评价。',
      targetType: 'ORDER',
      targetId: 7001,
      read: false,
      createdAt: '2026-05-18T17:20:00.000Z'
    }
  ],
  reviews: [],
  uploadedFiles: [],
  reports: [],
  announcements: [
    {
      id: 16001,
      title: '关于平台维护的通知',
      content: 'CampusHub 将于 2026 年 5 月 28 日 02:00-04:00 进行例行维护，期间部分功能可能短暂不可用。',
      priority: 'IMPORTANT',
      isActive: true,
      publisherId: 20001,
      createdAt: '2026-05-19T20:00:00.000Z',
      updatedAt: '2026-05-19T20:00:00.000Z'
    },
    {
      id: 16002,
      title: 'P4 联调阶段功能说明',
      content: '公告模块已开放前台查看，管理员可以在后台发布、编辑、下线或删除公告。',
      priority: 'NORMAL',
      isActive: true,
      publisherId: 20001,
      createdAt: '2026-05-18T16:30:00.000Z',
      updatedAt: '2026-05-18T16:30:00.000Z'
    },
    {
      id: 16003,
      title: '旧版公告示例',
      content: '这是一条已下线公告，仅管理员后台可见。',
      priority: 'NORMAL',
      isActive: false,
      publisherId: 20001,
      createdAt: '2026-05-16T09:20:00.000Z',
      updatedAt: '2026-05-17T10:00:00.000Z'
    }
  ]
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function loadDb(): MockDatabase {
  const raw = localStorage.getItem(dbKey)
  if (!raw) {
    localStorage.setItem(dbKey, JSON.stringify(initialDb))
    return clone(initialDb)
  }

  const db = JSON.parse(raw) as MockDatabase
  db.verificationCodes ||= []
  db.uploadedFiles ||= []
  db.reports ||= []
  db.announcements ||= clone(initialDb.announcements)
  return db
}

function saveDb(db: MockDatabase) {
  localStorage.setItem(dbKey, JSON.stringify(db))
}

function getUploadedFileById(db: MockDatabase, imageId: number, businessType?: UploadBusinessType) {
  const inMemory = volatileUploadedFiles.find((item) => item.id === imageId)
  const persisted = db.uploadedFiles.find((item) => item.id === imageId)
  const file = inMemory || persisted
  if (!file) return undefined
  if (businessType && file.businessType !== businessType) return undefined
  return file
}

function getCurrentUser(db = loadDb()) {
  const user = db.users.find((item) => item.id === db.currentUserId)
  if (!user) {
    throw new Error('请先登录')
  }
  if (user.status !== 'ACTIVE') {
    throw new Error('账号已被禁用')
  }
  return user
}

function toLoginUser(user: MockUser): LoginUser {
  return {
    id: user.id,
    email: user.email,
    role: user.role,
    status: user.status,
    verified: user.verified,
    nickname: user.profile.nickname,
    avatarUrl: user.profile.avatarUrl
  }
}

function toProfile(user: MockUser): UserProfile {
  return {
    id: user.id,
    email: user.email,
    role: user.role,
    status: user.status,
    verified: user.verified,
    profile: clone(user.profile),
    credit: clone(user.credit),
    createdAt: user.createdAt
  }
}

function paginate<T>(records: T[], page = 1, size = 20): PageData<T> {
  const start = (page - 1) * size
  return {
    total: records.length,
    page,
    size,
    pages: Math.max(1, Math.ceil(records.length / size)),
    records: records.slice(start, start + size)
  }
}

function pushNotification(db: MockDatabase, item: Omit<NotificationItem, 'id' | 'read' | 'createdAt'>) {
  const nextId = Math.max(9000, ...db.notifications.map((notification) => notification.id)) + 1
  db.notifications.unshift({
    ...item,
    id: nextId,
    read: false,
    createdAt: new Date().toISOString()
  })
}

function saveVerificationCode(db: MockDatabase, email: string, purpose: MockVerificationCode['purpose']) {
  db.verificationCodes = db.verificationCodes.filter((item) => !(item.email === email && item.purpose === purpose))
  db.verificationCodes.push({
    email,
    purpose,
    code: mockVerificationCode,
    expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString()
  })
}

function ensureUploadAllowed(file: File, businessType: UploadBusinessType) {
  const maxSize = businessType === 'AVATAR' ? 2 * 1024 * 1024 : 5 * 1024 * 1024
  const allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp']

  if (!allowedImageTypes.includes(file.type)) {
    throw new Error('仅支持 JPG、PNG、WebP 图片上传')
  }
  if (file.size > maxSize) {
    throw new Error(`文件大小不能超过 ${Math.round(maxSize / 1024 / 1024)}MB`)
  }
}

function normalizeAnnouncementPriority(priority?: string): AnnouncementPriority {
  return priority === 'IMPORTANT' ? 'IMPORTANT' : 'NORMAL'
}

function validateAnnouncementPayload(payload: Pick<AnnouncementForm, 'title' | 'content'>) {
  if (!payload.title.trim()) throw new Error('请填写公告标题')
  if (payload.title.trim().length > 100) throw new Error('公告标题不能超过 100 字')
  if (!payload.content.trim()) throw new Error('请填写公告内容')
  if (payload.content.trim().length > 5000) throw new Error('公告内容不能超过 5000 字')
}

export const mockApi = {
  async login(email: string, password: string): Promise<LoginResult> {
    await wait()
    const db = loadDb()
    const user = db.users.find((item) => item.email === email && item.password === password)
    if (!user) throw new Error('邮箱或密码错误')
    if (user.status !== 'ACTIVE') throw new Error('账号已被禁用')

    db.currentUserId = user.id
    saveDb(db)

    return {
      token: `mock-token-${user.id}`,
      tokenType: 'Bearer',
      expiresIn: 86400,
      user: toLoginUser(user)
    }
  },

  async register(email: string, password: string, confirmPassword: string, code: string) {
    await wait()
    if (!email.endsWith('@smail.nju.edu.cn') && !email.endsWith('@nju.edu.cn')) throw new Error('请使用学校邮箱注册')
    if (password !== confirmPassword) throw new Error('两次输入的密码不一致')

    const db = loadDb()
    if (db.users.some((item) => item.email === email)) {
      throw new Error('邮箱已注册')
    }

    const record = db.verificationCodes.find((item) => item.email === email && item.purpose === 'REGISTER')
    if (!record || record.code !== code.trim() || new Date(record.expiresAt).getTime() < Date.now()) {
      throw new Error('验证码错误或已过期')
    }

    const id = Math.max(...db.users.map((item) => item.id)) + 1
    db.users.push({
      id,
      email,
      password,
      role: 'STUDENT',
      status: 'ACTIVE',
      verified: false,
      profile: {
        nickname: email.split('@')[0],
        avatarUrl: '',
        gender: 'OTHER',
        grade: '2024',
        college: '',
        bio: '',
        campus: '仙林校区',
        contact: '',
        contactVisible: false
      },
      credit: {
        score: 100,
        completedOrders: 0,
        praiseRate: 1
      },
      createdAt: new Date().toISOString()
    })

    saveDb(db)
    return { userId: id, email }
  },

  async sendVerificationCode(email: string, purpose: 'REGISTER' | 'RESET_PASSWORD') {
    await wait()
    const db = loadDb()
    const user = db.users.find((item) => item.email === email)
    if (!email.endsWith('@smail.nju.edu.cn') && !email.endsWith('@nju.edu.cn')) throw new Error('请使用学校邮箱')
    if (purpose === 'REGISTER' && user) throw new Error('邮箱已注册')
    if (purpose === 'RESET_PASSWORD' && !user) throw new Error('邮箱未注册')

    saveVerificationCode(db, email, purpose)
    saveDb(db)
    return null
  },

  async verifyEmail(email: string, code: string) {
    await wait()
    const db = loadDb()
    const user = db.users.find((item) => item.email === email)
    if (!user) throw new Error('邮箱未注册')

    const record = db.verificationCodes.find((item) => item.email === email && item.purpose === 'REGISTER')
    if (!record || record.code !== code.trim() || new Date(record.expiresAt).getTime() < Date.now()) {
      throw new Error('验证码错误或已过期')
    }

    user.verified = true
    db.verificationCodes = db.verificationCodes.filter((item) => !(item.email === email && item.purpose === 'REGISTER'))
    saveDb(db)
    return { verified: true }
  },

  async resetPassword(email: string, code: string, newPassword: string, confirmNewPassword: string) {
    await wait()
    if (newPassword !== confirmNewPassword) throw new Error('两次输入的新密码不一致')

    const db = loadDb()
    const user = db.users.find((item) => item.email === email)
    if (!user) throw new Error('邮箱未注册')

    const record = db.verificationCodes.find((item) => item.email === email && item.purpose === 'RESET_PASSWORD')
    if (!record || record.code !== code.trim()) throw new Error('验证码错误或已过期')

    user.password = newPassword
    db.verificationCodes = db.verificationCodes.filter((item) => !(item.email === email && item.purpose === 'RESET_PASSWORD'))
    saveDb(db)
    return null
  },

  async logout() {
    await wait()
    const db = loadDb()
    db.currentUserId = null
    saveDb(db)
  },

  async me(): Promise<UserProfile> {
    await wait()
    return toProfile(getCurrentUser())
  },

  async updateMe(payload: Partial<UserProfile['profile']>): Promise<UserProfile> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    user.profile = { ...user.profile, ...payload }
    saveDb(db)
    return toProfile(user)
  },

  async listTasks(params: {
    page?: number
    size?: number
    category?: string
    campus?: string
    keyword?: string
    sort?: string
  }): Promise<PageData<TaskItem>> {
    await wait()
    const db = loadDb()
    let records = [...db.tasks]
    if (params.category) records = records.filter((item) => item.category === params.category)
    if (params.campus) records = records.filter((item) => item.campus === params.campus)
    if (params.keyword) {
      const keyword = params.keyword.trim().toLowerCase()
      records = records.filter(
        (item) => item.title.toLowerCase().includes(keyword) || item.description.toLowerCase().includes(keyword)
      )
    }
    records.sort((a, b) =>
      params.sort === 'deadline'
        ? new Date(a.deadline).getTime() - new Date(b.deadline).getTime()
        : new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    return paginate(records, params.page, params.size)
  },

  async getTask(taskId: number): Promise<TaskItem> {
    await wait()
    const task = loadDb().tasks.find((item) => item.id === taskId)
    if (!task) throw new Error('需求不存在')
    return clone(task)
  },

  async createTask(payload: TaskForm): Promise<{ id: number; status: string; createdAt: string }> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    if (!user.verified) throw new Error('未完成校园认证，不能发布需求')

    const imageUrls = payload.imageIds
      .map((imageId) => getUploadedFileById(db, imageId, 'TASK_IMAGE')?.url)
      .filter((url): url is string => Boolean(url))

    const id = Math.max(3000, ...db.tasks.map((item) => item.id)) + 1
    const createdAt = new Date().toISOString()
    db.tasks.unshift({
      id,
      publisherId: user.id,
      publisherNickname: user.profile.nickname,
      category: payload.category,
      title: payload.title,
      description: payload.description,
      campus: payload.campus,
      rewardType: payload.rewardType,
      deadline: payload.deadline,
      status: 'OPEN',
      anonymous: payload.anonymous,
      imageUrls,
      applicationCount: 0,
      favoriteCount: 0,
      isFavorited: false,
      createdAt,
      updatedAt: createdAt,
      categoryFields: payload.categoryFields
    })
    saveDb(db)
    return { id, status: 'OPEN', createdAt }
  },

  async applyTask(taskId: number, message: string) {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const task = db.tasks.find((item) => item.id === taskId)
    if (!task) throw new Error('需求不存在')
    if (task.publisherId === user.id) throw new Error('不能申请自己的需求')
    if (task.status !== 'OPEN') throw new Error('需求已过期或已被接单')
    if (db.applications.some((item) => item.taskId === taskId && item.applicantId === user.id)) {
      throw new Error('你已经提交过接单申请')
    }

    const applicationId = Math.max(6000, ...db.applications.map((item) => item.id)) + 1
    db.applications.push({
      id: applicationId,
      taskId,
      applicantId: user.id,
      applicantNickname: user.profile.nickname,
      applicantCreditScore: user.credit.score,
      message,
      status: 'PENDING',
      createdAt: new Date().toISOString()
    })
    task.applicationCount += 1
    pushNotification(db, {
      type: 'APPLICATION',
      title: '新的接单申请',
      content: `${user.profile.nickname} 申请了你的任务：${task.title}`,
      targetType: 'TASK',
      targetId: task.id
    })
    saveDb(db)
    return { applicationId, taskId, status: 'PENDING', createdAt: new Date().toISOString() }
  },

  async listApplications(taskId: number): Promise<ApplicationItem[]> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const task = db.tasks.find((item) => item.id === taskId)
    if (!task) throw new Error('需求不存在')
    if (task.publisherId !== user.id && user.role !== 'ADMIN') {
      throw new Error('无权查看该需求的接单申请')
    }
    return clone(db.applications.filter((item) => item.taskId === taskId))
  },

  async confirmApplication(applicationId: number) {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const application = db.applications.find((item) => item.id === applicationId)
    if (!application) throw new Error('接单申请不存在')
    const task = db.tasks.find((item) => item.id === application.taskId)
    if (!task) throw new Error('需求不存在')
    if (task.publisherId !== user.id) throw new Error('无权确认该申请')

    application.status = 'APPROVED'
    db.applications
      .filter((item) => item.taskId === task.id && item.id !== applicationId && item.status === 'PENDING')
      .forEach((item) => {
        item.status = 'REJECTED'
      })

    task.status = 'IN_PROGRESS'
    const orderId = Math.max(7000, ...db.orders.map((item) => item.id)) + 1
    const createdAt = new Date().toISOString()
    db.orders.unshift({
      id: orderId,
      taskId: task.id,
      taskTitle: task.title,
      taskDescription: task.description,
      campus: task.campus,
      rewardType: task.rewardType,
      publisherId: task.publisherId,
      publisherNickname: task.publisherNickname,
      serviceProviderId: application.applicantId,
      serviceProviderNickname: application.applicantNickname,
      status: 'IN_PROGRESS',
      createdAt,
      statusLogs: [
        {
          id: Math.max(1, ...db.orders.flatMap((order) => order.statusLogs.map((log) => log.id))) + 1,
          toStatus: 'IN_PROGRESS',
          operatorNickname: user.profile.nickname,
          reason: '确认接单并创建订单',
          createdAt
        }
      ],
      messages: []
    })

    pushNotification(db, {
      type: 'ORDER_STATUS',
      title: '接单申请已通过',
      content: `你的接单申请已通过，订单 ${orderId} 已创建`,
      targetType: 'ORDER',
      targetId: orderId
    })
    saveDb(db)
    return { orderId, taskId: task.id, status: 'IN_PROGRESS', createdAt }
  },

  async listOrders(params: { page?: number; size?: number; role?: string; status?: OrderStatus; keyword?: string }): Promise<PageData<OrderItem>> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    let records = db.orders.filter((item) => item.publisherId === user.id || item.serviceProviderId === user.id)
    if (params.role === 'PUBLISHER') records = records.filter((item) => item.publisherId === user.id)
    if (params.role === 'PROVIDER') records = records.filter((item) => item.serviceProviderId === user.id)
    if (params.status) records = records.filter((item) => item.status === params.status)
    if (params.keyword) records = records.filter((item) => item.taskTitle.includes(params.keyword || ''))
    return paginate(records.map(({ messages: _messages, statusLogs: _logs, ...item }) => item), params.page, params.size)
  },

  async getOrder(orderId: number): Promise<OrderDetail> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const order = db.orders.find((item) => item.id === orderId)
    if (!order) throw new Error('订单不存在')
    if (order.publisherId !== user.id && order.serviceProviderId !== user.id && user.role !== 'ADMIN') {
      throw new Error('无权查看该订单')
    }
    return clone(order)
  },

  async updateOrderStatus(orderId: number, status: OrderStatus, reason: string) {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const order = db.orders.find((item) => item.id === orderId)
    if (!order) throw new Error('订单不存在')

    const fromStatus = order.status
    order.status = status
    order.statusLogs.push({
      id: Math.max(1, ...db.orders.flatMap((item) => item.statusLogs.map((log) => log.id))) + 1,
      fromStatus,
      toStatus: status,
      operatorNickname: user.profile.nickname,
      reason,
      createdAt: new Date().toISOString()
    })
    pushNotification(db, {
      type: status === 'COMPLETED' ? 'REVIEW_REQUEST' : 'ORDER_STATUS',
      title: status === 'COMPLETED' ? '订单已完成' : '订单状态已更新',
      content: `${order.taskTitle} 的状态变更为 ${status}`,
      targetType: 'ORDER',
      targetId: order.id
    })
    saveDb(db)
  },

  async sendMessage(orderId: number, content: string): Promise<OrderMessage> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const order = db.orders.find((item) => item.id === orderId)
    if (!order) throw new Error('订单不存在')

    const message: OrderMessage = {
      id: Math.max(12000, ...db.orders.flatMap((item) => item.messages.map((msg) => msg.id))) + 1,
      orderId,
      senderId: user.id,
      senderNickname: user.profile.nickname,
      messageType: 'TEXT',
      content,
      createdAt: new Date().toISOString()
    }
    order.messages.push(message)
    saveDb(db)
    return clone(message)
  },

  async sendImage(orderId: number, imageId: number): Promise<OrderMessage> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const order = db.orders.find((item) => item.id === orderId)
    if (!order) throw new Error('订单不存在')

    const uploaded = getUploadedFileById(db, imageId, 'CHAT_IMAGE')
    if (!uploaded) throw new Error('聊天图片不存在或已失效')

    const message: OrderMessage = {
      id: Math.max(12000, ...db.orders.flatMap((item) => item.messages.map((msg) => msg.id))) + 1,
      orderId,
      senderId: user.id,
      senderNickname: user.profile.nickname,
      messageType: 'IMAGE',
      imageUrl: uploaded.url,
      createdAt: new Date().toISOString()
    }
    order.messages.push(message)
    saveDb(db)
    return clone(message)
  },

  async submitReview(orderId: number, rating: number, content: string) {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const order = db.orders.find((item) => item.id === orderId)
    if (!order) throw new Error('订单不存在')
    if (order.status !== 'COMPLETED') throw new Error('订单未完成，不能评价')
    if (db.reviews.some((item) => item.orderId === orderId && item.reviewerId === user.id)) {
      throw new Error('不能重复评价')
    }

    const revieweeId = order.publisherId === user.id ? order.serviceProviderId : order.publisherId
    const revieweeNickname = order.publisherId === user.id ? order.serviceProviderNickname : order.publisherNickname
    const reviewId = Math.max(13000, ...db.reviews.map((item) => item.id)) + 1
    db.reviews.push({
      id: reviewId,
      orderId,
      reviewerId: user.id,
      reviewerNickname: user.profile.nickname,
      revieweeId,
      revieweeNickname,
      rating,
      content,
      createdAt: new Date().toISOString()
    })
    saveDb(db)
    return { reviewId, orderId, rating, createdAt: new Date().toISOString() }
  },

  async getOrderReviews(orderId: number): Promise<ReviewItem[]> {
    await wait()
    return clone(loadDb().reviews.filter((item) => item.orderId === orderId))
  },

  async listNotifications(params: { page?: number; size?: number; read?: boolean }) {
    await wait()
    let records = loadDb().notifications
    if (typeof params.read === 'boolean') records = records.filter((item) => item.read === params.read)
    return paginate(records, params.page, params.size)
  },

  async unreadCount() {
    await wait()
    return { count: loadDb().notifications.filter((item) => !item.read).length }
  },

  async markNotificationRead(notificationId: number) {
    await wait()
    const db = loadDb()
    const item = db.notifications.find((notification) => notification.id === notificationId)
    if (item) item.read = true
    saveDb(db)
  },

  async readAllNotifications() {
    await wait()
    const db = loadDb()
    db.notifications.forEach((item) => {
      item.read = true
    })
    saveDb(db)
  },

  async adminUsers(params: { page?: number; size?: number; keyword?: string; status?: UserStatus }) {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    if (user.role !== 'ADMIN') throw new Error('权限不足')

    let records: AdminUserItem[] = db.users.map((item) => ({
      id: item.id,
      email: item.email,
      nickname: item.profile.nickname,
      role: item.role,
      status: item.status,
      verified: item.verified,
      creditScore: item.credit.score,
      createdAt: item.createdAt
    }))

    if (params.keyword) {
      records = records.filter((item) => item.email.includes(params.keyword || '') || item.nickname.includes(params.keyword || ''))
    }
    if (params.status) records = records.filter((item) => item.status === params.status)
    return paginate(records, params.page, params.size)
  },

  async updateUserStatus(userId: number, status: UserStatus) {
    await wait()
    const db = loadDb()
    const admin = getCurrentUser(db)
    if (admin.role !== 'ADMIN') throw new Error('权限不足')

    const user = db.users.find((item) => item.id === userId)
    if (!user) throw new Error('用户不存在')
    user.status = status
    saveDb(db)
    return { userId, status }
  },

  async listAnnouncements(params: { page?: number; size?: number } = {}): Promise<PageData<AnnouncementItem>> {
    await wait()
    const records = loadDb()
      .announcements.filter((item) => item.isActive)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return paginate(clone(records), params.page, params.size)
  },

  async adminAnnouncements(params: { page?: number; size?: number } = {}): Promise<PageData<AnnouncementItem>> {
    await wait()
    const db = loadDb()
    const admin = getCurrentUser(db)
    if (admin.role !== 'ADMIN') throw new Error('权限不足')

    const records = [...db.announcements].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    return paginate(clone(records), params.page, params.size)
  },

  async createAnnouncement(payload: AnnouncementForm): Promise<AnnouncementPublishResult> {
    await wait()
    const db = loadDb()
    const admin = getCurrentUser(db)
    if (admin.role !== 'ADMIN') throw new Error('权限不足')
    validateAnnouncementPayload(payload)

    const now = new Date().toISOString()
    const item: AnnouncementItem = {
      id: Math.max(16000, ...db.announcements.map((announcement) => announcement.id)) + 1,
      title: payload.title.trim(),
      content: payload.content.trim(),
      priority: normalizeAnnouncementPriority(payload.priority),
      isActive: true,
      publisherId: admin.id,
      createdAt: now,
      updatedAt: now
    }

    db.announcements.unshift(item)
    saveDb(db)
    return { announcementId: item.id, status: 'PUBLISHED' }
  },

  async updateAnnouncement(announcementId: number, payload: Partial<AnnouncementForm>): Promise<AnnouncementPublishResult> {
    await wait()
    const db = loadDb()
    const admin = getCurrentUser(db)
    if (admin.role !== 'ADMIN') throw new Error('权限不足')

    const item = db.announcements.find((announcement) => announcement.id === announcementId)
    if (!item) throw new Error('公告不存在')

    const nextTitle = payload.title ?? item.title
    const nextContent = payload.content ?? item.content
    validateAnnouncementPayload({ title: nextTitle, content: nextContent })

    item.title = nextTitle.trim()
    item.content = nextContent.trim()
    if (payload.priority) item.priority = normalizeAnnouncementPriority(payload.priority)
    if (typeof payload.isActive === 'boolean') item.isActive = payload.isActive
    item.updatedAt = new Date().toISOString()
    saveDb(db)

    return { announcementId: item.id, status: item.isActive ? 'ACTIVE' : 'INACTIVE' }
  },

  async deleteAnnouncement(announcementId: number) {
    await wait()
    const db = loadDb()
    const admin = getCurrentUser(db)
    if (admin.role !== 'ADMIN') throw new Error('权限不足')

    const before = db.announcements.length
    db.announcements = db.announcements.filter((announcement) => announcement.id !== announcementId)
    if (db.announcements.length === before) throw new Error('公告不存在')
    saveDb(db)
  },

  async uploadFile(file: File, businessType: UploadBusinessType): Promise<UploadedFileItem> {
    await wait()
    const db = loadDb()
    getCurrentUser(db)
    ensureUploadAllowed(file, businessType)

    const url = URL.createObjectURL(file)
    const item: UploadedFileItem = {
      id: Math.max(15000, ...db.uploadedFiles.map((uploaded) => uploaded.id)) + 1,
      businessType,
      fileName: file.name,
      contentType: file.type,
      size: file.size,
      url,
      createdAt: new Date().toISOString()
    }
    volatileUploadedFiles = [item, ...volatileUploadedFiles]
    return clone(item)
  },

  async submitReport(taskId: number, reason: string, evidenceImageIds: number[]): Promise<ReportSubmission> {
    await wait()
    const db = loadDb()
    const user = getCurrentUser(db)
    const task = db.tasks.find((item) => item.id === taskId)
    if (!task) throw new Error('需求不存在')
    if (!reason.trim()) throw new Error('请填写举报原因')

    const validEvidence = evidenceImageIds.filter((imageId) =>
      Boolean(getUploadedFileById(db, imageId, 'REPORT_EVIDENCE'))
    )

    const report: ReportSubmission = {
      reportId: Math.max(16000, ...db.reports.map((item) => item.reportId)) + 1,
      taskId,
      reason: reason.trim(),
      evidenceImageIds: validEvidence,
      createdAt: new Date().toISOString()
    }

    db.reports.unshift(report)
    pushNotification(db, {
      type: 'REPORT_RESULT',
      title: '举报已提交',
      content: `${user.profile.nickname} 已提交针对任务《${task.title}》的举报`,
      targetType: 'TASK',
      targetId: taskId
    })
    saveDb(db)
    return clone(report)
  },

  async getPublicProfile(userId: number): Promise<PublicProfile> {
    await wait()
    const db = loadDb()
    const user = db.users.find((item) => item.id === userId)
    if (!user) throw new Error('用户不存在')
    return {
      userId: user.id,
      nickname: user.profile.nickname,
      avatarUrl: user.profile.avatarUrl,
      gender: user.profile.gender,
      college: user.profile.college,
      campus: user.profile.campus,
      verified: user.verified,
      contact: user.profile.contact,
      contactVisible: user.profile.contactVisible,
      creditScore: user.credit.score,
      completedOrders: user.credit.completedOrders,
      praiseRate: user.credit.praiseRate,
      memberSince: user.createdAt
    }
  },

  async getUserCredit(userId: number): Promise<CreditInfo> {
    await wait()
    const db = loadDb()
    const user = db.users.find((item) => item.id === userId)
    if (!user) throw new Error('用户不存在')
    const recentReviews = db.reviews
      .filter((item) => item.revieweeId === userId)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)
    return {
      userId: user.id,
      score: user.credit.score,
      completedOrders: user.credit.completedOrders,
      praiseRate: user.credit.praiseRate,
      recentReviews: clone(recentReviews)
    }
  },

  reset() {
    volatileUploadedFiles = []
    localStorage.setItem(dbKey, JSON.stringify(initialDb))
  }
}
