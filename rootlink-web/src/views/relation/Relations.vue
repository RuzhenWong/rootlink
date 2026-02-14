<template>
  <div class="relation-page">
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">

      <!-- Tab1: 我的亲属 -->
      <el-tab-pane label="我的亲属" name="my">
        <div class="tab-header">
          <span class="tab-title">已确认的亲属（{{ relations.length }}）</span>
          <el-button type="primary" size="small" @click="activeTab = 'search'">+ 添加亲属</el-button>
        </div>
        <el-empty v-if="relations.length === 0 && !loadingRelations" description="暂无亲属，点右上角添加" />
        <el-table v-else v-loading="loadingRelations" :data="relations" stripe>
          <el-table-column label="姓名" width="110">
            <template #default="{ row }">
              <el-button text @click="openProfile(row.relatedUserId, row.relationDesc)">
                {{ row.realName || '未知' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="关系" width="130">
            <template #default="{ row }">
              <el-tag type="primary" size="small">{{ row.relationDesc }}</el-tag>
              <el-tag v-if="row.inferStatus === 2" type="success" size="small" style="margin-left:4px">推断</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生命状态" width="90">
            <template #default="{ row }">
              <el-tag :type="lifeTagType(row.lifeStatus)" size="small">{{ lifeLabel(row.lifeStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="确认时间" min-width="140">
            <template #default="{ row }">{{ fmt(row.confirmTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small"
                @click="openProfile(row.relatedUserId, row.relationDesc)">详情</el-button>
              <el-popconfirm title="确认解除该亲属关系？" @confirm="handleRemove(row)">
                <template #reference>
                  <el-button text type="danger" size="small">解除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Tab2: 搜索添加 -->
      <el-tab-pane label="搜索添加" name="search">
        <p class="search-tip"><el-icon><InfoFilled /></el-icon>通过身份证号精确搜索，对方需已实名且开启"允许被搜索"</p>
        <el-form inline @submit.prevent="handleSearch">
          <el-form-item label="身份证号">
            <el-input v-model="searchForm.idCard" placeholder="请输入对方身份证号" style="width:240px" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="searching" @click="handleSearch">搜索</el-button>
          </el-form-item>
        </el-form>
        <div v-if="searchResult" class="search-result">
          <el-card shadow="hover">
            <div class="result-info">
              <el-avatar :size="48" icon="UserFilled" />
              <div class="result-detail">
                <div class="result-name">{{ searchResult.realName }}</div>
                <div class="result-sub">{{ searchResult.phone }}</div>
                <el-tag size="small" :type="searchResult.realNameStatus === 2 ? 'success' : 'info'">
                  {{ searchResult.realNameStatus === 2 ? '已实名' : '未实名' }}</el-tag>
                <el-tag size="small" :type="lifeTagType(searchResult.lifeStatus)" style="margin-left:6px">
                  {{ lifeLabel(searchResult.lifeStatus) }}</el-tag>
              </div>
              <div>
                <el-tag v-if="searchResult.relationStatus === 'related'" type="success">已是亲属</el-tag>
                <el-tag v-else-if="searchResult.relationStatus === 'pending'" type="warning">申请中</el-tag>
                <el-button v-else type="primary" @click="openApplyDialog(searchResult)">发起申请</el-button>
              </div>
            </div>
          </el-card>
        </div>
        <el-empty v-else-if="searched" description="未找到该用户" />
      </el-tab-pane>

      <!-- Tab3: 待处理申请 -->
      <el-tab-pane name="pending">
        <template #label>
          待处理申请
          <el-badge v-if="pendingCount > 0" :value="pendingCount" style="margin-left:4px" />
        </template>
        <el-empty v-if="pendingApplies.length === 0 && !loadingPending" description="暂无待处理申请" />
        <div v-else class="apply-list">
          <el-card v-for="apply in pendingApplies" :key="apply.applyId" shadow="hover" class="apply-card">
            <div class="apply-row">
              <div class="apply-left">
                <el-avatar :size="40" icon="UserFilled" />
                <div>
                  <div class="apply-name">{{ apply.applicantName }}
                    <el-tag size="small" type="primary">{{ apply.relationDesc }}</el-tag>
                  </div>
                  <div class="apply-sub">{{ apply.applicantPhone }}</div>
                  <div v-if="apply.reason" class="apply-reason">留言：{{ apply.reason }}</div>
                  <div class="apply-time">{{ fmt(apply.createTime) }}</div>
                </div>
              </div>
              <div class="apply-btns">
                <el-button type="success" size="small" :loading="handlingId === apply.applyId"
                  @click="handleAccept(apply)">同意</el-button>
                <el-button type="danger" size="small" plain @click="openRejectDialog(apply)">拒绝</el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

    </el-tabs>

    <!-- ═══ 申请关系对话框 - 步骤链选择 ═══ -->
    <el-dialog v-model="applyDialogVisible" title="建立亲属关系" width="540px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="目标用户">
          <strong>{{ applyForm.targetName }}</strong>（{{ applyForm.targetPhone }}）
        </el-form-item>

        <el-form-item label="关系路径" required>
          <div class="chain-builder">
            <!-- 步骤展示 -->
            <div class="chain-steps">
              <el-tag v-for="(s, i) in chainSteps" :key="i" closable
                type="primary" size="large" @close="removeStep(i)"
                style="margin-right:4px; font-size:13px; cursor:default">
                {{ stepLabel(s) }}
              </el-tag>
              <span v-if="chainSteps.length === 0" class="chain-placeholder">请从下方选择关系步骤</span>
              <span v-if="chainSteps.length > 0" class="chain-arrow"> → </span>
              <el-tag v-if="chainSteps.length > 0" type="success" size="large"
                style="font-size:13px; font-weight:700">
                {{ computedRelationName || '？' }}
              </el-tag>
            </div>

            <!-- 可选步骤按钮 -->
            <div v-if="availableNext.length > 0" class="chain-options">
              <span class="options-label">{{ chainSteps.length === 0 ? '第一步：' : '继续选：' }}</span>
              <el-button v-for="opt in availableNext" :key="opt.value"
                size="small" :type="chainSteps.length === 0 ? 'primary' : 'default'"
                plain @click="addStep(opt.value)" style="margin:2px">
                {{ opt.label }}
              </el-button>
            </div>
            <div v-else-if="chainSteps.length > 0" class="chain-done">
              <el-icon color="#67c23a"><CircleCheck /></el-icon> 关系路径已完整，或点 ✕ 删除最后一步继续调整
            </div>

            <!-- 清空 -->
            <el-button v-if="chainSteps.length > 0" link type="info" size="small"
              style="margin-top:6px" @click="chainSteps = []">清空重选</el-button>
          </div>
        </el-form-item>

        <el-form-item label="关系描述">
          <el-alert v-if="chainSteps.length > 0" :title="chainToDescription(chainSteps)"
            type="info" :closable="false" show-icon />
        </el-form-item>

        <el-form-item label="申请留言">
          <el-input v-model="applyForm.reason" type="textarea" :rows="2"
            placeholder="可选，说明申请原因" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying"
          :disabled="!computedRelationName || computedRelationName === '亲属'"
          @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- ═══ 拒绝对话框 ═══ -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="400px">
      <el-input v-model="rejectReason" type="textarea" placeholder="可选填拒绝原因" :rows="3" maxlength="100" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="handling" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- ═══ 亲属详情抽屉 ═══ -->
    <RelativeProfileDrawer v-model="profileDrawerVisible"
      :userId="profileTargetId" :relationDesc="profileRelationDesc" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, CircleCheck } from '@element-plus/icons-vue'
import { relationApi } from '@/api/relation'
import { chainToName, chainToDescription, nextStepOptions } from '@/utils/relationChain'
import RelativeProfileDrawer from '@/components/RelativeProfileDrawer.vue'

const activeTab = ref('my')

// ── 我的亲属 ──────────────────────────────────────────
const relations = ref([])
const loadingRelations = ref(false)

const loadRelations = async () => {
  loadingRelations.value = true
  try { relations.value = await relationApi.getMyRelations() || [] }
  catch (e) { console.error(e) }
  finally { loadingRelations.value = false }
}

const handleRemove = async (row) => {
  try { await relationApi.removeRelation(row.relationId); ElMessage.success('已解除'); loadRelations() }
  catch (e) { console.error(e) }
}

// ── 亲属详情抽屉 ─────────────────────────────────────
const profileDrawerVisible = ref(false)
const profileTargetId = ref(null)
const profileRelationDesc = ref('')

const openProfile = (userId, relationDesc) => {
  profileTargetId.value = userId
  profileRelationDesc.value = relationDesc || ''
  profileDrawerVisible.value = true
}

// ── 搜索 ──────────────────────────────────────────────
const searchForm = ref({ idCard: '' })
const searchResult = ref(null)
const searched = ref(false)
const searching = ref(false)

const handleSearch = async () => {
  if (!searchForm.value.idCard.trim()) { ElMessage.warning('请输入身份证号'); return }
  searching.value = true; searched.value = false; searchResult.value = null
  try { searchResult.value = await relationApi.searchUser(searchForm.value.idCard); searched.value = true }
  catch (e) { searched.value = true }
  finally { searching.value = false }
}

// ── 步骤链构建器 ──────────────────────────────────────
const chainSteps = ref([])

const availableNext = computed(() => nextStepOptions(chainSteps.value))
const computedRelationName = computed(() => chainToName(chainSteps.value))

const stepLabelMap = { '父':'父亲','母':'母亲','子':'儿子','女':'女儿','配偶':'配偶','哥':'哥哥','弟':'弟弟','姐':'姐姐','妹':'妹妹' }
const stepLabel = v => stepLabelMap[v] || v

const addStep = (v) => { chainSteps.value = [...chainSteps.value, v] }
const removeStep = (i) => { chainSteps.value = chainSteps.value.filter((_, idx) => idx !== i) }

// ── 申请 ──────────────────────────────────────────────
const applyDialogVisible = ref(false)
const applying = ref(false)
const applyForm = ref({ targetUserId: null, targetName: '', targetPhone: '', reason: '' })

const openApplyDialog = (user) => {
  applyForm.value = { targetUserId: user.userId, targetName: user.realName, targetPhone: user.phone, reason: '' }
  chainSteps.value = []
  applyDialogVisible.value = true
}

const submitApply = async () => {
  if (!computedRelationName.value || computedRelationName.value === '亲属') {
    ElMessage.warning('请选择有效的关系路径'); return
  }
  applying.value = true
  try {
    await relationApi.applyRelation({
      targetUserId: applyForm.value.targetUserId,
      relationChain: JSON.stringify(chainSteps.value),
      reason: applyForm.value.reason,
    })
    ElMessage.success(`申请已发送，对方确认后建立【${computedRelationName.value}】关系`)
    applyDialogVisible.value = false
    if (searchResult.value) searchResult.value.relationStatus = 'pending'
  } catch (e) { console.error(e) }
  finally { applying.value = false }
}

// ── 待处理申请 ────────────────────────────────────────
const pendingApplies = ref([])
const pendingCount = computed(() => pendingApplies.value.length)
const loadingPending = ref(false)
const handlingId = ref(null)

const loadPendingApplies = async () => {
  loadingPending.value = true
  try { pendingApplies.value = await relationApi.getPendingApplies() || [] }
  catch (e) { console.error(e) }
  finally { loadingPending.value = false }
}

const handling = ref(false)

const handleAccept = async (apply) => {
  handlingId.value = apply.applyId
  try {
    await relationApi.handleApply(apply.applyId, { action: 1 })
    ElMessage.success('已同意，系统正在推断关联关系...')
    pendingApplies.value = pendingApplies.value.filter(a => a.applyId !== apply.applyId)
    loadRelations()
    setTimeout(() => loadRelations(), 2000)
  } catch (e) { ElMessage.error('操作失败'); console.error(e) }
  finally { handlingId.value = null }
}

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentApply = ref(null)

const openRejectDialog = (apply) => { currentApply.value = apply; rejectReason.value = ''; rejectDialogVisible.value = true }
const submitReject = async () => {
  handling.value = true
  try {
    await relationApi.handleApply(currentApply.value.applyId, { action: 2, rejectReason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    pendingApplies.value = pendingApplies.value.filter(a => a.applyId !== currentApply.value.applyId)
  } catch (e) { ElMessage.error('操作失败'); console.error(e) }
  finally { handling.value = false }
}

// ── Tab 切换 + 工具 ────────────────────────────────────
const onTabChange = (name) => {
  if (name === 'my') loadRelations()
  if (name === 'pending') loadPendingApplies()
}

const lifeTagType = s => ({0:'success',1:'info',2:'warning',3:'danger'})[s] ?? 'info'
const lifeLabel   = s => ({0:'活跃',1:'不活跃',2:'疑似离世',3:'已离世'})[s] ?? '未知'
const fmt = t => t ? new Date(t).toLocaleString('zh-CN') : '-'

onMounted(() => { loadRelations(); loadPendingApplies() })
</script>

<style scoped>
.relation-page { padding: 0; }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 14px; color: #606266; }
.search-tip { color: #909399; font-size: 13px; margin-bottom: 14px; display: flex; align-items: center; gap: 6px; }
.search-result { max-width: 500px; margin-top: 14px; }
.result-info { display: flex; align-items: center; gap: 14px; }
.result-detail { flex: 1; }
.result-name { font-size: 16px; font-weight: 600; }
.result-sub { font-size: 13px; color: #909399; margin: 2px 0; }

.apply-list { display: flex; flex-direction: column; gap: 10px; }
.apply-card { cursor: default; }
.apply-row { display: flex; justify-content: space-between; align-items: center; }
.apply-left { display: flex; gap: 12px; align-items: flex-start; }
.apply-name { font-size: 14px; font-weight: 600; }
.apply-sub { font-size: 12px; color: #909399; }
.apply-reason { font-size: 12px; color: #606266; }
.apply-time { font-size: 11px; color: #c0c4cc; }
.apply-btns { display: flex; gap: 8px; }

/* 步骤链构建器 */
.chain-builder { display: flex; flex-direction: column; gap: 10px; }
.chain-steps {
  display: flex; flex-wrap: wrap; align-items: center; gap: 4px;
  min-height: 36px; padding: 6px 10px;
  background: #f8f9fc; border-radius: 8px; border: 1px dashed #dcdfe6;
}
.chain-placeholder { color: #c0c4cc; font-size: 13px; }
.chain-arrow { color: #909399; font-weight: 700; margin: 0 4px; }
.chain-options { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.options-label { font-size: 12px; color: #909399; margin-right: 4px; }
.chain-done { font-size: 13px; color: #67c23a; display: flex; align-items: center; gap: 4px; }
</style>
