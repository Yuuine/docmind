package yuuine.docmind.core.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.audit.valueobject.AuditStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {
    private Long userId;
    private AuditAction action;
    private String resourceType;
    private Long resourceId;
    private String ipAddress;
    private String userAgent;
    private String requestData;
    private AuditStatus status;
}
