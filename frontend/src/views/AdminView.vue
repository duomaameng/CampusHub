<script setup lang="ts">
import { Edit3, Megaphone, RefreshCcw, Search, ShieldAlert, Trash2, UserRoundCog, UsersRound } from '@lucide/vue'
import { onMounted, reactive, ref } from 'vue'

import { useRealtimeRefresh } from '@/composables/useRealtimeRefresh'
import { adminApi } from '@/services/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useConfirmDialog } from '@/composables/useConfirmDialog'
import { announcementPriorityText, reportStatusText, reportTargetTypeText, userStatusText } from '@/types'
import type {
  AdminReportItem,
  AdminUserItem,
  AnnouncementForm,
  AnnouncementItem,
  PageData,
  ReportStatus,
  ReportTargetType,
  UserStatus
} from '@/types'

type AdminTab = 'users' | 'reports' | 'announcements'

const activeTab = ref<AdminTab>('users')
const dangerDialog = useConfirmDialog()

const userFilters = reactive({
  keyword: '',
  status: '' as '' | UserStatus
})
const userPage = ref<PageData<AdminUserItem>>()
const usersLoading = ref(false)
const usersError = ref('')
const usersSuccess = ref('')

const reportFilters = reactive({
  keyword: '',
  status: 'PENDING' as '' | ReportStatus,
  targetType: '' as '' | ReportTargetType
})
const reportPageNumber = ref(1)
const reportPage = ref<PageData<AdminReportItem>>()
const reportsLoading = ref(false)
const reportsError = ref('')
const reportsSuccess = ref('')
const reportProcessingId = ref<number | null>(null)
const reportResults = ref<Record<number, string>>({})
const reportPenalties = ref<Record<number, number>>({})

const announcementPageNumber = ref(1)
const announcementPage = ref<PageData<AnnouncementItem>>()
const announcementsLoading = ref(false)
const announcementSubmitting = ref(false)
const announcementsError = ref('')
const announcementsSuccess = ref('')
const editingAnnouncementId = ref<number | null>(null)
const announcementForm = reactive<AnnouncementForm>({
  title: '',
  content: '',
  priority: 'NORMAL',
  isActive: true
})

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

async function loadUsers(silent: boolean | Event = false) {
  const isSilent = silent === true
  if (!isSilent) {
    usersLoading.value = true
    usersError.value = ''
  }
  try {
    userPage.value = await adminApi.users({
      page: 1,
      size: 20,
      keyword: userFilters.keyword || undefined,
      status: userFilters.status || undefined
    })
  } catch (err) {
    if (!isSilent) usersError.value = err instanceof Error ? err.message : '用户列表加载失败'
  } finally {
    if (!isSilent) usersLoading.value = false
  }
}

async function updateStatus(userId: number, status: UserStatus) {
  usersError.value = ''
  usersSuccess.value = ''
  try {
    await adminApi.updateUserStatus(userId, status)
    usersSuccess.value = '用户状态已更新'
    await loadUsers()
  } catch (err) {
    usersError.value = err instanceof Error ? err.message : '状态更新失败'
  }
}

async function loadReports(silent: boolean | Event = false) {
  const isSilent = silent === true
  if (!isSilent) {
    reportsLoading.value = true
    reportsError.value = ''
  }
  try {
    reportPage.value = await adminApi.reports({
      page: reportPageNumber.value,
      size: 10,
      keyword: reportFilters.keyword || undefined,
      status: reportFilters.status || undefined,
      targetType: reportFilters.targetType || undefined
    })
  } catch (err) {
    if (!isSilent) reportsError.value = err instanceof Error ? err.message : '举报列表加载失败'
  } finally {
    if (!isSilent) reportsLoading.value = false
  }
}

async function searchReports() {
  reportPageNumber.value = 1
  await loadReports()
}

async function processReport(item: AdminReportItem, status: Extract<ReportStatus, 'RESOLVED' | 'REJECTED'>) {
  const result = reportResults.value[item.id]?.trim()
  reportsError.value = ''
  reportsSuccess.value = ''
  if (!result) {
    reportsError.value = '请先填写处理结果'
    return
  }

  reportProcessingId.value = item.id
  try {
    const creditPenalty = item.reasonType === 'TIMEOUT' && status === 'RESOLVED'
      ? reportPenalties.value[item.id] || 10
      : undefined
    await adminApi.processReport(item.id, status, result, creditPenalty)
    reportsSuccess.value = '举报已处理'
    reportResults.value[item.id] = ''
    delete reportPenalties.value[item.id]
    await loadReports()
  } catch (err) {
    reportsError.value = err instanceof Error ? err.message : '举报处理失败'
  } finally {
    reportProcessingId.value = null
  }
}

async function goReportPage(nextPage: number) {
  if (nextPage < 1 || (reportPage.value && nextPage > reportPage.value.pages)) return
  reportPageNumber.value = nextPage
  await loadReports()
}

async function loadAnnouncements(silent: boolean | Event = false) {
  const isSilent = silent === true
  if (!isSilent) {
    announcementsLoading.value = true
    announcementsError.value = ''
  }
  try {
    announcementPage.value = await adminApi.announcements({
      page: announcementPageNumber.value,
      size: 10
    })
  } catch (err) {
    if (!isSilent) announcementsError.value = err instanceof Error ? err.message : '公告列表加载失败'
  } finally {
    if (!isSilent) announcementsLoading.value = false
  }
}

function resetAnnouncementForm() {
  editingAnnouncementId.value = null
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.priority = 'NORMAL'
  announcementForm.isActive = true
}

function editAnnouncement(item: AnnouncementItem) {
  activeTab.value = 'announcements'
  editingAnnouncementId.value = item.id
  announcementForm.title = item.title
  announcementForm.content = item.content
  announcementForm.priority = item.priority
  announcementForm.isActive = item.isActive
}

async function saveAnnouncement() {
  announcementsError.value = ''
  announcementsSuccess.value = ''
  announcementSubmitting.value = true
  try {
    if (editingAnnouncementId.value) {
      await adminApi.updateAnnouncement(editingAnnouncementId.value, {
        title: announcementForm.title,
        content: announcementForm.content,
        priority: announcementForm.priority,
        isActive: announcementForm.isActive
      })
      announcementsSuccess.value = '公告已更新'
    } else {
      await adminApi.createAnnouncement({
        title: announcementForm.title,
        content: announcementForm.content,
        priority: announcementForm.priority
      })
      announcementsSuccess.value = '公告已发布'
    }
    resetAnnouncementForm()
    announcementPageNumber.value = 1
    await loadAnnouncements()
  } catch (err) {
    announcementsError.value = err instanceof Error ? err.message : '公告保存失败'
  } finally {
    announcementSubmitting.value = false
  }
}

async function toggleAnnouncement(item: AnnouncementItem) {
  announcementsError.value = ''
  announcementsSuccess.value = ''
  try {
    await adminApi.updateAnnouncement(item.id, { isActive: !item.isActive })
    announcementsSuccess.value = item.isActive ? '公告已下线' : '公告已上线'
    await loadAnnouncements()
  } catch (err) {
    announcementsError.value = err instanceof Error ? err.message : '公告状态更新失败'
  }
}

function deleteAnnouncement(item: AnnouncementItem) {
  dangerDialog.request({
    title: '删除这条公告？',
    description: `“${item.title}”删除后将不再对用户展示，且无法恢复。`,
    confirmText: '确认删除'
  }, async () => {
    announcementsError.value = ''
    announcementsSuccess.value = ''
    try {
      await adminApi.deleteAnnouncement(item.id)
      announcementsSuccess.value = '公告已删除'
      if (editingAnnouncementId.value === item.id) resetAnnouncementForm()
      await loadAnnouncements()
    } catch (err) {
      announcementsError.value = err instanceof Error ? err.message : '公告删除失败'
      throw err
    }
  })
}

async function goAnnouncementPage(nextPage: number) {
  if (nextPage < 1 || (announcementPage.value && nextPage > announcementPage.value.pages)) return
  announcementPageNumber.value = nextPage
  await loadAnnouncements()
}

async function loadAdminData(silent = false) {
  await Promise.all([loadUsers(silent), loadReports(silent), loadAnnouncements(silent)])
}

useRealtimeRefresh(['ADMIN_CHANGED', 'ANNOUNCEMENTS_CHANGED'], () => loadAdminData(true))
onMounted(loadAdminData)
</script>

<template>
  <section class="admin-view">
    <div class="page-title">
      <div>
        <h1>后台管理</h1>
        <p>集中处理用户状态与系统公告。</p>
      </div>
    </div>

    <div class="admin-tabs" role="tablist" aria-label="后台管理模块">
      <button :class="['admin-tab', activeTab === 'users' ? 'active' : '']" type="button" @click="activeTab = 'users'">
        <UsersRound class="button-icon" aria-hidden="true" />
        <span>用户管理</span>
      </button>
      <button :class="['admin-tab', activeTab === 'reports' ? 'active' : '']" type="button" @click="activeTab = 'reports'">
        <ShieldAlert class="button-icon" aria-hidden="true" />
        <span>举报处理</span>
      </button>
      <button
        :class="['admin-tab', activeTab === 'announcements' ? 'active' : '']"
        type="button"
        @click="activeTab = 'announcements'"
      >
        <Megaphone class="button-icon" aria-hidden="true" />
        <span>公告管理</span>
      </button>
    </div>

    <section v-show="activeTab === 'users'">
      <form class="toolbar" @submit.prevent="loadUsers">
        <div class="field">
          <label for="keyword">
            <Search class="label-icon" aria-hidden="true" />
            关键词
          </label>
          <input id="keyword" v-model.trim="userFilters.keyword" type="search" placeholder="邮箱或昵称" />
        </div>
        <div class="field">
          <label for="status">状态</label>
          <select id="status" v-model="userFilters.status">
            <option value="">全部</option>
            <option value="ACTIVE">正常</option>
            <option value="DISABLED">禁用</option>
            <option value="ANONYMIZED">已注销</option>
          </select>
        </div>
        <div class="field">
          <label>&nbsp;</label>
          <button class="button secondary" type="submit">查询</button>
        </div>
      </form>

      <p v-if="usersError" class="error-message">{{ usersError }}</p>
      <p v-if="usersSuccess" class="success-message">{{ usersSuccess }}</p>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>邮箱</th>
              <th>昵称</th>
              <th>角色</th>
              <th>状态</th>
              <th>信用分</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in userPage?.records" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.nickname }}</td>
              <td>{{ user.role }}</td>
              <td>
                <span :class="['tag', user.status === 'ACTIVE' ? 'success' : 'danger']">{{ userStatusText[user.status] }}</span>
              </td>
              <td>{{ user.creditScore }}</td>
              <td>
                <div class="actions">
                  <button class="button ghost" type="button" :disabled="user.status === 'ACTIVE'" @click="updateStatus(user.id, 'ACTIVE')">
                    解禁
                  </button>
                  <button
                    class="button danger"
                    type="button"
                    :disabled="user.status === 'DISABLED'"
                    @click="updateStatus(user.id, 'DISABLED')"
                  >
                    禁用
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-show="activeTab === 'reports'">
      <form class="toolbar" @submit.prevent="searchReports">
        <div class="field">
          <label for="report-keyword">
            <Search class="label-icon" aria-hidden="true" />
            关键词
          </label>
          <input id="report-keyword" v-model.trim="reportFilters.keyword" type="search" placeholder="搜索举报原因" />
        </div>
        <div class="field">
          <label for="report-status">状态</label>
          <select id="report-status" v-model="reportFilters.status">
            <option value="">全部</option>
            <option value="PENDING">待处理</option>
            <option value="PROCESSING">处理中</option>
            <option value="RESOLVED">已处理</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </div>
        <div class="field">
          <label for="report-target">对象</label>
          <select id="report-target" v-model="reportFilters.targetType">
            <option value="">全部</option>
            <option value="TASK">任务</option>
            <option value="ORDER_MESSAGE">订单消息</option>
            <option value="REVIEW">评价</option>
            <option value="USER">用户</option>
          </select>
        </div>
        <div class="field">
          <label>&nbsp;</label>
          <button class="button secondary" type="submit">查询</button>
        </div>
      </form>

      <p v-if="reportsError" class="error-message">{{ reportsError }}</p>
      <p v-if="reportsSuccess" class="success-message">{{ reportsSuccess }}</p>
      <div v-if="!reportsLoading && !reportPage?.records.length" class="empty-state">暂无举报记录</div>

      <div v-if="reportPage?.records.length" class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>举报人</th>
              <th>对象</th>
              <th>原因</th>
              <th>状态</th>
              <th>时间</th>
              <th>处理</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="report in reportPage.records" :key="report.id">
              <td>{{ report.id }}</td>
              <td>{{ report.reporterId }}</td>
              <td>
                {{ reportTargetTypeText[report.targetType] }} #{{ report.targetId }}
                <span v-if="report.relatedOrderId" class="hint">订单 #{{ report.relatedOrderId }}</span>
              </td>
              <td class="report-reason">{{ report.reason }}</td>
              <td>
                <span :class="['tag', report.status === 'PENDING' || report.status === 'PROCESSING' ? 'warning' : report.status === 'RESOLVED' ? 'success' : 'danger']">
                  {{ reportStatusText[report.status] }}
                </span>
              </td>
              <td>{{ formatDate(report.createdAt) }}</td>
              <td>
                <div v-if="report.status === 'PENDING' || report.status === 'PROCESSING'" class="report-actions">
                  <textarea v-model.trim="reportResults[report.id]" maxlength="500" placeholder="填写处理结果" />
                  <div v-if="report.reasonType === 'TIMEOUT'" class="field">
                    <label :for="`report-penalty-${report.id}`">信用扣分</label>
                    <input
                      :id="`report-penalty-${report.id}`"
                      v-model.number="reportPenalties[report.id]"
                      type="number"
                      min="1"
                      max="30"
                      placeholder="默认 10"
                    />
                  </div>
                  <div class="actions">
                    <button
                      class="button secondary"
                      type="button"
                      :disabled="reportProcessingId === report.id"
                      @click="processReport(report, 'RESOLVED')"
                    >
                      标记已处理
                    </button>
                    <button
                      class="button danger"
                      type="button"
                      :disabled="reportProcessingId === report.id"
                      @click="processReport(report, 'REJECTED')"
                    >
                      驳回举报
                    </button>
                  </div>
                </div>
                <span v-else class="hint">已处理</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="reportPage && reportPage.pages > 1" class="pagination">
        <button class="button ghost" type="button" :disabled="reportPage.page <= 1" @click="goReportPage(reportPage.page - 1)">
          上一页
        </button>
        <span class="hint">第 {{ reportPage.page }} / {{ reportPage.pages }} 页</span>
        <button class="button ghost" type="button" :disabled="reportPage.page >= reportPage.pages" @click="goReportPage(reportPage.page + 1)">
          下一页
        </button>
      </div>
    </section>

    <section v-show="activeTab === 'announcements'" class="admin-announcements">
      <div class="detail-layout admin-announcement-layout">
        <form class="panel grid" @submit.prevent="saveAnnouncement">
          <div class="item-title">
            <h2>{{ editingAnnouncementId ? '编辑公告' : '发布公告' }}</h2>
            <span v-if="editingAnnouncementId" class="tag info">ID {{ editingAnnouncementId }}</span>
          </div>

          <div class="field">
            <label for="announcement-title">标题</label>
            <input id="announcement-title" v-model.trim="announcementForm.title" maxlength="100" required placeholder="公告标题" />
          </div>

          <div class="field">
            <label for="announcement-priority">优先级</label>
            <select id="announcement-priority" v-model="announcementForm.priority">
              <option value="NORMAL">普通</option>
              <option value="IMPORTANT">重要</option>
            </select>
          </div>

          <div class="field">
            <label for="announcement-content">内容</label>
            <textarea
              id="announcement-content"
              v-model.trim="announcementForm.content"
              maxlength="5000"
              required
              placeholder="公告内容"
            />
          </div>

          <label v-if="editingAnnouncementId" class="checkbox-label">
            <input v-model="announcementForm.isActive" type="checkbox" />
            <span>上线展示</span>
          </label>

          <div class="actions">
            <button class="button primary" type="submit" :disabled="announcementSubmitting">
              <UserRoundCog class="button-icon" aria-hidden="true" />
              <span>{{ announcementSubmitting ? '保存中...' : editingAnnouncementId ? '保存公告' : '发布公告' }}</span>
            </button>
            <button v-if="editingAnnouncementId" class="button ghost" type="button" @click="resetAnnouncementForm">取消编辑</button>
          </div>
        </form>

        <section class="grid">
          <div class="announcement-list-head">
            <div>
              <h2>公告列表</h2>
              <p class="hint">共 {{ announcementPage?.total ?? 0 }} 条</p>
            </div>
            <button class="button secondary" type="button" @click="loadAnnouncements">
              <RefreshCcw class="button-icon" aria-hidden="true" />
              <span>刷新</span>
            </button>
          </div>

          <p v-if="announcementsError" class="error-message">{{ announcementsError }}</p>
          <p v-if="announcementsSuccess" class="success-message">{{ announcementsSuccess }}</p>
          <div v-if="!announcementsLoading && !announcementPage?.records.length" class="empty-state">暂无公告</div>
          <div v-if="announcementPage?.records.length" class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>公告</th>
                  <th>优先级</th>
                  <th>状态</th>
                  <th>发布时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in announcementPage.records" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td class="announcement-summary">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.content }}</span>
                  </td>
                  <td>
                    <span :class="['tag', item.priority === 'IMPORTANT' ? 'warning' : 'info']">
                      {{ announcementPriorityText[item.priority] }}
                    </span>
                  </td>
                  <td>
                    <span :class="['tag', item.isActive ? 'success' : 'danger']">{{ item.isActive ? '上线' : '下线' }}</span>
                  </td>
                  <td>{{ formatDate(item.createdAt) }}</td>
                  <td>
                    <div class="actions">
                      <button class="button ghost" type="button" @click="editAnnouncement(item)">
                        <Edit3 class="button-icon" aria-hidden="true" />
                        <span>编辑</span>
                      </button>
                      <button class="button secondary" type="button" @click="toggleAnnouncement(item)">
                        {{ item.isActive ? '下线' : '上线' }}
                      </button>
                      <button class="button danger" type="button" @click="deleteAnnouncement(item)">
                        <Trash2 class="button-icon" aria-hidden="true" />
                        <span>删除</span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="announcementPage && announcementPage.pages > 1" class="pagination">
            <button class="button ghost" type="button" :disabled="announcementPage.page <= 1" @click="goAnnouncementPage(announcementPage.page - 1)">
              上一页
            </button>
            <span class="hint">第 {{ announcementPage.page }} / {{ announcementPage.pages }} 页</span>
            <button
              class="button ghost"
              type="button"
              :disabled="announcementPage.page >= announcementPage.pages"
              @click="goAnnouncementPage(announcementPage.page + 1)"
            >
              下一页
            </button>
          </div>
        </section>
      </div>
    </section>
    <ConfirmDialog v-bind="dangerDialog.state" @confirm="dangerDialog.confirm" @cancel="dangerDialog.cancel" />
  </section>
</template>

<style scoped>
.admin-view {
  --admin-green: #ffb454;
  --admin-dark: #191a23;
  --admin-grey: #f3f3f3;
  --admin-line: #000000;
}

.admin-view :deep(.page-title) {
  align-items: center;
  padding: 30px 34px;
  border: 2px solid var(--admin-line);
  border-radius: 28px;
  background:
    radial-gradient(circle at 94% 18%, rgba(255, 180, 84, 0.76) 0 58px, transparent 60px),
    #ffffff;
  box-shadow: none;
}

.admin-view :deep(.page-title h1) {
  width: max-content;
  margin-bottom: 10px;
  padding: 5px 10px;
  border-radius: 7px;
  border: 2px solid #000000;
  background: transparent;
  color: #000000;
  font-size: 34px;
  line-height: 1.12;
  letter-spacing: 0;
  box-shadow: none;
}

.admin-view :deep(.page-title p) {
  color: #2b2d35;
  font-size: 16px;
  font-weight: 700;
}

.admin-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 8px;
  background: #ffffff;
  border: 2px solid var(--admin-line);
  border-radius: 18px;
  margin-bottom: var(--space-6);
  box-shadow: none;
  animation: fadeSlideUp 0.4s var(--transition-slow) both;
}

.admin-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: 44px;
  padding: 10px 16px;
  border: 2px solid transparent;
  border-radius: 14px;
  color: #20222b;
  font-size: 13px;
  font-weight: 900;
  transition: background var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.admin-tab:hover {
  color: #000000;
  background: var(--admin-grey);
  border-color: var(--admin-line);
  transform: translateY(-1px);
}

.admin-tab.active {
  color: #000000;
  background: var(--admin-green);
  border-color: var(--admin-line);
  box-shadow: none;
}

.admin-view :deep(.toolbar),
.admin-view :deep(.panel),
.admin-view section.grid {
  border: 2px solid var(--admin-line);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: none;
}

.admin-view :deep(.toolbar) {
  padding: 22px;
}

.admin-view :deep(.field label) {
  color: #000000;
  font-weight: 900;
}

.admin-view :deep(.label-icon) {
  color: var(--admin-dark);
}

.admin-view :deep(.field input),
.admin-view :deep(.field select),
.admin-view :deep(.field textarea),
.report-actions textarea {
  border: 2px solid var(--admin-line);
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
}

.admin-view :deep(.field input:focus),
.admin-view :deep(.field select:focus),
.admin-view :deep(.field textarea:focus),
.report-actions textarea:focus {
  border-color: var(--admin-line);
  box-shadow: 0 0 0 4px rgba(255, 180, 84, 0.55);
}

.admin-view :deep(.button) {
  border: 2px solid var(--admin-line);
  border-radius: 14px;
  font-weight: 900;
  box-shadow: none;
}

.admin-view :deep(.button.primary) {
  background: var(--admin-dark);
  color: #ffffff;
}

.admin-view :deep(.button.secondary),
.admin-view :deep(.button.ghost) {
  background: #ffffff;
  color: #000000;
}

.admin-view :deep(.button.secondary:hover),
.admin-view :deep(.button.ghost:hover) {
  background: var(--admin-green);
  color: #000000;
  transform: translateY(-1px);
}

.admin-view :deep(.button.danger) {
  background: #ffffff;
  color: #b91c1c;
  border-color: #b91c1c;
}

.admin-view :deep(.button.danger:hover) {
  background: #fee2e2;
  transform: translateY(-1px);
}

.admin-announcement-layout {
  grid-template-columns: minmax(320px, 420px) 1fr;
}

.announcement-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: 6px 2px 0 18px;
}

.announcement-list-head h2 {
  width: max-content;
  padding: 4px 8px;
  border-radius: 24px;
  background: transparent;
  color: #000000;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.announcement-list-head .hint {
  display: block;
  margin-left: 16px;
  margin-top: 6px;
}

.table-wrapper {
  overflow-x: auto;
  border: 2px solid var(--admin-line);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: none;
}

.table-wrapper table {
  min-width: 760px;
}

.table-wrapper :deep(table) {
  border-collapse: separate;
  border-spacing: 0;
}

.table-wrapper :deep(thead) {
  background: var(--admin-dark);
}

.table-wrapper :deep(th) {
  color: #ffffff;
  font-weight: 900;
}

.table-wrapper :deep(td) {
  color: #20222b;
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

.table-wrapper :deep(tbody tr:hover td) {
  background: #fff1df;
}

.announcement-summary {
  max-width: 360px;
}

.report-reason {
  max-width: 260px;
  white-space: normal;
}

.report-actions {
  display: grid;
  gap: var(--space-2);
  min-width: 260px;
}

.report-actions textarea {
  min-height: 72px;
  padding: 10px 12px;
  resize: vertical;
}

.announcement-summary strong,
.announcement-summary span {
  display: block;
}

.announcement-summary strong {
  color: var(--text-primary);
  margin-bottom: 4px;
}

.announcement-summary span {
  overflow: hidden;
  color: var(--text-tertiary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: #20222b;
  font-size: 13px;
  font-weight: 900;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-5);
}

@media (max-width: 1024px) {
  .admin-announcement-layout {
    grid-template-columns: 1fr;
  }
}

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>





