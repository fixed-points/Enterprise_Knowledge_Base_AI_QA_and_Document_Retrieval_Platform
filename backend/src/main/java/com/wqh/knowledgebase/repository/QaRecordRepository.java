package com.wqh.knowledgebase.repository;

import com.wqh.knowledgebase.entity.QaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaRecordRepository extends JpaRepository<QaRecord, Long> {
    List<QaRecord> findAllByOrderByCreatedAtDesc();
}
