package yuuine.docmind.plugin.parser;

import lombok.extern.slf4j.Slf4j;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ParserPluginFactory {

    private final Map<String, ParserPlugin> parserMap = new ConcurrentHashMap<>();

    public ParserPluginFactory(List<ParserPlugin> parsers) {
        for (ParserPlugin parser : parsers) {
            for (String format : parser.getSupportedFormats()) {
                parserMap.put(format.toLowerCase(), parser);
            }
        }
        log.info("已加载解析器: {}", parserMap.keySet());
    }

    public Optional<ParserPlugin> getParser(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parserMap.get(extension.toLowerCase()));
    }

    public Optional<ParserPlugin> getParserByContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> Optional.ofNullable(parserMap.get("pdf"));
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    Optional.ofNullable(parserMap.get("docx"));
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    Optional.ofNullable(parserMap.get("xlsx"));
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    Optional.ofNullable(parserMap.get("pptx"));
            case "text/plain" -> Optional.ofNullable(parserMap.get("txt"));
            default -> Optional.empty();
        };
    }

/**
     * 获取文件扩展名
     * <p>
     * 从给定的文件名中提取文件扩展名（不包含点号），例如 "document.pdf" 返回 "pdf"。
     * 如果文件名为空或不包含扩展名，则返回 null。
     *
     * @param filename 文件名，可以为 null
     * @return 文件的小写扩展名，如果无法获取则返回 null
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
