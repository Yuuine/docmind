package yuuine.docmind.common.plugin;

import java.util.List;

public interface VectorStorePlugin {
    String getName();
    void addChunks(List<VectorChunk> chunks);
    void updateChunks(List<VectorChunk> chunks);

    /**
     * @param allowedFileIds metadata {@code fileId} values to restrict search to (e.g. document ids as strings).
     *                       {@code null} = no filter (legacy); empty = no hits.
     */
    List<SearchResult> search(String query, float[] queryEmbedding, int topK, List<String> allowedFileIds);

    default List<SearchResult> search(String query, float[] queryEmbedding, int topK) {
        return search(query, queryEmbedding, topK, null);
    }
    void deleteByFileId(String fileId);
    void deleteAll();

    record VectorChunk(String chunkId, String fileId, String content, int chunkIndex) {}
    record SearchResult(String chunkId, String fileId, String content, double score, int chunkIndex) {}
}
