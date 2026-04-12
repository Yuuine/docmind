-- 列表默认按 user_id 过滤并按 created_at 倒序；复合索引减轻排序与分页成本
CREATE INDEX idx_documents_user_created ON documents (user_id, created_at DESC);
