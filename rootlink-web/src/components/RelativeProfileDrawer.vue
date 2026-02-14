<template>
  <el-drawer v-model="visible" :title="profile?.realName || '亲属详情'"
    size="360px" direction="rtl" destroy-on-close>

    <div v-if="loading" v-loading="true" style="height:300px" />

    <div v-else-if="profile" class="drawer-wrap">
      <!-- 头像+基本信息 -->
      <div class="drawer-top">
        <el-avatar :size="68" :src="profile.avatarUrl || ''"
          :icon="!profile.avatarUrl ? 'UserFilled' : undefined"
          style="flex-shrink:0" />
        <div>
          <div class="dr-name">{{ profile.realName || '用户' + profile.userId }}</div>
          <div class="dr-tags">
            <el-tag v-if="relationDesc" type="primary" size="small">{{ relationDesc }}</el-tag>
            <el-tag :type="lifeTagType(profile.lifeStatus)" size="small">{{ lifeLabel(profile.lifeStatus) }}</el-tag>
            <el-tag type="info" size="small">{{ genderLabel(profile.gender) }}</el-tag>
          </div>
        </div>
      </div>

      <el-divider style="margin:12px 0" />

      <!-- 基本资料 -->
      <el-descriptions :column="1" border size="small" class="desc-block">
        <el-descriptions-item v-if="profile.birthDate" label="出生">{{ profile.birthDate?.slice(0,10) }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.deathDate" label="离世">{{ profile.deathDate?.slice(0,10) }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.nation" label="民族">{{ profile.nation }}</el-descriptions-item>
        <el-descriptions-item v-if="addr" label="地区">{{ addr }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.workUnit" label="单位">{{ profile.workUnit }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.position" label="职位">{{ profile.position }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.education" label="学历">{{ eduLabel(profile.education) }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.maritalStatus" label="婚况">{{ maritalLabel(profile.maritalStatus) }}</el-descriptions-item>
        <el-descriptions-item v-if="profile.bio" label="简介">{{ profile.bio }}</el-descriptions-item>
      </el-descriptions>

      <!-- 联系方式 -->
      <template v-if="profile.phone || profile.wechat || profile.qq">
        <el-divider content-position="left" style="margin:14px 0 8px;font-size:12px">联系方式</el-divider>
        <el-descriptions :column="1" border size="small" class="desc-block">
          <el-descriptions-item v-if="profile.phone" label="手机">{{ profile.phone }}</el-descriptions-item>
          <el-descriptions-item v-if="profile.wechat" label="微信">{{ profile.wechat }}</el-descriptions-item>
          <el-descriptions-item v-if="profile.qq" label="QQ">{{ profile.qq }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <div v-if="isEmpty" class="empty-tip">该用户暂未填写详细资料</div>
    </div>

    <el-empty v-else-if="!loading" description="加载失败，请重试" />
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { userApi } from '@/api/user'

const props = defineProps({
  modelValue: Boolean,
  userId: [Number, String, null],
  relationDesc: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v),
})

const loading = ref(false)
const profile = ref(null)

watch([() => props.modelValue, () => props.userId], async ([open, uid]) => {
  if (!open || !uid) return
  loading.value = true
  profile.value = null
  try { profile.value = await userApi.getRelativeProfile(uid) }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}, { immediate: true })

const addr = computed(() => {
  if (!profile.value) return ''
  return [profile.value.nativePlace, profile.value.province, profile.value.city, profile.value.district]
    .filter(Boolean).join(' ')
})

const isEmpty = computed(() => {
  if (!profile.value) return false
  const p = profile.value
  return !p.birthDate && !p.nation && !p.workUnit && !p.bio && !addr.value && !p.phone && !p.wechat
})

const lifeTagType = s => ({0:'success',1:'info',2:'warning',3:'danger'})[s] ?? 'info'
const lifeLabel   = s => ({0:'活跃',1:'不活跃',2:'疑似离世',3:'已离世'})[s] ?? '未知'
const genderLabel = g => ({1:'男',2:'女'})[g] ?? '性别未知'
const eduLabel    = e => ({1:'小学',2:'初中',3:'高中',4:'大专',5:'本科',6:'硕士',7:'博士'})[e] ?? ''
const maritalLabel= m => ({1:'未婚',2:'已婚',3:'离异',4:'丧偶'})[m] ?? ''
</script>

<style scoped>
.drawer-wrap { padding: 4px 0 20px; }
.drawer-top  { display: flex; align-items: center; gap: 14px; }
.dr-name     { font-size: 17px; font-weight: 700; margin-bottom: 6px; }
.dr-tags     { display: flex; flex-wrap: wrap; gap: 5px; }
.desc-block  { margin-top: 4px; }
.empty-tip   { text-align: center; color: #c0c4cc; font-size: 13px; margin-top: 24px; }
</style>
