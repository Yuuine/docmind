package yuuine.docmind.core.document.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SentenceChunkingStrategy implements ChunkingStrategy {

    private final int maxSegmentSize;
    private final int maxOverlapSize;

    @Override
    public List<TextSegment> split(Document document) {
        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(maxSegmentSize, maxOverlapSize);
        return splitter.split(document);
    }

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.SENTENCE;
    }
}
