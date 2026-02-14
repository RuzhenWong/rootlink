import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false, title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { requiresAuth: false, title: '注册' },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { requiresAuth: true, title: '首页' },
      },
      {
        path: 'realname',
        name: 'RealName',
        component: () => import('@/views/user/RealName.vue'),
        meta: { requiresAuth: true, title: '实名认证' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { requiresAuth: true, title: '个人中心' },
      },
      {
        path: 'relations',
        name: 'Relations',
        component: () => import('@/views/relation/Relations.vue'),
        meta: { requiresAuth: true, title: '亲属关系' },
      },
      {
        path: 'eulogy/wall',
        name: 'EulogyWall',
        component: () => import('@/views/eulogy/EulogyWall.vue'),
        meta: { requiresAuth: true, title: '挽联墙' },
      },
      {
        path: 'eulogy/submit',
        name: 'SubmitEulogy',
        component: () => import('@/views/eulogy/SubmitEulogy.vue'),
        meta: { requiresAuth: true, title: '提交挽联' },
      },
      {
        path: 'eulogy/review',
        name: 'ReviewEulogy',
        component: () => import('@/views/eulogy/ReviewEulogy.vue'),
        meta: { requiresAuth: true, title: '审核挽联' },
      },
      {
        path: 'testament',
        name: 'Testament',
        component: () => import('@/views/testament/Testament.vue'),
        meta: { requiresAuth: true, title: '生前遗言' },
      },
      {
        path: 'family-tree',
        name: 'FamilyTree',
        component: () => import('@/views/relation/FamilyTree.vue'),
        meta: { requiresAuth: true, title: '家族关系树' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - RootLink` : 'RootLink'

  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    // 如果没有用户信息，先获取
    if (!authStore.userInfo) {
      try {
        await authStore.fetchUserInfo()
      } catch (error) {
        authStore.logout()
        next({ name: 'Login' })
        return
      }
    }
  }

  next()
})

export default router
