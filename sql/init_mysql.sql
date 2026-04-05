CREATE DATABASE IF NOT EXISTS enterprise_knowledge_base
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE enterprise_knowledge_base;

CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  file_type VARCHAR(32) NOT NULL,
  file_size BIGINT NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL,
  summary VARCHAR(1000),
  chunk_count INT DEFAULT 0,
  error_message VARCHAR(500),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  source_label VARCHAR(100),
  created_at DATETIME,
  KEY idx_document_chunk (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS qa_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question LONGTEXT NOT NULL,
  answer LONGTEXT NOT NULL,
  source_summary LONGTEXT NOT NULL,
  latency_ms BIGINT,
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS user_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qa_record_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME,
  KEY idx_feedback_record (qa_record_id)
);
