<template>
  <div class="realname-page">
    <el-card class="realname-card">
      <template #header>
        <div class="card-header">
          <h2>实名认证</h2>
          <p>完成实名认证后，您可以使用完整的功能</p>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="notice-alert"
      >
        <template #title>
          <div>
            <div style="font-weight: bold; margin-bottom: 8px">重要提示</div>
            <ul style="margin: 0; padding-left: 20px">
              <li>您的身份证号将被加密存储，仅用于身份验证</li>
              <li>实名信息一旦认证成功，不可修改</li>
              <li>当前为Mock模式，仅做格式校验，不会真实调用第三方API</li>
            </ul>
          </div>
        </template>
      </el-alert>

      <el-form
        ref="realnameFormRef"
        :model="realnameForm"
        :rules="rules"
        label-width="100px"
        class="realname-form"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input
            v-model="realnameForm.realName"
            placeholder="请输入真实姓名"
            size="large"
          />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input
            v-model="realnameForm.idCard"
            placeholder="请输入18位身份证号"
            maxlength="18"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSubmit"
          >
            提交认证
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { validator } from '@/utils/validator'

const router = useRouter()
const authStore = useAuthStore()

const realnameFormRef = ref()
const loading = ref(false)

const realnameForm = reactive({
  realName: '',
  idCard: '',
})

const rules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!validator.isIdCard(value)) {
          callback(new Error('请输入正确的身份证号'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const handleSubmit = async () => {
  try {
    await realnameFormRef.value.validate()

    loading.value = true

    await userApi.submitRealName(realnameForm)

    ElMessage.success('实名认证成功')

    // 刷新用户信息
    await authStore.fetchUserInfo()

    // 跳转到首页
    router.push('/')
  } catch (error) {
    console.error('实名认证失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.realname-page { max-width: 640px; }
:deep(.el-card) { border-radius: var(--radius-md) !important; border: 1px solid var(--c-border) !important; box-shadow: var(--shadow-sm) !important; }
:deep(.el-card__header) { background: #F8FAFC; border-bottom: 1px solid var(--c-border); }
:deep(.el-input__wrapper) { border-radius: var(--radius-sm) !important; border: 1.5px solid var(--c-border) !important; box-shadow: none !important; transition: var(--transition) !important; }
:deep(.el-input__wrapper:hover) { border-color: var(--c-primary) !important; }
:deep(.el-input__wrapper.is-focus) { border-color: var(--c-primary) !important; box-shadow: 0 0 0 3px rgba(90,103,242,.1) !important; }
:deep(.el-form-item__label) { font-weight: 600; color: var(--c-txt-s); font-size: 13px; }
:deep(.el-alert) { border-radius: var(--radius-sm) !important; border: 1px solid; }
:deep(.el-result) { padding: 40px 20px; }

@media (max-width: 768px) {
  .realname-page { max-width: 100%; }
  :deep(.el-card__body) { padding: 16px 12px; }
  :deep(.el-form-item__label) { width: 100% !important; float: none !important; text-align: left !important; padding: 0 0 4px; }
  :deep(.el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-steps) { overflow-x: auto; flex-wrap: nowrap; padding-bottom: 4px; }
  :deep(.el-step__title) { font-size: 11px; }
  :deep(.el-button--large) { width: 100%; }
}

</style>