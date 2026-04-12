package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PptParserPlugin implements ParserPlugin {

    @Override
    public String getName() {
        return "ppt-parser";
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("pptx", "ppt");
    }

    @Override
    public String parse(InputStream inputStream, String filename) {
        StringBuilder text = new StringBuilder();
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            int slideNumber = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                text.append("Slide ").append(slideNumber).append(":\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String shapeText = textShape.getText();
                        if (!shapeText.isBlank()) {
                            text.append(shapeText).append("\n");
                        }
                    }
                }
                text.append("\n");
                slideNumber++;
            }
        } catch (IOException e) {
            log.error("PowerPoint文档解析失败: {}", filename, e);
            throw new RuntimeException("PowerPoint文档解析失败", e);
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
                int lastNewLine = text.lastIndexOf('\n', end);
                if (lastNewLine > start) {
                    end = lastNewLine;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - overlap;
            if (start < 0) start = 0;
        }

        return chunks;
    }
}
