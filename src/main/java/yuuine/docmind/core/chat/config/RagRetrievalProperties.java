package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    /** 向量检索返回的最大 chunk 数（对应配置键 {@code rag.retrieval.top-k}）。 */
    private int topK = 8;

    /** 是否调用 Rerank 插件对检索结果重排（{@code rag.retrieval.rerank-enabled}）。 */
    private boolean rerankEnabled = true;

    /** 重排后写入上下文的条数上限（{@code rag.retrieval.rerank-top-k}）。 */
    private int rerankTopK = 5;
}
