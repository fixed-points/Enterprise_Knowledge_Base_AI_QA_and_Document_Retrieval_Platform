package com.wqh.knowledgebase.client;

import com.wqh.knowledgebase.common.BusinessException;
import com.wqh.knowledgebase.dto.SourceView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String baseUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void normalizeBaseUrl() {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    public IndexResponse indexDocument(Long documentId, String title, String filePath, String fileType) {
        Map<String, Object> request = Map.of(
                "documentId", documentId,
                "title", title,
                "filePath", filePath,
                "fileType", fileType
        );
        ResponseEntity<IndexResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/index",
                request,
                IndexResponse.class
        );
        if (response.getBody() == null) {
            throw new BusinessException("AI 服务未返回索引结果");
        }
        return response.getBody();
    }

    public QueryResponse query(String question, Integer topK) {
        Map<String, Object> request = Map.of(
                "question", question,
                "topK", topK == null ? 5 : topK
        );
        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/query",
                request,
                QueryResponse.class
        );
        if (response.getBody() == null) {
            throw new BusinessException("AI 服务未返回问答结果");
        }
        return response.getBody();
    }

    public void removeDocument(Long documentId) {
        try {
            restTemplate.exchange(
                    baseUrl + "/api/index/" + documentId,
                    HttpMethod.DELETE,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );
        } catch (Exception ignored) {
        }
    }

    public HealthResponse getHealth() {
        try {
            ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
                    baseUrl + "/api/health",
                    HealthResponse.class
            );
            return response.getBody();
        } catch (Exception ex) {
            return null;
        }
    }

    public static class IndexResponse {
        private String summary;
        private Integer chunkCount;
        private List<ChunkPayload> chunks = Collections.emptyList();

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Integer getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(Integer chunkCount) {
            this.chunkCount = chunkCount;
        }

        public List<ChunkPayload> getChunks() {
            return chunks;
        }

        public void setChunks(List<ChunkPayload> chunks) {
            this.chunks = chunks;
        }
    }

    public static class ChunkPayload {
        private Integer chunkIndex;
        private String content;
        private String sourceLabel;

        public Integer getChunkIndex() {
            return chunkIndex;
        }

        public void setChunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSourceLabel() {
            return sourceLabel;
        }

        public void setSourceLabel(String sourceLabel) {
            this.sourceLabel = sourceLabel;
        }
    }

    public static class QueryResponse {
        private String answer;
        private Long retrievalTimeMs;
        private String generationProvider;
        private String generationModel;
        private Boolean llmEnabled;
        private String fallbackReason;
        private List<SourceView> sources = Collections.emptyList();

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public Long getRetrievalTimeMs() {
            return retrievalTimeMs;
        }

        public void setRetrievalTimeMs(Long retrievalTimeMs) {
            this.retrievalTimeMs = retrievalTimeMs;
        }

        public String getGenerationProvider() {
            return generationProvider;
        }

        public void setGenerationProvider(String generationProvider) {
            this.generationProvider = generationProvider;
        }

        public String getGenerationModel() {
            return generationModel;
        }

        public void setGenerationModel(String generationModel) {
            this.generationModel = generationModel;
        }

        public Boolean getLlmEnabled() {
            return llmEnabled;
        }

        public void setLlmEnabled(Boolean llmEnabled) {
            this.llmEnabled = llmEnabled;
        }

        public String getFallbackReason() {
            return fallbackReason;
        }

        public void setFallbackReason(String fallbackReason) {
            this.fallbackReason = fallbackReason;
        }

        public List<SourceView> getSources() {
            return sources;
        }

        public void setSources(List<SourceView> sources) {
            this.sources = sources;
        }
    }

    public static class HealthResponse {
        private String status;
        private String embeddingBackend;
        private Integer indexedDocuments;
        private Integer indexedChunks;
        private Boolean llmEnabled;
        private String generationProvider;
        private String generationModel;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getEmbeddingBackend() {
            return embeddingBackend;
        }

        public void setEmbeddingBackend(String embeddingBackend) {
            this.embeddingBackend = embeddingBackend;
        }

        public Integer getIndexedDocuments() {
            return indexedDocuments;
        }

        public void setIndexedDocuments(Integer indexedDocuments) {
            this.indexedDocuments = indexedDocuments;
        }

        public Integer getIndexedChunks() {
            return indexedChunks;
        }

        public void setIndexedChunks(Integer indexedChunks) {
            this.indexedChunks = indexedChunks;
        }

        public Boolean getLlmEnabled() {
            return llmEnabled;
        }

        public void setLlmEnabled(Boolean llmEnabled) {
            this.llmEnabled = llmEnabled;
        }

        public String getGenerationProvider() {
            return generationProvider;
        }

        public void setGenerationProvider(String generationProvider) {
            this.generationProvider = generationProvider;
        }

        public String getGenerationModel() {
            return generationModel;
        }

        public void setGenerationModel(String generationModel) {
            this.generationModel = generationModel;
        }
    }
}
