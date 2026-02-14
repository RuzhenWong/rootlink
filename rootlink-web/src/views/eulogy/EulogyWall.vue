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
.eulogy-wall {
  max-width: 800px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
.header-sub {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}
.eulogy-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 4px;
}
.eulogy-item {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  transition: box-shadow 0.2s;
}
.eulogy-item:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.eulogy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.eulogy-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.arrow {
  font-size: 12px;
  color: #909399;
}
.publish-time {
  font-size: 12px;
  color: #c0c4cc;
}
.eulogy-content {
  font-size: 15px;
  line-height: 1.9;
  color: #333;
  white-space: pre-wrap;
  padding: 0 4px;
  border-left: 3px solid #ddd;
  padding-left: 14px;
}
.eulogy-footer {
  margin-top: 12px;
  text-align: right;
}
.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
