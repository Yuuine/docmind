package yuuine.docmind.core.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import yuuine.docmind.common.exception.BusinessException;
import yuuine.docmind.common.exception.ErrorCode;
import yuuine.docmind.core.model.dto.AIModelCreateRequest;
import yuuine.docmind.core.model.dto.AIModelResponse;
import yuuine.docmind.core.model.dto.AIModelUpdateRequest;
import yuuine.docmind.core.model.entity.AIModel;
import yuuine.docmind.core.model.repository.AIModelRepository;
import yuuine.docmind.core.model.service.ModelService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final AIModelRepository aiModelRepository;

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
                .providerType(request.getProviderType())
                .extraConfig(request.getExtraConfig())
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
        if (StringUtils.hasText(request.getProviderType())) {
            model.setProviderType(request.getProviderType());
        }
        if (request.getExtraConfig() != null) {
            model.setExtraConfig(request.getExtraConfig());
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
                .providerType(model.getProviderType())
                .extraConfig(model.getExtraConfig())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
