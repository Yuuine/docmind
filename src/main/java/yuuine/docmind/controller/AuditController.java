package yuuine.docmind.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.dto.AuditLogQueryRequest;
import yuuine.docmind.core.audit.dto.AuditLogResponse;
import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.audit.service.AuditService;
import yuuine.docmind.core.audit.valueobject.AuditAction;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Audited(
            action = AuditAction.QUERY_AUDIT_LOG,
            resourceType = "AuditLog",
            describe = "查询审计日志",
            logResponse = false
    )
    @GetMapping("/logs")
    public Result<PageResponse<AuditLogResponse>> getLogs(AuditLogQueryRequest request) {
        return Result.success(auditService.queryAuditLogs(request));
    }

    @Audited(
            action = AuditAction.QUERY_AUDIT_LOG,
            resourceType = "AuditLog",
            resourceIdParam = "id",
            describe = "查询单条审计日志",
            logRequest = false,
            logResponse = false
    )
    @GetMapping("/logs/{id}")
    public Result<AuditLogResponse> getLog(@PathVariable Long id) {
        return Result.success(auditService.getAuditLog(id));
    }
}
