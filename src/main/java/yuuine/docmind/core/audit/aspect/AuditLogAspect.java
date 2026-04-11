package yuuine.docmind.core.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.filter.SensitiveDataFilter;
import yuuine.docmind.core.audit.model.AuditLog;
import yuuine.docmind.core.audit.service.AuditLogPersistenceService;
import yuuine.docmind.core.audit.valueobject.AuditStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("yuuine.docmind.core.audit");

    private final AuditLogPersistenceService auditLogPersistenceService;
    private final SensitiveDataFilter sensitiveDataFilter;
    private final ObjectMapper objectMapper;
    private final AuditDataCollector dataCollector;

    @Around("@annotation(yuuine.docmind.core.audit.annotation.Audited)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Audited auditAnnotation = signature.getMethod().getAnnotation(Audited.class);

        var request = dataCollector.getRequest();
        Map<String, Object> requestData = dataCollector.buildRequestData(joinPoint, request, auditAnnotation);

        AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                .action(auditAnnotation.action().name())
                .resourceType(auditAnnotation.resourceType())
                .httpMethod(request != null ? request.getMethod() : null)
                .requestPath(request != null ? request.getRequestURI() : null)
                .queryString(request != null ? request.getQueryString() : null)
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status(AuditStatus.SUCCESS.name())
                .operationDescription(auditAnnotation.describe())
                .serverHost(dataCollector.getServerHost())
                .traceId(dataCollector.getTraceId())
                .createdAt(LocalDateTime.now());

        if (auditAnnotation.logRequest()) {
            auditLogBuilder.requestData(toJson(requestData, auditAnnotation.sensitiveParams()));
        }

        Object resourceId = dataCollector.resolveResourceId(joinPoint, auditAnnotation);
        if (resourceId != null) {
            auditLogBuilder.resourceId(AuditDataCollector.toLong(resourceId));
        }

        auditLogBuilder.userId(dataCollector.resolveUserId(request));

        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            if (result instanceof Result<?> resultData && auditAnnotation.logResponse()) {
                auditLogBuilder.responseData(toJson(resultData, auditAnnotation.sensitiveParams()));
            }
            return result;
        } catch (Throwable e) {
            auditLogBuilder.status(AuditStatus.FAILURE.name());
            auditLogBuilder.errorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            auditLogBuilder.responseData(toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), auditAnnotation.sensitiveParams()));
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            auditLogBuilder.executionTime(executionTime);

            AuditLog auditLog = auditLogBuilder.build();

            logAudit(auditLog);

            if (auditAnnotation.async()) {
                auditLogPersistenceService.saveAsync(auditLog);
            } else {
                auditLogPersistenceService.saveSync(auditLog);
            }
        }
    }

    private String toJson(Object obj, String[] sensitiveParams) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return sensitiveDataFilter.filterSensitiveData(json, sensitiveParams, false);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }

    private void logAudit(AuditLog auditLog) {
        AUDIT_LOGGER.info("[AUDIT] {} - {} - {}ms - {} - {} - {}",
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getExecutionTime(),
                auditLog.getStatus(),
                auditLog.getRequestPath(),
                auditLog.getIpAddress());
    }
}
