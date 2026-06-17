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
  <section class="user-public-profile-view">
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
.user-public-profile-view {
  --public-green: #7dbe8e;
  --public-dark: #191a23;
  --public-grey: #f3f3f3;
}

.page-title {
  margin-bottom: 26px;
}

.page-title h1 {
  width: max-content;
  padding: 5px 14px;
  border-radius: 18px;
  border: 2px solid #000000;
  background: var(--public-green);
  color: #000000;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: 0;
  -webkit-text-fill-color: #000000;
  box-shadow: 0 4px 0 #000000;
}

.page-title p {
  margin-top: 12px;
  color: #3f4350;
  font-size: 16px;
  font-weight: 700;
}

.detail-layout {
  align-items: start;
  gap: 26px;
}

.panel {
  border: 2px solid #000000;
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 7px 0 #000000;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  overflow: hidden;
}

.panel::before,
.panel::after,
.empty-state::before {
  display: none;
}

.detail-layout > .panel {
  position: relative;
  padding: 30px;
  background:
    radial-gradient(circle at 94% 8%, var(--public-green) 0 72px, transparent 73px),
    #ffffff;
}

aside.panel {
  padding: 28px;
  background:
    radial-gradient(circle at 96% 8%, var(--public-green) 0 62px, transparent 63px),
    var(--public-dark);
  color: #ffffff;
}

aside.panel h2,
aside.panel h3 {
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  background: var(--public-green);
  color: #000000;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
}

aside.panel h2 {
  font-size: 22px;
}

aside.panel h3 {
  font-size: 18px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.avatar-preview.large {
  width: 92px;
  height: 92px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: var(--public-green);
  box-shadow: 0 5px 0 #000000;
  color: #000000;
  font-size: 32px;
  font-weight: 900;
}

.avatar-preview::after {
  display: none;
}

.profile-meta h2 {
  margin: 0;
  width: max-content;
  max-width: 100%;
  padding: 5px 12px;
  border-radius: 18px;
  background: var(--public-green);
  color: #000000;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.18;
}

.profile-tags {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.tag {
  border: 2px solid #000000;
  border-radius: 999px;
  background: #ffffff;
  color: #000000;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  padding: 5px 12px;
  box-shadow: none;
}

.tag.success {
  background: var(--public-green);
  color: #000000;
  border-color: #000000;
}

.grid.two > div,
.field {
  padding: 20px;
  border: 2px solid #000000;
  border-radius: 22px;
  background: var(--public-grey);
  box-shadow: 0 4px 0 #000000;
}

.grid.two p:not(.hint),
.field p:not(.hint) {
  color: #000000;
  font-weight: 900;
}

.hint {
  color: #6f7485;
  font-size: 12.5px;
  font-weight: 700;
}

aside.panel > .grid.two > div {
  background: #ffffff;
}

aside.panel strong {
  display: block;
  color: #000000;
  font-size: 34px;
  font-weight: 900;
}

aside.panel > p {
  color: rgba(255, 255, 255, 0.84);
  font-weight: 800;
}

aside.panel > .hint {
  color: rgba(255, 255, 255, 0.74);
}

.review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.review-item {
  padding: 16px;
  border: 2px solid #000000;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 0 #000000;
}

.review-item:last-child {
  border-bottom: 2px solid #000000;
}

.review-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.reviewer {
  font-weight: 900;
  font-size: 13px;
  color: #000000;
}

.rating {
  padding: 3px 9px;
  border: 2px solid #000000;
  border-radius: 999px;
  background: var(--public-green);
  color: #000000;
  font-size: 12px;
  font-weight: 900;
}

.review-content {
  margin: 4px 0;
  color: #343743;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.error-message,
.empty-state {
  padding: 12px 16px;
  border: 2px solid #000000;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 3px 0 #000000;
  font-weight: 800;
}

.empty-state {
  padding: var(--space-10) var(--space-6);
  border-style: dashed;
}

@media (max-width: 768px) {
  .profile-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
