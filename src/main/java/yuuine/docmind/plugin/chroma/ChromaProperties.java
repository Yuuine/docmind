package yuuine.docmind.plugin.chroma;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "docmind.vectorstore.chroma")
public class ChromaProperties {
    private String url = "http://localhost:8000";
    private String collectionName = "docmind_chunks";
    private int dimension = 1024;
}
