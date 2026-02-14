<template>
  <div class="profile-page">
    <el-tabs v-model="activeTab" type="border-card">

      <!-- =============== Tab1: 账号 =============== -->
      <el-tab-pane label="账号设置" name="account">
        <div v-if="!loadingProfile">

          <!-- 头像 -->
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
            >
              <el-avatar
                :size="88"
                :src="avatarPreview || profile.avatarUrl || ''"
                :icon="!avatarPreview && !profile.avatarUrl ? 'UserFilled' : undefined"
                class="avatar-img"
              />
              <div class="avatar-mask"><el-icon :size="18"><Camera /></el-icon><span>换头像</span></div>
            </el-upload>
            <div class="avatar-hint">点击头像更换，支持 jpg/png/webp，≤5MB</div>
          </div>

          <el-divider />

          <el-form :model="settingsForm" label-width="120px" style="max-width:520px">
            <el-form-item label="用户ID">
              <el-input :value="profile.userId" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :value="profile.phone" disabled />
            </el-form-item>
            <el-form-item label="实名状态">
              <el-tag :type="realNameTagType(profile.realNameStatus)">
                {{ realNameLabel(profile.realNameStatus) }}
              </el-tag>
              <el-button v-if="profile.realNameStatus !== 2" text type="primary"
                style="margin-left:10px" @click="$router.push('/realname')">
                去实名
              </el-button>
            </el-form-item>
            <el-form-item label="注册时间">
              <el-input :value="fmt(profile.createTime)" disabled />
            </el-form-item>
            <el-form-item label="最后登录">
              <el-input :value="fmt(profile.lastLoginTime)" disabled />
            </el-form-item>

            <el-divider content-position="left">隐私设置</el-divider>

            <el-form-item label="允许身份证搜索">
              <el-switch
                v-model="settingsForm.allowSearch"
                active-text="开启（他人可通过身份证号找到我）"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingSettings" @click="saveSettings">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-skeleton v-else :rows="7" animated />
      </el-tab-pane>

      <!-- =============== Tab2: 详细资料 =============== -->
      <el-tab-pane label="详细资料" name="detail">
        <div v-if="!loadingProfile">
          <!-- 婚姻状态推断提示 -->
          <el-alert
            v-if="inferredMarriage"
            :title="inferredMarriage"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom:16px"
          />

          <el-form :model="detailForm" label-width="100px">
            <el-row :gutter="20">

              <!-- 邮箱 -->
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="detailForm.email" placeholder="请输入邮箱" clearable />
                </el-form-item>
              </el-col>

              <!-- 性别 -->
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-select v-model="detailForm.gender" clearable placeholder="请选择" style="width:100%">
                    <el-option label="未填写" :value="0" />
                    <el-option label="男" :value="1" />
                    <el-option label="女" :value="2" />
                  </el-select>
                </el-form-item>
              </el-col>

              <!-- 出生日期 -->
              <el-col :span="12">
                <el-form-item label="出生日期">
                  <el-date-picker
                    v-model="detailForm.birthDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                    style="width:100%"
                  />
                </el-form-item>
              </el-col>

              <!-- 民族 -->
              <el-col :span="12">
                <el-form-item label="民族">
                  <el-input v-model="detailForm.nation" placeholder="如：汉族" />
                </el-form-item>
              </el-col>

              <!-- 婚姻状态（手动 + 推断两用） -->
              <el-col :span="12">
                <el-form-item label="婚姻状态">
                  <el-select v-model="detailForm.maritalStatus" clearable placeholder="请选择" style="width:100%">
                    <el-option label="未知" :value="0" />
                    <el-option label="未婚" :value="1" />
                    <el-option label="已婚" :value="2" />
                    <el-option label="离异" :value="3" />
                    <el-option label="丧偶" :value="4" />
                  </el-select>
                </el-form-item>
              </el-col>

              <!-- 家族排行 -->
              <el-col :span="12">
                <el-form-item label="家族排行">
                  <el-input-number
                    v-model="detailForm.familyRank"
                    :min="1"
                    :max="20"
                    placeholder="同辈中第几"
                    style="width:100%"
                  />
                  <div class="field-hint">1=老大，2=老二，用于显示"二叔"、"三姨"等称谓</div>
                </el-form-item>
              </el-col>

              <!-- 学历 -->
              <el-col :span="12">
                <el-form-item label="学历">
                  <el-select v-model="detailForm.education" clearable placeholder="请选择" style="width:100%">
                    <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
                  </el-select>
                </el-form-item>
              </el-col>

              <!-- 工作单位 -->
              <el-col :span="12">
                <el-form-item label="工作单位">
                  <el-input v-model="detailForm.workUnit" placeholder="公司/单位名称" />
                </el-form-item>
              </el-col>

              <!-- 职务 -->
              <el-col :span="12">
                <el-form-item label="职务">
                  <el-input v-model="detailForm.position" placeholder="职位/职称" />
                </el-form-item>
              </el-col>

              <!-- 微信 -->
              <el-col :span="12">
                <el-form-item label="微信号">
                  <el-input v-model="detailForm.wechat" placeholder="微信账号" />
                </el-form-item>
              </el-col>

              <!-- QQ -->
              <el-col :span="12">
                <el-form-item label="QQ 号">
                  <el-input v-model="detailForm.qq" placeholder="QQ 号码" />
                </el-form-item>
              </el-col>

              <!-- ---- 籍贯：省市联动 ---- -->
              <el-col :span="24">
                <el-form-item label="籍贯">
                  <div class="region-row">
                    <el-select
                      v-model="detailForm.province"
                      placeholder="省份"
                      clearable
                      style="width:150px"
                      @change="onProvinceChange"
                    >
                      <el-option
                        v-for="p in provinces"
                        :key="p.value"
                        :label="p.label"
                        :value="p.value"
                      />
                    </el-select>
                    <el-select
                      v-model="detailForm.city"
                      placeholder="城市"
                      clearable
                      style="width:150px"
                      :disabled="!detailForm.province"
                    >
                      <el-option v-for="c in availableCities" :key="c" :label="c" :value="c" />
                    </el-select>
                    <el-input
                      v-model="detailForm.district"
                      placeholder="区/县（可手填）"
                      style="width:150px"
                      clearable
                    />
                  </div>
                </el-form-item>
              </el-col>

              <!-- 居住地 -->
              <el-col :span="24">
                <el-form-item label="居住地">
                  <el-input v-model="detailForm.residence" placeholder="当前居住地详细地址" />
                </el-form-item>
              </el-col>

              <!-- 个人简介 -->
              <el-col :span="24">
                <el-form-item label="个人简介">
                  <el-input
                    v-model="detailForm.bio"
                    type="textarea"
                    :rows="3"
                    placeholder="介绍一下自己..."
                    maxlength="300"
                    show-word-limit
                  />
                </el-form-item>
              </el-col>

            </el-row>

            <el-form-item>
              <el-button type="primary" :loading="savingDetail" @click="saveDetail">保存资料</el-button>
              <el-button @click="resetDetail" style="margin-left:8px">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-skeleton v-else :rows="10" animated />
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { relationApi } from '@/api/relation'
import { useAuthStore } from '@/stores/auth'
import { provinces, getCitiesByProvince } from '@/utils/chinaRegions'

const authStore = useAuthStore()
const activeTab = ref('account')
const loadingProfile = ref(true)
const profile = ref({})

const settingsForm = ref({ allowSearch: false })
const detailForm = ref({})
const savingSettings = ref(false)
const savingDetail = ref(false)
const avatarPreview = ref('')

const educationOptions = [
  '无文化/无教育经历', '小学', '初中', '高中', '中专/职高',
  '大专', '本科', '硕士研究生', '博士研究生', '博士后',
]

// 省市联动
const availableCities = computed(() => {
  return detailForm.value.province ? getCitiesByProvince(detailForm.value.province) : []
})
const onProvinceChange = () => { detailForm.value.city = ''; detailForm.value.district = '' }

// 婚姻推断（从已建立关系推断）
const inferredMarriage = ref('')

const loadInferredMarriage = async () => {
  try {
    const rels = await relationApi.getMyRelations()
    if (!rels || rels.length === 0) return
    const hasSpouse = rels.some(r => r.relationDesc === '配偶')
    const hasChild = rels.some(r => ['儿子', '女儿', '孙子', '孙女', '外孙', '外孙女'].includes(r.relationDesc))
    const hasBothParents = rels.filter(r => ['父亲', '母亲'].includes(r.relationDesc)).length === 2
    if (hasSpouse) {
      inferredMarriage.value = '✅ 根据您的亲属关系（配偶），系统推断您的婚姻状态为「已婚」'
      if (!detailForm.value.maritalStatus || detailForm.value.maritalStatus === 0 || detailForm.value.maritalStatus === 1) {
        detailForm.value.maritalStatus = 2
      }
    } else if (hasChild) {
      inferredMarriage.value = '📌 您有子女亲属关系，婚姻状态可能需要更新'
    }
  } catch (e) { /* 静默 */ }
}

const loadProfile = async () => {
  loadingProfile.value = true
  try {
    const data = await userApi.getFullProfile()
    profile.value = data
    settingsForm.value = { allowSearch: !!data.allowSearch }
    detailForm.value = {
      email: data.email || '',
      gender: data.gender ?? null,
      maritalStatus: data.maritalStatus ?? null,
      birthDate: data.birthDate || null,
      nation: data.nation || '',
      province: data.province || null,
      city: data.city || null,
      district: data.district || '',
      residence: data.residence || '',
      workUnit: data.workUnit || '',
      position: data.position || '',
      education: data.education || null,
      wechat: data.wechat || '',
      qq: data.qq || '',
      bio: data.bio || '',
      familyRank: data.familyRank || null,
    }
    await loadInferredMarriage()
  } catch (e) { console.error(e) }
  finally { loadingProfile.value = false }
}

const saveSettings = async () => {
  savingSettings.value = true
  try {
    await userApi.updateSettings(settingsForm.value)
    ElMessage.success('设置已保存')
    await authStore.fetchUserInfo?.()
  } catch (e) { console.error(e) }
  finally { savingSettings.value = false }
}

const saveDetail = async () => {
  savingDetail.value = true
  try {
    await userApi.updateProfile(detailForm.value)
    ElMessage.success('资料已保存')
    profile.value = { ...profile.value, email: detailForm.value.email }
  } catch (e) { console.error(e) }
  finally { savingDetail.value = false }
}

const resetDetail = () => loadProfile()

// 头像上传
const beforeAvatarUpload = (file) => {
  const isImg = /\.(jpg|jpeg|png|gif|webp)$/i.test(file.name)
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImg) { ElMessage.error('只能上传图片格式！'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过 5MB！'); return false }
  // 本地预览
  avatarPreview.value = URL.createObjectURL(file)
  return true
}
const handleAvatarUpload = async ({ file }) => {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await userApi.uploadAvatar(fd)
    if (res && res.url) {
      profile.value.avatarUrl = res.url
      avatarPreview.value = ''
      ElMessage.success('头像已更新')
    }
  } catch (e) { avatarPreview.value = ''; console.error(e) }
}

// 工具
const fmt = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'
const realNameLabel = (s) => ({ 0: '未实名', 1: '审核中', 2: '已实名', 3: '审核失败' }[s] ?? '未知')
const realNameTagType = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] ?? 'info')

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { max-width: 900px; }
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0 4px;
}
.avatar-uploader { position: relative; cursor: pointer; }
.avatar-img { display: block; }
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-uploader:hover .avatar-mask { opacity: 1; }
.avatar-hint { font-size: 12px; color: #909399; }
.region-row { display: flex; gap: 10px; flex-wrap: wrap; }
.field-hint { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
</style>
