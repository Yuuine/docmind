package yuuine.docmind.core.document.service;

import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;

public interface DocumentService {
    DocumentResponse uploadDocument(DocumentUploadRequest request, Long userId);

    DocumentResponse getDocument(Long documentId, Long userId);

    PageResponse<DocumentResponse> listDocuments(Long userId, DocumentQueryRequest request);

    void deleteDocument(Long documentId, Long userId);
}
