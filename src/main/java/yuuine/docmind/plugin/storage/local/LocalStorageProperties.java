package yuuine.docmind.plugin.storage.local;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "docmind.storage.local")
public class LocalStorageProperties {
    private String basePath = "./data/storage";
}
