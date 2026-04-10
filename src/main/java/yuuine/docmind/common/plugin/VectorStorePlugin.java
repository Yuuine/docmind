package yuuine.docmind.common.plugin;

import java.util.List;

public interface VectorStorePlugin {
    String getName();
    void addChunks(List<VectorChunk> chunks);
    void updateChunks(List<VectorChunk> chunks);
    List<SearchResult> search(String query, float[] queryEmbedding, int topK);
    void deleteByFileId(String fileId);
    void deleteAll();

    record VectorChunk(String chunkId, String fileId, String content, int chunkIndex) {}
    record SearchResult(String chunkId, String fileId, String content, double score, int chunkIndex) {}
}
