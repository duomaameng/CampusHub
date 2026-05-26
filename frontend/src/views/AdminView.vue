<script setup lang="ts">
import { Edit3, Megaphone, RefreshCcw, Search, Trash2, UserRoundCog, UsersRound } from '@lucide/vue'
import { onMounted, reactive, ref } from 'vue'

import { adminApi } from '@/services/api'
import { announcementPriorityText, userStatusText } from '@/types'
import type { AdminUserItem, AnnouncementForm, AnnouncementItem, PageData, UserStatus } from '@/types'

type AdminTab = 'users' | 'announcements'

const activeTab = ref<AdminTab>('users')

const userFilters = reactive({
  keyword: '',
  status: '' as '' | UserStatus
})
const userPage = ref<PageData<AdminUserItem>>()
const usersLoading = ref(false)
const usersError = ref('')
const usersSuccess = ref('')

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

async function loadUsers() {
  usersLoading.value = true
  usersError.value = ''
  try {
    userPage.value = await adminApi.users({
      page: 1,
      size: 20,
      keyword: userFilters.keyword || undefined,
      status: userFilters.status || undefined
    })
  } catch (err) {
    usersError.value = err instanceof Error ? err.message : '用户列表加载失败'
  } finally {
    usersLoading.value = false
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

async function loadAnnouncements() {
  announcementsLoading.value = true
  announcementsError.value = ''
  try {
    announcementPage.value = await adminApi.announcements({
      page: announcementPageNumber.value,
      size: 10
    })
  } catch (err) {
    announcementsError.value = err instanceof Error ? err.message : '公告列表加载失败'
  } finally {
    announcementsLoading.value = false
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

async function deleteAnnouncement(item: AnnouncementItem) {
  if (!window.confirm(`确定删除公告“${item.title}”？`)) return

  announcementsError.value = ''
  announcementsSuccess.value = ''
  try {
    await adminApi.deleteAnnouncement(item.id)
    announcementsSuccess.value = '公告已删除'
    if (editingAnnouncementId.value === item.id) resetAnnouncementForm()
    await loadAnnouncements()
  } catch (err) {
    announcementsError.value = err instanceof Error ? err.message : '公告删除失败'
  }
}

async function goAnnouncementPage(nextPage: number) {
  if (nextPage < 1 || (announcementPage.value && nextPage > announcementPage.value.pages)) return
  announcementPageNumber.value = nextPage
  await loadAnnouncements()
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadAnnouncements()])
})
</script>

<template>
  <section>
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
      <div v-if="usersLoading" class="empty-state">正在加载用户</div>

      <div v-else class="table-wrapper">
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
          <div v-if="announcementsLoading" class="empty-state">正在加载公告</div>

          <div v-else-if="!announcementPage?.records.length" class="empty-state">暂无公告</div>
          <div v-else class="table-wrapper">
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
  </section>
</template>

<style scoped>
.admin-tabs {
  display: inline-flex;
  gap: var(--space-2);
  padding: var(--space-1);
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-6);
}

.admin-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 9px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.admin-tab:hover,
.admin-tab.active {
  color: var(--primary-700);
  background: var(--primary-50);
}

.admin-announcement-layout {
  grid-template-columns: minmax(320px, 420px) 1fr;
}

.announcement-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.announcement-list-head h2 {
  font-size: 18px;
  font-weight: 700;
}

.table-wrapper {
  overflow-x: auto;
}

.table-wrapper table {
  min-width: 760px;
}

.announcement-summary {
  max-width: 360px;
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
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
}

@media (max-width: 1024px) {
  .admin-announcement-layout {
    grid-template-columns: 1fr;
  }
}
</style>
