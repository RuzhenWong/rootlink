<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <h2>欢迎回来，{{ userName }}！</h2>
      <p>这里是您的家族家谱管理平台</p>

      <el-alert
        v-if="userInfo?.realNameStatus !== 2"
        type="warning"
        :closable="false"
        class="alert"
      >
        <template #title>
          <div style="display: flex; align-items: center; justify-content: space-between">
            <span>您还未完成实名认证，部分功能将受到限制</span>
            <el-button type="primary" size="small" @click="goToRealName">
              立即认证
            </el-button>
          </div>
        </template>
      </el-alert>

      <div class="info-grid">
        <div class="info-item">
          <div class="info-label">账号状态</div>
          <div class="info-value">
            <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'">
              {{ userInfo?.status === 1 ? '正常' : '异常' }}
            </el-tag>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">实名状态</div>
          <div class="info-value">
            <el-tag :type="getRealNameStatusType(userInfo?.realNameStatus)">
              {{ getRealNameStatusText(userInfo?.realNameStatus) }}
            </el-tag>
          </div>
        </div>

        <div class="info-item">
          <div class="info-label">注册时间</div>
          <div class="info-value">{{ formatTime(userInfo?.createTime) }}</div>
        </div>

        <div class="info-item">
          <div class="info-label">最后登录</div>
          <div class="info-value">{{ formatTime(userInfo?.lastLoginTime) }}</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="feature-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/relations')">
          <div class="feature-item">
            <el-icon :size="40" color="#667eea"><Share /></el-icon>
            <h3>亲属关系</h3>
            <p>建立和管理家族关系网络</p>
            <el-button type="primary" size="small">进入管理</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/eulogy/wall')">
          <div class="feature-item">
            <el-icon :size="40" color="#10b981"><Memo /></el-icon>
            <h3>挽联缅怀</h3>
            <p>为已故亲人送上深切缅怀</p>
            <el-button type="success" size="small">查看挽联墙</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/testament')">
          <div class="feature-item">
            <el-icon :size="40" color="#f59e0b"><Notebook /></el-icon>
            <h3>生前遗言</h3>
            <p>记录和管理数字遗产</p>
            <el-button type="warning" size="small">写遗言</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="feature-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="feature-card" @click="router.push('/family-tree')">
          <div class="feature-item">
            <el-icon :size="40" color="#667eea"><Connection /></el-icon>
            <h3>家族关系树</h3>
            <p>可视化查看家族关系图谱</p>
            <el-button type="primary" size="small">查看家族树</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Share, Memo, Notebook, Connection } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const userName = computed(() => userInfo.value?.realName || userInfo.value?.phone || '用户')

const goToRealName = () => {
  router.push('/realname')
}

const getRealNameStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
  }
  return map[status] || 'info'
}

const getRealNameStatusText = (status) => {
  const map = {
    0: '未实名',
    1: '审核中',
    2: '已实名',
    3: '审核失败',
  }
  return map[status] || '未知'
}

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.welcome-card {
  margin-bottom: 20px;
}

.welcome-card h2 {
  margin: 0 0 10px 0;
  color: #333;
}

.welcome-card p {
  margin: 0 0 20px 0;
  color: #666;
}

.alert {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.info-item {
  text-align: center;
}

.info-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.feature-cards {
  margin-top: 20px;
}

.feature-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.feature-card:hover {
  transform: translateY(-3px);
}
.feature-item {
  text-align: center;
  padding: 20px 0;
}

.feature-item h3 {
  margin: 15px 0 10px 0;
  color: #333;
}

.feature-item p {
  margin: 0 0 15px 0;
  color: #666;
  font-size: 14px;
}
</style>
