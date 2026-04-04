package yuuine.docmind.core.chat.service;

import java.util.List;
import java.util.Map;
import yuuine.docmind.core.chat.model.ChatMessage;

public interface PromptAssembler {
    List<Map<String, String>> assemble(
        String systemPrompt,
        List<ChatMessage> history,
        String context,
        String query,
        int maxHistoryRounds
    );
}
