<template>
  <div class="shell">
    <aside class="sidebar">
      <div>
        <div class="brand-tag">Enterprise RAG Console</div>
        <h1>企业知识库问答与文档检索平台</h1>
        <p class="brand-copy">
          基于 Spring Boot、Vue3、Python、Sentence-BERT 与向量检索的企业知识应用示范项目。
        </p>
      </div>

      <div class="nav-list">
        <button
          v-for="item in menus"
          :key="item.key"
          :class="['nav-item', { active: activeView === item.key }]"
          @click="activeView = item.key"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
        </button>
      </div>
    </aside>

    <main class="content">
      <section class="hero">
        <div>
          <div class="hero-badge">Knowledge Search + Document Retrieval</div>
          <h2>上传制度文档、FAQ 和操作手册，让知识检索和问答形成闭环。</h2>
        </div>
        <div class="stat-grid">
          <StatCard label="知识文档" :value="overview.documentCount" hint="已纳入平台管理的文档数量" />
          <StatCard label="已索引" :value="overview.indexedCount" hint="完成切分与向量化的文档数量" />
          <StatCard label="问答记录" :value="overview.qaCount" hint="平台累计处理的问题数量" />
          <StatCard label="用户反馈" :value="overview.feedbackCount" hint="用于效果优化的评价数量" />
        </div>
      </section>

      <SearchView v-if="activeView === 'search'" @changed="loadOverview" />
      <DocumentsView v-else-if="activeView === 'documents'" @changed="loadOverview" />
      <RecordsView v-else @changed="loadOverview" />
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatCard from './components/StatCard.vue'
import SearchView from './views/SearchView.vue'
import DocumentsView from './views/DocumentsView.vue'
import RecordsView from './views/RecordsView.vue'
import { getOverview } from './api/client'

const activeView = ref('search')
const menus = [
  { key: 'search', label: '智能问答', desc: '语义检索与答案生成' },
  { key: 'documents', label: '文档管理', desc: '上传、查看、删除与切片' },
  { key: 'records', label: '问答记录', desc: '问答追踪与用户反馈' }
]

const overview = reactive({
  documentCount: 0,
  indexedCount: 0,
  qaCount: 0,
  feedbackCount: 0
})

const loadOverview = async () => {
  try {
    const response = await getOverview()
    Object.assign(overview, response.data)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 320px 1fr;
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 36px 28px;
  color: #f6f8f9;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.96), rgba(9, 63, 71, 0.9)),
    linear-gradient(180deg, #0f172a 0%, #155e75 100%);
}

.brand-tag {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin: 18px 0 12px;
  font-size: 34px;
  line-height: 1.1;
}

.brand-copy {
  margin: 0;
  color: rgba(246, 248, 249, 0.74);
  line-height: 1.7;
}

.nav-list {
  display: grid;
  gap: 14px;
}

.nav-item {
  display: grid;
  gap: 8px;
  padding: 16px;
  text-align: left;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: inherit;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.nav-item span {
  font-size: 18px;
  font-weight: 600;
}

.nav-item small {
  color: rgba(246, 248, 249, 0.7);
}

.nav-item:hover,
.nav-item.active {
  transform: translateY(-2px);
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.34), rgba(251, 191, 36, 0.18));
}

.content {
  padding: 28px;
}

.hero {
  margin-bottom: 24px;
}

.hero-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.12);
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h2 {
  max-width: 860px;
  margin: 16px 0 22px;
  font-size: 38px;
  line-height: 1.2;
  color: #102133;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1080px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .content {
    padding: 18px;
  }

  .hero h2 {
    font-size: 28px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
