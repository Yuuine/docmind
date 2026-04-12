package yuuine.docmind.plugin.python;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "docmind.embedding.python")
public class PythonEmbeddingProperties {
    private String url = "http://localhost:8001";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
}
