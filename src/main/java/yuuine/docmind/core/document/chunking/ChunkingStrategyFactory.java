package yuuine.docmind.core.document.chunking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.core.document.config.DocumentParserProperties;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkingStrategyFactory {

    private final DocumentParserProperties properties;

    private final Map<ChunkingStrategyType, ChunkingStrategy> strategyCache = new HashMap<>();

    public ChunkingStrategy getStrategy(ChunkingStrategyType type) {
        return strategyCache.computeIfAbsent(type, this::createStrategy);
    }

    public ChunkingStrategy getDefaultStrategy() {
        return getStrategy(properties.getChunkingStrategy());
    }

    public ChunkingStrategy recommendStrategy(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return getDefaultStrategy();
        }

        return switch (extension.toLowerCase()) {
            case "pdf" -> getStrategy(ChunkingStrategyType.SENTENCE);
            case "docx", "doc" -> getStrategy(ChunkingStrategyType.PARAGRAPH);
            case "xlsx", "xls" -> getStrategy(ChunkingStrategyType.LINE);
            case "pptx", "ppt" -> getStrategy(ChunkingStrategyType.PARAGRAPH);
            case "txt", "md" -> getStrategy(ChunkingStrategyType.HYBRID);
            default -> getDefaultStrategy();
        };
    }

    private ChunkingStrategy createStrategy(ChunkingStrategyType type) {
        int maxSize = properties.getMaxSegmentSize();
        int maxOverlap = properties.getMaxOverlapSize();

        return switch (type) {
            case CHARACTER -> new CharacterChunkingStrategy(maxSize, maxOverlap);
            case WORD -> new WordChunkingStrategy(maxSize, maxOverlap);
            case SENTENCE -> new SentenceChunkingStrategy(maxSize, maxOverlap);
            case PARAGRAPH -> new ParagraphChunkingStrategy(maxSize, maxOverlap);
            case LINE -> new LineChunkingStrategy(maxSize, maxOverlap);
            case REGEX -> new RegexChunkingStrategy(maxSize, maxOverlap, properties.getRegex().getPattern());
            case HYBRID -> new HybridChunkingStrategy(
                    maxSize,
                    maxOverlap,
                    properties.getHybrid().getParagraphThreshold(),
                    properties.getHybrid().getSentenceThreshold()
            );
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
