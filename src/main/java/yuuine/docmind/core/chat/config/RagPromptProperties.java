package yuuine.docmind.core.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag.prompt")
public class RagPromptProperties {

    private String system = "你是一个专业的文档助手，可以基于提供的文档内容回答用户问题。";

    private int maxHistoryRounds = 10;

    private String contextTemplate = "相关文档内容：\n{context}";

    private String queryTemplate = "用户问题：{query}";

    public String formatContext(String context) {
        return (getContextTemplate() != null ? getContextTemplate() : "相关文档内容：\n{context}").replace("{context}", context);
    }

    public String formatQuery(String query) {
        return (getQueryTemplate() != null ? getQueryTemplate() : "用户问题：{query}").replace("{query}", query);
    }
}
