package yuuine.docmind.core.document.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RegexChunkingStrategy implements ChunkingStrategy {

    private final int maxSegmentSize;
    private final int maxOverlapSize;
    private final String regex;

    @Override
    public List<TextSegment> split(Document document) {
        DocumentByRegexSplitter splitter = new DocumentByRegexSplitter(regex, regex, maxSegmentSize, maxOverlapSize);
        return splitter.split(document);
    }

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.REGEX;
    }
}
