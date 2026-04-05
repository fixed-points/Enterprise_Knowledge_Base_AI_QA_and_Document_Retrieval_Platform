<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h3>文档管理</h3>
        <p>支持制度文档、FAQ、操作手册等非结构化资料的上传、索引与切片查看。</p>
      </div>
      <el-upload :show-file-list="false" :http-request="handleUpload" accept=".txt,.md,.pdf,.docx">
        <el-button type="primary" size="large">上传文档</el-button>
      </el-upload>
    </div>

    <el-table :data="documents" stripe class="table">
      <el-table-column prop="title" label="文档名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="90" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="tagTypeMap[row.status] || 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="切片数" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" width="920px" title="文档详情">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文档名称">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="文档状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="切片数">{{ detail.chunkCount }}</el-descriptions-item>
          <el-descriptions-item label="摘要" :span="2">{{ detail.summary || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMessage" label="错误信息" :span="2">
            {{ detail.errorMessage }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="chunk-list">
          <div v-for="chunk in detail.chunks" :key="chunk.id" class="chunk-item">
            <div class="chunk-index">{{ chunk.sourceLabel }}</div>
            <div class="chunk-content">{{ chunk.content }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDocument, getDocumentDetail, getDocuments, uploadDocument } from '../api/client'

const emit = defineEmits(['changed'])

const documents = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const tagTypeMap = {
  INDEXED: 'success',
  PROCESSING: 'warning',
  FAILED: 'danger'
}

const loadDocuments = async () => {
  try {
    const response = await getDocuments()
    documents.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const handleUpload = async ({ file }) => {
  try {
    await uploadDocument(file)
    ElMessage.success('文档上传并索引完成')
    await loadDocuments()
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const openDetail = async (id) => {
  try {
    const response = await getDocumentDetail(id)
    detail.value = response.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('删除后将移除文档元数据与向量索引，是否继续？', '提示', { type: 'warning' })
    await deleteDocument(id)
    ElMessage.success('删除成功')
    await loadDocuments()
    emit('changed')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(loadDocuments)
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

.chunk-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
  max-height: 420px;
  overflow: auto;
}

.chunk-item {
  padding: 16px;
  border-radius: 18px;
  background: #f6fbfa;
  border: 1px solid rgba(16, 33, 51, 0.08);
}

.chunk-index {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f766e;
  text-transform: uppercase;
}

.chunk-content {
  line-height: 1.8;
  color: #22384e;
}
</style>
