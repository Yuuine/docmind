package yuuine.docmind.plugin.python;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PythonEmbeddingPlugin implements EmbeddingPlugin {

    private final PythonEmbeddingProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PythonEmbeddingPlugin(
            PythonEmbeddingProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getUrl())) {
            throw new IllegalStateException("Python embedding url is not configured");
        }
        if (!properties.getUrl().startsWith("http://") && !properties.getUrl().startsWith("https://")) {
            throw new IllegalStateException("Python embedding url must start with http:// or https://");
        }
        log.info("Python embedding config validated: url={}", properties.getUrl());
    }

    @Override
    public String getName() {
        return "python-embedding";
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        log.info("[Python Embedding] batch embed {} texts", texts.size());
        
        Map<String, Object> request = new HashMap<>();
        request.put("texts", texts);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getUrl() + "/api/v1/embeddings/batch",
                    entity,
                    String.class
            );
            
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingsNode = root.get("embeddings");
            
            List<float[]> results = new ArrayList<>();
            for (int i = 0; i < embeddingsNode.size(); i++) {
                JsonNode embeddingNode = embeddingsNode.get(i);
                float[] embedding = new float[embeddingNode.size()];
                for (int j = 0; j < embeddingNode.size(); j++) {
                    embedding[j] = (float) embeddingNode.get(j).asDouble();
                }
                results.add(embedding);
            }
            
            return results;
        } catch (Exception e) {
            log.error("批量向量化失败，降级为逐个向量化", e);
            List<float[]> results = new ArrayList<>();
            for (String text : texts) {
                results.add(embed(text));
            }
            return results;
        }
    }

    @Override
    public float[] embed(String text) {
        log.debug("[Python Embedding] single embed, text length={}", text.length());

        Map<String, Object> request = new HashMap<>();
        request.put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getUrl() + "/api/v1/embeddings",
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.get("embedding");

            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }

            return embedding;
        } catch (RestClientException e) {
            log.error("调用 Python Embedding 服务失败: /api/v1/embeddings", e);
            throw new RuntimeException("调用 Python Embedding 服务失败", e);
        } catch (Exception e) {
            log.error("解析 Python Embedding 响应失败", e);
            throw new RuntimeException("解析 Python Embedding 响应失败", e);
        }
    }

    @Override
    public int getDimension() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    properties.getUrl() + "/api/v1/embeddings/dimension",
                    String.class
            );
            
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("dimension").asInt();
        } catch (Exception e) {
            log.error("获取 Embedding 维度失败，降级为测试文本向量化", e);
            return embed("test").length;
        }
    }
}
