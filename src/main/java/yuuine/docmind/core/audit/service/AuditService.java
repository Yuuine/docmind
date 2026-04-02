package yuuine.docmind.core.audit.service;

import yuuine.docmind.core.audit.dto.AuditLogQueryRequest;
import yuuine.docmind.core.audit.dto.AuditLogResponse;
import yuuine.docmind.core.audit.dto.PageResponse;

public interface AuditService {
    PageResponse<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request);

    AuditLogResponse getAuditLog(Long auditLogId);
}
