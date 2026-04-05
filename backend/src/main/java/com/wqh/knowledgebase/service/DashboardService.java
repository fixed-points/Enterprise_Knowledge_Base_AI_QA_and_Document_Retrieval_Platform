package com.wqh.knowledgebase.service;

import com.wqh.knowledgebase.dto.DashboardOverview;
import com.wqh.knowledgebase.repository.KnowledgeDocumentRepository;
import com.wqh.knowledgebase.repository.QaRecordRepository;
import com.wqh.knowledgebase.repository.UserFeedbackRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final KnowledgeDocumentRepository documentRepository;
    private final QaRecordRepository qaRecordRepository;
    private final UserFeedbackRepository feedbackRepository;

    public DashboardService(KnowledgeDocumentRepository documentRepository,
                            QaRecordRepository qaRecordRepository,
                            UserFeedbackRepository feedbackRepository) {
        this.documentRepository = documentRepository;
        this.qaRecordRepository = qaRecordRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public DashboardOverview overview() {
        DashboardOverview overview = new DashboardOverview();
        overview.setDocumentCount(documentRepository.count());
        overview.setIndexedCount(documentRepository.countByStatus("INDEXED"));
        overview.setQaCount(qaRecordRepository.count());
        overview.setFeedbackCount(feedbackRepository.count());
        return overview;
    }
}
