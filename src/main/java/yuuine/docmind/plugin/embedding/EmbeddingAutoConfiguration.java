package yuuine.docmind.plugin.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
@ConditionalOnProperty(prefix = "docmind.embedding", name = "type", havingValue = "default", matchIfMissing = true)
public class EmbeddingAutoConfiguration {

    @Bean("embeddingPlugin")
    public DefaultEmbeddingPlugin defaultEmbeddingPlugin(EmbeddingProperties properties) {
        return new DefaultEmbeddingPlugin(properties);
    }
}
