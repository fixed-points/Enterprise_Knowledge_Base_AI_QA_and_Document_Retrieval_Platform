<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h3>问答记录</h3>
        <p>记录用户问题、平台回答、命中来源以及用户评价，用于效果追踪和迭代优化。</p>
      </div>
      <el-button plain @click="loadRecords">刷新记录</el-button>
    </div>

    <el-table :data="records" stripe class="table">
      <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="answer" label="回答" min-width="300" show-overflow-tooltip />
      <el-table-column label="来源摘要" min-width="280">
        <template #default="{ row }">
          <span>{{ formatSourceSummary(row.sourceSummary) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
      <el-table-column prop="createdAt" label="时间" min-width="180" />
      <el-table-column label="反馈" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openFeedback(row)">评价</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="feedbackVisible" width="520px" title="提交反馈">
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" placeholder="例如：命中片段准确，但答案可再精炼" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCurrentFeedback">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecords, submitFeedback } from '../api/client'

const emit = defineEmits(['changed'])

const records = ref([])
const feedbackVisible = ref(false)
const currentRecordId = ref(null)
const feedbackForm = reactive({
  rating: 5,
  comment: ''
})

const loadRecords = async () => {
  try {
    const response = await getRecords()
    records.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const formatSourceSummary = (raw) => {
  try {
    const parsed = JSON.parse(raw || '[]')
    return parsed.map((item) => item.documentTitle).filter(Boolean).slice(0, 3).join('、') || '-'
  } catch {
    return '-'
  }
}

const openFeedback = (row) => {
  currentRecordId.value = row.id
  feedbackForm.rating = 5
  feedbackForm.comment = ''
  feedbackVisible.value = true
}

const submitCurrentFeedback = async () => {
  try {
    await submitFeedback({
      qaRecordId: currentRecordId.value,
      rating: feedbackForm.rating,
      comment: feedbackForm.comment
    })
    feedbackVisible.value = false
    ElMessage.success('反馈提交成功')
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadRecords)
</script>

<style scoped>
.panel {
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(17, 33, 58, 0.08);
  box-shadow: 0 20px 50px rgba(17, 33, 58, 0.08);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.panel-header h3 {
  margin: 0;
  font-size: 26px;
}

.panel-header p {
  margin: 10px 0 0;
  color: #5e7388;
}

.table {
  margin-top: 22px;
}
</style>
