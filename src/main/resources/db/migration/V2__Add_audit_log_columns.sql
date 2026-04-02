-- ============================================================
-- V2__Add_audit_log_columns.sql
-- 
-- 目的: 为 audit_logs 表补充审计日志系统所需的扩展字段
-- ============================================================

-- 添加 HTTP 请求方法
ALTER TABLE audit_logs ADD COLUMN http_method VARCHAR(10) DEFAULT NULL COMMENT 'HTTP请求方法';

-- 添加请求路径
ALTER TABLE audit_logs ADD COLUMN request_path VARCHAR(500) DEFAULT NULL COMMENT '请求路径';

-- 添加查询参数
ALTER TABLE audit_logs ADD COLUMN query_string VARCHAR(1000) DEFAULT NULL COMMENT 'URL查询字符串';

-- 添加错误信息
ALTER TABLE audit_logs ADD COLUMN error_message TEXT DEFAULT NULL COMMENT '错误详情信息';

-- 添加方法执行耗时 (毫秒)
ALTER TABLE audit_logs ADD COLUMN execution_time BIGINT DEFAULT NULL COMMENT '方法执行耗时(ms)';

-- 添加操作描述
ALTER TABLE audit_logs ADD COLUMN operation_description VARCHAR(500) DEFAULT NULL COMMENT '操作描述';

-- 添加服务器主机名
ALTER TABLE audit_logs ADD COLUMN server_host VARCHAR(100) DEFAULT NULL COMMENT '服务器主机名';

-- 添加链路追踪ID
ALTER TABLE audit_logs ADD COLUMN trace_id VARCHAR(50) DEFAULT NULL COMMENT '链路追踪ID';

-- 创建索引
CREATE INDEX idx_trace_id ON audit_logs(trace_id);
CREATE INDEX idx_http_method ON audit_logs(http_method);
CREATE INDEX idx_request_path ON audit_logs(request_path);
CREATE INDEX idx_execution_time ON audit_logs(execution_time);