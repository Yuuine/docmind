package yuuine.docmind.core.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@EnableAsync
@Configuration
public class AuditLogAsyncConfig {

    @Value("${audit.log.thread-pool.core-size:4}")
    private int corePoolSize;

    @Value("${audit.log.thread-pool.max-size:10}")
    private int maxPoolSize;

    @Value("${audit.log.thread-pool.queue-capacity:500}")
    private int queueCapacity;

    @Value("${audit.log.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.error("Audit log task rejected, thread pool exhausted. Pending tasks: {}, Active count: {}",
                    e.getQueue().size(), e.getActiveCount());
            throw new RejectedExecutionException();
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
