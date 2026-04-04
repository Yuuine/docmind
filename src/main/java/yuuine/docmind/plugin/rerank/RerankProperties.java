package yuuine.docmind.plugin.rerank;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "docmind.rerank")
public class RerankProperties {
    private String type = "default";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
}
