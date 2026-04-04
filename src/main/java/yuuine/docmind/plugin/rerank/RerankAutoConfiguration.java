package yuuine.docmind.plugin.rerank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yuuine.docmind.common.plugin.RerankPlugin;

@Configuration
@EnableConfigurationProperties(RerankProperties.class)
public class RerankAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RerankPlugin.class)
    public DefaultRerankPlugin defaultRerankPlugin(RerankProperties properties) {
        return new DefaultRerankPlugin(properties);
    }
}
