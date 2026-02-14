<template>
  <div class="page">
    <el-card>
      <h2>挽联审核</h2>
      <el-alert type="warning" :closable="false" style="margin-bottom: 20px">
        <p><strong>全员100%通过机制：</strong>所有直系两代亲属都必须通过，挽联才能发布</p>
        <p><strong>一票否决：</strong>您拒绝后，整个挽联将被立即拒绝</p>
      </el-alert>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="缅怀对象" prop="targetUserName" width="120"/>
        <el-table-column label="提交人" prop="submitterUserName" width="120"/>
        <el-table-column label="挽联内容" prop="content" show-overflow-tooltip/>
        <el-table-column label="提交时间" prop="submitTime" width="180"/>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="review(row, 1)">通过</el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(row)">拒绝</el-button>
            <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 拒绝理由对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝挽联" width="500px">
      <el-input
        type="textarea"
        v-model="rejectOpinion"
        placeholder="请输入拒绝理由"
        :rows="4"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { eulogyApi } from '@/api/eulogy'

const list = ref([])
const loading = ref(false)
const rejectDialogVisible = ref(false)
const rejectOpinion = ref('')
const currentEulogy = ref(null)

onMounted(() => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    list.value = await eulogyApi.getMyPendingReviews()
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const review = async (row, status) => {
  try {
    await ElMessageBox.confirm('确认通过这条挽联吗？', '提示')
    await eulogyApi.reviewEulogy(row.id, { reviewStatus: status })
    ElMessage.success('审核成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核失败:', error)
    }
  }
}

const showRejectDialog = (row) => {
  currentEulogy.value = row
  rejectOpinion.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectOpinion.value) {
    ElMessage.error('请输入拒绝理由')
    return
  }

  try {
    await eulogyApi.reviewEulogy(currentEulogy.value.id, {
      reviewStatus: 2,
      reviewOpinion: rejectOpinion.value,
    })
    ElMessage.success('已拒绝（一票否决）')
    rejectDialogVisible.value = false
    loadList()
  } catch (error) {
    console.error('拒绝失败:', error)
  }
}

const viewDetail = async (row) => {
  try {
    const detail = await eulogyApi.getEulogyDetail(row.id)
    ElMessageBox.alert(
      `审核进度：已审核 ${detail.reviewers.filter(r => r.reviewStatus !== 0).length} / ${detail.reviewers.length} 人`,
      '挽联详情'
    )
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}
</script>
