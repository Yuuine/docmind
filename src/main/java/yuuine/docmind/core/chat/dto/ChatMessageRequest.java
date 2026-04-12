package yuuine.docmind.core.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long sessionId;
    private String content;
    /** null 或 true：走向量检索并注入参考资料；false：纯对话，不检索、使用 {@code rag.prompt.system-without-rag} */
    private Boolean ragEnabled;
}
