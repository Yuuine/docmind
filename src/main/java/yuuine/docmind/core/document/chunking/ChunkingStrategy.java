package yuuine.docmind.core.document.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface ChunkingStrategy {

    List<TextSegment> split(Document document);

    ChunkingStrategyType getType();
}
