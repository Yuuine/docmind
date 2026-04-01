package yuuine.docmind.core.document.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_chunks")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("chunk_id")
    private String chunkId;

    @TableField("document_id")
    private Long documentId;

    @TableField("chunk_index")
    private int chunkIndex;

    @TableField("content")
    private String content;

    @TableField("char_count")
    private int charCount;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
