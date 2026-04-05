<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h3>智能问答</h3>
        <p>输入自然语言问题，系统会先做语义检索，再基于命中片段调用真实大模型生成回答。</p>
      </div>
      <div class="tag-group">
        <el-tag type="success">Sentence-BERT / Vector Search</el-tag>
        <el-tag :type="result.llmEnabled ? 'warning' : 'info'">
          {{ result.llmEnabled ? `${result.generationProvider} · ${result.generationModel}` : '本地摘要回退' }}
        </el-tag>
      </div>
    </div>

    <el-form class="search-form" :inline="true" @submit.prevent>
      <el-form-item class="question-item">
        <el-input
          v-model="form.question"
          size="large"
          placeholder="例如：企业开办一网通办系统里，设立登记通常有哪些步骤？"
          clearable
          @keyup.enter="handleAsk"
        />
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="form.topK" :min="1" :max="10" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="handleAsk">开始检索</el-button>
      </el-form-item>
    </el-form>

    <div class="result-grid">
      <div class="answer-card">
        <div class="section-title">生成式回答</div>
        <div class="answer-box">{{ result.answer || '提交问题后，这里会显示基于检索上下文生成的回答。' }}</div>
        <div class="meta-row" v-if="result.recordId">
          <span>问答记录 ID：{{ result.recordId }}</span>
          <span>检索耗时：{{ result.retrievalTimeMs }} ms</span>
        </div>
        <div v-if="result.fallbackReason" class="fallback-tip">
          {{ result.fallbackReason }}
        </div>
      </div>

      <div class="source-card">
        <div class="section-title">命中片段</div>
        <div v-if="result.sources.length" class="source-list">
          <article v-for="source in result.sources" :key="`${source.documentId}-${source.chunkIndex}`" class="source-item">
            <div class="source-head">
              <strong>{{ source.documentTitle }}</strong>
              <el-tag type="warning">chunk-{{ source.chunkIndex }}</el-tag>
            </div>
            <p>{{ source.snippet }}</p>
            <div class="score-row">
              <span>相似度</span>
              <el-progress :percentage="Math.max(1, Math.min(100, Math.round(source.score * 100)))" :stroke-width="10" />
            </div>
          </article>
        </div>
        <el-empty v-else description="暂未生成命中片段" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { askQuestion } from '../api/client'

const emit = defineEmits(['changed'])

const loading = ref(false)
const form = reactive({
  question: '',
  topK: 5
})

const result = reactive({
  recordId: null,
  answer: '',
  retrievalTimeMs: 0,
  generationProvider: '',
  generationModel: '',
  llmEnabled: false,
  fallbackReason: '',
  sources: []
})

const handleAsk = async () => {
  if (!form.question.trim()) {
    ElMessage.warning('请输入要检索的问题')
    return
  }
  loading.value = true
  try {
    const response = await askQuestion({
      question: form.question,
      topK: form.topK
    })
    Object.assign(result, response.data)
    emit('changed')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
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
  align-items: flex-start;
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

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.search-form {
  margin-top: 24px;
}

.question-item {
  width: min(720px, 100%);
}

.result-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  margin-top: 12px;
}

.answer-card,
.source-card {
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(240, 247, 244, 0.9), rgba(255, 255, 255, 0.9));
  border: 1px solid rgba(16, 33, 51, 0.08);
}

.section-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.answer-box {
  min-height: 220px;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #163148;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  color: #648098;
  font-size: 13px;
}

.fallback-tip {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  color: #9a5b00;
  background: rgba(245, 158, 11, 0.12);
  font-size: 13px;
  line-height: 1.6;
}

.source-list {
  display: grid;
  gap: 14px;
}

.source-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
}

.source-item p {
  margin: 10px 0 14px;
  color: #294256;
  line-height: 1.7;
}

.source-head,
.score-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.score-row {
  align-items: center;
}

@media (max-width: 960px) {
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
