package yuuine.docmind.core.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelProviderType {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
    OPENAI("OpenAI", "https://api.openai.com"),
    MOONSHOT("Moonshot AI (Kimi)", "https://api.moonshot.cn/v1"),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    CUSTOM("自定义", null);

    private final String displayName;
    private final String defaultBaseUrl;

    public static ModelProviderType fromString(String value) {
        if (value == null) {
            return CUSTOM;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }

}
