package yuuine.docmind.core.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.common.plugin.EmbeddingPlugin;
import yuuine.docmind.common.plugin.ParserPlugin;
import yuuine.docmind.common.plugin.StoragePlugin;
import yuuine.docmind.common.plugin.VectorStorePlugin;
import yuuine.docmind.core.document.chunking.ChunkingStrategy;
import yuuine.docmind.core.document.chunking.ChunkingStrategyFactory;
import yuuine.docmind.core.document.config.DocumentParserProperties;
import yuuine.docmind.core.document.config.DocumentUploadProperties;
import yuuine.docmind.core.document.model.Document;
import yuuine.docmind.core.document.model.DocumentChunk;
import yuuine.docmind.core.document.repository.DocumentChunkRepository;
import yuuine.docmind.core.document.repository.DocumentRepository;
import yuuine.docmind.core.document.service.DocumentParsingService;
import yuuine.docmind.core.document.service.LangChain4jDocumentChunkingService;
import yuuine.docmind.core.document.valueobject.DocumentStatus;
import yuuine.docmind.plugin.parser.MarkdownParserPlugin;
import yuuine.docmind.plugin.parser.ParserPluginFactory;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParsingServiceImpl implements DocumentParsingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ParserPluginFactory parserPluginFactory;
    private final LangChain4jDocumentChunkingService langChain4jChunkingService;
    private final ChunkingStrategyFactory chunkingStrategyFactory;
    private final StoragePlugin storagePlugin;
    private final DocumentParserProperties parserProperties;
    private final DocumentUploadProperties uploadProperties;
    private final VectorStorePlugin vectorStorePlugin;
    private final EmbeddingPlugin embeddingPlugin;

    @Override
    @Async("documentParsingExecutor")
    @Transactional
    public void parseDocumentAsync(Long documentId) {
        log.info("开始解析文档: documentId={}", documentId);
        try {
            Document document = documentRepository.selectById(documentId);
            if (document == null) {
                log.error("文档不存在: documentId={}", documentId);
                return;
            }

            if (document.getFileSize() > uploadProperties.getMaxFileSize()) {
                long maxSizeMB = uploadProperties.getMaxFileSize() / 1024 / 1024;
                long fileSizeMB = document.getFileSize() / 1024 / 1024;
                log.error("文件过大无法解析: documentId={}, size={}MB, max={}MB",
                        documentId, fileSizeMB, maxSizeMB);
                updateDocumentStatus(documentId, DocumentStatus.ERROR, 
                        "文件过大（最大支持" + maxSizeMB + "MB）");
                return;
            }

            updateDocumentStatus(documentId, DocumentStatus.PARSING, null);

            ParserPlugin parser = parserPluginFactory.getParser(document.getFilename())
                    .or(() -> parserPluginFactory.getParserByContentType(document.getContentType()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_FILE_TYPE, "不支持的文档格式"));

            try (InputStream inputStream = storagePlugin.retrieveFile(document.getFileId())) {
                List<String> chunks;

                if (parser instanceof MarkdownParserPlugin mdParser) {
                    int chunkSize = parserProperties.getMaxSegmentSize();
                    int overlap = parserProperties.getMaxOverlapSize();
                    log.debug("Markdown文件使用结构化分块: chunkSize={}, overlap={}", chunkSize, overlap);
                    chunks = mdParser.parseChunks(inputStream, document.getFilename(), chunkSize, overlap);
                } else {
                    String text = parser.parse(inputStream, document.getFilename());
                    updateDocumentStatus(documentId, DocumentStatus.INDEXING, null);

                    ChunkingStrategy strategy = chunkingStrategyFactory.recommendStrategy(document.getFilename());
                    List<dev.langchain4j.data.segment.TextSegment> segments = langChain4jChunkingService.chunk(text, strategy);
                    chunks = segments.stream().map(dev.langchain4j.data.segment.TextSegment::text).toList();

                    log.info("文档解析完成: documentId={}, 策略={}, 分块数量={}",
                            documentId, strategy.getType(), chunks.size());
                }

                updateDocumentStatus(documentId, DocumentStatus.INDEXING, null);
                saveDocumentChunks(documentId, chunks);

                updateDocumentStatus(documentId, DocumentStatus.READY, null);
                log.info("文档解析+分块完成: documentId={}, 分块数量={}", documentId, chunks.size());
            }
        } catch (Exception e) {
            log.error("文档解析失败: documentId={}", documentId, e);
            updateDocumentStatus(documentId, DocumentStatus.ERROR, e.getMessage());
        }
    }

    private void updateDocumentStatus(Long documentId, DocumentStatus status, String errorMessage) {
        Document doc = new Document();
        doc.setId(documentId);
        doc.setStatus(status);
        doc.setErrorMessage(errorMessage);
        documentRepository.updateById(doc);
    }

    private void saveDocumentChunks(Long documentId, List<String> chunks) {
        LambdaQueryWrapper<DocumentChunk> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DocumentChunk::getDocumentId, documentId);
        documentChunkRepository.delete(deleteWrapper);

        List<VectorStorePlugin.VectorChunk> vectorChunks = new java.util.ArrayList<>();

        log.info("开始向量化 {} 个 chunks", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            String chunkId = UUID.randomUUID().toString();
            DocumentChunk chunk = DocumentChunk.builder()
                    .chunkId(chunkId)
                    .documentId(documentId)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .charCount(chunkContent.length())
                    .build();
            documentChunkRepository.insert(chunk);

            float[] embedding = embeddingPlugin.embed(chunkContent);
            vectorChunks.add(new VectorStorePlugin.VectorChunk(
                    chunkId,
                    String.valueOf(documentId),
                    chunkContent,
                    embedding,
                    i
            ));
        }

        log.info("开始存储到向量库: documentId={}, chunks={}", documentId, vectorChunks.size());
        vectorStorePlugin.addChunks(vectorChunks);
    }
}
