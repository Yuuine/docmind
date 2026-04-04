package yuuine.docmind.plugin.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component("embeddingPlugin")
@RequiredArgsConstructor
public class DefaultEmbeddingPlugin implements EmbeddingPlugin {

    private final EmbeddingProperties properties;

    @Override
    public String getName() {
        return "default-embedding";
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        // TODO: 实现批量文本向量化
        // 1. 调用外部 Embedding API (如 OpenAI text-embedding-3-small, 本地模型等)
        // 2. 支持配置的维度 (dimension) 参数
        // 3. 支持批量处理以提高效率，注意API的批量限制
        // 4. 处理API调用失败、超时等异常情况
        log.debug("[Embedding] batch embed {} texts, model={}", texts.size(), properties.getModel());
        return Collections.emptyList();
    }

    @Override
    public float[] embed(String text) {
        // TODO: 实现单文本向量化
        // 1. 调用外部 Embedding API
        // 2. 返回与配置维度匹配的 float[] 向量
        // 3. 可复用 embed(List<String>) 方法
        log.debug("[Embedding] single embed, text length={}, model={}", text.length(), properties.getModel());
        return null;
    }

    @Override
    public int getDimension() {
        return properties.getDimension();
    }
}
