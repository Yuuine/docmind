package yuuine.docmind.core.model.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import yuuine.docmind.core.chat.model.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ModelConfigFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> buildChatRequestBody(
            yuuine.docmind.core.model.entity.AIModel model,
            List<ChatMessage> messages,
            boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model.getModelName());
        requestBody.put("messages", convertMessages(messages));
        if (model.getTemperature() != null) {
            requestBody.put("temperature", model.getTemperature());
        }
        if (model.getMaxTokens() != null) {
            requestBody.put("max_tokens", model.getMaxTokens());
        }
        requestBody.put("stream", stream);
        return mergeExtraConfig(requestBody, model.getExtraConfig());
    }

    public static Map<String, Object> mergeExtraConfig(
            Map<String, Object> baseRequest,
            String extraConfigJson) {
        if (extraConfigJson == null || extraConfigJson.isBlank()) {
            return new HashMap<>(baseRequest);
        }
        try {
            Map<String, Object> extraConfig = objectMapper.readValue(
                    extraConfigJson,
                    new TypeReference<>() {
                    }
            );
            Map<String, Object> merged = new HashMap<>(baseRequest);
            merged.putAll(extraConfig);
            return merged;
        } catch (Exception e) {
            log.warn("解析extraConfig失败, 使用原始请求体, error={}", e.getMessage());
            return new HashMap<>(baseRequest);
        }
    }

    // role 映射: USER→"user", ASSISTANT→"assistant", SYSTEM→"system"
    // DeepSeek API 支持这三种角色类型
    private static List<Map<String, String>> convertMessages(List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> result = new ArrayList<>(chatMessages.size());
        for (ChatMessage message : chatMessages) {
            if (message == null || message.getRole() == null) {
                continue;
            }
            Map<String, String> msg = new HashMap<>(2);
            msg.put("role", message.getRole().name().toLowerCase());
            msg.put("content", message.getContent() != null ? message.getContent() : "");
            result.add(msg);
        }
        return result;
    }
}
