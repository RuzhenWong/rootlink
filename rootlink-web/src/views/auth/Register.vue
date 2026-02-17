<template>
  <div class="auth-shell">
    <div class="brand-panel">
      <div class="brand-bg"></div>
      <div class="brand-orb orb1"></div>
      <div class="brand-orb orb2"></div>
      <div class="brand-orb orb3"></div>
      <div class="brand-content">
        <div class="brand-logo">🌳</div>
        <h1 class="brand-name">RootLink</h1>
        <p class="brand-tagline">加入您的家族</p>
        <div class="brand-steps">
          <div class="step-item" v-for="(s,i) in steps" :key="i">
            <div class="step-num">{{ i+1 }}</div>
            <div class="step-text">{{ s }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-card" ref="cardRef">
        <div class="form-header">
          <h2 class="form-title">创建账户</h2>
          <p class="form-sub">填写以下信息，开启家族传承之旅</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules">

          <el-form-item prop="phone">
            <label class="field-label">手机号</label>
            <el-input v-model="form.phone" size="large" placeholder="请输入手机号" class="rl-input" clearable>
              <template #prefix><svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon"><path d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code" style="margin-top:14px">
            <label class="field-label">验证码</label>
            <div class="code-row">
              <el-input v-model="form.code" size="large" placeholder="短信验证码" class="rl-input" style="flex:1">
                <template #prefix><svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon"><path d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></template>
              </el-input>
              <button class="code-btn" :disabled="countdown > 0" @click="sendCode">
                {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
              </button>
            </div>
          </el-form-item>

          <el-form-item prop="password" style="margin-top:14px">
            <label class="field-label">密码</label>
            <el-input v-model="form.password" type="password" size="large"
              placeholder="6-20位，包含大小写字母和数字" class="rl-input" show-password>
              <template #prefix><svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon"><rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="M8 11V7a4 4 0 018 0v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword" style="margin-top:14px">
            <label class="field-label">确认密码</label>
            <el-input v-model="form.confirmPassword" type="password" size="large"
              placeholder="请再次输入密码" class="rl-input" show-password @keyup.enter="handleRegister">
              <template #prefix><svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="input-icon"><path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></template>
            </el-input>
          </el-form-item>

          <el-button type="primary" size="large" class="submit-btn"
            :loading="loading" @click="handleRegister">
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form>

        <div class="alt-link">
          已有账号？<router-link to="/login" class="link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { validator } from '@/utils/validator'

const router = useRouter()
const formRef = ref()
const cardRef = ref()
const loading = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({ phone: '', code: '', password: '', confirmPassword: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: (r,v,cb) => validator.isPhone(v) ? cb() : cb(new Error('请输入正确的手机号')), trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: (r,v,cb) => validator.isPassword(v) ? cb() : cb(new Error('密码需6-20位，含大小写字母和数字')), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (r,v,cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

const steps = ['填写手机号和验证码', '设置安全密码', '完成实名认证，建立家族关系']

const sendCode = async () => {
  try {
    await formRef.value.validateField('phone')
    try {
      await authApi.sendCode({ phone: form.phone, type: 1 })
      ElMessage.success('验证码已发送')
    } catch {
      form.code = '000000'
      ElMessage.warning('开发模式：验证码已自动填入 000000')
    }
    countdown.value = 60
    timer = setInterval(() => { if (--countdown.value <= 0) clearInterval(timer) }, 1000)
  } catch {}
}

const handleRegister = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    await authApi.register({ phone: form.phone, code: form.code, password: form.password })
    ElMessage.success('注册成功！请登录')
    router.push('/login')
  } catch {} finally { loading.value = false }
}

onMounted(() => setTimeout(() => cardRef.value?.classList.add('visible'), 50))
</script>

<style scoped>
.auth-shell {
  display: flex; min-height: 100vh; background: var(--c-sidebar);
}
.brand-panel {
  width: 44%;
  position: relative; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}
.brand-bg { position: absolute; inset: 0; background: linear-gradient(145deg,#0C1730,#1A2D54,#0F2040); }
.brand-orb { position: absolute; border-radius: 50%; filter: blur(55px); }
.orb1 { width: 320px;height:320px;background:radial-gradient(circle,rgba(90,103,242,.28)0%,transparent 70%);top:-60px;left:-50px;animation:float1 9s ease-in-out infinite; }
.orb2 { width: 240px;height:240px;background:radial-gradient(circle,rgba(245,158,11,.18)0%,transparent 70%);bottom:-40px;right:-30px;animation:float2 11s ease-in-out infinite; }
.orb3 { width: 180px;height:180px;background:radial-gradient(circle,rgba(16,185,129,.12)0%,transparent 70%);top:55%;right:25%;animation:float3 8s ease-in-out infinite; }
.brand-content { position:relative;z-index:2;text-align:center;padding:40px; }
.brand-logo { font-size:52px;margin-bottom:12px;filter:drop-shadow(0 4px 20px rgba(245,158,11,.4));animation:pulse-logo 3s ease-in-out infinite; }
.brand-name { font-size:36px;font-weight:900;background:linear-gradient(135deg,#fff 20%,#F59E0B 80%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;margin-bottom:8px; }
.brand-tagline { font-size:14px;color:#8899BB;letter-spacing:3px;margin-bottom:32px; }
.brand-steps { display:flex;flex-direction:column;gap:14px;align-items:flex-start; }
.step-item { display:flex;align-items:center;gap:12px; }
.step-num { width:24px;height:24px;border-radius:50%;background:linear-gradient(135deg,var(--c-primary),var(--c-amber));color:#fff;font-size:12px;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0; }
.step-text { color:#C8D5EE;font-size:13px; }

.form-panel { flex:1;display:flex;align-items:center;justify-content:center;background:#F8FAFF;padding:40px; }
.form-card { width:100%;max-width:420px;background:#fff;border-radius:var(--radius-lg);padding:40px;box-shadow:var(--shadow-lg);border:1px solid var(--c-border);opacity:0;transform:translateY(20px);transition:opacity .5s ease,transform .5s ease; }
.form-card.visible { opacity:1;transform:translateY(0); }
.form-header { margin-bottom:28px; }
.form-title { font-size:24px;font-weight:800;color:var(--c-txt);margin-bottom:5px; }
.form-sub { font-size:13px;color:var(--c-txt-s); }
.field-label { display:block;font-size:13px;font-weight:600;color:var(--c-txt-s);margin-bottom:6px; }
.rl-input :deep(.el-input__wrapper) { height:44px;border-radius:var(--radius-sm)!important;border:1.5px solid var(--c-border)!important;box-shadow:none!important;transition:var(--transition)!important; }
.rl-input :deep(.el-input__wrapper:hover) { border-color:var(--c-primary)!important; }
.rl-input :deep(.el-input__wrapper.is-focus) { border-color:var(--c-primary)!important;box-shadow:0 0 0 3px rgba(90,103,242,.12)!important; }
.input-icon { color:var(--c-txt-i);margin-right:4px; }
.code-row { display:flex;gap:10px; }
.code-btn { flex-shrink:0;height:44px;padding:0 14px;border-radius:var(--radius-sm);border:1.5px solid var(--c-primary);background:transparent;color:var(--c-primary);font-size:13px;font-weight:600;cursor:pointer;transition:var(--transition);white-space:nowrap; }
.code-btn:not(:disabled):hover { background:var(--c-primary);color:#fff; }
.code-btn:disabled { border-color:var(--c-border);color:var(--c-txt-i);cursor:not-allowed; }
.submit-btn { width:100%;height:46px;margin-top:24px;font-size:15px;font-weight:700;background:linear-gradient(135deg,var(--c-primary) 0%,#7C3AED 100%)!important;border:none!important;border-radius:var(--radius-sm)!important;box-shadow:0 4px 14px rgba(90,103,242,.35)!important;transition:var(--transition)!important; }
.submit-btn:hover { transform:translateY(-1px);box-shadow:0 6px 20px rgba(90,103,242,.45)!important; }
.alt-link { text-align:center;margin-top:20px;font-size:13px;color:var(--c-txt-s); }
.link { color:var(--c-primary);font-weight:600;text-decoration:none;margin-left:4px; }
.link:hover { text-decoration:underline; }

@keyframes float1 { 0%,100%{transform:translate(0,0)}50%{transform:translate(18px,12px)} }
@keyframes float2 { 0%,100%{transform:translate(0,0)}50%{transform:translate(-12px,-16px)} }
@keyframes float3 { 0%,100%{transform:translate(-50%,-50%)}50%{transform:translate(-50%,-50%) scale(1.1)} }
@keyframes pulse-logo { 0%,100%{filter:drop-shadow(0 4px 20px rgba(245,158,11,.4))}50%{filter:drop-shadow(0 4px 30px rgba(245,158,11,.65))} }
@media(max-width:768px){.auth-shell{flex-direction:column}.brand-panel{width:100%;min-height:200px}.form-panel{padding:24px 20px}}

@media (max-width: 768px) {
  .auth-shell  { flex-direction: column; min-height: 100svh; }
  .brand-panel { width: 100%; min-height: 180px; }
  .brand-logo  { font-size: 40px; margin-bottom: 8px; }
  .brand-name  { font-size: 28px; }
  .brand-tagline { margin-bottom: 16px; }
  .brand-steps { flex-direction: row; flex-wrap: wrap; gap: 8px; justify-content: center; }
  .step-text   { font-size: 12px; }
  .form-panel  { padding: 20px 16px 32px; }
  .form-card   { padding: 24px 18px 20px; }
  .form-title  { font-size: 20px; }
  .code-row    { flex-wrap: nowrap; }
  .code-btn    { flex-shrink: 0; font-size: 12px; padding: 0 10px; }
}
@media (max-width: 380px) {
  .code-btn { padding: 0 8px; font-size: 11px; }
}

</style>
