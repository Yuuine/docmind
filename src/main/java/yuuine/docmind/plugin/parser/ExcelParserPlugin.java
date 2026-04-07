package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExcelParserPlugin implements ParserPlugin {

    @Override
    public String getName() {
        return "excel-parser";
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("xlsx", "xls");
    }

    @Override
    public String parse(InputStream inputStream, String filename) {
        StringBuilder text = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (Sheet sheet : workbook) {
                text.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = getCellValue(cell);
                        if (!cellValue.isBlank()) {
                            rowText.append(cellValue).append("\t");
                        }
                    }
                    if (rowText.length() > 0) {
                        text.append(rowText).append("\n");
                    }
                }
                text.append("\n");
            }
        } catch (IOException e) {
            log.error("Excel文档解析失败: {}", filename, e);
            throw new RuntimeException("Excel文档解析失败", e);
        }
        return text.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
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
