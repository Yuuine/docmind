package yuuine.docmind.core.chat.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import yuuine.docmind.core.chat.config.RagPromptProperties;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.service.PromptAssembler;
import yuuine.docmind.core.chat.valueobject.MessageRole;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptAssemblerImpl implements PromptAssembler {

    private final RagPromptProperties ragPromptProperties;

    @Override
    public List<Map<String, String>> assemble(
            String systemPrompt,
            List<ChatMessage> history,
            String context,
            String query,
            int maxHistoryRounds) {

        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System 消息（固定首位）
        Map<String, String> sysMsg = new HashMap<>(2);
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        // 2. 历史消息截断：按轮数取最近 N 轮
        boolean willAppendRagUser = (query != null && !query.isBlank())
                || (context != null && !context.isBlank());
        if (history != null && !history.isEmpty()) {
            List<ChatMessage> truncatedHistory = truncateByRounds(history, maxHistoryRounds);
            // 末尾会追加「当前问题 + 参考资料」合成 user 消息时，去掉历史中最后一条 USER，避免同一轮问题重复出现两次
            if (willAppendRagUser && !truncatedHistory.isEmpty()) {
                int lastIdx = truncatedHistory.size() - 1;
                if (truncatedHistory.get(lastIdx).getRole() == MessageRole.USER) {
                    truncatedHistory = new ArrayList<>(truncatedHistory.subList(0, lastIdx));
                }
            }
            for (ChatMessage msg : truncatedHistory) {
                if (msg.getRole() == null) continue;
                Map<String, String> m = new HashMap<>(2);
                m.put("role", msg.getRole().name().toLowerCase());
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
                messages.add(m);
            }
        }

        // 3. 合并 Context 和 Query 为一条 user 消息（遵循 OpenAI API 规范）
        StringBuilder userContent = new StringBuilder();
        
        // 先添加 query
        if (query != null && !query.isBlank()) {
            userContent.append(ragPromptProperties.formatQuery(query));
        }
        
        // 再添加 context（如果存在）
        if (context != null && !context.isBlank()) {
            if (!userContent.isEmpty()) {
                userContent.append("\n\n");
            }
            userContent.append(ragPromptProperties.formatContext(context));
        }
        
        // 只有当内容非空时才添加消息
        if (!userContent.isEmpty()) {
            Map<String, String> userMsg = new HashMap<>(2);
            userMsg.put("role", "user");
            userMsg.put("content", userContent.toString());
            messages.add(userMsg);
        }

        return messages;
    }

    /**
     * 按轮数截断历史消息
     * history 格式: [user, assistant, user, assistant, ..., user(当前)]
     * 注意: 最后一条是当前用户消息（刚持久化的），不应被截断
     * 截断策略: 保留最后一条用户消息 + 前面最多 (maxRounds*2 - 1) 条
     */
    private List<ChatMessage> truncateByRounds(List<ChatMessage> history, int maxRounds) {
        if (history.size() <= maxRounds * 2) {
            return new ArrayList<>(history);
        }
        return new ArrayList<>(
            history.subList(history.size() - maxRounds * 2, history.size())
        );
    }
}
