package yuuine.docmind.core.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.chat.dto.*;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.model.ChatSession;
import yuuine.docmind.core.chat.repository.ChatMessageRepository;
import yuuine.docmind.core.chat.repository.ChatSessionRepository;
import yuuine.docmind.core.chat.valueobject.MessageRole;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements yuuine.docmind.core.chat.service.ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatSessionResponse createSession(ChatSessionCreateRequest request, Long userId) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title(request.getTitle() != null ? request.getTitle() : "新会话")
                .build();

        chatSessionRepository.insert(session);
        return toSessionResponse(session);
    }

    @Override
    public ChatSessionResponse updateSession(Long sessionId, ChatSessionUpdateRequest request, Long userId) {
        ChatSession session = chatSessionRepository.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此会话");
        }

        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        chatSessionRepository.updateById(session);

        return toSessionResponse(session);
    }

    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Long userId) {
        ChatSession session = chatSessionRepository.selectById(request.getSessionId());
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
        }

        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .role(MessageRole.USER)
                .content(request.getContent())
                .build();
        chatMessageRepository.insert(userMessage);

        String assistantContent = "RAG检索功能待实现";
        ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .role(MessageRole.ASSISTANT)
                .content(assistantContent)
                .retrievedDocs("[]")
                .build();
        chatMessageRepository.insert(assistantMessage);

        return toMessageResponse(assistantMessage);
    }

    @Override
    public List<ChatSessionResponse> listSessions(Long userId) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt);

        return chatSessionRepository.selectList(queryWrapper).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    public List<ChatMessageResponse> getSessionMessages(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt);

        return chatMessageRepository.selectList(queryWrapper).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此会话");
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageRepository.delete(queryWrapper);

        chatSessionRepository.deleteById(sessionId);
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .retrievedDocs(message.getRetrievedDocs())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
