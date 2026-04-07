package yuuine.docmind.core.document.service;

import java.util.List;

public interface DocumentChunkingService {

    List<String> chunk(String text);

    List<String> chunk(String text, int chunkSize, int overlap);
}
