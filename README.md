# 企业知识库问答与文档检索平台

一个按 `Java / Spring Boot / Vue3 / Python / MySQL / Sentence-BERT / 向量检索 / OpenAI Responses API` 技术栈实现的企业知识库问答与文档检索平台，覆盖文档上传、内容切分、向量检索、生成式回答、问答记录和用户反馈闭环。

## 项目亮点

- `Spring Boot` 负责文档管理、问答记录、反馈接口和业务编排。
- `Python + FastAPI` 负责文档解析、文本切分、Embedding 编码、语义召回和基于真实大模型的答案生成。
- `Vue3 + Element Plus` 提供检索页、知识库管理页、问答记录页。
- 同时支持 `txt / md / pdf / docx` 文档。
- 检索默认使用 `Sentence-BERT`，若本地模型不可用则自动降级到 `HashingVectorizer`。
- 生成式回答默认支持 `OpenAI Responses API`，未配置 `OPENAI_API_KEY` 时会自动回退到本地摘要回答，方便离线演示。
- 提供 `MySQL` 建表脚本，也提供 `H2` 开发配置用于本地快速启动。

## 目录结构

```text
D:\qiye_zhishiku
├─ backend        # Spring Boot 主后端
├─ frontend       # Vue3 前端
├─ ai-service     # Python 文档解析、向量检索与生成式回答服务
├─ sql            # MySQL 初始化脚本
└─ sample-docs    # 示例知识文档与公开资料
```

## 核心功能

1. 文档上传与索引
   后端接收文件并落盘，然后调用 Python 服务完成文档解析、文本切分和向量化。
2. 智能问答与语义检索
   用户输入自然语言问题，系统检索最相关片段，并基于命中上下文生成最终回答。
3. 问答记录与效果反馈
   每次问答都会落库，支持后续追踪和人工反馈。
4. 文档切片追踪
   可以查看每个文档的切片内容，便于展示知识库预处理流程。

## 启动方式

### 1. 启动 Python AI 服务

```powershell
cd D:\qiye_zhishiku\ai-service
py -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

可选环境变量：

- `EMBEDDING_MODEL`
  默认值为 `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`
- `OPENAI_API_KEY`
  配置后启用大模型生成式回答
- `OPENAI_MODEL`
  默认值为 `gpt-5.4-mini`
- `OPENAI_BASE_URL`
  可选，适用于 OpenAI 兼容网关

示例：

```powershell
$env:OPENAI_API_KEY="sk-..."
$env:OPENAI_MODEL="gpt-5.4-mini"
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2. 启动 Spring Boot 后端

默认开发环境使用 `H2`，无需额外数据库即可启动：

```powershell
cd D:\qiye_zhishiku\backend
mvn spring-boot:run
```

如果切换到 `MySQL`：

1. 执行 [init_mysql.sql](sql/init_mysql.sql)
2. 修改 [application-mysql.yml](backend/src/main/resources/application-mysql.yml) 中的账号密码
3. 使用以下命令启动：

```powershell
cd D:\qiye_zhishiku\backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 3. 启动 Vue3 前端

```powershell
cd D:\qiye_zhishiku\frontend
npm install
npm run dev
```

打开 `http://localhost:5173` 即可访问平台。

## 一键脚本

如果你希望在重启电脑后快速恢复环境，可以直接使用这些脚本：

- [start-all.ps1](scripts/start-all.ps1)
  一次性打开 3 个 PowerShell 窗口，分别启动 AI、后端、前端
- [start-ai.ps1](scripts/start-ai.ps1)
  单独启动 AI 服务，并自动加载豆包 Ark 配置
- [start-backend.ps1](scripts/start-backend.ps1)
  单独启动后端
- [start-frontend.ps1](scripts/start-frontend.ps1)
  单独启动前端
- [stop-services.ps1](scripts/stop-services.ps1)
  一键关闭 8000、8081、5173 端口对应的服务

推荐直接执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\qiye_zhishiku\scripts\start-all.ps1
```

## 推荐演示文档

3 份内容更丰富、适合企业知识库演示的公开正式文档，来源记录在 [SOURCES.md](sample-docs/public/SOURCES.md)：

- [系统操作手册（企业）.pdf](sample-docs/public/系统操作手册（企业）.pdf)
- [企业开办一网通办系统操作手册.pdf](sample-docs/public/企业开办一网通办系统操作手册.pdf)
- [全面数字化的电子发票常见问题即问即答（适用纳税人）.pdf](sample-docs/public/全面数字化的电子发票常见问题即问即答（适用纳税人）.pdf)

推荐问题示例：

- `企业开办一网通办系统里，设立登记通常有哪些步骤？`
- `全面数字化电子发票常见问题里，纳税人最容易遇到哪些开票场景？`
- `系统操作手册（企业）里，企业用户首次使用时需要重点关注哪些功能模块？`

## 接口说明

### 后端接口

- `POST /api/documents/upload` 上传并索引文档
- `GET /api/documents` 获取文档列表
- `GET /api/documents/{id}` 获取文档详情与切片
- `DELETE /api/documents/{id}` 删除文档
- `POST /api/qa/ask` 发起知识库问答
- `GET /api/qa/records` 获取问答记录
- `POST /api/feedback` 提交用户反馈
- `GET /api/dashboard/overview` 获取概览指标

### Python 服务接口

- `GET /api/health` 健康检查
- `POST /api/index` 文档解析与索引
- `DELETE /api/index/{documentId}` 删除索引
- `POST /api/query` 语义检索与生成式回答

## 说明

- 检索部分使用本地向量召回，生成部分支持接入 ai 模型。
- 如果未配置 AI Key，平台会自动回退到本地摘要回答，便于离线演示。
- 如果后续你要继续升级成更完整的 RAG，可以继续加入：
  - 文件级权限控制与多知识库隔离
  - 重排序模型
  - 混合检索
  - 对话上下文记忆
  - 文档标签与分类管理
