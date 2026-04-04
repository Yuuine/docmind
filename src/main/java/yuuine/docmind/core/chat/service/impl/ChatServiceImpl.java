package yuuine.docmind.core.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.chat.dto.*;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.model.ChatSession;
import yuuine.docmind.core.chat.repository.ChatMessageRepository;
import yuuine.docmind.core.chat.repository.ChatSessionRepository;
import yuuine.docmind.core.chat.valueobject.MessageRole;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements yuuine.docmind.core.chat.service.ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

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
    public SseEmitter sendMessageStream(ChatMessageRequest request, Long userId) {
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

        SseEmitter emitter = new SseEmitter(180_000L);

        streamExecutor.execute(() -> {
            try {
                String fullContent = "RAG检索功能待实现，当前为流式输出模拟。";
                StringBuilder accumulatedContent = new StringBuilder();

                for (int i = 0; i < fullContent.length(); i++) {
                    String chunk = String.valueOf(fullContent.charAt(i));
                    accumulatedContent.append(chunk);

                    String data = objectMapper.writeValueAsString(Map.of("content", chunk));
                    emitter.send(SseEmitter.event().data(data));

                    Thread.sleep(30);
                }

                ChatMessage assistantMessage = ChatMessage.builder()
                        .sessionId(request.getSessionId())
                        .role(MessageRole.ASSISTANT)
                        .content(accumulatedContent.toString())
                        .retrievedDocs("[]")
                        .build();
                chatMessageRepository.insert(assistantMessage);

                String doneData = objectMapper.writeValueAsString(Map.of("done", true));
                emitter.send(SseEmitter.event().data(doneData));

                emitter.complete();
            } catch (IOException e) {
                log.error("SSE流式输出IO异常, sessionId={}", request.getSessionId(), e);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                log.warn("SSE流式输出被中断, sessionId={}", request.getSessionId(), e);
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("SSE流式输出异常, sessionId={}", request.getSessionId(), e);
                try {
                    String errorData = objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorData));
                } catch (IOException ioEx) {
                    log.error("发送SSE错误事件失败", ioEx);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
