package yuuine.docmind.core.model.service;

import yuuine.docmind.core.model.dto.AIModelCreateRequest;
import yuuine.docmind.core.model.dto.AIModelResponse;
import yuuine.docmind.core.model.dto.AIModelUpdateRequest;
import yuuine.docmind.core.model.dto.ModelTestConnectionRequest;

import java.util.List;

public interface ModelService {
    List<AIModelResponse> getModels(Long userId);

    AIModelResponse createModel(AIModelCreateRequest request, Long userId);

    AIModelResponse updateModel(Long id, AIModelUpdateRequest request, Long userId);

    void deleteModel(Long id, Long userId);

    void activateModel(Long id, Long userId);

    void testConnection(ModelTestConnectionRequest request);
}
