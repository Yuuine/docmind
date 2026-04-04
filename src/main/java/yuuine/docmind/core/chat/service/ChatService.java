package yuuine.docmind.core.chat.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yuuine.docmind.core.chat.dto.ChatMessageRequest;
import yuuine.docmind.core.chat.dto.ChatMessageResponse;
import yuuine.docmind.core.chat.dto.ChatSessionCreateRequest;
import yuuine.docmind.core.chat.dto.ChatSessionResponse;
import yuuine.docmind.core.chat.dto.ChatSessionUpdateRequest;

import java.util.List;

public interface ChatService {
    ChatSessionResponse createSession(ChatSessionCreateRequest request, Long userId);

    ChatSessionResponse updateSession(Long sessionId, ChatSessionUpdateRequest request, Long userId);

    ChatMessageResponse sendMessage(ChatMessageRequest request, Long userId);

    SseEmitter sendMessageStream(ChatMessageRequest request, Long userId);

    List<ChatSessionResponse> listSessions(Long userId);

    List<ChatMessageResponse> getSessionMessages(Long sessionId, Long userId);

    void deleteSession(Long sessionId, Long userId);
}
