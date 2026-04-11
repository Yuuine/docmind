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

    /**
     * 关闭 RAG（不注入参考资料）时使用的 system 提示，避免与「仅依据参考资料」冲突。
     */
    private String systemWithoutRag = "你是「潇潇」，友善、简洁的助手。当前对话未挂载文档知识库，请基于通用知识回答；不确定时请说明。";

    /**
     * 历史里每条 assistant 的最大字符数，超出部分截断（0 表示不截断）。用于控制长代码/长 JSON 占满上下文。
     */
    private int maxAssistantContentChars = 3500;

    public String formatContext(String context) {
        return (getContextTemplate() != null ? getContextTemplate() : "相关文档内容：\n{context}").replace("{context}", context);
    }

    public String formatQuery(String query) {
        return (getQueryTemplate() != null ? getQueryTemplate() : "用户问题：{query}").replace("{query}", query);
    }
}
