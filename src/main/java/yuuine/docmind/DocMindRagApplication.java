package yuuine.docmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import yuuine.docmind.core.chat.config.HistoryMessageProperties;
import yuuine.docmind.core.chat.config.RagPromptProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        RagPromptProperties.class,
        HistoryMessageProperties.class
})
public class DocMindRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocMindRagApplication.class, args);
    }

}
