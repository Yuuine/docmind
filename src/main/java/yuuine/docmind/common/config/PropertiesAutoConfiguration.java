package yuuine.docmind.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import yuuine.docmind.core.chat.config.HistoryMessageProperties;
import yuuine.docmind.core.chat.config.RagPromptProperties;
import yuuine.docmind.core.chat.config.RagRetrievalProperties;
import yuuine.docmind.core.document.config.DocumentParserProperties;
import yuuine.docmind.core.document.config.DocumentUploadProperties;
import yuuine.docmind.plugin.embedding.EmbeddingProperties;
import yuuine.docmind.plugin.python.PythonEmbeddingProperties;
import yuuine.docmind.plugin.python.PythonVectorStoreProperties;
import yuuine.docmind.plugin.rerank.RerankProperties;

@Configuration
@EnableConfigurationProperties({
    RagPromptProperties.class,
    RagRetrievalProperties.class,
    HistoryMessageProperties.class,
    DocumentUploadProperties.class,
    DocumentParserProperties.class,
    EmbeddingProperties.class,
    PythonEmbeddingProperties.class,
    PythonVectorStoreProperties.class,
    RerankProperties.class
})
public class PropertiesAutoConfiguration {
}