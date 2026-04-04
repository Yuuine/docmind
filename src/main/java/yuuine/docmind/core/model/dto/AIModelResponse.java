package yuuine.docmind.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModelResponse {
    private Long id;
    private String name;
    private String baseUrl;
    private String modelName;
    private Integer maxTokens;
    private Double temperature;
    private String providerType;
    private String extraConfig;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
