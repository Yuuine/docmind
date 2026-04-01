package yuuine.docmind.plugin.chroma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.plugin.VectorStorePlugin;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChromaVectorStorePlugin implements VectorStorePlugin {

    private final ChromaProperties properties;

    @Override
    public String getName() {
        return "chroma";
    }

    @Override
    public void addChunks(List<VectorChunk> chunks) {
        log.info("Adding {} chunks to ChromaDB collection: {}", chunks.size(), properties.getCollectionName());
    }

    @Override
    public List<SearchResult> search(String query, float[] queryEmbedding, int topK) {
        log.info("Searching in ChromaDB, topK: {}", topK);
        return List.of();
    }

    @Override
    public void deleteByFileId(String fileId) {
        log.info("Deleting chunks from ChromaDB for fileId: {}", fileId);
    }

    @Override
    public void deleteAll() {
        log.info("Deleting all chunks from ChromaDB");
    }
}
