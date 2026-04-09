package yuuine.docmind.plugin.chroma;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import yuuine.docmind.common.plugin.VectorStorePlugin;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChromaVectorStorePlugin implements VectorStorePlugin {

    private final ChromaProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getCollectionUrl() {
        return properties.getUrl() + "/api/v1/collections/" + properties.getCollectionName();
    }

    private void ensureCollectionExists() {
        try {
            restTemplate.getForEntity(getCollectionUrl(), String.class);
        } catch (Exception e) {
            log.info("Collection 不存在，创建: {}", properties.getCollectionName());
            Map<String, Object> createRequest = new HashMap<>();
            createRequest.put("name", properties.getCollectionName());
            createRequest.put("metadata", Map.of("description", "DocMindRAG document chunks"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(createRequest, headers);
            
            restTemplate.postForEntity(
                    properties.getUrl() + "/api/v1/collections",
                    entity,
                    String.class
            );
        }
    }

    @Override
    public String getName() {
        return "chroma";
    }

    @Override
    public void addChunks(List<VectorChunk> chunks) {
        log.info("开始添加 {} 个 chunks 到 ChromaDB", chunks.size());
        ensureCollectionExists();

        List<String> ids = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<float[]> embeddings = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();

        for (VectorChunk chunk : chunks) {
            ids.add(chunk.chunkId());
            documents.add(chunk.content());
            if (chunk.embedding() != null) {
                embeddings.add(chunk.embedding());
            }
            metadatas.add(Map.of(
                    "fileId", chunk.fileId(),
                    "chunkIndex", String.valueOf(chunk.chunkIndex())
            ));
        }

        Map<String, Object> request = new HashMap<>();
        request.put("ids", ids);
        request.put("documents", documents);
        if (!embeddings.isEmpty()) {
            request.put("embeddings", embeddings);
        }
        request.put("metadatas", metadatas);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(getCollectionUrl() + "/add", entity, String.class);
        log.info("成功添加 {} 个 chunks 到 ChromaDB", chunks.size());
    }

    @Override
    public List<SearchResult> search(String query, float[] queryEmbedding, int topK) {
        log.info("ChromaDB 搜索: query={}, topK={}", query, topK);
        ensureCollectionExists();

        Map<String, Object> request = new HashMap<>();
        if (queryEmbedding != null) {
            request.put("query_embeddings", List.of(queryEmbedding));
        } else {
            request.put("query_texts", List.of(query));
        }
        request.put("n_results", topK);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getCollectionUrl() + "/query",
                entity,
                Map.class
        );

        List<SearchResult> results = new ArrayList<>();
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<List<String>> ids = (List<List<String>>) body.get("ids");
            List<List<String>> documents = (List<List<String>>) body.get("documents");
            List<List<Double>> distances = (List<List<Double>>) body.get("distances");
            List<List<Map<String, String>>> metadatas = (List<List<Map<String, String>>>) body.get("metadatas");

            if (ids != null && !ids.isEmpty()) {
                for (int i = 0; i < ids.get(0).size(); i++) {
                    Map<String, String> metadata = metadatas.get(0).get(i);
                    results.add(new SearchResult(
                            ids.get(0).get(i),
                            metadata.get("fileId"),
                            documents.get(0).get(i),
                            1.0 - distances.get(0).get(i),
                            Integer.parseInt(metadata.getOrDefault("chunkIndex", "0"))
                    ));
                }
            }
        }

        log.info("ChromaDB 搜索完成: 返回 {} 个结果", results.size());
        return results;
    }

    @Override
    public void deleteByFileId(String fileId) {
        log.info("从 ChromaDB 删除 fileId: {}", fileId);
        ensureCollectionExists();

        Map<String, Object> request = new HashMap<>();
        request.put("where", Map.of("fileId", fileId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(getCollectionUrl() + "/delete", entity, String.class);
        log.info("ChromaDB 删除完成: fileId={}", fileId);
    }

    @Override
    public void deleteAll() {
        log.warn("清空 ChromaDB 所有数据");
        try {
            restTemplate.delete(getCollectionUrl());
            log.info("ChromaDB 清空完成");
        } catch (Exception e) {
            log.error("清空 ChromaDB 失败", e);
        }
    }
}
