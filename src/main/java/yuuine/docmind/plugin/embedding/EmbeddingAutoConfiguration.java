package yuuine.docmind.plugin.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingPlugin.class)
    public DefaultEmbeddingPlugin defaultEmbeddingPlugin(EmbeddingProperties properties) {
        return new DefaultEmbeddingPlugin(properties);
    }
}
