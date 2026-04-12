package yuuine.docmind.core.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStats {
    private Integer chunkCount;
    private Integer totalCharCount;
    private Integer avgChunkSize;
}
