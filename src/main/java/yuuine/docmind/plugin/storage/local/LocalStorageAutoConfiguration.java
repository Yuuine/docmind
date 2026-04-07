package yuuine.docmind.plugin.storage.local;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalStorageProperties.class)
public class LocalStorageAutoConfiguration {

    @Bean
    public LocalStoragePlugin localStoragePlugin(LocalStorageProperties properties) {
        return new LocalStoragePlugin(properties);
    }
}
