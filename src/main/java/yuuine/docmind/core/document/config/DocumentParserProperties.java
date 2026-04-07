package yuuine.docmind.core.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import yuuine.docmind.core.document.chunking.ChunkingStrategyType;

@Data
@Component
@ConfigurationProperties(prefix = "docmind.document.parser")
public class DocumentParserProperties {

    private ChunkingStrategyType chunkingStrategy = ChunkingStrategyType.SENTENCE;

    private int maxSegmentSize = 1000;

    private int maxOverlapSize = 200;

    private HybridConfig hybrid = new HybridConfig();

    private RegexConfig regex = new RegexConfig();

    @Data
    public static class HybridConfig {
        private int paragraphThreshold = 800;
        private int sentenceThreshold = 300;
    }

    @Data
    public static class RegexConfig {
        private String pattern = "\n\n";
    }
}
