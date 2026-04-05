package com.wqh.knowledgebase.controller;

import com.wqh.knowledgebase.common.ApiResponse;
import com.wqh.knowledgebase.dto.AskRequest;
import com.wqh.knowledgebase.dto.AskResponse;
import com.wqh.knowledgebase.dto.QaRecordView;
import com.wqh.knowledgebase.service.QaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/ask")
    public ApiResponse<AskResponse> ask(@Validated @RequestBody AskRequest request) {
        return ApiResponse.ok("检索成功", qaService.ask(request));
    }

    @GetMapping("/records")
    public ApiResponse<List<QaRecordView>> records() {
        return ApiResponse.ok(qaService.listRecords());
    }
}
