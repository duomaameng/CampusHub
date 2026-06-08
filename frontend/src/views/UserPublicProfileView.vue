<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { userApi } from '@/services/api'
import type { CreditInfo, PublicProfile, UserReviewItem } from '@/types'
import { resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const profile = ref<PublicProfile>()
const credit = ref<CreditInfo>()
const reviews = ref<UserReviewItem[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    const userId = Number(route.params.id)
    if (Number.isNaN(userId)) {
      throw new Error('无效的用户ID')
    }
    const [profileData, creditData, reviewData] = await Promise.all([
      userApi.getPublicProfile(userId),
      userApi.getUserCredit(userId),
      userApi.getUserReviews(userId)
    ])
    profile.value = profileData
    credit.value = creditData
    reviews.value = reviewData
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资料加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <h1>用户资料</h1>
        <p>查看用户的公开信息与信用评价。</p>
      </div>
    </div>

    <p v-if="error" class="error-message">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在加载资料</div>

    <div v-else-if="profile" class="detail-layout">
      <div class="panel grid">
        <div class="profile-header">
          <div class="avatar-preview large">
            <img v-if="profile.avatarUrl" :src="resolveAssetUrl(profile.avatarUrl)" alt="头像" />
            <span v-else>{{ profile.nickname?.slice(0, 1) || 'U' }}</span>
          </div>
          <div class="profile-meta">
            <h2>{{ profile.nickname }}</h2>
            <div class="profile-tags">
              <span v-if="profile.verified" class="tag success">已认证</span>
              <span v-else class="tag">未认证</span>
              <span v-if="profile.gender === 'MALE'" class="tag">男</span>
              <span v-else-if="profile.gender === 'FEMALE'" class="tag">女</span>
            </div>
          </div>
        </div>

        <div class="grid two">
          <div>
            <p class="hint">学院</p>
            <p>{{ profile.college || '未填写' }}</p>
          </div>
          <div>
            <p class="hint">常用校区</p>
            <p>{{ profile.campus || '未填写' }}</p>
          </div>
        </div>

        <div v-if="profile.contactVisible" class="field">
          <p class="hint">联系方式</p>
          <p>{{ profile.contact || '未填写' }}</p>
        </div>

        <p class="hint">注册时间：{{ new Date(profile.memberSince).toLocaleDateString('zh-CN') }}</p>
      </div>

      <aside class="panel grid">
        <h2>信用信息</h2>
        <div class="grid two">
          <div>
            <strong>{{ credit?.score ?? profile.creditScore }}</strong>
            <p class="hint">信用分</p>
          </div>
          <div>
            <strong>{{ credit?.completedOrders ?? profile.completedOrders }}</strong>
            <p class="hint">完成订单</p>
          </div>
        </div>
        <p>好评率 {{ Math.round((credit?.praiseRate ?? profile.praiseRate) * 100) }}%</p>

        <template v-if="reviews.length > 0">
          <h3>最近评价</h3>
          <ul class="review-list">
            <li v-for="review in reviews" :key="review.reviewId" class="review-item">
              <div class="review-header">
                <span class="reviewer">{{ review.reviewerNickname || `用户 ${review.reviewerId}` }}</span>
                <span class="rating">{{ review.rating }} 星</span>
              </div>
              <p class="review-content">{{ review.content }}</p>
              <p class="hint">{{ new Date(review.createdAt).toLocaleDateString('zh-CN') }}</p>
            </li>
          </ul>
        </template>
        <p v-else class="hint">暂无评价</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.avatar-preview.large {
  width: 80px;
  height: 80px;
  font-size: 28px;
}

.profile-meta h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.profile-tags {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.review-item {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border-light);
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.reviewer {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.rating {
  color: var(--primary-500);
  font-size: 12px;
  font-weight: 600;
}

.review-content {
  margin: 4px 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}
</style>
