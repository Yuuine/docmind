package yuuine.docmind.plugin.python;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import yuuine.docmind.common.plugin.VectorStorePlugin;

import java.util.*;

@Slf4j
public class PythonVectorStorePlugin implements VectorStorePlugin {

    private final PythonVectorStoreProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PythonVectorStorePlugin(
            PythonVectorStoreProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getBaseUrl() {
        return properties.getUrl() + "/api/v1/vectors";
    }

    @Override
    public String getName() {
        return "python";
    }

    @Override
    public void addChunks(List<VectorChunk> chunks) {
        log.info("开始添加 {} 个 chunks 到 Python VectorStore", chunks.size());

        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (VectorChunk chunk : chunks) {
            Map<String, Object> chunkMap = new HashMap<>();
            chunkMap.put("chunkId", chunk.chunkId());
            chunkMap.put("fileId", chunk.fileId());
            chunkMap.put("content", chunk.content());
            chunkMap.put("chunkIndex", chunk.chunkIndex());
            chunkList.add(chunkMap);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("chunks", chunkList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(getBaseUrl() + "/add", entity, String.class);
            log.info("成功添加 {} 个 chunks 到 Python VectorStore", chunks.size());
        } catch (RestClientException e) {
            log.error("添加 chunks 到 Python VectorStore 失败", e);
            throw e;
        }
    }

    @Override
    public List<SearchResult> search(String query, float[] queryEmbedding, int topK, List<String> allowedFileIds) {
        log.info("Python VectorStore 搜索: query={}, topK={}, fileIdFilterSize={}, hybrid={}",
                query, topK, allowedFileIds == null ? "none" : allowedFileIds.size(), properties.isHybridEnabled());

        if (allowedFileIds != null && allowedFileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> request = new HashMap<>();
        if (queryEmbedding != null) {
            request.put("queryEmbedding", queryEmbedding);
        } else {
            request.put("query", query);
        }
        request.put("topK", topK);
        if (allowedFileIds != null) {
            request.put("fileIds", allowedFileIds);
        }
        request.put("hybrid", properties.isHybridEnabled());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    getBaseUrl() + "/search",
                    entity,
                    Map.class
            );

            List<SearchResult> results = new ArrayList<>();
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> hits = (List<Map<String, Object>>) body.get("hits");

                if (hits != null) {
                    for (Map<String, Object> hit : hits) {
                        results.add(new SearchResult(
                                (String) hit.get("chunkId"),
                                (String) hit.get("fileId"),
                                (String) hit.get("content"),
                                ((Number) hit.get("score")).doubleValue(),
                                ((Number) hit.get("chunkIndex")).intValue()
                        ));
                    }
                }
            }

            log.info("Python VectorStore 搜索完成: 返回 {} 个结果 (hybrid={})",
                    results.size(), properties.isHybridEnabled());
            return results;
        } catch (RestClientException e) {
            log.error("Python VectorStore 搜索失败", e);
            throw e;
        }
    }

    @Override
    public void deleteByFileId(String fileId) {
        log.info("从 Python VectorStore 删除 fileId: {}", fileId);

        Map<String, Object> request = new HashMap<>();
        request.put("fileId", fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(getBaseUrl() + "/delete", entity, String.class);
            log.info("Python VectorStore 删除完成: fileId={}", fileId);
        } catch (RestClientException e) {
            log.error("从 Python VectorStore 删除 fileId 失败: fileId={}", fileId, e);
            throw e;
        }
    }

    @Override
    public void updateChunks(List<VectorChunk> chunks) {
        log.info("更新 {} 个 chunks 到 Python VectorStore", chunks.size());

        List<Map<String, Object>> chunkList = new ArrayList<>();
        for (VectorChunk chunk : chunks) {
            Map<String, Object> chunkMap = new HashMap<>();
            chunkMap.put("chunkId", chunk.chunkId());
            chunkMap.put("fileId", chunk.fileId());
            chunkMap.put("content", chunk.content());
            chunkMap.put("chunkIndex", chunk.chunkIndex());
            chunkList.add(chunkMap);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("chunks", chunkList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.exchange(
                    getBaseUrl() + "/update",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );
            log.info("成功更新 {} 个 chunks 到 Python VectorStore", chunks.size());
        } catch (RestClientException e) {
            log.error("更新 chunks 到 Python VectorStore 失败", e);
            throw e;
        }
    }

    @Override
    public void deleteAll() {
        log.warn("清空 Python VectorStore 所有数据");
        try {
            restTemplate.delete(getBaseUrl() + "/all");
            log.info("Python VectorStore 清空完成");
        } catch (RestClientException e) {
            log.error("清空 Python VectorStore 失败", e);
            throw e;
        }
    }
}
