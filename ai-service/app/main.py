from __future__ import annotations

import os
import re
import time
from dataclasses import dataclass
from pathlib import Path
from threading import Lock
from typing import Dict, List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sklearn.feature_extraction.text import HashingVectorizer

try:
    from docx import Document as DocxDocument
except Exception:
    DocxDocument = None

try:
    from openai import OpenAI
except Exception:
    OpenAI = None

try:
    from pypdf import PdfReader
except Exception:
    PdfReader = None

try:
    from sentence_transformers import SentenceTransformer
except Exception:
    SentenceTransformer = None


class IndexRequest(BaseModel):
    documentId: int
    title: str
    filePath: str
    fileType: str


class QueryRequest(BaseModel):
    question: str = Field(..., min_length=1)
    topK: int = Field(default=5, ge=1, le=10)


class ChunkPayload(BaseModel):
    chunkIndex: int
    content: str
    sourceLabel: str


class SourcePayload(BaseModel):
    documentId: int
    documentTitle: str
    chunkIndex: int
    score: float
    snippet: str


class IndexResponse(BaseModel):
    summary: str
    chunkCount: int
    chunks: List[ChunkPayload]


class QueryResponse(BaseModel):
    answer: str
    retrievalTimeMs: int
    generationProvider: str
    generationModel: str
    llmEnabled: bool
    fallbackReason: str = ""
    sources: List[SourcePayload]


@dataclass
class IndexedChunk:
    document_id: int
    document_title: str
    chunk_index: int
    content: str
    source_label: str
    semantic_vector: np.ndarray
    lexical_vector: np.ndarray


class EmbeddingEngine:
    def __init__(self) -> None:
        self.model_name = os.getenv(
            "EMBEDDING_MODEL",
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        )
        self.backend = "hashing"
        self.model = None
        if SentenceTransformer is not None:
            try:
                self.model = SentenceTransformer(self.model_name)
                self.backend = "sentence-transformers"
            except Exception:
                self.model = None
        self.hashing = HashingVectorizer(n_features=2048, alternate_sign=False, norm="l2")
        self.lexical = HashingVectorizer(
            n_features=4096,
            alternate_sign=False,
            norm="l2",
            analyzer="char",
            ngram_range=(2, 4),
        )

    def encode_semantic(self, texts: List[str]) -> np.ndarray:
        if not texts:
            return np.zeros((0, 1), dtype=np.float32)
        if self.model is not None:
            vectors = self.model.encode(texts, normalize_embeddings=True)
            return np.asarray(vectors, dtype=np.float32)
        matrix = self.hashing.transform(texts).toarray().astype(np.float32)
        norms = np.linalg.norm(matrix, axis=1, keepdims=True)
        norms[norms == 0] = 1.0
        return matrix / norms

    def encode_lexical(self, texts: List[str]) -> np.ndarray:
        if not texts:
            return np.zeros((0, 1), dtype=np.float32)
        matrix = self.lexical.transform(texts).toarray().astype(np.float32)
        norms = np.linalg.norm(matrix, axis=1, keepdims=True)
        norms[norms == 0] = 1.0
        return matrix / norms


class KnowledgeIndex:
    def __init__(self, embedding_engine: EmbeddingEngine) -> None:
        self.embedding_engine = embedding_engine
        self.lock = Lock()
        self.documents: Dict[int, Dict[str, object]] = {}
        self.flat_chunks: List[IndexedChunk] = []
        self.semantic_matrix = np.zeros((0, 1), dtype=np.float32)
        self.lexical_matrix = np.zeros((0, 1), dtype=np.float32)

    def upsert(self, document_id: int, title: str, chunks: List[ChunkPayload]) -> None:
        texts_for_index = [f"{title}\n{item.content}" for item in chunks]
        semantic_vectors = self.embedding_engine.encode_semantic(texts_for_index)
        lexical_vectors = self.embedding_engine.encode_lexical(texts_for_index)
        indexed_chunks = []
        for idx, item in enumerate(chunks):
            indexed_chunks.append(
                IndexedChunk(
                    document_id=document_id,
                    document_title=title,
                    chunk_index=item.chunkIndex,
                    content=item.content,
                    source_label=item.sourceLabel,
                    semantic_vector=semantic_vectors[idx],
                    lexical_vector=lexical_vectors[idx],
                )
            )
        with self.lock:
            self.documents[document_id] = {"title": title, "chunks": indexed_chunks}
            self._rebuild()

    def remove(self, document_id: int) -> None:
        with self.lock:
            self.documents.pop(document_id, None)
            self._rebuild()

    def search(self, question: str, top_k: int) -> List[SourcePayload]:
        with self.lock:
            if not self.flat_chunks:
                raise HTTPException(status_code=400, detail="当前知识库为空，请先上传并索引文档")
            semantic_vector = self.embedding_engine.encode_semantic([question])[0]
            lexical_vector = self.embedding_engine.encode_lexical([question])[0]
            semantic_scores = self.semantic_matrix @ semantic_vector
            lexical_scores = self.lexical_matrix @ lexical_vector
            scores = semantic_scores * 0.65 + lexical_scores * 0.35
            top_indices = np.argsort(scores)[::-1][:top_k]
            result = []
            for index in top_indices:
                chunk = self.flat_chunks[int(index)]
                result.append(
                    SourcePayload(
                        documentId=chunk.document_id,
                        documentTitle=chunk.document_title,
                        chunkIndex=chunk.chunk_index,
                        score=float(round(float(scores[int(index)]), 4)),
                        snippet=chunk.content[:320],
                    )
                )
            return result

    def _rebuild(self) -> None:
        self.flat_chunks = []
        for document in self.documents.values():
            self.flat_chunks.extend(document["chunks"])
        if not self.flat_chunks:
            self.semantic_matrix = np.zeros((0, 1), dtype=np.float32)
            self.lexical_matrix = np.zeros((0, 1), dtype=np.float32)
            return
        self.semantic_matrix = np.vstack([chunk.semantic_vector for chunk in self.flat_chunks]).astype(np.float32)
        self.lexical_matrix = np.vstack([chunk.lexical_vector for chunk in self.flat_chunks]).astype(np.float32)


class OpenAIGenerator:
    def __init__(self) -> None:
        self.api_key = os.getenv("OPENAI_API_KEY", "").strip() or os.getenv("ARK_API_KEY", "").strip()
        self.base_url = os.getenv("OPENAI_BASE_URL", "").strip() or os.getenv("ARK_BASE_URL", "").strip()
        self.model = os.getenv("OPENAI_MODEL", "").strip() or os.getenv("ARK_MODEL", "").strip() or "gpt-5.4-mini"
        self.provider = "openai"
        self.client = None
        self.enabled = bool(self.api_key and OpenAI is not None)
        if "volces.com" in self.base_url:
            self.provider = "doubao-ark"
        if self.enabled:
            kwargs = {"api_key": self.api_key}
            if self.base_url:
                kwargs["base_url"] = self.base_url
            self.client = OpenAI(**kwargs)

    def generate(self, question: str, sources: List[SourcePayload]) -> tuple[str, str]:
        if not self.enabled or self.client is None:
            return fallback_answer(question, sources), "未配置 OPENAI_API_KEY，已使用本地摘要回答"

        system_prompt = (
            "你是企业知识库问答助手。"
            "请严格基于检索到的文档片段作答，使用中文回答。"
            "如果证据不足，请明确说明“根据当前检索结果无法完全确认”。"
            "回答要求结构清晰、简洁专业，并尽量在句子里引用来源标记。"
        )
        context_blocks = []
        for item in sources:
            context_blocks.append(
                f"[来源 {item.documentTitle}#chunk-{item.chunkIndex}]\n"
                f"相似度: {item.score}\n"
                f"内容: {item.snippet}"
            )
        user_prompt = (
            f"用户问题：{question}\n\n"
            f"检索上下文：\n{chr(10).join(context_blocks)}\n\n"
            "请给出：\n"
            "1. 直接回答\n"
            "2. 关键依据\n"
            "3. 如有必要，补充注意事项\n"
        )

        try:
            response = self.client.responses.create(
                model=self.model,
                input=[
                    {
                        "role": "system",
                        "content": [{"type": "input_text", "text": system_prompt}],
                    },
                    {
                        "role": "user",
                        "content": [{"type": "input_text", "text": user_prompt}],
                    },
                ],
            )
            answer = (response.output_text or "").strip()
            if answer:
                if is_invalid_llm_answer(question, answer):
                    return fallback_answer(question, sources), "大模型未正确理解问题，已回退到本地摘要回答"
                return answer, ""
            return fallback_answer(question, sources), "OpenAI 返回空内容，已回退到本地摘要回答"
        except Exception as exc:
            return fallback_answer(question, sources), f"OpenAI 调用失败，已回退到本地摘要回答: {exc}"


def read_text_file(file_path: Path) -> str:
    for encoding in ("utf-8", "gbk", "gb2312"):
        try:
            return file_path.read_text(encoding=encoding)
        except Exception:
            continue
    return file_path.read_text(encoding="utf-8", errors="ignore")


def read_pdf(file_path: Path) -> str:
    if PdfReader is None:
        raise HTTPException(status_code=500, detail="缺少 PDF 解析依赖 pypdf")
    reader = PdfReader(str(file_path))
    return "\n".join(page.extract_text() or "" for page in reader.pages)


def read_docx(file_path: Path) -> str:
    if DocxDocument is None:
        raise HTTPException(status_code=500, detail="缺少 DOCX 解析依赖 python-docx")
    document = DocxDocument(str(file_path))
    return "\n".join(paragraph.text for paragraph in document.paragraphs)


def clean_extracted_text(raw: str) -> str:
    cleaned_lines = []
    for line in raw.splitlines():
        candidate = re.sub(r"\s+", " ", line).strip()
        if not candidate:
            continue
        if re.search(r"[.。·•]{6,}", candidate):
            continue
        if re.fullmatch(r"[0-9 ]+", candidate):
            continue
        if re.search(r"^第?\s*[0-9一二三四五六七八九十]+\s*页$", candidate):
            continue
        cleaned_lines.append(candidate)
    text = "\n".join(cleaned_lines)
    text = re.sub(r"\n{2,}", "\n", text)
    return text.strip()


def parse_document(file_path: str, file_type: str) -> str:
    path = Path(file_path)
    if not path.exists():
        raise HTTPException(status_code=404, detail="待解析文件不存在")
    file_type = file_type.lower()
    if file_type in {"txt", "md"}:
        raw = read_text_file(path)
    elif file_type == "pdf":
        raw = read_pdf(path)
    elif file_type == "docx":
        raw = read_docx(path)
    else:
        raise HTTPException(status_code=400, detail=f"暂不支持的文件类型: {file_type}")
    cleaned = clean_extracted_text(raw)
    cleaned = re.sub(r"[ \t]+", " ", cleaned)
    if not cleaned:
        raise HTTPException(status_code=400, detail="文档解析后内容为空")
    return cleaned


def split_text(text: str, max_length: int = 520, overlap: int = 100) -> List[ChunkPayload]:
    if len(text) <= max_length:
        return [ChunkPayload(chunkIndex=1, content=text, sourceLabel="chunk-1")]
    chunks = []
    start = 0
    index = 1
    while start < len(text):
        end = min(start + max_length, len(text))
        snippet = text[start:end].strip()
        if snippet:
            chunks.append(ChunkPayload(chunkIndex=index, content=snippet, sourceLabel=f"chunk-{index}"))
            index += 1
        if end >= len(text):
            break
        start = max(end - overlap, start + 1)
    return chunks


def summarize(text: str) -> str:
    clipped = text[:220]
    return clipped + ("..." if len(text) > 220 else "")


def fallback_answer(question: str, sources: List[SourcePayload]) -> str:
    if not sources:
        return "知识库中没有检索到与该问题相关的内容。"
    lines = ["知识库命中了以下重点信息："]
    for idx, source in enumerate(sources[:3], start=1):
        lines.append(
            f"{idx}. 来源《{source.documentTitle}》 chunk-{source.chunkIndex}：{source.snippet[:140]}。"
        )
    lines.append("建议结合命中文档原文进一步核对细节。")
    return "\n".join(lines)


def is_invalid_llm_answer(question: str, answer: str) -> bool:
    if not question.strip():
        return False
    invalid_patterns = [
        "未明确具体的咨询问题",
        "未获取到有效的提问内容",
        "未提出具体的问题",
        "请您明确具体的咨询问题",
        "请您明确具体问题",
        "无法为您提供针对性的解答",
    ]
    return any(pattern in answer for pattern in invalid_patterns)


embedding_engine = EmbeddingEngine()
knowledge_index = KnowledgeIndex(embedding_engine)
llm_generator = OpenAIGenerator()
app = FastAPI(title="Enterprise Knowledge AI Service")


@app.get("/api/health")
def health() -> dict:
    return {
        "status": "ok",
        "embeddingBackend": embedding_engine.backend,
        "indexedDocuments": len(knowledge_index.documents),
        "indexedChunks": len(knowledge_index.flat_chunks),
        "llmEnabled": llm_generator.enabled,
        "generationProvider": llm_generator.provider,
        "generationModel": llm_generator.model,
    }


@app.post("/api/index", response_model=IndexResponse)
def index_document(request: IndexRequest) -> IndexResponse:
    text = parse_document(request.filePath, request.fileType)
    chunks = split_text(text)
    knowledge_index.upsert(request.documentId, request.title, chunks)
    return IndexResponse(summary=summarize(text), chunkCount=len(chunks), chunks=chunks)


@app.delete("/api/index/{document_id}")
def remove_document(document_id: int) -> dict:
    knowledge_index.remove(document_id)
    return {"success": True}


@app.post("/api/query", response_model=QueryResponse)
def query(request: QueryRequest) -> QueryResponse:
    begin = time.perf_counter()
    sources = knowledge_index.search(request.question, request.topK)
    answer, fallback_reason = llm_generator.generate(request.question, sources)
    elapsed = int((time.perf_counter() - begin) * 1000)
    return QueryResponse(
        answer=answer,
        retrievalTimeMs=elapsed,
        generationProvider=llm_generator.provider,
        generationModel=llm_generator.model,
        llmEnabled=llm_generator.enabled,
        fallbackReason=fallback_reason,
        sources=sources,
    )
