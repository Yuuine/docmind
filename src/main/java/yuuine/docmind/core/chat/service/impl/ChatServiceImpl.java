package yuuine.docmind.core.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.springframework.stereotype.Service;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.chat.dto.*;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.model.ChatSession;
import yuuine.docmind.core.chat.repository.ChatMessageRepository;
import yuuine.docmind.core.chat.repository.ChatSessionRepository;
import yuuine.docmind.core.chat.service.LlmService;
import yuuine.docmind.core.chat.valueobject.MessageRole;
import yuuine.docmind.core.model.entity.AIModel;
import yuuine.docmind.core.model.repository.AIModelRepository;
import yuuine.docmind.common.plugin.EmbeddingPlugin;
import yuuine.docmind.common.plugin.RerankPlugin;
import yuuine.docmind.common.plugin.VectorStorePlugin;
import yuuine.docmind.core.chat.config.RagPromptProperties;
import yuuine.docmind.core.chat.config.RagRetrievalProperties;
import yuuine.docmind.core.chat.service.PromptAssembler;
import yuuine.docmind.core.document.model.Document;
import yuuine.docmind.core.document.repository.DocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ChatServiceImpl implements yuuine.docmind.core.chat.service.ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIModelRepository aiModelRepository;
    private final ObjectMapper objectMapper;
    private final LlmService llmService;
    private final VectorStorePlugin vectorStorePlugin;
    private final EmbeddingPlugin embeddingPlugin;
    private final RerankPlugin rerankPlugin;
    private final PromptAssembler promptAssembler;
    private final RagPromptProperties ragPromptProperties;
    private final RagRetrievalProperties ragRetrievalProperties;
    private final DocumentRepository documentRepository;
    private final Executor ragTaskExecutor;

    public ChatServiceImpl(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AIModelRepository aiModelRepository,
            ObjectMapper objectMapper,
            LlmService llmService,
            VectorStorePlugin vectorStorePlugin,
            EmbeddingPlugin embeddingPlugin,
            RerankPlugin rerankPlugin,
            PromptAssembler promptAssembler,
            RagPromptProperties ragPromptProperties,
            RagRetrievalProperties ragRetrievalProperties,
            DocumentRepository documentRepository,
            @Qualifier("ragTaskExecutor") Executor ragTaskExecutor
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiModelRepository = aiModelRepository;
        this.objectMapper = objectMapper;
        this.llmService = llmService;
        this.vectorStorePlugin = vectorStorePlugin;
        this.embeddingPlugin = embeddingPlugin;
        this.rerankPlugin = rerankPlugin;
        this.promptAssembler = promptAssembler;
        this.ragPromptProperties = ragPromptProperties;
        this.ragRetrievalProperties = ragRetrievalProperties;
        this.documentRepository = documentRepository;
        this.ragTaskExecutor = ragTaskExecutor;
    }

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
        // TODO: 实现非流式发送消息
        // 1. 复用 sendMessageStream 的 RAG 流程 (retrieveContext, promptAssembler)
        // 2. 同步等待 LLM 完整响应，可使用 Flux.blockLast() 或 collectList().block()
        // 3. 保存用户消息和助手响应到数据库
        // 4. 返回完整的 ChatMessageResponse
        // 注意：非流式接口会阻塞线程，建议优先使用流式接口
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

        log.warn("非流式发送消息暂不支持完整RAG流程，请使用流式接口");
        throw new BusinessException(ErrorCode.LLM_ERROR, "非流式发送暂不可用，请使用流式聊天接口");
    }

    @Override
    public Flux<ServerSentEvent<String>> sendMessageStream(ChatMessageRequest request, Long userId) {
        return Flux.defer(() -> sendMessageStreamPipeline(request, userId))
                .subscribeOn(Schedulers.fromExecutor(ragTaskExecutor));
    }

    private Flux<ServerSentEvent<String>> sendMessageStreamPipeline(ChatMessageRequest request, Long userId) {
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

        LambdaQueryWrapper<AIModel> modelQuery = new LambdaQueryWrapper<>();
        modelQuery.eq(AIModel::getUserId, userId)
                .eq(AIModel::getIsActive, true)
                .orderByDesc(AIModel::getUpdatedAt)
                .last("LIMIT 1");
        AIModel activeModel = aiModelRepository.selectOne(modelQuery);

        if (activeModel == null) {
            try {
                return Flux.just(
                    ServerSentEvent.<String>builder()
                        .data(objectMapper.writeValueAsString(Map.of("error", "未配置可用的AI模型，请先添加并激活一个模型")))
                        .build()
                );
            } catch (Exception e) {
                return Flux.just(
                    ServerSentEvent.<String>builder().data("{\"error\":\"未配置可用的AI模型\"}").build()
                );
            }
        }

        LambdaQueryWrapper<ChatMessage> msgQuery = new LambdaQueryWrapper<>();
        msgQuery.eq(ChatMessage::getSessionId, request.getSessionId())
                .orderByAsc(ChatMessage::getCreatedAt);
        List<ChatMessage> chatMessages = chatMessageRepository.selectList(msgQuery);

        // 历史消息包含所有消息（包括刚插入的用户消息）
        // PromptAssembler 会正确处理消息截断
        List<ChatMessage> historyMessages = new ArrayList<>(chatMessages);

        String context = retrieveContext(userId, request.getContent());
        final String finalContext = context;

        // 使用 PromptAssembler 构建消息列表
        List<Map<String, String>> assembledMessages = promptAssembler.assemble(
            ragPromptProperties.getSystem(),
            historyMessages,
            context,
            request.getContent(),
            ragPromptProperties.getMaxHistoryRounds()
        );

        List<ChatMessage> llmMessages = assembledMessages.stream()
            .map(msg -> ChatMessage.builder()
                .role(MessageRole.valueOf(msg.get("role").toUpperCase()))
                .content(msg.get("content"))
                .build())
            .toList();

        StringBuilder accumulatedContent = new StringBuilder();

        return llmService.streamChat(activeModel, llmMessages)
            .doOnNext(chunk -> log.info("收到LlmChunk: content={}, done={}, error={}", 
                chunk.getContent(), chunk.isDone(), chunk.getError()))
            .map(chunk -> {
                if (chunk.getError() != null) {
                    try {
                        String data = objectMapper.writeValueAsString(Map.of("error", chunk.getError()));
                        log.info("发送SSE error: {}", data);
                        return ServerSentEvent.<String>builder()
                            .data(data)
                            .build();
                    } catch (Exception e) {
                        log.error("构造error SSE失败", e);
                        return ServerSentEvent.<String>builder().data("{\"error\":\"解析错误\"}").build();
                    }
                }
                if (chunk.isDone()) {
                    try {
                        String data = objectMapper.writeValueAsString(Map.of("done", true));
                        log.info("发送SSE done: {}", data);
                        return ServerSentEvent.<String>builder()
                            .data(data)
                            .build();
                    } catch (Exception e) {
                        log.error("构造done SSE失败", e);
                        return ServerSentEvent.<String>builder().data("{\"done\":true}").build();
                    }
                }
                accumulatedContent.append(chunk.getContent());
                try {
                    String data = objectMapper.writeValueAsString(Map.of("content", chunk.getContent()));
                    log.info("发送SSE content: {}", data);
                    return ServerSentEvent.<String>builder()
                        .data(data)
                        .build();
                } catch (Exception e) {
                    log.error("构造content SSE失败", e);
                    return ServerSentEvent.<String>builder().data("{\"content\":\"" + chunk.getContent() + "\"}").build();
                }
            })
            .doOnComplete(() -> {
                if (!accumulatedContent.isEmpty()) {
                    try {
                        String retrievedDocsJson = buildRetrievedDocsJson(finalContext);
                        ChatMessage assistantMessage = ChatMessage.builder()
                                .sessionId(request.getSessionId())
                                .role(MessageRole.ASSISTANT)
                                .content(accumulatedContent.toString())
                                .retrievedDocs(retrievedDocsJson)
                                .build();
                        chatMessageRepository.insert(assistantMessage);
                    } catch (Exception e) {
                        log.error("保存助手消息失败, error={}", e.getMessage(), e);
                    }
                }
            })
            .onErrorResume(e -> {
                log.error("消息流处理异常, sessionId={}, error={}", request.getSessionId(), e.getMessage(), e);
                try {
                    return Flux.just(
                        ServerSentEvent.<String>builder()
                            .data(objectMapper.writeValueAsString(Map.of("error", "服务器内部错误: " + e.getMessage())))
                            .build()
                    );
                } catch (Exception ex) {
                    return Flux.just(
                        ServerSentEvent.<String>builder().data("{\"error\":\"服务器内部错误\"}").build()
                    );
                }
            });
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

    private String retrieveContext(Long userId, String query) {
        try {
            LambdaQueryWrapper<Document> docQuery = new LambdaQueryWrapper<>();
            docQuery.eq(Document::getUserId, userId).select(Document::getId);
            List<Document> userDocs = documentRepository.selectList(docQuery);
            List<String> allowedFileIds = userDocs.stream()
                    .map(d -> String.valueOf(d.getId()))
                    .toList();
            if (allowedFileIds.isEmpty()) {
                return "";
            }

            int topK = ragRetrievalProperties.getTopK();
            float[] queryEmbedding = embeddingPlugin.embed(query);
            List<VectorStorePlugin.SearchResult> searchResults = vectorStorePlugin.search(
                    query, queryEmbedding, topK, allowedFileIds);

            if (searchResults == null || searchResults.isEmpty()) {
                return "";
            }

            List<String> documents = searchResults.stream()
                .map(VectorStorePlugin.SearchResult::content)
                .toList();

            List<RerankPlugin.RerankResult> rerankedDocs = null;
            if (rerankPlugin != null
                    && ragRetrievalProperties.isRerankEnabled()
                    && !documents.isEmpty()) {
                int rerankK = Math.min(ragRetrievalProperties.getRerankTopK(), documents.size());
                rerankedDocs = rerankPlugin.rerank(query, documents, rerankK);
            }

            StringBuilder contextBuilder = new StringBuilder();
            if (rerankedDocs != null && !rerankedDocs.isEmpty()) {
                for (int i = 0; i < rerankedDocs.size(); i++) {
                    RerankPlugin.RerankResult result = rerankedDocs.get(i);
                    contextBuilder.append(String.format("[文档%d] %s\n", i + 1, result.document()));
                }
            } else {
                for (int i = 0; i < searchResults.size(); i++) {
                    VectorStorePlugin.SearchResult result = searchResults.get(i);
                    contextBuilder.append(String.format("[文档%d] %s\n", i + 1, result.content()));
                }
            }

            return contextBuilder.toString().trim();
        } catch (Exception e) {
            log.warn("RAG检索失败, 降级为无RAG模式, error={}", e.getMessage(), e);
            return "";
        }
    }

    private String buildRetrievedDocsJson(String context) {
        if (context == null || context.isBlank()) {
            return "[]";
        }
        try {
            String[] lines = context.split("\n");
            List<Map<String, String>> docs = new java.util.ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    docs.add(Map.of("content", line));
                }
            }
            return objectMapper.writeValueAsString(docs);
        } catch (Exception e) {
            log.warn("构建retrieved_docs JSON失败, 使用原始文本, error={}", e.getMessage());
            try {
                return objectMapper.writeValueAsString(List.of(Map.of("content", context)));
            } catch (Exception ex) {
                return "[]";
            }
        }
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
