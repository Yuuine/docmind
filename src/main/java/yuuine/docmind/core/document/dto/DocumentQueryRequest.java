package yuuine.docmind.core.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yuuine.docmind.core.document.valueobject.DocumentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueryRequest {
    private String filename;
    private DocumentStatus status;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
