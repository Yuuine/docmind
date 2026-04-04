package yuuine.docmind.core.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yuuine.docmind.core.chat.model.ChatMessage;
import yuuine.docmind.core.chat.service.LlmService;
import yuuine.docmind.core.chat.valueobject.LlmChunk;
import yuuine.docmind.core.model.entity.AIModel;
import yuuine.docmind.core.model.factory.ModelConfigFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmWebFluxServiceImpl implements LlmService {

    private final ObjectMapper objectMapper;

    @Override
    public Flux<LlmChunk> streamChat(AIModel model, List<ChatMessage> messages) {
        Map<String, Object> requestBody = ModelConfigFactory.buildChatRequestBody(model, messages, true);
        String apiUrl = model.getBaseUrl().replaceAll("/$", "") + "/chat/completions";

        WebClient webClient = createWebClient();

        return webClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + model.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(this::parseSseLine)
                .onErrorResume(e -> {
                    log.error("LLM流式请求失败, model={}, error={}", model.getModelName(), e.getMessage(), e);
                    return Flux.just(LlmChunk.error("LLM 请求失败: " + e.getMessage()));
                })
                .timeout(Duration.ofMinutes(3));
    }

    private Mono<LlmChunk> parseSseLine(String line) {
        line = line.trim();
        if (!line.startsWith("data:")) {
            return Mono.empty();
        }
        String data = line.substring(5).trim();
        if ("[DONE]".equals(data)) {
            return Mono.just(LlmChunk.of("", true));
        }
        try {
            Map<String, Object> json = objectMapper.readValue(data, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta != null) {
                    String content = (String) delta.get("content");
                    if (content != null && !content.isEmpty()) {
                        return Mono.just(LlmChunk.of(content, false));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return Mono.empty();
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
