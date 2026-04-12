package yuuine.docmind.core.document.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.docmind.core.document.config.DocumentParserProperties;
import yuuine.docmind.core.document.service.DocumentChunkingService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkingServiceImpl implements DocumentChunkingService {

    private final DocumentParserProperties properties;

    @Override
    public List<String> chunk(String text) {
        return chunk(text, properties.getMaxSegmentSize(), properties.getMaxOverlapSize());
    }

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                int lastSplit = findGoodSplitPoint(text, start, end);
                if (lastSplit > start) {
                    end = lastSplit;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = end - overlap;
            if (start < 0) start = 0;
        }

        log.debug("文本分块完成: 原始长度={}, 分块数量={}", length, chunks.size());
        return chunks;
    }

    private int findGoodSplitPoint(String text, int start, int end) {
        String[] separators = {"\n\n", "\n", ". ", "! ", "? ", "。", "！", "？", " ", "\t"};
        for (String separator : separators) {
            int lastIndex = text.lastIndexOf(separator, end);
            if (lastIndex > start) {
                return lastIndex + separator.length();
            }
        }
        return end;
    }
}
