<template>
  <div class="shell" :class="{ 'mobile-open': mobileMenuOpen }">

    <!-- ════════════════════════════════════════
         桌面端：左侧边栏
    ═════════════════════════════════════════ -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <div class="logo-icon">🌳</div>
        <span class="logo-text">RootLink</span>
      </div>

      <button class="collapse-btn" @click="collapsed = !collapsed">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path :d="collapsed ? 'M5 2l5 5-5 5' : 'M9 2L4 7l5 5'"
            stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>

      <nav class="sidebar-nav">
        <div class="nav-section-label" v-if="!collapsed">主功能</div>
        <router-link v-for="item in mainNavItems" :key="item.path"
          :to="item.path" class="nav-item" :class="{ active: isActive(item) }">
          <span class="nav-icon" v-html="item.icon" />
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge > 0" class="rl-badge" style="margin-left:auto">{{ item.badge }}</span>
        </router-link>

        <div class="nav-divider" />
        <div class="nav-section-label" v-if="!collapsed">系统</div>
        <router-link v-for="item in systemNavItems" :key="item.path"
          :to="item.path" class="nav-item" :class="{ active: isActive(item) }">
          <span class="nav-icon" v-html="item.icon" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-user">
        <el-dropdown placement="top-start" @command="handleCommand">
          <div class="user-trigger">
            <el-avatar :size="collapsed ? 28 : 32" :src="userProfile?.avatarUrl || ''" class="user-avatar">{{ avatarInitial }}</el-avatar>
            <div class="user-meta" v-if="!collapsed">
              <div class="user-name">{{ userInfo?.realName || '未实名' }}</div>
              <div class="user-phone">{{ userInfo?.phone }}</div>
            </div>
            <svg v-if="!collapsed" width="12" height="12" viewBox="0 0 12 12" fill="none" style="flex-shrink:0;color:#8899BB">
              <path d="M2 4l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">⚙️ 个人中心</el-dropdown-item>
              <el-dropdown-item v-if="userInfo?.realNameStatus !== 2" command="realname">🪪 实名认证</el-dropdown-item>
              <el-dropdown-item divided command="logout">🚪 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <!-- ════════════════════════════════════════
         手机端：顶部导航栏
    ═════════════════════════════════════════ -->
    <header class="mobile-topbar">
      <div class="mob-logo">
        <span class="mob-logo-icon">🌳</span>
        <span class="mob-logo-text">{{ currentPageTitle }}</span>
      </div>
      <div class="mob-topbar-right">
        <div v-if="pendingCount > 0" class="mob-badge-btn" @click="$router.push('/relations')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 00-4.56-5.845A2 2 0 0012 4a2 2 0 00-1.44.155A6 6 0 006 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="mob-notif-dot"></span>
        </div>
        <el-avatar :size="30" :src="userProfile?.avatarUrl || ''" class="mob-avatar"
          @click="$router.push('/profile')">{{ avatarInitial }}</el-avatar>
      </div>
    </header>

    <!-- ════════════════════════════════════════
         主内容区
    ═════════════════════════════════════════ -->
    <div class="main-wrap">
      <!-- 桌面顶栏 -->
      <header class="topbar">
        <div class="topbar-breadcrumb">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="topbar-actions">
          <el-tooltip v-if="userInfo?.realNameStatus !== 2"
            content="未完成实名认证，部分功能受限" placement="bottom">
            <div class="warn-chip" @click="$router.push('/realname')">⚠️ 未实名</div>
          </el-tooltip>
          <div class="action-btn" @click="$router.push('/relations')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 00-4.56-5.845A2 2 0 0012 4a2 2 0 00-1.44.155A6 6 0 006 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span v-if="pendingCount > 0" class="rl-badge" style="position:absolute;top:-4px;right:-4px;scale:.85">{{ pendingCount }}</span>
          </div>
          <el-avatar :size="32" :src="userProfile?.avatarUrl || ''" class="topbar-avatar"
            @click="$router.push('/profile')">{{ avatarInitial }}</el-avatar>
        </div>
      </header>

      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="rl-enter">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- ════════════════════════════════════════
         手机端：底部 Tab 导航
    ═════════════════════════════════════════ -->
    <nav class="bottom-nav">
      <router-link v-for="tab in bottomTabs" :key="tab.path"
        :to="tab.path" class="bottom-tab" :class="{ active: isActive(tab) }">
        <div class="tab-icon-wrap">
          <span class="tab-icon" v-html="tab.icon" />
          <span v-if="tab.badge > 0" class="tab-badge">{{ tab.badge }}</span>
        </div>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { relationApi } from '@/api/relation'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const collapsed = ref(false)
const mobileMenuOpen = ref(false)

const userInfo = computed(() => authStore.userInfo)
const userProfile = ref(null)
const pendingCount = ref(0)
const avatarInitial = computed(() => (userInfo.value?.realName || userInfo.value?.phone || '?')[0])

// 桌面侧边栏菜单
const mainNavItems = computed(() => [
  { path: '/dashboard',   label: '首页',     badge: 0, exact: true,
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/relations',   label: '亲属关系', badge: pendingCount.value,
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/family-tree', label: '家族树',   badge: 0,
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="5" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="5" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="12" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="19" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><path d="M12 7v5M12 12H5m7 0h7M5 16v-4m14 4v-4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>` },
  { path: '/eulogy/wall', label: '挽联墙',   badge: 0,
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/testament',   label: '遗言',     badge: 0,
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
])

const systemNavItems = [
  { path: '/profile', label: '个人中心',
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none"><path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/><path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.7"/></svg>` },
]

// 手机底部 Tab（5 项）
const bottomTabs = computed(() => [
  { path: '/dashboard',   label: '首页',   badge: 0, exact: true,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/relations',   label: '亲属',   badge: pendingCount.value,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/family-tree', label: '家族树', badge: 0,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="5" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="5" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="12" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><circle cx="19" cy="18" r="2" stroke="currentColor" stroke-width="1.7"/><path d="M12 7v5M12 12H5m7 0h7M5 16v-4m14 4v-4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>` },
  { path: '/eulogy/wall', label: '挽联',   badge: 0,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
  { path: '/profile',     label: '我的',   badge: 0,
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>` },
])

const isActive = (item) => item.exact ? route.path === item.path : route.path.startsWith(item.path)

const pageTitles = {
  '/dashboard':    '首页',
  '/relations':    '亲属关系',
  '/family-tree':  '家族关系树',
  '/eulogy/wall':  '挽联墙',
  '/eulogy/submit':'提交挽联',
  '/eulogy/review':'审核挽联',
  '/testament':    '生前遗言',
  '/profile':      '个人中心',
  '/realname':     '实名认证',
}
const currentPageTitle = computed(() => pageTitles[route.path] || 'RootLink')

const handleCommand = async (command) => {
  if (command === 'profile') router.push('/profile')
  else if (command === 'realname') router.push('/realname')
  else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning',
      })
      await authApi.logout()
      authStore.logout()
      router.push('/login')
    } catch {}
  }
}

onMounted(async () => {
  try {
    const applies = await relationApi.getPendingApplies()
    pendingCount.value = applies?.length || 0
  } catch {}
  try { userProfile.value = await userApi.getFullProfile() } catch {}
})
</script>

<style scoped>
/* ════════════════════════════════════
   整体框架
═══════════════════════════════════ */
.shell {
  display: flex;
  height: 100vh;
  background: var(--c-bg);
  overflow: hidden;
}

/* ════════════════════════════════════
   桌面侧边栏（≥769px 显示）
═══════════════════════════════════ */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--c-sidebar);
  position: relative;
  transition: width .25s cubic-bezier(.4,0,.2,1);
  z-index: 50;
}
.sidebar.collapsed { width: 64px; }

.sidebar-logo {
  display: flex; align-items: center; gap: 10px;
  padding: 22px 18px 16px;
  overflow: hidden;
}
.logo-icon { font-size: 22px; flex-shrink: 0; filter: drop-shadow(0 0 8px rgba(245,158,11,.5)); }
.logo-text {
  font-size: 18px; font-weight: 800; color: #fff;
  white-space: nowrap;
  background: linear-gradient(135deg,#fff 40%,#F59E0B);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  transition: opacity .2s;
}
.sidebar.collapsed .logo-text { opacity: 0; width: 0; }

.collapse-btn {
  position: absolute; right: -12px; top: 70px;
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--c-sidebar); border: 2px solid rgba(255,255,255,.12);
  color: #8899BB; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: var(--transition); z-index: 10;
}
.collapse-btn:hover { background: var(--c-primary); color: #fff; border-color: var(--c-primary); }

.sidebar-nav { flex: 1; padding: 8px 10px; overflow-y: auto; overflow-x: hidden; }
.sidebar.collapsed .sidebar-nav { padding: 8px 6px; }
.nav-section-label { font-size: 10px; font-weight: 700; letter-spacing: 1px; color: #3D5070; padding: 10px 8px 4px; white-space: nowrap; }
.nav-item {
  display: flex; align-items: center; gap: 10px; padding: 9px 10px;
  border-radius: var(--radius-sm); color: #8899BB; text-decoration: none;
  font-size: 13.5px; font-weight: 500; transition: var(--transition);
  position: relative; white-space: nowrap; overflow: hidden; margin-bottom: 2px;
}
.nav-item:hover { background: rgba(255,255,255,.06); color: #C8D5EE; }
.nav-item.active { background: rgba(90,103,242,.18); color: #fff; }
.nav-item.active::before {
  content: ''; position: absolute; left: 0; top: 20%; bottom: 20%;
  width: 3px; background: var(--c-primary); border-radius: 0 3px 3px 0;
}
.nav-icon { flex-shrink: 0; display: flex; align-items: center; justify-content: center; width: 20px; }
.nav-label { transition: opacity .2s, width .2s; }
.sidebar.collapsed .nav-label { opacity: 0; width: 0; overflow: hidden; }
.sidebar.collapsed .nav-icon { margin: 0 auto; }
.sidebar.collapsed .nav-item { justify-content: center; padding: 10px; }
.nav-divider { height: 1px; background: rgba(255,255,255,.07); margin: 10px 0; }

.sidebar-user { padding: 10px; border-top: 1px solid rgba(255,255,255,.08); }
.user-trigger {
  display: flex; align-items: center; gap: 9px; padding: 8px;
  border-radius: var(--radius-sm); cursor: pointer; transition: var(--transition);
}
.user-trigger:hover { background: rgba(255,255,255,.06); }
.sidebar.collapsed .user-trigger { justify-content: center; }
.user-avatar { flex-shrink: 0; background: linear-gradient(135deg,var(--c-primary),var(--c-amber)); font-weight: 700; font-size: 13px; }
.user-meta { flex: 1; overflow: hidden; }
.user-name { font-size: 13px; font-weight: 600; color: #C8D5EE; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-phone { font-size: 11px; color: #3D5070; white-space: nowrap; }
.sidebar.collapsed .user-meta { display: none; }

/* ════════════════════════════════════
   主内容区
═══════════════════════════════════ */
.main-wrap { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }

.topbar {
  height: 56px; background: var(--c-surface); border-bottom: 1px solid var(--c-border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; flex-shrink: 0;
}
.page-title { font-size: 15px; font-weight: 700; color: var(--c-txt); }
.topbar-actions { display: flex; align-items: center; gap: 12px; }
.warn-chip {
  display: flex; align-items: center; gap: 4px;
  background: rgba(245,158,11,.1); color: var(--c-amber-h);
  font-size: 12px; font-weight: 600; padding: 4px 10px;
  border-radius: 99px; cursor: pointer; border: 1px solid rgba(245,158,11,.25);
  transition: var(--transition);
}
.warn-chip:hover { background: rgba(245,158,11,.18); }
.action-btn {
  position: relative; width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); color: var(--c-txt-s); cursor: pointer; transition: var(--transition);
}
.action-btn:hover { background: var(--c-bg); color: var(--c-primary); }
.topbar-avatar { cursor: pointer; background: linear-gradient(135deg,var(--c-primary),var(--c-amber)); font-weight: 700; font-size: 13px; transition: var(--transition); }
.topbar-avatar:hover { opacity: .85; }

.page-content { flex: 1; padding: 24px; overflow-y: auto; overflow-x: hidden; }

/* 内容进场：只做进场淡入，无离场等待，彻底避免 out-in 死锁 */
.rl-enter-active { transition: opacity .15s ease; }
.rl-enter-from   { opacity: 0; }

/* ════════════════════════════════════
   手机端专用（≤768px）
═══════════════════════════════════ */
.mobile-topbar { display: none; }
.bottom-nav    { display: none; }

@media (max-width: 768px) {
  /* 隐藏桌面侧边栏和顶栏 */
  .sidebar { display: none; }
  .topbar  { display: none; }

  /* 手机顶部导航条 */
  .mobile-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: fixed;
    top: 0; left: 0; right: 0;
    height: 52px;
    background: var(--c-sidebar);
    z-index: 100;
    padding: 0 16px;
    box-shadow: 0 2px 12px rgba(0,0,0,.25);
  }
  .mob-logo {
    display: flex; align-items: center; gap: 8px;
  }
  .mob-logo-icon { font-size: 20px; filter: drop-shadow(0 0 6px rgba(245,158,11,.4)); }
  .mob-logo-text {
    font-size: 15px; font-weight: 700;
    background: linear-gradient(135deg,#fff 30%,#F59E0B 90%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
    max-width: 180px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
  }
  .mob-topbar-right { display: flex; align-items: center; gap: 12px; }
  .mob-badge-btn {
    position: relative; width: 36px; height: 36px;
    display: flex; align-items: center; justify-content: center;
    color: #8899BB; border-radius: var(--radius-sm);
    transition: var(--transition);
  }
  .mob-notif-dot {
    position: absolute; top: 4px; right: 4px;
    width: 8px; height: 8px; border-radius: 50%;
    background: var(--c-danger);
    border: 1.5px solid var(--c-sidebar);
  }
  .mob-avatar {
    background: linear-gradient(135deg,var(--c-primary),var(--c-amber));
    font-weight: 700; font-size: 12px; cursor: pointer;
  }

  /* 主内容区：顶部留空（顶栏52px）+ 底部留空（底栏60px + safe area）*/
  .main-wrap { display: block; overflow: auto; }
  .page-content {
    padding: 12px 12px calc(68px + env(safe-area-inset-bottom, 8px));
    padding-top: calc(52px + 12px);
    min-height: 100vh;
    overflow-y: auto;
  }

  /* 底部导航栏 */
  .bottom-nav {
    display: flex;
    position: fixed;
    bottom: 0; left: 0; right: 0;
    height: calc(56px + env(safe-area-inset-bottom, 0px));
    background: var(--c-sidebar);
    border-top: 1px solid rgba(255,255,255,.08);
    z-index: 100;
    padding-bottom: env(safe-area-inset-bottom, 0px);
    box-shadow: 0 -4px 20px rgba(0,0,0,.2);
  }
  .bottom-tab {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    text-decoration: none;
    color: #5A6888;
    padding: 6px 4px;
    transition: var(--transition);
    position: relative;
  }
  .bottom-tab.active { color: #fff; }
  .bottom-tab.active::before {
    content: '';
    position: absolute;
    top: 0; left: 20%; right: 20%;
    height: 2.5px;
    background: var(--c-primary);
    border-radius: 0 0 3px 3px;
  }
  .tab-icon-wrap {
    position: relative;
    display: flex; align-items: center; justify-content: center;
  }
  .tab-icon { display: flex; align-items: center; transition: transform .15s; }
  .bottom-tab.active .tab-icon { transform: scale(1.1); }
  .tab-badge {
    position: absolute; top: -5px; right: -8px;
    min-width: 16px; height: 16px; padding: 0 4px;
    background: var(--c-danger); color: #fff;
    border-radius: 99px; font-size: 10px; font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    border: 1.5px solid var(--c-sidebar);
  }
  .tab-label { font-size: 10px; font-weight: 600; letter-spacing: .2px; }
}
</style>
