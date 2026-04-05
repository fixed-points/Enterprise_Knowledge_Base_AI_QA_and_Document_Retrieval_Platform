package com.wqh.knowledgebase.dto;

import java.util.List;

public class AskResponse {
    private Long recordId;
    private String answer;
    private Long retrievalTimeMs;
    private String generationProvider;
    private String generationModel;
    private Boolean llmEnabled;
    private String fallbackReason;
    private List<SourceView> sources;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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
