package yuuine.docmind.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.audit.annotation.Audited;
import yuuine.docmind.core.audit.dto.PageResponse;
import yuuine.docmind.core.audit.valueobject.AuditAction;
import yuuine.docmind.core.document.dto.DocumentBatchDeleteRequest;
import yuuine.docmind.core.document.dto.DocumentChunkInfo;
import yuuine.docmind.core.document.dto.DocumentDownloadResponse;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentStats;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;
import yuuine.docmind.core.document.service.DocumentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Audited(action = AuditAction.DOCUMENT_UPLOAD, resourceType = "Document", describe = "上传文档")
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

    @Audited(action = AuditAction.DOCUMENT_DELETE, resourceType = "Document", resourceIdFromPath = "id", describe = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        documentService.deleteDocument(id, userId);
        return Result.success();
    }

    @Audited(action = AuditAction.DOCUMENT_DELETE, resourceType = "Document", describe = "批量删除文档")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody DocumentBatchDeleteRequest request,
                                    @RequestParam Long userId) {
        documentService.deleteDocuments(request.getIds(), userId);
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

    @Audited(action = AuditAction.DOCUMENT_DOWNLOAD, resourceType = "Document", resourceIdFromPath = "id", describe = "下载文档")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, @RequestParam Long userId, HttpServletResponse response) {
        log.info("下载文档: documentId={}, userId={}", id, userId);
        DocumentDownloadResponse downloadResponse = documentService.downloadDocument(id, userId);
        
        try (InputStream inputStream = downloadResponse.getInputStream();
             OutputStream outputStream = response.getOutputStream()) {
            
            response.setContentType(downloadResponse.getContentType());
            response.setContentLengthLong(downloadResponse.getFileSize());
            String encodedFilename = URLEncoder.encode(downloadResponse.getFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件下载失败: documentId={}", id, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
}
