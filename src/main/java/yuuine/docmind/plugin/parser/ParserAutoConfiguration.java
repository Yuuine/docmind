package yuuine.docmind.plugin.parser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yuuine.docmind.common.plugin.ParserPlugin;

import java.util.List;

@Configuration
public class ParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PdfParserPlugin pdfParserPlugin() {
        return new PdfParserPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    public WordParserPlugin wordParserPlugin() {
        return new WordParserPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelParserPlugin excelParserPlugin() {
        return new ExcelParserPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    public PptParserPlugin pptParserPlugin() {
        return new PptParserPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    public MarkdownParserPlugin markdownParserPlugin() {
        return new MarkdownParserPlugin();
    }

    @Bean
    @ConditionalOnMissingBean
    public ParserPluginFactory parserPluginFactory(List<ParserPlugin> parsers) {
        return new ParserPluginFactory(parsers);
    }
}
