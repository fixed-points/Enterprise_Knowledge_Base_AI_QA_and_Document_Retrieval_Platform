package com.wqh.knowledgebase.repository;

import com.wqh.knowledgebase.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
