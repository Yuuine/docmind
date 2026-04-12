package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TextParserPlugin implements ParserPlugin {

    @Override
    public String getName() {
        return "text-parser";
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("txt", "text");
    }

    @Override
    public String parse(InputStream inputStream, String filename) {
        try {
            return readInputStream(inputStream);
        } catch (IOException e) {
            log.error("Text解析失败: {}", filename, e);
            throw new RuntimeException("Text解析失败", e);
        }
    }

    @Override
    public List<String> parseChunks(InputStream inputStream, String filename, int chunkSize, int overlap) {
        String fullText = parse(inputStream, filename);
        return splitIntoChunks(fullText, chunkSize, overlap);
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int length = text.length();
        if (length <= chunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        int start = 0;
        int lastStart = -1;
        int maxIterations = length / Math.max(1, chunkSize - overlap) + 10;
        int iteration = 0;

        while (start < length && iteration < maxIterations) {
            iteration++;

            if (start == lastStart) {
                start = Math.min(start + 1, length);
                lastStart = start;
                continue;
            }
            lastStart = start;

            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                int lastSplit = findGoodSplitPoint(text, start, end);
                if (lastSplit > start) {
                    end = lastSplit;
                }
            }

            int newStart = start;
            while (newStart < end && Character.isWhitespace(text.charAt(newStart))) {
                newStart++;
            }
            int newEnd = end;
            while (newEnd > newStart && Character.isWhitespace(text.charAt(newEnd - 1))) {
                newEnd--;
            }

            if (newStart < newEnd) {
                String chunk = text.substring(newStart, newEnd);
                chunks.add(chunk);
            }

            if (end >= length) {
                break;
            }

            start = end - overlap;
            if (start < 0) start = 0;
            if (start <= newStart) {
                start = newStart + 1;
            }
        }

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
