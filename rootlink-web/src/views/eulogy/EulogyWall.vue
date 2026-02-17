<template>
  <div class="eulogy-wall">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🕯️ 挽联墙</span>
          <span class="header-sub">共 {{ total }} 篇已发布挽联</span>
        </div>
      </template>

      <el-empty
        v-if="list.length === 0 && !loading"
        description="暂无挽联，亲属离世后可提交挽联"
        :image-size="120"
      />

      <div v-else v-loading="loading" class="eulogy-list">
        <div v-for="eulogy in list" :key="eulogy.id" class="eulogy-item">
          <div class="eulogy-header">
            <div class="eulogy-meta">
              <el-tag type="info" size="small">{{ eulogy.submitterUserName || '匿名' }}</el-tag>
              <span class="arrow">悼念</span>
              <el-tag type="danger" size="small">{{ eulogy.targetUserName || '已故亲人' }}</el-tag>
            </div>
            <span class="publish-time">{{ formatTime(eulogy.publishTime) }}</span>
          </div>
          <div class="eulogy-content">{{ eulogy.content }}</div>
          <div class="eulogy-footer">
            <el-button text size="small" @click="showDetail(eulogy.id)">查看审核详情</el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadList"
        style="margin-top: 20px; justify-content: center; display: flex"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="挽联详情" width="500px">
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="提交人">{{ detail.submitterUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="悼念对象">{{ detail.targetUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(detail.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="挽联内容">
            <div style="white-space: pre-wrap; line-height: 1.8">{{ detail.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.reviewers && detail.reviewers.length > 0" style="margin-top: 16px">
          <div style="font-size: 13px; color: #909399; margin-bottom: 8px">审核人列表（需全员通过）</div>
          <el-table :data="detail.reviewers" size="small" border>
            <el-table-column label="审核人" prop="reviewerUserName" />
            <el-table-column label="关系" prop="relationType" width="80">
              <template #default="{ row }">
                <span>{{ relationLabel(row.relationType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="reviewStatusType(row.reviewStatus)" size="small">
                  {{ reviewStatusLabel(row.reviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <div v-else v-loading="true" style="height: 100px" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { eulogyApi } from '@/api/eulogy'

const list = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    const res = await eulogyApi.getEulogyWall({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    list.value = res?.records || []
    total.value = res?.total || 0
  } catch (e) {
    console.error('加载挽联墙失败:', e)
  } finally {
    loading.value = false
  }
}

// 详情
const detailVisible = ref(false)
const detail = ref(null)

const showDetail = async (eulogyId) => {
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await eulogyApi.getEulogyDetail(eulogyId)
  } catch (e) {
    console.error(e)
  }
}

// 工具函数
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const relationLabel = (type) => {
  const map = { 1: '父母', 2: '父母', 3: '配偶', 4: '子女', 5: '祖父母', 6: '孙子女' }
  return map[type] || '亲属'
}

const reviewStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const reviewStatusLabel = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[s] || '未知')

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.eulogy-page { max-width: 900px; }
:deep(.el-card) { border-radius: var(--radius-md) !important; border: 1px solid var(--c-border) !important; box-shadow: var(--shadow-sm) !important; }
:deep(.el-card__header) { background: #F8FAFC; border-bottom: 1px solid var(--c-border); }
:deep(.el-input__wrapper) { border-radius: var(--radius-sm) !important; border: 1.5px solid var(--c-border) !important; box-shadow: none !important; transition: var(--transition) !important; }
:deep(.el-input__wrapper:hover) { border-color: var(--c-primary) !important; }
:deep(.el-input__wrapper.is-focus) { border-color: var(--c-primary) !important; box-shadow: 0 0 0 3px rgba(90,103,242,.1) !important; }
:deep(.el-textarea__inner) { border-radius: var(--radius-sm) !important; border: 1.5px solid var(--c-border) !important; box-shadow: none !important; resize: none; }
:deep(.el-tag) { border-radius: 6px !important; font-weight: 600; }
.wall-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.eulogy-card {
  background: var(--c-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);
  padding: 18px;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}
.eulogy-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--c-primary), var(--c-amber));
  opacity: 0;
  transition: opacity .2s;
}
.eulogy-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.eulogy-card:hover::before { opacity: 1; }

@media (max-width: 768px) {
  .eulogy-page { max-width: 100%; }
  :deep(.el-card__header) { padding: 10px 12px; }
  :deep(.el-card__body) { padding: 12px; }
  /* 搜索栏竖排 */
  :deep(.el-form--inline) { flex-direction: column; gap: 8px; }
  :deep(.el-form--inline .el-form-item) { width: 100%; margin-right: 0 !important; }
  :deep(.el-form--inline .el-input) { width: 100% !important; }
  :deep(.el-form--inline .el-select) { width: 100% !important; }
  /* 挽联墙：1列 */
  .wall-grid { grid-template-columns: 1fr; gap: 10px; }
  .eulogy-card { padding: 14px; }
  /* 表格 */
  :deep(.el-table) { font-size: 12px; }
  /* 按钮头 */
  :deep(.el-card__header .el-button) { padding: 6px 10px; font-size: 12px; }
}
@media (max-width: 480px) {
  .wall-grid { grid-template-columns: 1fr; }
}

</style>