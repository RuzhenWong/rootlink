<template>
  <div class="page">
    <el-card>
      <h2>提交挽联</h2>
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <p>挽联提交后需要直系两代亲属全员审核通过才能发布</p>
        <p>直系两代包括：父母、配偶、子女、祖父母、孙子女</p>
        <p><strong>一票否决：</strong>任何一人拒绝即整个挽联被拒绝</p>
      </el-alert>

      <el-form :model="form" label-width="100px">
        <el-form-item label="缅怀对象">
          <el-select v-model="form.targetUserId" placeholder="选择已离世的亲属">
            <el-option 
              v-for="r in deceasedRelatives" 
              :key="r.relatedUserId" 
              :label="r.relatedUserName" 
              :value="r.relatedUserId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="挽联内容">
          <el-input
            type="textarea"
            v-model="form.content"
            :rows="8"
            placeholder="请输入挽联内容，表达您的哀思..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submit">提交挽联</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { eulogyApi } from '@/api/eulogy'
import { relationApi } from '@/api/relation'

const router = useRouter()
const deceasedRelatives = ref([])

const form = reactive({
  targetUserId: null,
  content: '',
})

onMounted(async () => {
  try {
    const res = await relationApi.getMyRelations({ pageNum: 1, pageSize: 100 })
    // 筛选出已离世的亲属（life_status = 3）
    // 注意：实际需要从API返回life_status字段
    deceasedRelatives.value = res.records
  } catch (error) {
    console.error('加载亲属列表失败:', error)
  }
})

const submit = async () => {
  if (!form.targetUserId || !form.content) {
    ElMessage.error('请填写完整信息')
    return
  }

  try {
    await eulogyApi.submitEulogy(form)
    ElMessage.success('挽联已提交，等待直系两代亲属审核')
    router.back()
  } catch (error) {
    console.error('提交失败:', error)
  }
}
</script>
