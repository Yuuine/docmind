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
        if (history != null && !history.isEmpty()) {
            List<ChatMessage> truncatedHistory = truncateByRounds(history, maxHistoryRounds);
            for (ChatMessage msg : truncatedHistory) {
                if (msg.getRole() == null) continue;
                Map<String, String> m = new HashMap<>(2);
                m.put("role", msg.getRole().name().toLowerCase());
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
                messages.add(m);
            }
        }

        // 3. Context 消息（仅当 context 非空时添加）
        if (context != null && !context.isBlank()) {
            Map<String, String> ctxMsg = new HashMap<>(2);
            ctxMsg.put("role", "user");
            ctxMsg.put("content", ragPromptProperties.formatContext(context));
            messages.add(ctxMsg);
        }

        // 4. Query 消息（最后一条）
        Map<String, String> queryMsg = new HashMap<>(2);
        queryMsg.put("role", "user");
        queryMsg.put("content", ragPromptProperties.formatQuery(query));
        messages.add(queryMsg);

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
