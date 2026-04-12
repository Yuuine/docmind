package yuuine.docmind.core.document.service;

import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.document.dto.DocumentChunkInfo;
import yuuine.docmind.core.document.dto.DocumentDownloadResponse;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentStats;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;

import java.util.List;

public interface DocumentService {
    DocumentResponse uploadDocument(DocumentUploadRequest request, Long userId);

    DocumentResponse getDocument(Long documentId, Long userId);

    PageResponse<DocumentResponse> listDocuments(Long userId, DocumentQueryRequest request);

    void deleteDocument(Long documentId, Long userId);

    /**
     * 批量删除文档（同一事务；任一 id 校验失败则整批回滚）
     */
    void deleteDocuments(List<Long> documentIds, Long userId);

    /**
     * 失败文档重新走解析与索引（仅 {@code ERROR} 状态；异步执行解析）
     */
    void reprocessDocument(Long documentId, Long userId);

    DocumentStats getDocumentStats(Long documentId, Long userId);

    List<DocumentChunkInfo> getDocumentChunks(Long documentId, Long userId);

    DocumentChunkInfo getDocumentChunk(Long chunkId, Long userId);

    DocumentDownloadResponse downloadDocument(Long documentId, Long userId);
}
