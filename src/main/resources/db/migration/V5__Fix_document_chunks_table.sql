ALTER TABLE document_chunks
ADD COLUMN chunk_id VARCHAR(64) NOT NULL AFTER id,
ADD COLUMN char_count INT NOT NULL AFTER content,
DROP COLUMN embedding_id;

-- 为 chunk_id 添加唯一索引
ALTER TABLE document_chunks
ADD UNIQUE INDEX idx_chunk_id (chunk_id);
