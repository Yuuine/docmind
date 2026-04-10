package yuuine.docmind.core.document.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class DocumentDownloadResponse {
    private String filename;
    private String contentType;
    private Long fileSize;
    private InputStream inputStream;
}
