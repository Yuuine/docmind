package yuuine.docmind.common.plugin;

import java.util.List;

public interface RerankPlugin {
    String getName();
    List<RerankResult> rerank(String query, List<String> documents, int topK);

    record RerankResult(int index, String document, double score) {}
}
