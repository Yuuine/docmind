package yuuine.docmind.plugin.python;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import yuuine.docmind.common.plugin.EmbeddingPlugin;

@Configuration
@EnableConfigurationProperties(PythonEmbeddingProperties.class)
@ConditionalOnProperty(prefix = "docmind.embedding", name = "type", havingValue = "python")
public class PythonEmbeddingAutoConfiguration {

    @Bean
    public EmbeddingPlugin pythonEmbeddingPlugin(
            PythonEmbeddingProperties properties,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        RestTemplate restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
        return new PythonEmbeddingPlugin(properties, restTemplate, objectMapper);
    }
}
