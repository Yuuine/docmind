package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WordParserPlugin implements ParserPlugin {

    @Override
    public String getName() {
        return "word-parser";
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("docx", "doc");
    }

    @Override
    public String parse(InputStream inputStream, String filename) {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        } catch (IOException e) {
            log.error("Word文档解析失败: {}", filename, e);
            throw new RuntimeException("Word文档解析失败", e);
        }
        return text.toString();
    }

    @Override
    public List<String> parseChunks(InputStream inputStream, String filename, int chunkSize, int overlap) {
        String fullText = parse(inputStream, filename);
        return splitIntoChunks(fullText, chunkSize, overlap);
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - overlap;
            if (start < 0) start = 0;
        }

        return chunks;
    }
}
