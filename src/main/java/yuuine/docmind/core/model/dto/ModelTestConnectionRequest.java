package yuuine.docmind.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTestConnectionRequest {
    private String baseUrl;
    private String apiKey;
    private String modelName;
}
