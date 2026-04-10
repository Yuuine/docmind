package yuuine.docmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import yuuine.docmind.common.config.PropertiesAutoConfiguration;
import yuuine.docmind.plugin.embedding.EmbeddingAutoConfiguration;

@SpringBootApplication
@EnableConfigurationProperties
@Import({EmbeddingAutoConfiguration.class, PropertiesAutoConfiguration.class})
public class DocMindRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocMindRagApplication.class, args);
    }

}