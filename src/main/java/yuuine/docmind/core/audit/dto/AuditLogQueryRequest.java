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
public class AuditLogQueryRequest {
    private Long userId;
    private AuditAction action;
    private String resourceType;
    private AuditStatus status;
    private String startDate;
    private String endDate;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer pageSize = 20;
}
