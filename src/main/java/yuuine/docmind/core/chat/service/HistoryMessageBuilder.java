package yuuine.docmind.core.chat.service;

import yuuine.docmind.core.chat.model.ChatMessage;

import java.util.List;
import java.util.Map;

public interface HistoryMessageBuilder {
    List<Map<String, String>> buildHistoryMessages(
        List<ChatMessage> history,
        String systemPrompt,
        String context,
        String query
    );
}