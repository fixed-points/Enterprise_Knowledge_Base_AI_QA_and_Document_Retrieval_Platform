package com.wqh.knowledgebase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqh.knowledgebase.client.AiServiceClient;
import com.wqh.knowledgebase.dto.AskRequest;
import com.wqh.knowledgebase.dto.AskResponse;
import com.wqh.knowledgebase.dto.QaRecordView;
import com.wqh.knowledgebase.entity.QaRecord;
import com.wqh.knowledgebase.repository.QaRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QaService {

    private final AiServiceClient aiServiceClient;
    private final DocumentService documentService;
    private final QaRecordRepository qaRecordRepository;
    private final ObjectMapper objectMapper;

    public QaService(AiServiceClient aiServiceClient,
                     DocumentService documentService,
                     QaRecordRepository qaRecordRepository,
                     ObjectMapper objectMapper) {
        this.aiServiceClient = aiServiceClient;
        this.documentService = documentService;
        this.qaRecordRepository = qaRecordRepository;
        this.objectMapper = objectMapper;
    }

    public AskResponse ask(AskRequest request) {
        ensureKnowledgeIndexReady();
        AiServiceClient.QueryResponse queryResponse = aiServiceClient.query(request.getQuestion(), request.getTopK());

        QaRecord record = new QaRecord();
        record.setQuestion(request.getQuestion());
        record.setAnswer(queryResponse.getAnswer());
        record.setLatencyMs(queryResponse.getRetrievalTimeMs());
        try {
            record.setSourceSummary(objectMapper.writeValueAsString(queryResponse.getSources()));
        } catch (JsonProcessingException ex) {
            record.setSourceSummary("[]");
        }
        qaRecordRepository.save(record);

        AskResponse response = new AskResponse();
        response.setRecordId(record.getId());
        response.setAnswer(queryResponse.getAnswer());
        response.setRetrievalTimeMs(queryResponse.getRetrievalTimeMs());
        response.setGenerationProvider(queryResponse.getGenerationProvider());
        response.setGenerationModel(queryResponse.getGenerationModel());
        response.setLlmEnabled(queryResponse.getLlmEnabled());
        response.setFallbackReason(queryResponse.getFallbackReason());
        response.setSources(queryResponse.getSources());
        return response;
    }

    private void ensureKnowledgeIndexReady() {
        AiServiceClient.HealthResponse health = aiServiceClient.getHealth();
        if (health == null || health.getIndexedDocuments() == null) {
            return;
        }
        if (health.getIndexedDocuments() == 0 && health.getIndexedChunks() != null && health.getIndexedChunks() == 0) {
            documentService.rebuildIndexes();
        }
    }

    public List<QaRecordView> listRecords() {
        return qaRecordRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(record -> {
                    QaRecordView view = new QaRecordView();
                    view.setId(record.getId());
                    view.setQuestion(record.getQuestion());
                    view.setAnswer(record.getAnswer());
                    view.setSourceSummary(record.getSourceSummary());
                    view.setLatencyMs(record.getLatencyMs());
                    view.setCreatedAt(record.getCreatedAt());
                    return view;
                })
                .collect(Collectors.toList());
    }
}
