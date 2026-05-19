<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { adminApi } from '@/services/api'
import type { AdminUserItem, PageData, UserStatus } from '@/types'

const filters = reactive({
  keyword: '',
  status: '' as '' | UserStatus
})
const page = ref<PageData<AdminUserItem>>()
const loading = ref(false)
const error = ref('')
const success = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    page.value = await adminApi.users({
      page: 1,
      size: 20,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function updateStatus(userId: number, status: UserStatus) {
  error.value = ''
  success.value = ''
  try {
    await adminApi.updateUserStatus(userId, status)
    success.value = '用户状态已更新'
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态更新失败'
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>后台用户管理</h1>
        <p>首版后台聚焦用户查看、搜索、禁用与解禁。</p>
      </div>
    </div>

    <form class="toolbar" @submit.prevent="load">
      <div class="field">
        <label for="keyword">关键词</label>
        <input id="keyword" v-model.trim="filters.keyword" type="search" placeholder="邮箱或昵称" />
      </div>
      <div class="field">
        <label for="status">状态</label>
        <select id="status" v-model="filters.status">
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

    <p v-if="error" class="error-message">{{ error }}</p>
    <p v-if="success" class="success-message">{{ success }}</p>
    <div v-if="loading" class="empty-state">正在加载用户</div>

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
          <tr v-for="user in page?.records" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ user.role }}</td>
            <td><span :class="['tag', user.status === 'ACTIVE' ? 'success' : 'danger']">{{ user.status }}</span></td>
            <td>{{ user.creditScore }}</td>
            <td>
              <div class="actions">
                <button class="button ghost" type="button" :disabled="user.status === 'ACTIVE'" @click="updateStatus(user.id, 'ACTIVE')">
                  解禁
                </button>
                <button class="button danger" type="button" :disabled="user.status === 'DISABLED'" @click="updateStatus(user.id, 'DISABLED')">
                  禁用
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
