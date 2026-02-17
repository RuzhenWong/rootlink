<template>
  <div class="auth-shell">
    <!-- 左侧品牌面板 -->
    <div class="brand-panel">
      <div class="brand-bg"></div>
      <div class="brand-orb orb1"></div>
      <div class="brand-orb orb2"></div>
      <div class="brand-orb orb3"></div>
      <div class="brand-particles">
        <span v-for="i in 18" :key="i" class="particle"
          :style="particleStyle(i)" />
      </div>
      <div class="brand-content">
        <div class="brand-logo">🌳</div>
        <h1 class="brand-name">RootLink</h1>
        <p class="brand-tagline">家族家谱 · 亲情传承</p>
        <div class="brand-features">
          <div class="feat-item" v-for="f in features" :key="f">
            <span class="feat-dot">✦</span> {{ f }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单面板 -->
    <div class="form-panel">
      <div class="form-card" ref="cardRef">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-sub">登录您的家族账户，查看亲属动态</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <el-form-item prop="phone">
            <label class="field-label">手机号</label>
            <el-input v-model="form.phone" size="large" placeholder="请输入注册手机号"
              class="rl-input" clearable>
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon">
                  <path d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password" style="margin-top:16px">
            <label class="field-label">密码</label>
            <el-input v-model="form.password" type="password" size="large"
              placeholder="请输入密码" class="rl-input" show-password @keyup.enter="handleLogin">
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon">
                  <rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.7"/>
                  <path d="M8 11V7a4 4 0 018 0v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                  <circle cx="12" cy="16" r="1" fill="currentColor"/>
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="primary" size="large" class="submit-btn"
            :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '立即登录' }}
          </el-button>
        </el-form>

        <div class="alt-link">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { validator } from '@/utils/validator'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const cardRef = ref()
const loading = ref(false)

const form = reactive({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: (r, v, cb) => validator.isPhone(v) ? cb() : cb(new Error('请输入正确的手机号')), trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const features = ['完整的家族关系图谱', '生前遗言数字存档', '家族挽联缅怀墙', '智能亲属关系推断']

const particleStyle = (i) => {
  const angle = (i / 18) * 360
  const r = 30 + (i % 3) * 20
  const x = 50 + Math.cos(angle * Math.PI / 180) * r
  const y = 50 + Math.sin(angle * Math.PI / 180) * r
  return {
    left: x + '%',
    top: y + '%',
    animationDelay: (i * 0.3) + 's',
    width: (2 + i % 3) + 'px',
    height: (2 + i % 3) + 'px',
    opacity: 0.3 + (i % 4) * 0.1,
  }
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    await authStore.login(form)
    ElMessage.success('登录成功，欢迎回来！')
    router.push('/')
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

onMounted(() => {
  setTimeout(() => cardRef.value?.classList.add('visible'), 50)
})
</script>

<style scoped>
.auth-shell {
  display: flex;
  min-height: 100vh;
  background: var(--c-sidebar);
}

/* ── 左侧品牌 ── */
.brand-panel {
  width: 48%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-bg {
  position: absolute; inset: 0;
  background: linear-gradient(145deg, #0C1730 0%, #1A2D54 50%, #0F2040 100%);
}
.brand-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
}
.orb1 {
  width: 350px; height: 350px;
  background: radial-gradient(circle, rgba(90,103,242,.3) 0%, transparent 70%);
  top: -80px; left: -60px;
  animation: float1 8s ease-in-out infinite;
}
.orb2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(245,158,11,.2) 0%, transparent 70%);
  bottom: -50px; right: -40px;
  animation: float2 10s ease-in-out infinite;
}
.orb3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(16,185,129,.15) 0%, transparent 70%);
  top: 50%; right: 20%;
  animation: float3 7s ease-in-out infinite;
}

.brand-particles {
  position: absolute; inset: 0; pointer-events: none;
}
.particle {
  position: absolute;
  background: rgba(255,255,255,.6);
  border-radius: 50%;
  animation: twinkle 3s ease-in-out infinite;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 40px;
}
.brand-logo {
  font-size: 60px;
  margin-bottom: 16px;
  filter: drop-shadow(0 4px 20px rgba(245,158,11,.4));
  animation: pulse-logo 3s ease-in-out infinite;
}
.brand-name {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #fff 20%, #F59E0B 80%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 10px;
}
.brand-tagline {
  font-size: 15px;
  color: #8899BB;
  letter-spacing: 3px;
  margin-bottom: 36px;
}
.brand-features { display: flex; flex-direction: column; gap: 10px; align-items: flex-start; }
.feat-item { color: #C8D5EE; font-size: 14px; display: flex; align-items: center; gap: 10px; }
.feat-dot { color: var(--c-amber); font-size: 10px; }

/* ── 右侧表单 ── */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F8FAFF;
  padding: 40px;
}
.form-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 44px 40px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--c-border);
  opacity: 0;
  transform: translateY(20px);
  transition: opacity .5s ease, transform .5s ease;
}
.form-card.visible { opacity: 1; transform: translateY(0); }

.form-header { margin-bottom: 32px; }
.form-title { font-size: 26px; font-weight: 800; color: var(--c-txt); margin-bottom: 6px; }
.form-sub { font-size: 13px; color: var(--c-txt-s); }

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-txt-s);
  margin-bottom: 6px;
  letter-spacing: .2px;
}

.rl-input :deep(.el-input__wrapper) {
  padding: 0 14px;
  height: 44px;
  border-radius: var(--radius-sm) !important;
  border: 1.5px solid var(--c-border) !important;
  box-shadow: none !important;
  transition: var(--transition) !important;
}
.rl-input :deep(.el-input__wrapper:hover) {
  border-color: var(--c-primary) !important;
}
.rl-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--c-primary) !important;
  box-shadow: 0 0 0 3px rgba(90,103,242,.12) !important;
}
.rl-input :deep(.el-input__inner) { font-size: 14px; }

.input-icon { color: var(--c-txt-i); margin-right: 4px; }

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 28px;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--c-primary) 0%, #7C3AED 100%) !important;
  border: none !important;
  border-radius: var(--radius-sm) !important;
  letter-spacing: .5px;
  transition: var(--transition) !important;
  box-shadow: 0 4px 14px rgba(90,103,242,.35) !important;
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(90,103,242,.45) !important;
}
.submit-btn:active { transform: translateY(0); }

.alt-link {
  text-align: center;
  margin-top: 22px;
  font-size: 13px;
  color: var(--c-txt-s);
}
.link { color: var(--c-primary); font-weight: 600; text-decoration: none; margin-left: 4px; }
.link:hover { text-decoration: underline; }

/* ── Animations ── */
@keyframes float1 {
  0%,100% { transform: translate(0,0) scale(1); }
  50%      { transform: translate(20px,15px) scale(1.05); }
}
@keyframes float2 {
  0%,100% { transform: translate(0,0) scale(1); }
  50%      { transform: translate(-15px,-20px) scale(1.08); }
}
@keyframes float3 {
  0%,100% { transform: translate(-50%,-50%) scale(1); }
  50%      { transform: translate(-50%,-50%) scale(1.12); }
}
@keyframes twinkle {
  0%,100% { opacity: .2; transform: scale(1); }
  50%      { opacity: .8; transform: scale(1.5); }
}
@keyframes pulse-logo {
  0%,100% { filter: drop-shadow(0 4px 20px rgba(245,158,11,.4)); }
  50%      { filter: drop-shadow(0 4px 30px rgba(245,158,11,.7)); }
}

@media (max-width: 768px) {
  .auth-shell { flex-direction: column; }
  .brand-panel { width: 100%; min-height: 220px; }
  .form-panel { padding: 24px 20px; }
}

@media (max-width: 768px) {
  .auth-shell { flex-direction: column; min-height: 100svh; }
  .brand-panel { width: 100%; min-height: 200px; padding: 24px 20px 20px; }
  .brand-logo  { font-size: 44px; margin-bottom: 10px; }
  .brand-name  { font-size: 30px; }
  .brand-tagline { font-size: 12px; margin-bottom: 20px; }
  .brand-features { flex-direction: row; flex-wrap: wrap; gap: 6px; justify-content: center; }
  .feat-item { font-size: 12px; }
  .form-panel { padding: 20px 16px 32px; }
  .form-card  { padding: 28px 20px 24px; border-radius: var(--radius-lg); box-shadow: var(--shadow-md); }
  .form-title { font-size: 22px; }
}
@media (max-width: 380px) {
  .brand-panel { min-height: 160px; }
  .form-card   { padding: 22px 16px; }
}

</style>
