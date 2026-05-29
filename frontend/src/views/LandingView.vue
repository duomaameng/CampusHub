<script setup lang="ts">
import {
  ArrowRight,
  ArrowUpRight,
  Bell,
  GraduationCap,
  HandHeart,
  Mail,
  MessageCircleMore,
  PackageCheck,
  ShieldCheck,
  Sparkles,
  UserPlus
} from '@lucide/vue'
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const mounted = ref(false)
const useMock = import.meta.env.VITE_USE_MOCK === 'true'

const featureCards = [
  {
    icon: HandHeart,
    label: '互助闭环',
    title: '从发布、申请到成单协作，每一步都清楚可追踪。',
    description:
      '需求不会散落在聊天记录里，而是沉淀成可查看、可确认、可评价的任务链路，让校园互助更省心。'
  },
  {
    icon: ShieldCheck,
    label: '身份与信用',
    title: '校园身份验证配合信用沉淀，降低陌生协作的不确定感。',
    description:
      '每次完成、评价与举报处理，都会沉淀为平台内的信任资产，帮助大家更放心地发起和接受帮助。'
  },
  {
    icon: MessageCircleMore,
    label: '订单协作',
    title: '消息、凭证和进度都围绕订单展开，不再靠口头对齐。',
    description:
      '从接单到完成确认，沟通有上下文，过程有记录，减少误解和反复确认的时间成本。'
  }
]

const scenarioCards = [
  {
    category: '快递代取',
    title: '晚课前帮忙取一件京东快递',
    meta: '仙林校区 · 30 分钟内',
    reward: '现金 / 面议',
    accent: 'mint'
  },
  {
    category: '学习辅导',
    title: '软件工程作业结对讲解',
    meta: '鼓楼校区 · 今晚 20:00',
    reward: '积分意向',
    accent: 'sun'
  },
  {
    category: '组队搭子',
    title: '周末羽毛球双打临时补位',
    meta: '浦口校区 · 本周六',
    reward: 'AA / 面议',
    accent: 'sky'
  },
  {
    category: '二手交易',
    title: '转一台闲置显示器，支持宿舍自提',
    meta: '苏州校区 · 本周内',
    reward: '现金',
    accent: 'rose'
  }
]

const navigation = [
  { label: '平台特色', href: '#features' },
  { label: '任务场景', href: '#board' },
  { label: '使用方式', href: '#journey' },
  { label: '联系与说明', href: '#footer' }
]

onMounted(() => {
  requestAnimationFrame(() => {
    mounted.value = true
  })
})

function goExplore() {
  router.push('/tasks')
}
</script>

<template>
  <div class="campus-landing" :class="{ 'is-mounted': mounted }">
    <div class="landing-atmosphere" aria-hidden="true">
      <div class="landing-glow landing-glow-a" />
      <div class="landing-glow landing-glow-b" />
      <div class="landing-grid" />
      <div class="landing-grain" />
    </div>

    <header class="landing-nav">
      <RouterLink class="landing-brand" to="/">
        <span class="landing-brand-mark">
          <GraduationCap aria-hidden="true" />
        </span>
        <span class="landing-brand-copy">
          <strong>CampusHub</strong>
          <small>校园互助信息板</small>
        </span>
      </RouterLink>

      <nav class="landing-links" aria-label="首页导航">
        <a v-for="item in navigation" :key="item.href" :href="item.href">{{ item.label }}</a>
      </nav>

      <div class="landing-actions">
        <span class="landing-status">
          <Bell class="landing-status-icon" aria-hidden="true" />
          {{ useMock ? '演示数据模式' : '已连接真实服务' }}
        </span>
        <RouterLink class="landing-link-button" to="/login">登录</RouterLink>
        <RouterLink class="landing-primary-button" to="/register">
          <UserPlus class="landing-button-icon" aria-hidden="true" />
          立即注册
        </RouterLink>
      </div>
    </header>

    <main class="landing-content">
      <section class="hero-layout">
        <div class="hero-copy">
          <span class="hero-kicker">
            <Sparkles class="hero-kicker-icon" aria-hidden="true" />
            为大学生日常协作设计的轻量互助平台
          </span>

          <h1 class="hero-title">
            让校园里的每一次求助，
            <span>都能被看见、被回应、被放心托付。</span>
          </h1>

          <p class="hero-description">
            CampusHub 把任务发布、接单申请、订单协作、评价与举报整合成一条清晰主链。它不是冷冰冰的后台系统，而是一块真正适合校园使用的协作信息板，让日常互助变得更轻、更稳，也更可信。
          </p>

          <div class="hero-cta">
            <button class="landing-primary-button large" type="button" @click="goExplore">
              进入任务大厅
              <ArrowRight class="landing-button-icon" aria-hidden="true" />
            </button>
            <RouterLink class="landing-secondary-button" to="/login">
              先登录看看
            </RouterLink>
          </div>

          <div class="hero-metrics">
            <article>
              <strong>身份可信</strong>
              <span>校园邮箱验证与信用沉淀，适合学生之间的轻协作。</span>
            </article>
            <article>
              <strong>流程可追踪</strong>
              <span>从申请到完成确认，每一步都有清晰状态和记录。</span>
            </article>
            <article>
              <strong>沟通有上下文</strong>
              <span>消息围绕订单展开，不必再翻聊天记录找凭证。</span>
            </article>
          </div>
        </div>

        <aside class="hero-board">
          <div class="board-card lead">
            <span class="board-label">CampusHub Live Board</span>
            <strong>把零散的校园求助，整理成一张有温度的协作信息板。</strong>
            <p>
              需求方、服务方和管理员都能在同一视图里看到下一步，不再靠“记得回消息”维持协作。
            </p>
          </div>

          <div class="board-grid">
            <article class="board-card compact mint">
              <PackageCheck aria-hidden="true" />
              <strong>任务发布</strong>
              <span>结构化描述需求、截止时间与配图。</span>
            </article>
            <article class="board-card compact sun">
              <MessageCircleMore aria-hidden="true" />
              <strong>订单协作</strong>
              <span>消息、凭证与状态围绕订单集中展示。</span>
            </article>
            <article class="board-card compact sky">
              <ShieldCheck aria-hidden="true" />
              <strong>后台处理</strong>
              <span>举报、冻结与公告统一进入管理入口。</span>
            </article>
          </div>
        </aside>
      </section>

      <section id="features" class="landing-section">
        <div class="section-heading">
          <span class="section-eyebrow">平台特色</span>
          <h2>不是通用工单系统，而是更贴近学生日常节奏的协作体验。</h2>
        </div>

        <div class="feature-grid">
          <article v-for="item in featureCards" :key="item.label" class="feature-card">
            <span class="feature-icon">
              <component :is="item.icon" aria-hidden="true" />
            </span>
            <span class="feature-label">{{ item.label }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </article>
        </div>
      </section>

      <section id="board" class="landing-section board-section">
        <div class="section-heading split">
          <div>
            <span class="section-eyebrow">任务场景</span>
            <h2>把常见校园需求做成有质感的任务卡，而不是一串冰冷列表。</h2>
          </div>
          <RouterLink class="landing-inline-link" to="/tasks">
            浏览全部任务
            <ArrowUpRight class="landing-inline-icon" aria-hidden="true" />
          </RouterLink>
        </div>

        <div class="scenario-grid">
          <article
            v-for="item in scenarioCards"
            :key="item.title"
            class="scenario-card"
            :class="`accent-${item.accent}`"
          >
            <span class="scenario-category">{{ item.category }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.meta }}</p>
            <div class="scenario-footer">
              <span>{{ item.reward }}</span>
              <ArrowRight aria-hidden="true" />
            </div>
          </article>
        </div>
      </section>

      <section id="journey" class="landing-section journey-section">
        <div class="section-heading">
          <span class="section-eyebrow">使用方式</span>
          <h2>用一条简单顺畅的路径，把“帮个忙”变成真正完成闭环的协作。</h2>
        </div>

        <div class="journey-grid">
          <article class="journey-step">
            <span>01</span>
            <h3>发布需求</h3>
            <p>任务标题、校区、配图、截止时间与奖励方式一起整理好，减少反复问询。</p>
          </article>
          <article class="journey-step">
            <span>02</span>
            <h3>申请与确认</h3>
            <p>接单人提交申请，发布者统一查看并确认，平台自动生成订单进入协作阶段。</p>
          </article>
          <article class="journey-step">
            <span>03</span>
            <h3>沟通与完成</h3>
            <p>订单内沟通、上传凭证、确认完成、评价与举报全部闭环，减少纠纷成本。</p>
          </article>
        </div>
      </section>
    </main>

    <footer id="footer" class="landing-footer">
      <div>
        <strong>CampusHub</strong>
        <p>为校园日常协作打造的互助平台原型，适合展示完整的发布、接单、订单与管理主链。</p>
      </div>
      <div class="footer-links">
        <a href="#features">平台特色</a>
        <a href="#board">任务场景</a>
        <a href="#journey">使用方式</a>
      </div>
      <div class="footer-contact">
        <Mail class="footer-icon" aria-hidden="true" />
        <span>support@campushub.local</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.campus-landing {
  --landing-ink: #112031;
  --landing-ink-soft: #526071;
  --landing-paper: #f5efe3;
  --landing-paper-strong: #fbf7ef;
  --landing-line: rgba(17, 32, 49, 0.12);
  --landing-leaf: #1e7d5f;
  --landing-leaf-deep: #125440;
  --landing-sun: #e8a84f;
  --landing-sun-soft: #f6d7a0;
  --landing-rose: #d97f7f;
  --landing-sky: #7ca7cc;
  --landing-shadow: 0 20px 60px rgba(26, 32, 44, 0.12);
  --landing-display: 'STZhongsong', 'Songti SC', 'Noto Serif SC', 'SourceHanSansSC', serif;
  --landing-body: 'ManropeLocal', 'SourceHanSansSC', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  min-height: 100vh;
  position: relative;
  overflow-x: clip;
  background:
    linear-gradient(180deg, rgba(251, 247, 239, 0.98), rgba(245, 239, 227, 0.96)),
    var(--landing-paper);
  color: var(--landing-ink);
  font-family: var(--landing-body);
}

.landing-atmosphere {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.landing-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.7;
}

.landing-glow-a {
  width: 420px;
  height: 420px;
  top: -120px;
  right: -90px;
  background: radial-gradient(circle, rgba(30, 125, 95, 0.22), transparent 70%);
}

.landing-glow-b {
  width: 320px;
  height: 320px;
  left: -100px;
  bottom: 10%;
  background: radial-gradient(circle, rgba(232, 168, 79, 0.18), transparent 70%);
}

.landing-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(17, 32, 49, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 32, 49, 0.045) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.7), transparent 88%);
}

.landing-grain {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: radial-gradient(circle at 1px 1px, rgba(17, 32, 49, 0.22) 1px, transparent 0);
  background-size: 16px 16px;
}

.landing-nav,
.landing-content,
.landing-footer {
  position: relative;
  z-index: 1;
}

.landing-nav {
  width: min(1240px, calc(100vw - 32px));
  margin: 18px auto 0;
  padding: 16px 20px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 20px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(251, 247, 239, 0.72);
  backdrop-filter: blur(18px);
  border-radius: 22px;
  box-shadow: 0 8px 30px rgba(26, 32, 44, 0.06);
}

.landing-brand,
.landing-links a,
.landing-link-button,
.landing-primary-button,
.landing-secondary-button,
.landing-inline-link,
.footer-links a {
  text-decoration: none;
}

.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.landing-brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, rgba(30, 125, 95, 0.14), rgba(232, 168, 79, 0.14));
  color: var(--landing-leaf);
}

.landing-brand-copy {
  display: grid;
  gap: 3px;
}

.landing-brand-copy strong {
  font-size: 1.02rem;
  font-weight: 800;
  color: var(--landing-ink);
}

.landing-brand-copy small {
  color: var(--landing-ink-soft);
  font-size: 0.86rem;
}

.landing-links {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.landing-links a,
.landing-link-button,
.landing-inline-link,
.footer-links a {
  color: var(--landing-ink-soft);
  transition: color 180ms ease;
}

.landing-links a:hover,
.landing-link-button:hover,
.landing-inline-link:hover,
.footer-links a:hover {
  color: var(--landing-ink);
}

.landing-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.landing-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: var(--landing-ink-soft);
  font-size: 0.9rem;
}

.landing-status-icon,
.landing-button-icon,
.landing-inline-icon,
.footer-icon,
.hero-kicker-icon {
  width: 1em;
  height: 1em;
}

.landing-primary-button,
.landing-secondary-button,
.landing-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(17, 32, 49, 0.12);
  font-weight: 700;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease;
}

.landing-primary-button {
  color: #fff;
  border-color: transparent;
  background: var(--landing-leaf);
  box-shadow: 0 18px 30px rgba(30, 125, 95, 0.2);
}

.landing-primary-button.large {
  min-height: 56px;
  padding-inline: 28px;
  font-size: 1.08rem;
}

.landing-secondary-button,
.landing-link-button {
  color: var(--landing-ink);
  background: rgba(255, 255, 255, 0.58);
}

.landing-primary-button:hover,
.landing-secondary-button:hover,
.landing-link-button:hover {
  transform: translateY(-2px);
}

.landing-primary-button:hover {
  background: var(--landing-leaf-deep);
  box-shadow: 0 22px 34px rgba(30, 125, 95, 0.24);
}

.landing-content {
  width: min(1240px, calc(100vw - 32px));
  margin: 22px auto 0;
  display: grid;
  gap: 34px;
}

.hero-layout {
  display: grid;
  grid-template-columns: 1.35fr 0.9fr;
  gap: 26px;
  align-items: stretch;
}

.hero-copy,
.hero-board,
.landing-section,
.landing-footer {
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: rgba(251, 247, 239, 0.74);
  backdrop-filter: blur(18px);
  border-radius: 34px;
  box-shadow: var(--landing-shadow);
}

.hero-copy {
  padding: 34px 32px 30px;
}

.hero-kicker,
.section-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--landing-leaf-deep);
  font-size: 0.92rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.hero-title,
.section-heading h2,
.feature-card h3,
.scenario-card h3,
.journey-step h3,
.board-card.lead strong {
  font-family: var(--landing-display);
}

.hero-title {
  max-width: 12ch;
  margin: 20px 0 0;
  color: var(--landing-ink);
  font-size: clamp(2.15rem, 4.2vw, 4rem);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.05em;
}

.hero-title span {
  display: block;
  margin-top: 16px;
  color: var(--landing-leaf);
  font-size: clamp(1.05rem, 1.85vw, 1.65rem);
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.hero-description {
  max-width: 760px;
  margin: 28px 0 0;
  color: var(--landing-ink-soft);
  font-size: 1.05rem;
  line-height: 1.85;
}

.hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 28px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 34px;
}

.hero-metrics article {
  position: relative;
  overflow: hidden;
  min-height: 146px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid var(--landing-line);
  background:
    radial-gradient(circle at 16% 18%, rgba(214, 233, 225, 0.5), transparent 34%),
    radial-gradient(circle at 84% 84%, rgba(248, 229, 190, 0.28), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.34));
}

.hero-metrics strong {
  display: block;
  margin-bottom: 12px;
  color: var(--landing-ink);
  font-size: 1rem;
  font-weight: 800;
}

.hero-metrics span {
  color: var(--landing-ink-soft);
  line-height: 1.75;
}

.hero-board {
  padding: 34px;
  display: grid;
  gap: 22px;
}

.board-card {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--landing-line);
  border-radius: 28px;
  background:
    radial-gradient(circle at 15% 18%, rgba(219, 234, 227, 0.42), transparent 34%),
    radial-gradient(circle at 86% 86%, rgba(246, 228, 187, 0.22), transparent 28%),
    rgba(255, 255, 255, 0.66);
}

.board-card.lead {
  padding: 30px;
}

.board-card.lead strong {
  display: block;
  margin-top: 12px;
  color: var(--landing-ink);
  font-size: 1.65rem;
  line-height: 1.18;
}

.board-card.lead p {
  margin: 18px 0 0;
  color: var(--landing-ink-soft);
  line-height: 1.8;
}

.board-label {
  color: #56657a;
  font-size: 0.92rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.board-grid,
.feature-grid,
.scenario-grid,
.journey-grid {
  display: grid;
  gap: 18px;
}

.board-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.board-card.compact {
  min-height: 220px;
  padding: 24px 22px;
}

.board-card.compact svg {
  width: 24px;
  height: 24px;
  color: var(--landing-ink);
}

.board-card.compact strong {
  display: block;
  margin-top: 20px;
  font-size: 1.05rem;
}

.board-card.compact span {
  display: block;
  margin-top: 12px;
  color: var(--landing-ink-soft);
  line-height: 1.8;
}

.mint {
  background:
    radial-gradient(circle at 16% 16%, rgba(205, 231, 219, 0.74), transparent 36%),
    radial-gradient(circle at 82% 84%, rgba(245, 234, 210, 0.22), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.5));
}

.sun {
  background:
    radial-gradient(circle at 18% 16%, rgba(248, 223, 166, 0.78), transparent 36%),
    radial-gradient(circle at 82% 84%, rgba(220, 236, 228, 0.18), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.5));
}

.sky {
  background:
    radial-gradient(circle at 18% 16%, rgba(215, 228, 240, 0.78), transparent 36%),
    radial-gradient(circle at 82% 84%, rgba(245, 227, 188, 0.16), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.5));
}

.rose {
  background:
    radial-gradient(circle at 18% 16%, rgba(244, 224, 215, 0.74), transparent 36%),
    radial-gradient(circle at 82% 84%, rgba(247, 233, 196, 0.16), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.5));
}

.landing-section {
  padding: 34px 32px;
}

.section-heading {
  display: grid;
  gap: 14px;
}

.section-heading.split {
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 20px;
}

.section-heading h2 {
  max-width: 24ch;
  margin: 0;
  color: var(--landing-ink);
  font-size: clamp(1.6rem, 2.6vw, 2.35rem);
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: -0.04em;
}

.landing-inline-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.feature-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 24px;
}

.feature-card,
.scenario-card,
.journey-step {
  position: relative;
  overflow: hidden;
  min-height: 100%;
  padding: 28px;
  border-radius: 30px;
  border: 1px solid var(--landing-line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.42)),
    rgba(251, 247, 239, 0.78);
  box-shadow: 0 20px 50px rgba(17, 32, 49, 0.08);
}

.feature-card::before,
.scenario-card::before,
.journey-step::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
}

.feature-card:nth-child(3n + 1)::before,
.journey-step:nth-child(3n + 1)::before {
  background:
    radial-gradient(circle at 16% 14%, rgba(213, 232, 223, 0.64), transparent 34%),
    radial-gradient(circle at 84% 86%, rgba(249, 232, 196, 0.18), transparent 24%);
}

.feature-card:nth-child(3n + 2)::before,
.journey-step:nth-child(3n + 2)::before {
  background:
    radial-gradient(circle at 18% 14%, rgba(248, 226, 177, 0.66), transparent 34%),
    radial-gradient(circle at 84% 86%, rgba(220, 232, 240, 0.18), transparent 24%);
}

.feature-card:nth-child(3n + 3)::before,
.journey-step:nth-child(3n + 3)::before {
  background:
    radial-gradient(circle at 18% 14%, rgba(216, 228, 240, 0.68), transparent 34%),
    radial-gradient(circle at 84% 86%, rgba(244, 223, 213, 0.18), transparent 24%);
}

.feature-card > *,
.scenario-card > *,
.journey-step > * {
  position: relative;
  z-index: 1;
}

.feature-icon {
  width: 68px;
  height: 68px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--landing-ink);
}

.feature-label,
.scenario-category {
  display: inline-flex;
  margin-top: 22px;
  color: #4d5d71;
  font-size: 0.95rem;
  font-weight: 800;
}

.feature-card h3,
.scenario-card h3,
.journey-step h3 {
  margin: 18px 0 0;
  color: var(--landing-ink);
  font-weight: 800;
  line-height: 1.2;
}

.feature-card h3 {
  font-size: 1.45rem;
}

.feature-card p,
.scenario-card p,
.journey-step p {
  margin: 16px 0 0;
  color: var(--landing-ink-soft);
  line-height: 1.84;
}

.scenario-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 24px;
}

.scenario-card {
  display: flex;
  flex-direction: column;
  min-height: 360px;
}

.scenario-card.accent-mint::before {
  background:
    radial-gradient(circle at 14% 12%, rgba(208, 232, 220, 0.76), transparent 35%),
    radial-gradient(circle at 84% 84%, rgba(245, 234, 205, 0.18), transparent 24%);
}

.scenario-card.accent-sun::before {
  background:
    radial-gradient(circle at 16% 12%, rgba(248, 223, 164, 0.8), transparent 35%),
    radial-gradient(circle at 84% 84%, rgba(219, 234, 226, 0.16), transparent 24%);
}

.scenario-card.accent-sky::before {
  background:
    radial-gradient(circle at 16% 12%, rgba(214, 227, 240, 0.82), transparent 35%),
    radial-gradient(circle at 84% 84%, rgba(245, 227, 187, 0.14), transparent 24%);
}

.scenario-card.accent-rose::before {
  background:
    radial-gradient(circle at 16% 12%, rgba(244, 223, 214, 0.76), transparent 35%),
    radial-gradient(circle at 84% 84%, rgba(248, 232, 196, 0.14), transparent 24%);
}

.scenario-card h3 {
  font-size: 1.9rem;
}

.scenario-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 24px;
  color: var(--landing-ink);
  font-weight: 800;
}

.scenario-footer svg {
  width: 28px;
  height: 28px;
}

.journey-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 24px;
}

.journey-step span {
  width: 48px;
  height: 48px;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.74);
  color: var(--landing-ink);
  font-size: 1.02rem;
  font-weight: 800;
}

.journey-step h3 {
  font-size: 1.85rem;
}

.landing-footer {
  width: min(1240px, calc(100vw - 32px));
  margin: 0 auto 28px;
  padding: 28px 32px;
  display: grid;
  grid-template-columns: 1.2fr auto auto;
  align-items: center;
  gap: 24px;
}

.landing-footer strong {
  display: block;
  margin-bottom: 10px;
  font-size: 1.1rem;
}

.landing-footer p {
  margin: 0;
  color: var(--landing-ink-soft);
  line-height: 1.8;
}

.footer-links {
  display: flex;
  gap: 18px;
}

.footer-contact {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--landing-ink-soft);
}

.campus-landing.is-mounted .hero-copy,
.campus-landing.is-mounted .hero-board,
.campus-landing.is-mounted .landing-section,
.campus-landing.is-mounted .landing-footer {
  animation: landing-rise 500ms ease both;
}

.campus-landing.is-mounted .hero-board {
  animation-delay: 60ms;
}

.campus-landing.is-mounted .landing-section:nth-of-type(1) {
  animation-delay: 100ms;
}

.campus-landing.is-mounted .landing-section:nth-of-type(2) {
  animation-delay: 140ms;
}

.campus-landing.is-mounted .landing-section:nth-of-type(3) {
  animation-delay: 180ms;
}

.campus-landing.is-mounted .landing-footer {
  animation-delay: 220ms;
}

@keyframes landing-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (hover: hover) and (pointer: fine) {
  .feature-card,
  .scenario-card,
  .journey-step,
  .hero-metrics article,
  .board-card.compact,
  .board-card.lead {
    transition:
      transform 220ms ease,
      box-shadow 220ms ease,
      border-color 220ms ease,
      background 220ms ease;
  }

  .feature-card:hover,
  .scenario-card:hover,
  .journey-step:hover,
  .hero-metrics article:hover,
  .board-card.compact:hover,
  .board-card.lead:hover {
    transform: translateY(-5px);
    border-color: rgba(30, 125, 95, 0.2);
    box-shadow: 0 26px 56px rgba(17, 32, 49, 0.12);
  }
}

@media (max-width: 1180px) {
  .landing-nav,
  .hero-layout,
  .feature-grid,
  .scenario-grid,
  .journey-grid,
  .board-grid,
  .landing-footer {
    grid-template-columns: 1fr 1fr;
  }

  .hero-layout,
  .landing-footer {
    grid-template-columns: 1fr;
  }

  .feature-grid,
  .scenario-grid {
    grid-template-columns: 1fr 1fr;
  }

  .journey-grid,
  .board-grid {
    grid-template-columns: 1fr;
  }

  .landing-nav {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .landing-links,
  .landing-actions {
    flex-wrap: wrap;
  }

  .section-heading.split {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .landing-nav,
  .landing-content,
  .landing-footer {
    width: min(100vw - 20px, 100%);
  }

  .hero-copy,
  .hero-board,
  .landing-section,
  .landing-footer {
    padding: 24px 20px;
    border-radius: 26px;
  }

  .landing-links {
    display: none;
  }

  .feature-grid,
  .scenario-grid,
  .journey-grid {
    grid-template-columns: 1fr;
  }

  .hero-title {
    max-width: 100%;
    font-size: 2.15rem;
    line-height: 1.14;
  }

  .hero-title span {
    margin-top: 12px;
  }

  .section-heading h2,
  .feature-card h3,
  .scenario-card h3,
  .journey-step h3,
  .board-card.lead strong {
    max-width: 100%;
    font-size: 1.7rem;
  }

  .landing-primary-button.large,
  .landing-secondary-button,
  .landing-link-button {
    width: 100%;
  }

  .hero-cta {
    flex-direction: column;
  }
}
</style>
