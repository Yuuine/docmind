package yuuine.docmind.core.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.audit.dto.AuditLogQueryRequest;
import yuuine.docmind.core.audit.dto.AuditLogResponse;
import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.audit.model.AuditLog;
import yuuine.docmind.core.audit.repository.AuditLogRepository;
import yuuine.docmind.core.audit.service.AuditService;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.audit.valueobject.AuditStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResponse<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLog> queryWrapper = buildQueryWrapper(request);

        int page = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Page<AuditLog> auditLogPage = new Page<>(page, pageSize);
        Page<AuditLog> result = auditLogRepository.selectPage(auditLogPage, queryWrapper);

        List<AuditLogResponse> records = result.getRecords().stream()
                .map(this::toAuditLogResponse)
                .toList();

        return PageResponse.of(records, result.getTotal(), page, pageSize);
    }

    @Override
    public AuditLogResponse getAuditLog(Long auditLogId) {
        AuditLog auditLog = auditLogRepository.selectById(auditLogId);
        if (auditLog == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "审计日志不存在");
        }
        return toAuditLogResponse(auditLog);
    }

    private LambdaQueryWrapper<AuditLog> buildQueryWrapper(AuditLogQueryRequest request) {
        LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();

        if (request.getUserId() != null) {
            queryWrapper.eq(AuditLog::getUserId, request.getUserId());
        }
        if (request.getAction() != null) {
            queryWrapper.eq(AuditLog::getAction, request.getAction().name());
        }
        if (request.getResourceType() != null && !request.getResourceType().isBlank()) {
            queryWrapper.eq(AuditLog::getResourceType, request.getResourceType());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(AuditLog::getStatus, request.getStatus().name());
        }
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            LocalDateTime startDateTime = LocalDateTime.parse(request.getStartDate() + " 00:00:00", DATE_TIME_FORMATTER);
            queryWrapper.ge(AuditLog::getCreatedAt, startDateTime);
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            LocalDateTime endDateTime = LocalDateTime.parse(request.getEndDate() + " 23:59:59", DATE_TIME_FORMATTER);
            queryWrapper.le(AuditLog::getCreatedAt, endDateTime);
        }

        queryWrapper.orderByDesc(AuditLog::getCreatedAt);
        
        return queryWrapper;
    }

    private AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction() != null ? AuditAction.valueOf(auditLog.getAction()) : null)
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .requestData(auditLog.getRequestData())
                .responseData(auditLog.getResponseData())
                .status(auditLog.getStatus() != null ? AuditStatus.valueOf(auditLog.getStatus()) : null)
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
