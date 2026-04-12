package yuuine.docmind.core.document.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ParagraphChunkingStrategy implements ChunkingStrategy {

    private final int maxSegmentSize;
    private final int maxOverlapSize;

    @Override
    public List<TextSegment> split(Document document) {
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(maxSegmentSize, maxOverlapSize);
        return splitter.split(document);
    }

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.PARAGRAPH;
    }
}
