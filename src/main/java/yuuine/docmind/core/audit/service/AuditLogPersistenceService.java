package yuuine.docmind.core.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import yuuine.docmind.core.audit.model.AuditLog;
import yuuine.docmind.core.audit.repository.AuditLogRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogPersistenceService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;
    private static final String FALLBACK_DIR = "logs/audit-fallback";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AuditLogRepository auditLogRepository;

    @Async("auditLogExecutor")
    public void saveAsync(AuditLog auditLog) {
        saveWithRetries(auditLog);
    }

    public void saveSync(AuditLog auditLog) {
        saveWithRetries(auditLog);
    }

    private void saveWithRetries(AuditLog auditLog) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                auditLogRepository.insert(auditLog);
                log.debug("Audit log saved successfully for action: {}", auditLog.getAction());
                return;
            } catch (Exception e) {
                log.warn("Failed to save audit log (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to save audit log after {} attempts, writing to fallback file. Action: {}, Resource: {}",
                MAX_RETRIES, auditLog.getAction(), auditLog.getResourceType());
        writeToFallbackFile(auditLog);
    }

    private synchronized void writeToFallbackFile(AuditLog auditLog) {
        File dir = new File(FALLBACK_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "audit-fallback-" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".log";
        File file = new File(dir, fileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            writer.println(LocalDateTime.now() + " | " + auditLog);
        } catch (IOException e) {
            log.error("CRITICAL: Failed to write audit log to fallback file: {}", e.getMessage());
        }
    }
}
