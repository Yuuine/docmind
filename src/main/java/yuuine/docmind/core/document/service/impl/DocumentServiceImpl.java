package yuuine.docmind.core.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.common.plugin.StoragePlugin;
import yuuine.docmind.core.document.dto.DocumentQueryRequest;
import yuuine.docmind.core.document.dto.DocumentResponse;
import yuuine.docmind.core.document.dto.DocumentUploadRequest;
import yuuine.docmind.core.document.model.Document;
import yuuine.docmind.core.document.repository.DocumentRepository;
import yuuine.docmind.core.document.valueobject.DocumentStatus;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements yuuine.docmind.core.document.service.DocumentService {

    private final DocumentRepository documentRepository;
    private final StoragePlugin storagePlugin;

    @Override
    public DocumentResponse uploadDocument(DocumentUploadRequest request, Long userId) {
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件不能为空");
        }

        String filename = request.getFilename() != null ? request.getFilename() : file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "文件名不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String md5 = calculateMD5(inputStream);
            inputStream.close();

            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Document::getFileMd5, md5).eq(Document::getUserId, userId);
            if (documentRepository.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件已存在");
            }

            String fileId = UUID.randomUUID().toString();
            String storagePath = storagePlugin.storeFile(filename, file.getInputStream(), file.getContentType());

            Document document = Document.builder()
                    .userId(userId)
                    .fileId(fileId)
                    .filename(filename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileMd5(md5)
                    .storagePath(storagePath)
                    .status(DocumentStatus.UPLOADING)
                    .build();

            documentRepository.insert(document);
            return toDocumentResponse(document);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public DocumentResponse getDocument(Long documentId, Long userId) {
        Document document = documentRepository.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此文档");
        }
        return toDocumentResponse(document);
    }

    @Override
    public List<DocumentResponse> listDocuments(Long userId, DocumentQueryRequest request) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getUserId, userId);

        if (request.getFilename() != null && !request.getFilename().isBlank()) {
            queryWrapper.like(Document::getFilename, request.getFilename());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(Document::getStatus, request.getStatus());
        }

        int page = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        Page<Document> documentPage = new Page<>(page, pageSize);
        Page<Document> result = documentRepository.selectPage(documentPage, queryWrapper);

        return result.getRecords().stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Override
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此文档");
        }

        storagePlugin.deleteFile(document.getFileId());
        documentRepository.deleteById(documentId);
    }

    public void updateDocumentStatus(Long documentId, DocumentStatus status, String errorMessage) {
        LambdaUpdateWrapper<Document> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Document::getId, documentId)
                .set(Document::getStatus, status)
                .set(errorMessage != null, Document::getErrorMessage, errorMessage);
        documentRepository.update(null, updateWrapper);
    }

    private String calculateMD5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "MD5计算失败");
        }
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .fileId(document.getFileId())
                .filename(document.getFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .fileMd5(document.getFileMd5())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
