package yuuine.docmind.core.document.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class HybridChunkingStrategy implements ChunkingStrategy {

    private final int maxSegmentSize;
    private final int maxOverlapSize;
    private final int paragraphThreshold;
    private final int sentenceThreshold;

    @Override
    public List<TextSegment> split(Document document) {
        List<TextSegment> allSegments = new ArrayList<>();

        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(paragraphThreshold, 0);
        List<TextSegment> paragraphSegments = paragraphSplitter.split(document);

        for (TextSegment paragraphSegment : paragraphSegments) {
            String paragraphText = paragraphSegment.text();

            if (paragraphText.length() <= maxSegmentSize) {
                allSegments.add(paragraphSegment);
                continue;
            }

            Document paragraphDoc = Document.from(paragraphText);
            DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(sentenceThreshold, 0);
            List<TextSegment> sentenceSegments = sentenceSplitter.split(paragraphDoc);

            for (TextSegment sentenceSegment : sentenceSegments) {
                String sentenceText = sentenceSegment.text();

                if (sentenceText.length() <= maxSegmentSize) {
                    allSegments.add(sentenceSegment);
                    continue;
                }

                Document sentenceDoc = Document.from(sentenceText);
                DocumentByWordSplitter wordSplitter = new DocumentByWordSplitter(maxSegmentSize, maxOverlapSize);
                allSegments.addAll(wordSplitter.split(sentenceDoc));
            }
        }

        return mergeSmallSegments(allSegments);
    }

    private List<TextSegment> mergeSmallSegments(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }

        List<TextSegment> merged = new ArrayList<>();
        TextSegment current = null;

        for (TextSegment segment : segments) {
            if (current == null) {
                current = segment;
            } else {
                int combinedLength = current.text().length() + segment.text().length() + 1;
                if (combinedLength <= maxSegmentSize) {
                    current = TextSegment.from(current.text() + "\n" + segment.text());
                } else {
                    merged.add(current);
                    current = segment;
                }
            }
        }

        if (current != null) {
            merged.add(current);
        }

        return merged;
    }

    @Override
    public ChunkingStrategyType getType() {
        return ChunkingStrategyType.HYBRID;
    }
}
