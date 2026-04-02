package yuuine.docmind.core.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yuuine.docmind.core.chat.valueobject.MessageRole;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long sessionId;
    private MessageRole role;
    private String content;
    private String retrievedDocs;
    private LocalDateTime createdAt;
}
