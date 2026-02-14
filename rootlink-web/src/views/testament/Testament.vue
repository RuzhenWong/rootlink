<template>
  <div class="testament-page">
    <el-alert type="info" :closable="false" style="margin-bottom:16px">
      <template #title>
        📜 生前遗言将在您的亲属确认您离世后自动解锁。文字遗言所有亲属可见；给特定人的信仅指定人可见；财产分配可选私密或公开。
      </template>
    </el-alert>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的遗言（{{ list.length }} 封）</span>
          <el-button type="primary" @click="openEditor(null)">+ 新建遗言</el-button>
        </div>
      </template>

      <el-empty v-if="list.length === 0 && !loading" description="还没有遗言，点击右上角新建" />

      <div v-else v-loading="loading" class="testament-list">
        <el-card
          v-for="item in list"
          :key="item.id"
          class="testament-item"
          shadow="hover"
        >
          <div class="item-header">
            <div class="item-title">
              <el-icon><Document /></el-icon>
              <span>{{ item.title }}</span>
              <el-tag size="small" :type="item.unlockStatus === 1 ? 'danger' : 'info'" style="margin-left:8px">
                {{ item.unlockStatus === 1 ? '已解锁' : '锁定中' }}
              </el-tag>
              <el-tag size="small" type="primary" style="margin-left:4px">{{ typeLabel(item.testamentType) }}</el-tag>
              <el-tag size="small" :type="visibilityTagType(item.visibility)" style="margin-left:4px">
                {{ visibilityLabel(item.visibility, item.testamentType) }}
              </el-tag>
            </div>
            <div class="item-actions">
              <el-button text type="primary" @click="openEditor(item)">编辑</el-button>
              <el-button text type="info" @click="openView(item)">预览</el-button>
              <el-popconfirm title="确认删除该遗言？删除后无法恢复。" @confirm="handleDelete(item.id)">
                <template #reference>
                  <el-button text type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
          <div class="item-preview">{{ preview(item.contentEncrypted) }}</div>
          <div class="item-time">创建于 {{ formatTime(item.createTime) }}</div>
        </el-card>
      </div>
    </el-card>

    <!-- ======== 编辑器 Drawer ======== -->
    <el-drawer
      v-model="editorVisible"
      :title="editingId ? '编辑遗言' : '新建遗言'"
      size="680px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px" style="padding:0 8px">

        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="给这封遗言起个名字" maxlength="50" show-word-limit />
        </el-form-item>

        <!-- 遗言类型 -->
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.testamentType" @change="onTypeChange">
            <el-radio-button :value="1">文字遗言</el-radio-button>
            <el-radio-button :value="2">给特定人的信</el-radio-button>
            <el-radio-button :value="3">财产分配</el-radio-button>
            <el-radio-button :value="4">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- ---- 类型2: 指定接收人 ---- -->
        <el-form-item v-if="form.testamentType === 2" label="接收人" required>
          <div v-if="loadingRelations" class="loading-tip">加载亲属列表...</div>
          <el-checkbox-group v-else v-model="form.receiverIds">
            <div class="receiver-list">
              <el-checkbox
                v-for="rel in myRelations"
                :key="rel.relatedUserId"
                :value="rel.relatedUserId"
                class="receiver-item"
              >
                <span class="receiver-name">{{ rel.realName }}</span>
                <el-tag size="small" type="primary" style="margin-left:6px">{{ rel.relationDesc }}</el-tag>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <div class="field-tip">解锁后仅勾选的人可查看此信</div>
        </el-form-item>

        <!-- ---- 类型3: 财产分配隐私 ---- -->
        <el-form-item v-if="form.testamentType === 3" label="公开范围">
          <el-radio-group v-model="form.visibility">
            <el-radio :value="2">
              <span>私密分配</span>
              <span class="radio-hint">（仅直系两代内可见：爷爷奶奶↔孙子孙女）</span>
            </el-radio>
            <el-radio :value="3">
              <span>公开分配</span>
              <span class="radio-hint">（所有人可查看，适合包含公益捐赠内容）</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- ---- 遗言正文 ---- -->
        <el-form-item label="正文" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="16"
            :placeholder="contentPlaceholder"
            maxlength="10000"
            show-word-limit
            style="font-size:15px;line-height:1.8"
          />
        </el-form-item>

        <!-- 说明 -->
        <el-form-item>
          <el-alert :title="visibilityTip" type="warning" :closable="false" show-icon />
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display:flex;justify-content:flex-end;gap:12px">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            {{ editingId ? '保存修改' : '创建遗言' }}
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 预览 Dialog -->
    <el-dialog v-model="viewVisible" :title="viewItem?.title" width="600px" top="8vh">
      <div class="view-content">{{ viewItem?.contentEncrypted }}</div>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
        <el-button type="primary" @click="openEditor(viewItem); viewVisible = false">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { testamentApi } from '@/api/testament'
import { relationApi } from '@/api/relation'

const list = ref([])
const loading = ref(false)
const myRelations = ref([])
const loadingRelations = ref(false)

const loadList = async () => {
  loading.value = true
  try { list.value = await testamentApi.getMyList() || [] }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

const loadRelations = async () => {
  loadingRelations.value = true
  try { myRelations.value = await relationApi.getMyRelations() || [] }
  catch (e) { console.error(e) }
  finally { loadingRelations.value = false }
}

// ---- 编辑器 ----
const editorVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const form = ref({ title: '', content: '', testamentType: 1, visibility: 0, receiverIds: [] })

const onTypeChange = (type) => {
  // 自动设置默认 visibility
  if (type === 1 || type === 4) form.value.visibility = 0  // 仅自己（锁定状态），解锁后亲属可见
  if (type === 2) form.value.visibility = 1               // 指定人
  if (type === 3) form.value.visibility = 2               // 默认私密
}

const contentPlaceholder = computed(() => {
  const map = {
    1: '在这里写下您想留给所有亲属的话...',
    2: '在这里写下您想单独告诉这些人的话...',
    3: '在这里描述您的财产分配意愿...',
    4: '在这里写下您想记录的内容...',
  }
  return map[form.value.testamentType] || '请输入内容...'
})

const visibilityTip = computed(() => {
  const t = form.value.testamentType
  if (t === 1) return '📢 文字遗言解锁后对所有已确认的亲属可见'
  if (t === 2) return '🔒 给特定人的信解锁后仅您勾选的亲属可见'
  if (t === 3 && form.value.visibility === 2) return '🔒 私密财产分配仅直系两代内亲属可见（父母/祖父母/子女/孙子女）'
  if (t === 3 && form.value.visibility === 3) return '🌐 公开财产分配解锁后所有人可查看（适合捐赠等公益内容）'
  return '📝 内容锁定中，您离世确认后解锁'
})

const openEditor = (item) => {
  if (item) {
    editingId.value = item.id
    form.value = {
      title: item.title,
      content: item.contentEncrypted,
      testamentType: item.testamentType || 1,
      visibility: item.visibility ?? 0,
      receiverIds: [],
    }
  } else {
    editingId.value = null
    form.value = { title: '', content: '', testamentType: 1, visibility: 0, receiverIds: [] }
  }
  loadRelations()
  editorVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入遗言内容'); return }
  if (form.value.testamentType === 2 && form.value.receiverIds.length === 0) {
    ElMessage.warning('给特定人的信，请至少选择一位接收人'); return
  }
  saving.value = true
  try {
    const payload = {
      title: form.value.title,
      content: form.value.content,
      testamentType: form.value.testamentType,
      visibility: form.value.visibility,
      receiverIds: form.value.receiverIds,
    }
    if (editingId.value) {
      await testamentApi.update(editingId.value, payload)
      ElMessage.success('修改已保存')
    } else {
      await testamentApi.create(payload)
      ElMessage.success('遗言已创建')
    }
    editorVisible.value = false
    loadList()
  } catch (e) { console.error(e) }
  finally { saving.value = false }
}

// ---- 删除 ----
const handleDelete = async (id) => {
  try { await testamentApi.delete(id); ElMessage.success('已删除'); loadList() }
  catch (e) { console.error(e) }
}

// ---- 预览 ----
const viewVisible = ref(false)
const viewItem = ref(null)
const openView = (item) => { viewItem.value = item; viewVisible.value = true }

// ---- 工具 ----
const typeLabel = (t) => ({ 1: '文字遗言', 2: '给特定人', 3: '财产分配', 4: '其他' })[t] || '遗言'
const visibilityLabel = (v, t) => {
  if (t === 1) return '亲属可见'
  if (t === 2) return '指定人可见'
  if (t === 3 && v === 3) return '公开'
  if (t === 3) return '直系私密'
  return '锁定'
}
const visibilityTagType = (v) => ({ 0: 'info', 1: 'warning', 2: 'danger', 3: 'success' })[v] || 'info'
const preview = (s) => s ? (s.length > 80 ? s.slice(0, 80) + '...' : s) : ''
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'

onMounted(loadList)
</script>

<style scoped>
.testament-page { max-width: 900px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.testament-list { display: flex; flex-direction: column; gap: 14px; }
.testament-item { cursor: default; }
.item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.item-title { display: flex; align-items: center; gap: 6px; font-size: 15px; font-weight: 600; }
.item-actions { display: flex; gap: 4px; }
.item-preview {
  font-size: 13px; color: #606266; line-height: 1.7;
  white-space: pre-wrap; padding: 8px 0;
  border-top: 1px dashed #eee; border-bottom: 1px dashed #eee; min-height: 36px;
}
.item-time { font-size: 12px; color: #c0c4cc; margin-top: 8px; }
.receiver-list { display: flex; flex-direction: column; gap: 6px; padding: 4px 0; }
.receiver-item { display: flex; align-items: center; }
.receiver-name { font-weight: 500; }
.field-tip { font-size: 12px; color: #909399; margin-top: 4px; }
.loading-tip { font-size: 13px; color: #909399; padding: 8px 0; }
.radio-hint { font-size: 12px; color: #909399; }
.view-content {
  font-size: 15px; line-height: 1.9; white-space: pre-wrap; color: #333;
  padding: 12px; background: #fafafa; border-radius: 6px;
  max-height: 60vh; overflow-y: auto;
}
</style>
