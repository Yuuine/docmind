package yuuine.docmind.plugin.embedding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "docmind.embedding")
public class EmbeddingProperties {
    private String type = "default";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private int dimension = 1024;
    private int batchSize = 32;
}
