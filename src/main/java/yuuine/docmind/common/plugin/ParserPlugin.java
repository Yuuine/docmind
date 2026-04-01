package yuuine.docmind.common.plugin;

import java.io.InputStream;
import java.util.List;

public interface ParserPlugin {
    String getName();
    List<String> getSupportedFormats();
    String parse(InputStream inputStream, String filename);
    List<String> parseChunks(InputStream inputStream, String filename, int chunkSize, int overlap);
}
