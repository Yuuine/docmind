package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.prompt.history")
public class HistoryMessageProperties {

    private boolean enabled = true;

    private int maxRounds = 10;

    private boolean includeSystemPrompt = true;

    private boolean includeContext = true;
}