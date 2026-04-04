package yuuine.docmind.plugin.chroma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yuuine.docmind.common.plugin.VectorStorePlugin;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChromaVectorStorePlugin implements VectorStorePlugin {

    private final ChromaProperties properties;

    @Override
    public String getName() {
        return "chroma";
    }

    @Override
    public void addChunks(List<VectorChunk> chunks) {
        // TODO: 实现添加文档块到向量数据库
        // 1. 使用 ChromaDB Java Client 或 REST API
        // 2. 批量插入向量数据，包含 embedding、metadata(fileId, chunkIndex)、content
        // 3. 处理批量插入的大小限制，可能需要分批处理
        // 4. 处理网络异常、重复插入等边界情况
        log.info("Adding {} chunks to ChromaDB collection: {}", chunks.size(), properties.getCollectionName());
    }

    @Override
    public List<SearchResult> search(String query, float[] queryEmbedding, int topK) {
        // TODO: 实现向量相似度搜索
        // 1. 使用 ChromaDB 的 query 接口进行向量搜索
        // 2. 支持余弦相似度或欧氏距离
        // 3. 返回 topK 个最相关的文档块，包含 chunkId, fileId, content, score
        // 4. 处理空向量、连接失败等异常情况
        log.info("Searching in ChromaDB, topK: {}", topK);
        return List.of();
    }

    @Override
    public void deleteByFileId(String fileId) {
        // TODO: 实现按文件ID删除向量
        // 1. 使用 ChromaDB 的 delete 接口，按 metadata.fileId 过滤删除
        // 2. 确保删除该文件关联的所有文档块
        // 3. 处理文件不存在的情况
        log.info("Deleting chunks from ChromaDB for fileId: {}", fileId);
    }

    @Override
    public void deleteAll() {
        // TODO: 实现清空向量数据库
        // 1. 删除整个 collection 或清空所有数据
        // 2. 谨慎操作，可能需要权限验证
        // 3. 可选：重建 collection 以保持配置
        log.info("Deleting all chunks from ChromaDB");
    }
}
