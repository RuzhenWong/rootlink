<template>
  <div class="register-container">
    <div class="register-box">
      <div class="logo-section">
        <h1 class="logo-title">RootLink</h1>
        <p class="logo-subtitle">创建您的家族账号</p>
      </div>

      <el-form ref="registerFormRef" :model="registerForm" :rules="rules" class="register-form">
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="code">
          <div class="code-input-wrapper">
            <el-input
              v-model="registerForm.code"
              placeholder="请输入验证码"
              prefix-icon="Message"
              size="large"
              style="flex: 1"
            />
            <el-button
              type="primary"
              size="large"
              :disabled="countdown > 0"
              @click="handleSendCode"
              style="margin-left: 10px; width: 120px"
            >
              {{ countdown > 0 ? `${countdown}秒后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码(6-20位，包含大小写字母和数字)"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </el-button>

        <div class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { validator } from '@/utils/validator'

const router = useRouter()

const registerFormRef = ref()
const loading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const registerForm = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const validatePassword = (rule, value, callback) => {
  if (!validator.isPassword(value)) {
    callback(new Error('密码需6-20位，包含大小写字母和数字'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!validator.isPhone(value)) {
          callback(new Error('请输入正确的手机号'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  // TODO: 短信服务接入后，恢复 required: true 和 isCode 校验
  code: [],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleSendCode = async () => {
  try {
    await registerFormRef.value.validateField('phone')

    // TODO: 短信服务接入后，删除 try-catch 的降级逻辑，恢复正式短信发送
    try {
      await authApi.sendCode({
        phone: registerForm.phone,
        type: 1, // 1-注册
      })
      ElMessage.success('验证码已发送')
    } catch {
      // 短信服务未接入，开发模式自动填入 000000
      registerForm.code = '000000'
      ElMessage.warning('短信服务未接入（开发模式），已自动填入验证码 000000')
    }

    // 开始倒计时
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()

    loading.value = true

    await authApi.register({
      phone: registerForm.phone,
      code: registerForm.code,
      password: registerForm.password,
    })

    ElMessage.success('注册成功，请登录')

    // 跳转到登录页
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
  width: 450px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo-section {
  text-align: center;
  margin-bottom: 40px;
}

.logo-title {
  font-size: 36px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 10px;
}

.logo-subtitle {
  font-size: 14px;
  color: #999;
}

.register-form {
  margin-top: 30px;
}

.code-input-wrapper {
  display: flex;
  width: 100%;
}

.register-btn {
  width: 100%;
  margin-top: 10px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.login-link a {
  color: #667eea;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
