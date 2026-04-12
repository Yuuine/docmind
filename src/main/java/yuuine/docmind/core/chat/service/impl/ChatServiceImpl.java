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
import yuuine.docmind.plugin.python.PythonVectorStorePlugin;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        boolean ragOn = !Boolean.FALSE.equals(request.getRagEnabled());
        String systemPrompt = ragOn
                ? ragPromptProperties.getSystem()
                : ragPromptProperties.getSystemWithoutRag();
        String context = ragOn ? retrieveContext(userId, request.getContent()) : "";
        final String finalContext = context;

        // 使用 PromptAssembler 构建消息列表
        List<Map<String, String>> assembledMessages = promptAssembler.assemble(
            systemPrompt,
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
            .doOnNext(chunk -> log.trace("收到LlmChunk: content={}, done={}, error={}",
                chunk.getContent(), chunk.isDone(), chunk.getError()))
            .map(chunk -> {
                if (chunk.getError() != null) {
                    try {
                        String data = objectMapper.writeValueAsString(Map.of("error", chunk.getError()));
                        log.trace("发送SSE error: {}", data);
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
                        log.trace("发送SSE done: {}", data);
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
                    log.trace("发送SSE content: {}", data);
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
                log.info("RAG检索: 用户没有上传任何文档");
                return "";
            }

            int topK = ragRetrievalProperties.getTopK();
            float[] queryEmbedding = embeddingPlugin.embed(query);
            List<VectorStorePlugin.SearchResult> searchResults = vectorStorePlugin.search(
                    query, queryEmbedding, topK, allowedFileIds);

            if (searchResults == null || searchResults.isEmpty()) {
                log.info("RAG检索: 未找到任何相关文档");
                return "";
            }

            int minLen = ragRetrievalProperties.getMinChunkContentLength();
            if (minLen > 0) {
                int before = searchResults.size();
                searchResults = searchResults.stream()
                        .filter(r -> r.content() != null && r.content().length() >= minLen)
                        .toList();
                log.debug("RAG 按最短 chunk 过滤: {} -> {}", before, searchResults.size());
            }
            Double minScore = ragRetrievalProperties.getMinHitScore();
            if (minScore != null) {
                int before = searchResults.size();
                searchResults = searchResults.stream()
                        .filter(r -> r.score() >= minScore)
                        .toList();
                log.debug("RAG 按 score 下限过滤: {} -> {} (min={})", before, searchResults.size(), minScore);
            }

            if (searchResults.isEmpty()) {
                log.info("RAG检索: 过滤后无可用片段");
                return "";
            }

            double[] scoresForTop1 = searchResults.stream()
                    .mapToDouble(VectorStorePlugin.SearchResult::score)
                    .toArray();
            double rawTop1 = java.util.Arrays.stream(scoresForTop1).max().orElse(0.0);
            if (ragRetrievalProperties.isRawTop1GateEnabled()
                    && rawTop1 < ragRetrievalProperties.getMinRawTop1ForContext()) {
                log.info("RAG置信门控: Top1原始分={} < 下限={}，不注入上下文",
                        rawTop1, ragRetrievalProperties.getMinRawTop1ForContext());
                return "";
            }

            double[] confidenceResult = calculateConfidenceScore(searchResults);
            double confidenceScore = confidenceResult[0];
            double topKAvg = confidenceResult[1];
            double top1Score = confidenceResult[2];
            double stdDev = confidenceResult[3];
            double countScore = confidenceResult[4];
            double normalizedTop1 = confidenceResult[5];

            boolean isConfident = confidenceScore > ragRetrievalProperties.getConfidenceThreshold();

            double consistencyScore;
            double jaccardSimilarity;
            double rankOverlapValue;

            if (ragRetrievalProperties.isConsistencyEnabled()) {
                double[] consistencyResult = calculateConsistencyScore(vectorStorePlugin, searchResults);
                consistencyScore = consistencyResult[0];
                jaccardSimilarity = consistencyResult[1];
                rankOverlapValue = consistencyResult[2];

                if (isConfident) {
                    log.info("RAG置信度: 原始置信={} 已达标, 一致性={}, Jaccard={}, RankOverlap={}, 保持原判定",
                            confidenceScore, consistencyScore, jaccardSimilarity, rankOverlapValue);
                } else {
                    double boostedConfidence = confidenceScore + 0.15 * consistencyScore;
                    isConfident = boostedConfidence > ragRetrievalProperties.getConsistencyThreshold();
                    log.info("RAG置信度: 原始置信={} 不足, 加权后={}, 阈值={}, 置信={}, "
                                    + "Top1原始={}, Top-K均值={}, 标准差={}, Jaccard={}, RankOverlap={}",
                            confidenceScore, boostedConfidence,
                            ragRetrievalProperties.getConsistencyThreshold(), isConfident,
                            top1Score, topKAvg, stdDev, jaccardSimilarity, rankOverlapValue);
                }
            } else {
                log.info("RAG检索完成: 查询='{}', 结果数={}, 置信度={}, 阈值={}, 置信={}, "
                                + "Top1原始={}, Top1归一={}, Top-K均值={}, 标准差={}, 数量得分={}",
                        query, searchResults.size(), confidenceScore,
                        ragRetrievalProperties.getConfidenceThreshold(), isConfident,
                        top1Score, normalizedTop1, topKAvg, stdDev, countScore);
            }

            if (!isConfident) {
                log.info("RAG检索结果置信度低于阈值，返回空上下文");
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

    private double[] calculateConfidenceScore(List<VectorStorePlugin.SearchResult> searchResults) {
        int size = searchResults.size();
        double[] scores = searchResults.stream()
                .mapToDouble(VectorStorePlugin.SearchResult::score)
                .toArray();

        int topK = Math.min(ragRetrievalProperties.getConfidenceTopK(), size);
        double topKAvg = 0.0;
        if (topK > 0) {
            DoubleSummaryStatistics topKStats = java.util.Arrays.stream(scores)
                    .sorted()
                    .skip(size - topK)
                    .summaryStatistics();
            topKAvg = topKStats.getAverage();
        }

        double top1Score = java.util.Arrays.stream(scores).max().orElse(scores.length > 0 ? scores[0] : -100.0);

        double stdDev = 0.0;
        if (size > 1) {
            double mean = java.util.Arrays.stream(scores).average().orElse(0.0);
            stdDev = Math.sqrt(java.util.Arrays.stream(scores)
                    .map(s -> Math.pow(s - mean, 2))
                    .average()
                    .orElse(0.0));
        }

        double minRange = ragRetrievalProperties.getScoreRangeMin();
        double maxRange = ragRetrievalProperties.getScoreRangeMax();
        double range = maxRange - minRange;

        double normalizedTopKAvg = range > 0 ? (topKAvg - minRange) / range : 0.5;
        normalizedTopKAvg = Math.max(0.0, Math.min(1.0, normalizedTopKAvg));

        double normalizedTop1 = range > 0 ? (top1Score - minRange) / range : 0.5;
        normalizedTop1 = Math.max(0.0, Math.min(1.0, normalizedTop1));

        double normalizedStdDev = range > 0 ? stdDev / range : 0.0;
        normalizedStdDev = Math.max(0.0, Math.min(1.0, normalizedStdDev));

        int minResults = ragRetrievalProperties.getMinResults();
        double countScore = size >= minResults ? 1.0 : (double) size / minResults;

        double confidenceScore =
                ragRetrievalProperties.getTopKWeight() * normalizedTopKAvg +
                ragRetrievalProperties.getTop1Weight() * normalizedTop1 -
                ragRetrievalProperties.getStddevWeight() * normalizedStdDev +
                ragRetrievalProperties.getCountWeight() * countScore;

        if (top1Score < ragRetrievalProperties.getMinTop1Score()) {
            confidenceScore *= 0.5;
        }

        return new double[]{confidenceScore, topKAvg, top1Score, stdDev, countScore, normalizedTop1};
    }

    private double[] calculateConsistencyScore(VectorStorePlugin vectorStorePlugin,
            List<VectorStorePlugin.SearchResult> searchResults) {
        if (!(vectorStorePlugin instanceof PythonVectorStorePlugin pythonPlugin)) {
            log.warn("一致性检测仅支持 PythonVectorStorePlugin");
            return new double[]{1.0, 1.0, 1.0};
        }

        List<String> sources = pythonPlugin.getLastHybridSources();
        if (sources.isEmpty()) {
            log.warn("一致性检测: 无来源信息，跳过");
            return new double[]{0.0, 0.0, 0.0};
        }

        List<String> vectorResults = new ArrayList<>();
        List<String> bm25Results = new ArrayList<>();

        for (int i = 0; i < searchResults.size() && i < sources.size(); i++) {
            String chunkId = searchResults.get(i).chunkId();
            String source = sources.get(i);
            if ("vector".equals(source) || "both".equals(source)) {
                if (!vectorResults.contains(chunkId)) {
                    vectorResults.add(chunkId);
                }
            }
            if ("bm25".equals(source) || "both".equals(source)) {
                if (!bm25Results.contains(chunkId)) {
                    bm25Results.add(chunkId);
                }
            }
        }

        Set<String> intersection = new HashSet<>(vectorResults);
        intersection.retainAll(bm25Results);

        Set<String> union = new HashSet<>(vectorResults);
        union.addAll(bm25Results);

        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        double rankOverlap = calculateRankOverlap(vectorResults, bm25Results);

        double consistency = ragRetrievalProperties.getJaccardWeight() * jaccard +
                ragRetrievalProperties.getRankOverlapWeight() * rankOverlap;

        log.debug("一致性检测详情: 向量结果={}, BM25结果={}, 交集={}, 并集={}, Jaccard={}, RankOverlap={}, 综合={}",
                vectorResults.size(), bm25Results.size(), intersection.size(), union.size(), jaccard, rankOverlap, consistency);

        return new double[]{consistency, jaccard, rankOverlap};
    }

    private double calculateRankOverlap(List<String> vectorResults, List<String> bm25Results) {
        if (vectorResults.isEmpty() || bm25Results.isEmpty()) {
            return 0.0;
        }

        List<String> intersection = new ArrayList<>(vectorResults);
        intersection.retainAll(bm25Results);

        if (intersection.isEmpty()) {
            return 0.0;
        }

        double overlapScore = 0.0;
        for (String chunkId : intersection) {
            int vectorRank = findRank(vectorResults, chunkId);
            int bm25Rank = findRank(bm25Results, chunkId);
            if (vectorRank > 0 && bm25Rank > 0) {
                overlapScore += 1.0 / (vectorRank + bm25Rank);
            }
        }

        return Math.min(1.0, overlapScore * 2);
    }

    private int findRank(List<String> list, String chunkId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(chunkId)) {
                return i + 1;
            }
        }
        return Integer.MAX_VALUE;
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
