package yuuine.docmind.plugin.rerank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.plugin.RerankPlugin;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component("rerankPlugin")
@RequiredArgsConstructor
public class DefaultRerankPlugin implements RerankPlugin {

    private final RerankProperties properties;

    @Override
    public String getName() {
        return "default-rerank";
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> documents, int topK) {
        // TODO: 实现文档重排序
        // 1. 调用外部 Rerank API (如 Cohere Rerank, BGE Rerank, Jina Rerank 等)
        // 2. 返回按相关性分数降序排列的 RerankResult 列表
        // 3. 支持 topK 参数限制返回数量
        // 4. 处理空文档列表、API调用失败等边界情况
        log.debug("[Rerank] rerank {} documents, topK={}, model={}", documents.size(), topK, properties.getModel());
        return Collections.emptyList();
    }
}
