package yuuine.docmind.core.document.service.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.docmind.core.document.chunking.ChunkingStrategy;
import yuuine.docmind.core.document.chunking.ChunkingStrategyFactory;
import yuuine.docmind.core.document.chunking.ChunkingStrategyType;
import yuuine.docmind.core.document.service.LangChain4jDocumentChunkingService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jDocumentChunkingServiceImpl implements LangChain4jDocumentChunkingService {

    private final ChunkingStrategyFactory strategyFactory;

    @Override
    public List<TextSegment> chunk(String text) {
        return chunk(text, strategyFactory.getDefaultStrategy());
    }

    @Override
    public List<TextSegment> chunk(String text, ChunkingStrategyType strategyType) {
        ChunkingStrategy strategy = strategyFactory.getStrategy(strategyType);
        return chunk(text, strategy);
    }

    @Override
    public List<TextSegment> chunk(String text, ChunkingStrategy strategy) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Document document = Document.from(text);
        List<TextSegment> segments = strategy.split(document);

        log.debug("文本分块完成: 策略={}, 原始长度={}, 分块数量={}",
                strategy.getType(), text.length(), segments.size());

        return segments;
    }

    @Override
    public List<String> chunkToStrings(String text) {
        return chunk(text).stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> chunkToStrings(String text, ChunkingStrategyType strategyType) {
        return chunk(text, strategyType).stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());
    }
}
