package com.wqh.knowledgebase.service;

import com.wqh.knowledgebase.client.AiServiceClient;
import com.wqh.knowledgebase.common.BusinessException;
import com.wqh.knowledgebase.dto.DocumentChunkView;
import com.wqh.knowledgebase.dto.DocumentDetailResponse;
import com.wqh.knowledgebase.entity.KnowledgeChunk;
import com.wqh.knowledgebase.entity.KnowledgeDocument;
import com.wqh.knowledgebase.repository.KnowledgeChunkRepository;
import com.wqh.knowledgebase.repository.KnowledgeDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "pdf", "docx");

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final AiServiceClient aiServiceClient;

    @Value("${app.storage.path}")
    private String storagePath;

    public DocumentService(KnowledgeDocumentRepository documentRepository,
                           KnowledgeChunkRepository chunkRepository,
                           AiServiceClient aiServiceClient) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional
    public DocumentDetailResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请先选择需要上传的文档");
        }
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!SUPPORTED_TYPES.contains(extension)) {
            throw new BusinessException("仅支持 txt、md、pdf、docx 文档");
        }

        Path uploadDir = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            String storedName = UUID.randomUUID() + "_" + (originalName == null ? "document" : originalName);
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument document = new KnowledgeDocument();
            document.setTitle(originalName);
            document.setFileType(extension);
            document.setFileSize(file.getSize());
            document.setFilePath(target.toString());
            document.setStatus("PROCESSING");
            document.setChunkCount(0);
            documentRepository.save(document);

            try {
                AiServiceClient.IndexResponse indexResponse = aiServiceClient.indexDocument(
                        document.getId(),
                        document.getTitle(),
                        document.getFilePath(),
                        document.getFileType()
                );

                List<KnowledgeChunk> chunks = indexResponse.getChunks().stream().map(item -> {
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setDocumentId(document.getId());
                    chunk.setChunkIndex(item.getChunkIndex());
                    chunk.setContent(item.getContent());
                    chunk.setSourceLabel(item.getSourceLabel());
                    return chunk;
                }).collect(Collectors.toList());

                chunkRepository.saveAll(chunks);
                document.setStatus("INDEXED");
                document.setSummary(indexResponse.getSummary());
                document.setChunkCount(indexResponse.getChunkCount());
                document.setErrorMessage(null);
                documentRepository.save(document);
            } catch (Exception ex) {
                document.setStatus("FAILED");
                document.setErrorMessage(ex.getMessage());
                documentRepository.save(document);
                throw new BusinessException("文档索引失败: " + ex.getMessage());
            }

            return getDetail(document.getId());
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败: " + ex.getMessage());
        }
    }

    public List<KnowledgeDocument> list() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public int rebuildIndexes() {
        AiServiceClient.HealthResponse health = aiServiceClient.getHealth();
        if (health == null) {
            throw new BusinessException("AI 服务不可用，无法重建索引");
        }

        List<KnowledgeDocument> documents = documentRepository.findAllByOrderByCreatedAtDesc();
        int rebuiltCount = 0;

        for (KnowledgeDocument document : documents) {
            if (!StringUtils.hasText(document.getFilePath()) || !Files.exists(Paths.get(document.getFilePath()))) {
                chunkRepository.deleteByDocumentId(document.getId());
                document.setStatus("FAILED");
                document.setChunkCount(0);
                document.setErrorMessage("源文件不存在，无法重建索引");
                documentRepository.save(document);
                continue;
            }

            document.setStatus("PROCESSING");
            document.setErrorMessage(null);
            documentRepository.save(document);

            try {
                AiServiceClient.IndexResponse indexResponse = aiServiceClient.indexDocument(
                        document.getId(),
                        document.getTitle(),
                        document.getFilePath(),
                        document.getFileType()
                );

                chunkRepository.deleteByDocumentId(document.getId());
                List<KnowledgeChunk> chunks = indexResponse.getChunks().stream().map(item -> {
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setDocumentId(document.getId());
                    chunk.setChunkIndex(item.getChunkIndex());
                    chunk.setContent(item.getContent());
                    chunk.setSourceLabel(item.getSourceLabel());
                    return chunk;
                }).collect(Collectors.toList());

                chunkRepository.saveAll(chunks);
                document.setStatus("INDEXED");
                document.setSummary(indexResponse.getSummary());
                document.setChunkCount(indexResponse.getChunkCount());
                document.setErrorMessage(null);
                documentRepository.save(document);
                rebuiltCount++;
            } catch (Exception ex) {
                chunkRepository.deleteByDocumentId(document.getId());
                document.setStatus("FAILED");
                document.setChunkCount(0);
                document.setErrorMessage(ex.getMessage());
                documentRepository.save(document);
            }
        }

        return rebuiltCount;
    }

    public DocumentDetailResponse getDetail(Long documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));

        List<DocumentChunkView> chunkViews = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .map(chunk -> {
                    DocumentChunkView view = new DocumentChunkView();
                    view.setId(chunk.getId());
                    view.setChunkIndex(chunk.getChunkIndex());
                    view.setContent(chunk.getContent());
                    view.setSourceLabel(chunk.getSourceLabel());
                    return view;
                })
                .collect(Collectors.toList());

        DocumentDetailResponse response = new DocumentDetailResponse();
        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setSummary(document.getSummary());
        response.setChunkCount(document.getChunkCount());
        response.setErrorMessage(document.getErrorMessage());
        response.setCreatedAt(document.getCreatedAt());
        response.setChunks(chunkViews);
        return response;
    }

    @Transactional
    public void delete(Long documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        aiServiceClient.removeDocument(documentId);
        try {
            if (StringUtils.hasText(document.getFilePath())) {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            }
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
