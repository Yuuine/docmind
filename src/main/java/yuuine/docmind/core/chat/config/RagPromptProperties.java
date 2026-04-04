package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.prompt")
public class RagPromptProperties {

    private String system;

    private int maxHistoryRounds = 10;

    private String contextTemplate;

    private String queryTemplate;

    public String formatContext(String context) {
        return getContextTemplate().replace("{context}", context);
    }

    public String formatQuery(String query) {
        return getQueryTemplate().replace("{query}", query);
    }
}
