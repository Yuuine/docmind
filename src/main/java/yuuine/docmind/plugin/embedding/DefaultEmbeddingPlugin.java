package yuuine.docmind.plugin.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("embeddingPlugin")
@RequiredArgsConstructor
public class DefaultEmbeddingPlugin implements EmbeddingPlugin {

    private final EmbeddingProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("Embedding base-url is not configured");
        }
        if (!properties.getBaseUrl().startsWith("http://") && !properties.getBaseUrl().startsWith("https://")) {
            throw new IllegalStateException("Embedding base-url must start with http:// or https://");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("Embedding api-key is not configured");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("Embedding model is not configured");
        }
        if (properties.getDimension() <= 0) {
            throw new IllegalStateException("Embedding dimension must be positive");
        }
        log.info("Embedding config validated: baseUrl={}, model={}, dimension={}", 
                properties.getBaseUrl(), properties.getModel(), properties.getDimension());
    }

    @Override
    public String getName() {
        return "default-embedding";
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        log.info("[Embedding] batch embed {} texts, model={}", texts.size(), properties.getModel());
        
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        
        return results;
    }

    @Override
    public float[] embed(String text) {
        log.debug("[Embedding] single embed, text length={}, model={}", text.length(), properties.getModel());
        
        Map<String, Object> request = new HashMap<>();
        request.put("model", properties.getModel());
        request.put("input", text);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getBaseUrl() + "/embeddings",
                entity,
                String.class
        );
        
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.get("data").get(0).get("embedding");
            
            float[] embedding = new float[getDimension()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            
            return embedding;
        } catch (Exception e) {
            log.error("解析 Embedding 响应失败", e);
            throw new RuntimeException("解析 Embedding 响应失败", e);
        }
    }

    @Override
    public int getDimension() {
        return properties.getDimension();
    }
}
