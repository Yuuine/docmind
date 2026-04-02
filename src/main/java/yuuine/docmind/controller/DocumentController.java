package yuuine.docmind.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;
import yuuine.docmind.core.document.service.DocumentService;

import java.util.List;

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
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .file(file)
                .filename(filename)
                .build();
        return Result.success(documentService.uploadDocument(request, userId));
    }

    @GetMapping
    public Result<List<DocumentResponse>> list(@RequestParam Long userId,
                                               @RequestBody(required = false) DocumentQueryRequest request) {
        if (request == null) {
            request = new DocumentQueryRequest();
        }
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
}
