package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    private int topK = 8;

    private boolean rerankEnabled = true;

    private int rerankTopK = 5;

    /** 综合置信度阈值（归一化区间修正为 [0,1] 后略低于旧默认 0.5） */
    private double confidenceThreshold = 0.28;

    private boolean friendlyPromptEnabled = true;

    private String friendlyPrompt = "抱歉，我目前没有找到与您问题相关的资料。请您上传相关文档，或者换个问题试试~";

    private int confidenceTopK = 3;

    private double topKWeight = 0.4;

    private double top1Weight = 0.3;

    private double stddevWeight = 0.2;

    private double countWeight = 0.1;

    /**
     * 低于该原始 Top1 时对综合分打折（面向旧版负分 Chroma 量纲时多为 -25；当前 Python 为 0~1 时通常不触发）。
     */
    private double minTop1Score = -25.0;

    private int minResults = 2;

    /**
     * 归一化区间，须与 {@code SearchResult.score()} 量纲一致。
     * Python/Chroma 当前为 similarity≈1-distance，约在 [0,1]；若误用 [-30,0] 会把 0.02 映射成≈1 导致置信虚高。
     */
    private double scoreRangeMin = 0.0;

    private double scoreRangeMax = 1.0;

    /** 为 true 时：Top1 原始分低于 {@link #minRawTop1ForContext} 则整轮不注入参考资料 */
    private boolean rawTop1GateEnabled = true;

    /**
     * Top1 原始 score 下限（越大越相似）。日志里弱相关多在 0.01~0.03，可试 0.06~0.12。
     */
    private double minRawTop1ForContext = 0.08;

    /** 丢弃过短的 chunk（字符数），减少「构」等解析噪声进入上下文；0 表示不过滤 */
    private int minChunkContentLength = 20;

    /** 向量检索原始 score 下限（越高越相似，与插件返回的 score 一致）；null 表示不过滤 */
    private Double minHitScore;
}
