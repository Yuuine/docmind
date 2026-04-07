package yuuine.docmind.core.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.model.dto.*;
import yuuine.docmind.core.model.entity.AIModel;
import yuuine.docmind.core.model.repository.AIModelRepository;
import yuuine.docmind.core.model.service.ModelService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final AIModelRepository aiModelRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<AIModelResponse> getModels(Long userId) {
        LambdaQueryWrapper<AIModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIModel::getUserId, userId)
                .orderByDesc(AIModel::getUpdatedAt);

        return aiModelRepository.selectList(queryWrapper).stream()
                .map(this::toModelResponse)
                .toList();
    }

    @Override
    public AIModelResponse createModel(AIModelCreateRequest request, Long userId) {
        deactivateAllModels(userId);

        AIModel model = AIModel.builder()
                .userId(userId)
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .apiKey(request.getApiKey())
                .modelName(request.getModelName())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .isActive(true)
                .build();

        aiModelRepository.insert(model);
        return toModelResponse(model);
    }

    @Override
    public AIModelResponse updateModel(Long id, AIModelUpdateRequest request, Long userId) {
        AIModel model = aiModelRepository.selectById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        if (!model.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此模型");
        }

        if (StringUtils.hasText(request.getName())) {
            model.setName(request.getName());
        }
        if (StringUtils.hasText(request.getBaseUrl())) {
            model.setBaseUrl(request.getBaseUrl());
        }
        if (StringUtils.hasText(request.getApiKey())) {
            model.setApiKey(request.getApiKey());
        }
        if (StringUtils.hasText(request.getModelName())) {
            model.setModelName(request.getModelName());
        }
        if (request.getMaxTokens() != null) {
            model.setMaxTokens(request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            model.setTemperature(request.getTemperature());
        }

        aiModelRepository.updateById(model);
        return toModelResponse(model);
    }

    @Override
    public void deleteModel(Long id, Long userId) {
        AIModel model = aiModelRepository.selectById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        if (!model.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此模型");
        }

        aiModelRepository.deleteById(id);
    }

    @Override
    public void activateModel(Long id, Long userId) {
        AIModel model = aiModelRepository.selectById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        if (!model.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此模型");
        }

        deactivateAllModels(userId);

        model.setIsActive(true);
        aiModelRepository.updateById(model);
    }

    @Override
    public void testConnection(ModelTestConnectionRequest request) {
        String apiUrl = request.getBaseUrl().replaceAll("/$", "") + "/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.getModelName());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", "Hi")));
        requestBody.put("max_tokens", 10);
        requestBody.put("stream", false);

        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        try {
            webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + request.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            log.info("模型连接测试成功, model={}", request.getModelName());
        } catch (Exception e) {
            log.error("模型连接测试失败, model={}, error={}", request.getModelName(), e.getMessage());
            throw new BusinessException(ErrorCode.MODEL_CONNECTION_FAILED, "连接失败: " + e.getMessage());
        }
    }

    private void deactivateAllModels(Long userId) {
        LambdaUpdateWrapper<AIModel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AIModel::getUserId, userId)
                .set(AIModel::getIsActive, false);
        aiModelRepository.update(null, updateWrapper);
    }

    private AIModelResponse toModelResponse(AIModel model) {
        return AIModelResponse.builder()
                .id(model.getId())
                .name(model.getName())
                .baseUrl(model.getBaseUrl())
                .modelName(model.getModelName())
                .maxTokens(model.getMaxTokens())
                .temperature(model.getTemperature())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
