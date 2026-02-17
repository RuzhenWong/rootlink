<template>
  <div class="dashboard">

    <!-- 顶部欢迎区 -->
    <div class="welcome-banner" ref="bannerRef">
      <div class="welcome-bg">
        <div class="w-orb w-orb1"></div>
        <div class="w-orb w-orb2"></div>
      </div>
      <div class="welcome-body">
        <div class="welcome-left">
          <p class="greet">{{ greetText }}，</p>
          <h1 class="welcome-name">{{ userName }}</h1>
          <p class="welcome-sub">今天是 {{ today }}，您的家族记录安全存档中 ✦</p>
        </div>
        <div class="welcome-right">
          <el-avatar :size="72"
                     :src="userProfile?.avatarUrl || ''"
                     class="welcome-avatar">
            {{ avatarInitial }}
          </el-avatar>
        </div>
      </div>
      <!-- 未实名提示 -->
      <div v-if="userInfo?.realNameStatus !== 2" class="banner-alert" @click="goRealName">
        <span>⚠️</span>
        <span>您尚未完成实名认证，部分功能受限 — <strong>立即认证 →</strong></span>
      </div>
    </div>

    <!-- 数据统计卡片 -->
    <div class="stats-row">
      <div v-for="(stat, i) in stats" :key="i" class="stat-card"
           :style="{ animationDelay: (i * 0.08) + 's' }">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <span v-html="stat.icon"></span>
        </div>
        <div class="stat-body">
          <div class="stat-val">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-trend" v-if="stat.sub">{{ stat.sub }}</div>
      </div>
    </div>

    <!-- 功能入口 -->
    <div class="section-title">快捷入口</div>
    <div class="feature-grid">
      <div v-for="feat in features" :key="feat.path"
           class="feat-card" @click="router.push(feat.path)"
           :style="{ '--feat-c': feat.color }">
        <div class="feat-icon-wrap">
          <span class="feat-icon" v-html="feat.icon"></span>
        </div>
        <div class="feat-body">
          <div class="feat-name">{{ feat.name }}</div>
          <div class="feat-desc">{{ feat.desc }}</div>
        </div>
        <svg class="feat-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
    </div>

    <!-- 账户状态 -->
    <div class="section-title" style="margin-top:28px">账户状态</div>
    <div class="status-grid">
      <div class="status-item" v-for="(s, i) in accountStatus" :key="i">
        <div class="status-label">{{ s.label }}</div>
        <div class="status-val">
          <el-tag :type="s.type || 'info'" size="small">{{ s.val }}</el-tag>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import { relationApi } from '@/api/relation'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const bannerRef = ref()
const userInfo = computed(() => authStore.userInfo)
const userProfile = ref(null)
const relationsCount = ref('—')

const userName = computed(() => userInfo.value?.realName || userInfo.value?.phone || '用户')
const avatarInitial = computed(() => (userInfo.value?.realName || userInfo.value?.phone || '?')[0])

const today = dayjs().format('YYYY年MM月DD日')
const hours = new Date().getHours()
const greetText = computed(() => hours < 6 ? '夜深了' : hours < 12 ? '早上好' : hours < 18 ? '下午好' : '晚上好')

const stats = computed(() => [
  {
    label: '亲属成员',
    value: relationsCount.value,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
    bg: 'linear-gradient(135deg,rgba(90,103,242,.12),rgba(90,103,242,.06))',
    sub: '已关联',
  },
  {
    label: '账户状态',
    value: userInfo.value?.status === 1 ? '正常' : '异常',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
    bg: 'linear-gradient(135deg,rgba(16,185,129,.12),rgba(16,185,129,.06))',
    sub: null,
  },
  {
    label: '实名认证',
    value: { 0:'未认证',1:'审核中',2:'已认证',3:'认证失败' }[userInfo.value?.realNameStatus] || '—',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c0 1.657 1.343 3 3 3s3-1.343 3-3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
    bg: 'linear-gradient(135deg,rgba(245,158,11,.12),rgba(245,158,11,.06))',
    sub: null,
  },
  {
    label: '最后登录',
    value: userInfo.value?.lastLoginTime ? dayjs(userInfo.value.lastLoginTime).format('MM/DD HH:mm') : '—',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
    bg: 'linear-gradient(135deg,rgba(139,92,246,.12),rgba(139,92,246,.06))',
    sub: null,
  },
])

const features = [
  {
    path: '/relations',
    name: '亲属关系',
    desc: '建立和管理家族关系网络',
    color: '#5A67F2',
    icon: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
  },
  {
    path: '/family-tree',
    name: '家族关系树',
    desc: '可视化家族图谱',
    color: '#10B981',
    icon: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="5" r="2" stroke="currentColor" stroke-width="1.8"/><circle cx="5" cy="18" r="2" stroke="currentColor" stroke-width="1.8"/><circle cx="12" cy="18" r="2" stroke="currentColor" stroke-width="1.8"/><circle cx="19" cy="18" r="2" stroke="currentColor" stroke-width="1.8"/><path d="M12 7v5M12 12H5m7 0h7M5 16v-4m14 4v-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
  },
  {
    path: '/eulogy/wall',
    name: '挽联缅怀',
    desc: '为已故亲人送上缅怀',
    color: '#8B5CF6',
    icon: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
  },
  {
    path: '/testament',
    name: '生前遗言',
    desc: '记录和管理数字遗产',
    color: '#F59E0B',
    icon: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>`,
  },
]

const accountStatus = computed(() => [
  { label: '手机号', val: userInfo.value?.phone || '—', type: '' },
  { label: '注册时间', val: userInfo.value?.createTime ? dayjs(userInfo.value.createTime).format('YYYY-MM-DD') : '—', type: '' },
  { label: '账号状态', val: userInfo.value?.status === 1 ? '正常' : '异常', type: userInfo.value?.status === 1 ? 'success' : 'danger' },
  { label: '实名状态', val: { 0:'未认证',1:'审核中',2:'已认证',3:'失败' }[userInfo.value?.realNameStatus] || '—', type: userInfo.value?.realNameStatus === 2 ? 'success' : 'warning' },
])

const goRealName = () => router.push('/realname')

onMounted(async () => {
  try {
    userProfile.value = await userApi.getFullProfile()
  } catch {}
  try {
    const rels = await relationApi.getMyRelations()
    relationsCount.value = rels?.length ?? 0
  } catch {}
  // 入场动画
  setTimeout(() => bannerRef.value?.classList.add('visible'), 50)
})
</script>

<style scoped>
.dashboard { max-width: 1000px; }

/* ── 欢迎横幅 ── */
.welcome-banner {
  position: relative;
  background: var(--c-sidebar);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 24px;
  opacity: 0;
  transform: translateY(10px);
  transition: opacity .5s ease, transform .5s ease;
}
.welcome-banner.visible { opacity: 1; transform: translateY(0); }
.welcome-bg { position: absolute; inset: 0; }
.w-orb { position: absolute; border-radius: 50%; filter: blur(50px); }
.w-orb1 { width:260px;height:260px;top:-80px;left:-40px;background:radial-gradient(circle,rgba(90,103,242,.3),transparent 70%); }
.w-orb2 { width:200px;height:200px;bottom:-60px;right:-20px;background:radial-gradient(circle,rgba(245,158,11,.2),transparent 70%); }

.welcome-body {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 32px;
}
.greet { font-size: 13px; color: #8899BB; margin-bottom: 4px; letter-spacing: 1px; }
.welcome-name {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #fff 30%, #F59E0B 90%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}
.welcome-sub { font-size: 13px; color: #8899BB; }

.welcome-avatar {
  background: linear-gradient(135deg, var(--c-primary), var(--c-amber));
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 0 0 4px rgba(255,255,255,.1);
}

.banner-alert {
  position: relative; z-index: 2;
  display: flex; align-items: center; gap: 8px;
  background: rgba(245,158,11,.12);
  border-top: 1px solid rgba(245,158,11,.2);
  padding: 10px 32px;
  font-size: 13px;
  color: #F5C97E;
  cursor: pointer;
  transition: var(--transition);
}
.banner-alert:hover { background: rgba(245,158,11,.18); }

/* ── 统计卡片 ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}
.stat-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  animation: slide-up .4s ease both;
  transition: box-shadow .2s, transform .2s;
}
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-icon {
  width: 46px; height: 46px;
  border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-val { font-size: 22px; font-weight: 800; color: var(--c-txt); line-height: 1; margin-bottom: 3px; }
.stat-label { font-size: 12px; color: var(--c-txt-s); }
.stat-trend { margin-left: auto; font-size: 11px; color: var(--c-txt-i); white-space: nowrap; }

/* ── 功能入口 ── */
.section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--c-txt-s);
  letter-spacing: .5px;
  text-transform: uppercase;
  margin-bottom: 14px;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.feat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  padding: 18px 20px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}
.feat-card::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: var(--feat-c);
  opacity: 0;
  transition: opacity .2s;
}
.feat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateX(2px);
  border-color: var(--feat-c);
}
.feat-card:hover::before { opacity: 1; }

.feat-icon-wrap {
  width: 44px; height: 44px;
  border-radius: var(--radius-sm);
  background: rgba(var(--feat-c-rgb, 90,103,242),.1);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  color: var(--feat-c);
  transition: var(--transition);
}
.feat-card:hover .feat-icon-wrap {
  background: var(--feat-c);
  color: #fff;
}
.feat-icon { display: flex; align-items: center; }
.feat-body { flex: 1; }
.feat-name { font-size: 14px; font-weight: 700; color: var(--c-txt); margin-bottom: 2px; }
.feat-desc { font-size: 12px; color: var(--c-txt-s); }
.feat-arrow { color: var(--c-txt-i); flex-shrink: 0; transition: var(--transition); }
.feat-card:hover .feat-arrow { color: var(--feat-c); transform: translateX(3px); }

/* ── 账户状态 ── */
.status-grid {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  box-shadow: var(--shadow-sm);
}
.status-item {}
.status-label { font-size: 12px; color: var(--c-txt-s); margin-bottom: 6px; }
.status-val { font-size: 13px; font-weight: 600; color: var(--c-txt); }

@keyframes slide-up {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2,1fr); }
  .feature-grid { grid-template-columns: 1fr; }
  .status-grid { grid-template-columns: repeat(2,1fr); }
  .welcome-body { flex-direction: column; gap: 16px; }
}

@media (max-width: 768px) {
  .dashboard { max-width: 100%; }
  /* 欢迎横幅 */
  .welcome-banner { border-radius: var(--radius-sm); }
  .welcome-body { padding: 20px 16px 16px; flex-direction: column; align-items: flex-start; gap: 12px; }
  .welcome-name { font-size: 22px; }
  .banner-alert { padding: 8px 16px; font-size: 12px; }
  /* 统计卡片：2列 */
  .stats-row { grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 18px; }
  .stat-card { padding: 14px 12px; gap: 10px; }
  .stat-val { font-size: 18px; }
  .stat-trend { display: none; }
  /* 功能入口：2列 */
  .feature-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .feat-card { padding: 14px 12px; flex-direction: column; align-items: flex-start; gap: 10px; }
  .feat-arrow { display: none; }
  .feat-icon-wrap { width: 38px; height: 38px; }
  .feat-name { font-size: 13px; }
  .feat-desc { display: none; }
  /* 账户状态 */
  .status-grid { grid-template-columns: repeat(2, 1fr); padding: 14px; gap: 12px; }
  .section-title { font-size: 11px; margin-bottom: 10px; }
}
@media (max-width: 400px) {
  .stats-row { grid-template-columns: 1fr 1fr; }
  .feature-grid { grid-template-columns: 1fr 1fr; }
}

</style>