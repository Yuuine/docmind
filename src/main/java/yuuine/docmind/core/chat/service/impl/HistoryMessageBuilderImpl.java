package yuuine.docmind.core.chat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuuine.docmind.core.chat.config.HistoryMessageProperties;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.service.HistoryMessageBuilder;
import yuuine.docmind.core.chat.valueobject.MessageRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryMessageBuilderImpl implements HistoryMessageBuilder {

    private final HistoryMessageProperties historyProperties;

    @Override
    public List<Map<String, String>> buildHistoryMessages(
            List<ChatMessage> history,
            String systemPrompt,
            String context,
            String query) {

        List<Map<String, String>> messages = new ArrayList<>();

        if (!historyProperties.isEnabled()) {
            if (query != null && !query.isBlank()) {
                Map<String, String> queryMsg = new HashMap<>(2);
                queryMsg.put("role", "user");
                queryMsg.put("content", query);
                messages.add(queryMsg);
            }
            return messages;
        }

        if (historyProperties.isIncludeSystemPrompt() && systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, String> sysMsg = new HashMap<>(2);
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        List<ChatMessage> truncatedHistory = truncateByRounds(history, historyProperties.getMaxRounds());
        for (ChatMessage msg : truncatedHistory) {
            if (msg.getRole() == null) continue;
            Map<String, String> m = new HashMap<>(2);
            m.put("role", convertRoleToString(msg.getRole()));
            m.put("content", msg.getContent() != null ? msg.getContent() : "");
            messages.add(m);
        }

        if (historyProperties.isIncludeContext() && context != null && !context.isBlank()) {
            Map<String, String> ctxMsg = new HashMap<>(2);
            ctxMsg.put("role", "user");
            ctxMsg.put("content", "相关文档内容：\n" + context);
            messages.add(ctxMsg);
        }

        if (query != null && !query.isBlank()) {
            Map<String, String> queryMsg = new HashMap<>(2);
            queryMsg.put("role", "user");
            queryMsg.put("content", query);
            messages.add(queryMsg);
        }

        return messages;
    }

    private List<ChatMessage> truncateByRounds(List<ChatMessage> history, int maxRounds) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        int historySize = history.size();
        if (historySize <= maxRounds * 2) {
            return new ArrayList<>(history);
        }

        return new ArrayList<>(history.subList(historySize - maxRounds * 2, historySize));
    }

    private String convertRoleToString(MessageRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }
}