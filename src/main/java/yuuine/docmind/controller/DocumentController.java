package yuuine.docmind.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.document.dto.DocumentChunkInfo;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentStats;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;
import yuuine.docmind.core.document.service.DocumentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Audited(action = AuditAction.DOCUMENT_UPLOAD, resourceType = "Document", resourceIdParam = "userId", describe = "上传文档")
    @PostMapping("/upload")
    public Result<DocumentResponse> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "filename", required = false) String filename,
                                           @RequestParam Long userId) {
        log.info("收到上传请求: filename={}, originalFilename={}, size={}, userId={}", 
                filename, file.getOriginalFilename(), file.getSize(), userId);
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .file(file)
                .filename(filename)
                .build();
        DocumentResponse response = documentService.uploadDocument(request, userId);
        log.info("上传成功: documentId={}", response.getId());
        return Result.success(response);
    }

    @GetMapping
    public Result<PageResponse<DocumentResponse>> list(@RequestParam Long userId,
                                                        DocumentQueryRequest request) {
        return Result.success(documentService.listDocuments(userId, request));
    }

    @GetMapping("/{id}")
    public Result<DocumentResponse> getDetail(@PathVariable Long id, @RequestParam Long userId) {
        return Result.success(documentService.getDocument(id, userId));
    }

    @Audited(action = AuditAction.DOCUMENT_DELETE, resourceType = "Document", resourceIdParam = "id", describe = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        documentService.deleteDocument(id, userId);
        return Result.success();
    }

    @GetMapping("/{id}/stats")
    public Result<DocumentStats> getStats(@PathVariable Long id, @RequestParam Long userId) {
        log.info("获取文档统计信息: documentId={}, userId={}", id, userId);
        return Result.success(documentService.getDocumentStats(id, userId));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<DocumentChunkInfo>> getChunks(@PathVariable Long id, @RequestParam Long userId) {
        log.info("获取文档分块列表: documentId={}, userId={}", id, userId);
        return Result.success(documentService.getDocumentChunks(id, userId));
    }

    @GetMapping("/chunks/{chunkId}")
    public Result<DocumentChunkInfo> getChunk(@PathVariable Long chunkId, @RequestParam Long userId) {
        log.info("获取分块详情: chunkId={}, userId={}", chunkId, userId);
        return Result.success(documentService.getDocumentChunk(chunkId, userId));
    }
}
