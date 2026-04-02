package yuuine.docmind.core.chat.service;

import yuuine.docmind.core.chat.dto.ChatMessageRequest;
import yuuine.docmind.core.chat.dto.ChatMessageResponse;
import yuuine.docmind.core.chat.dto.ChatSessionCreateRequest;
import yuuine.docmind.core.chat.dto.ChatSessionResponse;

import java.util.List;

public interface ChatService {
    ChatSessionResponse createSession(ChatSessionCreateRequest request, Long userId);

    ChatMessageResponse sendMessage(ChatMessageRequest request, Long userId);

    List<ChatSessionResponse> listSessions(Long userId);

    List<ChatMessageResponse> getSessionMessages(Long sessionId, Long userId);

    void deleteSession(Long sessionId, Long userId);
}
