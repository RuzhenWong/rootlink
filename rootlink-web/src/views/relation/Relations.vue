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
          <el-table-column label="确认时间" min-width="140" class-name="col-confirm-time">
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
        <p class="search-tip"><el-icon><InfoFilled /></el-icon>通过身份证号精确搜索，对方需已实名且开启「允许被搜索」</p>
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
                  <!-- 清晰展示：谁申请、成为我的什么、以及对方视角 -->
                  <div class="apply-name">
                    <span class="apply-who">{{ apply.applicantName }}</span>
                    申请成为您的
                    <el-tag size="small" type="primary" style="vertical-align:middle">
                      {{ apply.myRoleDesc }}
                    </el-tag>
                  </div>
                  <div class="apply-sub-detail">
                    <span class="apply-sub">{{ apply.applicantPhone }}</span>
                    <!-- apply.relationDesc = 申请人对您的称谓（A叫您什么）
                         apply.myRoleDesc   = 您对申请人的称谓（您叫A什么） -->
                    <span class="apply-self-view">（TA 将称您为「{{ apply.relationDesc }}」）</span>
                  </div>
                  <div v-if="apply.reason" class="apply-reason">💬 {{ apply.reason }}</div>
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

    <!-- ═══ 申请关系对话框 ═══ -->
    <el-dialog v-model="applyDialogVisible" title="建立亲属关系" width="560px" :close-on-click-modal="false">
      <div class="apply-guide">
        <el-icon color="#5A67F2"><InfoFilled /></el-icon>
        <span>请从「我」的视角选择：<strong>对方（{{ applyForm.targetName }}）是我的什么人</strong></span>
      </div>

      <el-form label-width="90px" style="margin-top:16px">
        <el-form-item label="对方信息">
          <strong>{{ applyForm.targetName }}</strong>
          <span style="color:var(--c-txt-s);margin-left:6px">{{ applyForm.targetPhone }}</span>
        </el-form-item>

        <el-form-item label="关系路径" required>
          <div class="chain-builder">
            <!-- 步骤展示 -->
            <div class="chain-steps">
              <el-tag v-for="(s, i) in chainSteps" :key="i" closable
                type="primary" size="large" @close="removeStep(i)"
                style="margin-right:4px; font-size:13px">
                {{ stepLabel(s) }}
              </el-tag>
              <span v-if="chainSteps.length === 0" class="chain-placeholder">请从下方点击选择</span>
            </div>

            <!-- 可选步骤按钮 -->
            <div v-if="availableNext.length > 0" class="chain-options">
              <span class="options-label">{{ chainSteps.length === 0 ? '选择第一步：' : '继续选：' }}</span>
              <el-button v-for="opt in availableNext" :key="opt.value"
                size="small" type="primary" plain
                @click="addStep(opt.value)" style="margin:2px">
                {{ opt.label }}
              </el-button>
            </div>
            <div v-else-if="chainSteps.length > 0" class="chain-done">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              路径已完整，点 ✕ 可删除最后一步
            </div>

            <el-button v-if="chainSteps.length > 0" link type="info" size="small"
              style="margin-top:6px" @click="chainSteps = []">清空重选</el-button>
          </div>
        </el-form-item>

        <!-- 关系确认（双视角展示） -->
        <el-form-item v-if="chainSteps.length > 0" label="关系确认">
          <div class="relation-confirm">
            <div class="confirm-row">
              <span class="confirm-label">我的视角：</span>
              <span>对方（{{ applyForm.targetName }}）是我的
                <strong class="confirm-name">「{{ computedRelationName || '？' }}」</strong>
              </span>
            </div>
            <div v-if="computedReverseDesc" class="confirm-row confirm-reverse">
              <span class="confirm-label">对方视角：</span>
              <span>申请通过后，对方会看到「我是 TA 的
                <strong class="confirm-name-rev">{{ computedReverseDesc }}</strong>」
              </span>
            </div>
          </div>
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
          @click="submitApply">
          确认申请（{{ computedRelationName || '请先选择关系' }}）
        </el-button>
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

// 对方视角的关系描述（我是对方的什么）
// 前端粗略反转，仅用于展示预览；实际存储时后端用性别做精确反转
const computedReverseDesc = computed(() => {
  if (!chainSteps.value.length) return ''
  const name = computedRelationName.value
  if (!name || name === '亲属') return ''
  // 逆向称谓：「对方看到的我是 TA 的什么」
  const reverseMap = {
    // 直系
    '父亲': '儿子/女儿', '母亲': '儿子/女儿',
    '儿子': '父亲/母亲', '女儿': '父亲/母亲',
    // 祖辈
    '爷爷': '孙子/孙女', '奶奶': '孙子/孙女',
    '外公': '外孙/外孙女', '外婆': '外孙/外孙女',
    '太爷爷': '重孙子/重孙女', '太奶奶': '重孙子/重孙女',
    '太外公': '重孙子/重孙女', '太外婆': '重孙子/重孙女',
    // 孙辈
    '孙子': '爷爷/奶奶', '孙女': '爷爷/奶奶',
    '外孙': '外公/外婆', '外孙女': '外公/外婆',
    '重孙子': '太爷爷/太奶奶', '重孙女': '太爷爷/太奶奶',
    // 配偶
    '配偶': '配偶',
    // 兄弟姐妹
    '哥哥': '弟弟/妹妹', '姐姐': '弟弟/妹妹',
    '弟弟': '哥哥/姐姐', '妹妹': '哥哥/姐姐',
    // 父辈旁系
    '伯父': '侄子/侄女', '叔叔': '侄子/侄女',
    '姑姑': '侄子/侄女', '姑父': '侄媳/外甥媳',
    '舅舅': '外甥/外甥女', '舅妈': '外甥/外甥女',
    '姨妈': '外甥/外甥女', '姨父': '外甥/外甥女',
    '伯母': '侄子/侄女', '婶婶': '侄子/侄女',
    // 堂/表
    '堂兄': '堂弟/堂妹', '堂弟': '堂兄/堂姐',
    '堂姐': '堂弟/堂妹', '堂妹': '堂兄/堂姐',
    '表哥': '表弟/表妹', '表弟': '表哥/表姐',
    '表姐': '表弟/表妹', '表妹': '表哥/表姐',
    // 兄弟姐妹的配偶
    '嫂子': '小叔子/小姑子', '弟媳': '大伯子/小姑子',
    '姐夫': '妻弟/妻妹', '妹夫': '妻兄/妻姐',
    // 子女的配偶
    '儿媳': '公公/婆婆', '女婿': '岳父/岳母',
    // 配偶的父母（2步路径的关键修复）
    '岳父/公公': '儿婿/儿媳', '岳母/婆婆': '儿婿/儿媳',
    '岳父': '女婿/儿媳', '公公': '儿媳/女婿',
    '岳母': '女婿', '婆婆': '儿媳',
    // 配偶的兄弟姐妹
    '大舅子/大伯子': '妹妹/妻妹', '小舅子/小叔子': '妹妹/妻妹',
    '大姨子/大姑子': '兄弟/妻兄', '小姨子/小姑子': '兄弟/妻兄',
    '大舅子': '妻妹', '小舅子': '妻妹',
    '大姨子': '妻兄', '小姨子': '妻兄',
    '大伯子': '弟媳', '小叔子': '弟媳',
    '大姑子': '弟弟', '小姑子': '弟弟',
    // 继父母
    '继父': '继子/继女', '继母': '继子/继女',
    // 继子女
    '继子': '继父/继母', '继女': '继父/继母',
    // 孙媳孙婿
    '孙媳': '祖父/祖母', '孙女婿': '祖父/祖母',
    // 亲家
    '亲家公': '亲家公/亲家母', '亲家母': '亲家公/亲家母',
    // 表叔表姑
    '表叔': '表侄/表侄女', '表姑': '表侄/表侄女',
    // 侄孙辈
    '侄子': '伯父/叔叔/姑父（女性视角：伯母/婶婶/姑姑）', '侄女': '伯父/叔叔',
    '外甥': '舅舅/姨妈', '外甥女': '舅舅/姨妈',
  }
  return reverseMap[name] || null
})

const stepLabelMap = {
  '父': '父亲', '母': '母亲', '子': '儿子', '女': '女儿',
  '配偶': '配偶', '哥': '哥哥', '弟': '弟弟', '姐': '姐姐', '妹': '妹妹',
}
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
    ElMessage.success(`申请已发送！对方确认后，TA 将成为您的「${computedRelationName.value}」`)
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
    ElMessage.success('已同意！系统正在推断关联关系...')
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

const lifeTagType = s => ({ 0: 'success', 1: 'info', 2: 'warning', 3: 'danger' })[s] ?? 'info'
const lifeLabel   = s => ({ 0: '活跃', 1: '不活跃', 2: '疑似离世', 3: '已离世' })[s] ?? '未知'
const fmt = t => t ? new Date(t).toLocaleString('zh-CN') : '-'

onMounted(() => { loadRelations(); loadPendingApplies() })
</script>

<style scoped>
.relation-page { max-width: 900px; }

/* ── Tab ── */
:deep(.el-tabs--border-card) {
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--c-border) !important;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
:deep(.el-tabs__header) { background: #F8FAFC !important; border-bottom: 1px solid var(--c-border) !important; }
:deep(.el-tabs__item) { font-weight: 600; color: var(--c-txt-s) !important; }
:deep(.el-tabs__item.is-active) { color: var(--c-primary) !important; background: #fff !important; }

.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.tab-title { font-size: 13px; font-weight: 700; color: var(--c-txt-s); text-transform: uppercase; letter-spacing: .5px; }

.search-tip {
  color: var(--c-txt-s); font-size: 13px; margin-bottom: 16px;
  display: flex; align-items: center; gap: 6px;
  background: rgba(90,103,242,.06); padding: 10px 14px;
  border-radius: var(--radius-sm); border-left: 3px solid var(--c-primary);
}
.search-result { max-width: 500px; margin-top: 14px; }
.result-info { display: flex; align-items: center; gap: 14px; }
.result-detail { flex: 1; }
.result-name { font-size: 16px; font-weight: 700; color: var(--c-txt); }
.result-sub { font-size: 12px; color: var(--c-txt-s); margin: 3px 0; }

/* ── 申请列表 ── */
.apply-list { display: flex; flex-direction: column; gap: 10px; }
.apply-card { border: 1px solid var(--c-border) !important; border-radius: var(--radius-md) !important; }
.apply-row { display: flex; justify-content: space-between; align-items: center; }
.apply-left { display: flex; gap: 12px; align-items: flex-start; }
.apply-name {
  font-size: 14px; font-weight: 600; color: var(--c-txt);
  margin-bottom: 4px; display: flex; align-items: center; gap: 5px; flex-wrap: wrap;
}
.apply-who { color: var(--c-primary); font-weight: 700; }
.apply-sub-detail { display: flex; align-items: center; gap: 8px; margin: 2px 0; }
.apply-sub { font-size: 12px; color: var(--c-txt-s); }
.apply-self-view { font-size: 11px; color: var(--c-txt-i); }
.apply-reason { font-size: 12px; color: var(--c-txt-s); margin-top: 2px; }
.apply-time { font-size: 11px; color: var(--c-txt-i); margin-top: 3px; }
.apply-btns { display: flex; gap: 8px; flex-shrink: 0; }

/* ── 申请对话框 ── */
.apply-guide {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: var(--radius-sm);
  background: rgba(90,103,242,.06); border-left: 3px solid var(--c-primary);
  font-size: 13px; color: var(--c-txt-s);
}
.apply-guide strong { color: var(--c-primary); }

/* ── 步骤链构建器 ── */
.chain-builder { display: flex; flex-direction: column; gap: 10px; }
.chain-steps {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  min-height: 40px; padding: 8px 12px;
  background: #F8FAFC; border-radius: var(--radius-sm);
  border: 1.5px dashed var(--c-border); transition: border-color .2s;
}
.chain-placeholder { color: var(--c-txt-i); font-size: 13px; }
.chain-options { display: flex; flex-wrap: wrap; align-items: center; gap: 5px; }
.options-label { font-size: 12px; color: var(--c-txt-s); margin-right: 2px; font-weight: 600; }
.chain-done { font-size: 13px; color: var(--c-success); display: flex; align-items: center; gap: 5px; font-weight: 600; }

/* ── 关系确认 ── */
.relation-confirm {
  background: #F0F4FF; border: 1px solid rgba(90,103,242,.2);
  border-radius: var(--radius-sm); padding: 12px 14px;
  display: flex; flex-direction: column; gap: 6px;
}
.confirm-row { font-size: 13px; color: var(--c-txt); display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.confirm-reverse { color: var(--c-txt-s); }
.confirm-label { font-size: 12px; color: var(--c-txt-i); min-width: 68px; }
.confirm-name { color: var(--c-primary); font-size: 15px; }
.confirm-name-rev { color: var(--c-success, #67c23a); }

/* ── 响应式 ── */
@media (max-width: 768px) {
  .relation-page { max-width: 100%; }
  :deep(.el-tabs__nav) { flex-wrap: nowrap; }
  :deep(.el-tabs__item) { font-size: 12px !important; padding: 0 10px !important; }
  :deep(.el-table) { font-size: 13px; }
  .tab-header { flex-direction: column; align-items: flex-start; gap: 10px; }
  :deep(.el-form--inline .el-form-item) { flex-direction: column; width: 100%; }
  :deep(.el-form--inline .el-input) { width: 100% !important; }
  .search-result { max-width: 100%; }
  .result-info { flex-wrap: wrap; gap: 10px; }
  .apply-row { flex-direction: column; gap: 12px; align-items: flex-start; }
  .apply-btns { width: 100%; }
  .apply-btns .el-button { flex: 1; }
  :deep(.el-dialog) {
    margin: 0 !important; width: 100% !important;
    border-radius: 16px 16px 0 0 !important;
    position: fixed !important; bottom: 0 !important;
  }
  :deep(.el-dialog__body) { max-height: 70vh; overflow-y: auto; }
}
@media (max-width: 640px) {
  :deep(.col-confirm-time) { display: none !important; }
  :deep(.col-confirm-time *) { display: none !important; }
}
</style>
