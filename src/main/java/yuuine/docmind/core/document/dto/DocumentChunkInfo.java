package yuuine.docmind.core.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkInfo {
    private Long id;
    private String chunkId;
    private Integer chunkIndex;
    private String contentPreview;
    private String content;
    private Integer charCount;
    private LocalDateTime createdAt;
}
