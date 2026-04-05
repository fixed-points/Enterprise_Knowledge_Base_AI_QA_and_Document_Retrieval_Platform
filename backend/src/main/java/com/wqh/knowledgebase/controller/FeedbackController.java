package com.wqh.knowledgebase.controller;

import com.wqh.knowledgebase.common.ApiResponse;
import com.wqh.knowledgebase.dto.FeedbackRequest;
import com.wqh.knowledgebase.service.FeedbackService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<Void> submit(@Validated @RequestBody FeedbackRequest request) {
        feedbackService.save(request);
        return ApiResponse.ok("反馈已记录", null);
    }
}
