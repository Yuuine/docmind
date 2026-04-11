package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    private int topK = 8;

    private boolean rerankEnabled = true;

    private int rerankTopK = 5;

    private double confidenceThreshold = 0.5;

    private boolean friendlyPromptEnabled = true;

    private String friendlyPrompt = "抱歉，我目前没有找到与您问题相关的资料。请您上传相关文档，或者换个问题试试~";

    private int confidenceTopK = 3;

    private double topKWeight = 0.4;

    private double top1Weight = 0.3;

    private double stddevWeight = 0.2;

    private double countWeight = 0.1;

    private double minTop1Score = -25.0;

    private int minResults = 2;

    private double scoreRangeMin = -30.0;

    private double scoreRangeMax = 0.0;
}
