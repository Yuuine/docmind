package yuuine.docmind.core.document.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import yuuine.docmind.core.document.chunking.ChunkingStrategy;
import yuuine.docmind.core.document.chunking.ChunkingStrategyType;

import java.util.List;

public interface LangChain4jDocumentChunkingService {

    List<TextSegment> chunk(String text);

    List<TextSegment> chunk(String text, ChunkingStrategyType strategyType);

    List<TextSegment> chunk(String text, ChunkingStrategy strategy);

    List<String> chunkToStrings(String text);

    List<String> chunkToStrings(String text, ChunkingStrategyType strategyType);
}
