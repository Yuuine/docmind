package yuuine.docmind.core.chat.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
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

    Flux<ServerSentEvent<String>> sendMessageStream(ChatMessageRequest request, Long userId);

    List<ChatSessionResponse> listSessions(Long userId);

    List<ChatMessageResponse> getSessionMessages(Long sessionId, Long userId);

    void deleteSession(Long sessionId, Long userId);
}
