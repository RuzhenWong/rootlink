<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-left">
        <h2 class="logo">RootLink</h2>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{ userInfo?.realName || userInfo?.phone }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="realname" v-if="userInfo?.realNameStatus !== 2">
                实名认证
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <el-aside width="210px" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#fff"
          text-color="#333"
          active-text-color="#667eea"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-menu-item index="/relations">
            <el-icon><Share /></el-icon>
            <span>
              亲属关系
              <el-badge v-if="pendingCount > 0" :value="pendingCount" class="menu-badge" />
            </span>
          </el-menu-item>

          <el-sub-menu index="eulogy">
            <template #title>
              <el-icon><Memo /></el-icon>
              <span>挽联缅怀</span>
            </template>
            <el-menu-item index="/eulogy/wall">挽联墙</el-menu-item>
            <el-menu-item index="/eulogy/submit">提交挽联</el-menu-item>
            <el-menu-item index="/eulogy/review">
              审核挽联
              <el-badge v-if="eulogyReviewCount > 0" :value="eulogyReviewCount" class="menu-badge" />
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/testament">
            <el-icon><Notebook /></el-icon>
            <span>生前遗言</span>
          </el-menu-item>

          <el-menu-item index="/family-tree">
            <el-icon><Connection /></el-icon>
            <span>家族关系树</span>
          </el-menu-item>

          <el-divider style="margin: 8px 0" />

          <el-menu-item index="/realname" v-if="userInfo?.realNameStatus !== 2">
            <el-icon><User /></el-icon>
            <span>实名认证</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><Setting /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { UserFilled, HomeFilled, User, Setting, Share, Memo, Notebook, Connection } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { relationApi } from '@/api/relation'
import { eulogyApi } from '@/api/eulogy'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const activeMenu = computed(() => route.path)

// 角标：待处理关系申请数
const pendingCount = ref(0)
// 角标：待审核挽联数
const eulogyReviewCount = ref(0)

const loadBadgeCounts = async () => {
  try {
    const applies = await relationApi.getPendingApplies()
    pendingCount.value = applies?.length || 0
  } catch (e) { /* 忽略 */ }

  try {
    const reviews = await eulogyApi.getMyPendingReviews()
    eulogyReviewCount.value = reviews?.length || 0
  } catch (e) { /* 忽略 */ }
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'realname') {
    router.push('/realname')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await authApi.logout()
      authStore.logout()
      router.push('/login')
    } catch (error) {
      // 取消操作
    }
  }
}

onMounted(() => {
  loadBadgeCounts()
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 20px;
  z-index: 10;
}
.logo {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  margin: 0;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
  color: #333;
}
.sidebar {
  background: #fff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}
.main-content {
  background: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
}
.menu-badge {
  margin-left: 6px;
  vertical-align: middle;
}
</style>
