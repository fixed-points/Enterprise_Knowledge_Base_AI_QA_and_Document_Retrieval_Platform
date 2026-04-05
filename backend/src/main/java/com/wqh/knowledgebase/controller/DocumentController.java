package com.wqh.knowledgebase.controller;

import com.wqh.knowledgebase.common.ApiResponse;
import com.wqh.knowledgebase.dto.DocumentDetailResponse;
import com.wqh.knowledgebase.entity.KnowledgeDocument;
import com.wqh.knowledgebase.service.DocumentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentDetailResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("上传并索引成功", documentService.upload(file));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocument>> list() {
        return ApiResponse.ok(documentService.list());
    }

    @PostMapping("/rebuild-indexes")
    public ApiResponse<Integer> rebuildIndexes() {
        return ApiResponse.ok("已完成知识库索引重建", documentService.rebuildIndexes());
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
