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

        // TODO: 测试用
        try {
            String requestBodyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            log.info("发送LLM请求, model={}, apiUrl={}", model.getModelName(), apiUrl);
            log.info("完整请求体 JSON:\n{}", requestBodyJson);
        } catch (Exception e) {
            log.warn("格式化请求体失败, 使用默认日志输出: {}", e.getMessage());
            log.info("发送LLM请求, model={}, apiUrl={}, requestBody={}", model.getModelName(), apiUrl, requestBody);
        }

        WebClient webClient = createWebClient();

        return webClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + model.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(line -> log.debug("收到LLM原始响应行: {}", line))
                .flatMap(this::parseSseLine)
                .doOnNext(chunk -> log.debug("解析后的LlmChunk: content={}, done={}, error={}", 
                    chunk.getContent(), chunk.isDone(), chunk.getError()))
                .onErrorResume(e -> {
                    log.error("LLM流式请求失败, model={}, error={}", model.getModelName(), e.getMessage(), e);
                    return Flux.just(LlmChunk.error("LLM 请求失败: " + e.getMessage()));
                })
                .timeout(Duration.ofMinutes(3));
    }

    private Mono<LlmChunk> parseSseLine(String line) {
        line = line.trim();
        log.debug("parseSseLine - 原始行: {}", line);
        
        if (line.isEmpty()) {
            log.debug("parseSseLine - 空行，跳过");
            return Mono.empty();
        }
        
        String data = line;
        if (line.startsWith("data:")) {
            data = line.substring(5).trim();
        }
        log.debug("parseSseLine - data部分: {}", data);
        
        if ("[DONE]".equals(data)) {
            log.debug("parseSseLine - 收到[DONE]标记");
            return Mono.just(LlmChunk.of("", true));
        }
        try {
            Map<String, Object> json = objectMapper.readValue(data, Map.class);
            log.debug("parseSseLine - 解析后的JSON: {}", json);
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                log.debug("parseSseLine - delta: {}", delta);
                
                if (delta != null) {
                    String content = (String) delta.get("content");
                    log.debug("parseSseLine - content: {}", content);
                    
                    if (content != null && !content.isEmpty()) {
                        return Mono.just(LlmChunk.of(content, false));
                    }
                }
                
                String finishReason = (String) choices.get(0).get("finish_reason");
                if (finishReason != null && !finishReason.isEmpty()) {
                    log.debug("parseSseLine - 收到finish_reason: {}", finishReason);
                    return Mono.just(LlmChunk.of("", true));
                }
            }
        } catch (Exception e) {
            log.warn("parseSseLine - 解析异常: {}", e.getMessage(), e);
        }
        log.debug("parseSseLine - 返回空");
        return Mono.empty();
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
