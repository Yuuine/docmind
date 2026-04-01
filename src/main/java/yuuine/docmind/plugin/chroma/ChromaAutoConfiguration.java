package yuuine.docmind.plugin.chroma;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChromaProperties.class)
@ConditionalOnProperty(prefix = "docmind.vectorstore", name = "type", havingValue = "chroma", matchIfMissing = true)
public class ChromaAutoConfiguration {

    @Bean
    public ChromaVectorStorePlugin chromaVectorStorePlugin(ChromaProperties properties) {
        return new ChromaVectorStorePlugin(properties);
    }
}
