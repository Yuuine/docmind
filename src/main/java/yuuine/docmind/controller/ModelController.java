package yuuine.docmind.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import yuuine.docmind.common.model.Result;
import yuuine.docmind.core.model.dto.AIModelCreateRequest;
import yuuine.docmind.core.model.dto.AIModelResponse;
import yuuine.docmind.core.model.dto.AIModelUpdateRequest;
import yuuine.docmind.core.model.dto.ModelTestConnectionRequest;
import yuuine.docmind.core.model.service.ModelService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    public Result<List<AIModelResponse>> getModels(@RequestParam Long userId) {
        return Result.success(modelService.getModels(userId));
    }

    @PostMapping
    public Result<AIModelResponse> createModel(@RequestBody AIModelCreateRequest request,
                                               @RequestParam Long userId) {
        return Result.success(modelService.createModel(request, userId));
    }

    @PutMapping("/{id}")
    public Result<AIModelResponse> updateModel(@PathVariable Long id,
                                               @RequestBody AIModelUpdateRequest request,
                                               @RequestParam Long userId) {
        return Result.success(modelService.updateModel(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id, @RequestParam Long userId) {
        modelService.deleteModel(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/activate")
    public Result<Void> activateModel(@PathVariable Long id, @RequestParam Long userId) {
        modelService.activateModel(id, userId);
        return Result.success();
    }

    @PostMapping("/test-connection")
    public Result<Void> testConnection(@RequestBody ModelTestConnectionRequest request) {
        modelService.testConnection(request);
        return Result.success();
    }
}
