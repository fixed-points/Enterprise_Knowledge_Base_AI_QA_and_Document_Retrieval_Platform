package com.wqh.knowledgebase.service;

import com.wqh.knowledgebase.common.BusinessException;
import com.wqh.knowledgebase.dto.FeedbackRequest;
import com.wqh.knowledgebase.entity.UserFeedback;
import com.wqh.knowledgebase.repository.QaRecordRepository;
import com.wqh.knowledgebase.repository.UserFeedbackRepository;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;
    private final QaRecordRepository qaRecordRepository;

    public FeedbackService(UserFeedbackRepository feedbackRepository,
                           QaRecordRepository qaRecordRepository) {
        this.feedbackRepository = feedbackRepository;
        this.qaRecordRepository = qaRecordRepository;
    }

    public void save(FeedbackRequest request) {
        qaRecordRepository.findById(request.getQaRecordId())
                .orElseThrow(() -> new BusinessException("问答记录不存在"));

        UserFeedback feedback = new UserFeedback();
        feedback.setQaRecordId(request.getQaRecordId());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedbackRepository.save(feedback);
    }
}
