package yuuine.docmind.core.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docmind.document.upload")
public class DocumentUploadProperties {

    private long maxFileSize = 100 * 1024 * 1024;

}
